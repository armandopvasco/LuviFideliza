package br.com.luvi.fideliza.fidelidade.controller;

import br.com.luvi.fideliza.fidelidade.dto.*;
import br.com.luvi.fideliza.fidelidade.service.ProgramaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estabelecimentos/{estabelecimentoId}/programa")
public class ProgramaController {
  private final ProgramaService service;

  public ProgramaController(ProgramaService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProgramaResponse criar(
      @PathVariable UUID estabelecimentoId,
      @Valid @RequestBody ProgramaRequest dados,
      Authentication usuario) {
    return service.criar(estabelecimentoId, dados, usuario.getName());
  }

  @GetMapping
  public ProgramaResponse consultar(@PathVariable UUID estabelecimentoId, Authentication usuario) {
    return service.consultar(estabelecimentoId, usuario.getName());
  }

  @PutMapping
  public ProgramaResponse atualizar(
      @PathVariable UUID estabelecimentoId,
      @Valid @RequestBody AtualizarProgramaRequest dados,
      Authentication usuario) {
    return service.atualizar(estabelecimentoId, dados, usuario.getName());
  }

  @GetMapping("/historico")
  public List<ProgramaResponse> historico(
      @PathVariable UUID estabelecimentoId, Authentication usuario) {
    return service.historico(estabelecimentoId, usuario.getName());
  }
}
