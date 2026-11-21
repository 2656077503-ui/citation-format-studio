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
import java.io.OutputStream;

public class MainActivity extends Activity {
    private static final String HOST = "2656077503-ui.github.io";
    private static final String FINDER = "https://2656077503-ui.github.io/literature-finder/";
    private static final String CITATION = "https://2656077503-ui.github.io/citation-format-studio/";
    private static final int PICK_FILE = 101, SAVE_FILE = 102, MAX_FILE_SIZE = 25 * 1024 * 1024;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private byte[] pendingFile;
    private volatile String currentUrl = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.WHITE);
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(8, 8, 8, 8);
        Button find = new Button(this); find.setText("查找文献");
        Button cite = new Button(this); cite.setText("格式与引用");
        nav.addView(find, new LinearLayout.LayoutParams(0, -2, 1)); nav.addView(cite, new LinearLayout.LayoutParams(0, -2, 1));
        web = new WebView(this); WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true); settings.setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);
        web.addJavascriptInterface(new Downloads(), "LiteratureDownload");
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) { currentUrl=url; }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { Uri u=request.getUrl(); if (isInternal(u)) return false; openExternal(u); return true; }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null); fileCallback=callback;
                try { startActivityForResult(params.createIntent(), PICK_FILE); } catch (ActivityNotFoundException e) { fileCallback.onReceiveValue(null); fileCallback=null; toast("没有找到文件选择器"); }
                return true;
            }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean gesture, android.os.Message result) {
                WebView child = new WebView(MainActivity.this);
                child.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) { Uri u=req.getUrl(); if(isInternal(u)) web.loadUrl(u.toString()); else openExternal(u); v.post(v::destroy); return true; }
                });
                ((WebView.WebViewTransport)result.obj).setWebView(child); result.sendToTarget(); return true;
            }
        });
        web.setDownloadListener((url, userAgent, disposition, mime, length) -> {
            if (url.startsWith("blob:")) {
                String script="(async()=>{try{let r=await fetch("+org.json.JSONObject.quote(url)+");let b=await r.blob();if(b.size>"+MAX_FILE_SIZE+"){alert('文件过大，请使用浏览器下载');return}let d=await new Promise(ok=>{let f=new FileReader();f.onload=()=>ok(f.result);f.readAsDataURL(b)});LiteratureDownload.save(d, "+org.json.JSONObject.quote(android.webkit.URLUtil.guessFileName(url,disposition,mime))+", b.type)}catch(e){alert('下载失败，请在浏览器中重试')}})()";
                web.evaluateJavascript(script, null);
            } else openExternal(Uri.parse(url));
        });
        find.setOnClickListener(v->web.loadUrl(FINDER)); cite.setOnClickListener(v->web.loadUrl(CITATION));
        root.addView(nav); root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1)); setContentView(root);
        if(state==null)web.loadUrl(FINDER);else web.restoreState(state);
    }
    private boolean isInternal(Uri u) { return "https".equalsIgnoreCase(u.getScheme()) && HOST.equalsIgnoreCase(u.getHost()) && (u.getPath().startsWith("/literature-finder/") || u.getPath().startsWith("/citation-format-studio/")); }
    private void openExternal(Uri u) { if(!"https".equalsIgnoreCase(u.getScheme()) && !"http".equalsIgnoreCase(u.getScheme())) { toast("无法打开此链接"); return; } try { startActivity(new Intent(Intent.ACTION_VIEW,u)); } catch(ActivityNotFoundException e){toast("没有找到浏览器");} }
    private void toast(String msg){runOnUiThread(()->Toast.makeText(this,msg,Toast.LENGTH_SHORT).show());}
    private class Downloads {
        @JavascriptInterface public void save(String data, String name, String mime) {
            try {
                if(!isInternal(Uri.parse(currentUrl))) return;
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
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);web.saveState(state);}
    @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
}
