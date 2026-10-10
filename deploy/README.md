# Implantacao em VPS com Docker

## CI/CD no GitHub para Hostinger

O workflow `.github/workflows/ci-cd.yml` executa testes Java, build React e builds Docker em pull requests e pushes na `main`/`master`. Apos o CI passar, o deploy envia o commit testado via SSH e atualiza o Docker Compose na VPS. A publicacao fica desativada ate a variavel `VPS_DEPLOY_ENABLED` ser `true`.

1. Na VPS Hostinger, instale Docker Engine, Compose v2 (com suporte a `up --wait`) e `flock`. Crie um diretorio, por exemplo `/opt/ecommerce`, acessivel pelo usuario de deploy. Esse usuario precisa executar Docker. Se a loja ja estiver instalada, use o diretorio atual e confirme que o nome do projeto Compose corresponde ao nome desse diretorio, para reutilizar os volumes existentes.
2. Coloque o arquivo `.env` nesse diretorio conforme as instrucoes abaixo. Ele permanece exclusivamente na VPS. Configure dominio/DNS e firewall antes de ativar.
3. Crie uma chave SSH exclusiva para o GitHub Actions e adicione sua chave publica ao `~/.ssh/authorized_keys` do usuario da VPS. Nunca envie a chave privada pelo chat nem a adicione ao repositorio.
4. Em **GitHub > Settings > Secrets and variables > Actions**, crie os secrets `VPS_HOST` (IP ou hostname), `VPS_USER`, `VPS_PORT` (opcional; padrao 22), `VPS_SSH_KEY` (chave privada completa) e `VPS_KNOWN_HOSTS` (linha do known_hosts com a chave do servidor, conferida pelo console Hostinger; para porta diferente de 22 use `[host]:porta`). A conexao exige verificacao da chave do servidor.
5. Crie as variaveis `VPS_PATH` (exemplo `/opt/ecommerce`, sem espacos) e `VPS_DEPLOY_ENABLED` = `true`. O diretorio deve existir antes do primeiro deploy. Configure o environment `production` no GitHub caso queira regras de aprovacao.
6. Envie uma alteracao para `main` ou execute **Actions > CI/CD Hostinger VPS > Run workflow** nessa branch. Acompanhe a execucao no GitHub.

Cada release fica em `releases/<commit>` e a release bem-sucedida recebe o link `current`. Os volumes sao preservados. Antes de atualizar uma instalacao ativa, o script exporta o banco e copia os arquivos digitais para `backups/`. Guarde tambem backups fora da VPS e estabeleca retencao. O deploy constroi as imagens antes da atualizacao e exige containers saudaveis e resposta HTTP da API. Pode haver breve indisponibilidade durante a recriacao.

Se a atualizacao falhar, o workflow falha e o link `current` permanece na ultima release bem-sucedida; isso nao desfaz containers ou migracoes ja aplicadas. Revise logs e compatibilidade do esquema antes de reaplicar uma release anterior. Para operacoes manuais use o mesmo nome de projeto: `docker compose --project-name ecommerce --project-directory /opt/ecommerce/current -f /opt/ecommerce/current/compose.yaml ps` (adapte os caminhos e nome).

Esta configuracao sobe PostgreSQL, API Spring Boot e frontend React servido por Caddy. Apenas as portas 80 e 443 sao publicadas; banco e API ficam na rede do Compose. Os dados do banco, os arquivos digitais e os certificados TLS usam volumes persistentes.

O Caddy encaminha o IP do cliente para a API e remove o cabecalho `Forwarded` recebido. Nao publique a porta da API diretamente nem adicione outro proxy sem revisar os cabecalhos confiaveis e o limitador de requisicoes.

## Antes da primeira subida

### Stripe teste e Resend

Para homologacao na VPS, use `deploy/env.sandbox.example` como `.env`. `APP_PROFILE=sandbox` aceita somente `sk_test_`; o perfil `prod` continua exigindo `sk_live_`. Use banco e dominio separados caso ja exista uma loja com pagamentos reais. O perfil sandbox usa Flyway, validacao de esquema e HTTPS, sem criar produtos de demonstracao.

No sandbox da Stripe, configure o endpoint `https://SEU_DOMINIO/webhooks/stripe` com os eventos `checkout.session.completed`, `checkout.session.async_payment_succeeded` e `checkout.session.expired`; coloque a chave secreta de teste em `STRIPE_SECRET_KEY` e o segredo desse endpoint em `STRIPE_WEBHOOK_SECRET`. Nao use o segredo do Stripe CLI para o endpoint hospedado.

Na Resend, use a chave de API em `RESEND_API_KEY`. `onboarding@resend.dev` serve para testes com as restricoes de destinatario da Resend. Para envio aos compradores, verifique seu dominio na Resend e configure `EMAIL_FROM` com um remetente desse dominio. Para simular entrega sem enviar a clientes, use `delivered@resend.dev`. Referencias: https://docs.stripe.com/testing e https://resend.com/docs/dashboard/emails/send-test-emails.

1. Instale Docker Engine com Compose v2 na VPS. Aponte o registro DNS `A` (e `AAAA`, se houver IPv6) do dominio para a VPS e libere as portas 80 e 443 no firewall. O Caddy emite e renova o certificado HTTPS automaticamente quando o dominio estiver acessivel.
2. Copie `deploy/env.example` para `.env` na raiz do projeto. Preencha `SHOP_DOMAIN`, `DB_PASSWORD`, `JWT_SECRET` (32 bytes aleatorios em Base64), as chaves Stripe live, URLs HTTPS, `RESEND_API_KEY` e `EMAIL_FROM` de um dominio verificado no Resend. Proteja o arquivo com `chmod 600 .env`; ele esta no `.gitignore` e no `.dockerignore`.
3. Configure no painel Stripe o webhook `https://SEU_DOMINIO/webhooks/stripe` para `checkout.session.completed`, `checkout.session.async_payment_succeeded` e `checkout.session.expired`. Use o segredo de assinatura desse endpoint em `STRIPE_WEBHOOK_SECRET`. O retorno para `STRIPE_SUCCESS_URL` nao confirma pagamento; so o webhook confirma.
4. Confira backups e o estado do banco antes de atualizar uma instalacao existente. Esta configuracao inicializa um PostgreSQL novo em volume proprio; ela nao importa automaticamente o banco local ou de outra VPS.

```sh
docker compose config --quiet
docker compose build
docker compose up -d
docker compose ps
docker compose logs --tail=100 api web
```

O healthcheck `https://SEU_DOMINIO/api/actuator/health` responde apenas o estado agregado, sem detalhes internos. Configure monitoramento externo para esse endereco e alertas para falhas da API, do banco, dos e-mails pendentes e dos webhooks Stripe. O retorno do Stripe mostra ao comprador que a confirmacao pode levar alguns instantes.

O perfil `prod` impede a subida com chaves Stripe/Resend, armazenamento digital ou URLs HTTPS ausentes. Sexta-feira, antes de ativar pagamentos, faca uma compra de teste em ambiente separado com chaves `sk_test_` e webhook de teste; a configuracao `prod` exige chave live e nao deve ser usada para esse teste. Valide tambem envio de e-mail e download digital apos o webhook. Produtos ficticios permanecem apenas no perfil local `demo`.

## Backup e atualizacao

Antes de cada atualizacao, faca backup do banco e dos arquivos digitais, guarde as copias fora da VPS e teste uma restauracao em ambiente separado. Um exemplo de exportacao do banco:

```sh
mkdir -p backups
docker compose exec -T db pg_dump -U ecommerce -d ecommerce -Fc > "backups/ecommerce-$(date +%Y%m%d-%H%M%S).dump"
docker compose cp api:/data/digital "backups/digital-$(date +%Y%m%d-%H%M%S)"
```

Para atualizar o codigo, preserve `.env` e os volumes, rode os testes no checkout, faca novo backup e execute `docker compose up -d --build`. **Nao use `docker compose down -v`**, pois ele remove os volumes. A API em `prod` usa Flyway e valida o esquema no inicio; a primeira migracao suporta banco vazio e o esquema legado com apenas `usuario` e `produto`. Antes de aplicar em outro banco existente, compare o esquema e restaure um backup de teste. Nao trate esta configuracao como uma garantia de operacao segura ou disponibilidade sem observabilidade, backup restaurado e revisao de seguranca na VPS.
