# Testar participantes e carimbos na versão 0.4

## Atualização local
1. Pare a aplicação e guarde uma cópia ou commit da 0.3.
2. Abra o projeto 0.4 em outra pasta no IntelliJ.
3. Copie as variáveis do mesmo banco luvi_fideliza. Mantenha BOOTSTRAP_ENABLED=false.
4. Recarregue Maven e execute. Flyway aplicará V4; não apague tabelas ou dados.
5. Confira /actuator/health: UP. V1, V2 e V3 foram preservadas.
6. Novas tabelas: participacao, cartao_fidelidade e movimentacao_carimbo.

## Preparar Postman
Importe docs/LUVI_Fideliza_0.4.postman_collection.json como coleção nova.
Não precisa repetir as requisições de criação de estabelecimento, proprietário e programa.
A coleção mantém os itens 01 a 15 como referência e adiciona 16 a 30.
Configure as variáveis da NOVA coleção:
- base_url: http://localhost:8081
- owner_email e owner_password: proprietário existente
- estabelecimento_id: ID da barbearia, não o da cafeteria
- cliente_nome, cliente_email, cliente_password: NOVA conta de consumidor para os testes

O consumidor não precisa ser outra pessoa real: use outro e-mail de teste local.
A senha tem de 12 a 72 caracteres ASCII. Não envie senhas na conversa nem compartilhe
uma coleção com credenciais preenchidas. Cadastro de consumidor não faz login automático.
A autenticação de proprietário e cliente usa o mesmo endpoint, com contas diferentes.
O perfil do consumidor sem vínculo de equipe aparece como ROLE_CLIENTE.

Se não achar Variables, podemos orientar pela sua tela. Não precisa criar novas requisições:
esta coleção já tem URLs, scripts e corpos preparados.

## Parte A — participante e carimbos pelo comerciante
1. 06 Logout (204), 08 Login proprietário (204), 03 Meu acesso (ROLE_OPERADOR).
2. 16 Cadastrar participante e abrir cartão: 201. Salva participante_id e cartao_id automaticamente.
   Body: nomeExibicao é o nome local usado no balcão. Não dá acesso à conta global do cliente.
3. 17 Repetir cadastro: 200. Retorna o mesmo participante, sem criar outro cartão.
4. 18 Consultar cartões: saldo 0, ciclo 1, regras iguais à versão atual na abertura.
5. 19 Registrar primeiro carimbo: 201, saldoApos 1.
6. 20 Repetir primeiro carimbo: 200, mesmo ID e saldoApos 1. Não incrementa.
7. 21 Registrar outra compra: 201. SaldoApos 2.
8. 22 Histórico: total 2. Repetições não contam como novas movimentações.
9. 18 novamente: deve mostrar saldo 2 (ou ciclo concluído se a meta era 2).

A meta depende do programa existente. Se você terminou a 0.3 com meta 8, o cartão
fica em andamento com 2 de 8; não é preciso alterar o programa para este teste.

## Parte B — cliente com sua própria carteira
1. 23 Gerar convite: 200. Guarda convite_cliente na coleção. Token secreto, válido por 48h.
   Gerar outro convite invalida o anterior. Não há envio de e-mail ou WhatsApp nesta versão.
2. 24 Sair do proprietário: 204.
3. 25 Cadastrar conta do cliente: 201. Se já tem conta de consumidor, pule este cadastro.
   409 significa e-mail já cadastrado; não precisa criar nova conta a cada estabelecimento.
4. 26 Login cliente: 204. 03 Meu acesso deve mostrar ROLE_CLIENTE (se não for membro de equipe).
5. 27 Aceitar convite: 200 e vinculadoAoCliente=true. A conta assume os cartões desse participante.
6. 28 Minha carteira: 200, com o cartão e o saldo. O cliente vê apenas os próprios cartões.
7. 29 Cliente não pode registrar carimbo: 403.
8. 30 Listar participantes: 403 para esse cliente sem vínculo com a equipe.

Para registrar mais compras, faça 06 Logout e 08 Login proprietário.
CSRF é atualizado automaticamente em cada gravação, como na coleção 0.3.

## Chaves de idempotência
Os cadastros e os carimbos usam o header Idempotency-Key com UUID.
O script gera e guarda três chaves: cadastro_chave, carimbo_chave e nova_compra_chave.
Repetir a mesma operação com a mesma chave devolve 200 e o resultado já registrado.
Mudar o nome ou a compra mantendo a chave gera 409. Uma chave de carimbo também
não pode ser reaproveitada para outro participante no mesmo estabelecimento.
Cada compra nova precisa de chave nova; repetir após falha de rede precisa da mesma chave.

Para testar uma TERCEIRA compra no item 21:
- Troque referenciaCompra para COMPRA-PILOTO-003.
- Apague o valor de nova_compra_chave nas variáveis e salve.
- O script gerará uma chave nova. Envie 21: 201.
- Reenviar sem alterar nada retorna 200, sem duplicar.

Não use {{$guid}} diretamente no header: ele mudaria a cada envio e quebraria o retry seguro.
referenciaCompra é um registro informado pelo comerciante, sem integração com caixa ou pagamento.
Idempotência protege a repetição técnica da operação, não comprova que a compra ocorreu.
Reutilizar a mesma descrição com uma chave diferente representa uma nova operação.

## Completar um ciclo e preservar as regras
Cada operação credita UM carimbo. Não há parâmetro de quantidade.
Ao atingir a meta, o cartão fica CONCLUIDO, com sua recompensa e versão preservadas.
A próxima compra abre o ciclo seguinte usando a versão atual do programa e credita o primeiro carimbo.
Uma mudança de meta não muda cartões já abertos. O ciclo seguinte poderá usar a meta nova.
O cartão concluído ainda NÃO é um resgate: emissão e validação de recompensa serão a próxima entrega.
Não há estorno, exclusão ou reinício manual nesta versão, para não apagar o histórico.

## Testar privacidade e múltiplos estabelecimentos
- O cliente nunca informa um usuario_id para se apropriar de um cartão: precisa aceitar o convite secreto.
- Aceitar o mesmo convite pela mesma conta é seguro; outra conta não consegue tomar o vínculo.
- A mesma conta pode aceitar convites de negócios diferentes. Minha carteira reúne os cartões.
- Cada negócio só consulta seus participantes, sem conhecer onde mais o cliente participa.
- Não vincule a mesma conta a dois participantes do mesmo programa: recebe 409.
  Junção/correção de cadastros duplicados está pendente; não credite cartões duplicados.
- Funcionários ativos também podem cadastrar, convidar e registrar compras. Não alteram regras do programa.
- Administrador sem vínculo de equipe pode consultar para suporte, mas não operar carimbos.

As listagens são paginadas: ?pagina=0&tamanho=20, máximo 100 por página.
Retornam conteudo, pagina, tamanho e total.
A resposta de participantes não expõe senha, e-mail global ou token do convite.

## Limites desta entrega
Backend local, sem aplicativo Flutter ou telas web ainda.
Verificação de e-mail, recuperação de senha, limites de login/cadastro, regras de assinatura
comercial e auditoria geral continuam pendentes antes do piloto público.
O convite funciona como segredo de posse; compartilhe só com o cliente correto.
Registros e testes locais não equivalem a prontidão para publicação.
