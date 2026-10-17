package br.com.luvi.fideliza.recompensa;

import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.Pagina;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class RecompensaController {

    private final RecompensaService service;

    public RecompensaController(RecompensaService service) {
        this.service = service;
    }

    @GetMapping(
            "/api/v1/estabelecimentos/{id}/participantes/{participacao}/recompensas"
    )
    public Pagina<RecompensaService.Resposta> porParticipante(
            @PathVariable UUID id,
            @PathVariable UUID participacao,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            Authentication usuario) {

        return service.porParticipante(
                id, participacao, pagina, tamanho, usuario.getName());
    }

    @PostMapping(
            "/api/v1/estabelecimentos/{id}/recompensas/{recompensa}/resgate"
    )
    public ResponseEntity<RecompensaService.Resposta> resgatar(
            @PathVariable UUID id,
            @PathVariable UUID recompensa,
            @RequestHeader("Idempotency-Key") UUID chave,
            Authentication usuario) {

        var resultado = service.resgatar(
                id, recompensa, chave, usuario.getName());

        return ResponseEntity
                .status(resultado.repetido() ? 200 : 201)
                .body(resultado.dados());
    }

    @GetMapping("/api/v1/me/recompensas")
    public Pagina<RecompensaService.Resposta> doCliente(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            Authentication usuario) {

        return service.doCliente(pagina, tamanho, usuario.getName());
    }
}