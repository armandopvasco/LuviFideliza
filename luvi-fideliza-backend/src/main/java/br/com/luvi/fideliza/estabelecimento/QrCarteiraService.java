package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.fidelidade.entity.Participacao;
import br.com.luvi.fideliza.fidelidade.repository.CartaoRepository;
import br.com.luvi.fideliza.fidelidade.repository.ParticipacaoRepository;
import br.com.luvi.fideliza.recompensa.Recompensa;
import br.com.luvi.fideliza.recompensa.RecompensaRepository;
import br.com.luvi.fideliza.usuario.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class QrCarteiraService {
  private final ParticipacaoRepository participacoes;
  private final CartaoRepository cartoes;
  private final RecompensaRepository recompensas;
  private final PermissaoEstabelecimentoService permissoes;
  private final QrLinks links;

  public QrCarteiraService(
      ParticipacaoRepository participacoes,
      CartaoRepository cartoes,
      RecompensaRepository recompensas,
      PermissaoEstabelecimentoService permissoes,
      QrLinks links) {
    this.participacoes = participacoes;
    this.cartoes = cartoes;
    this.recompensas = recompensas;
    this.permissoes = permissoes;
    this.links = links;
  }

  public record Link(String url) {}

  @Transactional(readOnly = true)
  public Link participacaoDoCliente(UUID participacaoId, String identificador, HttpServletRequest request) {
    Participacao participacao = participacaoDoCliente(participacaoId, identificador);
    return new Link(links.atendimento(request, participacao.getEstabelecimentoId(), participacao.getId()));
  }

  @Transactional(readOnly = true)
  public Link recompensaDoCliente(UUID recompensaId, String identificador, HttpServletRequest request) {
    Recompensa recompensa = recompensaDoCliente(recompensaId, identificador);
    return new Link(links.recompensa(request, recompensa.getEstabelecimentoId(), recompensa.getId()));
  }

  @Transactional(readOnly = true)
  public Link participacaoDaEquipe(
      UUID estabelecimentoId, UUID participacaoId, String identificador, HttpServletRequest request) {
    permissoes.exigirLeitura(estabelecimentoId, identificador);
    Participacao participacao =
        participacoes
            .findById(participacaoId)
            .filter(p -> p.getEstabelecimentoId().equals(estabelecimentoId))
            .orElseThrow(QrCarteiraService::ausente);
    return new Link(links.atendimento(request, estabelecimentoId, participacao.getId()));
  }

  private Participacao participacaoDoCliente(UUID participacaoId, String identificador) {
    Usuario usuario = permissoes.usuarioAtivo(identificador);
    return participacoes
        .findById(participacaoId)
        .filter(p -> usuario.getId().equals(p.getUsuarioId()))
        .orElseThrow(QrCarteiraService::ausente);
  }

  private Recompensa recompensaDoCliente(UUID recompensaId, String identificador) {
    Usuario usuario = permissoes.usuarioAtivo(identificador);
    Recompensa recompensa =
        recompensas.findById(recompensaId).orElseThrow(QrCarteiraService::ausente);
    var cartao = cartoes.findById(recompensa.getCartaoId()).orElseThrow(QrCarteiraService::ausente);
    participacoes
        .findById(cartao.getParticipacaoId())
        .filter(p -> usuario.getId().equals(p.getUsuarioId()))
        .orElseThrow(QrCarteiraService::ausente);
    return recompensa;
  }

  private static ResponseStatusException ausente() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "QR Code não encontrado");
  }
}
