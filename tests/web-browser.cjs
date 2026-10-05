// Optional browser check: install Playwright and Microsoft Edge before running.
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({channel:'msedge',headless:true});
 const context=await browser.newContext({viewport:{width:390,height:844},deviceScaleFactor:2});
 const page=await context.newPage();const errors=[];page.on('pageerror',error=>errors.push(error.message));
 const url=process.argv[2]||'http://127.0.0.1:4173/';
 await page.goto(url);await page.waitForFunction(()=>document.querySelectorAll('#manual-diagrams .diagram-card').length===4);
 await page.click('#chords-tab');assert.equal(await page.locator('#manual-result').isVisible(),true);
 await page.fill('#chord-input','Do Sol Lam Fa');await page.click('#chords-form .primary');assert.equal(await page.locator('#manual-diagrams .diagram-card').count(),4);
 await page.locator('#manual-diagrams .diagram-card').first().click();assert.equal(await page.locator('#chord-dialog').isVisible(),true);await page.click('#close-chord');
 await page.click('#songs-tab');await page.click('#paste-button');await page.fill('#title-input','Prueba propia');
 await page.fill('#sheet-input','G 320033\nD xx0323\nCadd9 x32030\n\n[Verso]\nG       D\nHoy suena mi guitarra\nCadd9\ny vuelvo a empezar');await page.click('#paste-form .primary');
 assert.equal(await page.locator('#song-diagrams .diagram-card').count(),3);
 assert(!(await page.locator('#sheet').innerText()).includes('320033'));
 assert((await page.locator('#sheet').innerText()).includes('G       D'));
 assert.equal(await page.locator('#sheet .chord').count(),3);
 await page.screenshot({path:'screenshots/web-iphone.png',fullPage:true});
 assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
 await page.reload();await page.waitForFunction(()=>document.querySelector('#song-title').textContent==='Prueba propia');
 await page.evaluate(()=>navigator.serviceWorker.ready);await page.waitForFunction(()=>Boolean(navigator.serviceWorker.controller));
 await context.setOffline(true);await page.reload();await page.waitForFunction(()=>document.querySelector('#song-title').textContent==='Prueba propia');assert.equal(await page.locator('#song-diagrams .diagram-card').count(),3);await context.setOffline(false);
 await page.click('#paste-button');await page.selectOption('#tuning-input','other');await page.fill('#sheet-input','G   D\nTexto propio');await page.click('#paste-form .primary');assert.equal(await page.locator('#song-diagrams .diagram-card').count(),0);
 assert.deepEqual(errors,[]);console.log('Browser: mobile layout, source diagrams, preserved spacing, chord popup, persistence, offline and nonstandard tuning passed.');
 await browser.close();
})().catch(error=>{console.error(error);process.exit(1);});
