package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.ex.SlideSetException;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
    private HashMap<String, URL> pageIndex;
    private File base = null;

    // -- Constructor --

    public HelpLoader() {
        pageIndex = new HashMap<String, URL>(50);
        pageIndex.put(null, ClassLoader.getSystemResource(pre + "index.html"));
    }

    // -- Methods --

    /**
    * Open a documentation page.
    * 
    * @param pageKey The page to open.
    */
    public void getHelp(String pageKey) throws SlideSetException {
        final URL page = pageIndex.get(pageKey);
        if(page == null || !Desktop.isDesktopSupported())
            return;
        try {
            Desktop.getDesktop().browse(page.toURI());
        } catch(URISyntaxException e) {
            throw new SlideSetException(e);
        } catch(IOException e) {
            System.out.println(e);
            throw new SlideSetException(e);
        }
    }
    
    // -- Helper methods --
    
    private void start() throws SlideSetException {
        
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
