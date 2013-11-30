package edu.emory.cellbio.ijbat.ui;

import java.util.ArrayList;
import javax.swing.SwingUtilities;

/**
 * Utility for passing log messages between Slide Set components.
 * 
 * @author Benjamin Nanes
 */
public class SlideSetLog {
     
     // -- Fields --
     
     private ArrayList<LogListener> listeners = new ArrayList<LogListener>();
     
     // -- Constructor --
     
     public SlideSetLog() {
          
     }
     
     // -- Methods --
     
     /**
      * Register a {@link LogListener} to receive messages.
      */
     public void registerListener(final LogListener l) {
          listeners.add(l);
     }
     
     /**
      * Relay a message to the log listeners.
      * Messages will be sent asynchronously
      * on the event dispatch thread.
      */
     public void print(final String m) {
          for(final LogListener l : listeners)
               SwingUtilities.invokeLater( new Runnable() {
                    @Override
                    public void run() { l.logMessage(m); };
               });          
     }
     
     /**
      * Relay a message (with newline character appended) 
      * to the log listeners.
      * Messages will be sent asynchronously
      * on the event dispatch thread.
      */
     public void println(final String m) {
          print(m + "\n");
     }

}
