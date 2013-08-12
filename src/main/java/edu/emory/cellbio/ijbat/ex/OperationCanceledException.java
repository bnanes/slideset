package edu.emory.cellbio.ijbat.ex;

/**
 * Thrown when a SlideSet operation has been canceled.
 * 
 * @author Benjamin Nanes
 */
public class OperationCanceledException extends SlideSetException {

     /**
      * Creates a new instance of
      * <code>OperationCanceledException</code> without detail message.
      */
     public OperationCanceledException() {
     }

     /**
      * Constructs an instance of
      * <code>OperationCanceledException</code> with the specified detail
      * message.
      *
      * @param msg the detail message.
      */
     public OperationCanceledException(String msg) {
          super(msg);
     }

     public OperationCanceledException(Throwable cause) {
          super(cause);
     }

     public OperationCanceledException(String message, Throwable cause) {
          super(message, cause);
     }
}
