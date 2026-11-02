(()=>{'use strict';
const loja=document.getElementById('estabelecimento'),link=document.getElementById('link-adesao'),qr=document.getElementById('qr-adesao');
if(!loja||!link)return;
function atualizar(){
  if(!loja.value){link.value='';if(qr)qr.removeAttribute('src');return}
  link.value=new URL('/adesao.html?loja='+encodeURIComponent(loja.value),location.origin).href;
  if(qr)qr.src='/api/v1/publico/estabelecimentos/'+encodeURIComponent(loja.value)+'/qrcode';
}
loja.addEventListener('change',atualizar);
new MutationObserver(atualizar).observe(loja,{childList:true});
atualizar();
const aviso=document.getElementById('adesao-aviso');
document.getElementById('copiar-adesao').onclick=async()=>{
  if(!link.value)return alert('Selecione um estabelecimento.');
  try{await navigator.clipboard.writeText(link.value);if(aviso)aviso.textContent='Link copiado.';else alert('Link copiado!')}
  catch{alert('Selecione o link e copie com Ctrl+C.')}
};
document.getElementById('imprimir-adesao').onclick=()=>{
  if(!loja.value)return alert('Selecione um estabelecimento.');
  const imagem='/api/v1/publico/estabelecimentos/'+encodeURIComponent(loja.value)+'/qrcode';
  const w=window.open('','_blank');
  if(!w)return alert('Autorize a abertura de janelas para imprimir.');
  w.document.write('<!doctype html><html lang="pt-BR"><head><title>LUVI Fideliza+ - QR Code</title><link rel="stylesheet" href="/luvi-tokens.css"><style>body{margin:0;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;text-align:center;gap:16px;padding:32px 20px;background:#f6f5f3;font-family:Segoe UI,sans-serif;color:#1d3e5b}img{width:min(78vw,420px);height:auto;aspect-ratio:1;border-radius:28px}button{border:0;background:#1d3e5b;color:#fff;border-radius:11px;padding:12px 18px;font-weight:650;cursor:pointer}</style></head><body><h1>Participe do LUVI Fideliza+</h1><p>Escaneie para acompanhar seus benefícios.</p><img alt="QR Code" src="'+imagem+'"><p>'+link.value.replaceAll('&','&amp;').replaceAll('<','&lt;')+'</p><button onclick="window.print()">Imprimir</button></body></html>');
  w.document.close();
};
})();
