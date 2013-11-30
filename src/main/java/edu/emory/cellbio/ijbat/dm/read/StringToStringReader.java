package edu.emory.cellbio.ijbat.dm.read;

import edu.emory.cellbio.ijbat.dm.StringElement;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
@ElementReaderMetadata(
        name = "Text",
        elementType = StringElement.class,
        processedType = String.class,
        hidden = false)
public class StringToStringReader implements ElementReader<StringElement, String> {

    public String read(StringElement elementToRead) throws SlideSetException {
        return elementToRead.getUnderlying();
    }
    
}
