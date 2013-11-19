package edu.emory.cellbio.ijbat.ex;

/**
 * Signals a problem with a link to an image.
 *
 * @author Benjamin Nanes
 */
public class ImgLinkException extends LinkException {

    /**
     * Creates a new instance of <code>ImgLinkException</code> without detail message.
     */
    public ImgLinkException() {
    }

    /**
     * Constructs an instance of <code>ImgLinkException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public ImgLinkException(String msg) {
        super(msg);
    }

    public ImgLinkException(Throwable cause) {
        super(cause);
    }

    public ImgLinkException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
