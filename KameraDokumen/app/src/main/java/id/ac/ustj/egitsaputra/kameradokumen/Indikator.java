package id.ac.ustj.egitsaputra.kameradokumen;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.AsyncTask;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;

/**
 * Created by egit saputra on 5/25/2016.
 */
public class Indikator {

    private static final int banyak_garis = 8; // operasi sudut dilaksanakan jika nilai titik yang terdeteksi mencapai nilai ini.
    private static final int aKiriX = 0; //nilai untuk mereset indikator, dan nilai selebihnya adalah membaca parameter perangkat
    private static final int bKiriX = 0;
    private static final int aKiriY = 0;
    private static final int aKananY = 0;
    private static final int limitPoly=10; //nilai minimal panjang sisi .

    private int atasKiriX;
    private int atasKiriY;
    private int atasKananX;
    private int atasKananY;
    private int bawahKiriX;
    private int bawahKiriY;
    private int bawahKananX;
    private int bawahKananY;

    private Bitmap indikator;
    private int[] dataPiksel;
    private int[] pikselTepian;
    private int[] pikselHough;
    private int lebar;
    private int tinggi;
    private int langkah, hash1=0, hash2=0;
    private long tandaWaktuMulai, tandaWaktu=0, tandaWaktuBerakhir=500;
    private boolean _GRAYSCALING=false, _CANNY=false, _HOUGH=false;
    private boolean blink=true;
    private boolean _PROSESINDIKATOR=false, _POTRET =false;
    private List<Point> titik, titikPojok=new ArrayList<>();

    Canny canny;
    HoughTrasnsform sht;
    KoreksiSkew ks;

    public Bitmap getIndikator() {
            return indikator;
    }
    public boolean getProses() {
        return _PROSESINDIKATOR;
    }
    public void setProses(boolean b) {
        _PROSESINDIKATOR=b;
    }
    public void setPotret(boolean b) {
        _POTRET =b;
    }
    public void setLangkah(int langkah) {
        this.langkah=langkah;
    }

    //konstruktor
    public Indikator(){
        this.bawahKiriX = bKiriX;
        this.bawahKiriY = bawahKiriX;
        this.atasKiriX = bawahKiriX;
        this.bawahKananY = bawahKiriX;
        canny=new Canny();
        sht =new HoughTrasnsform();
        ks=new KoreksiSkew();

    }

    //getter titik pojok untuk  koreksi skew
    public List<Point> indikatoTerakhir(){
        Point satu=new Point(atasKiriX, atasKiriY);
        Point dua=new Point(atasKananX, atasKananY);
        Point tiga=new Point(bawahKananX, bawahKananY);
        Point empat=new Point(bawahKiriX, bawahKiriY);

        List<Point> p=new ArrayList<>();
        p.add(satu); p.add(dua); p.add(tiga); p.add(empat);
        return p;
    }

    //menerima data frame kamera
    public void setData(byte[] data, int lebar, int tinggi) {

        this.atasKiriY = tinggi;
        this.atasKananX = lebar;
        this.atasKananY = tinggi;
        this.bawahKananX = lebar;
        this.lebar = lebar;
        this.tinggi = tinggi;

        if (!_PROSESINDIKATOR) {
            boolean c=canny.getStatusBerjalan();
            boolean h=sht.getStatusBerjalan();

            switch (langkah) {
                case 0: {
                    if (!_GRAYSCALING) {
                        new satu().execute(data);
                        langkah = 1;
                        break;
                    }
                }
                case 1: {
                        if (dataPiksel != null) {
                            if (!c || !_CANNY){
                            new dua().execute(dataPiksel);
                            langkah = 2;
                            break;
                            }
                        }
                }
                case 2: {
                        if (pikselTepian != null) {
                            if(!h || !_HOUGH) {
                                new tiga().execute(pikselTepian);
                                langkah = 0;
                                break;
                            }
                        }
                }
            }
        }
        if (pikselHough !=null) {
            new empat().execute(pikselHough);
        }
//        int totalPiksel = lebar * tinggi;
//        int[] pixel = new int[totalPiksel];
//        for (int i = 0; i < totalPiksel; i++) {
//            int luminanxe = data[i] & 0xff;
//            pixel[i] = Color.argb(0xff, luminanxe, luminanxe, luminanxe);
//        }
//        dataPiksel=pixel;
//            if ( dataPiksel!=null) {
//                canny.setDataPiksel(dataPiksel, lebar, tinggi);
//                canny.process();
//            }
        ks.setPiksel(data, lebar, tinggi);
        this.titik=ks.getPojok();
        lima();
        if (!_POTRET) {
            if (tandaWaktu == 0) {
                tandaWaktuMulai = System.currentTimeMillis();
                hash1 = titikPojok.hashCode();
                tandaWaktu = 1;
            }
            long t = System.currentTimeMillis() - tandaWaktuMulai;
            if ((t > tandaWaktuBerakhir) && (tandaWaktu == 1)) {
                hash2 = titikPojok.hashCode();
                if (hash1 == hash2) {
                    titikPojok.clear();
                    reset();
                }
                tandaWaktu = 0;
            } else {
                rancangGaris(titikPojok);
            }
        }

    }



    //menjalankan operasi aras keabuan
    protected class satu extends AsyncTask<byte[], Boolean, int[] >{
        @Override
        protected void onPreExecute(){
            _PROSESINDIKATOR=true;
            _GRAYSCALING=true;
        }
        @Override
        protected void onCancelled(){
            cancel(true);
        }

        @Override
        protected int[] doInBackground(byte[]... params) {

            byte[] data=params[0];
            int totalPiksel = lebar * tinggi;
            int[] pixel = new int[totalPiksel];
            for (int i = 0; i < totalPiksel; i++) {
                int luminanxe = data[i] & 0xff;
                pixel[i] = Color.argb(0xff, luminanxe, luminanxe, luminanxe);
            }

            return pixel;
        }

        @Override
        protected void onPostExecute (int[] result){
            dataPiksel = result;
            _GRAYSCALING=false;
            _PROSESINDIKATOR=false;
        }
    }

    //menjalankan operasi canny
    protected class dua extends AsyncTask<int[], Boolean, int[] >{
        @Override
        protected void onPreExecute(){
            _CANNY=true;
        }
        @Override
        protected void onCancelled(){
            cancel(true);
        }

        @Override
        protected int[] doInBackground(int[]... params) {

            int[] pixel=params[0];
            canny.setDataPiksel(pixel, lebar, tinggi);
            canny.process();
            pixel=canny.getPikselTepi();
            return pixel;
        }

        @Override
        protected void onPostExecute (int[] result){
            pikselTepian =result;
            _PROSESINDIKATOR=false;
            _CANNY=false;
        }
    }

    //operasi Hough Transform
    protected class tiga extends AsyncTask<int[], Void, int[]>{
        @Override
        protected void onPreExecute(){
            _HOUGH=true;
        }

        @Override
        protected void onCancelled(){
            cancel(true);
        }

        @Override
        protected int[] doInBackground(int[]... params) {
            int[] pixel=params[0];
            sht.setPikselInput(pixel, lebar, tinggi);
            int[] hough= sht.process();
            return hough;
        }

        @Override
        protected void onPostExecute(int[] result){
            pikselHough =result;
            _HOUGH=false;
            _PROSESINDIKATOR=false;
        }

    }

    //menjalankan operasi pengambilan titik skew
    protected class empat extends AsyncTask<int[], Void, Bitmap>{

        @Override
        protected void onCancelled(){
            cancel(true);
        }

        @Override
        protected Bitmap doInBackground(int[]... params) {
            int[] pix=params[0];
            ArrayList<Integer> X = new ArrayList<>();
            ArrayList<Integer> Y = new ArrayList<>();
            int toleran=30;
            int[] tempX={-toleran,toleran,toleran,-toleran};
            int[] tempY={-toleran,-toleran,toleran,toleran};

            Bitmap bitmap=Bitmap.createBitmap(tinggi, lebar, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            Paint paint=new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setAlpha(100);

            if (pix!=null) {
               // Log.i("Empat ","Cari dan buat sudut ========================================");
                for (int i = 1; i < lebar - 1; i++) {
                    for (int j = 1; j < tinggi - 1; j++) {
                        int titik = i + lebar * j;
                        if ((pix[titik] & 0xff )== 255) {

                            int atas = titik + lebar,
                                    bawah = titik - lebar,
                                    kanan = titik + 1, kiri = titik - 1,
                                    kananAtas = atas + 1,
                                    kananBawah = bawah + 1,
                                    kiriAtas = atas - 1,
                                    kiriBawah = bawah - 1;
                            boolean plus = (pix[atas] == pix[bawah]) == (pix[kanan] == pix[kiri]);
                            boolean silang = (pix[kananAtas] == pix[kiriBawah]) == (pix[kiriAtas] == pix[kananBawah]);
                            if (silang || plus) {
                                X.add(i);
                                Y.add(j);
                            }
                        }
                    }
                }

                for (int i = 0; i < X.size(); i++) {
                    X.set(i, (X.get(i) - (lebar / 2)));
                    Y.set(i, (Y.get(i) - (tinggi / 2)));
                }


                for (int i = 0; i < X.size(); i++) {
                    int x = X.get(i);
                    int y = Y.get(i);
                    if ((x < tempX[0]) && (y < tempY[0])) {
                        tempX[0] = x;
                        tempY[0] = y;
                    }
                    if ((x > tempX[1]) && (y < tempY[1])) {
                        tempX[1] = x;
                        tempY[1] = y;
                    }
                    if ((x > tempX[2]) && (y > tempY[2])) {
                        tempX[2] = x;
                        tempY[2] = y;
                    }
                    if ((x < tempX[3]) && (y > tempY[3])) {
                        tempX[3] = x;
                        tempY[3] = y;
                    }
                }

                for (int i = 0; i < tempX.length; i++) {
                    tempX[i] = tempX[i] + (lebar / 2);
                    tempY[i] = tempY[i] + (lebar / 2);
                }
                paint.setColor(0x7700ff00);

            }
            else {
                if (blink){paint.setColor(0x77ff0000);blink=false;}
                else {paint.setColor(0x77550000);blink=true;}
                tempX = new int[]{5, lebar - 5, lebar - 5, 5};
                tempY = new int[]{5, 5, tinggi - 5, tinggi - 5};
            }

            paint.setStrokeWidth(6);
            Path p=new Path();
            p.moveTo(5,5);
            p.lineTo(tempY[0], lebar-tempX[0]);
            p.lineTo(tempY[1], lebar-tempX[1]);
            p.lineTo(tempY[2], lebar-tempX[2]);
            p.lineTo(tempY[3], lebar-tempX[3]);
            p.lineTo(tempY[0], lebar-tempX[0]);
            canvas.drawPath(p, paint);
           // Log.i("EMPAT ","Gambar sudut");
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap result){
            indikator=result;
            _PROSESINDIKATOR=false;

        }
    }

    //sortir titik sudut; menemukan 4 titik
    private void lima() {
        if (titik.size() > banyak_garis) {

            //hitung titik tengah dari kumpulan titik
            Point tengah = new Point(0, 0);
            titikPojok.clear();

            for (int x = 0; x < titik.size(); x++) {
                tengah.x += titik.get(x).x;
                tengah.y += titik.get(x).y;
            }

            tengah.x = (tengah.x / titik.size());
            tengah.y = (tengah.y / titik.size());

            ArrayList<Point> titikBawah = new ArrayList<>();
            ArrayList<Point> titikAtas = new ArrayList<>();

            //pisah titik menjadi titik atas dan bawah
            for (int i = 0; i < titik.size(); i++) {
                if (tengah.y < titik.get(i).y) {
                    titikBawah.add(titik.get(i));
                } else {
                    titikAtas.add(titik.get(i));
                }
            }

            //sortir dan pisahkan kumpulan titik bawah/atas menjadi dua bagian kanan dan bagian kiri
            if (titikBawah.size() > 0 && titikAtas.size() > 0) {
                Collections.sort(titikBawah, new Comparator<Point>() {
                    @Override
                    public int compare(Point a, Point b) {
                        int hasil=Double.compare(a.x, b.x);
                        if (hasil==0){
                            hasil=Double.compare(a.y, b.y);
                        }
                        return hasil;
                    }
                });
                Collections.sort(titikAtas, new Comparator<Point>() {
                    @Override
                    public int compare(Point a, Point b) {
                        int hasil=Double.compare(a.x, b.x);
                        if (hasil==0){
                            hasil=Double.compare(a.y, b.y);
                        }
                        return hasil;
                    }
                });


                titikPojok.add(new Point(titikAtas.get(0).x, titikAtas.get(0).y)); //|_
                titikPojok.add(new Point(titikAtas.get(titikAtas.size()-2).x, titikAtas.get(titikAtas.size()-2).y)); //_| 2
                titikPojok.add(new Point(titikBawah.get(titikBawah.size()-1).x, titikBawah.get(titikBawah.size()-1).y)); //-| 4
                titikPojok.add(new Point(titikBawah.get(0).x, titikBawah.get(0).y)); //|- 3
            }
        }
    }

    private double hitungSudutPojok(int i, int j, int k) {
        double a=Math.atan2(titik.get(j).y-titik.get(i).y, titik.get(j).x-titik.get(i).x);
        double b=Math.atan2(titik.get(k).y-titik.get(i).y, titik.get(k).x-titik.get(i).x);
        return a-b;
    }

    //operasi rancang garis indikator deteksi
    private void rancangGaris(List<Point> titik){
        if (titik.size()>=3 && !_POTRET) {
                atasKiriX = (int) titik.get(0).x;
                atasKiriY = (int) titik.get(0).y;
                atasKananX = (int) titik.get(1).x;
                atasKananY = (int) titik.get(1).y;
                bawahKananX = (int) titik.get(2).x;
                bawahKananY = (int) titik.get(2).y;
                bawahKiriX = (int) titik.get(3).x;
                bawahKiriY = (int) titik.get(3).y;

                if (limitPoly > radius(atasKiriX,
                        atasKiriY,
                        bawahKananX,
                        bawahKananY) && limitPoly > radius(bawahKiriX,
                        bawahKiriY,
                        atasKananX,
                        atasKananY)) {
                    reset();
                } else {
                    if (cekDimensi()) {
                        kontruksiGaris();
                    }
                }

        }
        else {
            reset();
        }
    }

    //untuk cek panjang sisi lebar dan sisi tinggi
    private boolean cekDimensi() {
        int a = atasKananX - atasKiriX;
        int b = bawahKananX - bawahKiriX;
        int c = bawahKananY-atasKananY;
        int d = bawahKiriY-atasKiriY;

        return a > limitPoly && b > limitPoly && c > limitPoly && d > limitPoly;
    }

    //buat garis indikator
    private void kontruksiGaris() {
        Bitmap temp = Bitmap.createBitmap(lebar+1, tinggi+1, Bitmap.Config.ARGB_8888);
        if (lebar>0 && tinggi>0) {
            temp = Bitmap.createBitmap(lebar, tinggi, Bitmap.Config.ARGB_8888);
        }
        Canvas canvas = new Canvas(temp);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAlpha(150);

        Path p = new Path();
        p.moveTo(atasKiriX, atasKiriY);
        p.lineTo(atasKiriX, atasKiriY);
        p.lineTo(atasKananX, atasKananY);
        p.lineTo(bawahKananX, bawahKananY);
        p.lineTo(bawahKiriX, bawahKiriY);
        p.lineTo(atasKiriX, atasKiriY);

        canvas.drawPath(p, paint);

        Matrix m = new Matrix();
        m.postRotate(90);
        indikator = Bitmap.createBitmap(temp, 0, 0, temp.getWidth(), temp.getHeight(), m, true);
        temp.recycle();

    }

    //radius titik xy
    private static float radius(double ax, double ay, double bx, double by) {
        return (float) (((ax - bx) * (ax - bx)) + ((ay - by) * (ay - by)));
    }

    //reset garis indikator
    void reset() {
        atasKiriX = aKiriX;
        atasKiriY = aKiriY ;
        atasKananX = lebar;
        atasKananY = aKananY;
        bawahKiriX = bKiriX;
        bawahKiriY = tinggi;
        bawahKananY = tinggi;
        bawahKananX = lebar;
        kontruksiGaris();
    }

}


