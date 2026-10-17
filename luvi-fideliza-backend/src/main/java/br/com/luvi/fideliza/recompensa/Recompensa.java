package br.com.luvi.fideliza.recompensa;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "recompensa",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_recompensa_resgate",
                columnNames = {"estabelecimento_id", "chave_resgate"}
        )
)
public class Recompensa {

    @Id
    private UUID id;

    @Column(name = "cartao_id", nullable = false,
            unique = true, updatable = false)
    private UUID cartaoId;

    @Column(name = "estabelecimento_id",
            nullable = false, updatable = false)
    private UUID estabelecimentoId;

    @Column(name = "emitida_em", nullable = false, updatable = false)
    private Instant emitidaEm;

    @Column(name = "resgatada_em")
    private Instant resgatadaEm;

    @Column(name = "resgatada_por")
    private UUID resgatadaPor;

    @Column(name = "chave_resgate")
    private UUID chaveResgate;

    protected Recompensa() {
    }

    public Recompensa(
            UUID cartaoId,
            UUID estabelecimentoId,
            Instant emitidaEm) {

        this.id = cartaoId;
        this.cartaoId = cartaoId;
        this.estabelecimentoId = estabelecimentoId;
        this.emitidaEm = emitidaEm;
    }

    public void resgatar(UUID autor, UUID chave, Instant agora) {
        if (resgatadaEm != null) {
            throw new IllegalStateException("Recompensa já resgatada");
        }

        this.resgatadaEm = agora;
        this.resgatadaPor = autor;
        this.chaveResgate = chave;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCartaoId() {
        return cartaoId;
    }

    public UUID getEstabelecimentoId() {
        return estabelecimentoId;
    }

    public Instant getEmitidaEm() {
        return emitidaEm;
    }

    public Instant getResgatadaEm() {
        return resgatadaEm;
    }
}