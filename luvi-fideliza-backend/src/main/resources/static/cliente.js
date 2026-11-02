(() => {
  'use strict';
  const $ = id => document.getElementById(id);
  const estado = { cartoes: [], premios: [] };

  function mensagem(t, erro = false) {
    const el = $('mensagem');
    el.textContent = t || '';
    el.classList.toggle('error', !!erro && !!t);
  }

  async function csrf() {
    const r = await fetch('/api/v1/auth/csrf', { credentials: 'same-origin' });
    if (!r.ok) throw Error('Não foi possível preparar a autenticação.');
    return r.json();
  }

  async function lerErro(r, fallback) {
    let dados = null;
    try { dados = await r.json(); } catch { /* corpo vazio ou não JSON */ }
    return Error(dados?.mensagem || dados?.detail || dados?.message || fallback);
  }

  function mostrarAcesso() {
    $('carteira').hidden = true;
    $('acesso').hidden = false;
    $('conta-topo').hidden = true;
    $('rodape').hidden = true;
    document.body.classList.remove('carteira-aberta');
    const dialog = $('painel-qr');
    if (dialog.open) dialog.close();
  }

  function mostrarCarteira(nome) {
    $('acesso').hidden = true;
    $('carteira').hidden = false;
    $('conta-topo').hidden = false;
    $('rodape').hidden = false;
    document.body.classList.add('carteira-aberta');
    const texto = (nome || '').trim();
    $('nome-cliente').textContent = texto || 'Minha conta';
  }

  function situacaoPremio(c) {
    const recompensaDoCartao = estado.premios.find(r => String(r.cartaoId) === String(c.id));
    const resgatada = !!(recompensaDoCartao && (String(recompensaDoCartao.status).toUpperCase() === 'RESGATADA' || recompensaDoCartao.resgatadaEm));
    const concluida = String(c.status || '').toUpperCase() === 'CONCLUIDO' || (Number(c.metaCarimbos) > 0 && Number(c.saldo) >= Number(c.metaCarimbos));
    const disponivel = !resgatada && (concluida || (recompensaDoCartao && String(recompensaDoCartao.status).toUpperCase() === 'DISPONIVEL'));
    return { recompensaDoCartao, resgatada, disponivel };
  }

  function imagemQr(src, alt) {
    const img = document.createElement('img');
    img.className = 'qr-cliente';
    img.alt = alt;
    img.src = src;
    return img;
  }

  function abrirQr() {
    const lista = $('lista-qr');
    lista.replaceChildren();
    if (!estado.cartoes.length) {
      const p = document.createElement('p');
      p.textContent = 'Você ainda não possui cartões para gerar um QR Code.';
      lista.append(p);
    }
    for (const c of estado.cartoes) {
      const item = document.createElement('article');
      item.className = 'qr-item';
      const loja = document.createElement('h3');
      loja.textContent = c.estabelecimento || 'Cartão';
      const campanha = document.createElement('p');
      campanha.textContent = c.titulo || '';
      const legenda = document.createElement('p');
      legenda.textContent = 'Mostre este QR no balcão para receber o carimbo.';
      item.append(loja, campanha, legenda, imagemQr('/api/v1/me/participacoes/' + encodeURIComponent(c.participacaoId) + '/qrcode', 'QR Code para mostrar no balcão'));
      const { recompensaDoCartao, disponivel } = situacaoPremio(c);
      if (disponivel && recompensaDoCartao) {
        const extra = document.createElement('div');
        extra.className = 'qr-extra';
        const titulo = document.createElement('h3');
        titulo.textContent = 'QR da recompensa';
        const detalhe = document.createElement('p');
        detalhe.textContent = 'Mostre este QR para o estabelecimento confirmar a entrega.';
        extra.append(titulo, detalhe, imagemQr('/api/v1/me/recompensas/' + encodeURIComponent(recompensaDoCartao.id) + '/qrcode', 'QR Code da recompensa para o balcão confirmar a entrega'));
        item.append(extra);
      }
      lista.append(item);
    }
    const dialog = $('painel-qr');
    if (!dialog.open) dialog.showModal();
  }

  async function carregar() {
    mensagem('');
    const r = await fetch('/api/v1/me/cartoes?tamanho=50', { credentials: 'same-origin', cache: 'no-store' });
    if (r.status === 401) { mostrarAcesso(); return; }
    if (!r.ok) throw await lerErro(r, 'Não foi possível consultar os cartões (' + r.status + ').');
    const d = await r.json();
    let nome = '';
    try {
      const me = await fetch('/api/v1/auth/me', { credentials: 'same-origin', cache: 'no-store' });
      if (me.ok) {
        const conta = await me.json();
        nome = (conta.nome || conta.email || '').trim();
      }
    } catch { /* o nome fica no identificador já conhecido, ou no fallback */ }
    mostrarCarteira(nome);
    estado.cartoes = d.conteudo || [];
    estado.premios = [];
    try {
      const rr = await fetch('/api/v1/me/recompensas?tamanho=100', { credentials: 'same-origin', cache: 'no-store' });
      if (rr.ok) {
        const dados = await rr.json();
        estado.premios = dados.conteudo || [];
      }
    } catch (e) { console.warn('Recompensas indisponíveis', e); }
    const area = $('cartoes');
    area.replaceChildren();
    if (!estado.cartoes.length) {
      const p = document.createElement('p');
      p.textContent = 'Você ainda não possui cartões vinculados. Peça ao estabelecimento um convite para participar.';
      area.append(p);
    }
    const cores = new Map();
    for (const c of estado.cartoes) {
      let visual = cores.get(c.estabelecimentoId);
      if (!visual) {
        try {
          const visualResp = await fetch('/api/v1/estabelecimentos/' + encodeURIComponent(c.estabelecimentoId) + '/visual-cartao', { credentials: 'same-origin' });
          if (visualResp.ok) visual = await visualResp.json();
        } catch { /* usa as cores padrão do cartão */ }
        visual = visual || { corCartao: '#30475A', corCarimbo: '#87A18F' };
        cores.set(c.estabelecimentoId, visual);
      }
      const box = document.createElement('article');
      box.className = 'cartao cartao-fidelidade';
      window.LuviCartao.aplicar(box, visual, c.estabelecimentoId);
      const loja = document.createElement('p');
      loja.className = 'estabelecimento';
      loja.textContent = c.estabelecimento;
      const h = document.createElement('h2');
      h.textContent = c.titulo;
      const premio = document.createElement('p');
      premio.className = 'premio';
      premio.textContent = 'Recompensa: ' + c.recompensa;
      const grade = document.createElement('div');
      grade.className = 'carimbos';
      grade.setAttribute('aria-label', c.saldo + ' de ' + c.metaCarimbos + ' carimbos');
      const total = Math.min(100, Math.max(1, Number(c.metaCarimbos) || 1));
      const saldo = Math.max(0, Number(c.saldo) || 0);
      for (let i = 0; i < total; i++) {
        const bolinha = document.createElement('span');
        bolinha.className = 'carimbo' + (i < saldo ? ' marcado' : '');
        if (i < saldo) bolinha.textContent = '✓';
        else window.LuviCartao.desenharIcone(bolinha, visual);
        bolinha.setAttribute('aria-hidden', 'true');
        grade.append(bolinha);
      }
      const status = document.createElement('p');
      status.className = 'cartao-status';
      status.textContent = c.saldo + ' de ' + c.metaCarimbos + ' carimbos · ' + c.status;
      box.append(loja, h, premio, grade, status);
      area.append(box);
      try {
        const { resgatada, disponivel } = situacaoPremio(c);
        if (resgatada || disponivel) {
          const selo = document.createElement('div');
          selo.className = 'luvi-premio-selo-v0893';
          selo.textContent = resgatada ? '✓ Recompensa resgatada' : '🎁 Prêmio disponível';
          box.insertBefore(selo, box.firstChild);
          const faixa = document.createElement('div');
          faixa.className = 'luvi-premio-faixa-v0893' + (resgatada ? ' resgatada' : '');
          const presente = document.createElement('span');
          presente.className = 'luvi-premio-icone-v0893';
          presente.textContent = resgatada ? '✓' : '🎁';
          const conteudo = document.createElement('div');
          const titulo = document.createElement('strong');
          titulo.textContent = resgatada ? 'Recompensa resgatada' : 'Parabéns! Você completou seu cartão!';
          const detalhe = document.createElement('p');
          detalhe.textContent = resgatada
            ? 'Este benefício já foi entregue pelo estabelecimento.'
            : 'Seu prêmio está disponível: ' + c.recompensa + '. Apresente este cartão ao estabelecimento para receber sua recompensa.';
          conteudo.append(titulo, detalhe);
          faixa.append(presente, conteudo);
          box.append(faixa);
        }
      } catch (erroAviso) { console.error('Falha ao exibir recompensa do cartão', c.id, erroAviso); }
    }
    const areaCampanhas = $('campanhas');
    areaCampanhas.replaceChildren();
    const lojas = [...new Set(estado.cartoes.map(c => c.estabelecimentoId))];
    for (const lojaId of lojas) {
      try {
        const resp = await fetch('/api/v1/estabelecimentos/' + encodeURIComponent(lojaId) + '/campanhas-cliente', { credentials: 'same-origin', cache: 'no-store' });
        if (!resp.ok) continue;
        for (const campanha of await resp.json()) {
          if (campanha.participando) continue;
          const card = document.createElement('article');
          card.className = 'cartao';
          const h = document.createElement('h2');
          h.textContent = campanha.titulo;
          const p = document.createElement('p');
          p.textContent = campanha.metaCarimbos + ' carimbos · ' + campanha.recompensa;
          const info = document.createElement('p');
          info.textContent = 'Disponível neste estabelecimento. Peça à equipe para participar.';
          card.append(h, p, info);
          areaCampanhas.append(card);
        }
      } catch { /* campanha indisponível não esconde os cartões */ }
    }
    if (!areaCampanhas.childElementCount) {
      const p = document.createElement('p');
      p.textContent = 'Nenhuma campanha adicional disponível para sua conta.';
      areaCampanhas.append(p);
    }
    if (d.total > estado.cartoes.length) mensagem('Exibindo os primeiros ' + estado.cartoes.length + ' cartões.');
    if ($('painel-qr').open) abrirQr();
  }

  $('login').addEventListener('submit', async e => {
    e.preventDefault();
    try {
      const f = new FormData(e.target);
      const c = await csrf();
      const dados = new URLSearchParams({ email: f.get('email'), password: f.get('senha') });
      const r = await fetch('/api/v1/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded', [c.headerName]: c.token },
        body: dados
      });
      if (!r.ok) throw Error('Telefone/e-mail ou senha inválidos.');
      await carregar();
    } catch (erro) { mensagem(erro.message, true); }
  });

  $('atualizar').onclick = () => carregar().catch(e => mensagem(e.message, true));
  $('abrir-qr').onclick = () => abrirQr();
  $('fechar-qr').onclick = () => $('painel-qr').close();
  $('sair').onclick = async () => {
    try {
      const c = await csrf();
      await fetch('/api/v1/auth/logout', { method: 'POST', credentials: 'same-origin', headers: { [c.headerName]: c.token } });
      mostrarAcesso();
      mensagem('Sessão encerrada.');
    } catch (e) { mensagem(e.message, true); }
  };
  carregar().catch(e => mensagem(e.message, true));
  if ('serviceWorker' in navigator && window.isSecureContext) navigator.serviceWorker.register('/cliente-sw.js').catch(() => {});
})();
