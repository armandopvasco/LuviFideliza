# LUVI Fideliza+ — Painel integrado 0.6.0

Esta versão reúne o backend e as telas no mesmo projeto Spring Boot. Tudo continua local, na porta 8081, usando seu PostgreSQL e os dados já cadastrados.

## Atualizar no IntelliJ

1. Pare a aplicação e faça uma cópia do projeto atual. Faça também um backup do banco antes de atualizar sua estrutura.
2. Extraia este ZIP em uma pasta nova. Abra o `pom.xml` no IntelliJ como projeto Maven e aguarde a importação.
3. Copie para a configuração de execução as mesmas variáveis de ambiente do seu projeto anterior: `LUVI_FIDELIZA_DB_URL`, `LUVI_FIDELIZA_DB_USER` e `LUVI_FIDELIZA_DB_PASSWORD`. Preserve `LUVI_FIDELIZA_PORT`, se o utilizava. Mantenha `LUVI_FIDELIZA_BOOTSTRAP_ENABLED=false` — seu administrador já existe.
4. Execute `LuviFidelizaApplication`. Na primeira inicialização, o Flyway executa **V6__convites_equipe.sql**, que adiciona a tabela de convites da equipe. As migrações V1 a V5 foram preservadas. Não exclua tabelas nem a tabela de histórico do Flyway.
5. Abra **http://localhost:8081/painel.html**, pressione **Ctrl+F5** e entre com o proprietário já cadastrado.

Não é necessário instalar Node, React, ferramentas de frontend ou editar HTML/JavaScript para usar o painel. O ZIP contém o `pom.xml` e toda a pasta `src`, inclusive testes. Não contém `target`, configurações pessoais do IntelliJ ou credenciais.

Se preferir manter seu projeto atual, substitua o `pom.xml` e mescle a pasta `src` deste pacote sobre ela, com a aplicação parada. A opção de abrir uma pasta nova facilita comparar e preservar configurações locais. O antigo `painel-funcoes.js` pode permanecer no disco; a nova tela usa `painel.js` e `painel.css` e não carrega o complemento antigo.

## O que você encontra

| Área | Recursos |
| --- | --- |
| Início | Totais reais de participantes, contas vinculadas, cartões em andamento e recompensas disponíveis/resgatadas; programa vigente e atalhos. |
| Clientes | Busca por nome, paginação, cadastro, cartão com progresso, registro de compra, recompensas, cartões anteriores, histórico de compras e convite do cliente. |
| Recompensas | Consulta de todo o estabelecimento, filtros por status, paginação, acesso ao cliente e confirmação de entrega do benefício. |
| Programa | Criação/edição pelo proprietário, prévia do cartão e histórico. Conflito de versão mantém o rascunho e mostra aviso junto ao formulário. |
| Equipe | Convite por e-mail, listagem de funcionários, ativação/desativação e revogação de convites pendentes. Somente o proprietário acessa essa área. |
| Conta do cliente | Página web para entrar/cadastrar, aceitar convite e acompanhar cartões e recompensas. Continua sendo a interface provisória antes do aplicativo nativo. |

O visual usa creme, verde suave e rosa queimado. O nome LUVI foi aplicado como identificação tipográfica; os arquivos de logomarca aprovados não estavam no projeto enviado e não foram substituídos por uma nova marca.

## Roteiro curto de testes

### 1. Proprietário: operação completa

- Entre e confira os números de **Início**. Eles representam todo o estabelecimento, sem dados fictícios pré-carregados.
- Em **Clientes**, busque um participante existente ou clique em **Novo cliente**. O cadastro abre o primeiro cartão.
- Selecione o cliente, informe a referência de uma compra válida e registre **+ Carimbo**. Ao completar a meta, confira a recompensa.
- Abra **Recompensas**, confirme a entrega de um benefício e depois filtre por **Resgatadas**. Atualize **Início** para conferir os totais.
- Em **Programa**, altere um texto e salve. Consulte o histórico. O teste em duas abas continua válido: a segunda aba deve bloquear a versão antiga e preservar seu rascunho. Agora o aviso fica junto ao formulário e em uma notificação visível.

### 2. Funcionário: convite pela tela

1. Entre como proprietário e abra **Equipe**.
2. Informe um e-mail diferente do proprietário e gere o convite. Uma conta já existente também pode aceitar, desde que use o mesmo e-mail convidado.
3. Copie o link, abra em janela anônima/privativa e cole. Entre ou crie a conta com o **mesmo e-mail informado no convite**.
4. Confirme o convite de funcionário e clique em **Abrir painel do estabelecimento**.
5. Esse acesso deve conseguir consultar clientes, cadastrar participantes, registrar compras e confirmar entregas. Em **Programa**, os campos e o botão de salvar ficam desabilitados. **Equipe** não aparece no menu do funcionário.
6. Na janela do proprietário, atualize a equipe e desative o funcionário. Uma nova requisição da sessão desse funcionário deve ser recusada pelo servidor, mesmo se sua tela ainda estiver aberta. O proprietário pode reativá-lo.

Gerar um novo convite para o mesmo e-mail invalida o anterior. Os convites vencem em 48 horas. O proprietário também pode revogar um convite pendente. Um convite já aceito não pode ser reutilizado para contornar a desativação de um funcionário. O proprietário não pode desativar seu próprio acesso por essa tela.

Funcionários vinculados manualmente no teste anterior continuam aparecendo na equipe normalmente. Nenhum novo comando SQL é necessário.

### 3. Cliente: convite do cartão

1. No painel, abra um cliente ainda sem conta vinculada e vá à aba **Convite**.
2. Gere o convite e abra o link em uma janela anônima separada da conta do proprietário/funcionário.
3. Entre ou crie uma conta de cliente, confira o e-mail exibido e confirme o vínculo.
4. Confira os cartões e benefícios. O cliente não tem botões para adicionar carimbos ou registrar resgates.

O convite da equipe e o convite do cartão são diferentes. O primeiro concede acesso de funcionário ao estabelecimento; o segundo vincula um participante à carteira do cliente. Ambos exigem aceite explícito. Compartilhe cada link somente com a pessoa indicada.

## Reenvios e permissões

- Cadastro de participante, carimbo e resgate mantêm uma chave por operação no navegador. Se a resposta se perder, reenvie a operação pendente; o servidor impede duplicar o mesmo lançamento.
- Ao existir um cadastro/carimbo sem confirmação, o formulário mantém os dados e orienta o reenvio. Reenvios pendentes da versão anterior do painel são reconhecidos no mesmo navegador e na mesma aba.
- As permissões são verificadas no backend. Administrador sem vínculo continua com acesso de consulta; não edita regras, não opera cartões e não gerencia equipe.
- As senhas são criadas pelo próprio usuário e continuam usando o fluxo atual, com 12 a 72 caracteres ASCII nesta fase de testes.

## Validação desta entrega

- **55 testes Java passaram**, incluindo permissões, equipe, busca literal por nome, resumo, emissão/resgate e isolamento entre estabelecimentos.
- Teste no Chromium com o backend real em execução e banco temporário H2: navegação, busca/paginação, cartão, convite/cadastro de cliente, conflito entre abas, convite/cadastro de funcionário, bloqueio de edição do funcionário, filtro e resgate de recompensas.
- Uma perda de resposta foi simulada após a gravação de uma compra. O reenvio usou a mesma chave e não duplicou o carimbo.
- As cinco telas foram verificadas em 390 px sem transbordamento horizontal, além da revisão visual em desktop.
- O PostgreSQL do seu computador não foi acessado aqui. A migração V6 e a integração com seus dados devem ser conferidas ao iniciar localmente.

## Limites atuais

O domínio e a hospedagem não foram alterados. Não há envio automático de convites por WhatsApp ou e-mail, recuperação de senha ou verificação de e-mail implementados nesta entrega. Os links são copiados manualmente. No celular, `localhost` aponta para o próprio aparelho: nesta etapa use o navegador no computador da aplicação.

O painel possui paginação de clientes, cartões, compras e recompensas. A página provisória do cliente mostra até 100 cartões e 100 recompensas, avisando quando houver mais. A listagem de estabelecimentos mantém o limite anterior de 100. Equipes e convites são exibidos em lista simples, adequada ao piloto com poucos funcionários.

Para executar os testes Java no IntelliJ, abra a janela Maven e execute **Lifecycle → test**, ou use `mvn test` na pasta do projeto.
