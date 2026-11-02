package br.com.luvi.fideliza.estabelecimento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrModernoTest {
  @Test
  void oCelularLeOEnderecoCurtoEOLongo() throws Exception {
    ler("http://192.168.3.232:8081/adesao.html?loja=11111111-1111-1111-1111-111111111111");
    ler("http://192.168.3.232:8081/painel.html?loja=aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee&participacao=ffffffff-1111-2222-3333-444444444444");
  }

  private static void ler(String url) throws Exception {
    byte[] png = QrModerno.png(url);
    assertTrue(png.length > 800, "png pequeno demais");
    var imagem = ImageIO.read(new ByteArrayInputStream(png));
    var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagem)));
    assertEquals(url, new QRCodeReader().decode(bitmap).getText());
  }
}
