package edu.emory.cellbio.ijbat.dm;

import ij.gui.Roi;

/**
 * Alias for {@code ij.gui.Roi[]}
 * 
 * @author Benjamin Nanes
 */
@TypeAliasMetadata()
public class RoisAlias implements TypeAlias {
    
    public Class getRealType() {
        return Roi[].class;
    }
    
}
