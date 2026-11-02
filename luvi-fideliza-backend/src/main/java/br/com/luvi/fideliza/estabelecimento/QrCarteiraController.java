package br.com.luvi.fideliza.estabelecimento;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QrCarteiraController {
  private final QrCarteiraService service;
  private final QrLinks links;

  public QrCarteiraController(QrCarteiraService service, QrLinks links) {
    this.service = service;
    this.links = links;
  }

  @GetMapping("/api/v1/me/participacoes/{id}/qr")
  public QrCarteiraService.Link meuLink(
      @PathVariable UUID id, Authentication auth, HttpServletRequest request) {
    return service.participacaoDoCliente(id, auth.getName(), request);
  }

  @GetMapping(value = "/api/v1/me/participacoes/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> meuPng(
      @PathVariable UUID id, Authentication auth, HttpServletRequest request) {
    return links.png(service.participacaoDoCliente(id, auth.getName(), request).url());
  }

  @GetMapping("/api/v1/me/recompensas/{id}/qr")
  public QrCarteiraService.Link recompensaLink(
      @PathVariable UUID id, Authentication auth, HttpServletRequest request) {
    return service.recompensaDoCliente(id, auth.getName(), request);
  }

  @GetMapping(value = "/api/v1/me/recompensas/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> recompensaPng(
      @PathVariable UUID id, Authentication auth, HttpServletRequest request) {
    return links.png(service.recompensaDoCliente(id, auth.getName(), request).url());
  }

  @GetMapping(
      value = "/api/v1/estabelecimentos/{loja}/participantes/{participacao}/qrcode",
      produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> equipePng(
      @PathVariable UUID loja,
      @PathVariable UUID participacao,
      Authentication auth,
      HttpServletRequest request) {
    return links.png(service.participacaoDaEquipe(loja, participacao, auth.getName(), request).url());
  }
}
