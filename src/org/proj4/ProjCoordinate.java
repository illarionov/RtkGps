package org.proj4;

public class ProjCoordinate {

    public double x;
    public double y;
    public double z;

    public ProjCoordinate()
    {
      this(0.0, 0.0);
    }
    public ProjCoordinate(double argX, double argY, double argZ)
    {
        this.x = argX;
        this.y = argY;
        this.z = argZ;
    }
    public ProjCoordinate(double argX, double argY)
    {
        this.x = argX;
        this.y = argY;
        this.z = Double.NaN;
    }
}
