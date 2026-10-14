package br.com.luvi.fideliza.estabelecimento;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
@RestController
public class AcessoController {
 private final AcessoEstabelecimentoService service;
 public AcessoController(AcessoEstabelecimentoService service) {this.service=service;}
 public record Cadastro(@NotBlank @Size(max=150) String nome) {}
 public record Convite(@NotBlank @Email @Size(max=254) String email) {}
 public record Aceite(@NotBlank @Size(max=100) String token,
  @NotBlank @Size(max=120) String nome,
  @NotBlank @Size(min=12,max=72) @Pattern(regexp="[\\x20-\\x7E]+",message="Use caracteres ASCII na senha") String senha) {}
 @GetMapping("/api/v1/auth/csrf") public Map<String,String> csrf(CsrfToken token) {
  return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
 }
 @GetMapping("/api/v1/auth/me") public Map<String,Object> me(Authentication a) {
  return Map.of("email",a.getName(),"perfis",a.getAuthorities().stream().map(Object::toString).toList());
 }
 @PostMapping("/api/v1/admin/estabelecimentos") @ResponseStatus(HttpStatus.CREATED)
 public AcessoEstabelecimentoService.Negocio cadastrar(@Valid @RequestBody Cadastro c,Authentication a) {return service.cadastrar(c.nome(),a.getName());}
 @PostMapping("/api/v1/admin/estabelecimentos/{id}/convites") @ResponseStatus(HttpStatus.CREATED)
 public AcessoEstabelecimentoService.ConviteCriado convidar(@PathVariable UUID id,@Valid @RequestBody Convite c,Authentication a) {return service.convidar(id,c.email(),a.getName());}
 @PostMapping("/api/v1/auth/convites/aceitar") @ResponseStatus(HttpStatus.CREATED)
 public AcessoEstabelecimentoService.Negocio aceitar(@Valid @RequestBody Aceite c) {return service.aceitar(c.token(),c.nome(),c.senha());}
 @GetMapping("/api/v1/estabelecimentos/{id}")
 public AcessoEstabelecimentoService.Negocio consultar(@PathVariable UUID id,Authentication a) {return service.consultar(id,a.getName());}
}
