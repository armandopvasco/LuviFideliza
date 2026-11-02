import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../api.dart';
import '../biometria.dart';
import '../modelos.dart';
import '../tema.dart';
import 'aderir.dart';
import 'cartao_visual.dart';
import 'qr_tela.dart';
import 'trava.dart';

class CarteiraPage extends StatefulWidget {
  const CarteiraPage({super.key, required this.api, required this.aoSair, required this.biometria, this.escanear});

  final LuviApi api;
  final Future<void> Function() aoSair;
  final Biometria biometria;
  final Future<String?> Function(BuildContext context)? escanear;

  @override
  State<CarteiraPage> createState() => _CarteiraPageState();
}

class _CarteiraPageState extends State<CarteiraPage> {
  List<CartaoCliente> _cartoes = const [];
  List<RecompensaCliente> _premios = const [];
  Map<String, VisualCartao> _visuais = const {};
  Map<String, Uint8List> _fotos = const {};
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
      final visuais = <String, VisualCartao>{};
      final fotos = <String, Uint8List>{};
      for (final cartao in cartoes) {
        if (visuais.containsKey(cartao.estabelecimentoId)) continue;
        try {
          final visual = await widget.api.visual(cartao.estabelecimentoId);
          visuais[cartao.estabelecimentoId] = visual;
          if (visual.temFoto) {
            final bytes = await widget.api.fundo(cartao.estabelecimentoId);
            if (bytes != null) fotos[cartao.estabelecimentoId] = Uint8List.fromList(bytes);
          }
        } catch (_) {
          visuais[cartao.estabelecimentoId] = VisualCartao.padrao;
        }
      }
      if (!mounted) return;
      setState(() { _cartoes = cartoes; _premios = premios; _visuais = visuais; _fotos = fotos; _aviso = null; });
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
          BotaoBiometria(biometria: widget.biometria),
          BotaoSair(aoSair: widget.aoSair),
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
                for (final cartao in _cartoes)
                  CartaoVisual(
                    cartao: cartao,
                    visual: _visuais[cartao.estabelecimentoId] ?? VisualCartao.padrao,
                    foto: _fotos[cartao.estabelecimentoId],
                    premioDisponivel: _premioDe(cartao) != null,
                  ),
              ],
            ),
      bottomNavigationBar: Material(
        elevation: 8,
        color: superficie,
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

