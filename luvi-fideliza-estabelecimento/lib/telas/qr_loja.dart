import 'dart:math' as math;

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
        child: LayoutBuilder(
          builder: (context, constraints) {
            final lado = math.min(constraints.maxWidth, constraints.maxHeight) * 0.72;
            final tamanho = lado.clamp(220.0, 420.0);
            return Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Padding(
                  padding: EdgeInsets.symmetric(horizontal: 28),
                  child: Text(
                    'O cliente aponta a câmera para este código para entrar no programa.',
                    textAlign: TextAlign.center,
                  ),
                ),
                const SizedBox(height: 24),
                Center(
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(28),
                    child: Image.network(
                      '$raiz/api/v1/publico/estabelecimentos/${loja.id}/qrcode',
                      width: tamanho,
                      height: tamanho,
                      fit: BoxFit.contain,
                      errorBuilder: (context, error, stackTrace) => SizedBox(
                        width: tamanho,
                        height: tamanho,
                        child: const Center(child: Text('Não foi possível carregar o QR da loja.', textAlign: TextAlign.center)),
                      ),
                    ),
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }
}
