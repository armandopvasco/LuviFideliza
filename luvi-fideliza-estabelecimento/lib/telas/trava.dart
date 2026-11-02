import 'package:flutter/material.dart';

import '../biometria.dart';
import '../tema.dart';

class TravaPage extends StatefulWidget {
  const TravaPage({super.key, required this.biometria, required this.aoLiberar, required this.aoSenha});

  final Biometria biometria;
  final VoidCallback aoLiberar;
  final Future<void> Function() aoSenha;

  @override
  State<TravaPage> createState() => _TravaPageState();
}

class _TravaPageState extends State<TravaPage> {
  String? _aviso;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _pedir());
  }

  Future<void> _pedir() async {
    final ok = await widget.biometria.confirmar();
    if (!mounted) return;
    if (ok) {
      widget.aoLiberar();
    } else {
      setState(() => _aviso = 'A biometria não foi confirmada.');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(28),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.fingerprint, size: 72, color: marca),
              const SizedBox(height: 16),
              Text('Confirme sua identidade', style: Theme.of(context).textTheme.headlineSmall?.copyWith(color: marca, fontWeight: FontWeight.w700)),
              if (_aviso != null) ...[
                const SizedBox(height: 12),
                Text(_aviso!, style: const TextStyle(color: erro, fontWeight: FontWeight.w600)),
              ],
              const SizedBox(height: 24),
              FilledButton(
                onPressed: _pedir,
                style: FilledButton.styleFrom(minimumSize: const Size.fromHeight(52)),
                child: const Text('Usar biometria'),
              ),
              TextButton(
                onPressed: widget.aoSenha,
                child: const Text('Entrar com senha'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

Future<void> oferecerBiometria(BuildContext context, Biometria biometria) async {
  if (!await biometria.podeUsar() || await biometria.estaLigada()) return;
  if (!context.mounted) return;
  final quer = await showDialog<bool>(
    context: context,
    builder: (contexto) => AlertDialog(
      title: const Text('Biometria'),
      content: const Text('Quer abrir este aplicativo com a digital ou o rosto deste celular?'),
      actions: [
        TextButton(onPressed: () => Navigator.pop(contexto, false), child: const Text('Agora não')),
        FilledButton(onPressed: () => Navigator.pop(contexto, true), child: const Text('Ativar')),
      ],
    ),
  );
  if (quer == true) await biometria.ligar();
}

class BotaoBiometria extends StatefulWidget {
  const BotaoBiometria({super.key, required this.biometria});

  final Biometria biometria;

  @override
  State<BotaoBiometria> createState() => _BotaoBiometriaState();
}

class _BotaoBiometriaState extends State<BotaoBiometria> {
  bool _pode = false;
  bool _ligada = false;

  @override
  void initState() {
    super.initState();
    _ler();
  }

  Future<void> _ler() async {
    final pode = await widget.biometria.podeUsar();
    final ligada = await widget.biometria.estaLigada();
    if (!mounted) return;
    setState(() { _pode = pode; _ligada = ligada; });
  }

  Future<void> _alternar() async {
    if (_ligada) {
      await widget.biometria.desligar();
    } else {
      await widget.biometria.ligar();
    }
    await _ler();
  }

  @override
  Widget build(BuildContext context) {
    if (!_pode) return const SizedBox.shrink();
    return IconButton(
      tooltip: _ligada ? 'Desligar biometria' : 'Usar biometria',
      onPressed: _alternar,
      icon: Icon(_ligada ? Icons.fingerprint : Icons.fingerprint_outlined),
    );
  }
}
