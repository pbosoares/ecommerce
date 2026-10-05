# E-commerce

API Spring Boot com cadastro de usuarios e CRUD de produtos. Requer Java 21 ou superior.

## Executar e testar

- Configure `DB_PASSWORD` para o PostgreSQL local (`localhost:5432/ecommerce`, usuario `postgres`).
- Inicie com `./mvnw spring-boot:run` (Windows: `.\mvnw.cmd spring-boot:run`).
- Execute `./mvnw test` (Windows: `.\mvnw.cmd test`). Os testes usam H2 em memoria e nao alteram o PostgreSQL.
- Com as dependencias ja no cache, acrescente `-o` para testar offline.

O Lombok esta configurado como processador de anotacoes explicitamente, inclusive para JDK 23+.

## Cadastro e login

`POST /usuarios` recebe `nome`, `email` e `senha`; a senha e armazenada como hash Argon2.

`POST /auth/login` recebe:

```json
{"email":"ana@example.com","senha":"senha-segura"}
```

Retorna `200` com `id`, `nome` e `email`, sem senha ou hash. Credenciais incorretas retornam `401`; campos invalidos retornam `400`.
O e-mail e comparado exatamente como no cadastro. Nesta etapa o login apenas verifica as credenciais; nao cria sessao nem token.
