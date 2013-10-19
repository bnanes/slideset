package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Load and display documentation in
 * the system-default web browser.
 * 
 * @author Benjamin Nanes
 */
public class HelpLoader {

    // -- Fields --
    
    private static String pre = "edu/emory/cellbio/ijbat/docs/";
    private ArrayList<String> roots;
    private HashMap<String, String> pageIndex;
    private JarHTTPd server = null;
    private int port = -1;

    // -- Constructor --

    public HelpLoader() {
        pageIndex = new HashMap<String, String>(50);
        pageIndex.put(null, "index.html");
        roots.add(pre);
    }

    // -- Methods --

    /**
    * Open a documentation page.
    * 
    * @param pageKey The page to open.
    */
    public void getHelp(String pageKey) throws SlideSetException {
        final String page = pageIndex.get(pageKey);
        if(page == null || !Desktop.isDesktopSupported())
            throw new SlideSetException("Web help not supported.");
        if(server == null || port < 1)
            start();
        try {
            Desktop.getDesktop().browse( new URI("http://127.0.0.1:" 
                    + String.valueOf(port) + "/" + page));
        } catch(URISyntaxException e) {
            System.out.println(e);
            throw new SlideSetException(e);
        } catch(IOException e) {
            System.out.println(e);
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
     * (ex.: {@code edu/emory/cellbio/ijbat/docs/})
     */
    public void addRoot(String root) {
        if(server == null || port < 1)
            roots.add(root);
        else
            server.addRoot(root);
    }
    
    // -- Helper methods --
    
    private void start() throws SlideSetException {
        System.out.println("Starting help server...");
        try {
            server = new JarHTTPd(0);
            for(String r : roots)
                server.addRoot(r);
        } catch(IOException e) {
            System.out.println(e);
            throw new SlideSetException(e);
        }
        port = server.getPort();
    }
    
    // -- Tests --
    
    public static void main(String... args) {
        try {
            new HelpLoader().getHelp(null);
        } catch(Exception e) {
            System.out.println(e);
        }
    }

}
