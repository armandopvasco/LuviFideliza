package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.Pagina;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/painel/estabelecimentos/{id}")
@Transactional(readOnly = true)
public class VisaoPainelController {
  private final PermissaoEstabelecimentoService permissoes;
  private final EstabelecimentoRepository estabelecimentos;
  private final EntityManager em;

  public VisaoPainelController(
      PermissaoEstabelecimentoService permissoes,
      EstabelecimentoRepository estabelecimentos,
      EntityManager em) {
    this.permissoes = permissoes;
    this.estabelecimentos = estabelecimentos;
    this.em = em;
  }

  public record Resumo(
      long participantes,
      long contasVinculadas,
      long cartoesEmAndamento,
      long recompensasDisponiveis,
      long recompensasResgatadas,
      long carimbos) {}

  public record RecompensaLinha(
      UUID id,
      UUID participacaoId,
      String participante,
      int ciclo,
      String descricao,
      String status,
      Instant emitidaEm,
      Instant resgatadaEm) {}

  @GetMapping("/resumo")
  public Resumo resumo(@PathVariable UUID id, Authentication auth) {
    validar(id, auth);
    return new Resumo(
        contar("select count(p) from Participacao p where p.estabelecimentoId = :id", id),
        contar(
            "select count(p) from Participacao p where p.estabelecimentoId = :id and p.usuarioId is"
                + " not null",
            id),
        contar(
            "select count(c) from CartaoFidelidade c, Participacao p where c.participacaoId = p.id"
                + " and p.estabelecimentoId = :id and c.concluidoEm is null",
            id),
        contar(
            "select count(r) from Recompensa r where r.estabelecimentoId = :id and r.resgatadaEm is"
                + " null",
            id),
        contar(
            "select count(r) from Recompensa r where r.estabelecimentoId = :id and r.resgatadaEm is"
                + " not null",
            id),
        contar("select count(m) from MovimentacaoCarimbo m where m.estabelecimentoId = :id", id));
  }

  @GetMapping("/recompensas")
  public Pagina<RecompensaLinha> recompensas(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "TODAS") String status,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "12") int tamanho,
      Authentication auth) {
    validar(id, auth);
    if (pagina < 0
        || tamanho < 1
        || tamanho > 100
        || !java.util.Set.of("TODAS", "DISPONIVEL", "RESGATADA").contains(status))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Filtro ou paginação inválida");
    String from =
        " from Recompensa r, CartaoFidelidade c, Participacao p, VersaoPrograma v"
            + " where r.cartaoId = c.id and c.participacaoId = p.id and c.versaoProgramaId = v.id"
            + " and r.estabelecimentoId = :id";
    if (status.equals("DISPONIVEL")) from += " and r.resgatadaEm is null";
    if (status.equals("RESGATADA")) from += " and r.resgatadaEm is not null";
    long total = contar("select count(r)" + from, id);
    var linhas =
        em
            .createQuery(
                "select r.id, p.id, p.nomeExibicao, c.numero, v.recompensa, r.emitidaEm,"
                    + " r.resgatadaEm"
                    + from
                    + " order by r.emitidaEm desc, r.id",
                Object[].class)
            .setParameter("id", id)
            .setFirstResult(Math.toIntExact((long) pagina * tamanho))
            .setMaxResults(tamanho)
            .getResultList()
            .stream()
            .map(
                r ->
                    new RecompensaLinha(
                        (UUID) r[0],
                        (UUID) r[1],
                        (String) r[2],
                        (Integer) r[3],
                        (String) r[4],
                        r[6] == null ? "DISPONIVEL" : "RESGATADA",
                        (Instant) r[5],
                        (Instant) r[6]))
            .toList();
    return new Pagina<>(linhas, pagina, tamanho, total);
  }

  private long contar(String jpql, UUID id) {
    return em.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
  }

  private void validar(UUID id, Authentication auth) {
    permissoes.exigirLeitura(id, auth.getName());
    estabelecimentos
        .findById(id)
        .filter(Estabelecimento::getAtivo)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Estabelecimento não encontrado"));
  }
}
