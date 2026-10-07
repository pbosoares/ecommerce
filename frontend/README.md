# Frontend Cazuma

Requer Node.js e npm. Nao abra `index.html` diretamente no navegador: o React precisa do servidor Vite.

No PowerShell, dentro da pasta `frontend`:

```powershell
npm install
npm run dev
```

Abra o endereco exibido pelo Vite, normalmente `http://localhost:5173/`. Para compilar: `npm run build`.

Durante o desenvolvimento, o Vite encaminha `/api` para o backend em `http://localhost:8080`. Para experimentar o catalogo com produtos ficticios, inicie o Spring Boot no perfil `demo` com `.\mvnw.cmd "-Dspring-boot.run.profiles=demo" spring-boot:run` na raiz do projeto. Esse perfil usa H2 em memoria, sem PostgreSQL; os dados sao apagados ao parar a API. Para usar dados persistentes, configure `DB_PASSWORD` e `JWT_SECRET` conforme o README principal e inicie a API sem o perfil `demo`. Sem o backend, a vitrine abre e mostra que o catalogo nao pôde ser carregado.

O frontend e responsivo. O checkout registra pedidos na API, mas ainda nao realiza pagamentos nem entrega produtos digitais.
