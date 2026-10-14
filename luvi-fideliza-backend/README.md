# LUVI Fideliza+ — backend 0.2

Java 17, Spring Boot 3.5.6, PostgreSQL, Maven e Flyway.
Esta entrega acrescenta login por sessão, bootstrap local do administrador,
cadastro de estabelecimento, convite de proprietário e consulta com isolamento.
Ainda não tem telas, carteiras, carimbos, cobrança, e-mail ou app Flutter.

## Atualizar seu projeto local
1. Pare a aplicação. Faça uma cópia da pasta atual ou um commit local.
2. Extraia este ZIP em uma nova pasta e abra o pom.xml no IntelliJ.
3. Mantenha o mesmo banco luvi_fideliza e copie apenas suas variáveis de ambiente.
4. Não apague tabelas nem altere V1. V1 é idêntica à primeira entrega.
   Flyway executará somente V2, que cria convite_proprietario.
5. Recarregue o Maven. Esta versão já inclui as correções para Spring Boot 3.5.6.

## Primeiro administrador local
Na configuração de execução do IntelliJ, configure:

- LUVI_FIDELIZA_DB_URL=jdbc:postgresql://localhost:5432/luvi_fideliza
- LUVI_FIDELIZA_DB_USER=luvi_fideliza_app
- LUVI_FIDELIZA_DB_PASSWORD=sua senha do banco
- SPRING_PROFILES_ACTIVE=local
- LUVI_FIDELIZA_BOOTSTRAP_ENABLED=true
- LUVI_FIDELIZA_ADMIN_EMAIL=seu e-mail
- LUVI_FIDELIZA_ADMIN_PASSWORD=senha forte de 12 a 72 caracteres ASCII

Execute LuviFidelizaApplication. O administrador é criado apenas se ainda não
existe um administrador no banco. Não há senha padrão e não se promove um
usuário já existente. Depois da primeira inicialização, remova as duas
variáveis ADMIN e configure BOOTSTRAP_ENABLED=false. O login passa a usar o
hash salvo no banco. A senha não pode ser recuperada por esta versão.

As contas ainda ficam com email_verificado=false. O piloto local faz entrega
manual do convite; verificação por e-mail, recuperação, limitação de tentativas
e auditoria completa devem ser implementadas antes de acesso público.

## Testar pelo Postman
Importe docs/LUVI_Fideliza_0.2.postman_collection.json.
O Postman deve preservar os cookies de localhost (JSESSIONID).
As variáveis da coleção não contêm credenciais reais. Preencha admin_email e
admin_password localmente. Nunca publique a coleção preenchida com segredos.

1. Execute **01 CSRF**. O script da coleção salva o token automaticamente.
2. Execute **02 Login**. Body é x-www-form-urlencoded, com email e password.
   Esperado: 204. Não é login JSON nem autenticação Basic/Bearer.
3. Execute **01 CSRF novamente** após login: o token antigo foi invalidado.
4. Execute **03 Meu acesso**: deve retornar o e-mail e ROLE_ADMIN.
5. Execute **04 Cadastrar estabelecimento**: retorna 201 e salva id na coleção.
6. Configure owner_email com outro e-mail, ainda não cadastrado. Execute
   **05 Convidar proprietário**: retorna um token de uso único válido por 48h.
   Esse token equivale a um segredo: entregue só ao destinatário combinado.
7. Execute **06 Logout**, depois **01 CSRF**.
8. Configure owner_name e owner_password. Execute **07 Aceitar convite**.
   Esperado: 201. Não inicia sessão automaticamente. Repetir o convite retorna 400.
9. Execute **08 Login proprietário**, **01 CSRF** e **09 Consultar estabelecimento**.
   Esperado: 200. Use 04 com o proprietário: deve retornar 403.
10. Para testar isolamento, cadastre um segundo negócio com o administrador,
    depois consulte seu ID com a sessão do primeiro proprietário: deve retornar 403.

Todas as operações POST exigem X-CSRF-TOKEN e o cookie da mesma sessão.
401 significa falta de login ou credenciais inválidas; 403 significa falta de
permissão ou CSRF; 400 indica dados ou convite inválidos; 409 indica cadastro
em conflito. Se receber 403 após login/logout, execute 01 CSRF de novo.

A criação aceita somente contas novas de proprietário. Vincular contas
existentes, gerir funcionários e reemitir/revogar convites ficam para a próxima
entrega. O convite não atribui administrador da plataforma.

## Build e testes
```bash
mvn clean package
```
Os testes de integração desta entrega usam H2 para verificar login, sessão,
CSRF, permissões, isolamento e convite. Não substituem a validação das migrações
em PostgreSQL, nem uma prova de concorrência no PostgreSQL. O V2 usa bloqueio
pessimista no consumo do convite, dentro da transação.

Endereços futuros: fideliza.luvi.net.br, /admin e /api/v1.
A aplicação continua local na porta 8081. Não foi publicada.
