package com.hs.club;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** 只暴露 App 自己下载目录里的更新 APK。 */
public class ApkProvider extends ContentProvider {
    public static final String AUTHORITY_SUFFIX = ".apk";

    @Override public boolean onCreate() { return true; }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        String name = uri.getLastPathSegment();
        File f;
        try {
            f = file(name);
        } catch (FileNotFoundException e) {
            f = null;
        }
        String[] cols = projection == null
                ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}
                : projection;
        MatrixCursor c = new MatrixCursor(cols, 1);
        Object[] row = new Object[cols.length];
        for (int i = 0; i < cols.length; i++) {
            if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i] = name;
            else if (OpenableColumns.SIZE.equals(cols[i])) row[i] = f == null ? 0 : f.length();
        }
        c.addRow(row);
        return c;
    }

    @Override public String getType(Uri uri) {
        return "application/vnd.android.package-archive";
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }

    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }

    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(uri.getLastPathSegment()), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File file(String name) throws FileNotFoundException {
        File dir = getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (dir == null) dir = getContext().getFilesDir();
        // 只接受单层、安全字符的文件名：防止 ..%2F 越出下载目录读到私有文件
        String safe = name == null ? "" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        if (safe.isEmpty() || safe.contains("..")) throw new FileNotFoundException("bad name");
        File f = new File(dir, safe);
        try {
            String base = dir.getCanonicalPath();
            String path = f.getCanonicalPath();
            if (!path.equals(base) && !path.startsWith(base + File.separator)) {
                throw new FileNotFoundException("bad path");
            }
        } catch (java.io.IOException e) {
            throw new FileNotFoundException("bad path");
        }
        return f;
    }
}
