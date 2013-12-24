package edu.emory.cellbio.ijbat.ex;

/**
 * Signal that an {@code Overlay} type is not supported.
 * 
 * @author Benjamin Nanes
 */
public class UnsupportedOverlayException extends SlideSetException {

    public UnsupportedOverlayException() {
    }

    public UnsupportedOverlayException(String msg) {
        super(msg);
    }

    public UnsupportedOverlayException(Throwable cause) {
        super(cause);
    }

    public UnsupportedOverlayException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
