package br.com.luvi.fideliza.fidelidade.controller;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.dto.*;
import br.com.luvi.fideliza.fidelidade.entity.*;
import br.com.luvi.fideliza.fidelidade.repository.*;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import br.com.luvi.fideliza.fidelidade.repository.ParticipacaoRepository;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/estabelecimentos/{estabelecimentoId}")
public class CampanhaController {
  private final ProgramaRepository programas;
  private final VersaoProgramaRepository versoes;
  private final EstabelecimentoRepository estabelecimentos;
  private final PermissaoEstabelecimentoService permissoes;
  private final UsuarioRepository usuarios;
  private final ParticipacaoRepository participacoes;

  public CampanhaController(ProgramaRepository programas, VersaoProgramaRepository versoes,
      EstabelecimentoRepository estabelecimentos, PermissaoEstabelecimentoService permissoes,
      UsuarioRepository usuarios, ParticipacaoRepository participacoes) {
    this.programas = programas; this.versoes = versoes;
    this.estabelecimentos = estabelecimentos; this.permissoes = permissoes;
    this.usuarios = usuarios; this.participacoes = participacoes;
  }

  public record Configuracao(boolean exibirTodasCampanhas) {}
  public record VisualCartao(String corCartao, String corCarimbo,
      String iconeCarimbo, boolean fotoFundoCartao) {}
  private static final Set<String> ICONES = Set.of("estrela", "coracao", "xicara", "tesoura",
      "presente", "coroa", "flor", "folha", "sol", "lua", "raio", "diamante", "trofeu",
      "fogo", "pizza", "hamburguer", "bolo", "sorvete", "pao", "livro", "musica",
      "bola", "pata", "carro", "bicicleta", "halter", "beleza", "check", "sorriso");
  private static String validarCor(String cor) {
    if (cor == null || !cor.matches("#[0-9a-fA-F]{6}"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cor inválida: informe #RRGGBB");
    return cor.toUpperCase(Locale.ROOT);
  }
  @GetMapping("/visual-cartao")
  @Transactional(readOnly = true)
  public VisualCartao visualCartao(@PathVariable UUID estabelecimentoId, Authentication auth) {
    Estabelecimento e = estabelecimento(estabelecimentoId);
    // Disponível somente para usuários autenticados que participam deste estabelecimento.
    UUID usuarioId = usuarios.findByEmailIgnoreCase(auth.getName())
        .filter(u -> u.getAtivo()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN)).getId();
    if (!participacoes.existsByEstabelecimentoIdAndUsuarioId(estabelecimentoId, usuarioId)) {
      permissoes.exigirLeitura(estabelecimentoId, auth.getName());
    }
    return new VisualCartao(e.getCorCartao(), e.getCorCarimbo(), e.getIconeCarimbo(), e.isFotoFundoCartao());
  }
  @PutMapping("/visual-cartao")
  @Transactional
  public VisualCartao salvarVisualCartao(@PathVariable UUID estabelecimentoId,
      @RequestBody VisualCartao dados, Authentication auth) {
    permissoes.exigirProprietario(estabelecimentoId, auth.getName());
    Estabelecimento e = estabelecimento(estabelecimentoId);
    e.setCorCartao(validarCor(dados.corCartao()));
    e.setCorCarimbo(validarCor(dados.corCarimbo()));
    String icone = dados.iconeCarimbo() == null ? "estrela" : dados.iconeCarimbo();
    if (!ICONES.contains(icone))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ícone inválido");
    e.setIconeCarimbo(icone);
    return new VisualCartao(e.getCorCartao(), e.getCorCarimbo(), e.getIconeCarimbo(), e.isFotoFundoCartao());
  }

  public record Campanha(UUID id, String titulo, int metaCarimbos, String recompensa,
      String criteriosCompra, int versao, boolean ativa, boolean principal) {}


  public record CampanhaCliente(UUID id, String titulo, int metaCarimbos,
      String recompensa, boolean participando) {}

  @GetMapping("/campanhas-cliente")
  @Transactional(readOnly = true)
  public List<CampanhaCliente> campanhasCliente(@PathVariable UUID estabelecimentoId,
      Authentication auth) {
    Estabelecimento e = estabelecimento(estabelecimentoId);
    UUID usuarioId = usuarios.findByEmailIgnoreCase(auth.getName())
        .filter(u -> u.getAtivo()).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.FORBIDDEN)).getId();
    // A vitrine só é visível para clientes já vinculados ao estabelecimento.
    if (!participacoes.existsByEstabelecimentoIdAndUsuarioId(estabelecimentoId, usuarioId))
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return programas.findByEstabelecimentoIdOrderByCriadoEmAsc(estabelecimentoId).stream()
        .filter(ProgramaFidelidade::isAtiva)
        .filter(c -> e.isExibirTodasCampanhas() ||
            participacoes.findByProgramaIdAndUsuarioId(c.getId(), usuarioId).isPresent())
        .map(c -> { Campanha resumo = resumo(c);
          return new CampanhaCliente(c.getId(), resumo.titulo(), resumo.metaCarimbos(),
              resumo.recompensa(), participacoes.findByProgramaIdAndUsuarioId(c.getId(), usuarioId).isPresent());
        }).toList();
  }

  @GetMapping("/configuracao-campanhas")
  @Transactional(readOnly = true)
  public Configuracao configuracao(@PathVariable UUID estabelecimentoId, Authentication auth) {
    permissoes.exigirLeitura(estabelecimentoId, auth.getName());
    return new Configuracao(estabelecimento(estabelecimentoId).isExibirTodasCampanhas());
  }

  @PutMapping("/configuracao-campanhas")
  @Transactional
  public Configuracao salvarConfiguracao(@PathVariable UUID estabelecimentoId,
      @RequestBody Configuracao dados, Authentication auth) {
    permissoes.exigirProprietario(estabelecimentoId, auth.getName());
    Estabelecimento e = estabelecimento(estabelecimentoId);
    e.setExibirTodasCampanhas(dados.exibirTodasCampanhas());
    return new Configuracao(e.isExibirTodasCampanhas());
  }

  @GetMapping("/campanhas")
  @Transactional(readOnly = true)
  public List<Campanha> listar(@PathVariable UUID estabelecimentoId, Authentication auth) {
    permissoes.exigirLeitura(estabelecimentoId, auth.getName());
    estabelecimento(estabelecimentoId);
    return programas.findByEstabelecimentoIdOrderByCriadoEmAsc(estabelecimentoId).stream()
        .map(this::resumo).toList();
  }

  @PostMapping("/campanhas")
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  public Campanha criar(@PathVariable UUID estabelecimentoId,
      @Valid @RequestBody ProgramaRequest dados, Authentication auth) {
    UUID autor = permissoes.exigirProprietario(estabelecimentoId, auth.getName()).getId();
    estabelecimento(estabelecimentoId);
    ProgramaFidelidade p = new ProgramaFidelidade(estabelecimentoId, Instant.now());
    // Compatibilidade: primeira campanha torna-se a principal da API existente.
    if (programas.findByEstabelecimentoIdAndPrincipalTrue(estabelecimentoId).isEmpty())
      p.setPrincipal(true);
    programas.save(p);
    versoes.save(new VersaoPrograma(p.getId(), 1, dados.titulo().trim(), dados.metaCarimbos(),
        dados.recompensa().trim(), dados.criteriosCompra().trim(), Instant.now(), autor));
    return resumo(p);
  }

  @PatchMapping("/campanhas/{campanhaId}/status")
  @Transactional
  public Campanha status(@PathVariable UUID estabelecimentoId, @PathVariable UUID campanhaId,
      @RequestBody Status dados, Authentication auth) {
    permissoes.exigirProprietario(estabelecimentoId, auth.getName());
    ProgramaFidelidade p = programas.findById(campanhaId)
        .filter(c -> c.getEstabelecimentoId().equals(estabelecimentoId))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    p.setAtiva(dados.ativa());
    return resumo(p);
  }
  public record Status(boolean ativa) {}

  private Campanha resumo(ProgramaFidelidade p) {
    VersaoPrograma v = versoes.findByProgramaIdAndNumero(p.getId(), p.getVersaoAtual())
        .orElseThrow();
    return new Campanha(p.getId(), v.getTitulo(), v.getMetaCarimbos(), v.getRecompensa(),
        v.getCriteriosCompra(), p.getVersaoAtual(), p.isAtiva(), p.isPrincipal());
  }

  private Estabelecimento estabelecimento(UUID id) {
    return estabelecimentos.findById(id).filter(Estabelecimento::getAtivo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Estabelecimento não encontrado"));
  }
}
