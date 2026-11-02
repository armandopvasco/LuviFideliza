package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.equipe.MembroRepository;
import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.usuario.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PainelController {

    private final MembroRepository membros;
    private final PermissaoEstabelecimentoService permissoes;

    @PersistenceContext
    private EntityManager entityManager;

    public PainelController(
            MembroRepository membros,
            PermissaoEstabelecimentoService permissoes) {
        this.membros = membros;
        this.permissoes = permissoes;
    }

    public record Negocio(
            UUID id,
            String nome,
            boolean podeOperar) {
    }

    @GetMapping("/api/v1/painel/estabelecimentos")
    @Transactional(readOnly = true)
    public List<Negocio> estabelecimentos(Authentication autenticacao) {

        Usuario usuario = permissoes.usuarioAtivo(autenticacao.getName());

        List<Estabelecimento> estabelecimentos;

        if (usuario.getAdministradorPlataforma()) {
            estabelecimentos = entityManager.createQuery("""
                    select e
                    from Estabelecimento e
                    where e.ativo = true
                    order by e.nome
                    """, Estabelecimento.class)
                    .setMaxResults(100)
                    .getResultList();
        } else {
            estabelecimentos = entityManager.createQuery("""
                    select e
                    from Estabelecimento e
                    where e.ativo = true
                      and exists (
                          select m.id
                          from MembroEstabelecimento m
                          where m.estabelecimentoId = e.id
                            and m.usuarioId = :usuario
                            and m.ativo = true
                      )
                    order by e.nome
                    """, Estabelecimento.class)
                    .setParameter("usuario", usuario.getId())
                    .setMaxResults(100)
                    .getResultList();
        }

        return estabelecimentos.stream()
                .map(e -> new Negocio(
                        e.getId(),
                        e.getNome(),
                        membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(
                                usuario.getId(), e.getId())))
                .toList();
    }
}