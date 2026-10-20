package br.com.luvi.fideliza.estabelecimento;

import br.com.luvi.fideliza.usuario.*;
import br.com.luvi.fideliza.fidelidade.entity.*;
import br.com.luvi.fideliza.fidelidade.repository.*;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AdesaoPublicaController {
  private final EstabelecimentoRepository estabelecimentos;
  private final UsuarioRepository usuarios;
  private final AdesaoRepository adesoes;
  private final ParticipacaoRepository participacoes;
  private final ProgramaRepository programas;
  private final VersaoProgramaRepository versoes;
  private final CartaoRepository cartoes;
  public AdesaoPublicaController(EstabelecimentoRepository e, UsuarioRepository u, AdesaoRepository a,
      ParticipacaoRepository p, ProgramaRepository pr, VersaoProgramaRepository v, CartaoRepository c) {
    estabelecimentos=e; usuarios=u; adesoes=a; participacoes=p; programas=pr; versoes=v; cartoes=c;
  }
  public record Loja(UUID id,String nome) {}
  @GetMapping("/api/v1/publico/estabelecimentos/{id}")
  public Loja loja(@PathVariable UUID id) {
    var e=estabelecimentos.findById(id).filter(Estabelecimento::getAtivo)
      .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Estabelecimento indisponível"));
    return new Loja(e.getId(),e.getNome());
  }
  @PostMapping("/api/v1/me/estabelecimentos/{id}/aderir")
  @Transactional
  public Loja aderir(@PathVariable UUID id, Authentication auth) {
    var e=estabelecimentos.buscarParaAlteracao(id).filter(Estabelecimento::getAtivo)
      .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Estabelecimento indisponível"));
    var usuario=(auth.getName().contains("@") ? usuarios.findByEmailIgnoreCase(auth.getName()) : usuarios.findByTelefone(auth.getName()))
      .orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    var chave=new AdesaoEstabelecimento.Chave(id,usuario.getId());
    if(!adesoes.existsById(chave)) adesoes.save(new AdesaoEstabelecimento(id,usuario.getId()));
    var programa=programas.findByEstabelecimentoIdAndPrincipalTrue(id);
    if(programa.isPresent() && participacoes.findByProgramaIdAndUsuarioId(programa.get().getId(),usuario.getId()).isEmpty()) {
      var existente=usuario.getTelefone()==null ? Optional.<Participacao>empty() : participacoes.findByProgramaIdAndWhatsapp(programa.get().getId(),usuario.getTelefone());
      if(existente.isPresent() && existente.get().getUsuarioId()==null) {
        existente.get().vincular(usuario.getId());
      } else if(existente.isEmpty()) {
        var p=new Participacao(UUID.randomUUID(),id,programa.get().getId(),usuario.getId(),usuario.getNome(),UUID.randomUUID(),Instant.now(),usuario.getId(),null,null);
        p.definirWhatsapp(usuario.getTelefone());
        participacoes.save(p);
        var v=versoes.findByProgramaIdAndNumero(programa.get().getId(),programa.get().getVersaoAtual()).orElseThrow();
        cartoes.save(new CartaoFidelidade(UUID.randomUUID(),p.getId(),v.getId(),1,0,Instant.now(),null));
      }
    }
    return new Loja(e.getId(),e.getNome());
  }
}
