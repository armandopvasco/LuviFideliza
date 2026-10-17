package br.com.luvi.fideliza.recompensa;

import static org.springframework.http.HttpStatus.*;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.estabelecimento.*;
import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.Pagina;
import br.com.luvi.fideliza.fidelidade.dto.CartaoDtos.Resultado;
import br.com.luvi.fideliza.fidelidade.entity.*;
import br.com.luvi.fideliza.fidelidade.repository.*;
import br.com.luvi.fideliza.usuario.*;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecompensaService {

    private final RecompensaRepository recompensas;
    private final CartaoRepository cartoes;
    private final ParticipacaoRepository participacoes;
    private final VersaoProgramaRepository versoes;
    private final EstabelecimentoRepository estabelecimentos;
    private final UsuarioRepository usuarios;
    private final PermissaoEstabelecimentoService permissoes;

    public RecompensaService(
            RecompensaRepository recompensas,
            CartaoRepository cartoes,
            ParticipacaoRepository participacoes,
            VersaoProgramaRepository versoes,
            EstabelecimentoRepository estabelecimentos,
            UsuarioRepository usuarios,
            PermissaoEstabelecimentoService permissoes) {

        this.recompensas = recompensas;
        this.cartoes = cartoes;
        this.participacoes = participacoes;
        this.versoes = versoes;
        this.estabelecimentos = estabelecimentos;
        this.usuarios = usuarios;
        this.permissoes = permissoes;
    }

    public record Resposta(
            UUID id,
            UUID cartaoId,
            UUID participacaoId,
            UUID estabelecimentoId,
            int ciclo,
            String descricao,
            String status,
            Instant emitidaEm,
            Instant resgatadaEm) {
    }

    // A emissão participa da mesma transação do carimbo.
    @Transactional(propagation = Propagation.MANDATORY)
    public void emitir(CartaoFidelidade cartao) {
        if (cartao.getConcluidoEm() == null) {
            return;
        }

        if (recompensas.existsById(cartao.getId())) {
            return;
        }

        Participacao participante = participacoes
                .findById(cartao.getParticipacaoId())
                .orElseThrow();

        recompensas.save(new Recompensa(
                cartao.getId(),
                participante.getEstabelecimentoId(),
                cartao.getConcluidoEm()));
    }

    @Transactional
    public Resultado<Resposta> resgatar(
            UUID estabelecimentoId,
            UUID recompensaId,
            UUID chave,
            String email) {

        UUID autor = permissoes
                .exigirOperacao(estabelecimentoId, email)
                .getId();

        // Usa a mesma ordem de bloqueio das operações de carimbo.
        estabelecimentoAtivo(estabelecimentoId, true);

        var repeticao = recompensas
                .findByEstabelecimentoIdAndChaveResgate(
                        estabelecimentoId, chave);

        if (repeticao.isPresent()) {
            Recompensa anterior = repeticao.get();

            if (!anterior.getId().equals(recompensaId)) {
                throw new ResponseStatusException(
                        CONFLICT,
                        "Chave já usada para outra recompensa");
            }

            return new Resultado<>(resposta(anterior), true);
        }

        Recompensa recompensa = recompensas
                .findById(recompensaId)
                .filter(r -> r.getEstabelecimentoId()
                        .equals(estabelecimentoId))
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Recompensa não encontrada neste estabelecimento"));

        if (recompensa.getResgatadaEm() != null) {
            throw new ResponseStatusException(
                    CONFLICT,
                    "Recompensa já resgatada");
        }

        recompensa.resgatar(autor, chave, Instant.now());

        return new Resultado<>(resposta(recompensa), false);
    }

    @Transactional(readOnly = true)
    public Pagina<Resposta> porParticipante(
            UUID estabelecimentoId,
            UUID participacaoId,
            int pagina,
            int tamanho,
            String email) {

        permissoes.exigirLeitura(estabelecimentoId, email);
        estabelecimentoAtivo(estabelecimentoId, false);

        participacoes.findById(participacaoId)
                .filter(p -> p.getEstabelecimentoId()
                        .equals(estabelecimentoId))
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Participante não encontrado neste estabelecimento"));

        Page<Recompensa> resultado = recompensas.porParticipante(
                participacaoId, paginacao(pagina, tamanho));

        return pagina(resultado, pagina, tamanho);
    }

    @Transactional(readOnly = true)
    public Pagina<Resposta> doCliente(
            int pagina,
            int tamanho,
            String email) {

        Usuario usuario = usuarios
                .findByEmailIgnoreCase(email)
                .filter(Usuario::getAtivo)
                .orElseThrow(() -> new AccessDeniedException(
                        "Acesso negado"));

        Page<Recompensa> resultado = recompensas.doCliente(
                usuario.getId(), paginacao(pagina, tamanho));

        return pagina(resultado, pagina, tamanho);
    }

    private Pagina<Resposta> pagina(
            Page<Recompensa> resultado,
            int pagina,
            int tamanho) {

        return new Pagina<>(
                resultado.map(this::resposta).getContent(),
                pagina,
                tamanho,
                resultado.getTotalElements());
    }

    private Pageable paginacao(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 100) {
            throw new ResponseStatusException(
                    BAD_REQUEST,
                    "Página deve ser >= 0 e tamanho de 1 a 100");
        }

        return PageRequest.of(
                pagina,
                tamanho,
                Sort.by(Sort.Direction.DESC, "emitidaEm")
                        .and(Sort.by("id")));
    }

    private void estabelecimentoAtivo(UUID id, boolean bloquear) {
        (bloquear
                ? estabelecimentos.buscarParaAlteracao(id)
                : estabelecimentos.findById(id))
                .filter(Estabelecimento::getAtivo)
                .orElseThrow(() -> new ResponseStatusException(
                        NOT_FOUND,
                        "Estabelecimento não encontrado ou inativo"));
    }

    private Resposta resposta(Recompensa recompensa) {
        CartaoFidelidade cartao = cartoes
                .findById(recompensa.getCartaoId())
                .orElseThrow();

        VersaoPrograma regras = versoes
                .findById(cartao.getVersaoProgramaId())
                .orElseThrow();

        return new Resposta(
                recompensa.getId(),
                cartao.getId(),
                cartao.getParticipacaoId(),
                recompensa.getEstabelecimentoId(),
                cartao.getNumero(),
                regras.getRecompensa(),
                recompensa.getResgatadaEm() == null
                        ? "DISPONIVEL" : "RESGATADA",
                recompensa.getEmitidaEm(),
                recompensa.getResgatadaEm());
    }
}