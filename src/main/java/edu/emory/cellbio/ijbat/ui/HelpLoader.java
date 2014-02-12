package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 * GUI-agnostic interface for a documentation system.
 * 
 * @author Benjamin Nanes
 */
public interface HelpLoader {
    
    public void getHelp(String pageKey) throws SlideSetException;
    
    public void getHelp() throws SlideSetException;
    
}
