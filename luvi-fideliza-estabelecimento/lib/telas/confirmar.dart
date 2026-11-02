import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';
import 'entrar.dart';

class ConfirmarCarimboPage extends StatefulWidget {
  const ConfirmarCarimboPage({super.key, required this.api, required this.loja, required this.participacaoId});

  final BalcaoApi api;
  final Loja loja;
  final String participacaoId;

  @override
  State<ConfirmarCarimboPage> createState() => _ConfirmarCarimboPageState();
}

class _ConfirmarCarimboPageState extends State<ConfirmarCarimboPage> {
  ClienteBalcao? _cliente;
  List<Campanha> _campanhas = const [];
  final Set<String> _marcadas = {};
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
      final cliente = await widget.api.cliente(widget.loja.id, widget.participacaoId);
      final campanhas = (await widget.api.campanhas(widget.loja.id)).where((c) => c.ativa).toList();
      if (!mounted) return;
      setState(() {
        _cliente = cliente;
        _campanhas = campanhas;
        _marcadas.addAll(campanhas.where((c) => campanhas.length == 1 || c.principal).map((c) => c.id));
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

  Future<void> _carimbar() async {
    final escolhidas = _campanhas.where((c) => _marcadas.contains(c.id)).map((c) => c.id).toList();
    if (escolhidas.isEmpty) {
      setState(() => _aviso = 'Escolha uma campanha.');
      return;
    }
    setState(() {
      _ocupado = true;
      _aviso = null;
    });
    try {
      final resultado = await widget.api.carimbar(widget.loja.id, widget.participacaoId, escolhidas);
      if (!mounted) return;
      setState(() {
        _ok = resultado.concluiu ? 'Cartão completo. A recompensa está disponível.' : 'Carimbo registrado. Saldo ${resultado.saldo}.';
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
    final cliente = _cliente;
    return Scaffold(
      appBar: AppBar(title: const Text('Carimbar')),
      body: SafeArea(
        child: _ocupado && cliente == null
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.all(24),
                children: [
                  if (cliente != null) ...[
                    Text(cliente.nome, style: Theme.of(context).textTheme.headlineSmall?.copyWith(fontWeight: FontWeight.w700)),
                    const SizedBox(height: 4),
                    Text(cliente.whatsapp),
                    const SizedBox(height: 20),
                    Text(cliente.titulo, style: Theme.of(context).textTheme.titleLarge),
                    Text('${cliente.saldo} de ${cliente.meta} carimbos'),
                    if (cliente.recompensa.isNotEmpty) Text('Recompensa: ${cliente.recompensa}'),
                  ],
                  if (_campanhas.length > 1) ...[
                    const SizedBox(height: 16),
                    for (final campanha in _campanhas)
                      CheckboxListTile(
                        contentPadding: EdgeInsets.zero,
                        value: _marcadas.contains(campanha.id),
                        title: Text(campanha.titulo + (campanha.principal ? ' · principal' : '')),
                        onChanged: _ok == null
                            ? (valor) => setState(() {
                                  if (valor == true) {
                                    _marcadas.add(campanha.id);
                                  } else {
                                    _marcadas.remove(campanha.id);
                                  }
                                })
                            : null,
                      ),
                  ],
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
                      onPressed: _ocupado || cliente == null ? null : _carimbar,
                      style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(56)),
                      child: const Text('Carimbar'),
                    ),
                ],
              ),
      ),
    );
  }
}
