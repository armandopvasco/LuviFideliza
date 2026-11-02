import 'modelos.dart';

abstract class Sessao {
  Future<String?> ler();
  Future<void> salvar(String token);
  Future<void> limpar();
}

class SessaoMemoria implements Sessao {
  String? valor;
  @override
  Future<void> limpar() async => valor = null;
  @override
  Future<String?> ler() async => valor;
  @override
  Future<void> salvar(String token) async => valor = token;
}

abstract class LuviApi {
  Future<bool> restaurar();
  Future<void> entrar(String identificador, String senha);
  Future<void> cadastrar({
    required String nome,
    String? email,
    String? telefone,
    required String senha,
  });
  Future<void> aderir(String lojaId);
  Future<List<CartaoCliente>> cartoes();
  Future<List<RecompensaCliente>> recompensas();
  Future<String> urlQrParticipacao(String participacaoId);
  Future<String> urlQrRecompensa(String recompensaId);
  Future<void> sair();
}

class ApiMemoria implements LuviApi {
  ApiMemoria({this.sessao});
  final Sessao? sessao;
  bool dentro = false;
  String? loja;
  var cartoesFixos = const <CartaoCliente>[
    CartaoCliente(
      id: 'cartao-1',
      participacaoId: 'part-1',
      estabelecimentoId: 'loja-1',
      estabelecimento: 'Barbearia',
      titulo: 'Cortes',
      recompensa: 'Corte grátis',
      meta: 5,
      saldo: 2,
      status: 'EM_ANDAMENTO',
    ),
  ];

  @override
  Future<void> aderir(String lojaId) async => loja = lojaId;

  @override
  Future<void> cadastrar({
    required String nome,
    String? email,
    String? telefone,
    required String senha,
  }) async {}

  @override
  Future<List<CartaoCliente>> cartoes() async => cartoesFixos;

  @override
  Future<void> entrar(String identificador, String senha) async {
    if (senha.length < 12) throw Exception('Senha inválida');
    dentro = true;
    await sessao?.salvar('token-teste');
  }

  @override
  Future<List<RecompensaCliente>> recompensas() async => const [];

  @override
  Future<bool> restaurar() async {
    final token = await sessao?.ler();
    dentro = token != null && token.isNotEmpty;
    return dentro;
  }

  @override
  Future<void> sair() async {
    dentro = false;
    await sessao?.limpar();
  }

  @override
  Future<String> urlQrParticipacao(String participacaoId) async =>
      'http://localhost:8081/painel.html?loja=loja-1&participacao=$participacaoId';

  @override
  Future<String> urlQrRecompensa(String recompensaId) async =>
      'http://localhost:8081/painel.html?loja=loja-1&recompensa=$recompensaId';
}
