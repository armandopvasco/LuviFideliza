package br.com.luvi.fideliza.recompensa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RecompensaRepository
        extends JpaRepository<Recompensa, UUID> {

    Optional<Recompensa> findByEstabelecimentoIdAndChaveResgate(
            UUID estabelecimentoId,
            UUID chaveResgate);

    @Query("""
            select r
            from Recompensa r, CartaoFidelidade c
            where r.cartaoId = c.id
              and c.participacaoId = :participacao
            """)
    Page<Recompensa> porParticipante(
            @Param("participacao") UUID participacao,
            Pageable pagina);

    @Query("""
            select r
            from Recompensa r, CartaoFidelidade c, Participacao p
            where r.cartaoId = c.id
              and c.participacaoId = p.id
              and p.usuarioId = :usuario
            """)
    Page<Recompensa> doCliente(
            @Param("usuario") UUID usuario,
            Pageable pagina);
}