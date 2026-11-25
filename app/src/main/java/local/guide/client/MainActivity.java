package local.guide.client;
import android.app.*; import android.os.*; import android.net.Uri; import android.view.*; import android.webkit.*; import android.widget.*;
import androidx.media3.common.MediaItem; import androidx.media3.common.Player; import androidx.media3.common.PlaybackException; import androidx.media3.exoplayer.ExoPlayer; import androidx.media3.ui.PlayerView; import androidx.media3.datasource.DefaultHttpDataSource; import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.OkHttpClient;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
public class MainActivity extends Activity {
 private String guideScript; private WebView web; private FrameLayout root; private View full; private WebChromeClient.CustomViewCallback callback; private volatile String origin; private ExoPlayer player; private PlayerView nativeView; private final Handler handler=new Handler(Looper.getMainLooper());
 private final AtomicBoolean handoff=new AtomicBoolean(false);
 private volatile boolean guideActive=false;
 private volatile long suppressUntil=0;
 private final OkHttpClient nativeHttp=new OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build();
 private int port(Uri a){return a.getPort()!=-1?a.getPort():"https".equals(a.getScheme())?443:80;}
 private boolean same(String url){try{Uri a=Uri.parse(url),b=Uri.parse(origin);return a.getHost()!=null&&a.getHost().equalsIgnoreCase(b.getHost())&&a.getScheme().equals(b.getScheme())&&port(a)==port(b);}catch(Exception e){return false;}}
 @Override public void onCreate(Bundle b){super.onCreate(b);try(InputStream in=getAssets().open("guide.js");ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[4096];int count;while((count=in.read(buf))!=-1)out.write(buf,0,count);guideScript=new String(out.toByteArray(),StandardCharsets.UTF_8);}catch(Exception e){throw new IllegalStateException(e);}origin=getPreferences(0).getString("origin","http://192.168.1.15:9191");root=new FrameLayout(this);setContentView(root);web=new WebView(this);root.addView(web,new FrameLayout.LayoutParams(-1,-1));WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setMediaPlaybackRequiresUserGesture(false);s.setAllowFileAccess(false);s.setAllowContentAccess(false);CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
 web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String u=r.getUrl().toString();if(!r.isForMainFrame())return false;if(!same(u))return true;if(r.hasGesture()&&stream(u)){play(u);return true;}String p=r.getUrl().getPath();if(!"/guide".equals(p)&&!"/guide/".equals(p)&&!"/login".equals(p)&&!"/login/".equals(p)){v.loadUrl(origin+"/guide");return true;}return false;}@Override public void onPageFinished(WebView v,String url){guideActive=same(url)&&"/guide".equals(Uri.parse(url).getPath());if(same(url))adapt();CookieManager.getInstance().flush();}
 @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){String u=r.getUrl().toString();String p=r.getUrl().getPath();if(!guideActive||!same(u)||!"GET".equals(r.getMethod())||p==null||!p.startsWith("/proxy/ts/stream/"))return null;
 if(handoff.get()||SystemClock.elapsedRealtime()<suppressUntil)return emptyStream();
 if(!handoff.compareAndSet(false,true))return emptyStream();
 HashMap<String,String> headers=new HashMap<>();for(Map.Entry<String,String> e:r.getRequestHeaders().entrySet()){if("Authorization".equalsIgnoreCase(e.getKey()))headers.put("Authorization",e.getValue());}
 handler.post(()->startHandoff(u,headers));return emptyStream();}
 });
 web.setWebChromeClient(new WebChromeClient(){@Override public void onShowCustomView(View v,CustomViewCallback cb){if(full!=null||player!=null){cb.onCustomViewHidden();return;}full=v;callback=cb;root.addView(v,new FrameLayout.LayoutParams(-1,-1));web.setVisibility(View.INVISIBLE);immersive();}@Override public void onHideCustomView(){closeFull();}});
 web.loadUrl(origin+"/guide");immersive();if(!getPreferences(0).contains("origin"))settings();handler.post(tick);}
 private boolean stream(String u){String p=Uri.parse(u).getPath();return p!=null&&(p.startsWith("/proxy/ts/stream/")||p.endsWith(".m3u8")||p.endsWith(".ts"));}
 private final Runnable tick=new Runnable(){public void run(){if(web!=null&&full==null&&player==null&&same(web.getUrl()))adapt();handler.postDelayed(this,1000);}};
 private void adapt(){String url=web.getUrl();guideActive=same(url)&&Uri.parse(url).getPath()!=null&&Uri.parse(url).getPath().replaceAll("/$","").equals("/guide");if(guideScript!=null)web.evaluateJavascript(guideScript,null);}
 private WebResourceResponse emptyStream(){return new WebResourceResponse("video/mp2t",null,204,"No Content",Collections.emptyMap(),new java.io.ByteArrayInputStream(new byte[0]));}
 private void startHandoff(String url,HashMap<String,String> headers){if(isFinishing()||isDestroyed()||!guideActive||!same(url)){handoff.set(false);return;}web.evaluateJavascript("(function(){for(const v of document.querySelectorAll('video')){v.pause();let n=v.parentElement;while(n&&n!==document.body){if(getComputedStyle(n).position==='fixed'){const c=n.querySelector('.mantine-CloseButton-root,button[aria-label*=close i]');if(c)c.click();break;}n=n.parentElement;}}return localStorage.getItem('accessToken')||'';})()",value->{if(isFinishing()||isDestroyed()||!same(url)){handoff.set(false);return;}if(!headers.containsKey("Authorization")){try{Object token=new org.json.JSONTokener(value).nextValue();if(token instanceof String&&!((String)token).isEmpty())headers.put("Authorization","Bearer "+token);}catch(Exception ignored){}}play(url,headers);});}
 private void restoreGuide(){web.requestFocus();web.evaluateJavascript("window.__guideClientV2&&window.__guideClientV2.restore()",null);}
 private void play(String url){play(url,new HashMap<>());}
 private void play(String url,HashMap<String,String> headers){if(!same(url)||!stream(url)||player!=null){handoff.set(false);return;}
 if(full!=null)closeFull();
 String cookie=CookieManager.getInstance().getCookie(url);if(cookie!=null)headers.put("Cookie",cookie);
 Uri uri=Uri.parse(url);if(uri.getPath()!=null&&uri.getPath().startsWith("/proxy/ts/stream/")&&uri.getQueryParameter("output_format")==null)uri=uri.buildUpon().appendQueryParameter("output_format","mpegts").appendQueryParameter("skip_redirect","true").build();
 OkHttpDataSource.Factory http=new OkHttpDataSource.Factory(nativeHttp).setDefaultRequestProperties(headers);
 DefaultLoadControl load=new DefaultLoadControl.Builder().setBufferDurationsMs(15000,50000,2500,5000).build();
 player=new ExoPlayer.Builder(this).setLoadControl(load).setMediaSourceFactory(new DefaultMediaSourceFactory(this).setDataSourceFactory(http)).build();
 nativeView=new PlayerView(this);nativeView.setPlayer(player);root.addView(nativeView,new FrameLayout.LayoutParams(-1,-1));web.setVisibility(View.INVISIBLE);nativeView.requestFocus();
 player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException e){Toast.makeText(MainActivity.this,"Native playback failed: "+e.getErrorCodeName(),Toast.LENGTH_LONG).show();closePlayer();}});
 player.setMediaItem(MediaItem.fromUri(uri));player.prepare();player.play();immersive();
 Toast.makeText(this,"Native Media3 playback",Toast.LENGTH_SHORT).show();
 }
 private void closePlayer(){suppressUntil=SystemClock.elapsedRealtime()+1200;handoff.set(false);if(player==null)return;nativeView.setPlayer(null);player.release();player=null;root.removeView(nativeView);nativeView=null;web.setVisibility(View.VISIBLE);restoreGuide();immersive();}
 private void immersive(){getWindow().getDecorView().setSystemUiVisibility(5894);}
 private void closeFull(){if(full==null)return;root.removeView(full);full=null;web.setVisibility(View.VISIBLE);if(callback!=null){callback.onCustomViewHidden();callback=null;}restoreGuide();immersive();}
 private void settings(){EditText input=new EditText(this);input.setSingleLine();input.setText(origin);new AlertDialog.Builder(this).setTitle("Server address").setView(input).setNegativeButton("Cancel",null).setPositiveButton("Connect",(d,w)->{Uri u=Uri.parse(input.getText().toString().trim());if(u.getHost()==null||!("http".equals(u.getScheme())||"https".equals(u.getScheme()))||u.getUserInfo()!=null){Toast.makeText(this,"Invalid server URL",Toast.LENGTH_LONG).show();return;}origin=u.getScheme()+"://"+u.getEncodedAuthority();getPreferences(0).edit().putString("origin",origin).apply();web.loadUrl(origin+"/guide");}).show();}
 @Override public boolean onKeyDown(int key,KeyEvent e){if(key==KeyEvent.KEYCODE_MENU&&player==null&&full==null){settings();return true;}return super.onKeyDown(key,e);}
 @Override public void onBackPressed(){if(player!=null)closePlayer();else if(full!=null){closeFull();web.evaluateJavascript("document.querySelectorAll('video').forEach(v=>v.pause());",null);}else if(same(web.getUrl())&&Uri.parse(web.getUrl()).getPath().startsWith("/guide")){web.evaluateJavascript("!!(window.__guideClientV2&&window.__guideClientV2.back())",result->{if(!"true".equals(result))finish();});}else super.onBackPressed();}
 @Override protected void onPause(){super.onPause();if(player!=null)player.pause();web.onPause();CookieManager.getInstance().flush();}
 @Override protected void onResume(){super.onResume();if(web!=null)web.onResume();immersive();}
 @Override protected void onDestroy(){handler.removeCallbacks(tick);closePlayer();closeFull();web.destroy();super.onDestroy();}
}