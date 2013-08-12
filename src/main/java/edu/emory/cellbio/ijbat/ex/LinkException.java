package edu.emory.cellbio.ijbat.ex;

/**
 * Signals a problem with a data table link (in a cell with an
 * associated {@link edu.emory.cellbio.ijbat.dm.LinkLinker LinkLinker}.
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
