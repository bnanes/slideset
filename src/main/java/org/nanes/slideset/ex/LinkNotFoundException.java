package org.nanes.slideset.ex;

/**
 * Indicates a link points to a file that does not exist.
 * @author Benjamin Nanes
 */
public class LinkNotFoundException extends LinkException {

     /**
      * Creates a new instance of
      * <code>LinkNotFoundException</code> without detail message.
      */
     public LinkNotFoundException() {
     }

     /**
      * Constructs an instance of
      * <code>LinkNotFoundException</code> with the specified detail message.
      *
      * @param msg the detail message.
      */
     public LinkNotFoundException(String msg) {
          super(msg);
     }

     public LinkNotFoundException(Throwable cause) {
          super(cause);
     }

     public LinkNotFoundException(String message, Throwable cause) {
          super(message, cause);
     }
     
}
