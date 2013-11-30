package edu.emory.cellbio.ijbat.dm;

import imagej.data.overlay.AbstractOverlay;

/**
 *
 * @author Benjamin Nanes
 */
@TypeAliasMetadata()
public class AbstractOverlaysAlias implements TypeAlias {

    public Class getRealType() {
        return AbstractOverlay[].class;
    }
    
}
