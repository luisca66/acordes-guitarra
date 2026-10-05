const SEARCH='https://www.ultimate-guitar.com/search.php';
const MAX_BODY=3_000_000;
export class CatalogError extends Error {
 constructor(message,status=502){super(message);this.status=status;}
}
export function safeSongUrl(value){
 try{const url=new URL(value);return url.protocol==='https:'&&url.hostname==='tabs.ultimate-guitar.com'&&!url.port&&!url.username&&!url.password&&/^\/tab\/[a-zA-Z0-9_/-]+$/.test(url.pathname)&&!url.search&&!url.hash;}catch{return false;}
}
export function decodeEntities(text){
 return text.replace(/&(#x[0-9a-f]+|#\d+|quot|amp|apos|lt|gt);/gi,(whole,entity)=>{
  if(entity[0]==='#'){const point=entity[1].toLowerCase()==='x'?parseInt(entity.slice(2),16):Number(entity.slice(1));return point>0&&point<=0x10ffff?String.fromCodePoint(point):whole;}
  return {quot:'"',amp:'&',apos:"'",lt:'<',gt:'>'}[entity.toLowerCase()]||whole;
 });
}
export function extractStore(html){
 const tag=html.match(/<[^>]+\bclass=["'][^"']*\bjs-store\b[^"']*["'][^>]*>/i)?.[0];
 const content=tag?.match(/\bdata-content="([^"]*)"/i)?.[1]||tag?.match(/\bdata-content='([^']*)'/i)?.[1];
 if(!content)throw new CatalogError('El catálogo cambió su formato. Intenta más tarde o pega tu hoja de acordes.');
 try{const data=JSON.parse(decodeEntities(content)).store.page.data;if(!data||typeof data!=='object')throw Error();return data;}catch{throw new CatalogError('No se pudo leer la respuesta del catálogo. Intenta de nuevo.');}
}
export function noResults(html){return /<title[^>]*>\s*No results\s*@\s*Ultimate-Guitar\.Com Search\s*<\/title>/i.test(html);}
async function limitedText(response){
 if(Number(response.headers.get('content-length'))>MAX_BODY)throw new CatalogError('La respuesta del catálogo es demasiado grande.');
 if(!response.body)return '';
 const reader=response.body.getReader(),chunks=[];let size=0;
 try{while(true){const {done,value}=await reader.read();if(done)break;size+=value.byteLength;if(size>MAX_BODY)throw new CatalogError('La respuesta del catálogo es demasiado grande.');chunks.push(value);}}catch(error){await reader.cancel().catch(()=>{});throw error;}finally{reader.releaseLock();}
 const body=new Uint8Array(size);let offset=0;for(const chunk of chunks){body.set(chunk,offset);offset+=chunk.length;}return new TextDecoder().decode(body);
}
async function page(url,fetcher){
 let response;
 try{response=await fetcher(url,{signal:AbortSignal.timeout(14000),redirect:'error',headers:{'User-Agent':'AcordesGuitarra/2.3','Accept':'text/html','Accept-Language':'en'}});}catch{throw new CatalogError('El catálogo tardó demasiado o no está disponible. Intenta de nuevo.',504);}
 if(response.status===403)throw new CatalogError('El catálogo no permite cargar esta versión ahora. Prueba más tarde o pega tu hoja de acordes.');
 if(response.status===429)throw new CatalogError('El catálogo está ocupado. Espera un momento y vuelve a intentar.',503);
 if(!response.ok&&response.status!==404)throw new CatalogError('El catálogo no respondió. Intenta de nuevo.');
 return {status:response.status,html:await limitedText(response)};
}
export async function searchSongs(query,fetcher=fetch){
 const q=typeof query==='string'?query.trim():'';
 if(!q||q.length>150)throw new CatalogError('Escribe un título de hasta 150 caracteres; puedes agregar el artista.',400);
 const url=new URL(SEARCH);url.searchParams.set('search_type','title');url.searchParams.set('value',q);
 const result=await page(url.href,fetcher);
 if(noResults(result.html))return [];
 if(result.status===404)throw new CatalogError('La página de búsqueda no está disponible. Intenta de nuevo.');
 const rows=extractStore(result.html).results;if(!Array.isArray(rows))throw new CatalogError('No se pudo leer la lista de canciones. Intenta de nuevo.');
 return rows.filter(row=>row.type==='Chords'&&safeSongUrl(row.tab_url)).slice(0,20).map(row=>({id:row.id,title:String(row.song_name||'Canción'),artist:String(row.artist_name||''),url:row.tab_url,version:row.version||null,rating:row.rating||null,votes:row.votes||null}));
}
export async function loadSong(url,fetcher=fetch){
 if(!safeSongUrl(url))throw new CatalogError('El enlace de la versión no es válido.',400);
 const result=await page(url,fetcher);
 if(result.status===404)throw new CatalogError('Esta versión ya no está disponible. Elige otra canción.',404);
 const data=extractStore(result.html),view=data.tab_view,wiki=view?.wiki_tab,text=wiki?.content;
 if(typeof text!=='string'||!text.trim())throw new CatalogError('Esta versión no tiene una hoja pública de letra y acordes. Elige otra versión.',404);
 if(text.length>100000)throw new CatalogError('Esta hoja es demasiado larga. Elige otra versión.');
 const tuning=view?.meta?.tuning?.value||'';
 return {text,source:url,author:String(wiki.username||''),tuning:!tuning||/^[Ee]\s+[Aa]\s+[Dd]\s+[Gg]\s+[Bb]\s+[Ee]$/.test(tuning.trim())?'standard':'other',originalTuning:tuning,capo:view?.meta?.capo??null};
}
