import 'modelos.dart';

abstract class Sessao {
  Future<String?> ler();
  Future<void> salvar(String token);
  Future<String?> lerNome();
  Future<void> salvarNome(String nome);
  Future<void> limpar();
}

class SessaoMemoria implements Sessao {
  String? token;
  String? nome;

  @override
  Future<void> limpar() async {
    token = null;
    nome = null;
  }

  @override
  Future<String?> ler() async => token;

  @override
  Future<String?> lerNome() async => nome;

  @override
  Future<void> salvar(String token) async => this.token = token;

  @override
  Future<void> salvarNome(String nome) async => this.nome = nome;
}

abstract class BalcaoApi {
  String get nome;
  String get base;
  Future<bool> restaurar();
  Future<void> entrar(String identificador, String senha);
  Future<void> sair();
  Future<List<Loja>> lojas();
  Future<List<Campanha>> campanhas(String lojaId);
  Future<ClienteBalcao> cliente(String lojaId, String participacaoId);
  Future<ResultadoCarimbo> carimbar(String lojaId, String participacaoId, List<String> campanhas);
  Future<ResultadoCarimbo> atenderWhatsapp(String lojaId, String whatsapp, String nome);
  Future<RecompensaBalcao> recompensa(String lojaId, String recompensaId);
  Future<void> entregar(String lojaId, String recompensaId);
}

class FalhaApi implements Exception {
  FalhaApi(this.mensagem);
  final String mensagem;
  @override
  String toString() => mensagem;
}

class ApiMemoria implements BalcaoApi {
  ApiMemoria({this.sessao});

  final Sessao? sessao;
  bool clientePuro = false;
  bool pedirNome = false;
  bool dentro = false;
  String? participacaoLida;
  List<String> campanhasCarimbadas = const [];
  String? whatsappAtendido;
  String? nomeInformado;
  String? recompensaEntregue;
  int saldo = 1;

  @override
  String nome = '';

  @override
  String get base => 'http://localhost:8081';

  @override
  Future<bool> restaurar() async {
    final token = await sessao?.ler();
    nome = await sessao?.lerNome() ?? '';
    dentro = token != null && token.isNotEmpty;
    return dentro;
  }

  @override
  Future<void> entrar(String identificador, String senha) async {
    if (clientePuro) throw FalhaApi('Esta conta é de cliente. O aplicativo do balcão é da barbearia.');
    if (senha.length < 12) throw FalhaApi('Senha inválida');
    nome = 'Atendente';
    dentro = true;
    await sessao?.salvar('token-teste');
    await sessao?.salvarNome(nome);
  }

  @override
  Future<void> sair() async {
    dentro = false;
    nome = '';
    await sessao?.limpar();
  }

  @override
  Future<List<Loja>> lojas() async => const [
        Loja(id: 'loja-1', nome: 'Barbearia Piloto', podeOperar: true),
      ];

  @override
  Future<List<Campanha>> campanhas(String lojaId) async => const [
        Campanha(id: 'camp-1', titulo: 'Cortes', meta: 5, recompensa: 'Um corte', ativa: true, principal: true),
      ];

  @override
  Future<ClienteBalcao> cliente(String lojaId, String participacaoId) async {
    participacaoLida = participacaoId;
    return ClienteBalcao(
      id: participacaoId,
      nome: 'João',
      whatsapp: '5581999990001',
      titulo: 'Cortes',
      recompensa: 'Um corte',
      saldo: saldo,
      meta: 5,
    );
  }

  @override
  Future<ResultadoCarimbo> carimbar(String lojaId, String participacaoId, List<String> campanhas) async {
    campanhasCarimbadas = campanhas;
    saldo += 1;
    return ResultadoCarimbo(nome: 'João', saldo: saldo, concluiu: saldo >= 5, cadastrado: false);
  }

  @override
  Future<ResultadoCarimbo> atenderWhatsapp(String lojaId, String whatsapp, String nome) async {
    if (pedirNome && nome.trim().isEmpty) throw FalhaApi('Informe o nome do cliente');
    whatsappAtendido = whatsapp;
    nomeInformado = nome;
    saldo = 1;
    return ResultadoCarimbo(nome: nome.trim().isEmpty ? 'João' : nome.trim(), saldo: saldo, concluiu: false, cadastrado: pedirNome);
  }

  @override
  Future<RecompensaBalcao> recompensa(String lojaId, String recompensaId) async =>
      RecompensaBalcao(id: recompensaId, descricao: 'Um corte', status: recompensaEntregue == recompensaId ? 'RESGATADA' : 'DISPONIVEL');

  @override
  Future<void> entregar(String lojaId, String recompensaId) async {
    recompensaEntregue = recompensaId;
  }
}
