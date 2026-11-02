package br.com.luvi.fideliza.autenticacao;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class TokenAutenticacaoFilter extends OncePerRequestFilter {
  private final TokenService tokens;

  public TokenAutenticacaoFilter(TokenService tokens) {
    this.tokens = tokens;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
      var autenticacao = tokens.autenticar(header.substring(7).trim());
      if (autenticacao == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      SecurityContextHolder.getContext().setAuthentication(autenticacao);
    }
    chain.doFilter(request, response);
  }
}
