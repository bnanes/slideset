package org.nanes.slideset.ex;

/**
 * Indicates that a new link cannot be generated because the default link
 * path is not set in the 
 * {@link org.nanes.slideset.SlideSet#columnProperties column properties}
 * @author Ben
 */
public class DefaultPathNotSetException extends LinkException {

     /**
      * Creates a new instance of
      * <code>DefaultPathNotSetException</code> without detail message.
      */
     public DefaultPathNotSetException() {
     }

     /**
      * Constructs an instance of
      * <code>DefaultPathNotSetException</code> with the specified detail message.
      *
      * @param msg the detail message.
      */
     public DefaultPathNotSetException(String msg) {
          super(msg);
     }

     public DefaultPathNotSetException(Throwable cause) {
          super(cause);
     }

     public DefaultPathNotSetException(String message, Throwable cause) {
          super(message, cause);
     }
}
