package id.ac.ustj.egitsaputra.kameradokumen;

import android.os.Debug;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by egit saputra on 5/18/2016.
 */
public class Canny{

    private final static float SKALA_MAGNITUDE = 100;
    private final static float BATAS_MAGNITUDE = 1000;
    private final static int MAGNITUDE_MAX = (int) (SKALA_MAGNITUDE * BATAS_MAGNITUDE);

    private int tinggiCitra;
    private int lebarCitra;
    private int dimensi;
    private int lebarKernelGauss;
    private int modeKernelGauss=2; //1= proses hitung 1D, 2= proses hitung 2D, 3= kernel template
    private float sigmaGauss;
    private float ambangBawah;
    private float ambangAtas;

    private int[] dataPiksel;
    private int[] magnitude;
    private float[] konvoX;
    private float[] konvoY;
    private float[] gradientX;
    private float[] gradientY;
    private boolean _BERJALAN=false;


    public Canny() {
        ambangBawah =8;
        ambangAtas = 9;
        sigmaGauss = 7;
        lebarKernelGauss = 5;
    }

    public int[] getPikselTepi() {
        return dataPiksel;
    }

    public boolean getStatusBerjalan(){
        return _BERJALAN;
    }

    public void setDataPiksel(int[] data, int l, int t ) {
        this.lebarCitra =l;
        this.tinggiCitra =t;
        this.dataPiksel = data;
    }

    // methods untuk memulai eksekusi
    public void process() {
        _BERJALAN = true;
        this.dimensi = lebarCitra * tinggiCitra;
        inisialisasi();
        deteksi(sigmaGauss, lebarKernelGauss);
        int bawah = Math.round(ambangBawah * SKALA_MAGNITUDE);
        int atas = Math.round( ambangAtas * SKALA_MAGNITUDE);
        hyteresis(bawah, atas);
        thresholdEdges();
        _BERJALAN=false;
    }

    // inisialisasi ARRAY
    private void inisialisasi() {
        if (dataPiksel != null || dimensi == dataPiksel.length) {
            magnitude = new int[dimensi];
            konvoX = new float[dimensi];
            konvoY = new float[dimensi];
            gradientX = new float[dimensi];
            gradientY = new float[dimensi];
        }
    }

    private void deteksi(float sigma, int lebarKernel) {


        //1. Smoothing (membuat kernel gaussian)
        float kernel[] = new float[lebarKernel];
        int posisiX=0;
        switch (modeKernelGauss){
            case 1:{
                    int CposisiX = lebarKernel / 2;
                    float Total = 0;
                    for (int posX = -CposisiX; posX <= CposisiX; posX++) {
                        float minExp = (float) Math.exp(-((posX * posX) / (2 * (sigma * sigma))));
                        float awal = (float) (1 / Math.sqrt(2 * Math.PI * sigma));
                        kernel[posX + CposisiX] = awal * minExp;
                        Total += kernel[posX + CposisiX];
                    }
                    for (int x = 0; x < lebarKernel; x++) {
                        kernel[x] = kernel[x] * 1 / Total;
                    }

                break;
            }
            case 2:{

                    float[][] kernel2 = GaussianBlur(lebarKernel, sigma);
                    //...

                break;
            }
            case 3:{
                for (posisiX=0;posisiX<lebarKernel;posisiX++){
                    float e1 = gaussian(posisiX, sigma);
                    if (e1 <= 0.5f && posisiX >= 2) break;
                    float e2 = gaussian(posisiX - 0.5f, sigma);
                    float e3 = gaussian(posisiX + 0.5f, sigma);
                    kernel[posisiX] = (e1 + e2 + e3) / 3f / (2f * (float) Math.PI * sigma * sigma);
                }
                break;
            }
        }

        posisiX=kernel.length;
        int batasX = posisiX - 1;
        int maxX = lebarCitra - (posisiX - 1);
        int batasY = lebarCitra * (posisiX - 1);
        int maxY = lebarCitra * (tinggiCitra - (posisiX - 1));

        //2. Finding Gradien ()
        for (int x = batasX; x < maxX; x++) {
            for (int y = batasY; y < maxY; y += lebarCitra) {
                int indeks = x + y;
                float hasilX = dataPiksel[indeks] * kernel[0];
                float hasilY = hasilX;
                int xOffset = 1;
                int yOffset = lebarCitra;
                for(; xOffset < posisiX ;) {
                    hasilY += kernel[xOffset] * (dataPiksel[indeks - yOffset] + dataPiksel[indeks + yOffset]);
                    hasilX += kernel[xOffset] * (dataPiksel[indeks - xOffset] + dataPiksel[indeks + xOffset]);
                    yOffset += lebarCitra;
                    xOffset++;
                }
                konvoY[indeks] = hasilY;
                konvoX[indeks] = hasilX;
            }
        }
        for (int x = batasX; x < maxX; x++) {
            for (int y = batasY; y < maxY; y += lebarCitra) {
                float nilaiPix = 0f;
                int indeks = x + y;
                for (int i = 1; i < posisiX; i++)
                    nilaiPix += /*rentangKernel[i]* */ (konvoY[indeks - i] - konvoY[indeks + i]);
                gradientX[indeks] = nilaiPix;
            }
        }

        for (int x = posisiX; x < lebarCitra - posisiX; x++) {
            for (int y = batasY; y < maxY; y += lebarCitra) {
                float nilaiPix = 0.0f;
                int index = x + y;
                int yOffset = lebarCitra;
                for (int i = 1; i < posisiX; i++) {
                    nilaiPix += /*rentangKernel[i] */(konvoX[index - yOffset] - konvoX[index + yOffset]);
                    yOffset += lebarCitra;
                }
                gradientY[index] = nilaiPix;
            }
        }

        batasX = posisiX;
        maxX = lebarCitra - posisiX;
        batasY = lebarCitra * posisiX;
        maxY = lebarCitra * (tinggiCitra - posisiX);

        for (int x = batasX; x < maxX; x++) {
            for (int y = batasY; y < maxY; y += lebarCitra) {
                int indeks = x + y;
                int indeksUtara = indeks - lebarCitra;
                int indeksSelatan = indeks + lebarCitra;
                int indeksBarat = indeks - 1;
                int indeksTimur = indeks + 1;
                int indeksBLaut = indeksUtara - 1;
                int indeksTLaut = indeksUtara + 1;
                int indeksBDaya = indeksSelatan - 1;
                int indeksTenggara = indeksSelatan + 1;

                float xGrad = gradientX[indeks];
                float yGrad = gradientY[indeks];
                float gradMag = hypot(xGrad, yGrad);

                //3.
                //operasi non-maximal supression
                float utMag = hypot(gradientX[indeksUtara], gradientY[indeksUtara]);
                float seMag = hypot(gradientX[indeksSelatan], gradientY[indeksSelatan]);
                float baMag = hypot(gradientX[indeksBarat], gradientY[indeksBarat]);
                float tiMag = hypot(gradientX[indeksTimur], gradientY[indeksTimur]);
                float tlMag = hypot(gradientX[indeksTLaut], gradientY[indeksTLaut]);
                float teMag = hypot(gradientX[indeksTenggara], gradientY[indeksTenggara]);
                float bdMag = hypot(gradientX[indeksBDaya], gradientY[indeksBDaya]);
                float blMag = hypot(gradientX[indeksBLaut], gradientY[indeksBLaut]);
                float tmp;

                if (xGrad * yGrad <= (float) 0
                        ? Math.abs(xGrad) >= Math.abs(yGrad)
                        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * tlMag - (xGrad + yGrad) * tiMag)
                        && tmp > Math.abs(yGrad * bdMag - (xGrad + yGrad) * baMag)
                        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * tlMag - (yGrad + xGrad) * utMag)
                        && tmp > Math.abs(xGrad * bdMag - (yGrad + xGrad) * seMag)
                        : Math.abs(xGrad) >= Math.abs(yGrad)
                        ? (tmp = Math.abs(xGrad * gradMag)) >= Math.abs(yGrad * teMag + (xGrad - yGrad) * tiMag)
                        && tmp > Math.abs(yGrad * blMag + (xGrad - yGrad) * baMag)
                        : (tmp = Math.abs(yGrad * gradMag)) >= Math.abs(xGrad * teMag + (yGrad - xGrad) * seMag)
                        && tmp > Math.abs(xGrad * blMag + (yGrad - xGrad) * utMag)
                        ) {
                    magnitude[indeks] = gradMag >= BATAS_MAGNITUDE ? MAGNITUDE_MAX : (int) (SKALA_MAGNITUDE * gradMag);
                } else {
                    magnitude[indeks] = 0;
                }
            }
        }
    }

    //1.1
    //operasi gaussian lanjutan
    private float gaussian(float x, float sigma) {
        return (float) Math.exp(-(x * x) / (2f * (sigma * sigma)));
    }
    private float[][] GaussianBlur(int k, float s) {
        float[][] Kernel = new float[k][k];
        float Total = 0;

        int kernelRadius = k / 2;
        float distance;

        float calculatedEuler =(float) (1.0 / (2.0 * Math.PI * Math.pow(s, 2)));

        for (int filterY = -kernelRadius; filterY <= kernelRadius; filterY++)
        {
            for (int filterX = -kernelRadius; filterX <= kernelRadius; filterX++)
            {
                distance = ((filterX * filterX) + (filterY * filterY)) / (2 * (s * s));
                Kernel[filterX + kernelRadius][filterY + kernelRadius] = (float) (calculatedEuler * Math.exp(-distance));
                Total += Kernel[filterX + kernelRadius][filterY + kernelRadius];
            }
        }

        for (int y = 0; y < k; y++)
        {
            for (int x = 0; x < k; x++)
            {
                Kernel[x][y] = Kernel[x][y] * (float) (1.0 / Total);
            }
        }
        return Kernel;
    }
    //3.1
    //operasi hypot (trigonometry)
    private float hypot(float x, float y) {
        return (float) Math.hypot(x, y);
    }
    //4.
    // Tresholding
    private void thresholdEdges() {
        for (int i = 0; i < dimensi; i++) {
            dataPiksel[i] = dataPiksel[i] > 0 ? -1 : 0xff000000;
        }
    }
    //5.
    //hyteresis
    private void hyteresis(int bawah, int atas) {
        Arrays.fill(dataPiksel, 0);

        int offset = 0;
        for (int y = 0; y < tinggiCitra; y++) {
            for (int x = 0; x < lebarCitra; x++) {
                if (dataPiksel[offset] == 0 && magnitude[offset] >= atas) {
                    lanjutanHysteresis(x, y, offset, bawah);
                }
                offset++;
            }
        }
    }

    //5.1 Hysteresis lanjutan
    private void lanjutanHysteresis(int x1, int y1, int i1, int threshold) {
        int x0 = x1 == 0 ? x1 : x1 - 1;
        int x2 = x1 == lebarCitra - 1 ? x1 : x1 + 1;
        int y0 = y1 == 0 ? y1 : y1 - 1;
        int y2 = y1 == tinggiCitra -1 ? y1 : y1 + 1;

        dataPiksel[i1] = magnitude[i1];
        for (int x = x0; x <= x2; x++) {
            for (int y = y0; y <= y2; y++) {
                int i2 = x + y * lebarCitra;
                if ((y != y1 || x != x1)
                        && dataPiksel[i2] == 0
                        && magnitude[i2] >= threshold) {
                    lanjutanHysteresis(x, y, i2, threshold);
                    return;
                }
            }
        }
    }


}

