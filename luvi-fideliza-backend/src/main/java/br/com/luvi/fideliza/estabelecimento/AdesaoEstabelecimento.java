package br.com.luvi.fideliza.estabelecimento;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name="adesao_estabelecimento")
@IdClass(AdesaoEstabelecimento.Chave.class)
public class AdesaoEstabelecimento {
  @Id @Column(name="estabelecimento_id") public UUID estabelecimentoId;
  @Id @Column(name="usuario_id") public UUID usuarioId;
  @Column(name="criado_em") public Instant criadoEm;
  protected AdesaoEstabelecimento() {}
  public AdesaoEstabelecimento(UUID e, UUID u) { estabelecimentoId=e; usuarioId=u; criadoEm=Instant.now(); }
  public static class Chave implements java.io.Serializable {
    public UUID estabelecimentoId;
    public UUID usuarioId;
    public Chave() {}
    public Chave(UUID e, UUID u) { estabelecimentoId=e; usuarioId=u; }
    public boolean equals(Object o) { return o instanceof Chave c && java.util.Objects.equals(estabelecimentoId,c.estabelecimentoId) && java.util.Objects.equals(usuarioId,c.usuarioId); }
    public int hashCode() { return java.util.Objects.hash(estabelecimentoId,usuarioId); }
  }
}
