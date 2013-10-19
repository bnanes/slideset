package edu.emory.cellbio.ijbat.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

/**
 *
 * @author Benjamin Nanes
 */
public class JarHTTPd extends NanoHTTPd {
    
    // -- Fields --
    
    /** List of packages within which to search for
     *  resources to serve **/
    private ArrayList<String> roots = new ArrayList<String>();
    
    // -- Constructor --
    
    /**
     * @param port Port to use, or 0 to select any open port
     */
    public JarHTTPd(int port) throws IOException {
        super(port);
    }
    
    // -- Methods --
    
    /** Get the port on which the server is listening. */
    public int getPort() {
        return myServerSocket.getLocalPort();
    }
    
    public void addRoot(String root) {
        if(root != null)
            roots.add(root);
    }
    
    /**
     * Override of {@link NanoHTTPd#serve(java.lang.String, java.lang.String, java.util.Properties, java.util.Properties, java.util.Properties) NanoHTTPd}
     * <br\> See {@link #serveJar(java.lang.String, java.util.Properties, java.lang.String) serveJar}
     */
    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, Properties files) {
        System.out.println(method + " '" + uri + "' ");
        
        // Kill non-loobback requests, just in case
        if(!header.getProperty("host").startsWith("127.0.0.1"))
            return null;

        return serveJar(uri, header);
    }
    
    /**
     * Search for a JVM resource
     * 
     * @param uri Resource to serve, relative to {@code root}
     * @param header Parsed HTTP request header
     * @return The requested resource, or an error
     */
    public Response serveJar(String uri, Properties header) {
        
        // Remove URL arguments
        uri = uri.trim().replace(File.separatorChar, '/');
        if (uri.indexOf('?') >= 0)
            uri = uri.substring(0, uri.indexOf('?'));

        // Prohibit getting out of current directory
        if (uri.startsWith("..") || uri.endsWith("..") || uri.indexOf("../") >= 0)
            return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
                    "FORBIDDEN: Won't serve ../ for security reasons.");
        
        // Lookup the target resource
        Iterator<String> rs = roots.iterator();
        URL target = null;
        while(target == null && rs.hasNext())
            target = ClassLoader.getSystemResource(rs.next() + uri);
        if(target == null)
            return new Response(HTTP_NOTFOUND, MIME_PLAINTEXT,
                "404 Error: File not found.");
        
        try {
            // Try to open a connection
            URLConnection tarCon = target.openConnection();
            
            // Check if our target is a directory
            boolean isDir = false;
            if(tarCon instanceof JarURLConnection) {
                 if(((JarURLConnection)tarCon).getJarEntry().isDirectory())
                     isDir = true;
                 else {
                     try {
                         tarCon.getInputStream().available();
                     } catch(Exception e) {
                         isDir = true;
                     }
                 }
            }
            else if(target.getProtocol().startsWith("file")
                 && new File(target.getPath()).isDirectory())
                isDir = true;
            if(isDir) {

                // Browsers get confused without '/' after the
                // directory, send a redirect.
                if (!uri.endsWith("/")) {
                    uri += "/";
                    Response r = new Response(HTTP_REDIRECT, MIME_HTML,
                            "<html><body>Redirected: <a href=\"" + uri + "\">" +
                                    uri + "</a></body></html>");
                    r.addHeader("Location", uri);
                    return r;
                }

                // Try to find the index page
                URL old = target;
                target = new URL(old.toString() + "index.html");
                try {
                    tarCon = target.openConnection();
                    tarCon.connect();
                } catch(IOException e) {
                    target = new URL(old.toString() + "index.htm");
                    try {
                        tarCon = target.openConnection();
                        tarCon.connect();
                    } catch(IOException ee) {
                        return new Response(HTTP_FORBIDDEN, MIME_PLAINTEXT,
                            "FORBIDDEN!");
                    }
                }
            }

            // Get MIME type from file name extension, if possible
            String mime = null;
            int dot = target.getPath().lastIndexOf('.');
            int hash = target.getPath().lastIndexOf('#');
            int quer = target.getPath().lastIndexOf('?');
            hash = hash < 0 ? target.getPath().length() : hash;
            quer = quer < 0 ? target.getPath().length() : quer;
            int end = Math.min(target.getPath().length(), Math.min(hash, quer));
            if (dot >= 0)
                mime = (String) theMimeTypes.get(target.getPath().substring(dot + 1, end).toLowerCase());
            if (mime == null)
                mime = MIME_DEFAULT_BINARY;
            
            // Support (simple) skipping:
            long startFrom = 0;
            String range = header.getProperty("range");
            if (range != null) {
                if (range.startsWith("bytes=")) {
                    range = range.substring("bytes=".length());
                    int minus = range.indexOf('-');
                    if (minus > 0)
                        range = range.substring(0, minus);
                    try {
                        startFrom = Long.parseLong(range);
                    } catch (NumberFormatException nfe) {
                    }
                }
            }
            
            // Serve the stream
            tarCon.connect();
            InputStream tarStr = tarCon.getInputStream();
            tarStr.skip(startFrom);
            Response r = new Response(HTTP_OK, mime, tarStr);
            r.addHeader("Content-length", "" + (tarCon.getContentLength() - startFrom));
            r.addHeader("Content-range", "" + startFrom + "-" +
                    (tarCon.getContentLength() - 1) + "/" + tarCon.getContentLength());
            return r;
            
        } catch(IOException e) {
            System.out.println(e);
            return new Response(HTTP_INTERNALERROR, MIME_PLAINTEXT, "Server error!");
        }

    }
    
}
