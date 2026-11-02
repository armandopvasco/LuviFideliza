import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../modelos.dart';

const _simbolos = {
  'estrela': '★',
  'coracao': '♥',
  'xicara': '☕',
  'tesoura': '✂',
  'presente': '🎁',
  'coroa': '♛',
  'flor': '✿',
  'folha': '❧',
  'sol': '☀',
  'lua': '☾',
  'raio': 'ϟ',
  'diamante': '◆',
  'trofeu': '🏆',
  'fogo': '🔥',
  'pizza': '🍕',
  'hamburguer': '🍔',
  'bolo': '🎂',
  'sorvete': '🍦',
  'pao': '🥐',
  'livro': '📖',
  'musica': '♫',
  'bola': '⚽',
  'pata': '🐾',
  'carro': '🚗',
  'bicicleta': '🚲',
  'halter': '🏋',
  'beleza': '💅',
  'check': '✓',
  'sorriso': '☺',
};

Color corHex(String hex, Color padrao) {
  final limpo = hex.replaceFirst('#', '').trim();
  if (limpo.length != 6) return padrao;
  final valor = int.tryParse(limpo, radix: 16);
  if (valor == null) return padrao;
  return Color(0xFF000000 | valor);
}

class CartaoVisual extends StatelessWidget {
  const CartaoVisual({super.key, required this.cartao, required this.visual, this.foto, this.premioDisponivel = false});

  final CartaoCliente cartao;
  final VisualCartao visual;
  final Uint8List? foto;
  final bool premioDisponivel;

  @override
  Widget build(BuildContext context) {
    final cor = corHex(visual.corCartao, const Color(0xFF30475A));
    final carimbo = corHex(visual.corCarimbo, const Color(0xFF87A18F));
    final simbolo = _simbolos[visual.icone] ?? '★';
    final total = cartao.meta.clamp(1, 40);
    final saldo = cartao.saldo.clamp(0, total);
    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: cor,
        borderRadius: BorderRadius.circular(18),
        image: foto == null
            ? null
            : DecorationImage(
                image: MemoryImage(foto!),
                fit: BoxFit.cover,
                colorFilter: const ColorFilter.mode(Color(0xB3102638), BlendMode.srcATop),
              ),
        boxShadow: const [BoxShadow(color: Color(0x331A2E3B), blurRadius: 18, offset: Offset(0, 8))],
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(cartao.estabelecimento.toUpperCase(), style: const TextStyle(color: Colors.white70, letterSpacing: 0.8, fontSize: 12)),
            const SizedBox(height: 6),
            Text(cartao.titulo, style: const TextStyle(color: Colors.white, fontSize: 26, fontWeight: FontWeight.w700)),
            const SizedBox(height: 4),
            Text('Recompensa: ${cartao.recompensa}', style: const TextStyle(color: Colors.white)),
            const SizedBox(height: 16),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (var i = 0; i < total; i++)
                  Container(
                    width: 40,
                    height: 40,
                    alignment: Alignment.center,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: i < saldo ? carimbo : Colors.transparent,
                      border: Border.all(color: const Color(0xE6FFFFFF), width: 2),
                    ),
                    child: Text(i < saldo ? '✓' : simbolo, style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.w700)),
                  ),
              ],
            ),
            const SizedBox(height: 12),
            Text('${cartao.saldo} de ${cartao.meta} carimbos', style: const TextStyle(color: Colors.white70)),
            if (premioDisponivel) ...[
              const SizedBox(height: 16),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(color: const Color(0xFFE5F3E8), borderRadius: BorderRadius.circular(12)),
                child: Text('Prêmio disponível: ${cartao.recompensa}', style: const TextStyle(color: Color(0xFF245D39), fontWeight: FontWeight.w700)),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
