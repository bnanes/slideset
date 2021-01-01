package org.nanes.slideset.dm;

import org.nanes.slideset.dm.read.ElementReader;
import org.nanes.slideset.SlideSet;
import org.nanes.slideset.ex.SlideSetException;

/**
 * Binds an {@link ElementReader} to a {@link SlideSet}
 * table column or to a constant value.
 * 
 * @param <E> {@code DataElement} type read by the bound reader
 * @param <P> "Processed" data type produced by the bound reader
 * 
 * @author Benjamin Nanes
 */
public class ColumnBoundReader<E extends DataElement, P> {
    
    // -- Fields --
    
    private ElementReader<E, P> reader;
    private SlideSet data;
    private int column;
    private E constant;
    private final String typeName;
    private final String columnName;
    
    // -- Constructor --
    
    /**
     * Bind an {@link ElementReader} to a {@link SlideSet}
     * table column.
     * 
     * @param data The table to which the reader should be bound
     * @param column Index of the column to which the reader should be bound
     * @param reader Reader instance to bind to the column
     */
    public ColumnBoundReader(
            SlideSet data,
            int column,
            ElementReader<E, P> reader) {
        if(data == null || reader == null)
            throw new IllegalArgumentException(
                    "Can't bind reader if binding info isn't provided!");
        if(data.getNumCols() <= column || column < 0)
            throw new IllegalArgumentException("Invalid column!");
        this.data = data;
        this.reader = reader;
        this.column = column;
        typeName = data.getColumnTypeName(column);
        columnName = data.getColumnName(column);
    }
    
    /**
     * Bind an {@link ElementReader} to a constant value. The
     * {@code ColumnBoundReader} created will behave similarly
     * to a normal {@code ColumnBoundReader}, except that it
     * will always read from the {@code constant} parameter,
     * regardless of the table row specified.
     * 
     * @param constant "Underlying" value to be read
     * @param reader Reader instance to bind to the constance
     * @param dtid  Instance of {@code DataTypeIDService}, needed
     *    to generate a human-readable name for the "column" type
     */
    public ColumnBoundReader(
            E constant,
            ElementReader<E, P> reader,
            DataTypeIDService dtid) {
        if(constant == null || reader == null)
            throw new IllegalArgumentException(
                    "Can't bind reader if binding info isn't provided!");
        this.constant = constant;
        this.reader = reader;
        columnName = constant.getUnderlyingText() + " (constant)";
        typeName = dtid.getReadableElementType(
                constant.getClass(), constant.getMimeType());
    }
    
    // -- Methods --
    
    /** Read data from the specified row */
    public P read(int row) throws SlideSetException {
        if(data != null)
            return reader.read((E)data.getDataElement(column, row));
        if(constant != null)
            return reader.read(constant);
        throw new IllegalStateException("Can't read because no data has been bound!");
    }
    
    /** Get a human-readable label for the column type */
    public String getColumnTypeName() {
        return typeName;
    }
    
    /** Get the human-readable label for the column */
    public String getColumnName() {
        return columnName;
    }
    
    /** Get the column index */
    public int getColumnNum() {
        return column;
    }
    
}
