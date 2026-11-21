package at.fotoupload.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

public class CameraProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"/capture.jpg".equals(uri.getPath())) throw new FileNotFoundException();
        File image = new File(getContext().getCacheDir(), "capture.jpg");
        return ParcelFileDescriptor.open(image, ParcelFileDescriptor.parseMode(mode));
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if (!"/capture.jpg".equals(uri.getPath())) return null;
        String[] columns = projection != null ? projection : new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
        MatrixCursor result = new MatrixCursor(columns);
        MatrixCursor.RowBuilder row = result.newRow();
        File image = new File(getContext().getCacheDir(), "capture.jpg");
        for (String column : columns) {
            if (OpenableColumns.DISPLAY_NAME.equals(column)) row.add("kamera.jpg");
            else if (OpenableColumns.SIZE.equals(column)) row.add(image.length());
            else row.add(null);
        }
        return result;
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
