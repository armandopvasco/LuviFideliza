(()=>{'use strict';
const itens=[{link:'link-convite',contato:'proprietario-email',papel:'proprietário'},{link:'link-equipe',contato:'equipe-email',papel:'funcionário'},{link:'link-cliente',contato:'cliente-whatsapp',papel:'cliente'}];
for(const item of itens){const input=document.getElementById(item.link),campo=document.getElementById(item.contato);if(!input||!campo)continue;
const btn=document.createElement('button');btn.type='button';btn.className='secondary';btn.textContent='Enviar convite';
btn.onclick=()=>{if(!input.value)return alert('Gere o convite primeiro.');const contato=campo.value.trim();const msg='Olá! Você recebeu um convite para acessar o LUVI Fideliza+ como '+item.papel+'. Abra este link para aceitar: '+input.value;
if(contato.includes('@')){location.href='mailto:'+encodeURIComponent(contato)+'?subject='+encodeURIComponent('Convite LUVI Fideliza+')+'&body='+encodeURIComponent(msg);return;}
let tel=contato.replace(/\D/g,'');if(tel.length===11)tel='55'+tel;if(!/^55[1-9][0-9]9[0-9]{8}$/.test(tel))return alert('Informe um celular com DDD válido.');
window.open('https://wa.me/'+tel+'?text='+encodeURIComponent(msg),'_blank','noopener');};
input.insertAdjacentElement('afterend',btn);
}
})();
