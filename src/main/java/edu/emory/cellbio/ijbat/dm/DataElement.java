package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 * Basic element of data stored in {@link SlideSet} tables.
 * All {@code DataElement} types <em><b>must</b></em>
 * implement a zero-argument constructor that instantiates
 * the element with a default underlying value.
 * All {@code DataElement} types should also be marked
 * with the {@code @}{@link DataElementMetadata}
 * annotation type, although it is not currently used
 * for run-time recognition of {@code DataElement} classes.
 * 
 * @param <T> Class of this element's "underlying" data.
 * 
 * @author Benjamin Nanes
 */
public abstract class DataElement<T> {
    
    protected T underlying;
    private String mimeType = "";
    private SlideSet owner;
    
    // -- Constructor --
         
    public DataElement(T underlying) {
        this.underlying = underlying;
    }
    
    // -- Methods --
    
    /** Set this element's "underlying" value */
    public final void setUnderlying(T underlying) {
        this.underlying = underlying;
    }
    
    /** Get this element's "underlying" value */
    public final T getUnderlying() {
        return underlying;
    }
    
    /**
     * Set this element's "underlying" value based on a
     * {@code String}
     * @throws SlideSetException If {@code text} cannot
     * be converted to the appropriate underlying data type.
     */
    public abstract void setUnderlyingText(String text) throws SlideSetException;
    
    /** Get a {@code String} representation of this
     *  element's "underlying" value */
    public abstract String getUnderlyingText();
    
    /** Set the MIME type of this element */
    public final void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
    
    /** Get the MIME type of this element */
    public final String getMimeType() {
        return mimeType;
    }
    
    /** Set the {@code SlideSet} table which owns this element */
    public final void setOwner(SlideSet owner) {
        this.owner = owner;
    }
    
    /** Get the {@code SlideSet} table which owns this element */
    public final SlideSet getOwner() {
        return owner;
    }
    
}
