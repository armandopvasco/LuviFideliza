package br.com.luvi.fideliza.fidelidade.dto;

import br.com.luvi.fideliza.fidelidade.entity.ProgramaFidelidade;
import br.com.luvi.fideliza.fidelidade.entity.VersaoPrograma;
import java.time.Instant;
import java.util.UUID;

public record ProgramaResponse(
    UUID id,
    UUID estabelecimentoId,
    UUID versaoId,
    int versao,
    String titulo,
    int metaCarimbos,
    String recompensa,
    String criteriosCompra,
    Instant criadoEm,
    UUID criadoPor) {
  public static ProgramaResponse de(ProgramaFidelidade programa, VersaoPrograma versao) {
    return new ProgramaResponse(
        programa.getId(),
        programa.getEstabelecimentoId(),
        versao.getId(),
        versao.getNumero(),
        versao.getTitulo(),
        versao.getMetaCarimbos(),
        versao.getRecompensa(),
        versao.getCriteriosCompra(),
        versao.getCriadoEm(),
        versao.getCriadoPor());
  }
}
