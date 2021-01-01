package org.nanes.slideset.ui;

/**
 * Interface for receiving messages sent
 * from the {@link org.nanes.slideset.ui.SlideSetLog}.
 * 
 * @author Benjamin Nanes
 */
public interface LogListener {
     
     /**
      * Receive a message from the log.
      * Implementors should expect this
      * method to be called on the 
      * event dispatch thread.
      */
     public void logMessage(String message);
     
}
