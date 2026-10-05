import {searchSongs,loadSong,CatalogError} from '../lib/catalog.js';
const WEB_ORIGIN='https://luisca66.github.io';
export default async function handler(req,res){
 const origin=req.headers.origin;
 const allowed=origin===WEB_ORIGIN||origin==='https://acordes-guitarra-luisca66.vercel.app'||(!process.env.VERCEL&&origin==='http://127.0.0.1:4173');
 res.setHeader('Cache-Control','no-store');res.setHeader('Vary','Origin');res.setHeader('X-Content-Type-Options','nosniff');
 if(origin&&!allowed)return res.status(403).json({error:'Origen no permitido.'});
 res.setHeader('Access-Control-Allow-Origin',origin||WEB_ORIGIN);
 if(req.method==='OPTIONS'){res.setHeader('Access-Control-Allow-Methods','GET, OPTIONS');return res.status(204).end();}
 if(req.method!=='GET'){res.setHeader('Allow','GET, OPTIONS');return res.status(405).json({error:'Usa GET para consultar canciones.'});}
 try{
  const params=new URL(req.url,'https://service.local').searchParams;
  if(params.has('q')===params.has('url'))throw new CatalogError('Indica un título o una versión de canción.',400);
  const result=params.has('q')?{songs:await searchSongs(params.get('q'))}:await loadSong(params.get('url'));
  res.setHeader('Cache-Control','public, max-age=0, s-maxage=300, stale-while-revalidate=600');
  return res.status(200).json(result);
 }catch(error){return res.status(error instanceof CatalogError?error.status:502).json({error:error instanceof CatalogError?error.message:'No se pudo cargar el catálogo. Intenta de nuevo.'});}
}
