import 'package:flutter/material.dart';

import 'api.dart';
import 'telas/carteira.dart';
import 'telas/entrar.dart';

class LuviApp extends StatefulWidget {
  const LuviApp({super.key, required this.api, this.escanear});

  final LuviApi api;
  final Future<String?> Function(BuildContext context)? escanear;

  @override
  State<LuviApp> createState() => _LuviAppState();
}

class _LuviAppState extends State<LuviApp> {
  bool _pronto = false;
  bool _dentro = false;

  @override
  void initState() {
    super.initState();
    widget.api.restaurar().then((dentro) {
      if (!mounted) return;
      setState(() { _pronto = true; _dentro = dentro; });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LUVI Fideliza+',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1D3E5B), primary: const Color(0xFF1D3E5B)),
        scaffoldBackgroundColor: const Color(0xFFFCFAF8),
        useMaterial3: true,
      ),
      home: !_pronto
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : _dentro
              ? CarteiraPage(
                  api: widget.api,
                  escanear: widget.escanear,
                  aoSair: () async {
                    await widget.api.sair();
                    if (mounted) setState(() => _dentro = false);
                  },
                )
              : EntrarPage(api: widget.api, aoEntrar: () => setState(() => _dentro = true)),
    );
  }
}
