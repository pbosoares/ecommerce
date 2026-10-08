# Implantacao em VPS com Docker

Esta configuracao sobe PostgreSQL, API Spring Boot e frontend React servido por Caddy. Apenas as portas 80 e 443 sao publicadas; banco e API ficam na rede do Compose. Os dados do banco, os arquivos digitais e os certificados TLS usam volumes persistentes.

O Caddy encaminha o IP do cliente para a API e remove o cabecalho `Forwarded` recebido. Nao publique a porta da API diretamente nem adicione outro proxy sem revisar os cabecalhos confiaveis e o limitador de requisicoes.

## Antes da primeira subida

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
