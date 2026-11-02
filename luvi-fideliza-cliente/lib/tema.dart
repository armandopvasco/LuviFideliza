import 'package:flutter/material.dart';

const marca = Color(0xFF1D3E5B);
const fundo = Color(0xFFD5E0E8);
const erro = Color(0xFFA12B32);

ThemeData temaLuvi() {
  return ThemeData(
    colorScheme: ColorScheme.fromSeed(seedColor: marca, primary: marca, surface: Colors.white),
    scaffoldBackgroundColor: fundo,
    appBarTheme: const AppBarTheme(
      backgroundColor: marca,
      foregroundColor: Colors.white,
      elevation: 0,
      iconTheme: IconThemeData(color: Colors.white),
    ),
    useMaterial3: true,
  );
}

class BotaoSair extends StatelessWidget {
  const BotaoSair({super.key, required this.aoSair});

  final Future<void> Function() aoSair;

  @override
  Widget build(BuildContext context) {
    return IconButton(
      tooltip: 'Sair',
      onPressed: () { aoSair(); },
      icon: const Icon(Icons.logout),
    );
  }
}
