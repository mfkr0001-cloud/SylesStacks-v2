'use strict';
const STORAGE_KEY='sylesstacks_v23_data';
const LEGACY_STORAGE_KEYS=['sylesstacks_v22_data','sylesstacks_v21_data'];
let storageLastError='';
const FPS_OPTIONS=['60','90','120','240','360','480','Unlimited'];
const POWER_MODES=[{id:'saving',name:'Экономия',short:'ЭКО',desc:'Приоритет автономности'},{id:'balanced',name:'Баланс',short:'BAL',desc:'Универсальный режим'},{id:'performance',name:'Производительный',short:'MAX',desc:'Приоритет отклика'}];
const DEFAULT_GAME={id:'standoff2',name:'Standoff 2',packageId:'com.axlebolt.standoff2'};
const DEFAULT_DATA={
 games:[DEFAULT_GAME],selectedFPS:'90',powerMode:'balanced',
 crosshair:{shape:'plus',size:34,color:'#62ddff',opacity:100},
 crossProfiles:[],gameProfiles:{},
 settings:{loading:true,animations:true,edgePanel:true,notifications:true,showProfile:true}
};
let data=loadData();
function clone(obj){return JSON.parse(JSON.stringify(obj));}
function normalizeData(parsed){
 const merged={...clone(DEFAULT_DATA),...(parsed||{})};
 merged.settings={...DEFAULT_DATA.settings,...((parsed&&parsed.settings)||{})};
 merged.games=Array.isArray(merged.games)?merged.games:clone(DEFAULT_DATA.games);
 merged.crossProfiles=Array.isArray(merged.crossProfiles)?merged.crossProfiles:[];
 merged.gameProfiles=merged.gameProfiles&&typeof merged.gameProfiles==='object'?merged.gameProfiles:{};
 merged.crosshair={...DEFAULT_DATA.crosshair,...(merged.crosshair||{})};
 if(!FPS_OPTIONS.includes(String(merged.selectedFPS)))merged.selectedFPS='90';
 if(!POWER_MODES.some(m=>m.id===merged.powerMode))merged.powerMode='balanced';
 return merged;
}
function loadData(){
 try{
  let raw=localStorage.getItem(STORAGE_KEY);
  if(!raw){for(const key of LEGACY_STORAGE_KEYS){raw=localStorage.getItem(key);if(raw)break;}}
  if(!raw)return clone(DEFAULT_DATA);
  const parsed=JSON.parse(raw);const restored=normalizeData(parsed);
  // Migrate old v2.1/v2.2 data to the current, versioned key.
  localStorage.setItem(STORAGE_KEY,JSON.stringify(restored));
  return restored;
 }catch(e){storageLastError=String(e&&e.message||e);return clone(DEFAULT_DATA);}
}
function saveData(silent=false){
 try{
  const payload=JSON.stringify(data);
  localStorage.setItem(STORAGE_KEY,payload);
  if(localStorage.getItem(STORAGE_KEY)!==payload)throw new Error('Проверка записи не пройдена');
  storageLastError='';return true;
 }catch(e){storageLastError=String(e&&e.message||e);if(!silent)toast('Не удалось сохранить настройки. Проверь доступ к хранилищу браузера.');return false;}
}
function esc(value){return String(value??'').replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));}
function toast(msg){if(!data.settings.notifications)return;const t=document.getElementById('toast');t.textContent=msg;t.style.display='block';clearTimeout(window._toastTimer);window._toastTimer=setTimeout(()=>t.style.display='none',2600);}
function go(id){document.querySelectorAll('.page').forEach(p=>p.classList.toggle('active',p.id===id));document.querySelectorAll('[data-go]').forEach(b=>b.classList.toggle('active',b.dataset.go===id));if(data.settings.animations)window.scrollTo({top:0,behavior:'smooth'});else window.scrollTo(0,0);if(id==='gameprofiles')renderGameProfiles();}
document.querySelectorAll('[data-go]').forEach(b=>b.addEventListener('click',()=>go(b.dataset.go)));
function drawer(){document.getElementById('drawer').classList.toggle('open');document.getElementById('shade').classList.toggle('show');}
function setFPS(value){data.selectedFPS=FPS_OPTIONS.includes(value)?value:'90';saveData();renderFPS();updateOverview();toast('Целевой профиль: '+(data.selectedFPS==='Unlimited'?'без лимита':data.selectedFPS+' FPS')+'. Это не меняет FPS игры.');}
function renderFPS(){const select=document.getElementById('fpsSelect');if(select)select.value=data.selectedFPS;const root=document.getElementById('powerModes');if(root){root.innerHTML='';POWER_MODES.forEach(mode=>{const b=document.createElement('button');b.className='fps'+(data.powerMode===mode.id?' selected':'');b.innerHTML=esc(mode.short)+'<small>'+esc(mode.name)+'</small>';b.title=mode.desc;b.addEventListener('click',()=>{data.powerMode=mode.id;saveData();renderFPS();toast('Выбрано: '+mode.name+'. Настройка сохранена как предпочтение.');});root.appendChild(b);});}}
function updateOverview(){document.getElementById('homeFps').textContent=data.settings.showProfile?(data.selectedFPS==='Unlimited'?'∞':data.selectedFPS):'—';document.getElementById('gameCount').textContent=String(data.games.length);document.getElementById('libraryCount').textContent=data.games.length+' игр';}
function renderGames(){const root=document.getElementById('games');root.innerHTML='';if(!data.games.length){root.innerHTML='<div class="empty">Библиотека пока пуста. Добавь игру выше.</div>';}data.games.forEach(game=>{const row=document.createElement('div');row.className='listitem';const meta=document.createElement('div');meta.className='meta';meta.innerHTML='<b>'+esc(game.name)+'</b><small>'+esc(game.packageId||'Package ID не указан')+'</small>';const actions=document.createElement('div');actions.className='row';const launch=document.createElement('button');launch.className='secondary tiny';launch.textContent='Запуск';launch.title='Для запуска требуется нативная Android-оболочка';launch.addEventListener('click',()=>toast('Запуск '+game.name+' доступен только после подключения Android-моста.'));const del=document.createElement('button');del.className='secondary danger tiny';del.textContent='Удалить';del.addEventListener('click',()=>removeGame(game.id));actions.append(launch,del);row.append(meta,actions);root.append(row);});renderGameSelect();updateOverview();}
function addGame(){const name=document.getElementById('newgame').value.trim();const packageId=document.getElementById('newpackage').value.trim();if(!name)return toast('Введи название игры.');if(packageId&&!/^[A-Za-z0-9_]+(\.[A-Za-z0-9_]+)+$/.test(packageId))return toast('Проверь формат Package ID. Пример: com.example.game');if(data.games.some(g=>g.name.toLowerCase()===name.toLowerCase()||(packageId&&g.packageId===packageId)))return toast('Такая игра уже есть в библиотеке.');data.games.push({id:'game_'+Date.now(),name,packageId});saveData();document.getElementById('newgame').value='';document.getElementById('newpackage').value='';renderGames();toast('Игра добавлена и сохранена.');}
function addDefaultGame(){if(data.games.some(g=>g.packageId===DEFAULT_GAME.packageId))return toast('Standoff 2 уже есть в библиотеке.');data.games.push({...DEFAULT_GAME,id:'game_'+Date.now()});saveData();renderGames();toast('Standoff 2 добавлен.');}
function removeGame(id){const game=data.games.find(g=>g.id===id);if(!game)return;if(!confirm('Удалить «'+game.name+'» из библиотеки?'))return;data.games=data.games.filter(g=>g.id!==id);delete data.gameProfiles[id];saveData();renderGames();renderGameProfiles();toast('Игра удалена из списка.');}
function renderGameSelect(){const select=document.getElementById('profileGame');if(!select)return;const previous=select.value;select.innerHTML='';data.games.forEach(g=>{const o=document.createElement('option');o.value=g.id;o.textContent=g.name;select.append(o);});if(data.games.some(g=>g.id===previous))select.value=previous;}
function updateCross(){const shape=document.getElementById('shape').value,size=Number(document.getElementById('size').value),color=document.getElementById('color').value,opacity=Number(document.getElementById('opacity').value);const c=document.getElementById('cross');c.className='crosshair'+(shape==='plus'?'':' '+shape);c.style.transform='scale('+(size/34)+')';c.style.setProperty('--cross-color',color);c.style.setProperty('--cross-opacity',String(opacity/100));document.getElementById('sizeValue').textContent=size;document.getElementById('opacityValue').textContent=opacity;data.crosshair={shape,size,color,opacity};saveData();}
function applyCross(cross){const c={...DEFAULT_DATA.crosshair,...cross};document.getElementById('shape').value=c.shape;document.getElementById('size').value=c.size;document.getElementById('color').value=c.color;document.getElementById('opacity').value=c.opacity;updateCross();}
function resetCrossEditor(){applyCross(DEFAULT_DATA.crosshair);toast('Параметры редактора сброшены.');}
function saveCrossProfile(){const name=prompt('Название профиля прицела:');if(!name||!name.trim())return;data.crossProfiles.push({id:'cross_'+Date.now(),name:name.trim().slice(0,32),config:clone(data.crosshair)});saveData();renderCrossProfiles();toast('Профиль прицела сохранён.');}
function renderCrossProfiles(){const root=document.getElementById('crossProfiles');root.innerHTML='';if(!data.crossProfiles.length){root.innerHTML='<div class="empty">Пока нет сохранённых профилей.</div>';return;}data.crossProfiles.forEach(profile=>{const row=document.createElement('div');row.className='listitem';const meta=document.createElement('div');meta.className='meta';meta.innerHTML='<b>'+esc(profile.name)+'</b><small>'+esc(profile.config.shape)+' · '+profile.config.size+' px · '+profile.config.opacity+'%</small>';const actions=document.createElement('div');actions.className='row';const use=document.createElement('button');use.className='secondary tiny';use.textContent='Применить';use.addEventListener('click',()=>{applyCross(profile.config);toast('Профиль прицела применён.');});const del=document.createElement('button');del.className='secondary danger tiny';del.textContent='Удалить';del.addEventListener('click',()=>{data.crossProfiles=data.crossProfiles.filter(p=>p.id!==profile.id);saveData();renderCrossProfiles();});actions.append(use,del);row.append(meta,actions);root.append(row);});}
function saveGameProfile(){const id=document.getElementById('profileGame').value;if(!id)return toast('Сначала добавь игру в библиотеку.');data.gameProfiles[id]={fps:document.getElementById('profileFps').value,power:document.getElementById('profilePower').value,updated:new Date().toISOString()};saveData();renderGameProfiles();toast('Профиль игры сохранён.');}
function renderGameProfiles(){const root=document.getElementById('gameProfiles');if(!root)return;root.innerHTML='';const entries=Object.entries(data.gameProfiles);if(!entries.length){root.innerHTML='<div class="empty">Сохранённых игровых профилей пока нет.</div>';return;}entries.forEach(([id,p])=>{const game=data.games.find(g=>g.id===id);const row=document.createElement('div');row.className='listitem';const meta=document.createElement('div');meta.className='meta';meta.innerHTML='<b>'+esc(game?game.name:'Удалённая игра')+'</b><small>Цель: '+esc(p.fps)+' FPS · '+esc(POWER_MODES.find(m=>m.id===p.power)?.name||p.power)+'</small>';const del=document.createElement('button');del.className='secondary danger tiny';del.textContent='Удалить';del.addEventListener('click',()=>{delete data.gameProfiles[id];saveData();renderGameProfiles();});row.append(meta,del);root.append(row);});}
function refreshDeviceInfo(){const ua=navigator.userAgent||'Неизвестно';const platform=navigator.userAgentData?.platform||navigator.platform||'Не определено';const memory=navigator.deviceMemory?navigator.deviceMemory+' GB (оценка браузера)':'Недоступно';const cores=navigator.hardwareConcurrency?navigator.hardwareConcurrency+' логических потоков':'Недоступно';const screenInfo=screen.width+' × '+screen.height+' CSS px · DPR '+(window.devicePixelRatio||1);const connection=navigator.connection?.effectiveType||'Недоступно';const rows=[['Платформа браузера',platform],['User-Agent',ua],['Экран',screenInfo],['Оценка RAM',memory],['Логические потоки CPU',cores],['Тип сети',connection],['Язык',navigator.language||'Неизвестно'],['Часовой пояс',Intl.DateTimeFormat().resolvedOptions().timeZone||'Неизвестно']];const markup=rows.map(([k,v])=>'<div class="setting"><div><b>'+esc(k)+'</b><small>'+esc(v)+'</small></div></div>').join('');document.getElementById('deviceInfo').innerHTML=markup;document.getElementById('deviceDetails').innerHTML=markup;document.getElementById('storageStatus').textContent=storageAvailable()?(storageLastError?'Ошибка записи':'Доступно'):'Недоступно';document.getElementById('shareStatus').textContent=navigator.share?'Доступно':'Нет поддержки';}
function storageAvailable(){try{const k='__ss_test__';localStorage.setItem(k,'1');localStorage.removeItem(k);return true;}catch(e){return false;}}
function exportData(){const blob=new Blob([JSON.stringify({app:'SylesStacks',version:'2.3',exportedAt:new Date().toISOString(),data},null,2)],{type:'application/json'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download='SylesStacks_backup.json';document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);toast('Резервная копия JSON подготовлена.');}
function importData(event){const file=event.target.files?.[0];if(!file)return;const reader=new FileReader();reader.onload=()=>{try{const parsed=JSON.parse(String(reader.result));const incoming=parsed.data||parsed;if(!incoming||!Array.isArray(incoming.games)||!incoming.settings||!incoming.crosshair)throw new Error('Формат файла не распознан');if(!confirm('Импорт заменит текущие настройки. Продолжить?'))return;data={...clone(DEFAULT_DATA),...incoming,settings:{...DEFAULT_DATA.settings,...incoming.settings},games:incoming.games,crossProfiles:Array.isArray(incoming.crossProfiles)?incoming.crossProfiles:[],gameProfiles:incoming.gameProfiles||{}};saveData();refreshAll();toast('Данные успешно импортированы.');}catch(err){toast('Ошибка импорта: '+err.message);}};reader.onerror=()=>toast('Не удалось прочитать файл.');reader.readAsText(file);event.target.value='';}
function resetAll(){if(!confirm('Удалить все локальные игры, профили и настройки? Рекомендуется сначала сделать экспорт JSON.'))return;data=clone(DEFAULT_DATA);saveData();refreshAll();toast('Настройки сброшены.');}
function refreshAll(){renderGames();renderFPS();applyCross(data.crosshair);renderCrossProfiles();renderGameProfiles();refreshDeviceInfo();syncSwitches();updateOverview();}
function syncSwitches(){document.querySelectorAll('[data-setting]').forEach(btn=>{const key=btn.dataset.setting;const enabled=!!data.settings[key];btn.classList.toggle('on',enabled);btn.setAttribute('aria-pressed',String(enabled));btn.setAttribute('role','switch');});document.getElementById('edgeButton').style.display=data.settings.edgePanel?'block':'none';}
document.querySelectorAll('[data-setting]').forEach(btn=>btn.addEventListener('click',()=>{const key=btn.dataset.setting;if(!(key in data.settings))return;const previous=!!data.settings[key];data.settings[key]=!previous;if(!saveData()){data.settings[key]=previous;syncSwitches();return;}syncSwitches();updateOverview();if(key==='notifications'&&data.settings[key])toast('Уведомления включены.');}));
document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='hidden')saveData(true);});
window.addEventListener('pagehide',()=>saveData(true));
window.addEventListener('storage',event=>{if(event.key===STORAGE_KEY&&event.newValue){try{data=normalizeData(JSON.parse(event.newValue));refreshAll();}catch(e){}}});
document.getElementById('shape').addEventListener('change',updateCross);
function openStore(){go('services');}
function toggleFullscreen(){if(!document.fullscreenElement){document.documentElement.requestFullscreen?.().catch(()=>toast('Полный экран недоступен в этом окне.'));}else{document.exitFullscreen?.();}}
function updateLauncherClock(){const el=document.getElementById('launcherClock');if(el)el.textContent=new Intl.DateTimeFormat('ru-RU',{hour:'2-digit',minute:'2-digit'}).format(new Date());}
function boot(){refreshAll();if(storageLastError)toast('Хранилище недоступно: изменения могут не сохраниться.');updateLauncherClock();setInterval(updateLauncherClock,15000);let p=0;const loading=document.getElementById('loading');if(!data.settings.loading){loading.classList.add('hide');return;}const timer=setInterval(()=>{p=Math.min(100,p+12);document.getElementById('barfill').style.width=p+'%';document.getElementById('loadlabel').textContent='Запуск интерфейса · '+p+'%';if(p>=100){clearInterval(timer);setTimeout(()=>loading.classList.add('hide'),220);}},65);}
boot();
