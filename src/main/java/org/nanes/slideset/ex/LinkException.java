package org.nanes.slideset.ex;

/**
 * Signals a problem with a data table file reference
 * 
 * @author Benjamin Nanes
 */
public class LinkException extends SlideSetException {

     /**
      * Creates a new instance of
      * <code>LinkException</code> without detail message.
      */
     public LinkException() {
     }

     /**
      * Constructs an instance of
      * <code>LinkException</code> with the specified detail message.
      *
      * @param msg the detail message.
      */
     public LinkException(String msg) {
          super(msg);
     }

     public LinkException(Throwable cause) {
          super(cause);
     }

     public LinkException(String message, Throwable cause) {
          super(message, cause);
     }
     
}
