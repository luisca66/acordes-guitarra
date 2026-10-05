import {manual,parseSheet,diagram,describe} from './music.js';
const $=id=>document.getElementById(id);
const status=text=>{$('status').textContent=text;};
let catalog={},mode='songs',song=null,parsed=null,manualPositions=[];
const CATALOG_API=location.hostname==='127.0.0.1'?'/api/catalog':'https://acordes-guitarra-luisca66.vercel.app/api/catalog';
let lookup=0,controller=null;
function cancelLookup(){lookup++;controller?.abort();$('search-button').disabled=false;$('search-results').removeAttribute('aria-busy');}
async function requestCatalog(params,signal){
 const request=new AbortController();let timedOut=false;
 const cancel=()=>request.abort();signal.addEventListener('abort',cancel,{once:true});if(signal.aborted)cancel();
 const timer=setTimeout(()=>{timedOut=true;request.abort();},25000);
 try{const response=await fetch(CATALOG_API+'?'+new URLSearchParams(params),{signal:request.signal,credentials:'omit'});
  let value;try{value=await response.json();}catch{throw Error('No se pudo conectar con el buscador. Intenta de nuevo.');}
  if(!response.ok)throw Error(value.error||'No se pudo cargar el catálogo. Intenta de nuevo.');
  return value;
 }catch(error){if(timedOut)throw Error('El buscador tardó demasiado. Intenta de nuevo.');throw error;}
 finally{clearTimeout(timer);signal.removeEventListener('abort',cancel);}
}
const store={get(key){try{return localStorage.getItem('acordes.'+key);}catch{return null;}},set(key,value){try{localStorage.setItem('acordes.'+key,value);return true;}catch{status('No se pudo guardar en este navegador. Puedes seguir usando la app.');return false;}}};
function switchMode(next){mode=next;$('songs-tab').setAttribute('aria-pressed',next==='songs');$('chords-tab').setAttribute('aria-pressed',next==='chords');$('songs-panel').hidden=next!=='songs';$('chords-panel').hidden=next!=='chords';$('song').hidden=next!=='songs'||!song;$('manual-result').hidden=next!=='chords'||!manualPositions.length;status('');}
function positionFor(name){return parsed?.definitions.get(name)||(catalog[name]?{name,frets:catalog[name],source:false}:null);}
function openPosition(position){if(song?.tuning==='other'&&mode==='songs'){status('Esta hoja usa otra afinación. Las posiciones del catálogo son para afinación estándar.');return;}$('chord-title').textContent=position.name;$('large-diagram').innerHTML=diagram(position.frets);$('chord-description').textContent=describe(position.frets)+(position.source?'. Digitación de esta versión.':'. Una posición en afinación estándar.');$('chord-dialog').showModal();}
function cards(target,positions){target.replaceChildren();positions.forEach(position=>{const card=document.createElement('button');card.className='diagram-card';const title=document.createElement('strong');title.textContent=position.name;card.append(title);const image=document.createElement('div');image.innerHTML=diagram(position.frets);image.setAttribute('aria-hidden','true');card.append(image);const note=document.createElement('small');note.textContent=position.source?'Digitación de esta versión':'Afinación estándar';card.append(note);card.setAttribute('aria-label',position.name+': '+describe(position.frets));card.onclick=()=>openPosition(position);target.append(card);});}
function showSong(value,save=true){
 song=value;parsed=parseSheet(value.text,catalog);$('song-title').textContent=value.title||'Mi canción';
 let url=null;try{const source=new URL(value.source);if(source.protocol==='https:'||source.protocol==='http:')url=source.href;}catch{}
 $('source-link').hidden=!url;if(url)$('source-link').href=url;
 const meta=[];if(value.author)meta.push('Ultimate Guitar · '+value.author);if(value.capo!==null&&value.capo!==undefined&&String(value.capo)!=='0')meta.push('Cejilla: traste '+value.capo);if(value.tuning==='other'&&value.originalTuning)meta.push('Afinación: '+value.originalTuning);$('song-meta').textContent=meta.join(' · ');
 const nonstandard=value.tuning==='other';cards($('song-diagrams'),nonstandard?[]:parsed.positions);
 $('position-note').textContent=nonstandard?'Esta hoja usa otra afinación. Se conserva la letra; no se muestran posiciones estándar.':parsed.missing.length?'Sin diagrama para: '+parsed.missing.join(', ')+'. La letra se conserva.':parsed.definitions.size?'Se conservan las digitaciones de la lista original.':'Toca una posición para ampliarla.';
 $('song-download').hidden=nonstandard||!parsed.positions.length;
 const sheet=$('sheet');sheet.replaceChildren();
 parsed.lines.filter(line=>!line.position).forEach(line=>{const row=document.createElement('div');row.className='line'+(line.heading?' heading':'');line.parts.forEach(part=>{if(part.name){const position=positionFor(part.name);const badge=document.createElement(position&&!nonstandard?'button':'span');badge.className='chord';badge.textContent=part.text;if(position&&!nonstandard){badge.setAttribute('aria-label','Ver posición de '+part.name);badge.onclick=()=>openPosition(position);}else badge.classList.add('unavailable');row.append(badge);}else row.append(document.createTextNode(part.text));});sheet.append(row);});
 if(save)store.set('song',JSON.stringify(value));switchMode('songs');
}
function openPaste(edit=false){$('title-input').value=edit&&song?song.title:'';$('source-input').value=edit&&song?song.source:'';$('sheet-input').value=edit&&song?song.text:'';$('tuning-input').value=edit&&song?song.tuning:'standard';$('paste-dialog').showModal();}
$('songs-tab').onclick=()=>switchMode('songs');$('chords-tab').onclick=()=>{cancelLookup();switchMode('chords');};
async function chooseSong(selected){
 cancelLookup();const token=lookup;controller=new AbortController();const active=controller;
 status('Cargando letra y acordes…');$('search-results').setAttribute('aria-busy','true');
 try{const sheet=await requestCatalog({url:selected.url},active.signal);if(token!==lookup)return;if(typeof sheet.text!=='string'||sheet.text.length>100000)throw Error('No se pudo leer esta versión. Elige otra.');showSong({...sheet,title:selected.title+(selected.artist?' · '+selected.artist:'')});$('search-results').replaceChildren();status('Canción lista. Toca cualquier acorde para ver su posición.');$('song').scrollIntoView({block:'start',behavior:'instant'});}
 catch(error){if(token===lookup&&error.name!=='AbortError')status(error.message==='Failed to fetch'?'No se pudo conectar con el buscador. Revisa tu conexión e intenta de nuevo.':error.message);}
 finally{if(token===lookup)$('search-results').removeAttribute('aria-busy');}
}
$('search-form').onsubmit=async event=>{
 event.preventDefault();const query=$('query').value.trim();if(!query){status('Escribe un título; puedes agregar el artista.');return;}
 cancelLookup();const token=lookup;controller=new AbortController();const active=controller;store.set('query',query);$('search-results').replaceChildren();$('search-results').setAttribute('aria-busy','true');$('search-button').disabled=true;status('Buscando canciones…');
 try{const value=await requestCatalog({q:query},active.signal);if(token!==lookup)return;if(!Array.isArray(value.songs))throw Error('No se pudo leer la lista de canciones. Intenta de nuevo.');
  for(const row of value.songs){const button=document.createElement('button');button.className='song-result';const title=document.createElement('strong');title.textContent=row.title;const detail=document.createElement('span');detail.textContent=row.artist+(row.version?' · Versión '+row.version:'');button.append(title,detail);button.onclick=()=>chooseSong(row);$('search-results').append(button);}
  status(value.songs.length?'Elige canción, artista y versión:':'No encontré canciones con «'+query+'». Prueba una parte del título o agrega el artista.');
 }catch(error){if(token===lookup&&error.name!=='AbortError')status(error.message==='Failed to fetch'?'No se pudo conectar con el buscador. Revisa tu conexión e intenta de nuevo.':error.message);}
 finally{if(token===lookup){$('search-button').disabled=false;$('search-results').removeAttribute('aria-busy');}}
};
$('paste-button').onclick=()=>{cancelLookup();openPaste();};$('edit-song').onclick=()=>{cancelLookup();openPaste(true);};$('cancel-paste').onclick=()=>$('paste-dialog').close();$('close-chord').onclick=()=>$('chord-dialog').close();
$('paste-form').onsubmit=event=>{event.preventDefault();const text=$('sheet-input').value;if(!text.trim())return;showSong({title:$('title-input').value.trim()||'Mi canción',source:$('source-input').value.trim(),tuning:$('tuning-input').value,text});$('paste-dialog').close();};
$('chords-form').onsubmit=event=>{event.preventDefault();try{manualPositions=manual($('chord-input').value,catalog);cards($('manual-diagrams'),manualPositions);$('manual-result').hidden=false;status('');store.set('manual',$('chord-input').value);}catch(error){status(error.message);}};
$('example-button').onclick=()=>{cancelLookup();showSong({title:'Hoy suena la guitarra · Ejemplo',source:'',tuning:'standard',text:'G 320033\nD xx0232\nEm 022000\nC x32010\n\n[Intro]\nG   D   Em   C\n\n[Verso]\nG              D\nHoy suena la guitarra\nEm           C\ny vuelvo a empezar\n'},false);};
async function download(positions,title){
 if(!positions.length)return;const button=mode==='songs'?$('song-download'):$('manual-download');button.disabled=true;
 try{const columns=2,cardWidth=420,cardHeight=440,top=120;const canvas=document.createElement('canvas');canvas.width=columns*cardWidth;canvas.height=top+Math.ceil(positions.length/columns)*cardHeight;const ctx=canvas.getContext('2d');ctx.fillStyle='#F7F3EB';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.fillStyle='#29382E';ctx.font='bold 30px sans-serif';ctx.fillText('Posiciones de guitarra',30,45);ctx.font='20px sans-serif';ctx.fillText('E A D G B e · Afinación estándar',30,82);
 for(let i=0;i<positions.length;i++){const p=positions[i],x=(i%columns)*cardWidth,y=top+Math.floor(i/columns)*cardHeight;ctx.fillStyle='#29382E';ctx.font='bold 32px sans-serif';ctx.fillText(p.name,x+50,y+40);const blob=new Blob([diagram(p.frets)],{type:'image/svg+xml'}),url=URL.createObjectURL(blob);try{const image=new Image();await new Promise((resolve,reject)=>{image.onload=resolve;image.onerror=reject;image.src=url;});ctx.drawImage(image,x+20,y+55,380,344);}finally{URL.revokeObjectURL(url);}}
 const blob=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));if(!blob)throw Error('No se pudo crear la imagen.');const file=new File([blob],'posiciones.png',{type:'image/png'});
 if(navigator.canShare?.({files:[file]})){await navigator.share({files:[file],title:title||'Posiciones de guitarra'});}else{const url=URL.createObjectURL(blob),link=document.createElement('a');link.href=url;link.download='posiciones.png';link.click();setTimeout(()=>URL.revokeObjectURL(url),60000);}
 }catch(error){if(error.name!=='AbortError')status('No se pudo compartir la imagen. Intenta de nuevo.');}finally{button.disabled=false;}
}
$('manual-download').onclick=()=>download(manualPositions);$('song-download').onclick=()=>download(parsed.positions,song.title);
try{const response=await fetch('./chords.json');if(!response.ok)throw Error();catalog=await response.json();$('query').value=store.get('query')||'';$('chord-input').value=store.get('manual')||'C G Am F';manualPositions=manual($('chord-input').value,catalog);cards($('manual-diagrams'),manualPositions);const saved=store.get('song');if(saved){try{const value=JSON.parse(saved);if(typeof value.text==='string'&&value.text.length<=100000)showSong(value,false);}catch{}}}catch{status('No se cargaron las posiciones. Revisa tu conexión y vuelve a abrir la página.');}
if('serviceWorker'in navigator)navigator.serviceWorker.register('./sw.js').catch(()=>{});
