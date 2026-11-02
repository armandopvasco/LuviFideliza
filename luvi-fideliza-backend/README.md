# LUVI Fideliza+

Cartão de fidelidade para o pequeno comércio. O atendente usa o aplicativo do balcão no celular. Campanhas e equipe continuam no painel web. O cliente usa o aplicativo da carteira ou a página `cliente.html`.

O backend é Java 17, Spring Boot 3.5, PostgreSQL, Maven e Flyway. A versão de tela do painel é 0.9.0.

## O que o produto faz

- O proprietário define o programa (meta de carimbos e recompensa) e convida a equipe.
- No balcão, o WhatsApp localiza o cliente e registra o carimbo. A primeira visita pede o nome e abre o cartão.
- Ao completar a meta, a recompensa fica disponível e o balcão confirma a entrega.
- O QR da loja leva o cliente à adesão. O QR do cliente e o QR da recompensa são lidos pela câmera do aplicativo do balcão. O mesmo link também abre o painel web.
- O aplicativo do cliente entra com token, adere pelo QR da loja e mostra os cartões. O aplicativo do balcão usa o mesmo token e só aceita operador ou administrador. O painel continua entrando por cookie de sessão.

## Executar o backend

Abra o `pom.xml` no IntelliJ, selecione JDK 17 e configure:

- `LUVI_FIDELIZA_DB_URL=jdbc:postgresql://localhost:5432/luvi_fideliza`
- `LUVI_FIDELIZA_DB_USER=luvi_fideliza_app`
- `LUVI_FIDELIZA_DB_PASSWORD` com a senha do banco

Execute `LuviFidelizaApplication`. A porta padrão é 8081. O painel fica em `http://localhost:8081/painel.html`.

Se a subida parar com `Migration checksum mismatch` nas versões 1 a 7, crie um banco novo e aponte `LUVI_FIDELIZA_DB_URL` para ele. O Flyway aplica os scripts atuais, da versão 1 até a 14. Para o primeiro administrador, use o perfil `local` com `LUVI_FIDELIZA_BOOTSTRAP_ENABLED=true`, `LUVI_FIDELIZA_ADMIN_EMAIL` e `LUVI_FIDELIZA_ADMIN_PASSWORD` (12 a 72 caracteres ASCII).

`LUVI_FLYWAY_REPAIR=true` só entra quando o banco antigo precisa ser conservado. Suba uma vez com essa variável e depois remova. O reparo atualiza o histórico e não recria as tabelas.

Para o QR apontar para um endereço que o celular alcança, defina `LUVI_PUBLIC_URL` (por exemplo `http://192.168.0.10:8081`). Sem essa variável, o QR usa o endereço do próprio pedido.

O aplicativo do cliente está em `../luvi-fideliza-cliente`. O do balcão está em `../luvi-fideliza-estabelecimento`. Os dois chamam `POST /api/v1/auth/token` e enviam `Authorization: Bearer`. O balcão consulta a recompensa em `GET /api/v1/estabelecimentos/{id}/recompensas/{recompensa}` antes de confirmar a entrega.

## Testes

```bash
mvn test
```

Os roteiros manuais antigos continuam em `docs/`. Eles descrevem entregas anteriores e não substituem este fluxo.
