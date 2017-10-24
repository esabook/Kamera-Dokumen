package id.ac.ustj.egitsaputra.kameradokumen;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;

import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by egit saputra on 5/16/2016.
 */
public class PusatOperasi extends KontrolParameter implements Camera.PictureCallback {

    public int lebarPreviewKamera;
    public int tinggiPreviewKamera;
    public int panjangBuffer;
    public boolean _BERJALAN = false, SETTING_KAMERA = true;
    public Indikator indikator = new Indikator();
    public KoreksiSkew koreksiSkew=new KoreksiSkew();
    public List<Point> points;

    public void aturParameter(int lebarPreviewKamera, int tinggiPreviewKamera, int pBuffer, boolean setKamera) {
        this.lebarPreviewKamera = lebarPreviewKamera;
        this.tinggiPreviewKamera = tinggiPreviewKamera;
        this.panjangBuffer = pBuffer;
        this.SETTING_KAMERA = setKamera;
    }

    public Camera.Parameters P() {
        return camera.getParameters();
    }

    //sebagai method preview
    // penampil preview kamera
    public SurfaceHolder.Callback cameraPreviewCallback = new SurfaceHolder.Callback() {

        //surface penampil preview kamera dibuat
        public void surfaceCreated(SurfaceHolder holder) {
            indikator.setProses(true);
            try {
                if (camera != null) {
                    stopCameraPreview();
                }
                camera = Camera.open();
                camera.setPreviewDisplay(holder);
            } catch (Exception ex) {
                try {
                    if (camera != null) {
                        stopCameraPreview();
                    }
                } catch (Exception ex3) {
                    ex.printStackTrace();
                }
            } catch (Throwable t) {
            }
            synchronized (this) {
                if (PREVIEW_KAMERA) {
                    return;
                }
            }


        }

        @Override//surface berubah
        public void surfaceChanged(final SurfaceHolder holder, int format, int lebar, int tinggi) {
            Camera.Parameters parameters = P();
            while (SETTING_KAMERA) {
                Camera.Size size = aturUkuranPreview(lebar, tinggi, parameters);
                parameters.setPreviewFormat(ImageFormat.NV21);
                camera.setParameters(parameters);
                PixelFormat p = new PixelFormat();
                PixelFormat.getPixelFormatInfo(parameters.getPreviewFormat(), p);
                if (FPS) {
                    aturFPSPreview();
                }

                if (AUTO_FOKUS) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                    AUTO_FOKUS = false;
                }
                if (size != null) {
                    camera.setDisplayOrientation(90);
                    camera.setParameters(parameters);
                }
                if (ATUR_DIMENSI_PREVIEW) {
                    aturDimensiPenampilPreview();
                }
                lebarPreviewKamera = parameters.getPreviewSize().width;
                tinggiPreviewKamera = parameters.getPreviewSize().height;
                panjangBuffer = (lebarPreviewKamera * tinggiPreviewKamera * p.bitsPerPixel) / 8;
                aturParameter(lebarPreviewKamera, tinggiPreviewKamera, panjangBuffer, false);
            }
            final byte[] buffer = new byte[panjangBuffer];
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    synchronized (holder) {
                        if (PREVIEW_KAMERA) {
                            ImageView m = (ImageView) findViewById(R.id.indikator);
                            cameraPreview.invalidate();
                            if (indikator.getProses()) {
                                indikator.setData(data, lebarPreviewKamera, tinggiPreviewKamera);
                            }
                            if (indikator.getIndikator() != null) {
                                m.setImageBitmap(indikator.getIndikator());
                            }

                        }
                        camera.addCallbackBuffer(buffer);
                    }
                }
            });
            camera.startPreview();
            PREVIEW_KAMERA = true;
        }

        //surface dihapus
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            if (camera != null) {
                try {
                    stopCameraPreview();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                camera = null;
            }
        }
    };

    @Override
    //sebagai method potret
    //ambil alih operasi onPictureTaken milik kelas android.hardware.Camera
    public void onPictureTaken(byte[] data, Camera camera) {
        koreksiSkew.setDimensiAsalIndikator(lebarPreviewKamera, tinggiPreviewKamera);
        koreksiSkew.setPojok(points);
        Bitmap bitDecode = BitmapFactory.decodeByteArray(data, 0, data.length);
        Bitmap bitmap=koreksiSkew.koreksi(bitDecode);
        if (bitmap!=null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
            onPictureTaken(bitmap);
        }

        else {
            dialogKesalahan(this, getString(R.string.gagal_koreksi),7);
            Bitmap bitmapAlt = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap oldBitmap = bitmapAlt;
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            bitmapAlt = Bitmap.createBitmap(bitmapAlt, 0, 0, bitmapAlt.getWidth(), bitmapAlt.getHeight(), matrix, false);
            oldBitmap.recycle();
            onPictureTaken(bitmapAlt);
        }

    }

    //sebagai method simpan
    // konversi bitmap ke jpg dan menyimpan menjadi file
    public void onPictureTaken(Bitmap bitmap) {
        File lokasiSimpan = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM
                ),
                getString(R.string.nama_aplikasi)
        );

        if (!lokasiSimpan.exists()) {
            if (!lokasiSimpan.mkdirs()) {
                return;
            }
        }

        String waktu = new SimpleDateFormat(getString(R.string.format_tanggal)).format(new Date());
        File fileCitra = new File(lokasiSimpan.getPath() + File.separator + getString(R.string.nama_awal_file) + waktu + ".jpg");
        try {
            FileOutputStream stream = new FileOutputStream(fileCitra);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        } catch (IOException exception) {
            dialogKesalahan(this, getString(R.string.gagal_simpan), 5);
            return;
        }

        refreshMediaDb(fileCitra);
        Intent intent = new Intent(this, TampilanPratinjau.class);
        intent.setData(Uri.fromFile(fileCitra));
        startActivity(intent);
    }

    //refresh database media-store; agar file terindeks
    public void refreshMediaDb(File file) {
        MediaScannerConnection.scanFile(
                this,
                new String[]{file.toString()},
                new String[]{"image/jpeg"},
                null
        );
    }

    //untuk hentikan service kamera
    public synchronized void stopCameraPreview() {
        try {
            camera.setPreviewCallbackWithBuffer(null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            camera.stopPreview();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            camera.release();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try {
            camera = null;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public interface CameraFragmentListener {
        void onCameraError();
    }
}
