package org.nanes.slideset.ui;

import java.awt.event.WindowListener;

/**
 * Interface for SlideSet window management functions
 * @author Benjamin Nanes
 */
public interface SlideSetWindow {
     
     /**
      * Perform any clean-up functions necessary to close the window.
      * May ask for user input, such as weather to save changes.
      */
     public void kill();
     
     public void addWindowListener(WindowListener l);
     
}
