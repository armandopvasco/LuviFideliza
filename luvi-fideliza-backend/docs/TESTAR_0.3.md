# Testar programa de fidelidade local

## Atualizar o projeto
1. Pare a aplicação e guarde uma cópia ou commit da versão 0.2.
2. Extraia o ZIP 0.3 em outra pasta e abra o pom.xml no IntelliJ.
3. Copie sua configuração de variáveis do banco. O banco continua luvi_fideliza.
4. Mantenha LUVI_FIDELIZA_BOOTSTRAP_ENABLED=false. O administrador e o proprietário existentes continuam válidos.
5. Recarregue o Maven e execute LuviFidelizaApplication.
6. O Flyway executará V3. Não apague tabelas nem edite V1 ou V2.
   As novas tabelas são programa_fidelidade e versao_programa.
7. Confira http://localhost:8081/actuator/health: deve retornar UP.

## Preparar o Postman
Importe docs/LUVI_Fideliza_0.3.postman_collection.json como uma nova coleção.
Não é necessário refazer o cadastro da barbearia ou o convite da versão 0.2.
Na aba Variables da NOVA coleção, configure os valores locais:

- base_url: http://localhost:8081
- owner_email: e-mail do proprietário já criado (não o administrador)
- owner_password: senha definida ao aceitar o convite
- estabelecimento_id: ID da barbearia à qual o proprietário está vinculado

Copie o ID da coleção anterior ou consulte no banco:
SELECT id, nome FROM estabelecimento;
O script desta coleção busca automaticamente o CSRF antes dos POST e PUT,
inclusive login e logout. Não precisa copiar tokens manualmente.
Mantenha cookies habilitados no Postman e use localhost em todas as requisições.
Se o Postman não executar os scripts, use 01 CSRF e o token em X-CSRF-TOKEN,
como fizemos na versão anterior.

## Primeiro teste com o proprietário existente
1. Execute 06 Logout para encerrar qualquer sessão antiga: 204.
2. Execute 08 Login proprietário: 204.
3. Execute 03 Meu acesso: deve mostrar o e-mail do proprietário e ROLE_OPERADOR.
4. Execute 10 Criar programa: 201. Em Body já há um exemplo com meta 10,
   recompensa de um corte grátis e critérios da compra. Pode editar os textos.
5. Execute 11 Consultar programa atual: 200, versao 1 e metaCarimbos 10.
6. Execute 12 Atualizar regras: 200. O exemplo altera a meta para 8.
7. Execute 13 Histórico: deve listar versão 2 (meta 8) e versão 1 (meta 10).
8. Execute 14: 409, pois há um programa por estabelecimento.
9. Execute 15: 409, pois tenta salvar sobre a versão 1 já substituída.

A coleção salva programa_versao após criar, consultar ou atualizar.
Em uso real, consulte o programa antes de editar. Em caso de 409 por versão
antiga, consulte novamente e revise as mudanças antes de salvar. Não reenvie
automaticamente uma edição antiga com a nova versão.
Se repetir 10 depois de cadastrar, 409 é esperado; use 11 e 12.
Se salvar os mesmos valores normalizados, retorna a versão atual sem gerar histórico adicional.

## Conferir permissões
- Proprietário do próprio negócio: cria, consulta, altera e consulta histórico.
- Funcionário vinculado: consulta programa e histórico; não pode editar.
- Proprietário de outro negócio: recebe 403 em todos os endpoints do programa.
- Administrador da plataforma: consulta; só edita se também for proprietário daquele negócio.
- Sem login: GET retorna 401. POST/PUT sem CSRF podem retornar 403 antes do login.

Para repetir o teste de isolamento já conhecido, troque temporariamente
estabelecimento_id pelo ID da cafeteria. Consulte 11: esperado 403. Restaure o ID.
O CRUD de funcionários continua pendente; a permissão foi coberta nos testes automatizados.

## Contrato dos campos
- titulo: obrigatório, até 120 caracteres.
- metaCarimbos: inteiro obrigatório de 1 a 1000; não aceita frações.
- recompensa: obrigatória, até 200 caracteres.
- criteriosCompra: obrigatórios, até 500 caracteres. Explicam quando vale um carimbo.
- versaoAtual: obrigatório somente no PUT; é a versao que você consultou antes de editar.
Espaços nas pontas dos textos são removidos. Textos só com espaços são inválidos.
O histórico retorna as versões mais recentes primeiro, com autor (ID) e data UTC.

## O que ainda não faz
Não cria cartões nem credita carimbos. Não emite recompensa nem envia push.
Estas regras serão a referência para a próxima entrega de participação/cartões.
A versão é preservada para futuros ciclos; ainda não há cartões existentes a migrar.
Não há expiração automática das recompensas nem pausas do programa nesta etapa.

## Diagnóstico rápido
401: sessão ausente ou credenciais incorretas. Verifique 03 Meu acesso.
403: vínculo/perfil sem permissão ou CSRF. Confirme a sessão, o ID e a execução dos scripts.
404: negócio inativo/ausente ou programa ainda não criado.
400: dados inválidos; confira campos na resposta.
409: programa já existe ou edição desatualizada.

A aplicação continua local na porta 8081. Nenhum domínio ou servidor foi alterado.
