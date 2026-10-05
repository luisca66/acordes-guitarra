const spanish={Do:'C',Re:'D',Mi:'E',Fa:'F',Sol:'G',La:'A',Si:'B'};
export function normalize(text){
 let value=text.trim().replaceAll('♯','#').replaceAll('♭','b').replace(/[°º]/g,'dim');
 const s=value.match(/^(Do|Re|Mi|Fa|Sol|La|Si)([#b]?)(.*)$/);
 if(s&&/^(?:m|7|maj7|M7|min|m7|dim|dim7|aug|sus2|sus4|add9|madd9|7sus4|7sus2|6|m6|m7b5|5|9|m9|maj9)?(?:\/[A-Ga-g][#b]?)?$/.test(s[3]))value=spanish[s[1]]+s[2]+s[3];
 const m=value.match(/^([A-Ga-g])([#b]?)([^/]*)(?:\/([A-Ga-g])([#b]?))?$/);
 if(!m)throw Error(`No reconozco «${text}». Usa nombres como C, Am, Sol o D/F#.`);
 let quality=m[3]==='min'?'m':m[3]==='M7'?'maj7':m[3];
 return m[1].toUpperCase()+m[2]+quality+(m[4]?'/'+m[4].toUpperCase()+m[5]:'');
}
export function manual(text,catalog){
 const tokens=text.trim().split(/[\s,;|–—−-]+/).filter(Boolean);
 if(!tokens.length)throw Error('Escribe los acordes, por ejemplo: C G Am F.');
 if(tokens.length>64)throw Error('Usa hasta 64 nombres de acordes.');
 const names=[...new Set(tokens.map(normalize))];
 if(names.length>24)throw Error('Usa hasta 24 acordes diferentes.');
 return names.map(name=>{if(!catalog[name])throw Error(`Aún no hay una posición para «${name}».`);return {name,frets:catalog[name],source:false};});
}
function chordName(token){try{return normalize(token);}catch{return null;}}
const tokenPattern=/^[A-Ga-g][#b]?(?:m|maj|min|dim|aug|sus|add|M)?[a-z0-9#b]*(?:\/[A-Ga-g][#b]?)?$/;
export function parseSheet(text,catalog){
 const definitions=new Map(),names=new Set();
 const lines=text.replace(/\[\/?tab\]/gi,'').replace(/\r\n?/g,'\n').split('\n').map(raw=>{
  const plain=raw.replace(/\[ch\]([^\[]+)\[\/ch\]/gi,'$1');
  const position=plain.match(/^\s*([A-Ga-g][#b]?\S*)\s+([xX0-9]{6})\s*$/);
  if(position&&!/^x{6}$/i.test(position[2])){const name=chordName(position[1]);if(name){if(!definitions.has(name))definitions.set(name,{name,frets:[...position[2]].map(f=>/x/i.test(f)?-1:Number(f)),source:true});names.add(name);return {position:true,parts:[]};}}
  if(/^\s*\[[^\]]+\]\s*$/.test(raw)&&!/^\s*\[ch\]/i.test(raw))return {heading:true,parts:[{text:raw.trim().slice(1,-1)}]};
  const parts=[];
  const tagged=/\[ch\]([^\[]+)\[\/ch\]|\[([A-Ga-g][#b]?[^\]\s]*)\]/gi;
  let match,last=0,hasTags=false;
  while((match=tagged.exec(raw))){hasTags=true;if(match.index>last)parts.push({text:raw.slice(last,match.index)});const name=chordName(match[1]||match[2]);parts.push({text:match[1]||match[2],name});if(name)names.add(name);last=tagged.lastIndex;}
  if(hasTags){if(last<raw.length)parts.push({text:raw.slice(last)});return {parts};}
  const tokens=raw.trim().split(/\s+/).filter(Boolean);
  if(tokens.length&&tokens.every(token=>tokenPattern.test(token)&&chordName(token))){
   for(const chunk of raw.split(/(\s+)/)){const name=chunk.trim()?chordName(chunk):null;parts.push({text:chunk,name});if(name)names.add(name);}
  }else parts.push({text:raw});
  return {parts};
 });
 const positions=definitions.size?[...definitions.values()]:[...names].filter(name=>catalog[name]).map(name=>({name,frets:catalog[name],source:false}));
 const missing=[...names].filter(name=>!definitions.has(name)&&!catalog[name]);
 return {lines,positions,definitions,names:[...names],missing};
}
export function diagram(frets){
 const pressed=frets.filter(f=>f>0),start=pressed.length&&Math.max(...pressed)>5?Math.min(...pressed):1;
 const labels=['E','A','D','G','B','e'];
 let svg='<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 210 190" role="img" aria-label="Posición de guitarra"><g stroke="#29382E" stroke-width="1.4">';
 for(let s=0;s<6;s++)svg+=`<path d="M${40+s*28} 40V160"/>`;
 for(let f=0;f<=5;f++)svg+=`<path stroke-width="${f===0&&start===1?5:1.4}" d="M40 ${40+f*24}H180"/>`;
 svg+='</g><g fill="#29382E" font-family="sans-serif" font-size="13" text-anchor="middle">';
 if(start>1)svg+=`<text x="18" y="58">${start}</text>`;
 frets.forEach((f,s)=>{const x=40+s*28;svg+=`<text x="${x}" y="181">${labels[s]}</text>`;if(f===-1)svg+=`<text x="${x}" y="28" font-size="20">×</text>`;else if(f===0)svg+=`<circle cx="${x}" cy="22" r="6" fill="none" stroke="#29382E" stroke-width="1.7"/>`;else svg+=`<circle cx="${x}" cy="${40+(f-start+.5)*24}" r="9" fill="#A85432"/>`;});
 return svg+'</g></svg>';
}
export function describe(frets){return frets.map((f,i)=>`${['sexta','quinta','cuarta','tercera','segunda','primera'][i]} cuerda: ${f<0?'no tocar':f===0?'al aire':'traste '+f}`).join('; ');}
