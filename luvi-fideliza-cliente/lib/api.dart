import 'dart:typed_data';

import 'modelos.dart';

abstract class Sessao {
  Future<String?> ler();
  Future<void> salvar(String token);
  Future<String?> lerNome();
  Future<void> salvarNome(String nome);
  Future<void> limpar();
}

class SessaoMemoria implements Sessao {
  String? valor;
  String? nome;
  @override
  Future<void> limpar() async {
    valor = null;
    nome = null;
  }
  @override
  Future<String?> ler() async => valor;
  @override
  Future<String?> lerNome() async => nome;
  @override
  Future<void> salvar(String token) async => valor = token;
  @override
  Future<void> salvarNome(String nome) async => this.nome = nome;
}

abstract class LuviApi {
  String get nome;
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
  Future<VisualCartao> visual(String lojaId);
  Future<List<int>?> fundo(String lojaId);
  Future<List<RecompensaCliente>> recompensas();
  Future<Uint8List> pngQrParticipacao(String participacaoId);
  Future<Uint8List> pngQrRecompensa(String recompensaId);
  Future<void> sair();
}

class ApiMemoria implements LuviApi {
  ApiMemoria({this.sessao});
  final Sessao? sessao;
  bool dentro = false;
  String? loja;
  @override
  String nome = '';
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
  Future<VisualCartao> visual(String lojaId) async => const VisualCartao(icone: 'tesoura');

  @override
  Future<List<int>?> fundo(String lojaId) async => null;

  @override
  Future<void> entrar(String identificador, String senha) async {
    if (senha.length < 12) throw Exception('Senha inválida');
    dentro = true;
    nome = 'Cliente teste';
    await sessao?.salvar('token-teste');
    await sessao?.salvarNome(nome);
  }

  @override
  Future<List<RecompensaCliente>> recompensas() async => const [];

  @override
  Future<bool> restaurar() async {
    final token = await sessao?.ler();
    nome = await sessao?.lerNome() ?? '';
    dentro = token != null && token.isNotEmpty;
    return dentro;
  }

  @override
  Future<void> sair() async {
    dentro = false;
    nome = '';
    await sessao?.limpar();
  }

  @override
  Future<Uint8List> pngQrParticipacao(String participacaoId) async => _pngDemonstracao;

  @override
  Future<Uint8List> pngQrRecompensa(String recompensaId) async => _pngDemonstracao;
}

final _pngDemonstracao = Uint8List.fromList(const [
  137, 80, 78, 71, 13, 10, 26, 10, 0, 0, 0, 13, 73, 72, 68, 82, 0, 0, 0, 1, 0, 0, 0, 1, 8, 6, 0, 0, 0, 31, 21, 196, 137,
  0, 0, 0, 13, 73, 68, 65, 84, 120, 218, 99, 252, 207, 192, 80, 15, 0, 4, 133, 1, 128, 132, 169, 140, 33, 0, 0, 0, 0, 73,
  69, 78, 68, 174, 66, 96, 130,
]);
