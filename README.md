# E-commerce

API Spring Boot com cadastro de usuarios e CRUD de produtos. Requer Java 21 ou superior.

## Executar e testar

- Configure `DB_PASSWORD` para o PostgreSQL local (`localhost:5432/ecommerce`, usuario `postgres`).
- Configure `JWT_SECRET` com pelo menos 32 bytes aleatorios codificados em Base64. Sem uma chave valida, a aplicacao nao inicia. Nao coloque a chave no Git. No PowerShell, gere uma chave para a sessao atual com:

  ```powershell
  $jwtBytes = New-Object byte[] 32
  $jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
  $jwtRandom.GetBytes($jwtBytes)
  $jwtRandom.Dispose()
  $env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
  ```

  Mantenha a mesma chave entre reinicios para preservar a validade dos tokens existentes; trocar a chave invalida esses tokens.
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

Retorna `200` com `accessToken`, `tokenType` (`Bearer`) e `expiresIn` (`900`, em segundos), sem senha ou hash. Credenciais incorretas retornam `401`; campos invalidos retornam `400`.
O e-mail e comparado exatamente como no cadastro.

## Endpoints protegidos

Apenas `POST /usuarios` e `POST /auth/login` sao publicos. Todos os outros endpoints, incluindo leitura e escrita de produtos, exigem:

```http
Authorization: Bearer <accessToken>
```

O JWT usa HS256, identifica o usuario pelo ID (`sub`) e valida assinatura, emissor e expiracao. A validade padrao e de 15 minutos (`app.jwt.ttl-seconds=900`). Tokens ausentes, invalidos ou expirados retornam `401`. A API nao cria sessao; cada requisicao precisa do token. Basic Auth e formulario de login estao desativados.

Qualquer usuario autenticado pode acessar o CRUD de produtos; ainda nao ha papeis de administrador. Nao ha refresh token nem revogacao individual: ao expirar, faca login novamente. Use HTTPS fora do ambiente local.

Os testes cobrem login com Argon2, validacao de entrada, CRUD com JWT, bloqueio sem token, adulteracao, assinatura incorreta, emissor incorreto, expiracao e ausencia de sessao.
