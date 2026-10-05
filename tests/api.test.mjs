import assert from 'node:assert/strict';
import handler from '../api/catalog.js';
function response(){return {headers:{},statusCode:200,setHeader(key,value){this.headers[key]=value;},status(code){this.statusCode=code;return this;},json(value){this.body=value;return this;},end(){return this;}};}
const github='https://luisca66.github.io';
let res=response();await handler({url:'/api/catalog?q=Test',method:'GET',headers:{origin:'https://evil.example'}},res);assert.equal(res.statusCode,403);
res=response();await handler({url:'/api/catalog',method:'POST',headers:{origin:github}},res);assert.equal(res.statusCode,405);assert.equal(res.headers.Allow,'GET, OPTIONS');
res=response();await handler({url:'/api/catalog',method:'OPTIONS',headers:{origin:github}},res);assert.equal(res.statusCode,204);assert.equal(res.headers['Access-Control-Allow-Origin'],github);
for(const url of ['/api/catalog','/api/catalog?q=Test&url=bad','/api/catalog?url=https://evil.example/']){res=response();await handler({url,method:'GET',headers:{origin:github}},res);assert.equal(res.statusCode,400);assert.equal(res.headers['Cache-Control'],'no-store');}
const originalFetch=global.fetch;
try{global.fetch=async()=>new Response('<div class="js-store" data-content="{&quot;store&quot;:{&quot;page&quot;:{&quot;data&quot;:{&quot;results&quot;:[]}}}}"></div>');res=response();await handler({url:'/api/catalog?q=Original',method:'GET',headers:{origin:github}},res);assert.equal(res.statusCode,200);assert.deepEqual(res.body,{songs:[]});assert(res.headers['Cache-Control'].includes('s-maxage=300'));}finally{global.fetch=originalFetch;}
console.log('API: CORS, allowed methods, validation, responses and caching passed.');
