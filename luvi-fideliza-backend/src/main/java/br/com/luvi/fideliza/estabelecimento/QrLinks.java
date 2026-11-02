package br.com.luvi.fideliza.estabelecimento;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class QrLinks {
  private final String publico;

  public QrLinks(@Value("${luvi.publico.url:}") String publico) {
    this.publico = publico == null ? "" : publico.trim();
  }

  public String origem(HttpServletRequest request) {
    if (!publico.isBlank()) return publico.replaceAll("/+$", "");
    return ServletUriComponentsBuilder.fromContextPath(request).build().toUriString().replaceAll("/+$", "");
  }

  public String adesao(HttpServletRequest request, UUID loja) {
    return origem(request) + "/adesao.html?loja=" + loja;
  }

  public String atendimento(HttpServletRequest request, UUID loja, UUID participacao) {
    return origem(request) + "/painel.html?loja=" + loja + "&participacao=" + participacao;
  }

  public String recompensa(HttpServletRequest request, UUID loja, UUID recompensa) {
    return origem(request) + "/painel.html?loja=" + loja + "&recompensa=" + recompensa;
  }

  public ResponseEntity<byte[]> png(String url) {
    try {
      var matrix = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 360, 360);
      var bytes = new ByteArrayOutputStream();
      MatrixToImageWriter.writeToStream(matrix, "PNG", bytes);
      return ResponseEntity.ok()
          .cacheControl(CacheControl.noStore())
          .contentType(MediaType.IMAGE_PNG)
          .body(bytes.toByteArray());
    } catch (Exception ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Não foi possível gerar o QR Code");
    }
  }
}
