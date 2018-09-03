package gpsplus.rtkgps;

import gpsplus.rtklib.GTime;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class GPXTrace {

    @SuppressWarnings("unused")
    private class Point {
        private double lat    = 0;
        private double lon    = 0;
        private double height = 0;
        private double geoidheight = 0;
        private GTime gpstime = null;

        public Point(double lat, double lon, double height, double geoidheight, GTime gpstime) {
            this.lat = lat;
            this.lon = lon;
            this.height = height;
            this.geoidheight = geoidheight;
            this.gpstime = gpstime;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLon() {
            return lon;
        }

        public void setLon(double lon) {
            this.lon = lon;
        }

        public double getHeight() {
            return height;
        }

        public void setHeight(double height) {
            this.height = height;
        }

        public double getGeoidheight() {
            return geoidheight;
        }

        public void setGeoidheight(double geoidheight) {
            this.geoidheight = geoidheight;
        }

        public GTime getGpstime() {
            return gpstime;
        }

        public void setGpstime(GTime gpstime) {
            this.gpstime = gpstime;
        }

    }
    private ArrayList<Point> mPoints = null;


    public GPXTrace() {
        mPoints = new ArrayList<GPXTrace.Point>();
    }

    public void addPoint(double lat, double lon, double height, double geoidheight, GTime gpstime){

        mPoints.add(new Point(lat, lon, height, geoidheight, gpstime));
    }

    public static String documentToString(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            String szDocument = sw.toString();
            return szDocument;
        } catch (Exception ex) {
            throw new RuntimeException("Error converting to String", ex);
        }
    }

    public String getGPXTrace()
    {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
            //root elements
            Document doc = docBuilder.newDocument();
            Element rootElement = doc.createElement("gpx");
                    //rootElement.setAttributeNS("http://www.topografix.com/GPX/1/1", "xmlns:xsi",
                    //                                "http://www.w3.org/2001/XMLSchema-instance");
                    rootElement.setAttribute("xsi:schemaLocation",
                                                    "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
                    rootElement.setAttribute("xmlns",
                            "http://www.topografix.com/GPX/1/1");
                    rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            Attr versionAttr = doc.createAttribute("version");
                 versionAttr.setValue("1.1");
                 rootElement.setAttributeNode(versionAttr);
            Attr creatorAttr = doc.createAttribute("creator");
                 creatorAttr.setNodeValue("gpsplus.rtkgps");
                 rootElement.setAttributeNode(creatorAttr);
            doc.appendChild(rootElement);
            Element trkElement = doc.createElement("trk");
            Element trkSegElement = doc.createElement("trkseg");
            if (mPoints != null) {
                for (int i = 0; i < mPoints.size(); i++)
                {
                    Element trkPt = doc.createElement("trkpt");
                    trkPt.setAttribute("lat", Double.toString(mPoints.get(i).getLat()));
                    trkPt.setAttribute("lon", Double.toString(mPoints.get(i).getLon()));
                    Element ele = doc.createElement("ele");
                    ele.setTextContent(Double.toString(mPoints.get(i).height));
                    trkPt.appendChild(ele);
                    Element time = doc.createElement("time");
                    time.setTextContent(mPoints.get(i).getGpstime().getUtcXMLTime());
                    trkPt.appendChild(time);
                    Element geoidHeight = doc.createElement("geoidheight");
                    geoidHeight.setTextContent(Double
                            .toString(-1 * mPoints.get(i).getGeoidheight()));
                    trkPt.appendChild(geoidHeight);
                    trkSegElement.appendChild(trkPt);
                }
            }
            trkElement.appendChild(trkSegElement);
            rootElement.appendChild(trkElement);
            return documentToString(doc);

        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


        return "";
    }

    public void writeFile(String szPath) {
        try{
        File file = new File(szPath);
        FileOutputStream fop = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        }
        byte[] buffer = getGPXTrace().getBytes();
        fop.write(buffer, 0, buffer.length);
        fop.close();
        }
        catch(IOException ioE)
        {

        }

    }
}
