package br.com.luvi.fideliza.equipe;
import jakarta.persistence.*;
import java.util.UUID;
import java.time.Instant;
@Entity @Table(name="membro_estabelecimento")
public class MembroEstabelecimento {
 @Id
 private UUID id;
 @Column(name="usuario_id", nullable=false)
 private UUID usuarioId;
 @Column(name="estabelecimento_id", nullable=false)
 private UUID estabelecimentoId;
 @Column(nullable=false, length=20)
 private String papel;
 @Column(nullable=false)
 private boolean ativo;
 protected MembroEstabelecimento() {}
 public MembroEstabelecimento(UUID id, UUID usuarioId, UUID estabelecimentoId, String papel, boolean ativo) {
  this.id=id;
  this.usuarioId=usuarioId;
  this.estabelecimentoId=estabelecimentoId;
  this.papel=papel;
  this.ativo=ativo;
 }
 public UUID getId() { return id; }
 public UUID getUsuarioId() { return usuarioId; }
 public UUID getEstabelecimentoId() { return estabelecimentoId; }
 public String getPapel() { return papel; }
 public boolean getAtivo() { return ativo; }
}
