package edu.emory.cellbio.ijbat.ui;

import java.io.InputStream;

/**
 * Dynamically generated resource for the
 * by the documentation server.
 * 
 * @author Benjamin Nanes
 */
public abstract class DynamicHelpResource {
    
    /** Generate the dynamic resource */
    public abstract GeneratedResource generateResource(String params);
    
    /** Generated dynamic resource */
    public final class GeneratedResource {
        /** Resource stream */
        public InputStream stream;
        /** Number of bytes in the resource stream */
        public int length;
        /** MIME type of the resource stream */
        public String mime;
        
        /**
         * @param stream Resource stream
         * @param length Number of bytes in the resource stream
         * @param mime MIME type of the resource stream
         */
        public GeneratedResource(InputStream stream, int length, String mime) {
            this.stream = stream;
            this.length = length;
            this.mime = mime;
        }
    }
    
}
