package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import java.util.ArrayList;

/**
 * Scheme for selecting among available options for
 * handling plugin output values.
 * May require user input.
 * 
 * @see edu.emory.cellbio.ijbat.pi.SlideSetPluginLoader
 * 
 * @author Benjamin Nanes
 */
public interface PluginOutputPicker {
    
    /**
     * Register a plugin output and available
     * options for handling its value.
     * This method is invoked sequentially for
     * each plugin output parameter.
     * 
     * @param label Human-readable name of the output,
     *     i.e. for labeling a drop-down box.
     * <br>
     * @param choices Human-readable names for each
     *     available option for handling the output value.
     * <br>
     * @param link Indicator for an output handling option
     *     that should be treated as a file link, and will
     *     require values for the link path, file name
     *     prefix, and file extension.
     * <br>
     * @param linkDir Default path used to write output
     *     data for output handling options using file
     *     links (ignored for others). May be {@code null},
     *     in which case this {@code PluginOutputPicker}
     *     will determine the default.
     * <br>
     * @param linkPre Default file prefix used to write output
     *     data for output handling options using file
     *     links (ignored for others). May be {@code null},
     *     in which case this {@code PluginOutputPicker}
     *     will determine the default.
     * <br>
     * @param linkExt Default file extension used to write
     *     output data for output handling options using file
     *     links (ignored for others). May be {@code null},
     *     in which case this {@code PluginOutputPicker}
     *     will determine the default.
     */
    public void addOutput(
            String label,
            String[] choices,
            boolean[] link,
            String[] linkDir,
            String[] linkPre,
            String[] linkExt);
    
    /**
     * Register fields from the parent table that
     * are available for copying to the results table.
     * 
     * @param labels Human-readable names for the fields.
     */
    public void setParentFieldLabels(String[] labels);
    
    /**
     * Get chosen options for handling of each plugin output
     * value, as well as fields from the parent table
     * that should be copied to the results table.
     * This method is invoked after all outputs
     * have been registered. If user input is required, it
     * should block until all selections have been made.
     * 
     * @param outputChoices Empty list to be filled with index
     *    values of the selected output handling methods. The order of
     *    values corresponds to the order in which the plugin
     *    outputs were registered through {@link #addOutput addOutput}.
     *    The values correspond to the indeces of the selected
     *    option in the {@code choices} array passed to {@code addOutput}.
     * <br>
     * @param selectedParentFields Empty list to be filled with
     *    index values of the fields from the parent table that
     *    have been selected for copying to the results table.
     *    Values correspond to the indeces of selected fields
     *    in the {@code labels} array passed to
     *    {@link #setParentFieldLabels setParentFieldLabels}.
     * <br>
     * @param linkDir Empty list to be filled with default paths
     *    to be used to write output data, or {@code null} if
     *    the selected plugin output handling method does not
     *    require a file link.
     * <br>
     * @param linkPre Empty list to be filled with default file
     *    prefixes to be used to write output data, or {@code null} if
     *    the selected plugin output handling method does not
     *    require a file link.
     * <br>
     * @param linkExt Empty list to be filled with default file
     *    extensions to be used to write output data, or {@code null} if
     *    the selected plugin output handling method does not
     *    require a file link.
     * 
     * @throws OperationCanceledException If the user aborts
     *    the plugin, for example, by dismissing a dialog box
     *    without making the required selections.
     */
    public void getOutputChoices(
            ArrayList<Integer> outputChoices,
            ArrayList<Integer> selectedParentFields,
            ArrayList<String> linkDir,
            ArrayList<String> linkPre,
            ArrayList<String> linkExt)
            throws OperationCanceledException;
    
}
