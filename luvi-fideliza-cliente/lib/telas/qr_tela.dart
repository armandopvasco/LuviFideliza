import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';

class QrTela extends StatelessWidget {
  const QrTela({super.key, required this.titulo, required this.legenda, required this.url});

  final String titulo;
  final String legenda;
  final String url;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(titulo)),
      body: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          children: [
            Text(legenda, textAlign: TextAlign.center),
            const SizedBox(height: 24),
            QrImageView(data: url, size: 260, backgroundColor: Colors.white),
          ],
        ),
      ),
    );
  }
}
