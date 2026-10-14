package br.com.luvi.fideliza.compartilhado;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
@RestControllerAdvice
public class ApiExceptionHandler {
 @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> negocio(ResponseStatusException e) {
  return ResponseEntity.status(e.getStatusCode()).body(Map.of("mensagem",e.getReason()==null?"Operação inválida":e.getReason()));
 }
 @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
  // Nunca retornar valores rejeitados: podem conter senha ou token.
  return ResponseEntity.badRequest().body(Map.of("mensagem","Dados inválidos","campos",e.getBindingResult().getFieldErrors().stream().map(f -> f.getField()).distinct().toList()));
 }
 @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflito() {
  return ResponseEntity.status(409).body(Map.of("mensagem","Cadastro em conflito com um registro existente"));
 }
 @ExceptionHandler(AccessDeniedException.class) ResponseEntity<?> negado() {
  return ResponseEntity.status(403).body(Map.of("mensagem","Acesso negado"));
 }
}
