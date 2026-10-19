/* Estado da recompensa compartilhado pelas telas do cliente. */
window.LuviRecompensa = (() => {
  function estado(cartao, recompensas) {
    const correspondente = (recompensas || []).find(r => String(r.cartaoId) === String(cartao.id));
    if (correspondente && (correspondente.status === 'RESGATADA' || correspondente.resgatadaEm)) return 'resgatada';
    if (correspondente && correspondente.status === 'DISPONIVEL') return 'disponivel';
    if (String(cartao.status || '').toUpperCase() === 'CONCLUIDO' || (Number(cartao.metaCarimbos) > 0 && Number(cartao.saldo) >= Number(cartao.metaCarimbos))) return 'disponivel';
    return 'andamento';
  }
  function adicionar(cartaoEl, cartao, recompensas) {
    const situacao = estado(cartao, recompensas);
    if (situacao === 'andamento') return;
    const faixa = document.createElement('div');
    faixa.className = 'luvi-faixa-recompensa luvi-faixa-' + situacao;
    faixa.setAttribute('role', 'status');
    const icone = document.createElement('span');
    icone.className = 'luvi-faixa-icone';
    icone.setAttribute('aria-hidden', 'true');
    icone.textContent = situacao === 'resgatada' ? '✓' : '🎁';
    const textos = document.createElement('div');
    const titulo = document.createElement('strong');
    const descricao = document.createElement('p');
    if (situacao === 'resgatada') {
      titulo.textContent = 'Recompensa resgatada';
      descricao.textContent = 'Este benefício já foi entregue pelo estabelecimento.';
    } else if (situacao === 'disponivel') {
      titulo.textContent = 'Parabéns! Você completou seu cartão!';
      descricao.textContent = 'Seu prêmio está disponível: ' + cartao.recompensa + '. Apresente o cartão ao estabelecimento.';
    } else {
      titulo.textContent = 'Parabéns! Você completou seu cartão!';
      descricao.textContent = 'Você conquistou sua recompensa. Consulte o estabelecimento para confirmar o resgate.';
    }
    textos.append(titulo, descricao);
    faixa.append(icone, textos);
    if (situacao === 'disponivel') { const aviso = document.createElement('div'); aviso.className = 'luvi-recompensa-aviso'; aviso.textContent = 'Apresente este cartão ao estabelecimento para receber sua recompensa.'; textos.append(aviso); }
    const selo = document.createElement('span'); selo.className = 'luvi-recompensa-selo'; selo.textContent = situacao === 'resgatada' ? '✓ Recompensa resgatada' : '♧ Prêmio disponível'; cartaoEl.insertBefore(selo, cartaoEl.firstChild);
    cartaoEl.append(faixa);
  }
  return { adicionar, estado };
})();
