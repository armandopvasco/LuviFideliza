package br.com.luvi.fideliza.fidelidade.dto;

import jakarta.validation.constraints.*;

public record AtualizarProgramaRequest(
    @NotNull @Min(1) Integer versaoAtual,
    @NotBlank @Size(max = 120) String titulo,
    @NotNull @Min(1) @Max(1000) Integer metaCarimbos,
    @NotBlank @Size(max = 200) String recompensa,
    @NotBlank @Size(max = 500) String criteriosCompra) {
  public ProgramaRequest regras() {
    return new ProgramaRequest(titulo, metaCarimbos, recompensa, criteriosCompra);
  }
}
