package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.dm.write.ElementWriter;
import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 * Binds and {@link ElementWriter} to a {@code SlideSet}
 * table column.
 * 
 * @param <E> {@code DataElement} type written to by the
 *    bound writer
 * @param <P> "Processed" data type that can be written\
 *    by the bound writer
 * 
 * @author Benjamin Nanes
 */
public class ColumnBoundWriter<E extends DataElement, P> {
    
    // -- Fields --
    
    private ElementWriter<E, P> writer;
    private SlideSet data;
    private int column;
    
    // -- Constructor --
    
    /**
     * Bind an {@link ElementWriter} to a {@link SlideSet}
     * table column.
     * 
     * @param data Table to which the writer will be bound
     * @param column Index of the column to which the writer will be bound
     * @param writer Writer instance to bind to the table column
     */
    public ColumnBoundWriter(
            SlideSet data,
            int column,
            ElementWriter<E, P> writer) {
        if(data == null || writer == null)
            throw new IllegalArgumentException(
                    "Can't bind reader if binding info isn't provided!");
        if(data.getNumCols() <= column || column < 0)
            throw new IllegalArgumentException("Invalid column!");
        this.writer = writer;
        this.data = data;
        this.column = column;
    }
    
    // -- Methods --

    /**
     * Write data to the table
     * @param item "Processed" data to write
     * @param row Index of the table row to which
     *    the data should be written
     * @throws SlideSetException If {@code item} is not compatible
     *    with the writer
     */
    public void write(P item, int row) throws SlideSetException {
        writer.write(item, (E)data.getDataElement(column, row));
    }
    
    /** Get the column index */
    public int getColumnNum() {
        return column;
    }
    
    /** Get the bound writer */
    public ElementWriter<E, P> getWriter() {
        return writer;
    }
    
}
