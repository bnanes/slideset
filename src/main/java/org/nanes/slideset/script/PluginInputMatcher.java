package org.nanes.slideset.script;

import java.util.ArrayList;
import java.util.HashMap;
import org.nanes.slideset.ex.OperationCanceledException;
import org.nanes.slideset.pi.PluginInputPicker;
import org.nanes.slideset.ui.HelpLoader;

/**
 * A scriptable (i.e. headless) implementation of {@link PluginInputPicker}.
 * No run-time user input is accepted. To register a handling choice for
 * an input parameter, call {@link makeColumnChoice} or {@link makeConstantChoice}
 * prior to passing the PluginOutputMatcher instance
 * to the {@link org.nanes.slideset.pi.SlideSetPluginLoader SlideSetPluginLoader}.
 * Note that a handling choice must be made for each input parameter (i.e. no
 * defaults will be used).
 * @author Benjamin Nanes
 */
public class PluginInputMatcher implements PluginInputPicker {
    
    private final HashMap<String, String> columnChoices = new HashMap();
    private final HashMap<String, String> constantChoices = new HashMap();
    private final ArrayList<Integer> choices = new ArrayList<>();
    private final ArrayList<String> constants = new ArrayList<>();
    
    /**
     * Register a column choice for a parameter
     * @param label - Label in the parameter annotation
     * @param colName - Name of the column to choose
     */
    public void makeColumnChoice(String label, String colName) {
        columnChoices.put(label, colName);
        constantChoices.put(label, null);
    }
    
    /**
     * Register a constant for a parameter
     * @param label - Label in the parameter annotation
     * @param typeName - Value type (ex. Text, Logical, etc.)
     * @param value - Constant value, as String
     */
    public void makeConstantChoice(String label, String typeName, String value) {
        columnChoices.put(label, typeName + " constant");
        constantChoices.put(label, value);
    }

    @Override
    public void addInput(String label, String[] choices, Object[] constantRequest, String[] acceptableValues) {
        String columnChoice = columnChoices.get(label);
        if(columnChoice == null)
            throw new IllegalArgumentException("No choices have been registered for required parameter " + label);
        String constantChoice = constantChoices.get(label);
        for(int cIndex = 0; cIndex < choices.length; cIndex ++) {
            if(!choices[cIndex].contains(columnChoice))
                continue;
            this.choices.add(cIndex);
            this.constants.add(constantChoice);
            return;
        }
        throw new IllegalArgumentException("The selection for parameter " + label + " (" + columnChoice + ") does not match any of the appropriate options.");
    }

    @Override
    public void setHelpPath(String helpPath, HelpLoader helpLoader) {
        // No help option in non-interactive mode
    }

    @Override
    public void getInputChoices(ArrayList<Integer> inputChoices, ArrayList<Object> constants) throws OperationCanceledException {
        for(int i = 0; i < choices.size(); i++) {
            inputChoices.add(choices.get(i));
            constants.add(this.constants.get(i));
        }
    }
    
}
