package org.nanes.slideset.ex;

/**
 * Signals the column types of a table do not match the expected types.
 * @author Benjamin Nanes
 */
public class ColumnTypeException extends SlideSetException {

    public ColumnTypeException() {
    }

    public ColumnTypeException(String msg) {
        super(msg);
    }

    public ColumnTypeException(Throwable cause) {
        super(cause);
    }

    public ColumnTypeException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
