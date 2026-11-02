import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

class QrTela extends StatelessWidget {
  const QrTela({
    super.key,
    required this.titulo,
    required this.legenda,
    required this.url,
    this.tituloExtra,
    this.legendaExtra,
    this.urlExtra,
  });

  final String titulo;
  final String legenda;
  final String url;
  final String? tituloExtra;
  final String? legendaExtra;
  final String? urlExtra;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          Text(legenda, textAlign: TextAlign.center),
          const SizedBox(height: 24),
          Center(child: QrImageView(data: url, size: 260, backgroundColor: Colors.white)),
          if (urlExtra != null) ...[
            const SizedBox(height: 32),
            Text(tituloExtra ?? '', textAlign: TextAlign.center, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(legendaExtra ?? '', textAlign: TextAlign.center),
            const SizedBox(height: 24),
            Center(child: QrImageView(data: urlExtra!, size: 260, backgroundColor: Colors.white)),
          ],
        ],
      ),
    );
  }
}
