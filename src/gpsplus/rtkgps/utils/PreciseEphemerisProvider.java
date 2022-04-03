package gpsplus.rtkgps.utils;

public enum PreciseEphemerisProvider {

    IGU_NASA("IGU NASA","ftp://anonymous:world.com@cddis.gsfc.nasa.gov/gnss/products/%W/","igu%W%D_%hb.sp3",".Z",474050),
    IGU_IGN("IGU IGN","ftp://anonymous:world.com@igs.ensg.ign.fr/pub/igs/products/%W/","igu%W%D_%hb.sp3",".Z",474050),
    ESU_ESA("ESU ESA","http://navigation-office.esa.int/products/gnss-products/%W/","esu%W%D_%hb.sp3",".Z",800642);
    private final String name;
    private final String URLTemplateDir;
    private final String URLTemplateFileDest;
    private final String URLTemplateFileCompressExt;
    private final int PredictedSize; //TODO Find a way to know in advance the size of the uncompressed file

    PreciseEphemerisProvider(String name, String URLTemplateDir, String URLTemplateFileDest, String URLTemplateFileCompressExt, int PredictedSize)
    {
        this.name = name;
        this.URLTemplateDir = URLTemplateDir;
        this.URLTemplateFileDest = URLTemplateFileDest;
        this.URLTemplateFileCompressExt = URLTemplateFileCompressExt;
        this.PredictedSize = PredictedSize;
    }

    public String getURLTemplateDir() {
        return URLTemplateDir;
    }

    public String getURLTemplateFileDest() {
        return URLTemplateFileDest;
    }

    public String getURLTemplateFileCompressExt() {
        return URLTemplateFileCompressExt;
    }

    public String getName(){
        return name;
    }

    public String getURLTemplate(){
        return URLTemplateDir+URLTemplateFileDest+URLTemplateFileCompressExt;
    }

    public int getPredictedSize(){
        return PredictedSize;
    }
}
