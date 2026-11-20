(() => {
  const client = supabase.createClient('https://xiuxxhuvqivkfpxtuatr.supabase.co','sb_publishable_faJEThyAuUZjHTKfL_587Q_NEOk18vY');
  let owner = null, loading = true, generation = 0, writeQueue = Promise.resolve(), failed = false;
  const main = document.querySelector("main"); main.inert = true; papers = []; render();
  const button = document.createElement('button');
  button.textContent = '登录 / 注册'; button.className = 'secondary';
  button.type = 'button'; button.style.marginLeft = 'auto';
  document.querySelector('header').append(button);
  const box = document.createElement('dialog'); box.id='accountDialog';
  box.innerHTML = '<form method="dialog"><button aria-label="关闭" style="float:right">×</button></form><h2>邮箱登录 / 注册</h2><p>两个网站共用账号。登录后可同步文献列表。</p><form id="emailForm"><label>邮箱地址 <input type="email" id="accountEmail" required autocomplete="email"></label><button type="submit">发送验证码</button></form><form id="codeForm" hidden><label>邮箱验证码 <input id="accountCode" required inputmode="numeric" autocomplete="one-time-code"></label><button type="submit">登录</button></form><p role="status" id="accountMessage"></p><button type="button" id="retrySync" hidden>重新同步文献</button><button type="button" id="signOut" hidden>退出登录</button>';
  document.body.append(box);
  const email = box.querySelector('#accountEmail'), code = box.querySelector('#accountCode'), status = box.querySelector('#accountMessage');
  button.onclick = () => { box.showModal(); };
  box.querySelector('#emailForm').onsubmit = async e => {e.preventDefault(); status.textContent='正在发送…'; const result=await client.auth.signInWithOtp({email:email.value.trim()}); status.textContent=result.error?'发送失败，请检查邮箱或稍后重试。':'验证码已发送，请检查邮箱。'; if(!result.error)box.querySelector('#codeForm').hidden=false;};
  box.querySelector('#codeForm').onsubmit = async e => {e.preventDefault(); status.textContent='正在登录…'; const result=await client.auth.verifyOtp({email:email.value.trim(),token:code.value.trim(),type:'email'}); status.textContent=result.error?'验证码无效或已过期。':'登录成功。';if(!result.error){code.value='';box.close();}};
  box.querySelector('#retrySync').onclick=()=>{window.citationAccountSave(papers);};
  box.querySelector('#signOut').onclick=async()=>{await writeQueue;if(failed){status.textContent='文献同步尚未成功，请先恢复网络后重新保存文献。';return;}await client.auth.signOut();box.close();};
  window.citationAccountSave = list => {
    if (loading) {status.textContent='账户数据正在读取，请稍候再修改。'; return true;}
    if (!owner) return false;
    const id=owner, data=JSON.parse(JSON.stringify(list));try{localStorage.setItem('citation-unsynced-'+id,JSON.stringify(data))}catch{}
    writeQueue=writeQueue.then(async()=>{const {error}=await client.from('literature_saved_items').upsert({owner_id:id,kind:'record',item_id:'citation-studio-papers',payload:{papers:data}},{onConflict:'owner_id,kind,item_id'});if(error){failed=true;button.textContent='同步失败';box.querySelector('#retrySync').hidden=false;status.textContent='文献同步失败，请检查网络后点击重新同步。';throw error;}failed=false;button.textContent='我的账户';box.querySelector('#retrySync').hidden=true;status.textContent='文献已同步。';try{if(localStorage.getItem('citation-unsynced-'+id)===JSON.stringify(data))localStorage.removeItem('citation-unsynced-'+id)}catch{}}).catch(()=>{});
    return true;
  };
  async function finishLoading(){render();button.disabled=true;try{await importWorkflowTransfer()}finally{button.disabled=false;loading=false;main.inert=false}}
  async function switchUser(user) {
    const g=++generation; loading=true; main.inert=true; owner=user?.id||null; button.textContent='正在读取账户…';
    if(!user){try{papers=JSON.parse(localStorage.getItem(stateKey)||'[]')}catch{papers=[]}button.textContent='登录 / 注册';box.querySelector('#signOut').hidden=true;box.querySelector('#emailForm').hidden=false;box.querySelector('#codeForm').hidden=true;loading=false;await finishLoading();return;}
    papers=[];render(); const {data,error}=await client.from('literature_saved_items').select('payload').eq('owner_id',user.id).eq('kind','record').eq('item_id','citation-studio-papers').maybeSingle();
    if(g!==generation)return;
    if(error){status.textContent='账户文献读取失败，请检查网络并重新登录。';button.textContent='账户读取失败';box.querySelector('#signOut').hidden=false;box.querySelector('#emailForm').hidden=true;return;}
    papers=Array.isArray(data?.payload?.papers)?data.payload.papers:[];try{const pending=localStorage.getItem('citation-unsynced-'+user.id);if(pending){papers=JSON.parse(pending);failed=true;box.querySelector('#retrySync').hidden=false;status.textContent='检测到未同步的文献，请检查网络并点击重新同步。';}}catch{} placementPaperIds=papers.map(p=>p.id).join('|');button.textContent='我的账户';box.querySelector('#signOut').hidden=false;box.querySelector('#emailForm').hidden=true;box.querySelector('#codeForm').hidden=true;if(!failed)status.textContent='已登录：'+(user.email||''); loading=false;await finishLoading();
  }
  client.auth.onAuthStateChange((event,session)=>{if(event==='SIGNED_IN'||event==='SIGNED_OUT')setTimeout(()=>switchUser(session?.user||null),0)});
  client.auth.getSession().then(({data})=>switchUser(data.session?.user||null)).catch(()=>{button.textContent='账户读取失败';status.textContent='无法读取登录状态，请检查网络并刷新。';});
})();
