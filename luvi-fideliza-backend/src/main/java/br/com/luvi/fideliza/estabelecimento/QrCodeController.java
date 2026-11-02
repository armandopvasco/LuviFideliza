package br.com.luvi.fideliza.estabelecimento;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class QrCodeController {
  private final EstabelecimentoRepository lojas;
  private final QrLinks links;

  public QrCodeController(EstabelecimentoRepository lojas, QrLinks links) {
    this.lojas = lojas;
    this.links = links;
  }

  @GetMapping(value = "/api/v1/publico/estabelecimentos/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> qr(@PathVariable UUID id, HttpServletRequest request) {
    if (lojas.findById(id).filter(Estabelecimento::getAtivo).isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return links.png(links.adesao(request, id));
  }
}
