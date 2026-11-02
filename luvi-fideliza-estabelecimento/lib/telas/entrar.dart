import 'package:flutter/material.dart';

import '../api.dart';

const erro = Color(0xFFA12B32);

class EntrarPage extends StatefulWidget {
  const EntrarPage({super.key, required this.api, required this.aoEntrar});

  final BalcaoApi api;
  final VoidCallback aoEntrar;

  @override
  State<EntrarPage> createState() => _EntrarPageState();
}

class _EntrarPageState extends State<EntrarPage> {
  final _id = TextEditingController();
  final _senha = TextEditingController();
  bool _ocupado = false;
  String? _aviso;

  @override
  void dispose() {
    _id.dispose();
    _senha.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    setState(() {
      _ocupado = true;
      _aviso = null;
    });
    try {
      await widget.api.entrar(_id.text, _senha.text);
      if (mounted) widget.aoEntrar();
    } catch (e) {
      if (mounted) setState(() => _aviso = '$e');
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 48, 24, 24),
          children: [
            Text('LUVI Balcão', style: Theme.of(context).textTheme.headlineMedium?.copyWith(color: const Color(0xFF1D3E5B), fontWeight: FontWeight.w700)),
            const SizedBox(height: 8),
            const Text('Entre com a conta da barbearia.'),
            const SizedBox(height: 28),
            TextField(
              key: const Key('identificador'),
              controller: _id,
              keyboardType: TextInputType.emailAddress,
              textInputAction: TextInputAction.next,
              decoration: const InputDecoration(labelText: 'E-mail ou WhatsApp', border: OutlineInputBorder()),
            ),
            const SizedBox(height: 12),
            TextField(
              key: const Key('senha'),
              controller: _senha,
              obscureText: true,
              onSubmitted: (_) => _ocupado ? null : _enviar(),
              decoration: const InputDecoration(labelText: 'Senha', border: OutlineInputBorder()),
            ),
            if (_aviso != null) ...[
              const SizedBox(height: 12),
              Text(_aviso!, style: const TextStyle(color: erro, fontWeight: FontWeight.w600)),
            ],
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _ocupado ? null : _enviar,
              style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
              child: Text(_ocupado ? 'Entrando…' : 'Entrar'),
            ),
          ],
        ),
      ),
    );
  }
}
