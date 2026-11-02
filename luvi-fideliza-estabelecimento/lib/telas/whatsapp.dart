import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';
import 'entrar.dart';

class WhatsappPage extends StatefulWidget {
  const WhatsappPage({super.key, required this.api, required this.loja});

  final BalcaoApi api;
  final Loja loja;

  @override
  State<WhatsappPage> createState() => _WhatsappPageState();
}

class _WhatsappPageState extends State<WhatsappPage> {
  final _whatsapp = TextEditingController();
  final _nome = TextEditingController();
  bool _pedirNome = false;
  bool _ocupado = false;
  String? _aviso;
  String? _ok;

  @override
  void dispose() {
    _whatsapp.dispose();
    _nome.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    setState(() {
      _ocupado = true;
      _aviso = null;
    });
    try {
      final resultado = await widget.api.atenderWhatsapp(widget.loja.id, _whatsapp.text, _nome.text);
      if (!mounted) return;
      final extra = resultado.cadastrado ? ' Cliente cadastrado.' : '';
      final fim = resultado.concluiu ? ' Cartão completo.' : '';
      setState(() => _ok = 'Carimbo registrado para ${resultado.nome}. Saldo ${resultado.saldo}.$extra$fim');
    } on FalhaApi catch (e) {
      if (!mounted) return;
      setState(() {
        _aviso = e.mensagem;
        if (e.mensagem.contains('Informe o nome')) _pedirNome = true;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _aviso = '$e');
    } finally {
      if (mounted) setState(() => _ocupado = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('WhatsApp')),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            const Text('Digite o WhatsApp. Na primeira visita, informe também o nome.'),
            const SizedBox(height: 16),
            TextField(
              key: const Key('whatsapp'),
              controller: _whatsapp,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'WhatsApp', border: OutlineInputBorder()),
            ),
            if (_pedirNome) ...[
              const SizedBox(height: 12),
              TextField(
                key: const Key('nome-cliente'),
                controller: _nome,
                textCapitalization: TextCapitalization.words,
                decoration: const InputDecoration(labelText: 'Nome do cliente', border: OutlineInputBorder()),
              ),
            ],
            if (_aviso != null) ...[
              const SizedBox(height: 12),
              Text(_aviso!, style: const TextStyle(color: erro, fontWeight: FontWeight.w600)),
            ],
            if (_ok != null) ...[
              const SizedBox(height: 12),
              Text(_ok!, style: const TextStyle(color: Color(0xFF1D3E5B), fontWeight: FontWeight.w700)),
            ],
            const SizedBox(height: 20),
            if (_ok == null)
              FilledButton(
                onPressed: _ocupado ? null : _enviar,
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56)),
                child: const Text('Carimbar'),
              ),
          ],
        ),
      ),
    );
  }
}
