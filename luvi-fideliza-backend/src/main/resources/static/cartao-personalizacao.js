/* Ícones pré-definidos, sem dependências externas. */
window.LuviCartao = Object.freeze({
  icones: Object.freeze({estrela:'★',coracao:'♥',xicara:'☕',tesoura:'✂',presente:'🎁',
    coroa:'♛',flor:'✿',folha:'❧',sol:'☀',lua:'☾',raio:'ϟ',diamante:'◆',trofeu:'🏆',
    fogo:'🔥',pizza:'🍕',hamburguer:'🍔',bolo:'🎂',sorvete:'🍦',pao:'🥐',livro:'📖',
    musica:'♫',bola:'⚽',pata:'🐾',carro:'🚗',bicicleta:'🚲',halter:'🏋',beleza:'💅',
    check:'✓',sorriso:'☺'}),
  desenharIcone(elemento, visual){ const paths={estrela:"polygon points='12 2 15 9 22 9.5 17 14.5 18.5 22 12 18 5.5 22 7 14.5 2 9.5 9 9'",coracao:"path d='M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8L12 21l8.8-8.6a5.5 5.5 0 0 0 0-7.8Z'",xicara:"path d='M4 8h14v6a7 7 0 0 1-14 0V8Zm14 1h2a3 3 0 0 1 0 6h-2M2 22h20M8 2v3m5-3v3'",tesoura:"circle cx='6' cy='6' r='3'/><circle cx='6' cy='18' r='3'/><path d='m8 8 12 12M8 16 20 4'",presente:"rect x='3' y='9' width='18' height='12' rx='1'/><path d='M2 9h20M12 9v12M12 9c-7 0-8-6-4-7 3-1 4 4 4 7Zm0 0c7 0 8-6 4-7-3-1-4 4-4 7Z'",flor:"circle cx='12' cy='12' r='2'/><path d='M12 10C6 2 16 0 14 9c8-5 12 4 3 5 5 8-4 12-5 3-8 5-12-4-3-5C4 4 11 2 12 10Z'",folha:"path d='M20 4C7 4 3 10 4 17c7 4 17-2 16-13ZM4 21c4-7 9-11 15-14'",sol:"circle cx='12' cy='12' r='4'/><path d='M12 2v2m0 16v2M2 12h2m16 0h2M5 5l2 2m10 10 2 2M19 5l-2 2M7 17l-2 2'",lua:"path d='M20 16A9 9 0 0 1 8 4a9 9 0 1 0 12 12Z'",raio:"path d='m13 2-9 12h7l-1 8 10-12h-7l0-8Z'",diamante:"path d='m6 3 12 0 5 7-11 12L1 10l5-7ZM1 10h22M8 3l-3 7 7 12 7-12-3-7'",musica:"path d='M9 18V5l12-2v13M9 18c0 4-7 5-7 1s7-5 7-1Zm12-2c0 4-7 5-7 1s7-5 7-1Z'",pata:"circle cx='6' cy='8' r='1.5'/><circle cx='11' cy='5' r='1.5'/><circle cx='16' cy='6' r='1.5'/><circle cx='20' cy='10' r='1.5'/><path d='M12 11c-3 0-3 4-6 5-3 2-1 6 2 6 2 0 3-2 4-2s3 2 5 2c3 0 4-4 1-6-3-1-3-5-6-5Z'",bola:"circle cx='12' cy='12' r='10'/><path d='m12 6 5 4-2 6H9l-2-6 5-4ZM12 2v4M2 11l5-1m15 1-5-1M5 19l4-3m10 3-4-3'",check:"path d='m4 12 5 5L20 6'",sorriso:"circle cx='12' cy='12' r='10'/><path d='M8 14c2 4 6 4 8 0M8 9h.01M16 9h.01'",coroa:"path d='M2 7 7 12l5-9 5 9 5-5-2 13H4L2 7Z'",fogo:"path d='M12 22c-6 0-9-4-8-9 1-3 4-5 5-9 3 2 4 5 3 8 3-1 4-4 4-7 7 8 8 17-4 17Z'",livro:"path d='M12 5C8 2 5 3 2 4v15c4-2 7-1 10 1 3-2 6-3 10-1V4c-3-1-6-2-10 1Zm0 0v15'",bicicleta:"circle cx='5' cy='17' r='4'/><circle cx='19' cy='17' r='4'/><path d='m5 17 5-10 5 10H5m10 0 4-10h-4M9 7h4'",carro:"rect x='2' y='9' width='20' height='10' rx='2'/><path d='m5 9 2-5h10l2 5M6 19v2m12-2v2M5 14h2m10 0h2'",trofeu:"path d='M7 3h10v9a5 5 0 0 1-10 0V3ZM7 5H3v4c0 3 2 4 5 4m9-8h4v4c0 3-2 4-5 4m-4 4v4m-5 0h10'"}; const nome=visual?.iconeCarimbo||"estrela"; const path=paths[nome]||paths.estrela; elemento.innerHTML=`<svg viewBox="0 0 24 24" width="22" height="22" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><${path.endsWith("/>")?path:path+"/>"}</svg>`; },
  aplicar(cartao,visual,id){
    const cor=x=>/^#[0-9a-fA-F]{6}$/.test(x||'');
    cartao.style.setProperty('--cor-cartao',cor(visual.corCartao)?visual.corCartao:'#30475A');
    cartao.style.setProperty('--cor-carimbo',cor(visual.corCarimbo)?visual.corCarimbo:'#87A18F');
    cartao.dataset.icone=Object.hasOwn(this.icones,visual.iconeCarimbo)?visual.iconeCarimbo:'estrela';
    if(visual.fotoFundoCartao){
      const url='/api/v1/estabelecimentos/'+encodeURIComponent(id)+'/fundo-cartao';
      cartao.style.backgroundImage='linear-gradient(#102638b8,#102638b8),url("'+url+'")';
      cartao.style.backgroundSize='cover';cartao.style.backgroundPosition='center';
    }
  },
  simbolo(visual){return this.icones[visual?.iconeCarimbo]||'★';}
});
