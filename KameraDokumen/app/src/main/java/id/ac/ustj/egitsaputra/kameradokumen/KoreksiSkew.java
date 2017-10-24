package id.ac.ustj.egitsaputra.kameradokumen;

import android.graphics.Bitmap;
import android.util.Log;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by egit saputra on 5/22/2016.
 */
public class KoreksiSkew {

    private Bitmap gambarHasil;
    private Mat frameMat, garis;
    private int sudutVertikal=45;
    private int lebar;
    private int tinggi;
    private int minV =90-sudutVertikal, maxV=90+sudutVertikal;
    private int minH=0+sudutVertikal, maxH=180-sudutVertikal;

    private double[] data;
    private double r, t, a, b, x0, y0;
    private Point p1 = new Point();
    private Point p2 = new Point();
    private List<Point> titikPojok=new ArrayList<>(), titik;

    public void setDimensiAsalIndikator(int lebar, int tinggi) {
        this.lebar=lebar;
        this.tinggi = tinggi;
    }
    public List<Point> getPojok() {
        return titik;
    }

    public void setPojok(List<Point> p){
        this.titikPojok=p;
    }
    public void setPiksel(byte[] data, int l, int t) {
        this.lebar=l;
        this.tinggi=t;
        this.frameMat = new Mat(tinggi + tinggi / 2, lebar, CvType.CV_8UC1);
        this.frameMat.put(0, 0, data);
        eksekusi();
    }
    public void eksekusi() {

        Mat frameGray = new Mat(tinggi + tinggi / 2, lebar, CvType.CV_8UC1);
        Imgproc.cvtColor(frameMat, frameGray, Imgproc.COLOR_YUV2GRAY_NV21);
        Imgproc.GaussianBlur(frameGray, frameGray, new Size(5, 5), 500);
        Imgproc.Canny(frameGray, frameGray, 50, 200, 3, true);

        garis = new Mat();
        Imgproc.HoughLines(frameGray, garis, 1, Math.PI / 180, 50);
        titik = kontruksiTitikVH(garis);
    }
    public Bitmap koreksi(Bitmap b){

        int l=b.getWidth();
        int t=b.getHeight();

        if (titikPojok.size()!=0) {
            if (b!=null) {

                //ubah nilai xy tiap pojok sesuai hasil dimensi gambar
                for (int x = 0; x < titikPojok.size(); x++) {
                    titikPojok.set(x, new Point(titikPojok.get(x).x * l / lebar, titikPojok.get(x).y * t / tinggi));
                }

                Mat simpanan = new Mat(t, l, CvType.CV_8UC3);
                Utils.bitmapToMat(b, simpanan);

                //menentukan lebar & tinggi target---jarak manhatan
                double Lebar =titikPojok.get(1).x - titikPojok.get(0).x;
                double bawahL =titikPojok.get(2).x - titikPojok.get(3).x;
                double Tinggi =titikPojok.get(2).y - titikPojok.get(1).y;
                double bawahT =titikPojok.get(3).y - titikPojok.get(0).y;
                Lebar=Math.max(Lebar, bawahL);
                Tinggi=Math.max(Tinggi,bawahT);

                List<Point> titiktarget = new ArrayList<>();
                Point AKiri = new Point(0, 0);
                Point AKanan = new Point(Lebar, 0);
                Point BKanan = new Point(Lebar, Tinggi);
                Point BKiri = new Point(0, Tinggi);


                titiktarget.add(AKiri);
                titiktarget.add(AKanan);
                titiktarget.add(BKanan);
                titiktarget.add(BKiri);

                Mat pojok = Converters.vector_Point2f_to_Mat(titikPojok);
                Mat target = Converters.vector_Point2f_to_Mat(titiktarget);
                Mat PT = Imgproc.getPerspectiveTransform(pojok, target);
                Imgproc.warpPerspective(simpanan, simpanan, PT, new Size(Lebar, Tinggi));
                Bitmap tmap = Bitmap.createBitmap(simpanan.cols(), simpanan.rows(), Bitmap.Config.ARGB_8888);

                Utils.matToBitmap(simpanan, tmap);
                gambarHasil = tmap;
            }
        }
        return gambarHasil;
    }
    private List<Point> kontruksiTitikVH(Mat matGaris) {

        //bagi garis menjadi vertikal dan horizontal
        List<Point> titikMulaiV = new ArrayList<>();
        List<Point> titikAkhirV = new ArrayList<>();
        List<Point> titikMulaiH = new ArrayList<>();
        List<Point> titikAkhirH = new ArrayList<>();
        List<Point> titikXY = new ArrayList<>();

        for (int i = 0; i < matGaris.cols(); i++) {
            data = matGaris.get(0, i);
            r = data[0];
            t = data[1];
            a = Math.cos(t);
            b = Math.sin(t);
            x0 = a * r;
            y0 = b * r;
            p1.x = Math.round(x0 + lebar * (-b));
            p1.y = Math.round(y0 + lebar * (a));
            p2.x = Math.round(x0 - lebar * (-b));
            p2.y = Math.round(y0 - lebar * (a));

            int de = (int) Math.toDegrees(t);
            if (minV < de && de < maxV ) {
                titikMulaiV.add(new Point(p1.x, p1.y));
                titikAkhirV.add(new Point(p2.x, p2.y));
            }

            if (minH > de || de > maxH) {
                titikMulaiH.add(new Point(p1.x, p1.y));
                titikAkhirH.add(new Point(p2.x, p2.y));
            }
        }

        //cari garis singung V dan H
        for (int i = 0; i < titikMulaiV.size(); i++) {
            for (int j = 0; j < titikMulaiH.size(); j++) {
                Point p = titikSinggung(titikMulaiH.get(j), titikAkhirH.get(j), titikMulaiV.get(i), titikAkhirV.get(i));
                if (p != null) {
                    titikXY.add(p);
                }
            }
        }

        return titikXY;
    }
    private Point titikSinggung(Point p1, Point p2, Point p3, Point p4) {
        double x1, x2, x3, x4, y1, y2, y3, y4;
        x1 = p1.x;
        x2 = p2.x;
        x3 = p3.x;
        x4 = p4.x;
        y1 = p1.y;
        y2 = p2.y;
        y3 = p3.y;
        y4 = p4.y;

        double d = (x1 - x2) * (y3 - y4) - (y1 - y2) * (x3 - x4);
        if (d == 0) {
            return null;
        }

        double x = ((x3 - x4) * (x1 * y2 - y1 * x2) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d;
        double y = ((y3 - y4) * (x1 * y2 - y1 * x2) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d;
        return new Point(x, y);
    }
    static {
        System.loadLibrary("opencv_java");
    }
}
