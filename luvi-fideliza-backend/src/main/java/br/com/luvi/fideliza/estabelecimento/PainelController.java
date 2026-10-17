package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.equipe.MembroRepository;
import br.com.luvi.fideliza.usuario.Usuario;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PainelController {

    private final UsuarioRepository usuarios;
    private final MembroRepository membros;

    @PersistenceContext
    private EntityManager entityManager;

    public PainelController(
            UsuarioRepository usuarios,
            MembroRepository membros) {
        this.usuarios = usuarios;
        this.membros = membros;
    }

    public record Negocio(
            UUID id,
            String nome,
            boolean podeOperar) {
    }

    @GetMapping("/api/v1/painel/estabelecimentos")
    @Transactional(readOnly = true)
    public List<Negocio> estabelecimentos(Authentication autenticacao) {

        Usuario usuario = usuarios
                .findByEmailIgnoreCase(autenticacao.getName())
                .filter(Usuario::getAtivo)
                .orElseThrow(() -> new AccessDeniedException("Acesso negado"));

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