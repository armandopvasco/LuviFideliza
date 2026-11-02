import 'dart:convert';

import 'package:http/http.dart' as http;

import 'api.dart';
import 'modelos.dart';

class LuviHttp implements LuviApi {
  LuviHttp({required this.base, required this.sessao, http.Client? cliente})
      : cliente = cliente ?? http.Client();

  final String base;
  final Sessao sessao;
  final http.Client cliente;
  String? _token;
  String _nome = '';

  @override
  String get nome => _nome;

  String get _raiz => base.endsWith('/') ? base.substring(0, base.length - 1) : base;

  @override
  Future<bool> restaurar() async {
    _token = await sessao.ler();
    _nome = await sessao.lerNome() ?? '';
    return _token != null && _token!.isNotEmpty;
  }

  @override
  Future<void> entrar(String identificador, String senha) async {
    final dados = await _enviar('POST', '/api/v1/auth/token', {
      'identificador': identificador.trim(),
      'senha': senha,
    }, autenticado: false);
    _token = dados['token'] as String;
    final recebido = dados['nome'];
    _nome = recebido is String && recebido.trim().isNotEmpty ? recebido.trim() : identificador.trim();
    await sessao.salvar(_token!);
    await sessao.salvarNome(_nome);
  }

  @override
  Future<void> cadastrar({
    required String nome,
    String? email,
    String? telefone,
    required String senha,
  }) async {
    await _enviar('POST', '/api/v1/auth/clientes', {
      'nome': nome.trim(),
      if (email != null && email.trim().isNotEmpty) 'email': email.trim(),
      if (telefone != null && telefone.trim().isNotEmpty) 'telefone': telefone.trim(),
      'senha': senha,
    }, autenticado: false);
  }

  @override
  Future<void> aderir(String lojaId) async {
    await _enviar('POST', '/api/v1/me/estabelecimentos/$lojaId/aderir', const {});
  }

  @override
  Future<List<CartaoCliente>> cartoes() async {
    final dados = await _enviar('GET', '/api/v1/me/cartoes?tamanho=50', null);
    final lista = (dados['conteudo'] as List?) ?? const [];
    return lista.map((item) {
      final mapa = item as Map<String, dynamic>;
      return CartaoCliente(
        id: '${mapa['id']}',
        participacaoId: '${mapa['participacaoId']}',
        estabelecimentoId: '${mapa['estabelecimentoId']}',
        estabelecimento: '${mapa['estabelecimento'] ?? ''}',
        titulo: '${mapa['titulo'] ?? 'Cartão'}',
        recompensa: '${mapa['recompensa'] ?? ''}',
        meta: (mapa['metaCarimbos'] as num?)?.toInt() ?? 0,
        saldo: (mapa['saldo'] as num?)?.toInt() ?? 0,
        status: '${mapa['status'] ?? ''}',
      );
    }).toList();
  }

  @override
  Future<VisualCartao> visual(String lojaId) async {
    final dados = await _enviar('GET', '/api/v1/estabelecimentos/$lojaId/visual-cartao', null);
    return VisualCartao(
      corCartao: '${dados['corCartao'] ?? '#30475A'}',
      corCarimbo: '${dados['corCarimbo'] ?? '#87A18F'}',
      icone: '${dados['iconeCarimbo'] ?? 'estrela'}',
      temFoto: dados['fotoFundoCartao'] == true,
    );
  }

  @override
  Future<List<int>?> fundo(String lojaId) async {
    final uri = Uri.parse('$_raiz/api/v1/estabelecimentos/$lojaId/fundo-cartao');
    final resposta = await cliente.get(uri, headers: {
      'X-Luvi-App': '1',
      if (_token != null) 'Authorization': 'Bearer $_token',
    });
    if (resposta.statusCode != 200 || resposta.bodyBytes.isEmpty) return null;
    return resposta.bodyBytes;
  }

  @override
  Future<List<RecompensaCliente>> recompensas() async {
    final dados = await _enviar('GET', '/api/v1/me/recompensas?tamanho=100', null);
    final lista = (dados['conteudo'] as List?) ?? const [];
    return lista.map((item) {
      final mapa = item as Map<String, dynamic>;
      return RecompensaCliente(
        id: '${mapa['id']}',
        cartaoId: '${mapa['cartaoId']}',
        descricao: '${mapa['descricao'] ?? ''}',
        status: '${mapa['status'] ?? ''}',
      );
    }).toList();
  }

  @override
  Future<String> urlQrParticipacao(String participacaoId) async {
    final dados = await _enviar('GET', '/api/v1/me/participacoes/$participacaoId/qr', null);
    return dados['url'] as String;
  }

  @override
  Future<String> urlQrRecompensa(String recompensaId) async {
    final dados = await _enviar('GET', '/api/v1/me/recompensas/$recompensaId/qr', null);
    return dados['url'] as String;
  }

  @override
  Future<void> sair() async {
    if (_token != null) {
      try {
        await _enviar('DELETE', '/api/v1/auth/token', null);
      } catch (_) {}
    }
    _token = null;
    _nome = '';
    await sessao.limpar();
  }

  Future<Map<String, dynamic>> _enviar(
    String metodo,
    String caminho,
    Map<String, dynamic>? corpo, {
    bool autenticado = true,
  }) async {
    final uri = Uri.parse('$_raiz$caminho');
    final headers = {
      'Content-Type': 'application/json',
      'X-Luvi-App': '1',
      if (autenticado && _token != null) 'Authorization': 'Bearer $_token',
    };
    late http.Response resposta;
    switch (metodo) {
      case 'POST':
        resposta = await cliente.post(uri, headers: headers, body: jsonEncode(corpo ?? {}));
      case 'DELETE':
        resposta = await cliente.delete(uri, headers: headers);
      default:
        resposta = await cliente.get(uri, headers: headers);
    }
    if (resposta.statusCode >= 400) {
      String mensagem = 'Não foi possível concluir. Código ${resposta.statusCode}.';
      try {
        final corpoErro = jsonDecode(resposta.body);
        if (corpoErro is Map && corpoErro['mensagem'] is String) mensagem = corpoErro['mensagem'] as String;
      } catch (_) {}
      throw Exception(mensagem);
    }
    if (resposta.body.isEmpty) return {};
    final decodificado = jsonDecode(resposta.body);
    if (decodificado is Map<String, dynamic>) return decodificado;
    if (decodificado is Map) return decodificado.map((chave, valor) => MapEntry('$chave', valor));
    return {};
  }
}
