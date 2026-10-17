package br.com.luvi.fideliza.fidelidade.service;

import static org.springframework.http.HttpStatus.*;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.*;
import br.com.luvi.fideliza.fidelidade.entity.*;
import br.com.luvi.fideliza.fidelidade.repository.*;
import br.com.luvi.fideliza.recompensa.RecompensaService;
import br.com.luvi.fideliza.usuario.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CartaoService {
  private final ParticipacaoRepository participacoes;
  private final CartaoRepository cartoes;
  private final CarimboRepository carimbos;
  private final ProgramaRepository programas;
  private final VersaoProgramaRepository versoes;
  private final EstabelecimentoRepository estabelecimentos;
  private final UsuarioRepository usuarios;
  private final PermissaoEstabelecimentoService permissoes;
  private final RecompensaService recompensas;

  public CartaoService(
      ParticipacaoRepository participacoes,
      CartaoRepository cartoes,
      CarimboRepository carimbos,
      ProgramaRepository programas,
      VersaoProgramaRepository versoes,
      EstabelecimentoRepository estabelecimentos,
      UsuarioRepository usuarios,
      PermissaoEstabelecimentoService permissoes,
      RecompensaService recompensas) {
    this.participacoes = participacoes;
    this.cartoes = cartoes;
    this.carimbos = carimbos;
    this.programas = programas;
    this.versoes = versoes;
    this.estabelecimentos = estabelecimentos;
    this.usuarios = usuarios;
    this.permissoes = permissoes;
    this.recompensas = recompensas;
  }

  @Transactional
  public Resultado<Cadastro> cadastrar(UUID negocio, UUID chave, String nome, String email) {
    UUID autor = permissoes.exigirOperacao(negocio, email).getId();
    estabelecimento(negocio, true);
    Optional<Participacao> existente =
        participacoes.findByEstabelecimentoIdAndChaveCadastro(negocio, chave);
    if (existente.isPresent()) {
      Participacao p = existente.get();
      if (!p.getNomeExibicao().equals(nome.trim())) throw conflitoChave();
      CartaoFidelidade primeiro = cartoes.findByParticipacaoIdAndNumero(p.getId(), 1).orElseThrow();
      return new Resultado<>(new Cadastro(participante(p), cartao(primeiro)), true);
    }
    ProgramaFidelidade programa = programa(negocio);
    Participacao p =
        participacoes.save(
            new Participacao(
                UUID.randomUUID(),
                negocio,
                programa.getId(),
                null,
                nome.trim(),
                chave,
                Instant.now(),
                autor,
                null,
                null));
    CartaoFidelidade c = abrirCartao(p, 1, programa);
    return new Resultado<>(new Cadastro(participante(p), cartao(c)), false);
  }

  @Transactional
  public Convite convidar(UUID negocio, UUID participacao, String email) {
    permissoes.exigirOperacao(negocio, email);
    estabelecimento(negocio, true);
    Participacao p = participacao(negocio, participacao);
    if (p.getUsuarioId() != null)
      throw new ResponseStatusException(CONFLICT, "Participante já vinculado a uma conta");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    Instant expira = Instant.now().plus(Duration.ofHours(48));
    p.convidar(hash(token), expira);
    return new Convite(token, expira);
  }

  @Transactional
  public Participante aceitar(String token, String email) {
    Usuario usuario = usuario(email);
    String hash = hash(token);
    UUID negocio =
        participacoes.estabelecimentoDoConvite(hash).orElseThrow(CartaoService::conviteInvalido);
    estabelecimento(negocio, true);
    Participacao p =
        participacoes.findByConviteHash(hash).orElseThrow(CartaoService::conviteInvalido);
    if (p.getUsuarioId() != null) {
      if (p.getUsuarioId().equals(usuario.getId())) return participante(p);
      throw conviteInvalido();
    }
    if (p.getConviteExpiraEm() == null || !p.getConviteExpiraEm().isAfter(Instant.now()))
      throw conviteInvalido();
    if (participacoes.findByProgramaIdAndUsuarioId(p.getProgramaId(), usuario.getId()).isPresent())
      throw new ResponseStatusException(
          CONFLICT, "Você já participa deste programa. Solicite auxílio ao estabelecimento.");
    p.vincular(usuario.getId());
    return participante(p);
  }

  @Transactional
  public Resultado<Carimbo> creditar(
      UUID negocio, UUID participacao, UUID chave, String referencia, String email) {
    UUID autor = permissoes.exigirOperacao(negocio, email).getId();
    estabelecimento(negocio, true);
    Participacao p = participacao(negocio, participacao);
    Optional<MovimentacaoCarimbo> anterior =
        carimbos.findByEstabelecimentoIdAndChaveOperacao(negocio, chave);
    if (anterior.isPresent()) {
      MovimentacaoCarimbo m = anterior.get();
      if (!m.getParticipacaoId().equals(participacao)
          || !m.getReferenciaCompra().equals(referencia.trim())) throw conflitoChave();
      return new Resultado<>(carimbo(m), true);
    }
    CartaoFidelidade c = cartoes.findTopByParticipacaoIdOrderByNumeroDesc(p.getId()).orElseThrow();
    if (c.getConcluidoEm() != null) c = abrirCartao(p, c.getNumero() + 1, programa(negocio));
    VersaoPrograma regras = versoes.findById(c.getVersaoProgramaId()).orElseThrow();
    Instant agora = Instant.now();
    c.creditar(regras.getMetaCarimbos(), agora);
    recompensas.emitir(c);
    MovimentacaoCarimbo m =
        carimbos.save(
            new MovimentacaoCarimbo(
                UUID.randomUUID(),
                negocio,
                p.getId(),
                c.getId(),
                chave,
                referencia.trim(),
                c.getSaldo(),
                agora,
                autor));
    return new Resultado<>(carimbo(m), false);
  }

  @Transactional(readOnly = true)
  public Pagina<Participante> participantes(UUID negocio, int pagina, int tamanho, String email) {
    return participantes(negocio, pagina, tamanho, "", email);
  }

  @Transactional(readOnly = true)
  public Pagina<Participante> participantes(
      UUID negocio, int pagina, int tamanho, String busca, String email) {
    permissoes.exigirLeitura(negocio, email);
    estabelecimento(negocio, false);
    if (busca.length() > 120)
      throw new ResponseStatusException(BAD_REQUEST, "Busca limitada a 120 caracteres");
    String filtro =
        "%"
            + busca
                .trim()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_")
            + "%";
    Page<Participacao> lista =
        participacoes.pesquisar(negocio, filtro, pagina(pagina, tamanho, "criadoEm", false));
    return new Pagina<>(
        lista.map(this::participante).getContent(), pagina, tamanho, lista.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Pagina<Cartao> cartoes(
      UUID negocio, UUID participacao, int pagina, int tamanho, String email) {
    permissoes.exigirLeitura(negocio, email);
    estabelecimento(negocio, false);
    participacao(negocio, participacao);
    Page<CartaoFidelidade> lista =
        cartoes.findByParticipacaoId(participacao, pagina(pagina, tamanho, "numero", true));
    return new Pagina<>(
        lista.map(this::cartao).getContent(), pagina, tamanho, lista.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Participante consultarParticipante(UUID negocio, UUID id, String email) {
    permissoes.exigirLeitura(negocio, email);
    estabelecimento(negocio, false);
    return participante(participacao(negocio, id));
  }

  @Transactional(readOnly = true)
  public Pagina<Carimbo> historico(
      UUID negocio, UUID participacao, int pagina, int tamanho, String email) {
    permissoes.exigirLeitura(negocio, email);
    estabelecimento(negocio, false);
    participacao(negocio, participacao);
    Page<MovimentacaoCarimbo> lista =
        carimbos.findByParticipacaoId(participacao, pagina(pagina, tamanho, "registradoEm", true));
    return new Pagina<>(
        lista.map(this::carimbo).getContent(), pagina, tamanho, lista.getTotalElements());
  }

  @Transactional(readOnly = true)
  public Pagina<Cartao> carteira(int pagina, int tamanho, String email) {
    Usuario u = usuario(email);
    Page<CartaoFidelidade> lista =
        cartoes.carteira(u.getId(), pagina(pagina, tamanho, "abertoEm", true));
    return new Pagina<>(
        lista.map(this::cartao).getContent(), pagina, tamanho, lista.getTotalElements());
  }

  private Pageable pagina(int pagina, int tamanho, String campo, boolean decrescente) {
    if (pagina < 0 || tamanho < 1 || tamanho > 100)
      throw new ResponseStatusException(BAD_REQUEST, "Página deve ser >= 0 e tamanho de 1 a 100");
    return PageRequest.of(
        pagina,
        tamanho,
        Sort.by(decrescente ? Sort.Direction.DESC : Sort.Direction.ASC, campo).and(Sort.by("id")));
  }

  private CartaoFidelidade abrirCartao(Participacao p, int numero, ProgramaFidelidade programa) {
    VersaoPrograma regras =
        versoes
            .findByProgramaIdAndNumero(programa.getId(), programa.getVersaoAtual())
            .orElseThrow();
    return cartoes.save(
        new CartaoFidelidade(
            UUID.randomUUID(), p.getId(), regras.getId(), numero, 0, Instant.now(), null));
  }

  private Participacao participacao(UUID negocio, UUID id) {
    return participacoes
        .findById(id)
        .filter(p -> p.getEstabelecimentoId().equals(negocio))
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    NOT_FOUND, "Participante não encontrado neste estabelecimento"));
  }

  private ProgramaFidelidade programa(UUID negocio) {
    return programas
        .findByEstabelecimentoId(negocio)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    CONFLICT, "Cadastre o programa antes de adicionar participantes"));
  }

  private Estabelecimento estabelecimento(UUID id, boolean bloquear) {
    // Todas as gravações deste módulo usam a mesma ordem de lock do programa.
    // Adequado ao volume do piloto; evita saldo perdido e operações duplicadas.
    return (bloquear ? estabelecimentos.buscarParaAlteracao(id) : estabelecimentos.findById(id))
        .filter(Estabelecimento::getAtivo)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    NOT_FOUND, "Estabelecimento não encontrado ou inativo"));
  }

  private Usuario usuario(String email) {
    return usuarios
        .findByEmailIgnoreCase(email)
        .filter(Usuario::getAtivo)
        .orElseThrow(() -> new AccessDeniedException("Acesso negado"));
  }

  private Participante participante(Participacao p) {
    return new Participante(
        p.getId(), p.getEstabelecimentoId(), p.getNomeExibicao(), p.getUsuarioId() != null);
  }

  private Cartao cartao(CartaoFidelidade c) {
    Participacao p = participacoes.findById(c.getParticipacaoId()).orElseThrow();
    VersaoPrograma v = versoes.findById(c.getVersaoProgramaId()).orElseThrow();
    String negocio = estabelecimentos.findById(p.getEstabelecimentoId()).orElseThrow().getNome();
    return new Cartao(
        c.getId(),
        p.getId(),
        p.getEstabelecimentoId(),
        negocio,
        c.getNumero(),
        v.getId(),
        v.getNumero(),
        v.getTitulo(),
        v.getRecompensa(),
        v.getCriteriosCompra(),
        v.getMetaCarimbos(),
        c.getSaldo(),
        c.getConcluidoEm() == null ? "EM_ANDAMENTO" : "CONCLUIDO",
        c.getAbertoEm(),
        c.getConcluidoEm());
  }

  private Carimbo carimbo(MovimentacaoCarimbo m) {
    CartaoFidelidade c = cartoes.findById(m.getCartaoId()).orElseThrow();
    VersaoPrograma v = versoes.findById(c.getVersaoProgramaId()).orElseThrow();
    return new Carimbo(
        m.getId(),
        m.getParticipacaoId(),
        m.getCartaoId(),
        m.getChaveOperacao(),
        m.getReferenciaCompra(),
        m.getSaldoApos(),
        m.getSaldoApos() == v.getMetaCarimbos(),
        m.getRegistradoEm(),
        m.getRegistradoPor());
  }

  private static ResponseStatusException conflitoChave() {
    return new ResponseStatusException(
        CONFLICT,
        "Chave já usada com outros dados. Use a mesma chave apenas para repetir a mesma operação.");
  }

  private static ResponseStatusException conviteInvalido() {
    return new ResponseStatusException(BAD_REQUEST, "Convite inválido, expirado ou já utilizado");
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
