import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:luvi_fideliza_cliente/api.dart';
import 'package:luvi_fideliza_cliente/app.dart';
import 'package:luvi_fideliza_cliente/modelos.dart';

void main() {
  test('extrai a loja do link de adesão', () {
    const id = '98b293a1-5b28-4cb3-9bf2-ba65ba5ce682';
    expect(lojaDoTexto('http://localhost:8081/adesao.html?loja=$id'), id);
    expect(lojaDoTexto(id), id);
    expect(lojaDoTexto('sem codigo'), isNull);
  });

  testWidgets('entrar mostra o cartão e o QR', (tester) async {
    final api = ApiMemoria(sessao: SessaoMemoria());
    await tester.pumpWidget(LuviApp(api: api));
    await tester.pumpAndSettle();
    expect(find.text('Entrar'), findsOneWidget);

    await tester.enterText(find.byKey(const Key('identificador')), '81999990001');
    await tester.enterText(find.byKey(const Key('senha')), 'SenhaTeste123!');
    await tester.tap(find.widgetWithText(FilledButton, 'Entrar'));
    await tester.pumpAndSettle();

    expect(find.text('Cliente teste'), findsOneWidget);
    expect(find.text('Cortes'), findsOneWidget);
    expect(find.text('2 de 5 carimbos'), findsOneWidget);
    expect(find.text('Mostrar QR'), findsNothing);
    await tester.tap(find.text('QR Code'));
    await tester.pumpAndSettle();
    expect(find.text('Mostre este QR no balcão para receber o carimbo.'), findsOneWidget);
  });

  testWidgets('cola o link da loja e adere', (tester) async {
    final api = ApiMemoria(sessao: SessaoMemoria())..dentro = true;
    await api.sessao!.salvar('token-teste');
    await tester.pumpWidget(LuviApp(api: api, escanear: (_) async => 'adesao.html?loja=98b293a1-5b28-4cb3-9bf2-ba65ba5ce682'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Participar'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Escanear QR da loja'));
    await tester.pumpAndSettle();
    expect(api.loja, '98b293a1-5b28-4cb3-9bf2-ba65ba5ce682');
    expect(find.text('Cortes'), findsOneWidget);
  });
}
