import 'package:flutter/material.dart';

import '../api.dart';

class EntrarPage extends StatefulWidget {
  const EntrarPage({super.key, required this.api, required this.aoEntrar});

  final LuviApi api;
  final VoidCallback aoEntrar;

  @override
  State<EntrarPage> createState() => _EntrarPageState();
}

class _EntrarPageState extends State<EntrarPage> {
  final _id = TextEditingController();
  final _senha = TextEditingController();
  final _nome = TextEditingController();
  final _contato = TextEditingController();
  bool _criando = false;
  bool _ocupado = false;
  String? _aviso;

  @override
  void dispose() {
    _id.dispose();
    _senha.dispose();
    _nome.dispose();
    _contato.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    setState(() { _ocupado = true; _aviso = null; });
    try {
      if (_criando) {
        final contato = _contato.text.trim();
        final email = contato.contains('@') ? contato : null;
        final telefone = email == null ? contato : null;
        await widget.api.cadastrar(nome: _nome.text, email: email, telefone: telefone, senha: _senha.text);
        await widget.api.entrar(contato, _senha.text);
      } else {
        await widget.api.entrar(_id.text, _senha.text);
      }
      widget.aoEntrar();
    } catch (erro) {
      setState(() => _aviso = '$erro'.replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const SizedBox(height: 24),
            Text('LUVI Fideliza+', style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 8),
            Text(_criando ? 'Crie sua conta para guardar os cartões.' : 'Entre para ver seus cartões.'),
            const SizedBox(height: 24),
            if (_criando)
              TextField(
                key: const Key('nome'),
                controller: _nome,
                decoration: const InputDecoration(labelText: 'Nome'),
              ),
            TextField(
              key: const Key('identificador'),
              controller: _criando ? _contato : _id,
              decoration: const InputDecoration(labelText: 'WhatsApp ou e-mail'),
            ),
            TextField(
              key: const Key('senha'),
              controller: _senha,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'Senha'),
            ),
            const SizedBox(height: 16),
            FilledButton(
              onPressed: _ocupado ? null : _enviar,
              child: Text(_criando ? 'Criar conta e entrar' : 'Entrar'),
            ),
            TextButton(
              onPressed: _ocupado ? null : () => setState(() => _criando = !_criando),
              child: Text(_criando ? 'Já tenho conta' : 'Criar conta'),
            ),
            if (_aviso != null) Text(_aviso!),
          ],
        ),
      ),
    );
  }
}
