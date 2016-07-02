package org.ggp.base.util.ui;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.ggp.base.util.files.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.xhtmlrenderer.swing.Java2DRenderer;
import org.xhtmlrenderer.swing.NaiveUserAgent;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * GameStateRenderer generates an image that represents the current state
 * of a match, based on the current state of the match (in XML) and an XSLT
 * that converts that XML match state into HTML. After rendering the match
 * state in HTML as a DOM, it renders that DOM into a BufferedImage which
 * can be displayed to the user.
 *
 * TODO: This class is still pretty rough, and I suspect there's much room
 * for improvement. Furthermore, improving this class will yield immediate
 * visible benefits, in terms of better visualizations and such. For example,
 * when rendering games that don't take up the full 600x600 image, there's an
 * empty black space on the final image, which looks bad. That could be fixed.
 *
 * @author Ethan Dreyfuss and Sam Schreiber
 */
public class GameStateRenderer {
    private static final Dimension defaultSize = new Dimension(600,600);

    /* Note: NaiveUserAgent is not thread safe, so whenever the architecture
     * of this class is modified in a way that enables concurrent rendering,
     * this must be changed to another UserAgentCallback implementation that
     * uses a thread safe image cache */
    private static NaiveUserAgent userAgent = new NaiveUserAgent(128);

    public static Dimension getDefaultSize()
    {
        return defaultSize;
    }

    public static synchronized void renderImagefromGameXML(String gameXML, String XSL, BufferedImage backimage)
    {

        String xhtml = getXHTMLfromGameXML(gameXML, XSL);
        File folder = new File("vizSamples");
        folder.mkdir();
        org.jsoup.nodes.Document dom;
        try {
            dom = Jsoup.parse(xhtml);

            // Many existing visualization stylesheets have style elements
            // deep within html body content, where compliant renderers interpret
            // them as text, and not as styles. So we have to pull them out.
            Elements styles = dom.getElementsByTag("style");
            Element head = dom.getElementsByTag("head").get(0);
            for (int i = 0; i < styles.size(); i += 1) {
                Element styleTag = styles.get(i);
                Element parent = styleTag.parent();
                if (!parent.equals(head)) {
                    styleTag.remove();
                    head.appendChild(styleTag);
                }
            }

            Element style = dom.createElement("style");
            String bodyStyle = String.format("body { width: %dpx; height: %dpx; overflow:hidden; margin:auto;}",
                    defaultSize.width, defaultSize.height);
            style.appendChild(new TextNode(bodyStyle, null));
            head.appendChild(style);
        } finally {
            //...
        }


        try {

            String output = dom.outerHtml();
            output = output.replace("<br>", "<br/>"); //for some reason it still does this wrong =(
            output = output.replaceAll("(<img [^>]+)>", "$1/>");

            File vizDest = File.createTempFile("viz", ".html", folder);
            FileUtils.writeStringToFile(vizDest, output);

            Java2DRenderer r = new Java2DRenderer(toW3cDocument(output), backimage.getWidth(), backimage.getHeight());
            r.getSharedContext().setUserAgentCallback(userAgent);

            ChainingReplacedElementFactory chainingReplacedElementFactory = new ChainingReplacedElementFactory();
            chainingReplacedElementFactory.addReplacedElementFactory(r.getSharedContext().getReplacedElementFactory());
            chainingReplacedElementFactory.addReplacedElementFactory(new SVGReplacedElementFactory());
            r.getSharedContext().setReplacedElementFactory(chainingReplacedElementFactory);

            BufferedImage image = r.getImage();
            Raster imageData = image.getData();
            backimage.setData(imageData);
        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }
    }

    public static org.w3c.dom.Document toW3cDocument(String output) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory fact = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = fact.newDocumentBuilder();

        builder.setErrorHandler( null );
        return builder.parse(new InputSource(new StringReader(output)));
    }

    public static synchronized void shrinkCache() {
        userAgent.shrinkImageCache();
    }

    private static String getXHTMLfromGameXML(String gameXML, String XSL) {
        XSL = XSL.replace("<!DOCTYPE stylesheet [<!ENTITY ROOT \"http://games.ggp.org/base\">]>", "");
        //TODO: Find a better solution for this
        XSL = XSL.replace("&ROOT;", "http://games.ggp.org/base").trim();
//      XSL = XSL.replace("&ROOT;", "file:///C:/Users/Alex/bitbucket/Alloy/Alloy/games").trim();

        IOString game = new IOString(gameXML);
        IOString xslIOString = new IOString(XSL);
        IOString content = new IOString("");
        try {
            TransformerFactory tFactory = new net.sf.saxon.TransformerFactoryImpl();
            Transformer transformer = tFactory.newTransformer(new StreamSource(xslIOString.getInputStream()));
            transformer.setParameter("width", defaultSize.getWidth()-40);
            transformer.setParameter("height", defaultSize.getHeight()-40);
            transformer.transform(new StreamSource(game.getInputStream()),
                    new StreamResult(content.getOutputStream()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return content.getString();
    }

    //========IOstring code========
    private static class IOString
    {
        private StringBuilder buf;
        public IOString(String s) {
            buf = new StringBuilder(s);
        }
        public String getString() {
            return buf.toString();
        }

        public InputStream getInputStream() {
            return new IOString.IOStringInputStream();
        }
        public OutputStream getOutputStream() {
            return new IOString.IOStringOutputStream();
        }

        class IOStringInputStream extends java.io.InputStream {
            private int position = 0;
            @Override
            public int read() throws java.io.IOException
            {
                if (position < buf.length()) {
                    return buf.charAt(position++);
                } else {
                    return -1;
                }
            }
        }
        class IOStringOutputStream extends java.io.OutputStream {
            @Override
            public void write(int character) throws java.io.IOException {
                buf.append((char)character);
            }
        }
    }
}