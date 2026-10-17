/* LUVI Fideliza+ — painel local integrado. Sem dependências externas. */
(() => {
  "use strict";
  const $ = id => document.getElementById(id);
  const todos = seletor => [...document.querySelectorAll(seletor)];
  const state = { conta: null, negocios: [], negocio: null, direitos: {}, view: "inicio", busy: false,
    pessoa: null, programa: undefined, dirty: false, detalhe: "beneficios", busca: "", clientes: [],
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
      const mensagem = dados?.mensagem || (r.status === 401 ? "Entre novamente. Confira o e-mail e a senha." : r.status === 403 ? "Seu acesso não permite esta ação. Atualize o painel para conferir as permissões." : "Não foi possível concluir a operação. Código " + r.status + ".");
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
    const operacao = { chave: crypto.randomUUID(), dados };
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
    for (const id of ["novo-cliente", "nome-cliente", "cadastrar-cliente", "referencia", "carimbar"]) $(id).disabled = state.busy || !operar();
    $("form-carimbo").hidden = !operar();
    $("gerar-convite").disabled = state.busy || !operar() || !state.pessoa || state.pessoa.vinculadoAoCliente;
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
    todos("[data-view]").forEach(b => { b.classList.toggle("active", b.dataset.view === view); if (b.dataset.view === view) b.setAttribute("aria-current", "page"); else b.removeAttribute("aria-current"); });
    if (!state.negocio) return;
    if (view === "inicio") await carregarInicio();
    if (view === "clientes") await carregarClientes();
    if (view === "recompensas") await carregarRecompensas();
    if (view === "programa" && state.programa === undefined) await carregarPrograma();
    if (view === "equipe") await carregarEquipe();
  }
  async function iniciar() {
    state.conta = await api("/api/v1/auth/me");
    state.negocios = await api("/api/v1/painel/estabelecimentos");
    $("conta").textContent = state.conta.email;
    $("estabelecimento").replaceChildren(...state.negocios.map(n => new Option(n.nome, n.id)));
    $("login").hidden = true; $("app").hidden = false;
    await trocarNegocio();
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
    await navegar("inicio");
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
    if (!dados.conteudo.length) vazio("lista-clientes", state.busca ? "Nenhum cliente encontrado com esse nome." : "Seu primeiro vínculo começa aqui. Cadastre um cliente para abrir o cartão.");
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
  }
  function restaurarCarimbo() {
    const op = lerPendente("carimbo", state.pessoa.id);
    $("referencia").value = op?.dados.referenciaCompra || ""; $("referencia").readOnly = !!op;
    $("carimbar").textContent = op ? "Reenviar carimbo" : "+ Carimbo";
    $("carimbo-ajuda").textContent = op ? "Há uma compra sem confirmação. Reenvie os mesmos dados para evitar duplicidade." : "Cada compra válida adiciona um carimbo ao cartão.";
  }
  async function selecionar(p) {
    state.pessoa = p; ["cartoes", "historico", "beneficios"].forEach(t => state.paginas[t] = 0);
    $("convite-cliente").hidden = true; $("link-cliente").value = ""; $("clientes-aviso").hidden = true;
    $("cliente-vazio").hidden = true; $("cliente-detalhe").hidden = false; identidadeCliente(); restaurarCarimbo();
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
      if (!confirm("O benefício “" + r.descricao + "” foi entregue ao cliente? Confirmar registra o resgate desta recompensa.")) return;
      const op = pendente("resgate", {}, r.id);
      await api(raiz() + "/recompensas/" + r.id + "/resgate", { method: "POST", headers: { "Idempotency-Key": op.chave } });
      concluirPendente("resgate", r.id); notificar("Entrega confirmada. Recompensa resgatada.", false, contexto);
      try { if (state.view === "clientes") await carregarCliente(); else await carregarRecompensas(); }
      catch (e) { notificar("Resgate confirmado. Atualize a consulta para ver o resultado. " + e.message, true, contexto); }
    }, contexto); return b;
  }
  function recompensaCliente(r) {
    const b = texto("div", "", "reward-item"), info = document.createElement("div");
    info.append(statusBadge(r.status === "DISPONIVEL" ? "Disponível" : "Resgatada", r.status === "DISPONIVEL"), texto("h3", r.descricao), texto("p", "Ciclo " + r.ciclo + " · " + (r.resgatadaEm ? "Entregue em " + formatar(r.resgatadaEm) : "Conquistada em " + formatar(r.emitidaEm)))); b.append(info);
    if (r.status === "DISPONIVEL" && operar()) b.append(botaoResgate(r, "clientes-aviso")); return b;
  }
  async function carregarRecompensas() {
    let dados = await api(painel() + "/recompensas?" + parametros("recompensas") + "&status=" + $("filtro-recompensas").value);
    if (!dados.conteudo.length && state.paginas.recompensas > 0) { state.paginas.recompensas--; return carregarRecompensas(); }
    $("lista-recompensas").replaceChildren(); paginar("recompensas", dados);
    if (!dados.conteudo.length) vazio("lista-recompensas", "Nenhuma recompensa neste filtro. Cada cartão completo abre uma nova conquista.");
    dados.conteudo.forEach(r => {
      const card = texto("article", "", "reward-tile"), top = texto("div", "", "reward-head"); top.append(statusBadge(r.status === "DISPONIVEL" ? "Disponível" : "Resgatada", r.status === "DISPONIVEL"), texto("small", "Ciclo " + r.ciclo, "muted"));
      card.append(top, texto("h3", r.descricao), texto("p", r.participante), texto("p", r.resgatadaEm ? "Entregue em " + formatar(r.resgatadaEm) : "Conquistada em " + formatar(r.emitidaEm)));
      const acoes = texto("div", "", "reward-actions"), ver = texto("button", "Ver cliente →", "text-button");
      ver.onclick = () => executar(async () => { await navegar("clientes"); await selecionar({ id: r.participacaoId, nomeExibicao: r.participante, vinculadoAoCliente: false }); await atualizarIdentidadeSelecionada(); }, "clientes-aviso"); acoes.append(ver);
      if (r.status === "DISPONIVEL" && operar()) acoes.append(botaoResgate(r, "recompensas-aviso")); card.append(acoes); $("lista-recompensas").append(card);
    });
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
    limparAvisos(); const op = lerPendente("cadastro"); $("nome-cliente").value = op?.dados.nomeExibicao || ""; $("nome-cliente").readOnly = !!op; $("cadastro-pendente").hidden = !op;
    $("cadastrar-cliente").textContent = op ? "Reenviar cadastro pendente" : "Cadastrar e abrir cartão"; $("modal-cliente").showModal(); $("nome-cliente").focus();
  };
  $("fechar-modal").onclick = () => $("modal-cliente").close();
  $("modal-cliente").addEventListener("cancel", e => { if (state.busy) e.preventDefault(); });
  $("form-cliente").onsubmit = e => { e.preventDefault(); executar(async () => {
    const nome = $("nome-cliente").value.trim(); if (!nome) throw new Error("Informe o nome do cliente.");
    const op = pendente("cadastro", { nomeExibicao: nome }); $("nome-cliente").value = op.dados.nomeExibicao; $("nome-cliente").readOnly = true; $("cadastro-pendente").hidden = false;
    const salvo = await api(raiz() + "/participantes", json(op.dados, "POST", op.chave)); concluirPendente("cadastro");
    $("modal-cliente").close(); state.busca = ""; $("busca").value = ""; state.paginas.clientes = 0;
    notificar("Cliente cadastrado e primeiro cartão aberto.", false, "clientes-aviso");
    try { await carregarClientes(); await selecionar(salvo.participante); notificar("Cliente cadastrado e primeiro cartão aberto.", false, "clientes-aviso"); }
    catch (e) { notificar("Cadastro confirmado. Atualize a consulta para ver o cliente. " + e.message, true, "lista-aviso"); }
  }, "cadastro-aviso"); };
  $("form-carimbo").onsubmit = e => { e.preventDefault(); executar(async () => {
    const referencia = $("referencia").value.trim(); if (!referencia) throw new Error("Informe uma referência da compra.");
    const op = pendente("carimbo", { referenciaCompra: referencia }, state.pessoa.id); restaurarCarimbo();
    const salvo = await api(pessoaRaiz() + "/carimbos", json(op.dados, "POST", op.chave)); concluirPendente("carimbo", state.pessoa.id); restaurarCarimbo();
    notificar(salvo.concluiuCartao ? "Cartão completo! Uma recompensa foi emitida para o cliente." : "Compra confirmada. Saldo após a operação: " + salvo.saldoApos + " carimbos.", false, "clientes-aviso");
    try { await carregarCliente(); } catch (e) { notificar("Carimbo confirmado. Atualize os dados do cliente. " + e.message, true, "clientes-aviso"); }
  }, "clientes-aviso"); };
  $("atualizar-cliente").onclick = () => executar(async () => { await atualizarIdentidadeSelecionada(); await carregarCliente(); }, "clientes-aviso");
  $("gerar-convite").onclick = () => executar(async () => { if (!confirm("Gerar convite para " + state.pessoa.nomeExibicao + "? O convite anterior desse cliente será substituído.")) return; linkConvite("cliente", await api(pessoaRaiz() + "/convite", { method: "POST" })); notificar("Convite gerado. Compartilhe somente com o titular do cartão.", false, "clientes-aviso"); }, "clientes-aviso");
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
  $("form-equipe").onsubmit = e => { e.preventDefault(); executar(async () => { if (!confirm("Gerar um convite de funcionário para " + $("equipe-email").value.trim() + "? Um novo convite para esse e-mail substitui o anterior.")) return; linkConvite("equipe", await api(raiz() + "/equipe/convites", json({ email: $("equipe-email").value.trim() }))); notificar("Convite da equipe gerado. A pessoa deve entrar com esse mesmo e-mail e aceitar o vínculo.", false, "equipe-aviso"); await carregarEquipe(); }, "equipe-aviso"); };
  const cargas = { clientes: carregarClientes, recompensas: carregarRecompensas, cartoes: () => carregarDetalhe("cartoes"), historico: () => carregarDetalhe("historico"), beneficios: () => carregarDetalhe("beneficios") };
  Object.keys(cargas).forEach(tipo => ["anterior", "proxima"].forEach(direcao => $(tipo + "-" + direcao).onclick = () => executar(async () => {
    const anterior = state.paginas[tipo]; state.paginas[tipo] += direcao === "anterior" ? -1 : 1;
    try { await cargas[tipo](); } catch (e) { state.paginas[tipo] = anterior; throw e; }
  }, state.view + "-aviso")));
  window.addEventListener("beforeunload", e => { if (state.dirty) { e.preventDefault(); e.returnValue = ""; } });
  $("data-hoje").textContent = new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  executar(async () => { try { await iniciar(); } catch (e) { if (e.status === 401) { sairDaTela(); return; } throw e; } });
})();
