package br.com.luvi.fideliza.equipe;

import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConviteEquipeRepository extends JpaRepository<ConviteEquipe, UUID> {
  Optional<ConviteEquipe> findByEstabelecimentoIdAndEmail(UUID estabelecimento, String email);

  Optional<ConviteEquipe> findByTokenHash(String hash);

  List<ConviteEquipe> findByEstabelecimentoIdOrderByEmail(UUID estabelecimento);

  @org.springframework.data.jpa.repository.Query(
      "select c.estabelecimentoId from ConviteEquipe c where c.tokenHash = :hash")
  Optional<UUID> estabelecimentoDoToken(
      @org.springframework.data.repository.query.Param("hash") String hash);
}
