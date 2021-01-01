package org.nanes.slideset.dm;

import net.imagej.overlay.AbstractOverlay;

/**
 * Alias for {@code imagej.data.overlay.AbstractOverlay[]}
 * 
 * @author Benjamin Nanes
 */
@TypeAliasMetadata()
public class AbstractOverlaysAlias implements TypeAlias {

    public Class getRealType() {
        return AbstractOverlay[].class;
    }
    
}
