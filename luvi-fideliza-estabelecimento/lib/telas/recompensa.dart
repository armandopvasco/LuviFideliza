import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';
import 'entrar.dart';

class RecompensaPage extends StatefulWidget {
  const RecompensaPage({super.key, required this.api, required this.loja, required this.recompensaId});

  final BalcaoApi api;
  final Loja loja;
  final String recompensaId;

  @override
  State<RecompensaPage> createState() => _RecompensaPageState();
}

class _RecompensaPageState extends State<RecompensaPage> {
  RecompensaBalcao? _recompensa;
  String? _aviso;
  String? _ok;
  bool _ocupado = true;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    try {
      final recompensa = await widget.api.recompensa(widget.loja.id, widget.recompensaId);
      if (!mounted) return;
      setState(() {
        _recompensa = recompensa;
        _ocupado = false;
        if (recompensa.status != 'DISPONIVEL') _ok = 'Esta recompensa já foi entregue.';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _aviso = '$e';
        _ocupado = false;
      });
    }
  }

  Future<void> _entregar() async {
    setState(() {
      _ocupado = true;
      _aviso = null;
    });
    try {
      await widget.api.entregar(widget.loja.id, widget.recompensaId);
      if (!mounted) return;
      setState(() {
        _ok = 'Recompensa entregue.';
        _ocupado = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _aviso = '$e';
        _ocupado = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final recompensa = _recompensa;
    return Scaffold(
      appBar: AppBar(title: const Text('Entregar recompensa')),
      body: SafeArea(
        child: _ocupado && recompensa == null
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(24),
                children: [
                  if (recompensa != null)
                    Text(recompensa.descricao, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
                  if (_aviso != null) ...[
                    const SizedBox(height: 16),
                    Text(_aviso!, style: const TextStyle(color: erro, fontWeight: FontWeight.w600)),
                  ],
                  if (_ok != null) ...[
                    const SizedBox(height: 16),
                    Text(_ok!, style: const TextStyle(color: Color(0xFF1D3E5B), fontWeight: FontWeight.w700)),
                  ],
                  const SizedBox(height: 24),
                  if (_ok == null)
                    FilledButton(
                      onPressed: _ocupado || recompensa == null ? null : _entregar,
                      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56)),
                      child: const Text('Entregar'),
                    ),
                ],
              ),
      ),
    );
  }
}
