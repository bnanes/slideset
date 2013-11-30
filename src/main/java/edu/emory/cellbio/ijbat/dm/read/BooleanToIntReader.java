package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.BooleanElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "0 or 1",
        elementType = BooleanElement.class,
        processedType = Integer.class,
        hidden = true)
public class BooleanToIntReader implements
        ElementReader<BooleanElement, Integer> {

    public Integer read(BooleanElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying() ? 1 : 0;
    }
    
}
