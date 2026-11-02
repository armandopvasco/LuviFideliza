class VisualCartao {
  const VisualCartao({
    this.corCartao = '#30475A',
    this.corCarimbo = '#87A18F',
    this.icone = 'estrela',
    this.temFoto = false,
  });

  final String corCartao;
  final String corCarimbo;
  final String icone;
  final bool temFoto;

  static const padrao = VisualCartao();
}

class CartaoCliente {
  const CartaoCliente({
    required this.id,
    required this.participacaoId,
    required this.estabelecimentoId,
    required this.estabelecimento,
    required this.titulo,
    required this.recompensa,
    required this.meta,
    required this.saldo,
    required this.status,
  });

  final String id;
  final String participacaoId;
  final String estabelecimentoId;
  final String estabelecimento;
  final String titulo;
  final String recompensa;
  final int meta;
  final int saldo;
  final String status;

  bool get concluido => status.toUpperCase() == 'CONCLUIDO' || (meta > 0 && saldo >= meta);
}

class RecompensaCliente {
  const RecompensaCliente({
    required this.id,
    required this.cartaoId,
    required this.descricao,
    required this.status,
  });

  final String id;
  final String cartaoId;
  final String descricao;
  final String status;

  bool get disponivel => status.toUpperCase() == 'DISPONIVEL';
}

String? lojaDoTexto(String texto) {
  final limpo = texto.trim();
  if (limpo.isEmpty) return null;
  final uri = Uri.tryParse(limpo);
  final query = uri?.queryParameters['loja'];
  if (query != null && _uuid.hasMatch(query)) return query;
  return _uuid.firstMatch(limpo)?.group(0);
}

final _uuid = RegExp(
  r'[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}',
);
