package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import java.util.ArrayList;

/**
 * Scheme for selecting among available options
 * for assigning plugin input parameter values.
 * May require user input.
 * 
 * @see edu.emory.cellbio.ijbat.pi.SlideSetPluginLoader
 * 
 * @author Benjamin Nanes
 */
public interface PluginInputPicker {
    
    /**
     * Register a plugin input and the options
     * available for assigning its value.
     * This method is invoked sequentially for
     * each input that cannot be filled automatically.
     * 
     * @param label Human-readable name of the input,
     *     i.e. for labeling a drop-down box.
     * <br>
     * @param choices Human-readable names for each
     *     available option for assigning the input value.
     * <br>
     * @param constantRequest If the corresponding option
     *     for assigning the input value requires a
     *     constant, the default value; otherwise, {@code null}.
     *     This array must have the same length as {@code choices}.
     * <br>
     * @param acceptableValues List of acceptable {@code String}
     *     values for this input parameter, or {@code null} if
     *     not applicable (i.e. no list of acceptable values, or
     *     parameter does not take a {@code String} value).
     */
    public void addInput(
            String label,
            String[] choices,
            Object[] constantRequest,
            String[] acceptableValues);
    
    /**
     * Get chosen options for assignment of each plugin
     * input value. This method is invoked after all inputs
     * have been registered. If user input is required, it
     * should block until all selections have been made.
     * 
     * @param inputChoices Empty list to be filled with index
     *    values of the selected input sources. The order of
     *    values corresponds to the order in which the plugin
     *    inputs were registered through {@link #addInput addInput}.
     *    The values correspond to the indeces of the selected
     *    option in the {@code choices} array passed to {@code addInput}.
     * <br>
     * @param constants Empty list to be filled with constant
     *    values selected, or {@code null} if no constant
     *    value is required for the selected option.
     * <br>
     * @throws OperationCanceledException If the user aborts
     *    the plugin, for example, by dismissing a dialog box
     *    without making the required selections.
     */
    public void getInputChoices(
            ArrayList<Integer> inputChoices,
            ArrayList<Object> constants)
            throws OperationCanceledException;
    
}
