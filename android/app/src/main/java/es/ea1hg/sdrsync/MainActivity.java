package es.ea1hg.sdrsync;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Ventana a la página de control remoto de EA1HG SDR SYNC, con ajustes guardados y audio en segundo plano. */
public class MainActivity extends Activity {
    static final String PREFS = "cfg";
    static final String EXTRA_EXIT = "exit";

    private WebView web;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        sp = getSharedPreferences(PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(0xFF070B11);
        getWindow().setNavigationBarColor(0xFF070B11);

        web = new WebView(this);
        web.setBackgroundColor(0xFF070B11);
        setContentView(web, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);
        web.addJavascriptInterface(new Bridge(), "EA1HG_APP");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r != null && r.isForMainFrame()) {
                    showError(String.valueOf(e.getDescription()));
                }
            }
        });

        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        applyKeepOn();
        startKeepAlive();
        if (sp.getString("url", "").isEmpty()) {
            showSettings(true);
        } else {
            load();
        }
    }

    // ---- conexión
    private void load() {
        String u = sp.getString("url", "").trim();
        if (u.isEmpty()) {
            showSettings(true);
            return;
        }
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "http://" + u;
        }
        int hash = u.indexOf('#');
        if (hash >= 0) {
            u = u.substring(0, hash);
        }
        String pw = sp.getString("pass", "");
        web.loadUrl(u + (pw.isEmpty() ? "" : "#pw=" + Uri.encode(pw)));
    }

    private void showError(String detail) {
        String html = "<html><body style='background:#070b11;color:#e8f0f7;font-family:sans-serif;text-align:center;padding:12vh 20px'>"
                + "<h2>No se puede conectar</h2>"
                + "<p style='color:#7f93a7'>Comprueba que el programa está abierto en el PC, que el control remoto está activado "
                + "y que Tailscale (o la Wi-Fi de casa) está conectado.</p>"
                + "<p style='color:#7f93a7;font-size:12px'>" + android.text.TextUtils.htmlEncode(detail) + "</p>"
                + "<p><button style='font-size:18px;padding:14px 26px;border-radius:12px;border:0;background:#38bdf8' "
                + "onclick='EA1HG_APP.retry()'>Reintentar</button></p>"
                + "<p><button style='font-size:16px;padding:12px 22px;border-radius:12px;border:1px solid #223042;background:#141d29;color:#e8f0f7' "
                + "onclick='EA1HG_APP.settings()'>Ajustes</button></p></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
    }

    // ---- ajustes
    private void showSettings(boolean first) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        box.setPadding(p, p / 2, p, 0);

        TextView t1 = new TextView(this);
        t1.setText("Dirección del PC (por ejemplo http://100.x.x.x:8765)");
        final EditText url = new EditText(this);
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setText(sp.getString("url", "http://"));
        TextView t2 = new TextView(this);
        t2.setText("Contraseña del control remoto");
        final EditText pass = new EditText(this);
        pass.setSingleLine(true);
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setText(sp.getString("pass", ""));
        final CheckBox keep = new CheckBox(this);
        keep.setText("Mantener la pantalla encendida");
        keep.setChecked(sp.getBoolean("keepon", false));
        box.addView(t1);
        box.addView(url);
        box.addView(t2);
        box.addView(pass);
        box.addView(keep);

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Ajustes")
                .setView(box)
                .setPositiveButton("Guardar y conectar", (d, w) -> {
                    sp.edit().putString("url", url.getText().toString().trim())
                            .putString("pass", pass.getText().toString())
                            .putBoolean("keepon", keep.isChecked()).apply();
                    applyKeepOn();
                    load();
                })
                .setNeutralButton("Salir de la app", (d, w) -> exitApp());
        if (first) {
            b.setCancelable(false);
        } else {
            b.setNegativeButton("Cancelar", null);
        }
        b.show();
    }

    private void applyKeepOn() {
        if (sp.getBoolean("keepon", false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // ---- segundo plano
    private void startKeepAlive() {
        try {
            startForegroundService(new Intent(this, KeepAliveService.class));
        } catch (RuntimeException ignored) {
            // si el sistema no lo permite, la app sigue funcionando con la pantalla encendida
        }
    }

    private void exitApp() {
        stopService(new Intent(this, KeepAliveService.class));
        finishAndRemoveTask();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getBooleanExtra(EXTRA_EXIT, false)) {
            exitApp();
        }
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            moveTaskToBack(true);   // no se cierra: el audio sigue sonando
        }
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            stopService(new Intent(this, KeepAliveService.class));
        }
        web.destroy();
        super.onDestroy();
    }

    /** Lo que la página puede pedir a la app (la página muestra el botón ⚙ solo si existe EA1HG_APP). */
    public class Bridge {
        @JavascriptInterface
        public void settings() {
            runOnUiThread(() -> showSettings(false));
        }

        @JavascriptInterface
        public void retry() {
            runOnUiThread(MainActivity.this::load);
        }
    }
}
