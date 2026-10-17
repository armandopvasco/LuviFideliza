package br.com.luvi.fideliza.fidelidade.controller;

import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.*;
import br.com.luvi.fideliza.fidelidade.service.CartaoService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class CartaoController {
  private final CartaoService service;

  public CartaoController(CartaoService service) {
    this.service = service;
  }

  @PostMapping("/api/v1/estabelecimentos/{id}/participantes")
  public ResponseEntity<Cadastro> cadastrar(
      @PathVariable UUID id,
      @RequestHeader("Idempotency-Key") UUID chave,
      @Valid @RequestBody CadastrarParticipante dados,
      Authentication usuario) {
    Resultado<Cadastro> resultado =
        service.cadastrar(id, chave, dados.nomeExibicao(), usuario.getName());
    return ResponseEntity.status(resultado.repetido() ? 200 : 201).body(resultado.dados());
  }

  @GetMapping("/api/v1/estabelecimentos/{id}/participantes")
  public Pagina<Participante> participantes(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho,
      @RequestParam(defaultValue = "") String busca,
      Authentication usuario) {
    return service.participantes(id, pagina, tamanho, busca, usuario.getName());
  }

  @PostMapping("/api/v1/estabelecimentos/{id}/participantes/{participacao}/convite")
  public Convite convidar(
      @PathVariable UUID id, @PathVariable UUID participacao, Authentication usuario) {
    return service.convidar(id, participacao, usuario.getName());
  }

  @GetMapping("/api/v1/estabelecimentos/{id}/participantes/{participacao}")
  public Participante consultarParticipante(
      @PathVariable UUID id, @PathVariable UUID participacao, Authentication usuario) {
    return service.consultarParticipante(id, participacao, usuario.getName());
  }

  @PostMapping("/api/v1/me/participacoes/aceitar")
  public Participante aceitar(@Valid @RequestBody AceitarConvite dados, Authentication usuario) {
    return service.aceitar(dados.token(), usuario.getName());
  }

  @GetMapping("/api/v1/estabelecimentos/{id}/participantes/{participacao}/cartoes")
  public Pagina<Cartao> cartoes(
      @PathVariable UUID id,
      @PathVariable UUID participacao,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho,
      Authentication usuario) {
    return service.cartoes(id, participacao, pagina, tamanho, usuario.getName());
  }

  @PostMapping("/api/v1/estabelecimentos/{id}/participantes/{participacao}/carimbos")
  public ResponseEntity<Carimbo> creditar(
      @PathVariable UUID id,
      @PathVariable UUID participacao,
      @RequestHeader("Idempotency-Key") UUID chave,
      @Valid @RequestBody RegistrarCarimbo dados,
      Authentication usuario) {
    Resultado<Carimbo> resultado =
        service.creditar(id, participacao, chave, dados.referenciaCompra(), usuario.getName());
    return ResponseEntity.status(resultado.repetido() ? 200 : 201).body(resultado.dados());
  }

  @GetMapping("/api/v1/estabelecimentos/{id}/participantes/{participacao}/carimbos")
  public Pagina<Carimbo> historico(
      @PathVariable UUID id,
      @PathVariable UUID participacao,
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho,
      Authentication usuario) {
    return service.historico(id, participacao, pagina, tamanho, usuario.getName());
  }

  @GetMapping("/api/v1/me/cartoes")
  public Pagina<Cartao> carteira(
      @RequestParam(defaultValue = "0") int pagina,
      @RequestParam(defaultValue = "20") int tamanho,
      Authentication usuario) {
    return service.carteira(pagina, tamanho, usuario.getName());
  }
}
