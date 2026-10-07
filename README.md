# E-commerce

API Spring Boot com cadastro de usuarios, permissoes e catalogo de produtos por categoria. Requer Java 21 ou superior.

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

`POST /usuarios`, `POST /auth/login`, `GET /categorias` e `GET /produtos` (inclusive por ID) sao publicos. Apenas administradores podem criar categorias e criar, atualizar ou excluir produtos. Os demais endpoints exigem autenticacao:

```http
Authorization: Bearer <accessToken>
```

O JWT usa HS256, identifica o usuario pelo ID (`sub`) e valida assinatura, emissor e expiracao. A validade padrao e de 15 minutos (`app.jwt.ttl-seconds=900`). Tokens ausentes, invalidos ou expirados retornam `401`. A API nao cria sessao; cada requisicao precisa do token. Basic Auth e formulario de login estao desativados.

O cadastro publico sempre cria um `CLIENTE`, mesmo se o corpo da requisicao tentar enviar `papel: ADMIN`. Contas antigas sem papel sao tratadas como clientes. Para provisionar o primeiro administrador, cadastre-o normalmente e execute, com acesso administrativo ao PostgreSQL:

```sql
UPDATE usuario SET papel = 'ADMIN' WHERE email = 'admin@exemplo.com';
```

Substitua o e-mail pelo da conta desejada. Faca login novamente apos a promocao; o papel fica no JWT assinado. Uma alteracao de papel passa a valer para novos tokens; tokens anteriores expiram em ate 15 minutos. Nao ha refresh token nem revogacao individual. Use HTTPS fora do ambiente local.

Os testes cobrem login com Argon2, validacao de entrada, CRUD com JWT, bloqueio sem token, adulteracao, assinatura incorreta, emissor incorreto, expiracao e ausencia de sessao.

## Categorias no catalogo

O administrador cria uma categoria com `POST /categorias`:

```json
{"nome":"Eletronicos","slug":"eletronicos"}
```

O `slug` e unico e usa letras minusculas, numeros e hifens. `GET /categorias` fornece a lista para montar as abas. Produtos novos precisam do ID de uma categoria:

```json
{"nome":"Fone","descricao":"Sem fio","preco":99.90,"estoque":5,"categoriaId":1}
```

Use `POST /produtos` para criar e `PUT /produtos/{id}` para atualizar, ambos com o corpo acima. `GET /produtos?categoria=eletronicos` lista apenas a aba dessa categoria; `GET /produtos` lista todos. Categorias desconhecidas retornam uma lista vazia. Produtos cadastrados antes desta mudanca permanecem visiveis na lista geral sem categoria; um administrador pode atribuir categoria ao atualiza-los.

A pagina publica `/` mostra o catalogo com uma aba para cada categoria e permite compartilhar o filtro na URL (`/?categoria=eletronicos`). A pagina exibe disponibilidade, mas ainda nao existe carrinho, checkout ou calculo de frete.
