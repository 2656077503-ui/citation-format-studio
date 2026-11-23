package cn.literature.assistant;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final String HOST = "2656077503-ui.github.io";
    private static final String FINDER = "https://2656077503-ui.github.io/literature-finder/";
    private static final String CITATION = "https://2656077503-ui.github.io/citation-format-studio/";
    private static final int PICK_FILE = 101, SAVE_FILE = 102, MAX_FILE_SIZE = 25 * 1024 * 1024;
    private WebView web, finder, citation;
    private FrameLayout content;
    private LinearLayout bottom;
    private String pendingFinder = "", pendingCitation = "";
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingFile;
    private volatile String currentUrl = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(246,249,246));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE); root.setFitsSystemWindows(true);
        content=new FrameLayout(this); bottom=new LinearLayout(this); bottom.setPadding(dp(8),dp(5),dp(8),dp(8)); bottom.setBackgroundColor(Color.WHITE);
        finder=createWeb(); citation=createWeb();
        content.addView(finder,new FrameLayout.LayoutParams(-1,-1)); content.addView(citation,new FrameLayout.LayoutParams(-1,-1));
        root.addView(content,new LinearLayout.LayoutParams(-1,0,1)); root.addView(bottom,new LinearLayout.LayoutParams(-1,dp(68))); setContentView(root);
        finder.loadUrl(FINDER); citation.loadUrl(CITATION);
        showFinder("startup");
    }
    private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
    private GradientDrawable bg(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(color);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
    private final int ink=Color.rgb(26,53,48), muted=Color.rgb(103,126,116), pale=Color.rgb(240,247,241), accent=Color.rgb(38,91,74);
    private TextView card(String title,String detail,Runnable click){TextView v=text(title+"    →\n"+detail,16,ink,true);v.setLineSpacing(dp(5),1);v.setPadding(dp(19),dp(18),dp(19),dp(18));v.setBackground(bg(pale,16));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(10),0,0);v.setLayoutParams(lp);v.setOnClickListener(x->click.run());return v;}
    private void nav(String selected){bottom.removeAllViews();String[] labels={"首页","查文献","我的文献","引用助手"};for(int i=0;i<labels.length;i++){final int index=i;TextView item=text(labels[i],13,selected.equals(labels[i])?accent:muted,selected.equals(labels[i]));item.setGravity(Gravity.CENTER);item.setBackground(bg(selected.equals(labels[i])?pale:Color.WHITE,14));bottom.addView(item,new LinearLayout.LayoutParams(0,-1,1));item.setOnClickListener(v->{if(index==0)showHome();else if(index==1)showFinder("search");else if(index==2)showLibrary();else showCitation("placement");});}}
    private void hideContent(){finder.setVisibility(View.GONE);citation.setVisibility(View.GONE);for(int i=content.getChildCount()-1;i>=0;i--){View v=content.getChildAt(i);if(v!=finder&&v!=citation)content.removeViewAt(i);}}
    private LinearLayout page(String title,String subtitle){ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(Color.WHITE);LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(22),dp(30),dp(22),dp(22));scroll.addView(body);content.addView(scroll,new FrameLayout.LayoutParams(-1,-1));TextView brand=text("文献助手",14,accent,true);body.addView(brand);TextView h=text(title,27,ink,true);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(22),0,dp(8));body.addView(h,hp);TextView sub=text(subtitle,14,muted,false);sub.setLineSpacing(dp(3),1);body.addView(sub);return body;}
    private void showHome(){hideContent();LinearLayout body=page("把文献工作，理清楚","从查找、格式整理到引用，一步一步完成");body.addView(card("查找文献","输入 DOI、题目或组合关键词",()->showFinder("search")));body.addView(card("整理文献格式","添加文献，选择目标参考文献格式",()->showCitation("style")));body.addView(card("推荐引用位置","导入 Word，核对并插入参考文献",()->showCitation("placement")));nav("首页");}
    private void showLibrary(){hideContent();LinearLayout body=page("我的文献","统一查看添加的文献、收藏和查询记录。");body.addView(card("已添加文献","查看格式助手中的参考文献列表",()->showCitation("selected")));body.addView(card("我的收藏","查看已登录账户收藏的文献",()->showFinder("favorites")));body.addView(card("查询记录","查看此前在本应用中的检索结果",()->showFinder("records")));TextView hint=text("收藏需要登录；访客记录保存在本应用中。",12,muted,false);LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);hp.setMargins(0,dp(22),0,0);body.addView(hint,hp);nav("我的文献");}
    private void showFinder(String target){hideContent();finder.setVisibility(View.VISIBLE);web=finder;currentUrl=finder.getUrl()==null?"":finder.getUrl();nav("查文献");if("startup".equals(target)){pendingFinder=target;if(finder.getProgress()>=100)openStartupLogin(finder);}else if(!scrollTo(finder,target))pendingFinder=target;}
    private void showCitation(String target){hideContent();citation.setVisibility(View.VISIBLE);web=citation;currentUrl=citation.getUrl()==null?"":citation.getUrl();nav("引用助手");if(!scrollTo(citation,target))pendingCitation=target;}
    private boolean scrollTo(WebView view,String target){if(view.getUrl()==null||view.getProgress()<100)return false;String selector;switch(target){case "search":selector=".search-panel";break;case "records":selector="section[aria-label=查询结果]";break;case "favorites":selector="section[aria-label=收藏文献]";break;case "selected":selector="#selected";break;case "style":selector="#style";break;default:selector="#placement";}
        String js="(function(){let x=document.querySelector("+org.json.JSONObject.quote(selector)+");if(x){x.scrollIntoView({behavior:'smooth',block:'start'});return 'ok'}return 'missing'})()";
        view.evaluateJavascript(js,value->{if("\"missing\"".equals(value)){if("favorites".equals(target))toast("暂无收藏，请先登录并收藏文献");else if("records".equals(target))toast("暂无查询记录，请先查找文献");}});return true;}
    private WebView createWeb(){
        final WebView view=new WebView(this); WebSettings settings=view.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(view,true);
        view.addJavascriptInterface(new Downloads(view),"LiteratureDownload");
        view.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView v,String url){if(v==web)currentUrl=url; if(isInternal(Uri.parse(url))) applyAppDisplay(v,url); String target=v==finder?pendingFinder:pendingCitation;if(!target.isEmpty()){if(v==finder)pendingFinder="";else pendingCitation="";if("startup".equals(target))openStartupLogin(v);else scrollTo(v,target);}}
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest request){Uri u=request.getUrl();if(isInternal(u))return false;openExternal(u);return true;}
        });
        view.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;try{startActivityForResult(params.createIntent(),PICK_FILE);}catch(ActivityNotFoundException e){fileCallback.onReceiveValue(null);fileCallback=null;toast("没有找到文件选择器");}return true;}
            @Override public boolean onCreateWindow(WebView v,boolean dialog,boolean gesture,android.os.Message result){WebView child=new WebView(MainActivity.this);child.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest req){Uri u=req.getUrl();if(isInternal(u))v.loadUrl(u.toString());else openExternal(u);w.post(w::destroy);return true;}});((WebView.WebViewTransport)result.obj).setWebView(child);result.sendToTarget();return true;}
        });
        view.setDownloadListener((url,userAgent,disposition,mime,length)->{if(url.startsWith("blob:")){String script="(async()=>{try{let r=await fetch("+org.json.JSONObject.quote(url)+");let b=await r.blob();if(b.size>"+MAX_FILE_SIZE+"){alert('文件过大，请使用浏览器下载');return}let d=await new Promise(ok=>{let f=new FileReader();f.onload=()=>ok(f.result);f.readAsDataURL(b)});LiteratureDownload.save(d, "+org.json.JSONObject.quote(android.webkit.URLUtil.guessFileName(url,disposition,mime))+", b.type)}catch(e){alert('下载失败，请在浏览器中重试')}})()";view.evaluateJavascript(script,null);}else openExternal(Uri.parse(url));});return view;
    }
    private void applyAppDisplay(WebView v,String url){
        String css=url.contains("/literature-finder/")
            ? ".brand small,.owner-link,.panel-intro .section-kicker,.search-hint,.steps{display:none!important}"
            : ".brand small,.hero .eyebrow,.workspace-head .eyebrow,.placement-intro,#usePlainDraft,.placement-tool>p.hint,.document-tool>div>p:not(#docStatus),footer span{display:none!important}";
        String js="(function(){let s=document.getElementById('app-display-style');if(!s){s=document.createElement('style');s.id='app-display-style';document.head.appendChild(s)}s.textContent="+org.json.JSONObject.quote(css)+";let d=document.getElementById('docStatus');if(d&&d.textContent.includes('文件在当前设备中处理'))d.textContent='';})()";
        v.evaluateJavascript(js,null);
    }
    private void openStartupLogin(WebView v){
        String js="(function(){let n=0;function run(){let b=document.querySelector('.account-trigger');if(!b){if(++n<40)setTimeout(run,250);return}let session=false;try{for(let i=0;i<localStorage.length;i++){let k=localStorage.key(i);if(k&&/^sb-.*-auth-token$/.test(k)){let a=JSON.parse(localStorage.getItem(k));session=!!(a&&a.access_token&&a.expires_at*1000>Date.now());if(session)break}}}catch(e){}if(!session)b.click()}run()})()";
        v.evaluateJavascript(js,null);
    }
    private boolean isInternal(Uri u) { return "https".equalsIgnoreCase(u.getScheme()) && HOST.equalsIgnoreCase(u.getHost()) && (u.getPath().startsWith("/literature-finder/") || u.getPath().startsWith("/citation-format-studio/")); }
    private void openExternal(Uri u) { if(!"https".equalsIgnoreCase(u.getScheme()) && !"http".equalsIgnoreCase(u.getScheme())) { toast("无法打开此链接"); return; } try { startActivity(new Intent(Intent.ACTION_VIEW,u)); } catch(ActivityNotFoundException e){toast("没有找到浏览器");} }
    private void toast(String msg){runOnUiThread(()->Toast.makeText(this,msg,Toast.LENGTH_SHORT).show());}
    private class Downloads {
        private final WebView source; Downloads(WebView source){this.source=source;}
        @JavascriptInterface public void save(String data, String name, String mime) {
            try {
                if(source != web || !isInternal(Uri.parse(currentUrl))) return;
                int i=data.indexOf(','); if(i<0 || data.length() > (MAX_FILE_SIZE*4L/3L+4096))throw new IllegalArgumentException();
                byte[] bytes=Base64.decode(data.substring(i+1), Base64.DEFAULT); if(bytes.length>MAX_FILE_SIZE)throw new IllegalArgumentException();
                runOnUiThread(()->{pendingFile=bytes; Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType(mime!=null&&!mime.isEmpty()?mime:"application/octet-stream"); intent.putExtra(Intent.EXTRA_TITLE,name==null||name.isEmpty()?"文献文件":name); try{startActivityForResult(intent,SAVE_FILE);}catch(ActivityNotFoundException e){pendingFile=null;toast("没有找到文件保存工具");}});
            } catch(Exception e){toast("文件保存失败，请在浏览器重试");}
        }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_FILE && fileCallback!=null){fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data));fileCallback=null;}
        if(requestCode==SAVE_FILE){byte[] bytes=pendingFile;pendingFile=null;if(resultCode==RESULT_OK && data!=null && bytes!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new Exception();out.write(bytes);toast("文件已保存");}catch(Exception e){toast("文件保存失败");}}}
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);}
    @Override public void onBackPressed(){if(web!=null&&web.getVisibility()==View.VISIBLE&&web.canGoBack())web.goBack();else showHome();}
}
