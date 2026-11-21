package at.fotoupload.app;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private static final int CHOOSE_FILE = 100;
    private static final int CAPTURE_PHOTO = 101;
    private static final int EDIT_SETTINGS = 102;
    static final String PREFERENCES = "foto_upload_settings";
    static final String SERVER_KEY = "server";
    private static final Uri CAMERA_URI = Uri.parse("content://at.fotoupload.app.camera/capture.jpg");
    private WebView browser;
    private LinearLayout offline;
    private ValueCallback<Uri[]> fileCallback;
    private Uri server;
    private Uri[] sharedFiles;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.rgb(23, 60, 52));
        layout.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        getWindow().setStatusBarColor(Color.rgb(23, 60, 52));
        getWindow().setNavigationBarColor(Color.rgb(23, 60, 52));
        Button settings = new Button(this);
        settings.setText("⚙  Einstellungen");
        settings.setAllCaps(false);
        settings.setOnClickListener(view -> editSettings());
        layout.addView(settings);
        FrameLayout content = new FrameLayout(this);
        browser = new WebView(this);
        content.addView(browser, new FrameLayout.LayoutParams(-1, -1));
        offline = makeOfflineView();
        content.addView(offline, new FrameLayout.LayoutParams(-1, -1));
        layout.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        WebSettings web = browser.getSettings();
        web.setJavaScriptEnabled(true);
        web.setTextZoom(100);
        web.setUserAgentString(web.getUserAgentString() + " FotoUploadAndroid/1");
        web.setDomStorageEnabled(true);
        web.setAllowFileAccess(false);
        web.setAllowContentAccess(true);
        browser.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, true);
        browser.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap icon) {
                browser.setVisibility(View.VISIBLE);
                offline.setVisibility(View.GONE);
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showOffline();
            }

            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri target = request.getUrl();
                if (sameServer(target)) return false;
                if ("https".equals(target.getScheme()) || "http".equals(target.getScheme())) {
                    startActivity(new Intent(Intent.ACTION_VIEW, target));
                }
                return true;
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                if (sharedFiles != null && sharedFiles.length > 0) {
                    Uri[] selected = sharedFiles;
                    sharedFiles = null;
                    callback.onReceiveValue(selected);
                    Toast.makeText(MainActivity.this, "Geteilte Dateien werden hochgeladen", Toast.LENGTH_SHORT).show();
                    return true;
                }
                fileCallback = callback;
                if (params.isCaptureEnabled()) {
                    Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    camera.putExtra(MediaStore.EXTRA_OUTPUT, CAMERA_URI);
                    camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivityForResult(camera, CAPTURE_PHOTO);
                        return true;
                    } catch (android.content.ActivityNotFoundException error) {
                        // Fall through to the system photo picker.
                    }
                }
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                try { startActivityForResult(intent, CHOOSE_FILE); }
                catch (Exception error) { fileCallback = null; callback.onReceiveValue(null); return false; }
                return true;
            }
        });
        browser.setDownloadListener((url, agent, disposition, mime, length) -> {
            Uri target = Uri.parse(url);
            if (!sameServer(target)) return;
            DownloadManager.Request request = new DownloadManager.Request(target);
            String cookie = CookieManager.getInstance().getCookie(url);
            if (cookie != null) request.addRequestHeader("Cookie", cookie);
            request.addRequestHeader("User-Agent", agent);
            request.setMimeType(mime);
            request.setTitle("Foto-Upload");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS,
                android.webkit.URLUtil.guessFileName(url, disposition, mime));
            ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
            Toast.makeText(this, "Download gestartet", Toast.LENGTH_SHORT).show();
        });
        migratePreferences();
        receiveSharedFiles(getIntent());
        String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(SERVER_KEY, "");
        if (saved.isEmpty()) editSettings(); else openServer(saved);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receiveSharedFiles(intent);
    }

    private void receiveSharedFiles(Intent intent) {
        if (intent == null || !(Intent.ACTION_SEND.equals(intent.getAction())
            || Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()))) return;
        ArrayList<Uri> uris = new ArrayList<>();
        if (intent.getClipData() != null) {
            for (int index = 0; index < intent.getClipData().getItemCount(); index++) {
                Uri uri = intent.getClipData().getItemAt(index).getUri();
                if (uri != null && "content".equals(uri.getScheme())) uris.add(uri);
            }
        }
        if (uris.isEmpty()) {
            if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
                ArrayList<Uri> many = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                if (many != null) for (Uri uri : many) if (uri != null && "content".equals(uri.getScheme())) uris.add(uri);
            } else {
                Uri one = intent.getParcelableExtra(Intent.EXTRA_STREAM);
                if (one != null && "content".equals(one.getScheme())) uris.add(one);
            }
        }
        if (!uris.isEmpty()) {
            sharedFiles = uris.toArray(new Uri[0]);
            Toast.makeText(this, "Album öffnen und dort ‚Fotos auswählen‘ antippen.", Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout makeOfflineView() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setPadding(48, 48, 48, 48);
        panel.setBackgroundColor(Color.rgb(248, 250, 249));
        panel.setVisibility(View.GONE);
        TextView title = new TextView(this);
        title.setText("Server nicht erreichbar");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(23, 60, 52));
        title.setGravity(Gravity.CENTER);
        panel.addView(title);
        TextView message = new TextView(this);
        message.setText("Prüfe deine Internetverbindung oder die Serveradresse.");
        message.setTextSize(16);
        message.setGravity(Gravity.CENTER);
        message.setPadding(0, 20, 0, 24);
        panel.addView(message);
        Button retry = new Button(this);
        retry.setText("Erneut versuchen");
        retry.setAllCaps(false);
        retry.setOnClickListener(view -> browser.reload());
        panel.addView(retry);
        Button change = new Button(this);
        change.setText("Serveradresse ändern");
        change.setAllCaps(false);
        change.setOnClickListener(view -> editSettings());
        panel.addView(change);
        return panel;
    }

    private void showOffline() {
        browser.setVisibility(View.GONE);
        offline.setVisibility(View.VISIBLE);
    }

    private boolean sameServer(Uri target) {
        if (server == null || target.getHost() == null) return false;
        int leftPort = server.getPort() == -1 ? ("https".equals(server.getScheme()) ? 443 : 80) : server.getPort();
        int rightPort = target.getPort() == -1 ? ("https".equals(target.getScheme()) ? 443 : 80) : target.getPort();
        return server.getScheme().equalsIgnoreCase(target.getScheme())
            && server.getHost().equalsIgnoreCase(target.getHost()) && leftPort == rightPort;
    }

    private void openServer(String address) {
        Uri candidate = Uri.parse(address.trim());
        if (!("https".equalsIgnoreCase(candidate.getScheme()) || "http".equalsIgnoreCase(candidate.getScheme()))
            || candidate.getHost() == null || candidate.getUserInfo() != null
            || (candidate.getPath() != null && !candidate.getPath().isEmpty() && !candidate.getPath().equals("/"))
            || candidate.getQuery() != null || candidate.getFragment() != null) {
            Toast.makeText(this, "Bitte eine vollständige Serveradresse eingeben.", Toast.LENGTH_LONG).show();
            editSettings();
            return;
        }
        server = candidate;
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putString(SERVER_KEY, candidate.toString()).apply();
        browser.loadUrl(candidate.toString());
    }

    private void migratePreferences() {
        String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(SERVER_KEY, "");
        if (!saved.isEmpty()) return;
        String old = getPreferences(MODE_PRIVATE).getString(SERVER_KEY, "");
        if (!old.isEmpty()) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit().putString(SERVER_KEY, old).apply();
        }
    }

    private void editSettings() {
        startActivityForResult(new Intent(this, SettingsActivity.class), EDIT_SETTINGS);
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == CHOOSE_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        } else if (code == CAPTURE_PHOTO && fileCallback != null) {
            fileCallback.onReceiveValue(result == RESULT_OK ? new Uri[]{CAMERA_URI} : null);
            fileCallback = null;
        } else if (code == EDIT_SETTINGS) {
            String saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString(SERVER_KEY, "");
            if (!saved.isEmpty()) openServer(saved);
            else if (server == null) finish();
        }
    }

    @Override public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack(); else super.onBackPressed();
    }
}
