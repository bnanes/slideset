package org.nanes.slideset.ex;

/**
 * Signals a potentially recoverable SlideSet exception.
 * 
 * @author Benjamin Nanes
 */
public class SlideSetException extends Exception {

     /**
      * Creates a new instance of
      * <code>SlideSetException</code> without detail message.
      */
     public SlideSetException() {
     }

     /**
      * Constructs an instance of
      * <code>SlideSetException</code> with the specified detail message.
      *
      * @param msg the detail message.
      */
     public SlideSetException(String msg) {
          super(msg);
     }

     public SlideSetException(Throwable cause) {
          super(cause);
     }

     public SlideSetException(String message, Throwable cause) {
          super(message, cause);
     }
     
}
