package br.com.luvi.fideliza.fidelidade.service;

import static org.springframework.http.HttpStatus.*;

import br.com.luvi.fideliza.equipe.PermissaoEstabelecimentoService;
import br.com.luvi.fideliza.estabelecimento.Estabelecimento;
import br.com.luvi.fideliza.estabelecimento.EstabelecimentoRepository;
import br.com.luvi.fideliza.fidelidade.dto.*;
import br.com.luvi.fideliza.fidelidade.entity.*;
import br.com.luvi.fideliza.fidelidade.repository.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProgramaService {
  private final ProgramaRepository programas;
  private final VersaoProgramaRepository versoes;
  private final EstabelecimentoRepository estabelecimentos;
  private final PermissaoEstabelecimentoService permissoes;

  public ProgramaService(
      ProgramaRepository programas,
      VersaoProgramaRepository versoes,
      EstabelecimentoRepository estabelecimentos,
      PermissaoEstabelecimentoService permissoes) {
    this.programas = programas;
    this.versoes = versoes;
    this.estabelecimentos = estabelecimentos;
    this.permissoes = permissoes;
  }

  @Transactional
  public ProgramaResponse criar(UUID estabelecimentoId, ProgramaRequest dados, String email) {
    UUID autor = permissoes.exigirProprietario(estabelecimentoId, email).getId();
    // O bloqueio no estabelecimento também serializa o primeiro cadastro, quando ainda não há
    // programa.
    estabelecimentoAtivo(estabelecimentoId, true);
    if (programas.findByEstabelecimentoId(estabelecimentoId).isPresent()) {
      throw new ResponseStatusException(
          CONFLICT, "Já existe um programa. Use a atualização de regras.");
    }
    ProgramaFidelidade programa =
        programas.save(new ProgramaFidelidade(estabelecimentoId, Instant.now()));
    VersaoPrograma versao = versoes.save(novaVersao(programa, dados, autor));
    return ProgramaResponse.de(programa, versao);
  }

  @Transactional(readOnly = true)
  public ProgramaResponse consultar(UUID estabelecimentoId, String email) {
    permissoes.exigirLeitura(estabelecimentoId, email);
    estabelecimentoAtivo(estabelecimentoId, false);
    ProgramaFidelidade programa = programa(estabelecimentoId);
    return ProgramaResponse.de(programa, versaoAtual(programa));
  }

  @Transactional
  public ProgramaResponse atualizar(
      UUID estabelecimentoId, AtualizarProgramaRequest dados, String email) {
    UUID autor = permissoes.exigirProprietario(estabelecimentoId, email).getId();
    estabelecimentoAtivo(estabelecimentoId, true);
    ProgramaFidelidade programa = programa(estabelecimentoId);
    if (dados.versaoAtual() != programa.getVersaoAtual()) {
      throw new ResponseStatusException(
          CONFLICT, "As regras foram alteradas. Consulte a versão atual antes de salvar.");
    }
    ProgramaRequest regras = dados.regras();
    VersaoPrograma atual = versaoAtual(programa);
    if (mesmasRegras(atual, regras)) {
      return ProgramaResponse.de(programa, atual);
    }
    programa.avancarVersao();
    VersaoPrograma nova = versoes.save(novaVersao(programa, regras, autor));
    return ProgramaResponse.de(programa, nova);
  }

  @Transactional(readOnly = true)
  public List<ProgramaResponse> historico(UUID estabelecimentoId, String email) {
    permissoes.exigirLeitura(estabelecimentoId, email);
    estabelecimentoAtivo(estabelecimentoId, false);
    ProgramaFidelidade programa = programa(estabelecimentoId);
    return versoes.findByProgramaIdOrderByNumeroDesc(programa.getId()).stream()
        .map(v -> ProgramaResponse.de(programa, v))
        .toList();
  }

  private void estabelecimentoAtivo(UUID id, boolean bloquear) {
    (bloquear ? estabelecimentos.buscarParaAlteracao(id) : estabelecimentos.findById(id))
        .filter(Estabelecimento::getAtivo)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    NOT_FOUND, "Estabelecimento não encontrado ou inativo"));
  }

  private ProgramaFidelidade programa(UUID estabelecimentoId) {
    return programas
        .findByEstabelecimentoId(estabelecimentoId)
        .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Programa ainda não cadastrado"));
  }

  private VersaoPrograma versaoAtual(ProgramaFidelidade programa) {
    return versoes
        .findByProgramaIdAndNumero(programa.getId(), programa.getVersaoAtual())
        .orElseThrow(() -> new IllegalStateException("Programa sem versão de regras"));
  }

  private VersaoPrograma novaVersao(
      ProgramaFidelidade programa, ProgramaRequest dados, UUID autor) {
    return new VersaoPrograma(
        programa.getId(),
        programa.getVersaoAtual(),
        dados.titulo().trim(),
        dados.metaCarimbos(),
        dados.recompensa().trim(),
        dados.criteriosCompra().trim(),
        Instant.now(),
        autor);
  }

  private boolean mesmasRegras(VersaoPrograma atual, ProgramaRequest dados) {
    return atual.getTitulo().equals(dados.titulo().trim())
        && atual.getMetaCarimbos() == dados.metaCarimbos()
        && atual.getRecompensa().equals(dados.recompensa().trim())
        && atual.getCriteriosCompra().equals(dados.criteriosCompra().trim());
  }
}
