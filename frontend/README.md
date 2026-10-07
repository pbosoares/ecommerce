# Frontend Cazuma

Requer Node.js e npm. Nao abra `index.html` diretamente no navegador: o React precisa do servidor Vite.

No PowerShell, dentro da pasta `frontend`:

```powershell
npm install
npm run dev
```

Abra o endereco exibido pelo Vite, normalmente `http://localhost:5173/`. Para compilar: `npm run build`.

Durante o desenvolvimento, o Vite encaminha `/api` para o backend em `http://localhost:8080`. Para ver produtos, criar conta e usar carrinho e pedidos, inicie tambem o Spring Boot com PostgreSQL e as variaveis `DB_PASSWORD` e `JWT_SECRET` conforme o README principal. Sem o backend, a vitrine abre e mostra que o catalogo nao pôde ser carregado.

O frontend e responsivo. O checkout registra pedidos na API, mas ainda nao realiza pagamentos nem entrega produtos digitais.
