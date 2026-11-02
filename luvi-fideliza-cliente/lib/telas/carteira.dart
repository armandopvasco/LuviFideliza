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
    return null;
  }

  Future<void> _abrirQr() async {
    if (_cartoes.isEmpty) {
      setState(() => _aviso = 'Você ainda não tem cartão para mostrar.');
      return;
    }
    var cartao = _cartoes.first;
    if (_cartoes.length > 1) {
      final escolhido = await showModalBottomSheet<CartaoCliente>(
        context: context,
        builder: (contexto) => SafeArea(
          child: ListView(
            shrinkWrap: true,
            children: [
              const ListTile(title: Text('Escolha o cartão')),
              for (final item in _cartoes)
                ListTile(
                  title: Text(item.titulo),
                  subtitle: Text(item.estabelecimento),
                  onTap: () => Navigator.pop(contexto, item),
                ),
            ],
          ),
        ),
      );
      if (escolhido == null || !mounted) return;
      cartao = escolhido;
    }
    final url = await widget.api.urlQrParticipacao(cartao.participacaoId);
    final premio = _premioDe(cartao);
    final urlPremio = premio == null ? null : await widget.api.urlQrRecompensa(premio.id);
    if (!mounted) return;
    await Navigator.push(context, MaterialPageRoute(builder: (_) => QrTela(
      titulo: 'QR do cliente',
      legenda: 'Mostre este QR no balcão para receber o carimbo.',
      url: url,
      tituloExtra: urlPremio == null ? null : 'QR da recompensa',
      legendaExtra: urlPremio == null ? null : 'Mostre este QR para o estabelecimento confirmar a entrega.',
      urlExtra: urlPremio,
    )));
  }

  Future<void> _participar() async {
    final entrou = await Navigator.push<bool>(context, MaterialPageRoute(
      builder: (_) => AderirPage(api: widget.api, escanear: widget.escanear),
    ));
    if (entrou == true) await _atualizar();
  }

  @override
  Widget build(BuildContext context) {
    final nome = widget.api.nome.trim().isEmpty ? 'Minha conta' : widget.api.nome.trim();
    return Scaffold(
      appBar: AppBar(
        title: Text(nome),
        actions: [
          TextButton(onPressed: widget.aoSair, child: const Text('Sair')),
        ],
      ),
      body: _carregando
          ? const Center(child: CircularProgressIndicator())
          : ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_aviso != null) Text(_aviso!),
                if (_cartoes.isEmpty)
                  const Padding(
                    padding: EdgeInsets.symmetric(vertical: 24),
                    child: Text('Você ainda não tem cartões. Participe de uma loja pelo QR do balcão.'),
                  ),
                for (final cartao in _cartoes) _CartaoTile(cartao: cartao),
              ],
            ),
      bottomNavigationBar: Material(
        elevation: 8,
        color: Colors.white,
        child: SafeArea(
          child: SizedBox(
            height: 72,
            child: Row(
              children: [
                _AcaoRodape(icone: Icons.refresh, rotulo: 'Atualizar', aoPressionar: _carregando ? null : _atualizar),
                _AcaoRodape(icone: Icons.qr_code_2, rotulo: 'QR Code', aoPressionar: _carregando ? null : _abrirQr),
                _AcaoRodape(icone: Icons.storefront_outlined, rotulo: 'Participar', aoPressionar: _carregando ? null : _participar),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _AcaoRodape extends StatelessWidget {
  const _AcaoRodape({required this.icone, required this.rotulo, required this.aoPressionar});

  final IconData icone;
  final String rotulo;
  final VoidCallback? aoPressionar;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: aoPressionar,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(icone, color: const Color(0xFF1D3E5B)),
            const SizedBox(height: 4),
            Text(rotulo, style: const TextStyle(fontSize: 12, color: Color(0xFF1D3E5B))),
          ],
        ),
      ),
    );
  }
}

class _CartaoTile extends StatelessWidget {
  const _CartaoTile({required this.cartao});

  final CartaoCliente cartao;

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
          ],
        ),
      ),
    );
  }
}
