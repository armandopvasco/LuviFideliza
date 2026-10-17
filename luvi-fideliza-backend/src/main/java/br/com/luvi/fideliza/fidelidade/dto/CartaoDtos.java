package br.com.luvi.fideliza.fidelidade.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CartaoDtos {
  private CartaoDtos() {}

  public record CadastrarParticipante(@NotBlank @Size(max = 120) String nomeExibicao) {}

  public record RegistrarCarimbo(@NotBlank @Size(max = 120) String referenciaCompra) {}

  public record AceitarConvite(@NotBlank @Size(max = 100) String token) {}

  public record Participante(
      UUID id, UUID estabelecimentoId, String nomeExibicao, boolean vinculadoAoCliente) {}

  public record Cartao(
      UUID id,
      UUID participacaoId,
      UUID estabelecimentoId,
      String estabelecimento,
      int ciclo,
      UUID versaoId,
      int versao,
      String titulo,
      String recompensa,
      String criteriosCompra,
      int metaCarimbos,
      int saldo,
      String status,
      Instant abertoEm,
      Instant concluidoEm) {}

  public record Cadastro(Participante participante, Cartao cartao) {}

  public record Convite(String token, Instant expiraEm) {}

  public record Carimbo(
      UUID id,
      UUID participacaoId,
      UUID cartaoId,
      UUID chaveOperacao,
      String referenciaCompra,
      int saldoApos,
      boolean concluiuCartao,
      Instant registradoEm,
      UUID registradoPor) {}

  public record Resultado<T>(T dados, boolean repetido) {}

  public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long total) {}
}
