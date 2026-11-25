(function(){
const guidePath=()=>location.pathname.replace(/\/$/,'')==='/guide';
const loginPath=()=>location.pathname.replace(/\/$/,'')==='/login';
if(!guidePath()&&!loginPath()){location.replace('/guide');return;}
if(!guidePath())return;
if(window.__guideClientV2){window.__guideClientV2.refresh();return;}
const tile='.guide-program-container,.channel-logo', outer='.guide-list-outer', row='[data-testid="guide-row"]';
const controls='button:not([disabled]),a[href],input:not([disabled]),select:not([disabled]),[role="button"],[role="option"],[tabindex]:not([tabindex="-1"])';
let last=null, anchor=null, busy=false, modalWas=false, scheduled=false, currentGuide=null;
const style=document.createElement('style');style.textContent=`
.mantine-AppShell-navbar{display:none!important}
.mantine-AppShell-main{--app-shell-navbar-offset:0px!important;padding-inline-start:0!important;margin-inline-start:0!important;width:100%!important;max-width:none!important}
.tv-guide .guide-program-container:focus,.tv-guide .channel-logo:focus{outline:3px solid #55e8d0!important;outline-offset:-3px!important;z-index:40!important;box-shadow:inset 0 0 0 2px #111!important}
.tv-guide button:focus,.tv-guide input:focus,[role="dialog"] button:focus{outline:2px solid #55e8d0!important;outline-offset:2px}
`;document.head.appendChild(style);
function visible(n){if(!n||!n.isConnected||!n.getClientRects().length)return false;const s=getComputedStyle(n);return s.visibility!=='hidden'&&s.display!=='none'&&s.pointerEvents!=='none';}
function dialog(){return Array.from(document.querySelectorAll('[role="dialog"],[aria-modal="true"]')).filter(visible).pop();}
function grid(){return document.querySelector(outer);}
function rect(n){return n.getBoundingClientRect();}
function rows(){return Array.from(document.querySelectorAll(outer+' '+row)).filter(visible).sort((a,b)=>rect(a).top-rect(b).top);}
function tiles(r){return Array.from(r.querySelectorAll(tile)).filter(visible).sort((a,b)=>rect(a).left-rect(b).left);}
function onScreen(n){const g=grid();if(!g)return false;const a=rect(n),b=rect(g);return a.right>b.left&&a.left<b.right&&a.bottom>b.top&&a.top<b.bottom;}
function key(n){const r=n.closest(row);return {channel:r?.querySelector('.channel-logo img')?.alt||r?.querySelector('.channel-logo')?.textContent?.trim()||'',top:r?.style.top||'',left:n.style.left||'',logo:n.matches('.channel-logo'),text:n.textContent.trim().slice(0,160)};}
function focus(n,scroll=true){if(!visible(n))return false;if(n.matches(tile)){n.tabIndex=0;last=key(n);}n.focus({preventScroll:true});if(scroll)n.scrollIntoView({block:'nearest',inline:'nearest',behavior:'instant'});return document.activeElement===n;}
function initial(){const all=Array.from(document.querySelectorAll(outer+' .guide-program-container')).filter(onScreen);return all.find(n=>n.querySelector('.guide-program.live'))||all[0]||Array.from(document.querySelectorAll(outer+' .channel-logo')).find(onScreen);}
function restore(){if(last){const all=Array.from(document.querySelectorAll(outer+' '+row+' '+tile.replace(',',','+outer+' '+row+' ')));const hit=all.find(n=>{const k=key(n);return k.logo===last.logo&&k.channel===last.channel&&k.top===last.top&&k.left===last.left&&k.text===last.text;});if(hit&&focus(hit))return true;}return focus(initial());}
function candidates(scope){return Array.from(scope.querySelectorAll(controls)).filter(n=>visible(n)&&!n.closest('.mantine-AppShell-navbar')&&!n.disabled&&(!n.matches(tile)||onScreen(n)));}
function spatial(a,items,d){const r=rect(a),x=r.left+r.width/2,y=r.top+r.height/2;let best=null,score=Infinity;for(const n of items){if(n===a)continue;const q=rect(n),dx=q.left+q.width/2-x,dy=q.top+q.height/2-y,p=d==='ArrowRight'?dx:d==='ArrowLeft'?-dx:d==='ArrowDown'?dy:-dy,c=d==='ArrowRight'||d==='ArrowLeft'?dy:dx;if(p>2&&p+Math.abs(c)*3<score){best=n;score=p+Math.abs(c)*3;}}return best;}
function refresh(){if(!guidePath())return;const g=grid();if(g!==currentGuide){currentGuide=g;anchor=null;last=null;}document.querySelectorAll('.tv-guide '+tile.replace(',',',.tv-guide ')).forEach(n=>{if(n.tabIndex!==0)n.tabIndex=0;});const m=dialog();if(m){if(!modalWas){modalWas=true;requestAnimationFrame(()=>{if(!m.contains(document.activeElement))focus(candidates(m)[0],false);});}return;}if(modalWas){modalWas=false;restore();return;}if(g&&!busy&&(!document.activeElement||document.activeElement===document.body||document.activeElement===document.documentElement||document.activeElement.closest('.mantine-AppShell-navbar')))restore();}
function chooseAt(r,x,logo){const ts=tiles(r);if(logo)return ts.find(n=>n.matches('.channel-logo'))||ts[0];const ps=ts.filter(n=>!n.matches('.channel-logo'));return ps.find(n=>{const q=rect(n);return x>=q.left&&x<=q.right;})||ps.sort((a,b)=>Math.abs(rect(a).left+rect(a).width/2-x)-Math.abs(rect(b).left+rect(b).width/2-x))[0]||ts[0];}
function move(d){refresh();const m=dialog(),a=document.activeElement;if(m){if(!m.contains(a)){focus(candidates(m)[0]);return true;}if(['INPUT','TEXTAREA','SELECT'].includes(a.tagName))return false;focus(spatial(a,candidates(m),d));return true;}
if(a?.matches('[role="combobox"][aria-expanded="true"]')||a?.matches('input:not([readonly]),textarea,select')){if(d==='ArrowDown'&&a.matches('input:not([readonly])')&&a.getAttribute('aria-expanded')!=='true'){focus(initial());return true;}return false;}
if(!a?.matches(tile)){if(d==='ArrowDown'&&initial()){anchor=null;focus(initial());return true;}const g=document.querySelector('.tv-guide');return g?focus(spatial(a,candidates(g),d)):false;}
const r=a.closest(row),g=grid(),rs=rows(),i=rs.indexOf(r),q=rect(a);
if(!r||!g)return true;
if(d==='ArrowLeft'||d==='ArrowRight'){anchor=null;const ts=tiles(r),k=ts.indexOf(a),n=ts[k+(d==='ArrowRight'?1:-1)];if(n){focus(n);return true;}const before=g.scrollLeft;g.scrollLeft+=d==='ArrowRight'?Math.max(100,g.clientWidth*.6):-Math.max(100,g.clientWidth*.6);if(before!==g.scrollLeft){busy=true;setTimeout(()=>{busy=false;const cur=tiles(r),idx=cur.indexOf(a);focus(cur[idx+(d==='ArrowRight'?1:-1)]||a);},200);}return true;}
if(anchor===null){const b=rect(g);anchor=Math.max(b.left+2,Math.min(b.right-2,q.left+q.width/2));}
const next=rs[i+(d==='ArrowDown'?1:-1)];
if(next){focus(chooseAt(next,anchor,a.matches('.channel-logo')));return true;}
if(d==='ArrowUp'&&g.scrollTop<=1){anchor=null;const header=candidates(document.querySelector('.tv-guide')).filter(n=>!n.closest(outer));focus(spatial(a,header,d)||header[0]);return true;}
const before=g.scrollTop;g.scrollTop+=d==='ArrowDown'?q.height:-q.height;if(before!==g.scrollTop){busy=true;setTimeout(()=>{busy=false;const target=rows().filter(x=>onScreen(x));focus(chooseAt(d==='ArrowDown'?target[target.length-1]:target[0],anchor,a.matches('.channel-logo')));},180);}return true;}
function action(k){if(!guidePath()||document.fullscreenElement)return false;if(k==='Enter'){const a=document.activeElement;if(a?.matches(tile)){a.click();return true;}return false;}return move(k);}
document.addEventListener('keydown',e=>{if(['ArrowLeft','ArrowRight','ArrowUp','ArrowDown','Enter'].includes(e.key)&&action(e.key)){e.preventDefault();e.stopImmediatePropagation();}},true);
document.addEventListener('focusin',e=>{if(e.target.matches(tile))last=key(e.target);},true);
document.addEventListener('playing',e=>{const v=e.target;if(v.tagName==='VIDEO'&&guidePath()&&!document.fullscreenElement&&v.requestFullscreen){try{v.requestFullscreen().catch(()=>{});}catch(err){}}},true);
function back(){const m=dialog();if(!m)return false;const close=m.querySelector('button[aria-label*="close" i],.mantine-Modal-close,.mantine-Drawer-close');if(close){close.click();return true;}document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',code:'Escape',bubbles:true}));return true;}
const obs=new MutationObserver(()=>{if(!scheduled){scheduled=true;requestAnimationFrame(()=>{scheduled=false;refresh();});}});obs.observe(document.body,{childList:true,subtree:true});
window.__guideClientV2={refresh,restore,back,action};refresh();
})();