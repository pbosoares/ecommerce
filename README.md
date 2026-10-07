# E-commerce

API Spring Boot com cadastro de usuarios, permissoes e catalogo de produtos fisicos e digitais por categoria. Requer Java 21 ou superior.

O frontend React da loja **Cazuma** fica em [`frontend/`](frontend/README.md). Para abrir a loja, execute `npm install` e `npm run dev` nessa pasta e visite `http://localhost:5173/`; abrir o arquivo `index.html` diretamente deixa os modulos React sem o servidor Vite. A API precisa estar em `localhost:8080` para carregar o catalogo e usar a conta e o carrinho.

## Demonstracao sem PostgreSQL

Para apresentar o projeto ou simular uma compra, inicie a API no perfil `demo`:

```powershell
.\mvnw.cmd "-Dspring-boot.run.profiles=demo" spring-boot:run
```

Em outro terminal, inicie o frontend com `npm install` e `npm run dev` dentro de `frontend/`. Abra `http://localhost:5173/`. O catalogo sera preenchido com seis produtos ficticios, fisicos e digitais, em quatro categorias. Crie uma conta ficticia na loja, adicione um produto ao carrinho, consulte o frete com um CEP e registre um pedido. O pedido fica em `AGUARDANDO_PAGAMENTO`; nao ha cobranca nem entrega digital. O banco H2 deste perfil fica apenas em memoria e todo o conteudo, inclusive contas e pedidos de teste, desaparece quando a API e encerrada. O perfil so escuta em `127.0.0.1` e nao deve ser usado como ambiente publico. Fora de `demo`, nenhum produto e inserido automaticamente.

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
{"nome":"Fone","descricao":"Sem fio","preco":99.90,"estoque":5,"categoriaId":1,"tipo":"FISICO"}
```

Use `POST /produtos` para criar e `PUT /produtos/{id}` para atualizar, ambos com o corpo acima. `GET /produtos?categoria=eletronicos` lista apenas os produtos da categoria; `GET /produtos` lista todos. Categorias desconhecidas retornam uma lista vazia. Produtos cadastrados antes desta mudanca permanecem visiveis na lista geral sem categoria; um administrador pode atribuir categoria ao atualiza-los.

## Tipos de produto

O catalogo nao depende de um fornecedor. `tipo` pode ser `FISICO` (padrao para produtos antigos) ou `DIGITAL`.

- Campos comuns: `nome`, `descricao`, `preco`, `categoriaId`, `sku` opcional e unico, ate dez URLs HTTP(S) em `imagens` e ate vinte pares em `atributos` (por exemplo, cor/tamanho ou idioma/formato).
- Produtos fisicos exigem `estoque`. Informe `pesoGramas` para permitir a cotacao de frete; `alturaCm`, `larguraCm` e `comprimentoCm` continuam opcionais. Sem peso, a cotacao e o fechamento do pedido retornam `422`.
- Produtos digitais nao usam estoque nem dimensoes de envio. O administrador envia o arquivo com `POST /produtos/{id}/arquivo` (`multipart/form-data`, campo `arquivo`, ate 20 MB). O arquivo fica fora da pasta publica, em `DIGITAL_STORAGE_PATH`, e so pode ser baixado pelo comprador depois da confirmacao do pagamento.

Em producao, as imagens sao URLs HTTP(S) cadastradas pelo administrador; a API ainda nao recebe arquivos nem hospeda imagens. As ilustracoes locais em `frontend/public/demo/` servem apenas ao catalogo ficticio. O frontend React fica separado da API Spring Boot.

## Carrinho e pedidos

Todas as rotas abaixo exigem `Authorization: Bearer <accessToken>`. O usuario acessa somente seu proprio carrinho e seus pedidos.

- `GET /carrinho` retorna itens e subtotal calculado pelos precos atuais do catalogo.
- `POST /carrinho/itens` recebe `{"produtoId":1,"quantidade":2}` e adiciona a quantidade ao item.
- `PUT /carrinho/itens/1` recebe `{"quantidade":3}` e define a quantidade do produto 1.
- `DELETE /carrinho/itens/1` remove o produto 1 do carrinho.
- `POST /pedidos` transforma o carrinho em pedido e o esvazia. `GET /pedidos` e `GET /pedidos/{id}` consultam o historico do comprador.

Se houver algum produto fisico, `POST /pedidos` exige endereco:

```json
{"entrega":{"cep":"01001000","logradouro":"Praca da Se","numero":"1","bairro":"Se","cidade":"Sao Paulo","uf":"SP"}}
```

Antes de finalizar, `POST /frete/cotacoes` com `{"cep":"01001000"}` retorna o peso dos itens fisicos, subtotal, frete e total do carrinho atual. Itens digitais nao entram no peso. O valor e apenas uma consulta: o servidor recalcula tudo ao criar o pedido. `POST /pedidos` usa o CEP do endereco de entrega, grava o frete e o total e deixa o pedido em `AGUARDANDO_PAGAMENTO`. Pedidos somente digitais tem frete zero e total igual ao subtotal. Se algum produto fisico nao tiver peso, a API retorna `422` sem criar o pedido nem esvaziar o carrinho.

O frete padrao da loja e **R$ 15,00** para qualquer CEP e peso de produtos fisicos, configurado por `app.frete.valor-padrao`. O administrador pode criar tarifas especificas em `POST /frete/faixas` para substituir esse padrao em determinadas faixas de CEP e peso:

```json
{"cepInicio":"00000000","cepFim":"09999999","pesoMinimoGramas":0,"pesoMaximoGramas":1000,"valor":15.00}
```

`GET /frete/faixas` lista e `DELETE /frete/faixas/{id}` remove as faixas. As duas extremidades de CEP sao inclusivas; a faixa de peso aceita `pesoMinimoGramas < peso <= pesoMaximoGramas`. Faixas sobrepostas sao recusadas; fora das faixas cadastradas vale a tarifa padrao. A tarifa propria da loja nao consulta os Correios, nao promete preco ou prazo oficial e nao gera etiqueta. Itens guardam copia do nome, SKU, tipo e preco para preservar o historico.

## Pagamento, estoque e entrega

Ao criar um pedido, o servidor recalcula os precos e o frete e reserva o estoque fisico sob bloqueio de banco. Estoque insuficiente impede a compra. Envie um `Idempotency-Key` unico de 8 a 80 caracteres em `POST /pedidos` para que uma repeticao da mesma tentativa retorne o pedido original, sem criar outro. O frontend gera e conserva essa chave durante tentativas de envio. Alterar o carrinho inicia outra tentativa. Um pedido sem sessao de pagamento expira depois de 30 minutos; cancelamento antes do checkout e expiracao liberam a reserva uma unica vez.

`POST /pedidos/{id}/checkout` cria ou devolve a sessao Stripe Checkout de um pedido do comprador. O frontend redireciona para a URL retornada. O retorno do navegador nao confirma pagamento: apenas `POST /webhooks/stripe`, com assinatura Stripe valida, pode mudar o pedido para `PAGO`, depois de conferir sessao, moeda BRL e valor. O webhook de expiracao libera o estoque. O administrador consulta `GET /admin/pedidos` e avanca `PATCH /pedidos/{id}/status` de `PAGO` para `EM_PREPARACAO`, `ENVIADO` e `ENTREGUE`. Nao ha estorno automatico nem integracao de etiqueta/rastreio.

Para baixar um arquivo digital pago, o comprador usa `GET /pedidos/{pedidoId}/itens/{itemId}/download`. A API confere a identidade, o pedido e o pagamento; o arquivo nunca e exposto como URL publica. A troca do arquivo do produto preserva o arquivo associado a pedidos anteriores. A remocao fisica dos arquivos antigos requer uma politica de retencao/backup antes de operar em producao.

Cada mudanca de estado gera uma notificacao persistida. O Spring Mail envia via SMTP em segundo plano e tenta novamente apos falhas. Configure `MAIL_HOST`, `MAIL_PORT` (padrao 587), `MAIL_USERNAME`, `MAIL_PASSWORD` e `EMAIL_FROM`; sem SMTP configurado, os e-mails ficam pendentes. Nao inclua senhas no repositorio.

Para usar `prod`, configure tambem `DB_PASSWORD`, `JWT_SECRET`, `STRIPE_SECRET_KEY` (chave live), `STRIPE_WEBHOOK_SECRET`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` (HTTPS) e `DIGITAL_STORAGE_PATH` privado e persistente. Cadastre no Stripe o endpoint HTTPS `/webhooks/stripe` para os eventos `checkout.session.completed`, `checkout.session.async_payment_succeeded` e `checkout.session.expired`. O perfil `demo` continua com produtos ficticios e simulacao sem cobranca; seus pedidos nao ficam pagos automaticamente.

A API limita requisicoes por IP em memoria (login/cadastro: 10/min; leitura: 600/min; demais: 120/min) e valida tamanhos dos principais campos. Em varias instancias, use um limitador compartilhado no proxy. Antes de vender, ainda sao necessarios HTTPS no proxy, migracoes versionadas do banco, backup e restauracao testados, monitoramento/alertas, testes com credenciais Stripe/SMTP reais e uma revisao de seguranca de producao. Os testes automatizados nao garantem que o site seja inviolavel.
