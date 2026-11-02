import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';

class EscanearPage extends StatelessWidget {
  const EscanearPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Ler QR')),
      body: MobileScanner(
        onDetect: (captura) {
          final codigo = captura.barcodes.isEmpty ? null : captura.barcodes.first.rawValue;
          if (codigo == null || codigo.isEmpty) return;
          Navigator.pop(context, codigo);
        },
      ),
    );
  }
}
