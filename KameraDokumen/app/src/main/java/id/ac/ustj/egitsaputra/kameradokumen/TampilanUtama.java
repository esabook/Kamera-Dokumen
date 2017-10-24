package id.ac.ustj.egitsaputra.kameradokumen;

        import android.content.Intent;
        import android.hardware.Camera;
        import android.os.Bundle;
        import android.os.SystemClock;
        import android.util.Log;
        import android.view.SurfaceHolder;
        import android.view.SurfaceView;
        import android.view.View;
        import android.widget.ImageButton;
        import android.widget.ImageView;
        import android.widget.ProgressBar;
        import android.widget.Toast;

        import org.opencv.android.OpenCVLoader;
        import java.util.List;
        import java.util.concurrent.ExecutionException;


public class TampilanUtama extends PusatOperasi {

    //state onCreate
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tampilan_utama);
        mulai();
        if (!OpenCVLoader.initDebug()){
            Log.i(this.getClass().getSimpleName(), "OpenCV berfungsi");
        }else{
            Log.i(this.getClass().getSimpleName(), "OpenCV tidak berfungsi");
        }
    }

    //state onPause
    protected void onPause(){
        if (PREVIEW_KAMERA){
            stopCameraPreview();
            PREVIEW_KAMERA =false;
        }
        super.onPause();
    }

    //state onresume
    protected void onResume(){
        super.onResume();
        indikator.reset();
        indikator.setPotret(false);
        ImageView m=(ImageView)findViewById(R.id.indikator);
        m.setVisibility(View.VISIBLE);
        indikator.setLangkah(0);
        loading(false);
        aktifkanTCapture(true);
        AUTO_FOKUS=true;
        SETTING_KAMERA =true;
        ATUR_DIMENSI_PREVIEW =true;

        try {
            camera = Camera.open(cameraId);
        } catch (Exception exception) {
            dialogKesalahan(this, getString(R.string.gagal_buka_kamera),6);
            return;
        }


    }

    //awal operasi
    protected void mulai() {
        indikator.setLangkah(0);
        ImageButton TCapture = (ImageButton) findViewById(R.id.TCapture);
        ImageButton TFlash = (ImageButton) findViewById(R.id.TFlash);
        if (!cekKamera()) {
            TCapture.setEnabled(false);
        }
        if (!cekFlash()) {
            TFlash.setEnabled(false);
        }
        aktifkanTCapture(true);
        cameraPreview=(SurfaceView)findViewById(R.id.previewCamera);
        PREVIEW_KAMERA = false;
        previewHolder=cameraPreview.getHolder();
        previewHolder.addCallback(cameraPreviewCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        aturDimensiTCapture();

    }

    //disable tombol TCapture
    public void aktifkanTCapture(boolean b){
        ImageButton imageButton=(ImageButton)findViewById(R.id.TCapture);
        imageButton.setEnabled(b);
        return;
    }

    //operasi tombol potret (capture)
    public void TCapture(View view) throws ExecutionException, InterruptedException {
        ambilGambar();
        aktifkanTCapture(false);
    }

    //operasi tombol TFlash
    public void TFlash(View view) {
        Camera.Parameters parameters = camera.getParameters();
        ImageView TFlash = (ImageView) findViewById(R.id.TFlash);
        List<String> modeFlash = parameters.getSupportedFlashModes();
        String flashSaatIni = parameters.getFlashMode();
        if (modeFlash.get(3).matches(flashSaatIni)){
            parameters.setFlashMode(modeFlash.get(0));
        }else {
            for (int i = 0; i < modeFlash.size() - 1; i++)
                if (modeFlash.get(i).matches(flashSaatIni)) {
                    parameters.setFlashMode(modeFlash.get(i + 1));
                }
        }
        camera.setParameters(parameters);
        String s=parameters.getFlashMode();
        if (s.contains("on")) {TFlash.setImageResource(R.drawable.flash_on);}
        else if (s.contains("auto")) {TFlash.setImageResource(R.drawable.flash_auto);}
        else if (s.contains("off")) {TFlash.setImageResource(R.drawable.flash_off);}
        else {TFlash.setImageResource(R.drawable.flash_torch);}
    }

    //operasi ambil gambar
    public void ambilGambar() throws ExecutionException, InterruptedException {
        indikator.setPotret(true);
        points=indikator.indikatoTerakhir();
        PREVIEW_KAMERA =false;
        ImageView m=(ImageView)findViewById(R.id.indikator);
        m.setVisibility(View.GONE);
        loading(true);
        Camera.Parameters parameters=camera.getParameters();
        parameters.setJpegQuality(100);
        int W=parameters.getPreviewSize().width;
        int H=parameters.getPreviewSize().height;
        Camera.Size gambar= aturUkuranGambar(W, H);
        parameters.setPictureSize(gambar.width, gambar.height);
        camera.setParameters(parameters);
        camera.takePicture(null, null, this);
        _BERJALAN =false;
    }

    //kendali tampilan loading
    public void loading(boolean b){
        ImageView i = (ImageView) findViewById(R.id.block);
        ProgressBar p = (ProgressBar) findViewById(R.id.progressBar);
        if (b) {
            p.setVisibility(View.VISIBLE);
            i.setVisibility(View.VISIBLE);
        }else {
            p.setVisibility(View.GONE);
            i.setVisibility(View.GONE);
        }
    }

    //operasi saat imageview blok ditekan
    public void blok(View view){
        Toast.makeText(this, getString(R.string.sedang_simpan), Toast.LENGTH_SHORT).show();
    }

    //tombol buka galeri
    public void TGaleri(View view){

        if (GALERI) {
            tGaleri = System.currentTimeMillis();
            Intent galeri=new Intent();
            galeri.setAction(android.content.Intent.ACTION_VIEW);
            galeri.setType("image/*");
            galeri.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(galeri);
            }
            catch (Exception e){
                e.printStackTrace();
                dialogKesalahan(this, getString(R.string.gagal_buka_galeri), 4);
            }
            GALERI=false;
        }
        else {
            if ((System.currentTimeMillis()-tGaleri)>300){
                GALERI=true;
            }
            else {
                Toast.makeText(this, getString(R.string.sedang_membuka_galeri), Toast.LENGTH_SHORT).show();
            }
        }

    }

}
