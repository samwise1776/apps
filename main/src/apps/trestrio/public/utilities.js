(() => {
  const text = [
    ['Word counter','Count words in any passage',v=>wordList(v).length],
    ['Character counter','Count every character',v=>v.length],
    ['Character counter (no spaces)','Count visible characters',v=>v.replace(/\s/g,'').length],
    ['Sentence counter','Estimate the number of sentences',v=>v.trim()?v.trim().split(/[.!?]+(?=\s|$)/).filter(Boolean).length:0],
    ['Paragraph counter','Count blocks of text',v=>v.trim()?v.trim().split(/\n\s*\n/).filter(Boolean).length:0],
    ['Line counter','Count lines',v=>v? v.split(/\r?\n/).length:0],
    ['Reading time','Estimate reading time',v=>`${Math.max(1,Math.ceil(wordList(v).length/225))} min`],
    ['Reverse text','Reverse every character',v=>[...v].reverse().join('')],
    ['Sort lines','Sort lines alphabetically',v=>v.split(/\r?\n/).sort((a,b)=>a.localeCompare(b)).join('\n')],
    ['Unique lines','Remove repeated lines',v=>[...new Set(v.split(/\r?\n/))].join('\n')],
    ['Trim lines','Remove surrounding line spaces',v=>v.split(/\r?\n/).map(x=>x.trim()).join('\n')],
    ['Remove blank lines','Keep only lines with content',v=>v.split(/\r?\n/).filter(x=>x.trim()).join('\n')],
    ['Slug maker','Create a URL-friendly slug',v=>slug(v)],
    ['camelCase converter','Convert words to camelCase',v=>{const a=parts(v);return (a[0]||'')+a.slice(1).map(cap).join('')}],
    ['PascalCase converter','Convert words to PascalCase',v=>parts(v).map(cap).join('')],
    ['snake_case converter','Convert words to snake_case',v=>parts(v).join('_')],
    ['kebab-case converter','Convert words to kebab-case',v=>parts(v).join('-')],
    ['CONSTANT_CASE converter','Convert words to CONSTANT_CASE',v=>parts(v).join('_').toUpperCase()],
    ['dot.case converter','Convert words to dot.case',v=>parts(v).join('.')],
    ['aLtErNaTiNg CaSe','Alternate letter case',v=>[...v].map((c,i)=>i%2?c.toLowerCase():c.toUpperCase()).join('')],
    ['Initials maker','Extract initials',v=>wordList(v).map(x=>x[0].toUpperCase()).join('')],
    ['ROT13 encoder','Apply the ROT13 cipher',v=>v.replace(/[a-z]/gi,c=>String.fromCharCode(c.charCodeAt(0)+(c.toLowerCase()<'n'?13:-13)))],
    ['Base64 encoder','Encode text as Base64',v=>safe(()=>btoa(unescape(encodeURIComponent(v))))],
    ['Base64 decoder','Decode Base64 text',v=>safe(()=>decodeURIComponent(escape(atob(v.trim()))))],
    ['URL encoder','Encode a URL component',v=>encodeURIComponent(v)],
    ['URL decoder','Decode a URL component',v=>safe(()=>decodeURIComponent(v))],
    ['HTML escaper','Escape HTML special characters',v=>v.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))],
    ['HTML unescaper','Decode HTML entities',v=>{const d=document.createElement('textarea');d.innerHTML=v;return d.value}],
    ['HTML tag remover','Strip markup from text',v=>{const d=document.createElement('div');d.innerHTML=v;return d.textContent||''}],
    ['Duplicate word remover','Keep the first occurrence of each word',v=>[...new Set(wordList(v).map(x=>x.toLowerCase()))].join(' ')],
    ['Word frequency','Rank words by frequency',v=>{const m={};wordList(v.toLowerCase()).forEach(x=>m[x]=(m[x]||0)+1);return Object.entries(m).sort((a,b)=>b[1]-a[1]).map(([w,n])=>`${w}: ${n}`).join('\n')}],
    ['Longest word','Find the longest word',v=>wordList(v).sort((a,b)=>b.length-a.length)[0]||''],
    ['Vowel counter','Count vowels',v=>(v.match(/[aeiou]/gi)||[]).length],
    ['Consonant counter','Count consonants',v=>(v.match(/[b-df-hj-np-tv-z]/gi)||[]).length],
    ['Palindrome checker','Check text forwards and backwards',v=>{const s=v.toLowerCase().replace(/[^a-z0-9]/g,'');return s?String(s===[...s].reverse().join('')):'false'}]
  ].map(x=>tool('Text',...x,'text'));

  const number = [
    ['Square','Raise a number to power 2',a=>a*a],['Cube','Raise a number to power 3',a=>a*a*a],
    ['Square root','Find a square root',a=>Math.sqrt(a)],['Cube root','Find a cube root',a=>Math.cbrt(a)],
    ['Absolute value','Remove a number’s sign',a=>Math.abs(a)],['Round number','Round to the nearest integer',a=>Math.round(a)],
    ['Floor number','Round down',a=>Math.floor(a)],['Ceiling number','Round up',a=>Math.ceil(a)],
    ['Factorial','Multiply integers down to one',a=>{if(a<0||a>170||a%1)return 'Enter an integer from 0 to 170';let n=1;for(let i=2;i<=a;i++)n*=i;return n}],
    ['Fibonacci number','Find the nth Fibonacci number',a=>{if(a<0||a>1476||a%1)return 'Enter an integer from 0 to 1,476';let x=0,y=1;for(let i=0;i<a;i++)[x,y]=[y,x+y];return x}],
    ['Prime checker','Test whether an integer is prime',a=>{if(a<2||a%1)return 'false';for(let i=2;i<=Math.sqrt(a);i++)if(a%i===0)return 'false';return 'true'}],
    ['Greatest common divisor','Find the GCD of two numbers',(a,b)=>{a=Math.abs(a);b=Math.abs(b);while(b)[a,b]=[b,a%b];return a},'two'],
    ['Least common multiple','Find the LCM of two numbers',(a,b)=>Math.abs(a*b)/gcd(a,b),'two'],
    ['Percentage change','Measure change from A to B',(a,b)=>a===0?'Undefined':`${fmt((b-a)/Math.abs(a)*100)}%`,'two'],
    ['Discount calculator','Apply a percentage discount',(a,b)=>fmt(a*(1-b/100)),'two','Price','Discount %'],
    ['Sales tax calculator','Add tax to a price',(a,b)=>fmt(a*(1+b/100)),'two','Price','Tax %'],
    ['Tip calculator','Calculate tip amount',(a,b)=>fmt(a*b/100),'two','Bill','Tip %'],
    ['Simple interest','Calculate yearly simple interest',(a,b)=>fmt(a*b/100),'two','Principal','Rate %'],
    ['Compound growth','One year of compounded growth',(a,b)=>fmt(a*(1+b/100)),'two','Starting value','Growth %'],
    ['Monthly loan payment','Payment for a 12-month loan',(a,b)=>{const r=b/1200;return fmt(r?a*r/(1-(1+r)**-12):a/12)},'two','Loan','APR %'],
    ['BMI calculator','Calculate body mass index',(a,b)=>b?fmt(a/((b/100)**2)):'—','two','Weight kg','Height cm'],
    ['Celsius to Fahrenheit','Convert temperature',a=>fmt(a*9/5+32)],
    ['Fahrenheit to Celsius','Convert temperature',a=>fmt((a-32)*5/9)],
    ['Celsius to Kelvin','Convert temperature',a=>fmt(a+273.15)],
    ['Kilometers to miles','Convert distance',a=>fmt(a*.621371)],
    ['Miles to kilometers','Convert distance',a=>fmt(a*1.609344)],
    ['Kilograms to pounds','Convert weight',a=>fmt(a*2.2046226218)],
    ['Pounds to kilograms','Convert weight',a=>fmt(a*.45359237)],
    ['Centimeters to inches','Convert length',a=>fmt(a/2.54)],
    ['Liters to US gallons','Convert volume',a=>fmt(a*.264172052)]
  ].map(x=>tool('Numbers',x[0],x[1],x[2],x[3]||'number',x[4],x[5]));

  const developer = [
    ['JSON formatter','Pretty-print JSON',v=>safe(()=>JSON.stringify(JSON.parse(v),null,2))],
    ['JSON minifier','Compact JSON',v=>safe(()=>JSON.stringify(JSON.parse(v)))],
    ['JSON validator','Check JSON syntax',v=>{try{JSON.parse(v);return 'Valid JSON'}catch(e){return `Invalid: ${e.message}`}}],
    ['CSV to JSON','Convert simple CSV rows',v=>{const [h,...rows]=v.trim().split(/\r?\n/).map(csvRow);return JSON.stringify(rows.map(r=>Object.fromEntries(h.map((k,i)=>[k,r[i]||'']))),null,2)}],
    ['JSON to CSV','Convert an array of objects',v=>safe(()=>{const a=JSON.parse(v);if(!Array.isArray(a)||!a.length)return '';const h=Object.keys(a[0]);return [h,...a.map(o=>h.map(k=>csvEscape(o[k])))].map(r=>r.join(',')).join('\n')})],
    ['Hex to decimal','Convert a hexadecimal integer',v=>String(parseInt(v.trim().replace(/^0x/i,''),16))],
    ['Decimal to hex','Convert an integer to hexadecimal',v=>(Number(v)||0).toString(16).toUpperCase()],
    ['Binary to decimal','Convert a binary integer',v=>String(parseInt(v.trim(),2))],
    ['Decimal to binary','Convert an integer to binary',v=>(Number(v)||0).toString(2)],
    ['Octal to decimal','Convert an octal integer',v=>String(parseInt(v.trim(),8))],
    ['Decimal to octal','Convert an integer to octal',v=>(Number(v)||0).toString(8)],
    ['Query string parser','Turn URL parameters into JSON',v=>JSON.stringify(Object.fromEntries(new URLSearchParams(v.includes('?')?v.split('?')[1]:v)),null,2)],
    ['Query string builder','Turn a JSON object into parameters',v=>safe(()=>new URLSearchParams(JSON.parse(v)).toString())],
    ['CSS minifier','Remove comments and extra spacing',v=>v.replace(/\/\*[\s\S]*?\*\//g,'').replace(/\s+/g,' ').replace(/\s*([{}:;,])\s*/g,'$1').trim()],
    ['JavaScript string escaper','Create a safe JSON string',v=>JSON.stringify(v)]
  ].map(x=>tool('Developer',...x,'text'));

  const generators = [
    ['UUID generator','Create a random UUID',()=>crypto.randomUUID(),'generate'],
    ['Password generator','Create a strong 20-character password',()=>randomFrom('ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*',20),'generate'],
    ['PIN generator','Create a six-digit PIN',()=>randomFrom('0123456789',6),'generate'],
    ['Random integer','Pick an integer from 1 to 100',()=>String(crypto.getRandomValues(new Uint32Array(1))[0]%100+1),'generate'],
    ['Random yes or no','Make a binary choice',()=>Math.random()<.5?'Yes':'No','generate'],
    ['Coin flipper','Flip a virtual coin',()=>Math.random()<.5?'Heads':'Tails','generate'],
    ['Dice roller','Roll a six-sided die',()=>String(Math.floor(Math.random()*6)+1),'generate'],
    ['Random color','Create a hexadecimal color',()=>`#${Math.floor(Math.random()*0xffffff).toString(16).padStart(6,'0')}`,'generate'],
    ['Lorem ipsum','Generate a short placeholder paragraph',()=>lorem,'generate'],
    ['Random picker','Pick one item from separate lines',v=>{const a=v.split(/\r?\n/).filter(Boolean);return a[Math.floor(Math.random()*a.length)]||''},'text-button']
  ].map(x=>tool('Generate',...x));

  const dates = [
    ['Age calculator','Calculate age from a birth date',v=>{const d=new Date(v),n=new Date();let a=n.getFullYear()-d.getFullYear();if(n<new Date(n.getFullYear(),d.getMonth(),d.getDate()))a--;return Number.isFinite(a)?`${a} years`:'Choose a date'},'date'],
    ['Days between dates','Find the absolute day difference',(a,b)=>`${Math.round(Math.abs(new Date(b)-new Date(a))/864e5)} days`,'two-date'],
    ['Date to Unix time','Convert a date to Unix seconds',v=>String(Math.floor(new Date(v).getTime()/1000)),'date'],
    ['Unix time to date','Convert seconds to local time',v=>safe(()=>new Date(Number(v)*1000).toLocaleString()),'text'],
    ['Day of week','Name the weekday for a date',v=>new Date(`${v}T12:00:00`).toLocaleDateString(undefined,{weekday:'long'}),'date'],
    ['Day of year','Find a date’s ordinal day',v=>String(Math.floor((new Date(v)-new Date(new Date(v).getFullYear(),0,0))/864e5)),'date'],
    ['Week number','Find the ISO week number',v=>{const d=new Date(v);d.setHours(0,0,0,0);d.setDate(d.getDate()+3-(d.getDay()+6)%7);const w=new Date(d.getFullYear(),0,4);return String(1+Math.round(((d-w)/864e5-3+(w.getDay()+6)%7)/7))},'date'],
    ['Add days','Add days to today',v=>{const d=new Date();d.setDate(d.getDate()+(Number(v)||0));return d.toLocaleDateString()},'number'],
    ['Business days from now','Add weekdays to today',v=>{let d=new Date(),n=Math.max(0,Number(v)||0);while(n>0){d.setDate(d.getDate()+1);if(d.getDay()%6)n--}return d.toLocaleDateString()},'number'],
    ['Countdown in days','Count down to a date',v=>`${Math.ceil((new Date(`${v}T23:59:59`)-new Date())/864e5)} days`,'date']
  ].map(x=>tool('Date & Time',...x));

  function tool(category,name,description,run,type='text',labelA='Value A',labelB='Value B'){return {category,name,description,run,type,labelA,labelB,id:slug(name)}}
  function wordList(v){return v.trim().match(/[\p{L}\p{N}'’-]+/gu)||[]}
  function parts(v){return v.trim().replace(/([a-z])([A-Z])/g,'$1 $2').toLowerCase().split(/[^a-z0-9]+/).filter(Boolean)}
  function cap(v){return v.charAt(0).toUpperCase()+v.slice(1)}
  function slug(v){return parts(v).join('-')}
  function safe(fn){try{return fn()}catch(e){return `Invalid input: ${e.message}`}}
  function fmt(v){return Number.isFinite(v)?Number(v.toFixed(8)).toLocaleString():'—'}
  function gcd(a,b){while(b)[a,b]=[b,a%b];return Math.abs(a)}
  function csvRow(s){return s.match(/("(?:[^"]|"")*"|[^,]*)(?:,|$)/g).map(x=>x.replace(/,$/,'').replace(/^"|"$/g,'').replace(/""/g,'"')).slice(0,-1)}
  function csvEscape(v){v=String(v??'');return /[",\n]/.test(v)?`"${v.replace(/"/g,'""')}"`:v}
  function randomFrom(chars,length){const a=crypto.getRandomValues(new Uint32Array(length));return [...a].map(n=>chars[n%chars.length]).join('')}
  const lorem='Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer vitae justo vel sapien interdum posuere. Curabitur euismod, nunc at consequat feugiat, tellus erat viverra erat, a facilisis metus neque vitae lorem.';
  const all=[...text,...number,...developer,...generators,...dates];
  if(all.length!==100) throw new Error(`Expected 100 utilities, found ${all.length}`);
  window.TRESTRIO_UTILITIES=all;
})();
