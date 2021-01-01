package org.nanes.slideset.ex;

/**
 * Signals that a command cannot be run on a table
 * one of the plugin inputs requires a type which
 * cannot be read from the table or entered as a constant.
 * 
 * @author Benjamin Nanes
 */
public class NoPluginInputSourceException extends SlideSetException {

    /**
     * Creates a new instance of
     * <code>NoPluginInputSourceException</code> without detail message.
     */
    public NoPluginInputSourceException() {
    }

    /**
     * Constructs an instance of
     * <code>NoPluginInputSourceException</code> with the specified detail
     * message.
     *
     * @param msg the detail message.
     */
    public NoPluginInputSourceException(String msg) {
        super(msg);
    }

    public NoPluginInputSourceException(Throwable cause) {
        super(cause);
    }

    public NoPluginInputSourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
