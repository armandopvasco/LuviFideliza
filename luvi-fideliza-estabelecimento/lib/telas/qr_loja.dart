import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';

class QrLojaPage extends StatelessWidget {
  const QrLojaPage({super.key, required this.api, required this.loja});

  final BalcaoApi api;
  final Loja loja;

  @override
  Widget build(BuildContext context) {
    final raiz = api.base.endsWith('/') ? api.base.substring(0, api.base.length - 1) : api.base;
    return Scaffold(
      appBar: AppBar(title: Text(loja.nome)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const Text('O cliente aponta a câmera para este código para entrar no programa.'),
            const SizedBox(height: 20),
            Center(
              child: Image.network(
                '$raiz/api/v1/publico/estabelecimentos/${loja.id}/qrcode',
                width: 260,
                height: 260,
                errorBuilder: (context, error, stackTrace) => const SizedBox(
                  width: 260,
                  height: 260,
                  child: Center(child: Text('Não foi possível carregar o QR da loja.')),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
