import 'package:flutter/material.dart';

import '../api.dart';
import '../modelos.dart';
import 'aderir.dart';
import 'qr_tela.dart';

class CarteiraPage extends StatefulWidget {
  const CarteiraPage({super.key, required this.api, required this.aoSair, this.escanear});

  final LuviApi api;
  final Future<void> Function() aoSair;
  final Future<String?> Function(BuildContext context)? escanear;

  @override
  State<CarteiraPage> createState() => _CarteiraPageState();
}

class _CarteiraPageState extends State<CarteiraPage> {
  List<CartaoCliente> _cartoes = const [];
  List<RecompensaCliente> _premios = const [];
  String? _aviso;
  bool _carregando = true;

  @override
  void initState() {
    super.initState();
    _atualizar();
  }

  Future<void> _atualizar() async {
    setState(() => _carregando = true);
    try {
      final cartoes = await widget.api.cartoes();
      final premios = await widget.api.recompensas();
      if (!mounted) return;
      setState(() { _cartoes = cartoes; _premios = premios; _aviso = null; });
    } catch (erro) {
      if (!mounted) return;
      setState(() => _aviso = '$erro'.replaceFirst('Exception: ', ''));
    } finally {
      if (mounted) setState(() => _carregando = false);
    }
  }

  RecompensaCliente? _premioDe(CartaoCliente cartao) {
    for (final premio in _premios) {
      if (premio.cartaoId == cartao.id && premio.disponivel) return premio;
    }
    if (cartao.concluido) {
      for (final premio in _premios) {
        if (premio.cartaoId == cartao.id) return premio;
      }
    }
    return null;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Meus cartões'),
        actions: [
          IconButton(onPressed: _atualizar, icon: const Icon(Icons.refresh), tooltip: 'Atualizar'),
          TextButton(onPressed: widget.aoSair, child: const Text('Sair')),
        ],
      ),
      body: _carregando
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _atualizar,
              child: ListView(
                padding: const EdgeInsets.all(16),
                children: [
                  if (_aviso != null) Text(_aviso!),
                  if (_cartoes.isEmpty)
                    const Padding(
                      padding: EdgeInsets.symmetric(vertical: 24),
                      child: Text('Você ainda não tem cartões. Participe de uma loja pelo QR do balcão.'),
                    ),
                  for (final cartao in _cartoes) _CartaoTile(
                    cartao: cartao,
                    premio: _premioDe(cartao),
                    aoMostrarQr: () async {
                      final url = await widget.api.urlQrParticipacao(cartao.participacaoId);
                      if (!context.mounted) return;
                      await Navigator.push(context, MaterialPageRoute(builder: (_) => QrTela(
                        titulo: 'QR do cliente',
                        legenda: 'Mostre este QR no balcão para receber o carimbo.',
                        url: url,
                      )));
                    },
                    aoMostrarPremio: (premio) async {
                      final url = await widget.api.urlQrRecompensa(premio.id);
                      if (!context.mounted) return;
                      await Navigator.push(context, MaterialPageRoute(builder: (_) => QrTela(
                        titulo: 'QR da recompensa',
                        legenda: 'Mostre este QR para o estabelecimento confirmar a entrega.',
                        url: url,
                      )));
                    },
                  ),
                ],
              ),
            ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () async {
          final entrou = await Navigator.push<bool>(context, MaterialPageRoute(
            builder: (_) => AderirPage(api: widget.api, escanear: widget.escanear),
          ));
          if (entrou == true) await _atualizar();
        },
        label: const Text('Participar'),
        icon: const Icon(Icons.qr_code_scanner),
      ),
    );
  }
}

class _CartaoTile extends StatelessWidget {
  const _CartaoTile({
    required this.cartao,
    required this.premio,
    required this.aoMostrarQr,
    required this.aoMostrarPremio,
  });

  final CartaoCliente cartao;
  final RecompensaCliente? premio;
  final Future<void> Function() aoMostrarQr;
  final Future<void> Function(RecompensaCliente premio) aoMostrarPremio;

  @override
  Widget build(BuildContext context) {
    final progresso = cartao.meta == 0 ? 0.0 : (cartao.saldo / cartao.meta).clamp(0, 1).toDouble();
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(cartao.estabelecimento, style: Theme.of(context).textTheme.labelMedium),
            Text(cartao.titulo, style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 8),
            LinearProgressIndicator(value: progresso),
            const SizedBox(height: 8),
            Text('${cartao.saldo} de ${cartao.meta} carimbos'),
            Text(cartao.recompensa),
            const SizedBox(height: 8),
            Wrap(
              spacing: 8,
              children: [
                OutlinedButton(onPressed: aoMostrarQr, child: const Text('Mostrar QR')),
                if (premio != null && premio!.disponivel)
                  FilledButton(
                    onPressed: () => aoMostrarPremio(premio!),
                    child: const Text('QR da recompensa'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
