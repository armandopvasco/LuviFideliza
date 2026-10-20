package br.com.luvi.fideliza.fidelidade.controller;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.repository.ParticipacaoRepository;
import br.com.luvi.fideliza.usuario.UsuarioRepository;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/estabelecimentos/{estabelecimentoId}/fundo-cartao")
public class FundoCartaoController {
  private final EstabelecimentoRepository estabelecimentos;
  private final PermissaoEstabelecimentoService permissoes;
  private final ParticipacaoRepository participacoes;
  private final UsuarioRepository usuarios;
  private final Path pasta;

  public FundoCartaoController(EstabelecimentoRepository estabelecimentos,
      PermissaoEstabelecimentoService permissoes, ParticipacaoRepository participacoes,
      UsuarioRepository usuarios,
      @Value("${luvi.cartoes.imagens.diretorio:./dados/cartoes}") String diretorio) {
    this.estabelecimentos = estabelecimentos;
    this.permissoes = permissoes;
    this.participacoes = participacoes;
    this.usuarios = usuarios;
    this.pasta = Path.of(diretorio).toAbsolutePath().normalize();
  }

  private Estabelecimento estabelecimento(UUID id) {
    return estabelecimentos.findById(id).filter(Estabelecimento::getAtivo)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @GetMapping
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> imagem(@PathVariable UUID estabelecimentoId, Authentication auth) throws java.io.IOException {
    Estabelecimento e = estabelecimento(estabelecimentoId);
    UUID usuarioId = (auth.getName().contains("@") ? usuarios.findByEmailIgnoreCase(auth.getName()) : usuarios.findByTelefone(auth.getName())).filter(u -> u.getAtivo())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN)).getId();
    if (!participacoes.existsByEstabelecimentoIdAndUsuarioId(estabelecimentoId, usuarioId))
      permissoes.exigirLeitura(estabelecimentoId, auth.getName());
    Path arquivo = pasta.resolve(estabelecimentoId + ".jpg");
    if (!e.isFotoFundoCartao() || !Files.isRegularFile(arquivo))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG)
        .cacheControl(CacheControl.noStore()).body(Files.readAllBytes(arquivo));
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @Transactional
  public ResponseEntity<Void> enviar(@PathVariable UUID estabelecimentoId,
      @RequestParam("imagem") MultipartFile imagem, Authentication auth) throws java.io.IOException {
    permissoes.exigirProprietario(estabelecimentoId, auth.getName());
    Estabelecimento e = estabelecimento(estabelecimentoId);
    if (imagem.isEmpty() || imagem.getSize() > 2_000_000L)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Imagem deve ter até 2 MB");
    byte[] bytes = imagem.getBytes();
    var decodificada = ImageIO.read(new ByteArrayInputStream(bytes));
    if (decodificada == null || decodificada.getWidth() > 3000 || decodificada.getHeight() > 3000)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie uma imagem JPG ou PNG de até 3000px");
    Files.createDirectories(pasta);
    Path temporario = Files.createTempFile(pasta, "cartao-", ".tmp");
    try {
      var rgb = new java.awt.image.BufferedImage(decodificada.getWidth(), decodificada.getHeight(),
          java.awt.image.BufferedImage.TYPE_INT_RGB);
      var g = rgb.createGraphics();
      try { g.setColor(java.awt.Color.WHITE); g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(decodificada, 0, 0, null); } finally { g.dispose(); }
      if (!ImageIO.write(rgb, "jpg", temporario.toFile()))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível processar a imagem");
      try { Files.move(temporario, pasta.resolve(estabelecimentoId + ".jpg"),
          StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
      catch (AtomicMoveNotSupportedException ex) {
        Files.move(temporario, pasta.resolve(estabelecimentoId + ".jpg"), StandardCopyOption.REPLACE_EXISTING);
      }
    } finally { Files.deleteIfExists(temporario); }
    e.setFotoFundoCartao(true);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping
  @Transactional
  public ResponseEntity<Void> remover(@PathVariable UUID estabelecimentoId, Authentication auth) throws java.io.IOException {
    permissoes.exigirProprietario(estabelecimentoId, auth.getName());
    Estabelecimento e = estabelecimento(estabelecimentoId);
    e.setFotoFundoCartao(false);
    Files.deleteIfExists(pasta.resolve(estabelecimentoId + ".jpg"));
    return ResponseEntity.noContent().build();
  }
}
