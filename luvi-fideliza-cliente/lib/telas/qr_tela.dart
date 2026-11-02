import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter/material.dart';

class QrTela extends StatelessWidget {
  const QrTela({
    super.key,
    required this.titulo,
    required this.legenda,
    required this.png,
    this.tituloExtra,
    this.legendaExtra,
    this.pngExtra,
  });

  final String titulo;
  final String legenda;
  final Uint8List png;
  final String? tituloExtra;
  final String? legendaExtra;
  final Uint8List? pngExtra;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      body: LayoutBuilder(
        builder: (context, constraints) {
          final referencia = math.min(constraints.maxWidth, constraints.maxHeight);
          final lado = referencia * (pngExtra == null ? 0.72 : 0.5);
          final tamanho = lado.clamp(200.0, 380.0);
          return SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 28, 24, 32),
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: math.max(0, constraints.maxHeight - 60)),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(legenda, textAlign: TextAlign.center),
                  const SizedBox(height: 20),
                  _Imagem(png: png, tamanho: tamanho),
                  if (pngExtra != null) ...[
                    const SizedBox(height: 36),
                    Text(tituloExtra ?? '', textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleMedium),
                    const SizedBox(height: 8),
                    Text(legendaExtra ?? '', textAlign: TextAlign.center),
                    const SizedBox(height: 20),
                    _Imagem(png: pngExtra!, tamanho: tamanho),
                  ],
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}

class _Imagem extends StatelessWidget {
  const _Imagem({required this.png, required this.tamanho});

  final Uint8List png;
  final double tamanho;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ClipRRect(
        borderRadius: BorderRadius.circular(28),
        child: Image.memory(png, width: tamanho, height: tamanho, fit: BoxFit.contain, gaplessPlayback: true),
      ),
    );
  }
}
