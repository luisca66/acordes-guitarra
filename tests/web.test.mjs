import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import {manual,parseSheet,normalize,diagram} from '../docs/music.js';
const catalog=JSON.parse(readFileSync(new URL('../docs/chords.json',import.meta.url)));
assert.equal(normalize('Sol'),'G');assert.equal(normalize('Fa'),'F');assert.equal(normalize('Faug'),'Faug');assert.equal(normalize('D/F#'),'D/F#');
assert.deepEqual(manual('Do Sol Lam Fa C',catalog).map(x=>x.name),['C','G','Am','F']);
assert.throws(()=>manual('I V vi IV',catalog));assert.throws(()=>manual('Cfoobar',catalog));
const sheet=parseSheet('G 320033\nD xx0323\nCadd9 x32030\n\n[Verse 1]\n  [ch]G[/ch]       [ch]D[/ch]\n  Original words here\nG     Cadd9\nAnother line\n',catalog);
assert.equal(sheet.positions.length,3);assert.deepEqual(sheet.definitions.get('D').frets,[-1,-1,0,3,2,3]);assert.deepEqual(sheet.definitions.get('Cadd9').frets,[-1,3,2,0,3,0]);
assert.equal(sheet.lines[5].parts.map(p=>p.text).join(''),'  G       D');assert.equal(sheet.lines[6].parts[0].text,'  Original words here');assert.equal(sheet.lines[7].parts.filter(p=>p.name).length,2);assert(sheet.lines[4].heading);
assert.equal(parseSheet('G xxxxxx\nG 32003',catalog).definitions.size,0);
assert.equal(parseSheet('A lyric sentence',catalog).names.length,0);
assert.equal(parseSheet('[Am]Original text',catalog).lines[0].parts[0].name,'Am');
for(const [name,frets] of Object.entries(catalog)){assert.equal(frets.length,6,name);assert(frets.every(f=>Number.isInteger(f)&&f>=-1&&f<=24),name);assert(diagram(frets).includes('</svg>'));}
console.log(`Web: parser, alignment, source digitations and ${Object.keys(catalog).length} positions verified.`);
