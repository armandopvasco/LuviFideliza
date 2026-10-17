package br.com.luvi.fideliza.fidelidade.repository;

import br.com.luvi.fideliza.fidelidade.entity.Participacao;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ParticipacaoRepository extends JpaRepository<Participacao, UUID> {
  Optional<Participacao> findByEstabelecimentoIdAndChaveCadastro(UUID estabelecimento, UUID chave);

  Optional<Participacao> findByProgramaIdAndUsuarioId(UUID programa, UUID usuario);

  Optional<Participacao> findByConviteHash(String hash);

  Page<Participacao> findByEstabelecimentoId(UUID estabelecimento, Pageable pagina);

  @Query(
      "select p from Participacao p where p.estabelecimentoId = :estabelecimento and"
          + " lower(p.nomeExibicao) like :busca escape '!'")
  Page<Participacao> pesquisar(
      @Param("estabelecimento") UUID estabelecimento,
      @Param("busca") String busca,
      Pageable pagina);

  // A primeira busca não carrega a entidade: depois do lock, o hash é revalidado sem cache
  // obsoleto.
  @Query("select p.estabelecimentoId from Participacao p where p.conviteHash = :hash")
  Optional<UUID> estabelecimentoDoConvite(@Param("hash") String hash);
}
