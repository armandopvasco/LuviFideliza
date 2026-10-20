package br.com.luvi.fideliza.autenticacao;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Telefone {
  private Telefone() {}
  public static String normalizar(String valor) {
    if (valor == null || valor.isBlank()) return null;
    String numero = valor.replaceAll("\\D", "");
    if (numero.length() == 11) numero = "55" + numero;
    if (!numero.matches("55[1-9][0-9]9[0-9]{8}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe celular brasileiro com DDD válido.");
    return numero;
  }
}
