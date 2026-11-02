import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:luvi_fideliza_estabelecimento/api.dart';
import 'package:luvi_fideliza_estabelecimento/app.dart';
import 'package:luvi_fideliza_estabelecimento/modelos.dart';

void main() {
  const loja = 'loja-1';
  const participacao = '11111111-1111-4111-8111-111111111111';
  const recompensa = '22222222-2222-4222-8222-222222222222';

  test('extrai participação e recompensa do QR', () {
    final cliente = lerCodigo('http://localhost:8081/painel.html?loja=$loja&participacao=$participacao');
    expect(cliente?.lojaId, loja);
    expect(cliente?.participacaoId, participacao);
    final premio = lerCodigo('painel.html?loja=$loja&recompensa=$recompensa');
    expect(premio?.recompensaId, recompensa);
    expect(lerCodigo('adesao.html?loja=$loja'), isNull);
    expect(lerCodigo('sem codigo'), isNull);
  });

  testWidgets('conta de cliente não entra no balcão', (tester) async {
    final api = ApiMemoria()..clientePuro = true;
    await tester.pumpWidget(BalcaoApp(api: api));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('identificador')), '81999990001');
    await tester.enterText(find.byKey(const Key('senha')), 'SenhaTeste123!');
    await tester.tap(find.widgetWithText(FilledButton, 'Entrar'));
    await tester.pumpAndSettle();
    expect(find.text('Esta conta é de cliente. O aplicativo do balcão é da barbearia.'), findsOneWidget);
    expect(find.text('Ler QR'), findsNothing);
  });

  testWidgets('lê o QR do cliente e só carimba ao confirmar', (tester) async {
    final api = ApiMemoria(sessao: SessaoMemoria());
    await tester.pumpWidget(BalcaoApp(
      api: api,
      escanear: (_) async => 'http://localhost:8081/painel.html?loja=$loja&participacao=$participacao',
    ));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('identificador')), 'dono@teste.local');
    await tester.enterText(find.byKey(const Key('senha')), 'SenhaTeste123!');
    await tester.tap(find.widgetWithText(FilledButton, 'Entrar'));
    await tester.pumpAndSettle();

    expect(find.text('Atendente'), findsOneWidget);
    expect(find.text('Sair'), findsOneWidget);
    expect(find.text('Barbearia Piloto'), findsOneWidget);
    await tester.tap(find.text('Ler QR'));
    await tester.pumpAndSettle();
    expect(find.text('João'), findsOneWidget);
    expect(find.text('1 de 5 carimbos'), findsOneWidget);
    expect(api.campanhasCarimbadas, isEmpty);
    await tester.tap(find.widgetWithText(FilledButton, 'Carimbar'));
    await tester.pumpAndSettle();
    expect(api.campanhasCarimbadas, ['camp-1']);
    expect(find.text('Carimbo registrado. Saldo 2.'), findsOneWidget);
  });

  testWidgets('QR de outra loja permanece no balcão', (tester) async {
    final api = ApiMemoria(sessao: SessaoMemoria())..dentro = true;
    await api.sessao!.salvar('token-teste');
    await api.sessao!.salvarNome('Atendente');
    await tester.pumpWidget(BalcaoApp(
      api: api,
      escanear: (_) async => 'painel.html?loja=outra-loja&participacao=$participacao',
    ));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ler QR'));
    await tester.pumpAndSettle();
    expect(find.text('Este QR é de outra loja.'), findsOneWidget);
    expect(find.text('João'), findsNothing);
  });

  testWidgets('QR da recompensa entrega só ao confirmar', (tester) async {
    final api = ApiMemoria(sessao: SessaoMemoria())..dentro = true;
    await api.sessao!.salvar('token');
    await api.sessao!.salvarNome('Atendente');
    await tester.pumpWidget(BalcaoApp(
      api: api,
      escanear: (_) async => 'painel.html?loja=$loja&recompensa=$recompensa',
    ));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Ler QR'));
    await tester.pumpAndSettle();
    expect(find.text('Um corte'), findsOneWidget);
    expect(api.recompensaEntregue, isNull);
    await tester.tap(find.widgetWithText(FilledButton, 'Entregar'));
    await tester.pumpAndSettle();
    expect(api.recompensaEntregue, recompensa);
    expect(find.text('Recompensa entregue.'), findsOneWidget);
  });

  testWidgets('WhatsApp novo pede o nome e depois carimba', (tester) async {
    final api = ApiMemoria(sessao: SessaoMemoria())
      ..dentro = true
      ..pedirNome = true;
    await api.sessao!.salvar('token');
    await api.sessao!.salvarNome('Atendente');
    await tester.pumpWidget(BalcaoApp(api: api));
    await tester.pumpAndSettle();
    await tester.tap(find.text('WhatsApp'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('whatsapp')), '81999990008');
    await tester.tap(find.widgetWithText(FilledButton, 'Carimbar'));
    await tester.pumpAndSettle();
    expect(find.text('Informe o nome do cliente'), findsOneWidget);
    await tester.enterText(find.byKey(const Key('nome-cliente')), 'Ana');
    await tester.tap(find.widgetWithText(FilledButton, 'Carimbar'));
    await tester.pumpAndSettle();
    expect(api.whatsappAtendido, '81999990008');
    expect(api.nomeInformado, 'Ana');
    expect(find.textContaining('Carimbo registrado para Ana.'), findsOneWidget);
  });
}
