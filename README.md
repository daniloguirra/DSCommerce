# DSCommerce

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.9-blue?logo=apachemaven)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

API REST de um sistema de **e-commerce** desenvolvido em **Java + Spring Boot**, com autenticação/autorização via **OAuth2 Authorization Server + JWT**, persistência com **Spring Data JPA** e banco **H2** em memória (perfil de testes).

Projeto inspirado nas bootcamps da DevSuperior, aprimorado e adaptado com foco didático e profissional.

---

## Sumário

- [Visão geral](#visão-geral)
- [Tecnologias](#tecnologias)
- [Arquitetura e pacotes](#arquitetura-e-pacotes)
- [Modelo de domínio](#modelo-de-domínio)
- [Segurança (OAuth2 + JWT)](#segurança-oauth2--jwt)
- [Endpoints da API](#endpoints-da-api)
- [Tratamento de exceções](#tratamento-de-exceções)
- [Banco de dados](#banco-de-dados)
- [Configurações](#configurações)
- [Como executar](#como-executar)
- [Exemplos de uso](#exemplos-de-uso)
- [Estrutura de pastas](#estrutura-de-pastas)
- [Roadmap](#roadmap)
- [Licença](#licença)

---

## Visão geral

O **DSCommerce** é uma API REST para um catálogo de produtos e pedidos de um e-commerce. Ela expõe endpoints para consulta pública de produtos (paginada e filtrada por nome) e endpoints protegidos para operações administrativas (cadastro, atualização e remoção de produtos).

Principais características:

- API REST com padrão **MVC em camadas** (Controller → Service → Repository → Entity).
- **DTOs** com validação via Bean Validation (Jakarta Validation + Hibernate Validator).
- Autenticação via **OAuth2 Password Grant customizado** emitindo **JWT**.
- Autorização por **roles** (`ROLE_CLIENT`, `ROLE_ADMIN`) usando `@PreAuthorize`.
- **Paginação, ordenação e busca** através de `Pageable`.
- Tratamento centralizado de exceções com `@ControllerAdvice`.
- Configuração de **CORS** externalizada via `application.properties`.
- Perfis de ambiente (`test` por padrão) e H2 Console habilitado para desenvolvimento.

---

## Tecnologias

| Categoria | Tecnologia |
|-----------|------------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.5 |
| Web | Spring Web MVC |
| Persistência | Spring Data JPA + Hibernate |
| Banco de dados | H2 (em memória, perfil `test`) |
| Segurança | Spring Security + Spring Authorization Server + Resource Server (JWT) |
| Validação | Jakarta Validation + Hibernate Validator |
| Build | Maven |
| Testes | Spring Boot Test + Spring Security Test |

---

## Arquitetura e pacotes

```
com.devguirra.dscommerce
├── DscommerceApplication.java      // Classe principal (bootstrap do Spring)
├── config                          // Configurações de segurança (OAuth2 + CORS)
│   ├── AuthorizationServerConfig
│   ├── ResourceServerConfig
│   └── customgrant                 // Password Grant customizado (JWT)
├── controllers                     // Camada de exposição (REST)
│   ├── ProductController
│   └── handlers.ControllerExceptionHandler
├── dto                             // Objetos de transferência (+ validações)
│   ├── ProductDTO
│   ├── CustomError / ValidationError / FieldMessage
├── entities                        // Entidades JPA (modelo de domínio)
│   ├── Product, Category
│   ├── Order, OrderItem, OrderItemPK, OrderStatus, Payment
│   ├── User, Role
├── projections                     // Projeções JPA para otimização de consultas
│   └── UserDetailsProjection
├── repositories                    // Spring Data JPA
│   ├── ProductRepository
│   └── UserRepository
└── services                        // Regras de negócio
    ├── ProductService
    ├── UserService (UserDetailsService)
    └── exceptions                  // Exceções customizadas
        ├── ResourceNotFoundException
        └── DataBaseException
```

A separação em camadas (controller / service / repository) mantém a API desacoplada das regras de negócio e da persistência, favorecendo testabilidade e manutenção.

---

## Modelo de domínio

O diagrama lógico (simplificado) das entidades principais:

```
User (1) ───< (N) Order (1) ──< (N) OrderItem (N) >── (1) Product (N) >── (N) Category
                         │
                         └── (1) Payment
User (N) >── (N) Role
```

Entidades e tabelas:

| Entidade | Tabela | Papel |
|----------|--------|-------|
| `User` | `tb_user` | Usuário do sistema; implementa `UserDetails` (Spring Security) |
| `Role` | `tb_role` | Papel/autoridade do usuário (`ROLE_CLIENT`, `ROLE_ADMIN`) |
| `Product` | `tb_product` | Produto do catálogo |
| `Category` | `tb_category` | Categoria (relação N-N com Produto) |
| `Order` | `tb_order` | Pedido realizado por um cliente |
| `OrderItem` | `tb_order_item` | Item do pedido (chave composta `product + order`) |
| `Payment` | `tb_payment` | Pagamento associado a um pedido (1-1) |

Observações importantes:

- `OrderItem` usa **chave composta embutida** via `OrderItemPK`.
- `OrderStatus` é um **enum** persistido como ordinal (`WAITING_PAYMENT`, `PAID`, etc.).
- `User` implementa `UserDetails`, integrando direto com o Spring Security.

---

## Segurança (OAuth2 + JWT)

A aplicação é dividida em dois "servidores" do Spring Security:

### 1. Authorization Server (`AuthorizationServerConfig`)

Emite tokens **JWT** via um fluxo de **Password Grant customizado**, implementado em:

- `CustomPasswordAuthenticationConverter` – converte a requisição em um token de autenticação.
- `CustomPasswordAuthenticationProvider` – valida credenciais contra o `UserDetailsService`.
- `CustomPasswordAuthenticationToken` – token de autenticação próprio do grant.
- `CustomUserAuthorities` – customização das claims do JWT (inclui `authorities` e `username`).

O endpoint padrão de emissão de token é:

```
POST /oauth2/token
```

As credenciais do client são parametrizáveis via variáveis de ambiente:

| Variável | Default | Descrição |
|----------|---------|-----------|
| `CLIENT_ID` | `myclientid` | Client ID do OAuth2 |
| `CLIENT_SECRET` | `myclientsecret` | Client Secret |
| `JWT_DURATION` | `86400` | Duração do access token (em segundos) |

### 2. Resource Server (`ResourceServerConfig`)

Protege as rotas da API validando o JWT recebido no header `Authorization: Bearer <token>`.

Principais pontos:

- `oauth2ResourceServer().jwt()` para validação automática do JWT.
- `JwtAuthenticationConverter` customizado que extrai as **authorities** do claim `authorities` (sem prefixo adicional).
- **CORS** configurável via `cors.origins` (lista separada por vírgula).
- Filter chain específico para o **H2 Console** no perfil `test` (CSRF desabilitado, frame options liberadas).
- Autorização por método via `@PreAuthorize("hasAnyRole('ROLE_ADMIN')")`.

---

## Endpoints da API

Base path: `/products`

| Método | Rota | Autenticação | Role | Descrição |
|--------|------|--------------|------|-----------|
| `GET` | `/products/{id}` | Pública | — | Busca produto por ID |
| `GET` | `/products?name=&page=&size=&sort=` | Pública | — | Lista produtos paginados com filtro opcional por nome |
| `POST` | `/products` | Obrigatória | `ROLE_ADMIN` | Cria um novo produto |
| `PUT` | `/products/{id}` | Obrigatória | `ROLE_ADMIN` | Atualiza um produto existente |
| `DELETE` | `/products/{id}` | Obrigatória | `ROLE_ADMIN` | Remove um produto |

Extras:

| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/oauth2/token` | Emissão do JWT (password grant) |
| `GET` | `/h2-console` | Console do H2 (perfil `test`) |

### Payload de `ProductDTO`

```json
{
  "id": 1,
  "name": "PC Gamer",
  "description": "Descrição com ao menos 10 caracteres",
  "price": 1200.00,
  "imgUrl": "https://..."
}
```

Validações aplicadas (Bean Validation):

- `name`: obrigatório, entre 3 e 80 caracteres.
- `description`: obrigatório, mínimo de 10 caracteres.
- `price`: obrigatório e positivo (`@Positive`).

---

## Tratamento de exceções

`ControllerExceptionHandler` centraliza o tratamento:

| Exceção | HTTP Status | Resposta |
|---------|-------------|----------|
| `ResourceNotFoundException` | 404 Not Found | `CustomError` |
| `DataBaseException` | 400 Bad Request | `CustomError` |
| `MethodArgumentNotValidException` | 422 Unprocessable Entity | `ValidationError` com lista de `FieldMessage` |

Exemplo de resposta de erro de validação:

```json
{
  "timestamp": "2026-04-22T16:30:00Z",
  "status": 422,
  "error": "Dados Inválidos",
  "path": "/products",
  "errors": [
    { "fieldName": "name", "message": "Campo requerido" },
    { "fieldName": "price", "message": "O preço deve ser positivo" }
  ]
}
```

---

## Banco de dados

O projeto usa **H2 em memória** por padrão (perfil `test`). Os dados são recriados a cada inicialização através do arquivo `src/main/resources/import.sql`, que já popula:

- 3 categorias (Livros, Eletrônicos, Computadores)
- 25 produtos com imagens de referência
- 2 usuários (`maria@gmail.com` e `alex@gmail.com`)
- Roles `ROLE_CLIENT` e `ROLE_ADMIN`
- Alguns pedidos, itens e pagamentos de exemplo

Usuários seed (senha: `123456`):

| Email | Role(s) |
|-------|---------|
| `maria@gmail.com` | `ROLE_CLIENT` |
| `alex@gmail.com` | `ROLE_CLIENT`, `ROLE_ADMIN` |

Console do H2 disponível em: http://localhost:8080/h2-console

| Campo | Valor |
|-------|-------|
| JDBC URL | `jdbc:h2:mem:testdb` |
| User | `sa` |
| Password | *(vazio)* |

---

## Configurações

### `application.properties`

```properties
spring.application.name=dscommerce
spring.profiles.active=test
spring.jpa.open-in-view=false

security.client-id=${CLIENT_ID:myclientid}
security.client-secret=${CLIENT_SECRET:myclientsecret}
security.jwt.duration=${JWT_DURATION:86400}

cors.origins=${CORS_ORIGINS:http://localhost:3000,http://localhost:5173}
```

### Variáveis de ambiente suportadas

| Variável | Descrição |
|----------|-----------|
| `CLIENT_ID` | Client ID do OAuth2 |
| `CLIENT_SECRET` | Client Secret do OAuth2 |
| `JWT_DURATION` | Duração do token em segundos |
| `CORS_ORIGINS` | Lista de origens permitidas, separada por vírgula |

---

## Como executar

### Pré-requisitos

- **Java 17+**
- **Maven 3.9+** (ou use o `mvnw`/`mvnw.cmd` incluso)

### Clonando e rodando

```bash
git clone https://github.com/<seu-usuario>/dscommerce.git
cd dscommerce
```

Windows (PowerShell):

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

### Gerando o JAR

```bash
./mvnw clean package
java -jar target/dscommerce-0.0.1-SNAPSHOT.jar
```

---

## Exemplos de uso

### 1. Obter um token JWT

```bash
curl -X POST http://localhost:8080/oauth2/token \
  -u myclientid:myclientsecret \
  -d "grant_type=password" \
  -d "username=alex@gmail.com" \
  -d "password=123456"
```

Resposta (resumida):

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6...",
  "token_type": "Bearer",
  "expires_in": 86400
}
```

### 2. Listar produtos (público, paginado)

```bash
curl "http://localhost:8080/products?name=gamer&page=0&size=5&sort=name,asc"
```

### 3. Criar um produto (somente admin)

```bash
curl -X POST http://localhost:8080/products \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Teclado Mecânico",
        "description": "Teclado mecânico com switches blue.",
        "price": 350.00,
        "imgUrl": "https://exemplo.com/teclado.jpg"
      }'
```

### 4. Atualizar um produto

```bash
curl -X PUT http://localhost:8080/products/1 \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "The Lord of the Rings (Edição Especial)",
        "description": "Edição ilustrada com capa dura.",
        "price": 150.00,
        "imgUrl": "https://exemplo.com/lotr.jpg"
      }'
```

### 5. Remover um produto

```bash
curl -X DELETE http://localhost:8080/products/1 \
  -H "Authorization: Bearer <TOKEN>"
```

---

## Estrutura de pastas

```
dscommerce/
├── src/
│   ├── main/
│   │   ├── java/com/devguirra/dscommerce/
│   │   │   ├── config/
│   │   │   ├── controllers/
│   │   │   ├── dto/
│   │   │   ├── entities/
│   │   │   ├── projections/
│   │   │   ├── repositories/
│   │   │   ├── services/
│   │   │   └── DscommerceApplication.java
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-test.properties
│   │       └── import.sql
│   └── test/
│       └── java/com/devguirra/dscommerce/
│           └── DscommerceApplicationTests.java
├── pom.xml
├── mvnw, mvnw.cmd
└── README.md
```

---

## Roadmap

Ideias de evolução para este projeto:

- [ ] CRUD completo de **Categorias**
- [ ] CRUD de **Usuários** (cadastro self-service)
- [ ] Endpoints de **Pedidos** (checkout, histórico do cliente)
- [ ] Endpoint `/me` para retornar o usuário autenticado
- [ ] Migração do H2 para **PostgreSQL** em produção (perfil `prod`)
- [ ] **Flyway** ou **Liquibase** para versionamento de schema
- [ ] Documentação interativa com **Springdoc OpenAPI (Swagger UI)**
- [ ] Testes unitários e de integração (camada de serviços e controllers)
- [ ] Pipeline de CI/CD (GitHub Actions)
- [ ] Dockerfile + docker-compose para subir API + banco

---

## Licença

Este projeto é distribuído sob a licença **MIT**. Sinta-se livre para estudar, modificar e usar como base para seus próprios projetos.

---

> Desenvolvido por **Danilo Guirra** ([@devguirra](https://github.com/devguirra)) com fins didáticos e de portfólio.
