# LUVI Fideliza+ v0.9.1 — Roteiro de teste ponta a ponta

## Preparação

1. Faça backup do banco de teste (`pg_dump -U SEU_USUARIO -d SEU_BANCO -Fc -f backup_antes_limpeza.dump`).
2. Pare a aplicação para evitar gravações durante a limpeza.
3. Confirme o banco e o schema (`SELECT current_database(), current_schema();`). O script usa `public`.
4. Consulte o ADM: `SELECT id, nome, email, administrador_plataforma FROM public.usuario WHERE administrador_plataforma IS TRUE;`.
5. Abra `luvi_fideliza_limpeza_teste_v091.sql`, substitua `SUBSTITUA_PELO_EMAIL_DO_ADM@EXEMPLO.COM` pelo e-mail do seu ADM e execute **o arquivo inteiro** no banco de testes.
6. Confirme que resta somente o ADM e que a tabela `flyway_schema_history` permanece íntegra. Inicie a aplicação.

> O script exclui dados de todas as tabelas de negócio no schema `public` exceto `usuario`, e depois remove os usuários que não sejam o ADM. Ele preserva a estrutura e as migrações, mas apaga planos e configurações comerciais também. Não execute em produção.

## Dados fictícios sugeridos

- Estabelecimento: **Cafeteria Teste LUVI**.
- Proprietário: **Proprietário Teste**, usando um número de WhatsApp de teste controlado por você.
- Funcionário: **Atendente Teste**, usando um contato diferente.
- Cliente: **Cliente Teste**, usando outro telefone ou e-mail que você controle.
- Campanha: **Ganhe 1 café** após **3 carimbos** (facilita o teste; pode usar 10 se preferir).
- Recompensa: **1 café à moda da casa**.

## Cenário A — Administrador e estabelecimento

- [ ] Entrar no painel ADM com a conta preservada.
- [ ] Cadastrar a Cafeteria Teste LUVI.
- [ ] Verificar que o estabelecimento aparece na administração.
- [ ] Informar o telefone do proprietário e gerar convite.
- [ ] Confirmar que o WhatsApp abre com **destinatário correto**, texto e link individual preenchidos.
- [ ] Confirmar manualmente o envio no WhatsApp (o LUVI não envia sozinho).
- [ ] Repetir em outro ambiente com e-mail, verificando que o aplicativo de e-mail abre com mensagem pronta.

## Cenário B — Proprietário e funcionário

- [ ] Abrir o convite de proprietário em navegador anônimo.
- [ ] Criar conta/aceitar convite e entrar no painel da loja.
- [ ] Verificar acesso somente ao estabelecimento convidado.
- [ ] Criar/configurar campanha de 3 carimbos e prêmio de 1 café.
- [ ] Personalizar o cartão (cores, ícone e, se desejado, fotografia de fundo).
- [ ] Convidar funcionário por WhatsApp, conferindo destinatário e link.
- [ ] Aceitar convite com conta de funcionário em navegador anônimo diferente.
- [ ] Verificar que o funcionário não tem poderes de proprietário/ADM.

## Cenário C — Adesão pública do cliente

- [ ] No painel, localizar o link público da cafeteria.
- [ ] Gerar o QR Code e testar com a câmera do celular.
- [ ] Confirmar que o QR Code abre a página da cafeteria correta.
- [ ] Sem estar logado, cadastrar o cliente com **somente telefone**.
- [ ] Verificar que o cliente foi vinculado à cafeteria e possui cartão da campanha.
- [ ] Confirmar que o cartão começa com **zero carimbos**.
- [ ] Sair e entrar novamente com o telefone e a senha.
- [ ] Testar também um segundo cliente com **somente e-mail**.
- [ ] Escanear novamente o mesmo QR Code com o cliente já vinculado; confirmar que não cria vínculo/cartão duplicado.

## Cenário D — Carimbos e recompensa

- [ ] Entrar como funcionário e localizar o cliente.
- [ ] Registrar o primeiro carimbo; cliente deve ver 1/3.
- [ ] Registrar o segundo carimbo; cliente deve ver 2/3.
- [ ] Registrar o terceiro carimbo; cliente deve ver 3/3 e **Prêmio disponível**.
- [ ] Verificar a faixa de parabéns na visão do cliente.
- [ ] Registrar a entrega do prêmio no painel.
- [ ] Conferir que a recompensa passa a **Resgatada/Entregue**, sem novo resgate duplicado.
- [ ] Consultar o histórico do cliente: benefício, data/hora e responsável pela entrega.
- [ ] Conferir se a nova rodada de carimbos inicia corretamente, caso essa seja a regra configurada.

## Cenário E — Segurança e regressão

- [ ] Convite já aceito não deve conceder acesso novamente.
- [ ] Convite vencido não deve ser aceito.
- [ ] Funcionário não deve acessar administração da plataforma.
- [ ] QR Code público não deve conceder carimbos ou privilégios administrativos.
- [ ] Telefone/e-mail de outro usuário não deve permitir acesso sem senha.
- [ ] Testar visualização dos cartões no computador e no celular, com atualização de página.

## Registro de resultados

Anote para cada falha: etapa, conta/perfil usado, URL da tela, ação executada, resultado esperado, resultado obtido, mensagem no console e horário aproximado. Não compartilhe senhas nem tokens de convite ativos em capturas.
