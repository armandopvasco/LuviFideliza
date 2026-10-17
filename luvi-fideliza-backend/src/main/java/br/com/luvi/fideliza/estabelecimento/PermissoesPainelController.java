package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.equipe.MembroRepository;
import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class PermissoesPainelController {
  private final PermissaoEstabelecimentoService permissoes;
  private final MembroRepository membros;
  private final EstabelecimentoRepository estabelecimentos;

  public PermissoesPainelController(
      PermissaoEstabelecimentoService permissoes,
      MembroRepository membros,
      EstabelecimentoRepository estabelecimentos) {
    this.permissoes = permissoes;
    this.membros = membros;
    this.estabelecimentos = estabelecimentos;
  }

  @GetMapping("/api/v1/painel/estabelecimentos/{id}/permissoes")
  @Transactional(readOnly = true)
  public Permissoes consultar(@PathVariable UUID id, Authentication authentication) {
    var usuario = permissoes.exigirLeitura(id, authentication.getName());
    estabelecimentos.findById(id).filter(Estabelecimento::getAtivo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return new Permissoes(
        membros.existsByUsuarioIdAndEstabelecimentoIdAndPapelAndAtivoTrue(
            usuario.getId(), id, "PROPRIETARIO"),
        membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(usuario.getId(), id));
  }

  public record Permissoes(boolean podeEditarPrograma, boolean podeOperar) {}
}
