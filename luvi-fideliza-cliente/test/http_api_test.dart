import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:luvi_fideliza_cliente/api.dart';
import 'package:luvi_fideliza_cliente/http_api.dart';

class _ClienteFalso extends http.BaseClient {
  _ClienteFalso(this.respostas);
  final List<_Resposta> respostas;
  final List<http.Request> enviados = [];

  @override
  Future<http.StreamedResponse> send(http.BaseRequest request) async {
    final pedido = request as http.Request;
    enviados.add(pedido);
    final resposta = respostas.removeAt(0);
    return http.StreamedResponse(
      Stream.value(utf8.encode(resposta.corpo)),
      resposta.status,
      headers: const {'content-type': 'application/json'},
    );
  }
}

class _Resposta {
  _Resposta(this.status, this.corpo);
  final int status;
  final String corpo;
}

void main() {
  test('login por token e carteira enviam o cabeçalho do aplicativo', () async {
    final cliente = _ClienteFalso([
      _Resposta(200, '{"token":"abc","expiraEm":"2026-12-01T00:00:00Z","nome":"Ana","identificador":"5581999990001"}'),
      _Resposta(200, '{"conteudo":[{"id":"c1","participacaoId":"p1","estabelecimentoId":"e1","estabelecimento":"Barbearia","titulo":"Cortes","recompensa":"Corte","metaCarimbos":5,"saldo":1,"status":"EM_ANDAMENTO"}],"pagina":0,"tamanho":50,"total":1}'),
    ]);
    final api = LuviHttp(base: 'http://localhost:8081', sessao: SessaoMemoria(), cliente: cliente);
    await api.entrar('81999990001', 'SenhaTeste123!');
    expect(cliente.enviados.first.headers['X-Luvi-App'], '1');
    expect(cliente.enviados.first.headers.containsKey('Authorization'), isFalse);
    final cartoes = await api.cartoes();
    expect(cartoes.single.titulo, 'Cortes');
    expect(cliente.enviados.last.headers['Authorization'], 'Bearer abc');
  });
}
