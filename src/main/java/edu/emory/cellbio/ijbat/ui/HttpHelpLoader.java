package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import net.imagej.ImageJ;
import net.imagej.app.ImageJApp;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Load and display documentation in
 * the system-default web browser.
 * 
 * @author Benjamin Nanes
 */
public class HttpHelpLoader implements HelpLoader {

    // -- Fields --
    
    private static final String pre = "edu/emory/cellbio/ijbat/docs";
    private final ArrayList<String> roots;
    private final HashMap<String, String> pageIndex;
    private JarHTTPd server = null;
    private int port = -1;
    private final ImageJ ij;

    // -- Constructor --

    public HttpHelpLoader(ImageJ ij) {
        this.ij = ij;
        pageIndex = new HashMap<String, String>(50);
        pageIndex.put(null, "index.html");
        pageIndex.put("about", "about.html");
        roots = new ArrayList<String>();
        roots.add(pre);
    }

    // -- Methods --

    /**
    * Open a documentation page.
    * 
    * @param pageKey The page to open.
    */
    public void getHelp(String pageKey) throws SlideSetException {
        String page = pageIndex.get(pageKey);
        if(page == null)
            page = pageKey;
        if(page == null || !Desktop.isDesktopSupported())
            throw new SlideSetException("Web help not supported.");
        if(server == null || port < 1)
            start();
        try {
            Desktop.getDesktop().browse( new URI("http://127.0.0.1:" 
                    + String.valueOf(port) + "/" + page));
        } catch(URISyntaxException e) {
            ij.log().debug(e);
            throw new SlideSetException(e);
        } catch(IOException e) {
            ij.log().debug(e);
            throw new SlideSetException(e);
        }
    }
    public void getHelp() throws SlideSetException {
        getHelp(null);
    }
    
    /**
     * Add a package for the help server to search when looking for
     * documentation resources.
     * 
     * @param root JVM resource path
     * (ex.: {@code edu/emory/cellbio/ijbat/docs})
     */
    public void addRoot(String root) {
        if(server == null || port < 1)
            roots.add(root);
        else
            server.addRoot(root);
    }
    
    // -- Helper methods --
    
    private void start() throws SlideSetException {
        ij.log().debug("Starting help server...");
        try {
            server = new JarHTTPd(0);
            for(String r : roots)
                server.addRoot(r);
            server.addDynamicResource("/about.html", new AboutPage());
        } catch(IOException e) {
            ij.log().debug(e);
            throw new SlideSetException(e);
        }
        port = server.getPort();
    }
    
    // -- Helper classes --
    
    private class AboutPage extends DynamicHelpResource {
        
        private final static String mime = JarHTTPd.MIME_HTML;
        private final HashMap<String, String> wildcards
                = new HashMap<String, String>();

        @Override
        public GeneratedResource generateResource(String params) {
            final URL base = getClass().getClassLoader()
                    .getResource(pre + "/about.html");
            StringBuffer sb = new StringBuffer();
            try {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(base.openStream()));
                String line = br.readLine();
                while(line != null) {
                    sb.append(line).append("\n");
                    line = br.readLine();
                }
            } catch(IOException e) {
                throw new IllegalStateException(e);
            }
            
            wildcards.clear();
            wildcards.put("${os}", System.getProperty("os.name") + " "
                    + System.getProperty("os.arch"));
            wildcards.put("${javaVersion}", System.getProperty("java.version"));
            wildcards.put("${javaDir}", System.getProperty("java.home"));
            wildcards.put("${classPath}", System.getProperty("java.class.path"));
            wildcards.put("${userName}", System.getProperty("user.name"));
            wildcards.put("${ij2Version}", ((ImageJApp) ij.getApp()).getVersion());
            wildcards.put("${ij1Version}", ij.legacy().getLegacyVersion());
            wildcards.put("${jar}", getMyJarPath());
            for(String card : wildcards.keySet()) {
                int a = sb.indexOf(card);
                while(a >= 0) {
                    sb.replace(a, a + card.length(), wildcards.get(card));
                    a = sb.indexOf(card);
                }
            }
            
            byte[] bytes;
            try {
                bytes = sb.toString().getBytes("UTF-8");
            } catch(UnsupportedEncodingException e) {
                throw new IllegalArgumentException(e);
            }
            return new GeneratedResource(
                    new ByteArrayInputStream(bytes), bytes.length, mime);
        }
        
        private String getMyJarPath() {
            CodeSource cs = this.getClass().getProtectionDomain().getCodeSource();
            if(cs == null)
                return "<?>";
            String path = cs.getLocation().toExternalForm();
            int exc = path.indexOf("!");
            if(exc < 0)
                exc = path.length();
            int file = path.indexOf("file:");
            return path.substring(file + 5, exc);
        }
        
    }

}
