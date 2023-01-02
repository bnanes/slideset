package org.nanes.slideset.script;

import java.util.ArrayList;
import java.util.HashMap;
import org.nanes.slideset.ex.OperationCanceledException;
import org.nanes.slideset.pi.PluginOutputPicker;

/**
 * A scriptable (i.e. headless) implementation of {@link PluginOutputPicker}.
 * No run-time user input is accepted. To register a handling choice
 * for an output parameter, call {@link saveOutputValue} or 
 * {@link saveOutputFile} prior to passing the PluginOutputMatcher instance
 * to the {@link org.nanes.slideset.pi.SlideSetPluginLoader SlideSetPluginLoader}.
 * If no handling choice is selected for an output, the default will be used.
 * @author Benjamin Nanes
 */
public class PluginOutputMatcher implements PluginOutputPicker {
    
    private final HashMap<String, String> customTypes = new HashMap<>();
    private final HashMap<String, String> customLinkDirs = new HashMap<>();
    private final HashMap<String, String> customLinkPres = new HashMap<>();
    private final HashMap<String, String> customLinkExts = new HashMap<>();
    private final ArrayList<String> savedParentFields = new ArrayList<>();
    
    private final ArrayList<Integer> outputChoices = new ArrayList<>();
    private final ArrayList<Integer> selectedParentFields = new ArrayList<>();
    private final ArrayList<String> linkDir = new ArrayList<>();
    private final ArrayList<String> linkPre = new ArrayList<>();
    private final ArrayList<String> linkExt = new ArrayList<>();
    
    /**
     * Register an output to be saved directly in the result table.
     * Note that this should not be required in the vast majority of cases, as it is usually the default behavior.
     * @param label - Output parameter annotation label
     * @param type - Type name (ex. Text)
     */
    public void saveOutputValue(String label, String type) {
        customTypes.put(label, type + " value");
    }
    
    /**
     * Register an output to be saved as a file link. This may be useful if there are multiple writers available for the data.
     * It also provides the option to specify output file names.
     * @param label - Output parameter annotation label
     * @param type - Type name (ex. "Image file", "SVG file")
     * @param linkDir - File directory (null for default)
     * @param linkPre - File name prefix (null for default)
     * @param linkExt - File extension (null for default)
     */
    public void saveOutputFile(String label, String type, String linkDir, String linkPre, String linkExt) {
        customTypes.put(label, type);
        customLinkDirs.put(label, linkDir);
        customLinkPres.put(label, linkPre);
        customLinkExts.put(label, linkExt);
    }
    
    /**
     * Register a parent field to be copied to the result table
     * @param label - Name of the parent field (column) to save
     */
    public void saveParentField(String label) {
        savedParentFields.add(label);
    }

    @Override
    public void addOutput(String label, String[] choices, boolean[] link, String[] linkDir, String[] linkPre, String[] linkExt) {
        if(choices == null || choices.length < 1)
            throw new IllegalArgumentException("No options available for handling output parameter " + label);
        String type = customTypes.get(label);
        if(type == null) { // Use defaults if no choices entered
            outputChoices.add(0);
            this.linkDir.add(linkDir[0]!=null ? linkDir[0] : "dir");
            this.linkPre.add(linkPre[0]!=null ? linkPre[0] : "result");
            this.linkExt.add(linkExt[0]!=null ? linkExt[0] : "txt");
        } else {
            for(int i = 0; i < choices.length; i++) {
                if(!choices[i].contains(type))
                    continue;
                outputChoices.add(i);
                String cld = customLinkDirs.get(label);
                String clp = customLinkPres.get(label);
                String cle = customLinkExts.get(label);
                this.linkDir.add(cld!=null ? cld : (linkDir[i]!=null ? linkDir[i] : "dir"));
                this.linkPre.add(clp!=null ? clp : (linkPre[i]!=null ? linkPre[i] : "result"));
                this.linkExt.add(cle!=null ? cle : (linkExt[i]!=null ? linkExt[i] : "txt"));
                return;
            }
            throw new IllegalArgumentException("Ouput parameter " + label + " has an invalid output type set ( " + type + ")");
        }
    }

    @Override
    public void setParentFieldLabels(String[] labels) {
        for(int i = 0; i < labels.length; i++) {
            if(savedParentFields.contains(labels[i]))
                selectedParentFields.add(i);
        }
    }

    @Override
    public void getOutputChoices(ArrayList<Integer> outputChoices, ArrayList<Integer> selectedParentFields, ArrayList<String> linkDir, ArrayList<String> linkPre, ArrayList<String> linkExt) throws OperationCanceledException {
        for(int i = 0; i < this.outputChoices.size(); i++) {
            outputChoices.add(this.outputChoices.get(i));
            linkDir.add(this.linkDir.get(i));
            linkPre.add(this.linkPre.get(i));
            linkExt.add(this.linkExt.get(i));
        }
        for(int i = 0; i < this.selectedParentFields.size(); i++) {
            selectedParentFields.add(this.selectedParentFields.get(i));
        }
    }
    
}
