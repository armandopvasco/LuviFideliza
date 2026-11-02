import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';
import 'confirmar.dart';
import 'entrar.dart';
import 'escanear.dart';
import 'qr_loja.dart';
import 'recompensa.dart';
import 'whatsapp.dart';

class BalcaoPage extends StatefulWidget {
  const BalcaoPage({super.key, required this.api, required this.aoSair, this.escanear});

  final BalcaoApi api;
  final Future<void> Function() aoSair;
  final Future<String?> Function(BuildContext context)? escanear;

  @override
  State<BalcaoPage> createState() => _BalcaoPageState();
}

class _BalcaoPageState extends State<BalcaoPage> {
  List<Loja> _lojas = const [];
  Loja? _loja;
  String? _aviso;
  bool _carregando = true;

  @override
  void initState() {
    super.initState();
    _carregar();
  }

  Future<void> _carregar() async {
    try {
      final lista = await widget.api.lojas();
      if (!mounted) return;
      setState(() {
        _lojas = lista;
        _loja = lista.isEmpty ? null : lista.first;
        _carregando = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _aviso = '$e';
        _carregando = false;
      });
    }
  }

  Future<void> _ler() async {
    final loja = _loja;
    if (loja == null) return;
    setState(() => _aviso = null);
    final ler = widget.escanear ?? (contexto) => Navigator.push<String>(contexto, MaterialPageRoute(builder: (_) => const EscanearPage()));
    final lido = await ler(context);
    if (!mounted || lido == null || lido.isEmpty) return;
    final codigo = lerCodigo(lido);
    if (codigo == null) {
      setState(() => _aviso = 'QR não reconhecido.');
      return;
    }
    if (codigo.lojaId != loja.id) {
      setState(() => _aviso = 'Este QR é de outra loja.');
      return;
    }
    if (codigo.participacaoId != null) {
      await Navigator.push(context, MaterialPageRoute(builder: (_) => ConfirmarCarimboPage(api: widget.api, loja: loja, participacaoId: codigo.participacaoId!)));
      return;
    }
    await Navigator.push(context, MaterialPageRoute(builder: (_) => RecompensaPage(api: widget.api, loja: loja, recompensaId: codigo.recompensaId!)));
  }

  @override
  Widget build(BuildContext context) {
    final loja = _loja;
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.api.nome.isEmpty ? 'Balcão' : widget.api.nome),
        actions: [
          TextButton(onPressed: widget.aoSair, child: const Text('Sair')),
        ],
      ),
      body: SafeArea(
        child: _carregando
            ? const Center(child: CircularProgressIndicator())
            : ListView(
                padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
                children: [
                  if (_lojas.length > 1)
                    DropdownButton<String>(
                      isExpanded: true,
                      value: loja?.id,
                      items: _lojas.map((item) => DropdownMenuItem(value: item.id, child: Text(item.nome))).toList(),
                      onChanged: (id) => setState(() => _loja = _lojas.firstWhere((item) => item.id == id)),
                    )
                  else
                    Text(loja?.nome ?? 'Nenhuma loja para operar neste celular.', style: Theme.of(context).textTheme.titleLarge?.copyWith(color: const Color(0xFF1D3E5B))),
                  const SizedBox(height: 8),
                  const Text('Aponte a câmera para o QR do cliente ou digite o WhatsApp.'),
                  if (_aviso != null) ...[
                    const SizedBox(height: 12),
                    Text(_aviso!, style: const TextStyle(color: erro, fontWeight: FontWeight.w600)),
                  ],
                  const SizedBox(height: 20),
                  _Acao(
                    icone: Icons.qr_code_scanner,
                    titulo: 'Ler QR',
                    onPressed: loja == null ? null : _ler,
                  ),
                  const SizedBox(height: 12),
                  _Acao(
                    icone: Icons.chat_outlined,
                    titulo: 'WhatsApp',
                    onPressed: loja == null
                        ? null
                        : () => Navigator.push(context, MaterialPageRoute(builder: (_) => WhatsappPage(api: widget.api, loja: loja))),
                  ),
                  const SizedBox(height: 12),
                  _Acao(
                    icone: Icons.storefront_outlined,
                    titulo: 'QR da loja',
                    onPressed: loja == null
                        ? null
                        : () => Navigator.push(context, MaterialPageRoute(builder: (_) => QrLojaPage(api: widget.api, loja: loja))),
                  ),
                ],
              ),
      ),
    );
  }
}

class _Acao extends StatelessWidget {
  const _Acao({required this.icone, required this.titulo, required this.onPressed});

  final IconData icone;
  final String titulo;
  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return FilledButton.icon(
      onPressed: onPressed,
      icon: Icon(icone, size: 28),
      label: Text(titulo),
      style: FilledButton.styleFrom(
        minimumSize: const Size.fromHeight(72),
        textStyle: const TextStyle(fontSize: 20, fontWeight: FontWeight.w600),
        alignment: Alignment.centerLeft,
        padding: const EdgeInsets.symmetric(horizontal: 20),
      ),
    );
  }
}
