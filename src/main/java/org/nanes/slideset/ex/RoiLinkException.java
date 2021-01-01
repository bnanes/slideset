package org.nanes.slideset.ex;

/**
 * Signals a problem with a link to an ROI set.
 * 
 * @author Benjamin Nanes
 */
public class RoiLinkException extends LinkException {

    /**
     * Creates a new instance of
     * <code>RoiLinkException</code> without detail message.
     */
    public RoiLinkException() {
    }

    /**
     * Constructs an instance of
     * <code>RoiLinkException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    public RoiLinkException(String msg) {
        super(msg);
    }

    public RoiLinkException(Throwable cause) {
        super(cause);
    }

    public RoiLinkException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
