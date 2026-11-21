package at.fotoupload.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int CHOOSE_FILE = 100;
    private static final int CAPTURE_PHOTO = 101;
    private static final Uri CAMERA_URI = Uri.parse("content://at.fotoupload.app.camera/capture.jpg");
    private WebView browser;
    private ValueCallback<Uri[]> fileCallback;
    private Uri server;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        Button settings = new Button(this);
        settings.setText("Serveradresse ändern");
        settings.setOnClickListener(view -> editServer());
        layout.addView(settings);
        browser = new WebView(this);
        layout.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(layout);
        WebSettings web = browser.getSettings();
        web.setJavaScriptEnabled(true);
        web.setUserAgentString(web.getUserAgentString() + " FotoUploadAndroid/1");
        web.setDomStorageEnabled(true);
        web.setAllowFileAccess(false);
        web.setAllowContentAccess(true);
        browser.setWebViewClient(new WebViewClient() {
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
        String saved = getPreferences(MODE_PRIVATE).getString("server", "");
        if (saved.isEmpty()) editServer(); else openServer(saved);
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
            editServer();
            return;
        }
        server = candidate;
        getPreferences(MODE_PRIVATE).edit().putString("server", candidate.toString()).apply();
        browser.loadUrl(candidate.toString());
    }

    private void editServer() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("https://fotos.example.org");
        input.setText(getPreferences(MODE_PRIVATE).getString("server", ""));
        new AlertDialog.Builder(this).setTitle("Serveradresse")
            .setMessage("Gib die Adresse deines Foto-Upload-Servers ein.")
            .setView(input).setPositiveButton("Öffnen", (dialog, which) -> openServer(input.getText().toString()))
            .setNegativeButton("Abbrechen", (dialog, which) -> { if (server == null) finish(); }).show();
    }

    @Override protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (code == CHOOSE_FILE && fileCallback != null) {
            fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data));
            fileCallback = null;
        } else if (code == CAPTURE_PHOTO && fileCallback != null) {
            fileCallback.onReceiveValue(result == RESULT_OK ? new Uri[]{CAMERA_URI} : null);
            fileCallback = null;
        }
    }

    @Override public void onBackPressed() {
        if (browser.canGoBack()) browser.goBack(); else super.onBackPressed();
    }
}
