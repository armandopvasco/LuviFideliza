package br.com.luvi.fideliza.estabelecimento;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class QrCodeController {
  private final EstabelecimentoRepository lojas;
  public QrCodeController(EstabelecimentoRepository lojas) { this.lojas=lojas; }
  @GetMapping(value="/api/v1/publico/estabelecimentos/{id}/qrcode",produces=MediaType.IMAGE_PNG_VALUE)
  public ResponseEntity<byte[]> qr(@PathVariable UUID id,HttpServletRequest request) throws Exception {
    if(lojas.findById(id).filter(Estabelecimento::getAtivo).isEmpty()) return ResponseEntity.notFound().build();
    String url=org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath()
      .path("/adesao.html").queryParam("loja",id).build().toUriString();
    var matrix=new QRCodeWriter().encode(url,BarcodeFormat.QR_CODE,360,360);
    var bytes=new ByteArrayOutputStream();
    MatrixToImageWriter.writeToStream(matrix,"PNG",bytes);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(bytes.toByteArray());
  }
}
