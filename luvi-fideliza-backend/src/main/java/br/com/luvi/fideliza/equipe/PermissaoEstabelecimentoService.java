package br.com.luvi.fideliza.equipe;

import br.com.luvi.fideliza.usuario.Usuario;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class PermissaoEstabelecimentoService {
  private final UsuarioRepository usuarios;
  private final MembroRepository membros;

  public PermissaoEstabelecimentoService(UsuarioRepository usuarios, MembroRepository membros) {
    this.usuarios = usuarios;
    this.membros = membros;
  }

  public Usuario exigirLeitura(UUID estabelecimentoId, String email) {
    Usuario usuario = usuarioAtivo(email);
    if (!usuario.getAdministradorPlataforma()
        && !membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(
            usuario.getId(), estabelecimentoId)) {
      throw new AccessDeniedException("Acesso negado");
    }
    return usuario;
  }

  public Usuario exigirProprietario(UUID estabelecimentoId, String email) {
    Usuario usuario = usuarioAtivo(email);
    // Ser administrador da plataforma não concede edição das regras do comerciante.
    if (!membros.existsByUsuarioIdAndEstabelecimentoIdAndPapelAndAtivoTrue(
        usuario.getId(), estabelecimentoId, "PROPRIETARIO")) {
      throw new AccessDeniedException("Somente o proprietário pode alterar o programa");
    }
    return usuario;
  }

  public Usuario exigirOperacao(UUID estabelecimentoId, String email) {
    Usuario usuario = usuarioAtivo(email);
    if (!membros.existsByUsuarioIdAndEstabelecimentoIdAndAtivoTrue(
        usuario.getId(), estabelecimentoId)) {
      throw new AccessDeniedException(
          "Somente a equipe do estabelecimento pode registrar operações");
    }
    return usuario;
  }

  private Usuario usuarioAtivo(String email) {
    return usuarios
        .findByEmailIgnoreCase(email)
        .filter(Usuario::getAtivo)
        .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
  }
}
