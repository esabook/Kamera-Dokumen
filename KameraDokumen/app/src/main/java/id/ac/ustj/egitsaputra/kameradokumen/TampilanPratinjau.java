package id.ac.ustj.egitsaputra.kameradokumen;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import id.ac.ustj.egitsaputra.kameradokumen.HandleZoom.ImageLoader;
import id.ac.ustj.egitsaputra.kameradokumen.HandleZoom.ImageViewTouch;

import java.io.File;
import java.io.IOException;

public class TampilanPratinjau extends PusatOperasi {
    Uri uriFile;
    private ImageViewTouch mImageView;

    //state onCreate
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tampilan_pratinjau);
        uriFile = getIntent().getData();
        Cursor c = getContentResolver().query( MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null );
        if ( c != null ) {
            int count = c.getCount();
            int position = (int)( Math.random() * count );
            if ( c.moveToPosition( position ) ) {
                int orientation = c.getInt(c.getColumnIndex(MediaStore.Images.Media.ORIENTATION));
                Bitmap bitmap;
                try {
                    bitmap = ImageLoader.loadFromUri(this, uriFile.toString(), 1024, 1024);
                    mImageView.setImageBitmapReset( bitmap, orientation, true );
                }
                catch ( IOException e ) {
                    Toast.makeText( this, e.toString(), Toast.LENGTH_LONG ).show();
                }
            }
            c.close();
            c = null;
            return;
        }
        mImageView.setImageURI(uriFile);
    }

    //state onResume
    protected void onResume(){
        super.onResume();
    }

    //operasi tombol simpan
    public void TSimpan(View view){
        File file= new File(uriFile.getPath());
        if (file.exists()) {
            Toast.makeText(this, getString(R.string.telah_berhasil_disimpan), Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, getString(R.string.gagal_simpan), Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    //operasi pada tombol hapus
    public void THapus(View view) throws IOException {

        AlertDialog dialog;
        AlertDialog.Builder builder=new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setMessage(R.string.p_menghapus).setTitle(R.string.Pertanyyan).setCancelable(true);
        builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                hapus();
                finish();
            }
        });
        builder.setNegativeButton(getString(R.string.batal), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        dialog=builder.create();
        dialog.show();

    }

    //operasi hapus
    public void hapus() {

        File file= new File(uriFile.getPath());
        if (file.exists()){
            try {
                file.getAbsoluteFile().delete();
            }catch (Exception e){}
            if (file.exists()){
                try {
                    file.getCanonicalFile().delete();
                }catch (Exception e){}
                if (file.exists()){
                    try {
                        getApplicationContext().deleteFile(file.getName());
                    }catch (Exception e){}
                    if (file.exists()){
                        try {
                            file.getAbsoluteFile().delete();
                        }catch (Exception e){}
                        if (file.exists()){
                            try {
                                file.delete();
                            }catch (Exception e){}
                        }
                    }
                }
            }
            mImageView.clear();
        }
        if (file.exists()){
            dialogKesalahan(this, getString(R.string.gagal_hapus), 2);
        }else {
            Toast.makeText(this, getString(R.string.telah_berhasil_dihapus), Toast.LENGTH_SHORT).show();
        }
        refreshMediaDb(file);
    }

    @Override//ambil alih operasi tombol kembali
    public void onBackPressed(){
        Toast.makeText(this, getString(R.string.Tombol_back), Toast.LENGTH_SHORT).show();
        ImageButton tHapus=(ImageButton)findViewById(R.id.THapus);
        tHapus.performClick();
        return;
    }

    @Override//ambil alih penampil pratinjau
    public void onContentChanged() {
        super.onContentChanged();
        mImageView = (ImageViewTouch)findViewById(R.id.penampilPhoto);
    }

}
