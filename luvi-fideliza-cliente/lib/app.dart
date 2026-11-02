import 'package:flutter/material.dart';

import 'api.dart';
import 'biometria.dart';
import 'tema.dart';
import 'telas/carteira.dart';
import 'telas/entrar.dart';
import 'telas/trava.dart';

class LuviApp extends StatefulWidget {
  const LuviApp({super.key, required this.api, this.escanear, this.biometria});

  final LuviApi api;
  final Future<String?> Function(BuildContext context)? escanear;
  final Biometria? biometria;

  @override
  State<LuviApp> createState() => _LuviAppState();
}

class _LuviAppState extends State<LuviApp> {
  late final Biometria _biometria = widget.biometria ?? BiometriaMemoria();
  bool _pronto = false;
  bool _dentro = false;
  bool _travado = false;

  @override
  void initState() {
    super.initState();
    _abrir();
  }

  Future<void> _abrir() async {
    final dentro = await widget.api.restaurar();
    final travado = dentro && await _biometria.estaLigada();
    if (!mounted) return;
    setState(() {
      _pronto = true;
      _dentro = dentro;
      _travado = travado;
    });
  }

  Future<void> _entrou() async {
    setState(() { _dentro = true; _travado = false; });
    await oferecerBiometria(context, _biometria);
  }

  Future<void> _sair() async {
    await widget.api.sair();
    if (mounted) setState(() { _dentro = false; _travado = false; });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LUVI Fideliza+',
      theme: temaLuvi(),
      home: !_pronto
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : !_dentro
              ? EntrarPage(api: widget.api, aoEntrar: () { _entrou(); })
              : _travado
                  ? TravaPage(biometria: _biometria, aoLiberar: () => setState(() => _travado = false), aoSenha: _sair)
                  : CarteiraPage(api: widget.api, escanear: widget.escanear, biometria: _biometria, aoSair: _sair),
    );
  }
}
