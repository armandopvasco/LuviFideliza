import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';
import 'escanear.dart';

class AderirPage extends StatefulWidget {
  const AderirPage({super.key, required this.api, this.escanear});

  final LuviApi api;
  final Future<String?> Function(BuildContext context)? escanear;

  @override
  State<AderirPage> createState() => _AderirPageState();
}

class _AderirPageState extends State<AderirPage> {
  final _link = TextEditingController();
  String? _aviso;
  bool _ocupado = false;

  @override
  void dispose() {
    _link.dispose();
    super.dispose();
  }

  Future<void> _concluir(String texto) async {
    final loja = lojaDoTexto(texto);
    if (loja == null) {
      setState(() => _aviso = 'Não encontrei o código da loja nesse QR ou link.');
      return;
    }
    setState(() { _ocupado = true; _aviso = null; });
    try {
      await widget.api.aderir(loja);
      if (!mounted) return;
      Navigator.pop(context, true);
    } catch (erro) {
      setState(() => _aviso = '$erro'.replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Participar de uma loja')),
      body: ListView(
        padding: const EdgeInsets.all(24),
        children: [
          const Text('Escaneie o QR do balcão ou cole o link de adesão.'),
          const SizedBox(height: 16),
          TextField(
            key: const Key('link-adesao'),
            controller: _link,
            decoration: const InputDecoration(labelText: 'Link ou código da loja'),
          ),
          const SizedBox(height: 12),
          FilledButton(
            onPressed: _ocupado ? null : () => _concluir(_link.text),
            child: const Text('Participar'),
          ),
          OutlinedButton(
            onPressed: _ocupado
                ? null
                : () async {
                    final leitura = widget.escanear ??
                        (ctx) => Navigator.push<String>(ctx, MaterialPageRoute(builder: (_) => const EscanearPage()));
                    final codigo = await leitura(context);
                    if (codigo != null) await _concluir(codigo);
                  },
            child: const Text('Escanear QR da loja'),
          ),
          if (_aviso != null) Padding(padding: const EdgeInsets.only(top: 16), child: Text(_aviso!)),
        ],
      ),
    );
  }
}
