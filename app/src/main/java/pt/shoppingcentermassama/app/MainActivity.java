package pt.shoppingcentermassama.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

/**
 * App Android do Shopping Center Massamá.
 * Espelha o site público atualizado. O painel /admin não é exposto nesta app.
 * Para mudar o endereço de origem, editar apenas HOME.
 */
public class MainActivity extends Activity {
    private static final String HOME = "https://shoppingcentermassama.pt/";
    private static final String DOMAIN = "shoppingcentermassama.pt";
    private static final int CHOOSE_FILE = 501;
    private static final int TEAL = Color.rgb(7, 142, 152);
    private static final int DARK = Color.rgb(8, 65, 72);
    private static final int ORANGE = Color.rgb(250, 131, 30);

    private WebView webView;
    private ProgressBar progressBar;
    private LinearLayout errorPanel;
    private ValueCallback<Uri[]> pendingFiles;
    private TextView[] navLabels;
    private String[] paths = {"", "lojas/", "novidades/", "contactos/"};

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.WHITE);
        setContentView(layout);

        LinearLayout bar = new LinearLayout(this);
        bar.setPadding(dp(14),dp(6),dp(10),dp(6));
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(Color.WHITE);
        layout.addView(bar, new LinearLayout.LayoutParams(-1,dp(66)));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.logo_horizontal);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        bar.addView(logo,new LinearLayout.LayoutParams(dp(190),dp(52)));

        View empty = new View(this);
        bar.addView(empty,new LinearLayout.LayoutParams(0,1,1));

        TextView reload = new TextView(this);
        reload.setText("⟳");
        reload.setTextSize(28);
        reload.setTextColor(TEAL);
        reload.setGravity(Gravity.CENTER);
        reload.setContentDescription("Atualizar página");
        reload.setOnClickListener(v->webView.reload());
        bar.addView(reload,new LinearLayout.LayoutParams(dp(46),dp(46)));

        View separator = new View(this);
        separator.setBackgroundColor(0xFFE0EEEE);
        layout.addView(separator,new LinearLayout.LayoutParams(-1,dp(1)));

        progressBar = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressTintList(android.content.res.ColorStateList.valueOf(ORANGE));
        progressBar.setVisibility(View.GONE);
        layout.addView(progressBar,new LinearLayout.LayoutParams(-1,dp(3)));

        FrameLayout webFrame = new FrameLayout(this);
        layout.addView(webFrame,new LinearLayout.LayoutParams(-1,0,1));
        webView = new WebView(this);
        webFrame.addView(webView,new FrameLayout.LayoutParams(-1,-1));
        buildErrorPanel(webFrame);
        setupWebView();

        LinearLayout navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setPadding(dp(3),dp(2),dp(3),dp(28));
        navigation.setBackgroundColor(Color.WHITE);
        layout.addView(navigation,new LinearLayout.LayoutParams(-1,dp(90)));
        String[] titles = {"Início", "Lojas", "Novidades", "Contactos"};
        navLabels = new TextView[titles.length];
        for(int i=0;i<titles.length;i++) {
            final int index=i;
            TextView item=new TextView(this);
            item.setText(titles[i]);
            item.setTextSize(12);
            item.setGravity(Gravity.CENTER);
            item.setTextColor(DARK);
            item.setTypeface(null,i==0?1:0);
            item.setContentDescription("Abrir "+titles[i]);
            item.setOnClickListener(v->openSection(index));
            navLabels[i]=item;
            navigation.addView(item,new LinearLayout.LayoutParams(0,-1,1));
        }
        if(state!=null && webView.restoreState(state)!=null){
            updateTab(webView.getUrl());
        } else {
            webView.loadUrl(HOME);
        }
    }

    private void buildErrorPanel(FrameLayout frame) {
        errorPanel = new LinearLayout(this);
        errorPanel.setGravity(Gravity.CENTER);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        errorPanel.setPadding(dp(28),dp(28),dp(28),dp(28));
        errorPanel.setBackgroundColor(Color.WHITE);
        errorPanel.setVisibility(View.GONE);

        TextView title=new TextView(this);
        title.setText("Sem ligação à internet");
        title.setTextColor(DARK);
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null,1);
        errorPanel.addView(title);

        TextView description=new TextView(this);
        description.setText("Verifica a ligação e volta a tentar para consultar as lojas e novidades do Shopping Massamá.");
        description.setTextSize(15);
        description.setTextColor(0xFF506A70);
        description.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);
        p.topMargin=dp(12);
        errorPanel.addView(description,p);

        Button retry=new Button(this);
        retry.setAllCaps(false);
        retry.setText("Tentar novamente");
        retry.setTextColor(Color.WHITE);
        retry.setBackgroundTintList(android.content.res.ColorStateList.valueOf(TEAL));
        retry.setOnClickListener(v->{errorPanel.setVisibility(View.GONE);webView.reload();});
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-2,-2);
        bp.topMargin=dp(20);
        errorPanel.addView(retry,bp);
        frame.addView(errorPanel,new FrameLayout.LayoutParams(-1,-1));
    }

    private void setupWebView() {
        WebSettings s=webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setDefaultTextEncodingName("utf-8");
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView,false);

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri=request.getUrl();
                String scheme=uri.getScheme();
                if("https".equalsIgnoreCase(scheme)) {
                    String host=uri.getHost();
                    if(DOMAIN.equalsIgnoreCase(host) || ("www."+DOMAIN).equalsIgnoreCase(host))return false;
                }
                openExternal(uri);
                return true;
            }
            @Override public void onPageFinished(WebView view,String url) {
                if(errorPanel.getVisibility()!=View.VISIBLE)progressBar.setVisibility(View.GONE);
                updateTab(url);
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if(request.isForMainFrame()){
                    errorPanel.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.GONE);
                }
            }
            @Override public void onReceivedSslError(WebView view, android.webkit.SslErrorHandler handler, android.net.http.SslError error){
                handler.cancel();
                errorPanel.setVisibility(View.VISIBLE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView view,int progress) {
                progressBar.setProgress(progress);
                if(progress<100 && errorPanel.getVisibility()!=View.VISIBLE)progressBar.setVisibility(View.VISIBLE);
                else if(progress==100)progressBar.setVisibility(View.GONE);
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params){
                if(pendingFiles!=null)pendingFiles.onReceiveValue(null);
                pendingFiles=cb;
                try{
                    startActivityForResult(Intent.createChooser(params.createIntent(),"Escolher fotografia ou ficheiro"),CHOOSE_FILE);
                }catch(ActivityNotFoundException e){
                    pendingFiles.onReceiveValue(null);
                    pendingFiles=null;
                    Toast.makeText(MainActivity.this,"Não foi possível abrir os ficheiros",Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        webView.setDownloadListener((url,userAgent,contentDisposition,mimeType,length)-> {
            try {
                Uri uri=Uri.parse(url);
                if("https".equalsIgnoreCase(uri.getScheme())) {
                    Intent intent=new Intent(Intent.ACTION_VIEW,uri);
                    startActivity(intent);
                }
            } catch(Exception e) {Toast.makeText(this,"Não foi possível abrir o ficheiro",Toast.LENGTH_SHORT).show();}
        });
    }

    private void openExternal(Uri uri){
        try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}
        catch(Exception e){Toast.makeText(this,"Não foi possível abrir esta ligação",Toast.LENGTH_SHORT).show();}
    }

    private void openSection(int i){
        errorPanel.setVisibility(View.GONE);
        webView.loadUrl(HOME+paths[i]);
        updateTab(HOME+paths[i]);
    }
    private void updateTab(String url){
        if(navLabels==null || url==null)return;
        int index=0;
        if(url.contains("/lojas/"))index=1;
        else if(url.contains("/novidades/") || url.contains("/eventos/") || url.contains("/promocoes/"))index=2;
        else if(url.contains("/contactos/"))index=3;
        for(int i=0;i<navLabels.length;i++){
            navLabels[i].setTextColor(i==index?TEAL:DARK);
            navLabels[i].setTypeface(null,i==index?1:0);
        }
    }
    private int dp(int value){return (int)(getResources().getDisplayMetrics().density*value+0.5f);}

    @SuppressWarnings("deprecation")
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==CHOOSE_FILE && pendingFiles!=null) {
            Uri[] selected = WebChromeClient.FileChooserParams.parseResult(resultCode,data);
            if(selected!=null){
                java.util.ArrayList<Uri> safe=new java.util.ArrayList<>();
                for(Uri uri:selected){
                    if(uri!=null && "content".equalsIgnoreCase(uri.getScheme()))safe.add(uri);
                }
                selected=safe.isEmpty()?null:safe.toArray(new Uri[0]);
            }
            pendingFiles.onReceiveValue(selected);
            pendingFiles=null;
        }
    }
    @Override public void onBackPressed(){
        if(errorPanel.getVisibility()==View.VISIBLE) {
            errorPanel.setVisibility(View.GONE);webView.loadUrl(HOME);
        }else if(webView.canGoBack())webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onSaveInstanceState(Bundle outState){
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }
    @Override protected void onDestroy(){
        if(pendingFiles!=null){pendingFiles.onReceiveValue(null);pendingFiles=null;}
        webView.destroy();
        super.onDestroy();
    }
}
