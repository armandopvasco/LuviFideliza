class Loja {
  const Loja({required this.id, required this.nome, required this.podeOperar});

  final String id;
  final String nome;
  final bool podeOperar;
}

class Campanha {
  const Campanha({
    required this.id,
    required this.titulo,
    required this.meta,
    required this.recompensa,
    required this.ativa,
    required this.principal,
  });

  final String id;
  final String titulo;
  final int meta;
  final String recompensa;
  final bool ativa;
  final bool principal;
}

class ClienteBalcao {
  const ClienteBalcao({
    required this.id,
    required this.nome,
    required this.whatsapp,
    required this.titulo,
    required this.recompensa,
    required this.saldo,
    required this.meta,
  });

  final String id;
  final String nome;
  final String whatsapp;
  final String titulo;
  final String recompensa;
  final int saldo;
  final int meta;
}

class RecompensaBalcao {
  const RecompensaBalcao({required this.id, required this.descricao, required this.status});

  final String id;
  final String descricao;
  final String status;
}

class ResultadoCarimbo {
  const ResultadoCarimbo({required this.nome, required this.saldo, required this.concluiu, required this.cadastrado});

  final String nome;
  final int saldo;
  final bool concluiu;
  final bool cadastrado;
}

class CodigoBalcao {
  const CodigoBalcao({required this.lojaId, this.participacaoId, this.recompensaId});

  final String lojaId;
  final String? participacaoId;
  final String? recompensaId;
}

CodigoBalcao? lerCodigo(String texto) {
  final bruto = texto.trim();
  if (bruto.isEmpty) return null;
  var uri = Uri.tryParse(bruto);
  if (uri == null || uri.queryParameters.isEmpty) {
    uri = Uri.tryParse(bruto.startsWith('/') ? 'http://local$bruto' : 'http://local/$bruto');
  }
  if (uri == null) return null;
  final loja = uri.queryParameters['loja'];
  final participacao = uri.queryParameters['participacao'];
  final recompensa = uri.queryParameters['recompensa'];
  if (loja == null || loja.isEmpty) return null;
  if (participacao != null && participacao.isNotEmpty) {
    return CodigoBalcao(lojaId: loja, participacaoId: participacao);
  }
  if (recompensa != null && recompensa.isNotEmpty) {
    return CodigoBalcao(lojaId: loja, recompensaId: recompensa);
  }
  return null;
}
