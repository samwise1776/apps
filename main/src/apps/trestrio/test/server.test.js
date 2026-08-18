const test = require('node:test'); const assert = require('node:assert/strict'); const server = require('../server');
test('serves the app, utility library, and API state', async t => { await new Promise(r=>server.listen(0,r)); t.after(()=>server.close()); const base=`http://127.0.0.1:${server.address().port}`; const page=await fetch(base); assert.equal(page.status,200); const html=await page.text(); assert.match(html,/Trestrio/); assert.match(html,/utilities\.js/); const utilitySource=await (await fetch(`${base}/utilities.js`)).text(); assert.match(utilitySource,/Expected 100 utilities/); const api=await fetch(`${base}/api/state`); assert.equal(api.status,200); assert.ok(Array.isArray((await api.json()).tasks)); });
test('sets browser security headers and rejects cross-origin writes', async t => {
  await new Promise(r=>server.listen(0, '127.0.0.1', r));
  t.after(()=>server.close());
  const base=`http://127.0.0.1:${server.address().port}`;
  const page=await fetch(base);
  assert.equal(page.headers.get('x-content-type-options'), 'nosniff');
  assert.match(page.headers.get('content-security-policy'), /frame-ancestors 'none'/);
  const response=await fetch(`${base}/api/tasks`, {method:'POST', headers:{origin:'https://attacker.invalid','content-type':'application/json'}, body:'{"title":"blocked"}'});
  assert.equal(response.status,403);
});
