package br.com.luvi.fideliza.estabelecimento;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.ByteMatrix;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import javax.imageio.ImageIO;

/** QR com olhos arredondados e módulos em cápsulas horizontais, no estilo do cartaz. */
final class QrModerno {
  private static final int ZONA = 4;
  private static final int MODULO = 18;
  private static final Color FUNDO = new Color(0xF6, 0xF5, 0xF3);
  private static final Color TINTA = new Color(0x11, 0x11, 0x11);

  private QrModerno() {}

  static byte[] png(String url) {
    try {
      var imagem = desenhar(url);
      var bytes = new ByteArrayOutputStream();
      ImageIO.write(imagem, "PNG", bytes);
      return bytes.toByteArray();
    } catch (WriterException ex) {
      throw new IllegalArgumentException("Não foi possível gerar o QR Code", ex);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  static BufferedImage desenhar(String url) throws WriterException {
    QRCode codigo = Encoder.encode(url, ErrorCorrectionLevel.H, Map.of(EncodeHintType.CHARACTER_SET, "UTF-8"));
    ByteMatrix matriz = codigo.getMatrix();
    int largura = matriz.getWidth();
    int altura = matriz.getHeight();
    boolean[][] reservado = reservados(codigo, largura, altura);
    int lado = (largura + ZONA * 2) * MODULO;
    var imagem = new BufferedImage(lado, lado, BufferedImage.TYPE_INT_RGB);
    var g = imagem.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    g.setColor(FUNDO);
    g.fillRect(0, 0, lado, lado);
    g.setColor(TINTA);
    capsulas(g, matriz, reservado);
    olhos(g, largura, altura);
    alinhamentos(g, codigo, largura, altura);
    g.dispose();
    return imagem;
  }

  private static void capsulas(Graphics2D g, ByteMatrix matriz, boolean[][] reservado) {
    int altura = matriz.getHeight();
    int largura = matriz.getWidth();
    float folga = MODULO * 0.11f;
    for (int y = 0; y < altura; y++) {
      int x = 0;
      while (x < largura) {
        if (matriz.get(x, y) != 1 || reservado[x][y]) {
          x++;
          continue;
        }
        int inicio = x;
        while (x < largura && matriz.get(x, y) == 1 && !reservado[x][y]) x++;
        float esquerda = (ZONA + inicio) * MODULO + folga;
        float topo = (ZONA + y) * MODULO + folga;
        float comprimento = (x - inicio) * MODULO - folga * 2;
        float espessura = MODULO - folga * 2;
        g.fill(new RoundRectangle2D.Float(esquerda, topo, comprimento, espessura, espessura, espessura));
      }
    }
  }

  private static void olhos(Graphics2D g, int largura, int altura) {
    olho(g, 0, 0);
    olho(g, largura - 7, 0);
    olho(g, 0, altura - 7);
  }

  private static void olho(Graphics2D g, int coluna, int linha) {
    float x = (ZONA + coluna) * MODULO;
    float y = (ZONA + linha) * MODULO;
    float lado = 7f * MODULO;
    float raio = 1.85f * MODULO;
    var anel = new Area(new RoundRectangle2D.Float(x, y, lado, lado, raio * 2, raio * 2));
    float recuo = MODULO;
    anel.subtract(new Area(new RoundRectangle2D.Float(
        x + recuo, y + recuo, lado - recuo * 2, lado - recuo * 2, 1.45f * MODULO * 2, 1.45f * MODULO * 2)));
    g.fill(anel);
    float miolo = 2f * MODULO;
    g.fill(new RoundRectangle2D.Float(
        x + miolo, y + miolo, lado - miolo * 2, lado - miolo * 2, 1.15f * MODULO * 2, 1.15f * MODULO * 2));
  }

  private static void alinhamentos(Graphics2D g, QRCode codigo, int largura, int altura) {
    int[] centros = codigo.getVersion().getAlignmentPatternCenters();
    if (centros == null || centros.length == 0) return;
    for (int cy : centros) {
      for (int cx : centros) {
        if (encostaNoOlho(cx, cy, largura, altura)) continue;
        alinhamento(g, cx - 2, cy - 2);
      }
    }
  }

  private static void alinhamento(Graphics2D g, int coluna, int linha) {
    float x = (ZONA + coluna) * MODULO;
    float y = (ZONA + linha) * MODULO;
    float lado = 5f * MODULO;
    var anel = new Area(new RoundRectangle2D.Float(x, y, lado, lado, 1.45f * MODULO * 2, 1.45f * MODULO * 2));
    anel.subtract(new Area(new RoundRectangle2D.Float(
        x + MODULO, y + MODULO, 3f * MODULO, 3f * MODULO, 1.05f * MODULO * 2, 1.05f * MODULO * 2)));
    g.fill(anel);
    float diametro = MODULO * 0.92f;
    float desloca = 2f * MODULO + (MODULO - diametro) / 2f;
    g.fill(new Ellipse2D.Float(x + desloca, y + desloca, diametro, diametro));
  }

  private static boolean[][] reservados(QRCode codigo, int largura, int altura) {
    var mapa = new boolean[largura][altura];
    marcar(mapa, 0, 0, 7, 7);
    marcar(mapa, largura - 7, 0, 7, 7);
    marcar(mapa, 0, altura - 7, 7, 7);
    int[] centros = codigo.getVersion().getAlignmentPatternCenters();
    if (centros == null) return mapa;
    for (int cy : centros) {
      for (int cx : centros) {
        if (encostaNoOlho(cx, cy, largura, altura)) continue;
        marcar(mapa, cx - 2, cy - 2, 5, 5);
      }
    }
    return mapa;
  }

  private static void marcar(boolean[][] mapa, int x, int y, int w, int h) {
    for (int linha = y; linha < y + h; linha++) {
      for (int coluna = x; coluna < x + w; coluna++) {
        if (coluna >= 0 && linha >= 0 && coluna < mapa.length && linha < mapa[0].length) {
          mapa[coluna][linha] = true;
        }
      }
    }
  }

  private static boolean encostaNoOlho(int x, int y, int largura, int altura) {
    return (x <= 8 && y <= 8)
        || (x >= largura - 9 && y <= 8)
        || (x <= 8 && y >= altura - 9);
  }
}
