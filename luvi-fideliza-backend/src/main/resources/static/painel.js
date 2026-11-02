// UUID v4 para chaves de idempotência, inclusive em HTTP na rede local.
function gerarUUIDSeguro() {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID();
  }
  if (typeof crypto === 'undefined' || typeof crypto.getRandomValues !== 'function') {
    throw new Error('Identificador seguro indisponível. Acesse o painel por HTTPS.');
  }
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, b => b.toString(16).padStart(2, '0')).join('');
  return `${hex.slice(0,8)}-${hex.slice(8,12)}-${hex.slice(12,16)}-${hex.slice(16,20)}-${hex.slice(20)}`;
}

/* LUVI Fideliza+ — painel local integrado. Sem dependências externas. */
(() => {
  "use strict";
  const $ = id => document.getElementById(id);
  const todos = seletor => [...document.querySelectorAll(seletor)];
  const state = { conta: null, negocios: [], negocio: null, direitos: {}, view: "inicio", busy: false,
    pessoa: null, programa: undefined, dirty: false, detalhe: "beneficios", busca: "", clientes: [],
    campanhasRapidas: [], recompensaDestaque: null,
    paginas: { clientes: 0, recompensas: 0, cartoes: 0, historico: 0, beneficios: 0 }, totais: {} };
  const tamanho = tipo => tipo === "clientes" ? 10 : tipo === "recompensas" ? 12 : 6;
  const raiz = () => "/api/v1/estabelecimentos/" + encodeURIComponent(state.negocio.id);
  const painel = () => "/api/v1/painel/estabelecimentos/" + encodeURIComponent(state.negocio.id);
  const pessoaRaiz = () => raiz() + "/participantes/" + encodeURIComponent(state.pessoa.id);
  const operar = () => !!state.direitos.podeOperar;
  const dono = () => !!state.direitos.podeEditarPrograma;
  const formatar = data => new Date(data).toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
  const iniciais = nome => nome.trim().split(/\s+/).slice(0, 2).map(p => p[0]).join("").toUpperCase();
  const texto = (tag, valor, classe) => { const e = document.createElement(tag); e.textContent = valor; if (classe) e.className = classe; return e; };
  const vazio = (id, mensagem) => $(id).replaceChildren(texto("p", mensagem, "loading-placeholder"));
  const statusBadge = (valor, ok = true) => texto("span", valor, "badge " + (ok ? "positive" : "neutral"));
  function notificar(mensagem, erro = false, destino) {
    $("feedback-texto").textContent = mensagem;
    $("feedback").className = "feedback" + (erro ? " error" : ""); $("feedback").hidden = false;
    const id = destino || (state.conta ? state.view + "-aviso" : "login-aviso");
    if ($(id)) { $(id).textContent = mensagem; $(id).className = "notice" + (erro ? " error" : ""); $(id).hidden = false; }
  }
  function limparAvisos() { todos(".notice").forEach(e => { e.hidden = true; e.textContent = ""; }); $("feedback").hidden = true; }
  function sairDaTela() {
    state.conta = null; state.negocio = null; state.pessoa = null; state.direitos = {}; state.programa = undefined; state.dirty = false;
    $("app").hidden = true; $("login").hidden = false; $("senha").value = "";
    if ($("modal-cliente").open) $("modal-cliente").close();
  }
  async function api(url, opcoes = {}) {
    const method = opcoes.method || "GET", headers = new Headers(opcoes.headers || {});
    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
      const r = await fetch("/api/v1/auth/csrf", { credentials: "same-origin", cache: "no-store" });
      if (!r.ok) throw new Error("Não foi possível preparar a operação. Tente novamente.");
      const csrf = await r.json(); headers.set(csrf.headerName, csrf.token);
    }
    const r = await fetch(url, { ...opcoes, method, headers, credentials: "same-origin", cache: "no-store" });
    let dados = null; const body = await r.text(); try { dados = body ? JSON.parse(body) : null; } catch {}
    if (!r.ok) {
      if (r.status === 401) sairDaTela();
      const mensagem = dados?.mensagem || (r.status === 401 ? "Entre novamente. Confira o e-mail ou WhatsApp e a senha." : r.status === 403 ? "Seu acesso não permite esta ação. Atualize o painel para conferir as permissões." : "Não foi possível concluir a operação. Código " + r.status + ".");
      const erro = new Error(mensagem + (dados?.campos?.length ? " Confira: " + dados.campos.join(", ") + "." : "")); erro.status = r.status; throw erro;
    }
    return dados;
  }
  const json = (dados, method = "POST", chave) => ({ method, headers: { "Content-Type": "application/json", ...(chave ? { "Idempotency-Key": chave } : {}) }, body: JSON.stringify(dados) });
  async function executar(acao, destino) {
    if (state.busy) return;
    state.busy = true; controles();
    try { await acao(); }
    catch (e) { notificar(e.message || "Não foi possível concluir. Verifique sua conexão e tente novamente.", true, state.conta ? destino : "login-aviso"); }
    finally { state.busy = false; controles(); }
  }
  function chavePendente(tipo, id = "") { return JSON.stringify(["luvi-painel-v6", state.conta.email, state.negocio.id, tipo, id]); }
  function lerPendente(tipo, id) {
    const atual = chavePendente(tipo, id), v = sessionStorage.getItem(atual);
    if (v) return JSON.parse(v);
    // Preserva reenvios sem confirmação da versão anterior do painel.
    const antiga = tipo === "resgate" ? JSON.stringify(["luvi-resgate", state.conta.email, state.negocio.id, id])
      : JSON.stringify(["luvi-painel", tipo, state.conta.email, state.negocio.id, tipo === "carimbo" ? id : null]);
    const legado = sessionStorage.getItem(antiga);
    if (!legado) return null;
    const op = tipo === "resgate" ? { chave: legado, dados: {} } : JSON.parse(legado);
    sessionStorage.setItem(atual, JSON.stringify(op)); return op;
  }
  function pendente(tipo, dados, id) {
    const anterior = lerPendente(tipo, id); if (anterior) return anterior;
    const operacao = { chave: gerarUUIDSeguro(), dados };
    sessionStorage.setItem(chavePendente(tipo, id), JSON.stringify(operacao)); return operacao;
  }
  function concluirPendente(tipo, id) {
    sessionStorage.removeItem(chavePendente(tipo, id));
    const antiga = tipo === "resgate" ? JSON.stringify(["luvi-resgate", state.conta.email, state.negocio.id, id])
      : JSON.stringify(["luvi-painel", tipo, state.conta.email, state.negocio.id, tipo === "carimbo" ? id : null]);
    sessionStorage.removeItem(antiga);
  }
  function controles() {
    todos("button,input,select,textarea").forEach(e => e.disabled = state.busy);
    $("busy").hidden = !state.busy;
    $("nav-equipe").hidden = !dono();
    for (const id of ["novo-cliente", "nome-cliente", "cadastrar-cliente", "carimbar"]) $(id).disabled = state.busy || !operar();
    $("form-carimbo").hidden = !operar();
    const gerar = $("gerar-convite");
    const podeConvidar = !!state.pessoa && !state.pessoa.vinculadoAoCliente && operar();
    gerar.hidden = !podeConvidar;
    gerar.disabled = state.busy || !podeConvidar;
    const rapido = $("carimbar-rapido");
    if (rapido) rapido.disabled = state.busy || !operar();
    todos("[data-resgatar]").forEach(b => b.disabled = state.busy || !operar());
    for (const id of ["programa-titulo", "programa-meta", "programa-recompensa", "programa-criterios", "salvar-programa"]) $(id).disabled = state.busy || !dono() || state.programa === undefined;
    $("equipe-email").disabled = state.busy || !dono();
    todos("[data-team-action]").forEach(b => b.disabled = state.busy || !dono());
    Object.keys(state.paginas).forEach(tipo => {
      const anterior = $(tipo + "-anterior"), proxima = $(tipo + "-proxima");
      if (anterior) anterior.disabled = state.busy || state.paginas[tipo] <= 0;
      if (proxima) proxima.disabled = state.busy || (state.paginas[tipo] + 1) * tamanho(tipo) >= (state.totais[tipo] || 0);
    });
  }
  function paginar(tipo, dados) {
    state.totais[tipo] = dados.total;
    $(tipo + "-pagina").textContent = dados.total ? (state.paginas[tipo] + 1) + " / " + Math.ceil(dados.total / tamanho(tipo)) + " · " + dados.total + " registros" : "Nenhum registro";
  }
  const parametros = tipo => "pagina=" + state.paginas[tipo] + "&tamanho=" + tamanho(tipo);
  function trocarDetalhe(tipo) {
    state.detalhe = tipo;
    todos("[data-detail]").forEach(b => b.classList.toggle("active", b.dataset.detail === tipo));
    todos("[data-detail-page]").forEach(e => e.hidden = e.dataset.detailPage !== tipo);
    $("cliente-recompensas-pager").hidden = tipo !== "beneficios";
  }
  async function navegar(view) {
    if (view === "equipe" && !dono()) return;
    state.view = view; limparAvisos();
    todos("[data-page]").forEach(e => e.hidden = e.dataset.page !== view);
    todos("[data-view]").forEach(b => { const ativo = b.dataset.view === view || (["clientes", "recompensas"].includes(view) && b.dataset.view === "atendimento") || (view === "programa" && b.dataset.view === "inicio"); b.classList.toggle("active", ativo); if (ativo) b.setAttribute("aria-current", "page"); else b.removeAttribute("aria-current"); });
    if (!state.negocio) return;
    if (view === "campanhas") { const frame=$("campanhas-frame"); const url="/campanhas.html?embedded=1&estabelecimento="+encodeURIComponent(state.negocio.id || $("estabelecimento").value); if(frame.getAttribute("src")!==url)frame.setAttribute("src",url); }
    if (view === "inicio") await carregarInicio();
    if (view === "atendimento") await prepararAtendimento();
    if (view === "clientes") await carregarClientes();
    if (view === "recompensas") await carregarRecompensas();
    if (view === "programa" && state.programa === undefined) await carregarPrograma();
    if (view === "equipe") await carregarEquipe();
  }
  async function encerrarSessao() {
    try { await api("/api/v1/auth/logout", { method: "POST" }); }
    catch { /* a sessão pode já ter sido encerrada */ }
  }
  async function iniciar() {
    state.conta = await api("/api/v1/auth/me");
    const perfis = state.conta.perfis || [];
    if (!perfis.includes("ROLE_OPERADOR") && !perfis.includes("ROLE_ADMIN")) {
      await encerrarSessao();
      sairDaTela();
      notificar("Esta conta é de cliente. O painel é da barbearia.", true, "login-aviso");
      return;
    }
    state.negocios = await api("/api/v1/painel/estabelecimentos");
    $("conta").textContent = state.conta.email;
    $("link-admin").hidden = !state.conta.perfis.includes("ROLE_ADMIN");
    $("estabelecimento").replaceChildren(...state.negocios.map(n => new Option(n.nome, n.id)));
    const q = new URLSearchParams(location.search);
    if (q.get("loja") && [...$("estabelecimento").options].some(o => o.value === q.get("loja"))) $("estabelecimento").value = q.get("loja");
    $("login").hidden = true; $("app").hidden = false;
    await trocarNegocio();
    if ((q.get("participacao") || q.get("recompensa")) && state.negocio) await abrirAtalho(q);
  }
  async function trocarNegocio() {
    state.negocio = state.negocios.find(n => n.id === $("estabelecimento").value) || null;
    state.direitos = {}; state.pessoa = null; state.programa = undefined; state.dirty = false; state.busca = "";
    Object.keys(state.paginas).forEach(k => state.paginas[k] = 0); state.totais = {};
    $("busca").value = ""; $("cliente-detalhe").hidden = true; $("cliente-vazio").hidden = false;
    $("form-programa").reset(); $("programa-preview").replaceChildren(); $("programa-historico").replaceChildren();
    $("lista-equipe").replaceChildren(); $("lista-convites-equipe").replaceChildren(); $("equipe-email").value = "";
    for (const tipo of ["cliente", "equipe"]) { $("convite-" + tipo).hidden = true; $("link-" + tipo).value = ""; }
    $("sem-negocio").hidden = !!state.negocio; $("views").hidden = !state.negocio;
    if (state.negocio) state.direitos = await api(painel() + "/permissoes");
    $("perfil").textContent = dono() ? "Proprietário" : operar() ? "Funcionário" : state.conta.perfis.includes("ROLE_ADMIN") ? "Administração · consulta" : "Conta sem vínculo";
    await navegar(operar() ? "atendimento" : "inicio");
  }
  async function programaAtual() { try { return await api(raiz() + "/programa"); } catch (e) { if (e.status === 404) return null; throw e; } }
  async function carregarInicio() {
    const r = await api(painel() + "/resumo");
    $("stat-clientes").textContent = r.participantes;
    $("stat-vinculados").textContent = r.contasVinculadas + " com conta vinculada";
    $("stat-cartoes").textContent = r.cartoesEmAndamento;
    $("stat-disponiveis").textContent = r.recompensasDisponiveis;
    $("stat-resgatadas").textContent = r.recompensasResgatadas;
    const regras = await programaAtual();
    if (!regras) vazio("inicio-programa", "Seu programa ainda não foi configurado. O proprietário pode criar as primeiras regras em Programa.");
    else {
      $("inicio-programa").replaceChildren(texto("h3", regras.titulo), texto("p", regras.metaCarimbos + " carimbos para conquistar o benefício · Versão " + regras.versao, "meta-text"), texto("p", "♡  " + regras.recompensa, "reward-text"));
    }
  }
  async function carregarClientes() {
    const dados = await api(raiz() + "/participantes?" + parametros("clientes") + "&busca=" + encodeURIComponent(state.busca));
    state.clientes = dados.conteudo;
    $("lista-clientes").replaceChildren(); $("total-clientes").textContent = dados.total; paginar("clientes", dados);
    if (!dados.conteudo.length) vazio("lista-clientes", state.busca ? "Nenhum cliente encontrado com esse nome ou WhatsApp." : "Seu primeiro vínculo começa aqui. Cadastre um cliente para abrir o cartão.");
    dados.conteudo.forEach(p => {
      if (state.pessoa?.id === p.id) state.pessoa = p;
      const b = texto("button", "", "client-row" + (p.id === state.pessoa?.id ? " active" : ""));
      b.dataset.pessoa = p.id;
      const info = document.createElement("span"); info.append(texto("strong", p.nomeExibicao), texto("small", p.vinculadoAoCliente ? "Conta vinculada" : "Cartão no estabelecimento"));
      b.append(texto("span", iniciais(p.nomeExibicao), "avatar"), info);
      b.onclick = () => executar(() => selecionar(p), "clientes-aviso"); $("lista-clientes").append(b);
    });
    if (state.pessoa) identidadeCliente();
  }
  function identidadeCliente() {
    $("cliente-nome").textContent = state.pessoa.nomeExibicao;
    $("cliente-avatar").textContent = iniciais(state.pessoa.nomeExibicao);
    $("cliente-vinculo").textContent = state.pessoa.vinculadoAoCliente ? "Conta vinculada" : "Ainda sem conta vinculada";
    const contato = $("cliente-whatsapp");
    if (contato) contato.value = state.pessoa.whatsapp || "";
    const qr = $("qr-cliente");
    if (qr) qr.src = pessoaRaiz() + "/qrcode";
  }
  async function prepararAtendimento() {
    const area = $("campanhas-rapido");
    if (!area) return;
    area.replaceChildren();
    const campanhas = await api(raiz() + "/campanhas");
    state.campanhasRapidas = campanhas.filter(c => c.ativa);
    const unica = $("campanha-unica");
    if (state.campanhasRapidas.length <= 1) {
      area.hidden = true;
      unica.hidden = state.campanhasRapidas.length !== 1;
      unica.textContent = state.campanhasRapidas.length === 1 ? "Campanha: " + state.campanhasRapidas[0].titulo : "Nenhuma campanha ativa. O proprietário precisa ativar um programa.";
      return;
    }
    unica.hidden = true;
    area.hidden = false;
    state.campanhasRapidas.forEach(c => {
      const linha = texto("label", "", "campanha-opcao");
      const check = document.createElement("input");
      check.type = "checkbox"; check.value = c.id; check.checked = !!c.principal;
      linha.append(check, texto("span", c.titulo + (c.principal ? " · principal" : "")));
      area.append(linha);
    });
  }
  async function abrirAtalho(q) {
    if (q.get("participacao")) {
      await navegar("clientes");
      const pessoa = await api(raiz() + "/participantes/" + encodeURIComponent(q.get("participacao")));
      await selecionar(pessoa);
      $("form-carimbo").scrollIntoView({ block: "center" });
      notificar("Cliente localizado pelo QR. Confira o cartão e registre o carimbo.", false, "clientes-aviso");
      return;
    }
    if (q.get("recompensa")) {
      state.recompensaDestaque = q.get("recompensa");
      await navegar("recompensas");
      notificar("Recompensa localizada pelo QR. Confirme a entrega se ela ainda estiver disponível.", false, "recompensas-aviso");
    }
  }
  async function restaurarCarimbo() {
    const area = $("campanhas-atendimento");
    area.replaceChildren();
    const op = lerPendente("atendimento", state.pessoa.id);
    const campanhas = await api(raiz() + "/campanhas");
    const ativas = campanhas.filter(c => c.ativa);
    ativas.forEach(c => {
      const linha = texto("label", "", "campanha-opcao");
      const check = document.createElement("input");
      check.type = "checkbox"; check.value = c.id;
      check.checked = op ? op.dados.campanhas.includes(c.id) : false;
      check.disabled = !!op || state.busy;
      linha.append(check, texto("span", c.titulo + " — " + c.metaCarimbos + " carimbos"));
      area.append(linha);
    });
    $("carimbar").textContent = op ? "Reenviar atendimento" : "Registrar carimbos selecionados";
    $("carimbo-ajuda").textContent = op
      ? "Atendimento pendente de confirmação. Reenvie sem alterar as campanhas."
      : "Cada campanha selecionada recebe 1 carimbo. É permitido atender o mesmo cliente mais de uma vez no dia.";
    if (!ativas.length) area.append(texto("p", "Nenhuma campanha ativa.", "muted"));
  }
  async function selecionar(p) {
    state.pessoa = p; ["cartoes", "historico", "beneficios"].forEach(t => state.paginas[t] = 0);
    $("convite-cliente").hidden = true; $("link-cliente").value = ""; $("clientes-aviso").hidden = true;
    $("cliente-vazio").hidden = true; $("cliente-detalhe").hidden = false; identidadeCliente(); await restaurarCarimbo();
    todos("[data-pessoa]").forEach(b => b.classList.toggle("active", b.dataset.pessoa === p.id));
    trocarDetalhe("beneficios"); await carregarCliente();
  }
  function cartaoVisual(c) {
    const bloco = texto("div", "", "loyalty-card");
    const topo = texto("div", "LUVI fideliza+", "card-top"); topo.append(texto("span", c.ciclo ? "Ciclo " + c.ciclo + " · Versão " + c.versao : "Seu programa"));
    bloco.append(topo, texto("h3", c.titulo || "Nome do seu programa"), texto("p", c.recompensa || "Uma recompensa especial para seus clientes"));
    const meta = Math.max(1, Number(c.metaCarimbos) || 1), saldo = Math.max(0, Number(c.saldo) || 0);
    const dots = texto("div", "", "stamp-dots"); dots.setAttribute("aria-hidden", "true");
    if (meta <= 20) for (let n = 0; n < meta; n++) dots.append(texto("span", n < saldo ? "✓" : n === meta - 1 ? "♡" : "·", "stamp-dot" + (n < saldo ? " filled" : "")));
    else { const barra = texto("div", "", "card-progress"); const valor = texto("span", ""); valor.style.width = Math.min(100, saldo / meta * 100) + "%"; barra.append(valor); bloco.append(barra); }
    bloco.append(dots);
    const rodape = texto("div", "", "card-footer"); rodape.append(texto("span", saldo + " de " + meta + " carimbos"), texto("span", saldo >= meta ? "Recompensa conquistada ♡" : "Faltam " + (meta - saldo) + " para o benefício")); bloco.append(rodape); return bloco;
  }
  async function carregarCliente() {
    const ultimos = await api(pessoaRaiz() + "/cartoes?tamanho=1");
    $("cartao-atual").replaceChildren(...ultimos.conteudo.map(cartaoVisual));
    await carregarDetalhe(state.detalhe);
  }
  async function carregarDetalhe(tipo) {
    if (!state.pessoa || tipo === "convite") return;
    const endpoint = { beneficios: "recompensas", cartoes: "cartoes", historico: "carimbos" }[tipo];
    const dados = await api(pessoaRaiz() + "/" + endpoint + "?" + parametros(tipo)); paginar(tipo, dados);
    const alvo = { beneficios: "cliente-recompensas", cartoes: "cliente-cartoes", historico: "cliente-historico" }[tipo];
    $(alvo).replaceChildren();
    if (!dados.conteudo.length) vazio(alvo, tipo === "beneficios" ? "As recompensas aparecem aqui quando o cliente completa um cartão." : "Nenhum registro nesta etapa.");
    dados.conteudo.forEach(v => {
      if (tipo === "beneficios") { $(alvo).append(recompensaCliente(v)); return; }
      const bloco = texto("div", "", tipo === "cartoes" ? "history-card" : "timeline-item");
      if (tipo === "cartoes") bloco.append(texto("h3", "Ciclo " + v.ciclo + " · " + (v.status === "CONCLUIDO" ? "Concluído" : "Em andamento")), texto("p", v.saldo + " / " + v.metaCarimbos + " carimbos\n" + v.recompensa + "\nRegras: versão " + v.versao + " · " + v.criteriosCompra));
      else bloco.append(texto("strong", v.referenciaCompra), texto("small", formatar(v.registradoEm) + " · Saldo após a compra: " + v.saldoApos + (v.concluiuCartao ? " · Cartão concluído" : "")));
      $(alvo).append(bloco);
    });
  }
  function botaoResgate(r, contexto) {
    const b = texto("button", "Confirmar entrega", "primary"); b.dataset.resgatar = r.id;
    b.onclick = () => executar(async () => {
      const op = pendente("resgate", {}, r.id);
      await api(raiz() + "/recompensas/" + r.id + "/resgate", { method: "POST", headers: { "Idempotency-Key": op.chave } });
      concluirPendente("resgate", r.id); notificar("Entrega confirmada. Recompensa resgatada.", false, contexto);
      try { if (state.view === "clientes") await carregarCliente(); else await carregarRecompensas(); }
      catch (e) { notificar("Resgate confirmado. Atualize a consulta para ver o resultado. " + e.message, true, contexto); }
    }, contexto); return b;
  }
  function recompensaCliente(r) {
    const b = texto("div", "", "reward-item"), info = document.createElement("div");
    info.append(statusBadge(r.status === "DISPONIVEL" ? "Disponível" : "Resgatada", r.status === "DISPONIVEL"), texto("h3", r.descricao), texto("p", "Ciclo " + r.ciclo + " · " + (r.resgatadaEm ? "Entregue em " + formatar(r.resgatadaEm) + (r.entreguePor ? " por " + r.entreguePor : "") : "Conquistada em " + formatar(r.emitidaEm)))); b.append(info);
    if (r.status === "DISPONIVEL" && operar()) b.append(botaoResgate(r, "clientes-aviso")); return b;
  }
  async function carregarRecompensas() {
    let dados = await api(painel() + "/recompensas?" + parametros("recompensas") + "&status=" + $("filtro-recompensas").value);
    if (!dados.conteudo.length && state.paginas.recompensas > 0) { state.paginas.recompensas--; return carregarRecompensas(); }
    $("lista-recompensas").replaceChildren(); paginar("recompensas", dados);
    if (!dados.conteudo.length) vazio("lista-recompensas", "Nenhuma recompensa neste filtro. Cada cartão completo abre uma nova conquista.");
    dados.conteudo.forEach(r => {
      const card = texto("article", "", "reward-tile"); card.dataset.recompensa = r.id; const top = texto("div", "", "reward-head"); top.append(statusBadge(r.status === "DISPONIVEL" ? "Disponível" : "Resgatada", r.status === "DISPONIVEL"), texto("small", "Ciclo " + r.ciclo, "muted"));
      card.append(top, texto("h3", r.descricao), texto("p", r.participante), texto("p", r.resgatadaEm ? "Entregue em " + formatar(r.resgatadaEm) + (r.entreguePor ? " por " + r.entreguePor : "") : "Conquistada em " + formatar(r.emitidaEm)));
      const acoes = texto("div", "", "reward-actions"), ver = texto("button", "Ver cliente →", "text-button");
      ver.onclick = () => executar(async () => { await navegar("clientes"); await selecionar({ id: r.participacaoId, nomeExibicao: r.participante, vinculadoAoCliente: false }); await atualizarIdentidadeSelecionada(); }, "clientes-aviso"); acoes.append(ver);
      if (r.status === "DISPONIVEL" && operar()) acoes.append(botaoResgate(r, "recompensas-aviso")); card.append(acoes); $("lista-recompensas").append(card);
    });
    if (state.recompensaDestaque) {
      const alvo = document.querySelector('[data-recompensa="' + state.recompensaDestaque + '"]');
      if (alvo) { alvo.classList.add("client-row"); alvo.scrollIntoView({ block: "center" }); }
    }
  }
  async function atualizarIdentidadeSelecionada() {
    // Consulta pontual evita inferir o vínculo de um participante fora da página de clientes.
    if (!state.pessoa) return;
    state.pessoa = await api(raiz() + "/participantes/" + state.pessoa.id);
    identidadeCliente();
  }
  function preencherPrograma(p) {
    state.programa = p; state.dirty = false;
    $("programa-titulo").value = p?.titulo || ""; $("programa-meta").value = p?.metaCarimbos || "";
    $("programa-recompensa").value = p?.recompensa || ""; $("programa-criterios").value = p?.criteriosCompra || "";
    $("programa-versao").textContent = p ? "Versão " + p.versao : "Novo programa";
    $("programa-permissao").textContent = dono() ? "Ajuste as regras com clareza e mantenha um bom acordo com seus clientes." : "Consulta das regras vigentes. Somente o proprietário pode editar o programa."; previewPrograma();
  }
  function previewPrograma() {
    $("programa-preview").replaceChildren(cartaoVisual({ titulo: $("programa-titulo").value, recompensa: $("programa-recompensa").value, metaCarimbos: $("programa-meta").value || 5, saldo: 0 }));
  }
  async function carregarPrograma() { const regras = await programaAtual(); preencherPrograma(regras); }
  async function historicoPrograma() {
    if (!state.programa) { vazio("programa-historico", "Salve o primeiro programa para iniciar o histórico."); return; }
    const lista = await api(raiz() + "/programa/historico"); $("programa-historico").replaceChildren();
    lista.forEach(p => { const item = texto("article", "", "version-item"); item.append(texto("h3", "Versão " + p.versao + " · " + p.titulo), texto("p", p.metaCarimbos + " carimbos · " + p.recompensa + "\n" + p.criteriosCompra + "\n" + formatar(p.criadoEm))); $("programa-historico").append(item); });
  }
  async function carregarEquipe() {
    const equipe = await api(raiz() + "/equipe"), convites = await api(raiz() + "/equipe/convites");
    $("lista-equipe").replaceChildren(); $("lista-convites-equipe").replaceChildren();
    equipe.forEach(m => {
      const linha = texto("div", "", "team-person"), info = texto("div", "", "person-data");
      info.append(texto("strong", m.nome), texto("small", m.email), statusBadge(m.papel === "PROPRIETARIO" ? "Proprietário" : m.ativo ? "Funcionário ativo" : "Acesso desativado", m.ativo));
      linha.append(texto("span", iniciais(m.nome), "avatar"), info);
      if (m.papel === "FUNCIONARIO") {
        const b = texto("button", m.ativo ? "Desativar" : "Reativar", "ghost"); b.dataset.teamAction = m.id;
        b.onclick = () => executar(async () => { if (!confirm((m.ativo ? "Desativar" : "Reativar") + " o acesso de " + m.nome + " a este estabelecimento?")) return; await api(raiz() + "/equipe/" + m.id, json({ ativo: !m.ativo }, "PATCH")); notificar("Acesso atualizado.", false, "equipe-aviso"); await carregarEquipe(); }, "equipe-aviso"); linha.append(b);
      }
      $("lista-equipe").append(linha);
    });
    if (!convites.length) vazio("lista-convites-equipe", "Nenhum convite gerado ainda.");
    convites.forEach(c => {
      const linha = texto("div", "", "invite-item"); linha.append(texto("strong", c.email), texto("small", (c.status === "PENDENTE" ? "Pendente · Válido até " : c.status === "ACEITO" ? "Aceito · " : "Expirado ou revogado · ") + formatar(c.expiraEm)));
      if (c.status === "PENDENTE") { const b = texto("button", "Revogar convite", "text-button"); b.dataset.teamAction = c.id; b.onclick = () => executar(async () => { if (!confirm("Revogar este convite de funcionário?")) return; await api(raiz() + "/equipe/convites/" + c.id, { method: "DELETE" }); $("convite-equipe").hidden = true; $("link-equipe").value = ""; await carregarEquipe(); notificar("Convite revogado.", false, "equipe-aviso"); }, "equipe-aviso"); linha.append(b); }
      $("lista-convites-equipe").append(linha);
    });
  }
  function linkConvite(tipo, dados) {
    const link = new URL("/convite.html", location.origin);
    link.hash = new URLSearchParams({ token: dados.token, ...(tipo === "equipe" ? { tipo: "equipe" } : {}) }).toString();
    $("link-" + tipo).value = link.href; $("convite-" + tipo).hidden = false;
    $("validade-" + tipo).textContent = "Válido até " + formatar(dados.expiraEm) + ". Abra em uma janela anônima para testar com outra conta.";
  }
  async function copiar(tipo) {
    try { await navigator.clipboard.writeText($("link-" + tipo).value); notificar("Convite copiado."); }
    catch { $("link-" + tipo).disabled = false; $("link-" + tipo).select(); notificar("Selecione e copie o link com Ctrl+C."); }
  }
  // Ações e formulários.
  todos("[data-atendimento]").forEach(b => b.onclick = () => executar(async () => {
    const acao = b.dataset.atendimento;
    if (acao === "recompensa") { await navegar("recompensas"); return; }
    await navegar("clientes");
    const contexto = $("contexto-atendimento");
    if (contexto) contexto.textContent = ({buscar:"Busque pelo nome ou WhatsApp e selecione o cliente.", cadastrar:"Informe os dados para cadastrar um novo cliente.", carimbar:"Selecione um cliente para registrar carimbos nas campanhas desejadas."})[acao] || "";
    if (acao === "cadastrar") {
      if (!operar()) { notificar("Seu acesso não permite cadastrar clientes.", true, "clientes-aviso"); return; }
      $("novo-cliente").click(); return;
    }
    if (acao === "carimbar" && state.pessoa) {
      $("campanhas-atendimento").scrollIntoView({ behavior: "smooth", block: "center" }); $("form-carimbo").scrollIntoView({ behavior: "smooth", block: "center" });
    } else { $("busca").focus(); }
  }, "clientes-aviso"));
  todos("[data-voltar-inicio]").forEach(b => b.onclick = () => executar(() => navegar(b.closest("[data-page]")?.dataset.page === "programa" ? "inicio" : "atendimento")));
  todos("[data-view]").forEach(b => b.onclick = () => executar(() => navegar(b.dataset.view)));
  todos("[data-go]").forEach(b => b.onclick = () => executar(() => navegar(b.dataset.go)));
  todos("[data-detail]").forEach(b => b.onclick = () => executar(async () => { trocarDetalhe(b.dataset.detail); await carregarDetalhe(b.dataset.detail); }, "clientes-aviso"));
  $("fechar-feedback").onclick = () => $("feedback").hidden = true;
  $("form-login").onsubmit = e => { e.preventDefault(); executar(async () => { await api("/api/v1/auth/login", { method: "POST", body: new URLSearchParams({ email: $("email").value.trim(), password: $("senha").value }) }); $("senha").value = ""; limparAvisos(); await iniciar(); }, "login-aviso"); };
  $("sair").onclick = () => executar(async () => { if (state.dirty && !confirm("Sair e descartar as alterações não salvas do programa?")) return; await api("/api/v1/auth/logout", { method: "POST" }); sairDaTela(); limparAvisos(); });
  $("estabelecimento").onchange = () => executar(async () => { if (state.dirty && !confirm("Trocar de estabelecimento e descartar o rascunho do programa?")) { $("estabelecimento").value = state.negocio.id; return; } await trocarNegocio(); });
  $("atualizar-inicio").onclick = () => executar(carregarInicio);
  $("form-busca").onsubmit = e => { e.preventDefault(); executar(async () => { state.busca = $("busca").value.trim(); state.paginas.clientes = 0; await carregarClientes(); }, "lista-aviso"); };
  $("novo-cliente").onclick = () => {
    limparAvisos(); const op = lerPendente("cadastro"); $("nome-cliente").value = op?.dados.nomeExibicao || ""; $("nome-cliente").readOnly = !!op; $("whatsapp-cliente").value = op?.dados.whatsapp || ""; $("whatsapp-cliente").readOnly = !!op; $("cadastro-pendente").hidden = !op;
    $("cadastrar-cliente").textContent = op ? "Reenviar cadastro pendente" : "Cadastrar e abrir cartão"; $("modal-cliente").showModal(); $("nome-cliente").focus();
  };
  $("fechar-modal").onclick = () => $("modal-cliente").close();
  $("modal-cliente").addEventListener("cancel", e => { if (state.busy) e.preventDefault(); });
  $("form-cliente").onsubmit = e => { e.preventDefault(); executar(async () => {
    const nome = $("nome-cliente").value.trim(); if (!nome) throw new Error("Informe o nome do cliente.");
    const whatsapp = $("whatsapp-cliente").value.replace(/\D/g, ""); if (!/^(?:55)?[1-9][0-9]9[0-9]{8}$/.test(whatsapp)) throw new Error("Informe um celular com DDD válido.");
    const op = pendente("cadastro", { nomeExibicao: nome, whatsapp }); $("nome-cliente").value = op.dados.nomeExibicao; $("nome-cliente").readOnly = true; $("whatsapp-cliente").readOnly = true; $("cadastro-pendente").hidden = false;
    const salvo = await api(raiz() + "/participantes", json(op.dados, "POST", op.chave)); concluirPendente("cadastro");
    $("modal-cliente").close(); state.busca = ""; $("busca").value = ""; state.paginas.clientes = 0;
    notificar("Cliente cadastrado e primeiro cartão aberto.", false, "clientes-aviso");
    try { await carregarClientes(); await selecionar(salvo.participante); notificar("Cliente cadastrado e primeiro cartão aberto.", false, "clientes-aviso"); }
    catch (e) { notificar("Cadastro confirmado. Atualize a consulta para ver o cliente. " + e.message, true, "lista-aviso"); }
  }, "cadastro-aviso"); };
  $("form-rapido").onsubmit = e => { e.preventDefault(); executar(async () => {
    if (!operar()) throw new Error("Seu acesso não permite registrar carimbos.");
    const whatsapp = $("whatsapp-rapido").value.replace(/\D/g, "");
    if (!/^(?:55)?[1-9][0-9]9[0-9]{8}$/.test(whatsapp)) throw new Error("Informe um celular com DDD válido.");
    const nomeBox = $("nome-rapido-box");
    const nome = $("nome-rapido").value.trim();
    if (nomeBox.hidden) $("nome-rapido").required = false;
    else if (!nome) { $("nome-rapido").focus(); throw new Error("Informe o nome do cliente."); }
    const selecionadas = (state.campanhasRapidas || []).length <= 1
      ? (state.campanhasRapidas || []).map(c => c.id)
      : [...$("campanhas-rapido").querySelectorAll("input:checked")].map(c => c.value);
    if (!selecionadas.length) throw new Error((state.campanhasRapidas || []).length ? "Selecione pelo menos uma campanha." : "Nenhuma campanha ativa.");
    const anterior = lerPendente("rapido", whatsapp);
    const op = anterior || pendente("rapido", { whatsapp, nome, campanhas: selecionadas }, whatsapp);
    try {
      const salvo = await api(raiz() + "/atendimento-rapido", json({ whatsapp, nome: nome || null, campanhas: selecionadas }, "POST", op.chave));
      concluirPendente("rapido", whatsapp);
      $("nome-rapido").required = false; $("nome-rapido-box").hidden = true; $("nome-rapido").value = ""; $("whatsapp-rapido").value = "";
      notificar((salvo.cadastrado ? "Cliente cadastrado. " : "") + "Carimbo registrado.", false, "rapido-aviso");
    } catch (err) {
      if ((err.message || "").includes("Informe o nome") || (err.message || "").includes("Primeira visita")) {
        $("nome-rapido").required = false; $("nome-rapido-box").hidden = false; $("nome-rapido").focus();
        throw new Error("Primeira visita. Informe o nome para abrir o cartão e carimbar.");
      }
      throw err;
    }
  }, "rapido-aviso"); };
  $("form-carimbo").onsubmit = e => { e.preventDefault(); executar(async () => {
    const selecionadas = [...$("campanhas-atendimento").querySelectorAll("input:checked")].map(c => c.value);
    const existente = lerPendente("atendimento", state.pessoa.id);
    if (!existente && !selecionadas.length) throw new Error("Selecione pelo menos uma campanha.");
    const op = pendente("atendimento", { campanhas: selecionadas }, state.pessoa.id);
    await restaurarCarimbo();
    const salvos = await api(pessoaRaiz() + "/atendimentos", json(op.dados, "POST", op.chave));
    concluirPendente("atendimento", state.pessoa.id);
    await restaurarCarimbo();
    notificar(salvos.length + " carimbo(s) registrado(s) com sucesso!", false, "clientes-aviso");
    try { await carregarCliente(); } catch (e) { notificar("Carimbos confirmados. Atualize a consulta: " + e.message, true, "clientes-aviso"); }
  }, "clientes-aviso"); };
  $("atualizar-cliente").onclick = () => executar(async () => { await atualizarIdentidadeSelecionada(); await carregarCliente(); }, "clientes-aviso");
  $("gerar-convite").onclick = () => executar(async () => { linkConvite("cliente", await api(pessoaRaiz() + "/convite", { method: "POST" })); notificar("Convite gerado. Compartilhe somente com o titular do cartão.", false, "clientes-aviso"); }, "clientes-aviso");
  $("copiar-cliente").onclick = () => executar(() => copiar("cliente")); $("copiar-equipe").onclick = () => executar(() => copiar("equipe"));
  $("atualizar-recompensas").onclick = () => executar(carregarRecompensas);
  $("filtro-recompensas").onchange = () => executar(async () => { state.paginas.recompensas = 0; await carregarRecompensas(); });
  for (const id of ["programa-titulo", "programa-meta", "programa-recompensa", "programa-criterios"]) $(id).oninput = () => { state.dirty = true; previewPrograma(); };
  $("recarregar-programa").onclick = () => executar(async () => { if (state.dirty && !confirm("Descartar o rascunho e consultar as regras vigentes?")) return; await carregarPrograma(); $("programa-aviso").hidden = true; });
  $("form-programa").onsubmit = e => { e.preventDefault(); executar(async () => {
    const dados = { titulo: $("programa-titulo").value.trim(), metaCarimbos: Number($("programa-meta").value), recompensa: $("programa-recompensa").value.trim(), criteriosCompra: $("programa-criterios").value.trim() };
    if (!dados.titulo || !dados.recompensa || !dados.criteriosCompra || !Number.isInteger(dados.metaCarimbos) || dados.metaCarimbos < 1 || dados.metaCarimbos > 1000) throw new Error("Preencha os campos e informe uma meta inteira entre 1 e 1000.");
    if (state.programa) dados.versaoAtual = state.programa.versao;
    try { const salvo = await api(raiz() + "/programa", json(dados, state.programa ? "PUT" : "POST")); preencherPrograma(salvo); $("programa-historico").replaceChildren(); notificar("Regras salvas. Versão vigente: " + salvo.versao + ".", false, "programa-aviso"); }
    catch (e) { if (e.status === 409) throw new Error("O programa mudou desde a consulta. Seu rascunho foi mantido. Anote o que deseja preservar e use Consultar versão atual antes de salvar novamente."); throw e; }
  }, "programa-aviso"); };
  $("ver-historico").onclick = () => executar(historicoPrograma, "programa-aviso");
  $("atualizar-equipe").onclick = () => executar(carregarEquipe, "equipe-aviso");
  $("equipe-email").addEventListener("input", () => $("equipe-email").setCustomValidity(""));
  $("form-equipe").onsubmit = e => { e.preventDefault(); executar(async () => { const campo = $("equipe-email"); campo.setCustomValidity("");
    try { linkConvite("equipe", await api(raiz() + "/equipe/convites", json({ email: campo.value.trim() }))); }
    catch (erro) { if (erro.status === 409) { campo.setCustomValidity(erro.message.includes("desativada") ? "Esta conta está desativada." : "Este funcionário já está cadastrado na equipe."); campo.focus(); campo.reportValidity(); return; } throw erro; } notificar("Convite da equipe gerado. A pessoa deve entrar com esse mesmo telefone ou e-mail e aceitar o vínculo.", false, "equipe-aviso"); await carregarEquipe(); }, "equipe-aviso"); };
  const cargas = { clientes: carregarClientes, recompensas: carregarRecompensas, cartoes: () => carregarDetalhe("cartoes"), historico: () => carregarDetalhe("historico"), beneficios: () => carregarDetalhe("beneficios") };
  Object.keys(cargas).forEach(tipo => ["anterior", "proxima"].forEach(direcao => $(tipo + "-" + direcao).onclick = () => executar(async () => {
    const anterior = state.paginas[tipo]; state.paginas[tipo] += direcao === "anterior" ? -1 : 1;
    try { await cargas[tipo](); } catch (e) { state.paginas[tipo] = anterior; throw e; }
  }, state.view + "-aviso")));
  window.addEventListener("beforeunload", e => { if (state.dirty) { e.preventDefault(); e.returnValue = ""; } });
  $("data-hoje").textContent = new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  executar(async () => { try { await iniciar(); } catch (e) { if (e.status === 401) { sairDaTela(); return; } throw e; } });
})();

// Link para gestão de campanhas do estabelecimento selecionado.

