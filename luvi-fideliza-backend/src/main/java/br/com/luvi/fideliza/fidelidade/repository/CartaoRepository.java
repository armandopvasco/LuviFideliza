package br.com.luvi.fideliza.fidelidade.repository;

import br.com.luvi.fideliza.fidelidade.entity.CartaoFidelidade;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface CartaoRepository extends JpaRepository<CartaoFidelidade, UUID> {
  Optional<CartaoFidelidade> findTopByParticipacaoIdOrderByNumeroDesc(UUID participacao);

  Optional<CartaoFidelidade> findByParticipacaoIdAndNumero(UUID participacao, int numero);

  Page<CartaoFidelidade> findByParticipacaoId(UUID participacao, Pageable pagina);

  @Query(
      "select c from CartaoFidelidade c, Participacao p where c.participacaoId=p.id and"
          + " p.usuarioId=:usuario")
  Page<CartaoFidelidade> carteira(@Param("usuario") UUID usuario, Pageable pagina);
}
