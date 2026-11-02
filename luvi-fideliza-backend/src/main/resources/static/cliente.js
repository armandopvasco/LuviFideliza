(() => { 'use strict'; const $=id=>document.getElementById(id); const mensagem=t=>$('mensagem').textContent=t; async function csrf(){const r=await fetch('/api/v1/auth/csrf',{credentials:'same-origin'});if(!r.ok)throw Error('Não foi possível preparar a autenticação.');return r.json()} async function carregar(){mensagem('');const r=await fetch('/api/v1/me/cartoes?tamanho=50',{credentials:'same-origin',cache:'no-store'});if(r.status===401){$('carteira').hidden=true;$('acesso').hidden=false;return}if(!r.ok)throw Error('Não foi possível consultar os cartões ('+r.status+').');const d=await r.json();$('acesso').hidden=true;$('carteira').hidden=false;const area=$('cartoes');area.replaceChildren();if(!d.conteudo?.length){const p=document.createElement('p');p.textContent='Você ainda não possui cartões vinculados. Peça ao estabelecimento um convite para participar.';area.append(p)}let premios=[];try{const rr=await fetch("/api/v1/me/recompensas?tamanho=100",{credentials:"same-origin",cache:"no-store"});if(rr.ok){const dados=await rr.json();premios=dados.conteudo||[]}}catch(e){console.warn("Recompensas indisponíveis",e)}const cores=new Map();for(const c of d.conteudo||[]){
  let visual=cores.get(c.estabelecimentoId);
  if(!visual){try{const r=await fetch('/api/v1/estabelecimentos/'+encodeURIComponent(c.estabelecimentoId)+'/visual-cartao',{credentials:'same-origin'});if(r.ok)visual=await r.json()}catch{}visual=visual||{corCartao:'#30475A',corCarimbo:'#87A18F'};cores.set(c.estabelecimentoId,visual)}
  const box=document.createElement('article');box.className='cartao cartao-fidelidade';
  window.LuviCartao.aplicar(box,visual,c.estabelecimentoId);
  const loja=document.createElement('p');loja.className='estabelecimento';loja.textContent=c.estabelecimento;
  const h=document.createElement('h2');h.textContent=c.titulo;
  const premio=document.createElement('p');premio.className='premio';premio.textContent='Recompensa: '+c.recompensa;
  const grade=document.createElement('div');grade.className='carimbos';grade.setAttribute('aria-label',c.saldo+' de '+c.metaCarimbos+' carimbos');
  const total=Math.min(100,Math.max(1,Number(c.metaCarimbos)||1));const saldo=Math.max(0,Number(c.saldo)||0);
  for(let i=0;i<total;i++){const bolinha=document.createElement('span');bolinha.className='carimbo'+(i<saldo?' marcado':'');if(i<saldo)bolinha.textContent='✓';else window.LuviCartao.desenharIcone(bolinha,visual);bolinha.setAttribute('aria-hidden','true');grade.append(bolinha)}
  const status=document.createElement('p');status.className='cartao-status';status.textContent=c.saldo+' de '+c.metaCarimbos+' carimbos · '+c.status;
  box.append(loja,h,premio,grade,status);
  const qrCliente=document.createElement('img');qrCliente.className='qr-cliente';qrCliente.alt='QR Code para mostrar no balcão';qrCliente.src='/api/v1/me/participacoes/'+encodeURIComponent(c.participacaoId)+'/qrcode';box.append(qrCliente);// Renderizar o cartão primeiro; um erro no aviso não pode ocultar os carimbos.
  area.append(box);
  try {
    const recompensaDoCartao = premios.find(r => String(r.cartaoId) === String(c.id));
    const resgatada = recompensaDoCartao && (String(recompensaDoCartao.status).toUpperCase() === 'RESGATADA' || !!recompensaDoCartao.resgatadaEm);
    const concluida = String(c.status || '').toUpperCase() === 'CONCLUIDO' || (Number(c.metaCarimbos) > 0 && Number(c.saldo) >= Number(c.metaCarimbos));
    const disponivel = !resgatada && (concluida || (recompensaDoCartao && String(recompensaDoCartao.status).toUpperCase() === 'DISPONIVEL'));
    if (resgatada || disponivel) {
      const selo = document.createElement('div');
      selo.className = 'luvi-premio-selo-v0893';
      selo.textContent = resgatada ? '✓ Recompensa resgatada' : '🎁 Prêmio disponível';
      box.insertBefore(selo,box.firstChild);
      const faixa = document.createElement('div');
      faixa.className = 'luvi-premio-faixa-v0893' + (resgatada ? ' resgatada' : '');
      const presente = document.createElement('span'); presente.className = 'luvi-premio-icone-v0893'; presente.textContent = resgatada ? '✓' : '🎁';
      const conteudo = document.createElement('div');
      const titulo = document.createElement('strong'); titulo.textContent = resgatada ? 'Recompensa resgatada' : 'Parabéns! Você completou seu cartão!';
      const detalhe = document.createElement('p'); detalhe.textContent = resgatada ? 'Este benefício já foi entregue pelo estabelecimento.' : 'Seu prêmio está disponível: ' + c.recompensa + '. Apresente este cartão ao estabelecimento para receber sua recompensa.';
      conteudo.append(titulo,detalhe); faixa.append(presente,conteudo); box.append(faixa);
      if (disponivel && recompensaDoCartao) {
        const qrPremio=document.createElement('img');qrPremio.className='qr-cliente';qrPremio.alt='QR Code da recompensa para o balcão confirmar a entrega';qrPremio.src='/api/v1/me/recompensas/'+encodeURIComponent(recompensaDoCartao.id)+'/qrcode';box.append(qrPremio);
      }
    }
  } catch (erroAviso) { console.error('Falha ao exibir recompensa do cartão', c.id, erroAviso); }
}
const areaCampanhas=$('campanhas');areaCampanhas.replaceChildren();const lojas=[...new Set((d.conteudo||[]).map(c=>c.estabelecimentoId))];for(const lojaId of lojas){try{const resp=await fetch('/api/v1/estabelecimentos/'+encodeURIComponent(lojaId)+'/campanhas-cliente',{credentials:'same-origin',cache:'no-store'});if(!resp.ok)continue;for(const campanha of await resp.json()){if(campanha.participando)continue;const card=document.createElement('article');card.className='cartao';const h=document.createElement('h2');h.textContent=campanha.titulo;const p=document.createElement('p');p.textContent=campanha.metaCarimbos+' carimbos · '+campanha.recompensa;const info=document.createElement('p');info.textContent='Disponível neste estabelecimento. Peça à equipe para participar.';card.append(h,p,info);areaCampanhas.append(card)}}catch{}}if(!areaCampanhas.childElementCount){const p=document.createElement('p');p.textContent='Nenhuma campanha adicional disponível para sua conta.';areaCampanhas.append(p)}if(d.total>d.conteudo.length)mensagem('Exibindo os primeiros '+d.conteudo.length+' cartões.');} $('login').addEventListener('submit',async e=>{e.preventDefault();try{const f=new FormData(e.target);const c=await csrf();const dados=new URLSearchParams({email:f.get('email'),password:f.get('senha')});const r=await fetch('/api/v1/auth/login',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded',[c.headerName]:c.token},body:dados});if(!r.ok)throw Error('Telefone/e-mail ou senha inválidos.');await carregar()}catch(e){mensagem(e.message)}});$('atualizar').onclick=()=>carregar().catch(e=>mensagem(e.message));$('sair').onclick=async()=>{try{const c=await csrf();await fetch('/api/v1/auth/logout',{method:'POST',credentials:'same-origin',headers:{[c.headerName]:c.token}});$('carteira').hidden=true;$('acesso').hidden=false;mensagem('Sessão encerrada.')}catch(e){mensagem(e.message)}};carregar().catch(e=>mensagem(e.message));if('serviceWorker' in navigator && window.isSecureContext)navigator.serviceWorker.register('/cliente-sw.js').catch(()=>{});})();