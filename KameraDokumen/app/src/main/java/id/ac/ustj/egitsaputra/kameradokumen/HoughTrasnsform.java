package id.ac.ustj.egitsaputra.kameradokumen;

/**
 * Created by egit saputra on 5/26/2016.
 */

public class HoughTrasnsform {
    private int[] pikselInput;
    private int[] pikselOutput;
    private int lebar;
    private int tinggi;
    private int[] akumulator;
    private int banyakGaris =50;
    private int[] hasilVoting;
    private boolean _BERJALAN=false;

    //setter piksel masukkan
    public void setPikselInput(int[] pix, int lebar, int tinggi) {
        this.lebar =lebar;
        this.tinggi =tinggi;
        pikselInput = new int[this.lebar * this.tinggi];
        pikselOutput = new int[this.lebar * this.tinggi];
        pikselInput =pix;
    }

    //getter status berjalan
    public boolean getStatusBerjalan() {
        return _BERJALAN;
    }

    //eksekusi operasi hough transform
    public int[] process() {
        _BERJALAN=true;
        // kontruksi akumulator
        int nilaiMaksR = (int)Math.sqrt(lebar * lebar + tinggi * tinggi);
        akumulator = new int[nilaiMaksR*180];
        int r;

        //deteksi titik
        for(int x = 0; x< lebar; x++) {
            for(int y = 0; y< tinggi; y++) {
                if ((pikselInput[y* lebar +x] & 0xff)== 255) {
                    for (int theta=0; theta<180; theta++) {
                        r = (int)(x*Math.cos(((theta)*Math.PI)/180) + y*Math.sin(((theta)*Math.PI)/180));
                        if ((r > 0) && (r <= nilaiMaksR)) {
                            akumulator[r*180+theta] = akumulator[r*180+theta] + 1;
                        }
                    }
                }
            }
        }


        //putar titik dengan akumulator; ambil suara voting
        int max=0;
        for(r=0; r<nilaiMaksR; r++) {
            for(int theta=0; theta<180; theta++) {
                if (akumulator[r*180+theta] > max) {
                    max = akumulator[r*180+theta];
                }
            }
        }

        int value;
        for(r=0; r<nilaiMaksR; r++) {
            for(int theta=0; theta<180; theta++) {
                value = (int)(((double) akumulator[r*180+theta]/(double)max)*255.0);
                akumulator[r*180+theta] = 0xff000000 | (value << 16 | value << 8 | value);
            }
        }

        voting();
        return pikselOutput;
    }

    //voting dengan mencari tumpukan r terbanyak pada theta
    private int[] voting() {
        int nilaiMaksR = (int)Math.sqrt(lebar * lebar + tinggi * tinggi);
        hasilVoting = new int[banyakGaris *3];
        int[] output = new int[lebar * tinggi];

        for(int r=0; r<nilaiMaksR; r++) {
            for(int theta=0; theta<180; theta++) {
                int nilaiSuara = (akumulator[r*180+theta] & 0xff);
                if (nilaiSuara > hasilVoting[(banyakGaris -1)*3]) {

                    hasilVoting[(banyakGaris -1)*3] = nilaiSuara;
                    hasilVoting[(banyakGaris -1)*3+1] = r;
                    hasilVoting[(banyakGaris -1)*3+2] = theta;


                    int i = (banyakGaris -2)*3;
                    while ((i >= 0) && (hasilVoting[i+3] > hasilVoting[i])) {
                        for(int j=0; j<3; j++) {
                            int temp = hasilVoting[i+j];
                            hasilVoting[i+j] = hasilVoting[i+3+j];
                            hasilVoting[i+3+j] = temp;
                        }

                        i = i - 3;
                        if (i < 0) break;
                    }
                }
            }
        }

        for(int i = banyakGaris -1; i>=0; i--){
            konstruksiGaris(hasilVoting[i*3], hasilVoting[i*3+1], hasilVoting[i*3+2]);
        }
        return output;
    }

    //membuat garis
    private void konstruksiGaris(int nilaiSuara, int r, int theta) {
        for(int x = 0; x< lebar; x++) {
            for(int y = 0; y< tinggi; y++) {
                int temp = (int)(x*Math.cos(((theta)*Math.PI)/180) + y*Math.sin(((theta)*Math.PI)/180));
                if((temp - r) == 0)
                    pikselOutput[y* lebar +x] = 0xffffffff | (nilaiSuara << 16 | nilaiSuara << 8 | nilaiSuara);
            }
        }
    }
}
