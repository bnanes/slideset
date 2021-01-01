package org.nanes.slideset.dm.read;

import org.nanes.slideset.dm.StringElement;
import org.nanes.slideset.ex.SlideSetException;

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
