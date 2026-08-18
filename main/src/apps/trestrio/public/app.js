const $ = s => document.querySelector(s); const $$ = s => [...document.querySelectorAll(s)];
let state = { tasks: [], notes: '', focusSessions: 0, focusMinutes: 0 }, timerId, selectedMinutes = 25, seconds = 1500, running = false, saveTimer;
const savedTheme=localStorage.getItem('trestrio-theme'); if(savedTheme)document.documentElement.dataset.theme=savedTheme;
function syncTheme(){ const dark=document.documentElement.dataset.theme==='dark'; $('#theme-toggle span').textContent=dark?'☾':'☀'; $('#theme-toggle').title=dark?'Use light theme':'Use dark theme'; }
const request = async (url, options = {}) => { const r = await fetch(url, { headers: { 'Content-Type':'application/json' }, ...options }); if (!r.ok) throw new Error((await r.json()).error); return r.json(); };
function toast(message){ const el=$('#toast'); el.textContent=message; el.classList.add('show'); setTimeout(()=>el.classList.remove('show'),1800); }
function render(){
  const done=state.tasks.filter(t=>t.completed).length, total=state.tasks.length;
  $('#done-stat').textContent=`${done} / ${total}`; $('#task-progress').style.width=`${total ? done/total*100:0}%`; $('#task-count').textContent=`${total} item${total===1?'':'s'}`;
  $('#focus-stat').textContent=state.focusMinutes>=60?`${Math.floor(state.focusMinutes/60)}h ${state.focusMinutes%60}m`:`${state.focusMinutes}m`;
  $('#session-stat').textContent=state.focusSessions?`${state.focusSessions} session${state.focusSessions===1?'':'s'} completed`:'No sessions yet'; $('#focus-total').textContent=state.focusSessions;
  $('#task-list').innerHTML=state.tasks.map(t=>`<li class="task ${t.completed?'done':''}" data-id="${t.id}"><button class="check" aria-label="Toggle task"></button><span class="task-title"></span><button class="delete" aria-label="Delete task">×</button></li>`).join('');
  state.tasks.forEach((t,i)=>$$('.task-title')[i].textContent=t.title); $('#task-empty').style.display=total?'none':'block';
}
async function load(){ state=await request('/api/state'); $('#notes').value=state.notes; updateChars(); render(); }
const now=new Date(), hour=now.getHours(); $('#greeting').textContent=`Good ${hour<12?'morning':hour<18?'afternoon':'evening'}.`; $('#date').textContent=now.toLocaleDateString(undefined,{weekday:'short',month:'short',day:'numeric'}).toUpperCase(); $('#day-stat').textContent=now.getDate(); $('#month-stat').textContent=now.toLocaleDateString(undefined,{month:'long',year:'numeric'});
$$('.nav').forEach(btn=>btn.onclick=()=>{ $$('.nav,.view').forEach(x=>x.classList.remove('active')); btn.classList.add('active'); $(`#${btn.dataset.view}-view`).classList.add('active'); $('#eyebrow').textContent=btn.dataset.view==='today'?'YOUR DAILY SPACE':btn.dataset.view==='focus'?'DEEP WORK':'HANDY UTILITIES'; });
$('#task-form').onsubmit=async e=>{ e.preventDefault(); const input=$('#task-input'); if(!input.value.trim())return; try{ const task=await request('/api/tasks',{method:'POST',body:JSON.stringify({title:input.value})}); state.tasks.unshift(task); input.value=''; render(); }catch(e){toast(e.message)} };
$('#task-list').onclick=async e=>{ const row=e.target.closest('.task'); if(!row)return; const task=state.tasks.find(t=>t.id===row.dataset.id); if(e.target.closest('.check')){ const updated=await request(`/api/tasks/${task.id}`,{method:'PATCH',body:JSON.stringify({completed:!task.completed})}); Object.assign(task,updated); render(); } if(e.target.closest('.delete')){ await request(`/api/tasks/${task.id}`,{method:'DELETE'}); state.tasks=state.tasks.filter(t=>t.id!==task.id); render(); } };
function updateChars(){ $('#char-count').textContent=`${$('#notes').value.length.toLocaleString()} characters`; }
$('#theme-toggle').onclick=()=>{ const next=document.documentElement.dataset.theme==='dark'?'light':'dark'; document.documentElement.dataset.theme=next; localStorage.setItem('trestrio-theme',next); syncTheme(); };
$('#notes').oninput=()=>{ updateChars(); $('#save-status').textContent='Saving…'; clearTimeout(saveTimer); saveTimer=setTimeout(async()=>{ await request('/api/notes',{method:'PUT',body:JSON.stringify({notes:$('#notes').value})}); $('#save-status').textContent='Saved'; },550); };
$('#clear-note').onclick=()=>{ $('#notes').value=''; $('#notes').dispatchEvent(new Event('input')); };
function drawTimer(){ const m=Math.floor(seconds/60),s=seconds%60; $('#timer').textContent=`${String(m).padStart(2,'0')}:${String(s).padStart(2,'0')}`; document.title=running?`${$('#timer').textContent} — Trestrio`:'Trestrio — Your daily utility'; }
$$('[data-minutes]').forEach(b=>b.onclick=()=>{ if(running)return; selectedMinutes=Number(b.dataset.minutes); seconds=selectedMinutes*60; $$('[data-minutes]').forEach(x=>x.classList.toggle('selected',x===b)); drawTimer(); });
$('#timer-toggle').onclick=()=>{ running=!running; $('#timer-toggle').textContent=running?'Pause':'Resume focus'; clearInterval(timerId); if(running)timerId=setInterval(async()=>{ seconds--; drawTimer(); if(seconds<=0){ clearInterval(timerId); running=false; state={...state,...await request('/api/focus',{method:'POST',body:JSON.stringify({minutes:selectedMinutes})})}; render(); toast('Focus session complete'); seconds=selectedMinutes*60; $('#timer-toggle').textContent='Start focus'; drawTimer(); } },1000); };
$('#timer-reset').onclick=()=>{clearInterval(timerId);running=false;seconds=selectedMinutes*60;$('#timer-toggle').textContent='Start focus';drawTimer()};
$$('[data-case]').forEach(b=>b.onclick=()=>{ const el=$('#case-input'),type=b.dataset.case; el.value=type==='upper'?el.value.toUpperCase():type==='lower'?el.value.toLowerCase():el.value.toLowerCase().replace(/\b\w/g,c=>c.toUpperCase()); });
function percent(){ $('#percent-result').textContent=((Number($('#percent').value)||0)*(Number($('#percent-of').value)||0)/100).toLocaleString(undefined,{maximumFractionDigits:4}); } $('#percent').oninput=$('#percent-of').oninput=percent;
setInterval(()=>$('#timestamp').textContent=Math.floor(Date.now()/1000),1000); $('#timestamp').textContent=Math.floor(Date.now()/1000); $('#copy-time').onclick=async()=>{await navigator.clipboard.writeText($('#timestamp').textContent);toast('Timestamp copied')};

const utilities=window.TRESTRIO_UTILITIES||[], utilityCategories=['All',...new Set(utilities.map(x=>x.category))]; let utilityCategory='All';
function utilityInputs(tool){
  if(tool.type==='generate')return '<button class="utility-run">Generate</button>';
  if(tool.type==='two')return `<div class="utility-pair"><label>${tool.labelA}<input type="number" value="10" data-a></label><label>${tool.labelB}<input type="number" value="20" data-b></label></div>`;
  if(tool.type==='two-date')return '<div class="utility-pair"><label>Start<input type="date" data-a></label><label>End<input type="date" data-b></label></div>';
  if(tool.type==='number')return '<input class="utility-input" type="number" value="10" data-a>';
  if(tool.type==='date')return '<input class="utility-input" type="date" data-a>';
  return `<textarea class="utility-input" data-a placeholder="${tool.type==='text-button'?'One item per line…':'Enter text…'}"></textarea>${tool.type==='text-button'?'<button class="utility-run">Pick one</button>':''}`;
}
function renderUtilities(){
  const query=$('#utility-search').value.trim().toLowerCase(), shown=utilities.filter(x=>(utilityCategory==='All'||x.category===utilityCategory)&&(`${x.name} ${x.description} ${x.category}`).toLowerCase().includes(query));
  $('#utility-library').innerHTML=shown.map(tool=>`<article class="panel utility" data-tool="${tool.id}"><button class="utility-head" aria-expanded="false"><span class="utility-icon">${tool.name.charAt(0)}</span><span><small>${tool.category}</small><b>${tool.name}</b><em>${tool.description}</em></span><i>+</i></button><div class="utility-body">${utilityInputs(tool)}<div class="utility-output"><span>RESULT</span><button class="utility-copy" title="Copy result">Copy</button><output>—</output></div></div></article>`).join('');
  $('#utility-count').textContent=`${shown.length} tool${shown.length===1?'':'s'}`; $('#utility-empty').style.display=shown.length?'none':'grid';
}
$('#utility-filters').innerHTML=utilityCategories.map((x,i)=>`<button class="${i?'':'active'}" data-category="${x}">${x}</button>`).join('');
$('#utility-filters').onclick=e=>{const b=e.target.closest('[data-category]');if(!b)return;utilityCategory=b.dataset.category;$$('#utility-filters button').forEach(x=>x.classList.toggle('active',x===b));renderUtilities()};
$('#utility-search').oninput=renderUtilities;
$('#utility-library').onclick=async e=>{
  const card=e.target.closest('.utility');if(!card)return;const tool=utilities.find(x=>x.id===card.dataset.tool),head=e.target.closest('.utility-head');
  if(head){const open=card.classList.toggle('open');head.setAttribute('aria-expanded',String(open));head.querySelector('i').textContent=open?'−':'+';if(open){const input=card.querySelector('[data-a]');if(input&&input.type==='date'&&!input.value)input.value=new Date().toISOString().slice(0,10)}return}
  const output=card.querySelector('output');
  if(e.target.closest('.utility-copy')){if(output.textContent!=='—'){await navigator.clipboard.writeText(output.textContent);toast('Result copied')}return}
  if(e.target.closest('.utility-run'))runUtility(card,tool);
};
$('#utility-library').oninput=e=>{const card=e.target.closest('.utility');if(card)runUtility(card,utilities.find(x=>x.id===card.dataset.tool))};
function runUtility(card,tool){const a=card.querySelector('[data-a]')?.value??'',b=card.querySelector('[data-b]')?.value??'',numeric=tool.type==='number'||tool.type==='two';let result=tool.type==='generate'?tool.run():tool.type==='two'||tool.type==='two-date'?tool.run(numeric?Number(a):a,numeric?Number(b):b):tool.run(numeric?Number(a):a);card.querySelector('output').textContent=String(result??'')}
renderUtilities();
syncTheme(); load().catch(()=>toast('Could not load your workspace'));
