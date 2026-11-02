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
  w.document.write('<!doctype html><html lang="pt-BR"><head><title>LUVI Fideliza+ - QR Code</title><link rel="stylesheet" href="/luvi-tokens.css"></head><body style="text-align:center;font-family:Segoe UI,sans-serif;color:#1d3e5b"><h1>Participe do LUVI Fideliza+</h1><p>Escaneie para acompanhar seus benefícios.</p><img width="360" height="360" alt="QR Code" src="'+imagem+'"><p>'+link.value.replaceAll('&','&amp;').replaceAll('<','&lt;')+'</p><button onclick="window.print()">Imprimir</button></body></html>');
  w.document.close();
};
})();
