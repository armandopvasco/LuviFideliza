import 'package:flutter/material.dart';

import 'api.dart';
import 'biometria.dart';
import 'tema.dart';
import 'telas/balcao.dart';
import 'telas/entrar.dart';
import 'telas/trava.dart';

class BalcaoApp extends StatefulWidget {
  const BalcaoApp({super.key, required this.api, this.escanear, this.biometria});

  final BalcaoApi api;
  final Future<String?> Function(BuildContext context)? escanear;
  final Biometria? biometria;

  @override
  State<BalcaoApp> createState() => _BalcaoAppState();
}

class _BalcaoAppState extends State<BalcaoApp> {
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
      title: 'LUVI Balcão',
      theme: temaLuvi(),
      home: !_pronto
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : !_dentro
              ? EntrarPage(api: widget.api, aoEntrar: () { _entrou(); })
              : _travado
                  ? TravaPage(biometria: _biometria, aoLiberar: () => setState(() => _travado = false), aoSenha: _sair)
                  : BalcaoPage(api: widget.api, escanear: widget.escanear, biometria: _biometria, aoSair: _sair),
    );
  }
}
