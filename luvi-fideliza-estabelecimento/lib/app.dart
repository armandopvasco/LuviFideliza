import 'package:flutter/material.dart';

import 'api.dart';
import 'telas/balcao.dart';
import 'telas/entrar.dart';

class BalcaoApp extends StatefulWidget {
  const BalcaoApp({super.key, required this.api, this.escanear});

  final BalcaoApi api;
  final Future<String?> Function(BuildContext context)? escanear;

  @override
  State<BalcaoApp> createState() => _BalcaoAppState();
}

class _BalcaoAppState extends State<BalcaoApp> {
  bool _pronto = false;
  bool _dentro = false;

  @override
  void initState() {
    super.initState();
    widget.api.restaurar().then((dentro) {
      if (!mounted) return;
      setState(() {
        _pronto = true;
        _dentro = dentro;
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'LUVI Balcão',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF1D3E5B), primary: const Color(0xFF1D3E5B)),
        scaffoldBackgroundColor: const Color(0xFFFCFAF8),
        useMaterial3: true,
      ),
      home: !_pronto
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : _dentro
              ? BalcaoPage(
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
