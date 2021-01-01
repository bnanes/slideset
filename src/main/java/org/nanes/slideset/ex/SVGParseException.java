package org.nanes.slideset.ex;

/**
 * Signals an error parsing an SVG file
 * @author Benjamin Nanes
 */
public class SVGParseException extends SlideSetException {

    public SVGParseException() {
    }

    public SVGParseException(String msg) {
        super(msg);
    }

    public SVGParseException(Throwable cause) {
        super(cause);
    }

    public SVGParseException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
