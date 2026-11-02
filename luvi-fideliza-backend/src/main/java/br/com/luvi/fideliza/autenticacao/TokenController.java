package br.com.luvi.fideliza.autenticacao;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TokenController {
  private final TokenService tokens;

  public TokenController(TokenService tokens) {
    this.tokens = tokens;
  }

  public record PedidoToken(@NotBlank String identificador, @NotBlank String senha) {}

  @PostMapping("/api/v1/auth/token")
  public TokenService.TokenEmitido emitir(@Valid @RequestBody PedidoToken dados) {
    return tokens.emitir(dados.identificador(), dados.senha());
  }

  @DeleteMapping("/api/v1/auth/token")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void revogar(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) return;
    tokens.revogar(header.substring(7).trim());
  }
}
