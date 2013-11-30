package edu.emory.cellbio.ijbat.dm;

/**
 *
 * @author Benjamin Nanes
 */
@DataElementMetadata(name = "~ File")
public class FileLinkElement extends DataElement<String> implements FileLink {

    public FileLinkElement() {
        super("");
    }
    public FileLinkElement(String underlying) {
        super(underlying);
    }

    @Override
    public String getUnderlyingText() {
        return underlying;
    }

    @Override
    public void setUnderlyingText(String text) {
        underlying = text;
    }
    
}
