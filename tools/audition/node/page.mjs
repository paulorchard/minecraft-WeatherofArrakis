// Writes ../index.html from ../results.json (run after build.mjs).
import fs from 'fs';
import path from 'path';
const OUT = path.resolve(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1')), '..');
const data = JSON.parse(fs.readFileSync(path.join(OUT, 'results.json'), 'utf8'));
const html = `<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Wind sound audition</title>
<style>
:root{--bg:#14110d;--fg:#eadfce;--mut:#a39682;--card:#1f1a14;--line:#3a3126;--acc:#e0a050;--bad:#e07060;--ok:#8fc080}
@media (prefers-color-scheme: light){:root{--bg:#faf6ef;--fg:#2a2118;--mut:#6f6250;--card:#fff;--line:#ddd2c0;--acc:#a8641a;--bad:#b3392a;--ok:#3f7a30}}
body{background:var(--bg);color:var(--fg);font:14px/1.4 system-ui,sans-serif;margin:0;padding:16px}
h1{font-size:20px;margin:0 0 4px} p{margin:4px 0 10px;color:var(--mut)}
table{border-collapse:collapse;width:100%;background:var(--card)} th,td{border:1px solid var(--line);padding:4px 6px;text-align:left;vertical-align:middle}
th{position:sticky;top:0;background:var(--card);cursor:pointer;font-size:12px} td.n{text-align:right;font-variant-numeric:tabular-nums}
.v{color:var(--ok)} .b{color:var(--bad)} .g{color:var(--acc)} small{color:var(--mut)}
input[type=text]{width:180px;background:transparent;color:var(--fg);border:1px solid var(--line);padding:2px 4px}
audio{height:28px;width:230px} .bar{display:flex;gap:12px;flex-wrap:wrap;align-items:center;margin:8px 0}
select{max-width:240px;background:var(--card);color:var(--fg)} .wrap{overflow-x:auto}
</style></head><body>
<h1>Wind sound audition (Weather of Arrakis)</h1>
<p>Open this file straight from disk. Each row has a player, a loop box, a pitch slider (0.5 to 1.5) and a second layer to play on top. Rank columns: <b>Bed</b> = suitability as a steady loopable bed, <b>Gust</b> = suitability as a one-shot gust (0 to 100, from the numbers; the score cannot hear). Click a header to sort. Notes are saved in this browser.</p>
<p><span class="v">Vanilla</span> rows reference a sound event id (use it with <code>/storm_sound minecraft:...</code>); their audio files are extracted into <code>tools/audition/vanilla/</code> for this page only (gitignored; run <code>node build.mjs</code> in <code>tools/audition/node</code> to recreate). <span class="b">Pixabay</span> rows are the upstream files and a release blocker. <span class="g">Generated</span> rows are ours and free of third-party terms.</p>
<div class="bar"><label>Show <select id="grp"><option value="">all groups</option></select></label><label>Sort by <select id="sort"><option value="bedScore">Bed score</option><option value="gustScore">Gust score</option><option value="duration">Duration</option><option value="centroidHz">Centroid</option><option value="rmsDb">RMS</option></select></label><button id="stop">Stop all</button></div>
<div class="wrap"><table id="t"><thead><tr><th>Candidate</th><th>Play</th><th>Loop</th><th>Pitch</th><th>Layer with</th><th>Licence</th><th>Bed</th><th>Gust</th><th>Dur s</th><th>Hz / ch</th><th>Peak dB</th><th>RMS dB</th><th>Centroid Hz</th><th>Steady (std dB)</th><th>Seam dB / jump</th><th>Your note</th></tr></thead><tbody></tbody></table></div>
<script>
const DATA=${JSON.stringify(data)};
const tb=document.querySelector('#t tbody'),grp=document.querySelector('#grp'),srt=document.querySelector('#sort');
const players=new Map();
[...new Set(DATA.map(d=>d.group))].forEach(g=>{const o=document.createElement('option');o.value=g;o.textContent=g;grp.appendChild(o)});
const cls=d=>d.group.startsWith('Vanilla')?'v':d.group.startsWith('Upstream')?'b':'g';
function layerOptions(self){return '<option value="">none</option>'+DATA.filter(d=>d.id!==self).map(d=>'<option value="'+d.id+'">'+d.label.replace(/</g,'&lt;')+'</option>').join('')}
function draw(){
  tb.innerHTML='';
  const k=srt.value,rows=DATA.filter(d=>!grp.value||d.group===grp.value).sort((a,b)=>k==='duration'||k==='centroidHz'||k==='rmsDb'?b[k]-a[k]:b[k]-a[k]);
  for(const d of rows){
    const tr=document.createElement('tr');
    const note=(()=>{try{return localStorage.getItem('note:'+d.id)||''}catch(e){return ''}})();
    tr.innerHTML='<td class="'+cls(d)+'"><b>'+d.label.replace(/</g,'&lt;')+'</b>'+(d.event?'<br><small>'+d.event+'</small>':'')+'</td>'
     +'<td><audio controls preload="none" src="'+d.file+'"></audio></td>'
     +'<td><input type="checkbox" class="lp"></td>'
     +'<td><input type="range" class="pi" min="0.5" max="1.5" step="0.05" value="1"> <span class="pv">1.00</span></td>'
     +'<td><select class="ly">'+layerOptions(d.id)+'</select></td>'
     +'<td class="'+cls(d)+'"><small>'+d.licence+'</small></td>'
     +'<td class="n">'+d.bedScore+'</td><td class="n">'+d.gustScore+'</td><td class="n">'+d.duration+'</td><td class="n">'+d.sampleRate+' / '+d.channels+'</td><td class="n">'+d.peakDb+'</td><td class="n">'+d.rmsDb+'</td><td class="n">'+d.centroidHz+'</td><td class="n">'+d.steadyStdDb+'</td><td class="n">'+d.seamLevelDb+' / '+d.seamJump+'</td>'
     +'<td><input type="text" class="nt" placeholder="note"></td>';
    const a=tr.querySelector('audio'),lp=tr.querySelector('.lp'),pi=tr.querySelector('.pi'),pv=tr.querySelector('.pv'),ly=tr.querySelector('.ly'),nt=tr.querySelector('.nt');
    nt.value=note;nt.oninput=()=>{try{localStorage.setItem('note:'+d.id,nt.value)}catch(e){}};
    let second=null;
    const apply=()=>{a.loop=lp.checked;a.preservesPitch=false;a.playbackRate=+pi.value;pv.textContent=(+pi.value).toFixed(2);if(second){second.loop=lp.checked;second.preservesPitch=false;second.playbackRate=+pi.value}};
    lp.onchange=pi.oninput=apply;
    a.onplay=()=>{apply();if(ly.value){const o=DATA.find(x=>x.id===ly.value);second=new Audio(o.file);apply();second.play().catch(()=>{});}};
    a.onpause=()=>{if(second){second.pause();second=null}};
    players.set(d.id,a);tb.appendChild(tr);
  }
}
document.querySelector('#stop').onclick=()=>{document.querySelectorAll('audio').forEach(a=>a.pause())};
grp.onchange=srt.onchange=draw;draw();
</script></body></html>`;
fs.writeFileSync(path.join(OUT, 'index.html'), html);
console.log('index.html', html.length);
