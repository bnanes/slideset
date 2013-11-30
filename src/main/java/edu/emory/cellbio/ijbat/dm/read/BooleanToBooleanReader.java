package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.BooleanElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Logical",
        elementType = BooleanElement.class,
        processedType = Boolean.class,
        hidden = false)
public class BooleanToBooleanReader implements
        ElementReader<BooleanElement, Boolean> {

    public Boolean read(BooleanElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying();
    }
    
}
