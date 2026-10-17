package br.com.luvi.fideliza.fidelidade.repository;

import br.com.luvi.fideliza.fidelidade.entity.MovimentacaoCarimbo;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarimboRepository extends JpaRepository<MovimentacaoCarimbo, UUID> {
  Optional<MovimentacaoCarimbo> findByEstabelecimentoIdAndChaveOperacao(
      UUID estabelecimento, UUID chave);

  Page<MovimentacaoCarimbo> findByParticipacaoId(UUID participacao, Pageable pagina);
}
