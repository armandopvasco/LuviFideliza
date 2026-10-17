/* Complemento do painel local LUVI. Carregar depois do script original. */
(() => {
  "use strict";
  const painel = document.getElementById("painel");
  if (!painel) return;
  const bloco = document.createElement("section");
  bloco.id = "luvi-funcoes";
  bloco.innerHTML = `
    <h2>Programa de fidelidade</h2>
    <p id="lf-estado">Selecione um estabelecimento e consulte o programa.</p>
    <button type="button" id="lf-consultar">Consultar programa</button>
    <button type="button" id="lf-historico">Histórico de regras</button>
    <form id="lf-form" hidden>
      <label>Título <input id="lf-titulo" required maxlength="120"></label>
      <label>Meta de carimbos <input id="lf-meta" type="number" min="1" max="1000" step="1" required></label>
      <label>Recompensa <input id="lf-recompensa" required maxlength="200"></label>
      <label>Critérios da compra <textarea id="lf-criterios" required maxlength="500" rows="3"></textarea></label>
      <button id="lf-salvar">Salvar regras</button>
      <p>Cartões já abertos mantêm as regras da sua versão. Novos cartões usam as regras vigentes.</p>
    </form>
    <div id="lf-versoes"></div>
    <hr><h2>Convite do participante</h2>
    <p id="lf-participante">Selecione um participante no painel.</p>
    <button type="button" id="lf-gerar">Gerar link de convite</button>
    <div id="lf-convite" hidden>
      <label>Link para o cliente <input id="lf-link" readonly></label>
      <p id="lf-validade"></p>
      <button type="button" id="lf-copiar">Copiar link</button>
      <p>Abra em uma janela anônima para testar com a conta do cliente. Compartilhe somente com o titular.</p>
    </div>`;
  const estilo = document.createElement("style");
  estilo.textContent = "#luvi-funcoes{margin:20px 0;padding:24px;background:#fff;border:1px solid #ddd;border-radius:16px}#luvi-funcoes label{display:block;margin:12px 0}#luvi-funcoes input,#luvi-funcoes textarea{display:block;box-sizing:border-box;width:100%;padding:10px;font:inherit}#luvi-funcoes button{margin:5px 8px 5px 0}#lf-versoes article{border-top:1px solid #ddd;padding:12px 0;white-space:pre-wrap}#lf-link{font-size:14px}";
  document.head.append(estilo);
  painel.append(bloco);
  const el = id => document.getElementById("lf-" + id);
  let contexto = "", selecionado = "", regras = undefined, direitos = null, alterado = false;
  const rota = () => "/api/v1/estabelecimentos/" + encodeURIComponent(negocio().id);
  const campos = ["titulo", "meta", "recompensa", "criterios"];
  const limparConvite = () => { el("convite").hidden = true; el("link").value = ""; };
  function sincronizar() {
    const atual = conta && negocio() ? conta.email + ":" + negocio().id : "";
    if (atual !== contexto) {
      contexto = atual; selecionado = ""; regras = undefined; direitos = null; alterado = false;
      el("form").reset(); el("form").hidden = true; el("versoes").replaceChildren();
      el("estado").textContent = "Consulte o programa para visualizar ou editar as regras.";
      limparConvite();
    }
    const pessoa = contexto && participante ? participante.id : "";
    if (pessoa !== selecionado) { selecionado = pessoa; limparConvite(); }
    el("participante").textContent = pessoa ? participante.nomeExibicao + (participante.vinculadoAoCliente ? " — já vinculado a uma conta." : " — ainda sem conta vinculada.") : "Selecione um participante no painel.";
    const editar = !!contexto && !!direitos?.podeEditarPrograma && regras !== undefined;
    campos.forEach(c => el(c).disabled = ocupado || !editar);
    el("salvar").disabled = ocupado || !editar;
    el("consultar").disabled = ocupado || !contexto;
    el("historico").disabled = ocupado || !contexto;
    el("gerar").disabled = ocupado || !pessoa || !negocio()?.podeOperar || participante.vinculadoAoCliente;
    el("copiar").disabled = ocupado || !el("link").value;
    el("link").readOnly = true;
  }
  window.LuviFuncoes = { sincronizar };
  campos.forEach(c => el(c).addEventListener("input", () => { alterado = true; }));
  function preencher(dados) {
    regras = dados;
    el("form").hidden = false;
    el("titulo").value = dados?.titulo || "";
    el("meta").value = dados?.metaCarimbos || "";
    el("recompensa").value = dados?.recompensa || "";
    el("criterios").value = dados?.criteriosCompra || "";
    el("estado").textContent = dados ? "Versão vigente: " + dados.versao : "Nenhum programa cadastrado.";
    if (!direitos.podeEditarPrograma) el("estado").textContent += " A edição é exclusiva do proprietário.";
    alterado = false;
  }
  el("consultar").onclick = () => executar(async () => {
    if (alterado && !confirm("Descartar as alterações ainda não salvas e consultar as regras vigentes?")) return;
    direitos = await api("/api/v1/painel/estabelecimentos/" + encodeURIComponent(negocio().id) + "/permissoes");
    let dados;
    try { dados = await api(rota() + "/programa"); }
    catch (erro) { if (erro.status !== 404) throw erro; dados = null; }
    preencher(dados);
  });
  el("form").onsubmit = evento => {
    evento.preventDefault();
    if (!direitos?.podeEditarPrograma || regras === undefined) return;
    executar(async () => {
      const dados = { titulo: el("titulo").value.trim(), metaCarimbos: Number(el("meta").value), recompensa: el("recompensa").value.trim(), criteriosCompra: el("criterios").value.trim() };
      if (!dados.titulo || !dados.recompensa || !dados.criteriosCompra || !Number.isInteger(dados.metaCarimbos) || dados.metaCarimbos < 1 || dados.metaCarimbos > 1000) throw new Error("Preencha todos os campos e informe uma meta inteira entre 1 e 1000.");
      if (regras) dados.versaoAtual = regras.versao;
      try {
        const salvo = await api(rota() + "/programa", { method: regras ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(dados) });
        preencher(salvo); el("versoes").replaceChildren(); aviso("Regras salvas. Versão vigente: " + salvo.versao + ".");
      } catch (erro) {
        if (erro.status === 409) throw new Error("O programa mudou desde a consulta. Seu rascunho foi mantido. Anote as alterações e use Consultar programa para conferir a versão atual antes de salvar novamente.");
        throw erro;
      }
    });
  };
  el("historico").onclick = () => executar(async () => {
    const lista = await api(rota() + "/programa/historico");
    el("versoes").replaceChildren();
    lista.forEach(v => {
      const item = document.createElement("article");
      item.textContent = "Versão " + v.versao + " · " + v.titulo + "\nMeta: " + v.metaCarimbos + " carimbos\nRecompensa: " + v.recompensa + "\nCritérios: " + v.criteriosCompra;
      el("versoes").append(item);
    });
    if (!lista.length) el("versoes").textContent = "Nenhuma versão cadastrada.";
  });
  el("gerar").onclick = () => executar(async () => {
    if (!participante || !negocio()?.podeOperar || participante.vinculadoAoCliente) return;
    if (!confirm("Gerar um convite para " + participante.nomeExibicao + "? Um novo convite invalida o anterior desse participante.")) return;
    const dados = await api(rota() + "/participantes/" + encodeURIComponent(participante.id) + "/convite", { method: "POST" });
    const link = new URL("convite.html", location.href);
    link.hash = new URLSearchParams({ token: dados.token }).toString();
    el("link").value = link.href; el("convite").hidden = false;
    el("validade").textContent = "Válido até " + new Date(dados.expiraEm).toLocaleString("pt-BR") + ".";
    aviso("Convite gerado. O cliente deverá entrar ou criar sua conta e aceitar o vínculo.");
  });
  el("copiar").onclick = () => executar(async () => {
    try { await navigator.clipboard.writeText(el("link").value); aviso("Link copiado."); }
    catch { el("link").select(); aviso("Selecione e copie o link com Ctrl+C."); }
  });
  sincronizar();
})();
