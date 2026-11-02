import 'dart:convert';
import 'dart:math';

import 'package:http/http.dart' as http;

import 'api.dart';
import 'modelos.dart';

class LuviBalcaoHttp implements BalcaoApi {
  LuviBalcaoHttp({required this.base, required this.sessao, http.Client? httpCliente})
      : httpCliente = httpCliente ?? http.Client();

  @override
  final String base;
  final Sessao sessao;
  final http.Client httpCliente;
  String? _token;

  @override
  String nome = '';

  String get _raiz => base.endsWith('/') ? base.substring(0, base.length - 1) : base;

  @override
  Future<bool> restaurar() async {
    _token = await sessao.ler();
    nome = await sessao.lerNome() ?? '';
    if (_token == null || _token!.isEmpty) return false;
    try {
      await _conferirEquipe();
      return true;
    } catch (_) {
      await sair();
      return false;
    }
  }

  @override
  Future<void> entrar(String identificador, String senha) async {
    final dados = await _enviar('POST', '/api/v1/auth/token', {
      'identificador': identificador.trim(),
      'senha': senha,
    }, autenticado: false);
    _token = dados['token'] as String;
    final recebido = dados['nome'];
    nome = recebido is String && recebido.trim().isNotEmpty ? recebido.trim() : identificador.trim();
    try {
      await _conferirEquipe();
    } catch (e) {
      await _revogar();
      _token = null;
      nome = '';
      rethrow;
    }
    await sessao.salvar(_token!);
    await sessao.salvarNome(nome);
  }

  Future<void> _conferirEquipe() async {
    final me = await _enviar('GET', '/api/v1/auth/me', null);
    final perfis = (me['perfis'] as List?)?.map((p) => '$p').toList() ?? const <String>[];
    final nomeMe = me['nome'];
    if (nomeMe is String && nomeMe.trim().isNotEmpty) nome = nomeMe.trim();
    if (!perfis.contains('ROLE_OPERADOR') && !perfis.contains('ROLE_ADMIN')) {
      throw FalhaApi('Esta conta é de cliente. O aplicativo do balcão é da barbearia.');
    }
  }

  @override
  Future<List<Loja>> lojas() async {
    final dados = await _enviar('GET', '/api/v1/painel/estabelecimentos', null);
    final lista = dados['_lista'] as List? ?? const [];
    return lista.map((item) {
      final mapa = item as Map;
      return Loja(id: '${mapa['id']}', nome: '${mapa['nome'] ?? ''}', podeOperar: mapa['podeOperar'] == true);
    }).where((loja) => loja.podeOperar).toList();
  }

  @override
  Future<List<Campanha>> campanhas(String lojaId) async {
    final dados = await _enviar('GET', '/api/v1/estabelecimentos/$lojaId/campanhas', null);
    final lista = dados['_lista'] as List? ?? const [];
    return lista.map((item) {
      final mapa = item as Map;
      return Campanha(
        id: '${mapa['id']}',
        titulo: '${mapa['titulo'] ?? ''}',
        meta: (mapa['metaCarimbos'] as num?)?.toInt() ?? 0,
        recompensa: '${mapa['recompensa'] ?? ''}',
        ativa: mapa['ativa'] == true,
        principal: mapa['principal'] == true,
      );
    }).toList();
  }

  @override
  Future<ClienteBalcao> cliente(String lojaId, String participacaoId) async {
    final pessoa = await _enviar('GET', '/api/v1/estabelecimentos/$lojaId/participantes/$participacaoId', null);
    final cartoes = await _enviar('GET', '/api/v1/estabelecimentos/$lojaId/participantes/$participacaoId/cartoes?tamanho=20', null);
    final lista = (cartoes['conteudo'] as List?) ?? const [];
    Map atual = {};
    for (final item in lista) {
      final mapa = item as Map;
      if (mapa['status'] == 'EM_ANDAMENTO') atual = mapa;
    }
    if (atual.isEmpty && lista.isNotEmpty) atual = lista.last as Map;
    return ClienteBalcao(
      id: '${pessoa['id']}',
      nome: '${pessoa['nomeExibicao'] ?? ''}',
      whatsapp: '${pessoa['whatsapp'] ?? ''}',
      titulo: '${atual['titulo'] ?? 'Cartão'}',
      recompensa: '${atual['recompensa'] ?? ''}',
      saldo: (atual['saldo'] as num?)?.toInt() ?? 0,
      meta: (atual['metaCarimbos'] as num?)?.toInt() ?? 0,
    );
  }

  @override
  Future<ResultadoCarimbo> carimbar(String lojaId, String participacaoId, List<String> campanhas) async {
    final dados = await _enviar(
      'POST',
      '/api/v1/estabelecimentos/$lojaId/participantes/$participacaoId/atendimentos',
      {'campanhas': campanhas},
      chave: true,
    );
    return _resultado((dados['_lista'] as List?) ?? const [], nomeCliente: '', cadastrado: false);
  }

  @override
  Future<ResultadoCarimbo> atenderWhatsapp(String lojaId, String whatsapp, String nome) async {
    final dados = await _enviar(
      'POST',
      '/api/v1/estabelecimentos/$lojaId/atendimento-rapido',
      {
        'whatsapp': whatsapp.trim(),
        if (nome.trim().isNotEmpty) 'nome': nome.trim(),
      },
      chave: true,
    );
    final participante = dados['participante'];
    final nomeCliente = participante is Map ? '${participante['nomeExibicao'] ?? ''}' : nome.trim();
    return _resultado((dados['carimbos'] as List?) ?? const [], nomeCliente: nomeCliente, cadastrado: dados['cadastrado'] == true);
  }

  ResultadoCarimbo _resultado(List carimbos, {required String nomeCliente, required bool cadastrado}) {
    var saldo = 0;
    var concluiu = false;
    for (final item in carimbos) {
      if (item is! Map) continue;
      saldo = (item['saldoApos'] as num?)?.toInt() ?? saldo;
      if (item['concluiuCartao'] == true) concluiu = true;
    }
    return ResultadoCarimbo(nome: nomeCliente, saldo: saldo, concluiu: concluiu, cadastrado: cadastrado);
  }

  @override
  Future<RecompensaBalcao> recompensa(String lojaId, String recompensaId) async {
    final dados = await _enviar('GET', '/api/v1/estabelecimentos/$lojaId/recompensas/$recompensaId', null);
    return RecompensaBalcao(id: '${dados['id']}', descricao: '${dados['descricao'] ?? ''}', status: '${dados['status'] ?? ''}');
  }

  @override
  Future<void> entregar(String lojaId, String recompensaId) async {
    await _enviar('POST', '/api/v1/estabelecimentos/$lojaId/recompensas/$recompensaId/resgate', null, chave: true);
  }

  @override
  Future<void> sair() async {
    await _revogar();
    _token = null;
    nome = '';
    await sessao.limpar();
  }

  Future<void> _revogar() async {
    if (_token == null) return;
    try {
      await _enviar('DELETE', '/api/v1/auth/token', null);
    } catch (_) {}
  }

  Future<Map<String, dynamic>> _enviar(
    String metodo,
    String caminho,
    Map<String, dynamic>? corpo, {
    bool autenticado = true,
    bool chave = false,
  }) async {
    final uri = Uri.parse('$_raiz$caminho');
    final headers = {
      'Content-Type': 'application/json',
      'X-Luvi-App': '1',
      if (autenticado && _token != null) 'Authorization': 'Bearer $_token',
      if (chave) 'Idempotency-Key': _uuid(),
    };
    late http.Response resposta;
    switch (metodo) {
      case 'POST':
        resposta = await httpCliente.post(uri, headers: headers, body: jsonEncode(corpo ?? {}));
      case 'DELETE':
        resposta = await httpCliente.delete(uri, headers: headers);
      default:
        resposta = await httpCliente.get(uri, headers: headers);
    }
    if (resposta.statusCode >= 400) {
      var mensagem = 'Não foi possível concluir. Código ${resposta.statusCode}.';
      try {
        final corpoErro = jsonDecode(resposta.body);
        if (corpoErro is Map && corpoErro['mensagem'] is String) mensagem = corpoErro['mensagem'] as String;
      } catch (_) {}
      throw FalhaApi(mensagem);
    }
    if (resposta.body.isEmpty) return {};
    final decodificado = jsonDecode(resposta.body);
    if (decodificado is List) return {'_lista': decodificado};
    if (decodificado is Map<String, dynamic>) return decodificado;
    if (decodificado is Map) return decodificado.map((k, v) => MapEntry('$k', v));
    return {};
  }

  String _uuid() {
    final aleatorio = Random.secure();
    final bytes = List<int>.generate(16, (_) => aleatorio.nextInt(256));
    bytes[6] = (bytes[6] & 0x0f) | 0x40;
    bytes[8] = (bytes[8] & 0x3f) | 0x80;
    final hex = bytes.map((b) => b.toRadixString(16).padLeft(2, '0')).join();
    return '${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-${hex.substring(16, 20)}-${hex.substring(20)}';
  }
}
