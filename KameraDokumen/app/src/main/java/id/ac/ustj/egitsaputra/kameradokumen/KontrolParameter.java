package id.ac.ustj.egitsaputra.kameradokumen;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageButton;
import java.util.List;

/**
 * Created by egit saputra on 5/17/2016.
 */
public class KontrolParameter extends Activity {

    public Camera camera;
    public SurfaceView cameraPreview;
    public SurfaceHolder previewHolder;
    public int cameraId;
    public long tGaleri;
    public static boolean
            PREVIEW_KAMERA = false,
            ATUR_DIMENSI_PREVIEW = true,
            AUTO_FOKUS=true,
            FPS=true,
            GALERI=true;

    PusatOperasi.CameraFragmentListener listener = null;

    //cek ketersediaan kamera depan
    boolean cekKamera(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            return true;
        }else {
            return false;
        }
    }

    //cek ketersediaan lampu flash
    boolean cekFlash(){
        if(getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)){
            return true;
        }else {
            return false;
        }
    }

    //membaca ketersediaan kemampuan preview pada kamera dan menentukan dimensi preview yang cocok
    //untuk ditampilkan. Dengan nilai dimensi penampil : dimensi layar : dimensi preview kamera = lebar layar;
    public Camera.Size aturUkuranPreview(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return (result);
    }

    //untuk menetukan ukuran optimal untuk ukuran gambar (potret)
    public Camera.Size aturUkuranGambar(int width, int height){
        List<Camera.Size> sizes =camera.getParameters().getSupportedPictureSizes();
        Camera.Size size=sizes.get(0);
        for (int i=0; i< sizes.size();i++){
            float rasioUkuranPreview=width/height;
            float rasioKandidatHasil=sizes.get(i).width/sizes.get(i).height;
            boolean ukuran=sizes.get(i).width> size.width;
            boolean rasio=rasioKandidatHasil == rasioUkuranPreview;
            if (ukuran && rasio)
                size=sizes.get(i);
        }
        return size;
    }

    //atur range FPS (kecepatan preview kamera)
    public void aturFPSPreview() {
        Camera.Parameters parameters = camera.getParameters();
        int[] best = {Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int[] range : parameters.getSupportedPreviewFpsRange()) {
            if (range[0] > best[0]) {
                best = range;
            }
        }
        parameters.setPreviewFpsRange(best[0], best[1]);
        camera.setParameters(parameters);
    }

    //atur dimensi penampil preview kamera sesuai rasio dimensi preview dan layar
    public void aturDimensiPenampilPreview(){
        Display display = getWindowManager().getDefaultDisplay();
        cameraPreview = (SurfaceView) findViewById(R.id.previewCamera);
        ViewGroup.LayoutParams lp = cameraPreview.getLayoutParams();
        int Cwidth = camera.getParameters().getPreviewSize().width;
        int Cheight = camera.getParameters().getPreviewSize().height;
        lp.width = display.getWidth();
        lp.height = Cwidth*display.getWidth()/Cheight;
        cameraPreview.setLayoutParams(lp);
        ATUR_DIMENSI_PREVIEW = false;

    }

    //atur dimensi tombol potret
    public void aturDimensiTCapture(){
        ImageButton imageButton=(ImageButton)findViewById(R.id.TCapture);
        android.view.ViewGroup.LayoutParams tC=imageButton.getLayoutParams();
        int cpH=cameraPreview.getHeight();
        Display display=getWindowManager().getDefaultDisplay();
        int displayH=display.getHeight();
        int displayW=display.getWidth();
        tC.width=displayW;
        tC.height=displayH-cpH;
        int maxH=displayH*25/100;
        int minH=displayH*10/100;
        if (tC.height > maxH){
            tC.height= maxH;
        }
        else if (tC.height<minH){
            tC.height=minH;
        }
        imageButton.setLayoutParams(tC);
    }

    //menampilkan dialog kesalahan
    public void dialogKesalahan(final Context context, final String s, final int i){
        final int o=i;
        final AlertDialog.Builder builder=new AlertDialog.Builder(context, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
        builder.setMessage(s).setTitle(R.string.Kesalahan).setCancelable(true);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (o) {
                    case 1: {
                        Intent intent = new Intent(context, TampilanUtama.class);
                        int pid=android.os.Process.myPid();
                        stopService(intent);
                        onPause();
                        onDestroy();
                        onBackPressed();
                        android.os.Process.killProcess(pid);
                        System.exit(0);
                        finish();
                        finishAndRemoveTask();
                        break;
                    }
                    case 2: {
                        Intent intent = new Intent(context, TampilanPratinjau.class);
                        stopService(intent);
                        break;
                    }
                    case 3: {                        break;                    }
                    case 4: {                        break;                    }
                    case 5: {                        break;                    }
                    case 6: {                        break;                    }
                    case 7: {                        break;                    }
                }
                dialog.cancel();
            }
        });
        if (1<i) {
            builder.setNegativeButton(R.string.cobalagi, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (o) {
                        case 2: {
                            ImageButton imageButton=(ImageButton)findViewById(R.id.THapus);
                            imageButton.callOnClick();
                            break;
                        }
                        case 3: {
                            ImageButton imageButton=(ImageButton)findViewById(R.id.TFlash);
                            imageButton.callOnClick();
                            break;
                        }
                        case 4: {
                            ImageButton imageButton=(ImageButton)findViewById(R.id.TGaleri);
                            imageButton.callOnClick();
                            break;
                        }
                        case 5: {
                            ImageButton imageButton=(ImageButton)findViewById(R.id.TCapture);
                            imageButton.callOnClick();
                            dialog.dismiss();
                            break;
                        }
                        case 6: {
                            TampilanUtama tu=new TampilanUtama();
                            Intent intent=new Intent(context, TampilanUtama.class);
                            startActivity(intent);
                            tu.finish();
                            break;
                        }
                        case 7: {break;}
                    }
                    dialog.dismiss();
                }
            });
        }
        AlertDialog dialog=builder.create();
        dialog.show();
    }
}
