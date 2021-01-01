package org.nanes.slideset.ui;

import org.nanes.slideset.ex.SlideSetException;

/**
 * GUI-agnostic interface for a documentation system.
 * 
 * @author Benjamin Nanes
 */
public interface HelpLoader {
    
    public void getHelp(String pageKey) throws SlideSetException;
    
    public void getHelp() throws SlideSetException;
    
}
