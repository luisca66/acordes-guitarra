package com.luis.acordes;

import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;

/** Read-only provider restricted to generated PNGs in the private share directory. */
public final class ImageProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    private File resolve(Uri uri) throws FileNotFoundException {
        String name=uri.getLastPathSegment();
        if(name==null || !name.matches("acordes-[0-9]+\\.png")) throw new FileNotFoundException();
        return new File(new File(getContext().getCacheDir(),"shared"),name);
    }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!mode.equals("r")) throw new FileNotFoundException("Solo lectura");
        return ParcelFileDescriptor.open(resolve(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public String getType(Uri uri) { return "image/png"; }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) {
        try {
            File f=resolve(uri); String[] columns=projection!=null?projection:new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE};
            MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];
            for(int i=0;i<columns.length;i++) values[i]=columns[i].equals(OpenableColumns.DISPLAY_NAME)?f.getName():columns[i].equals(OpenableColumns.SIZE)?f.length():null;
            cursor.addRow(values);return cursor;
        } catch(FileNotFoundException e) {return null;}
    }
    @Override public Uri insert(Uri u,ContentValues v) {throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a) {throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a) {throw new UnsupportedOperationException();}
}
