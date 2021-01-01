package org.nanes.slideset.pi;

import org.nanes.slideset.ui.SlideSetLog;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.legacy.convert.DatasetToImagePlusConverter;
import net.imagej.legacy.convert.ImagePlusToDatasetConverter;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import trainableSegmentation.WekaSegmentation;

/**
 * Wrapper to access the Trainable Weka Segmentation plugin
 * 
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/trainablesegmentation.html")
@Plugin( type=SlideSetPlugin.class,
         name="Trainable Weka Segmentation",
         label="Trainable Weka Segmentation", visible = false,
         menuPath="Plugins > Slide Set > Commands > Segmentation > Trainable Weka Segmentation")
public class WekaSegmentationWrapper extends SlideSetPlugin {
    
    // -- Parameters --
    
    @Parameter(label="ImageJ", type=ItemIO.INPUT)
    private ImageJ ij;
    
    @Parameter(label="Log", type=ItemIO.INPUT)
    private SlideSetLog log;
    
    @Parameter(label="Images", type=ItemIO.INPUT)
    private Dataset ds;
    
    @Parameter(label="Classifier", type=ItemIO.INPUT)
    private WekaClassifierFile cfr;
    
    @Parameter(label="Probabilities", type=ItemIO.INPUT)
    private Boolean prob;
    
    @Parameter(label="Classification", type=ItemIO.OUTPUT)
    private Dataset out;
    
    private DatasetToImagePlusConverter dip;
    private ImagePlusToDatasetConverter ipd;
    
    // -- Methods --
    
    public void run() {
        try {
            this.getClass().getClassLoader().loadClass("trainableSegmentation.WekaSegmentation");
        } catch(ClassNotFoundException e) {
            log.println("####################################################");
            log.println("Error: Trainable Weka Segmentation plugin not found.");
            log.println("       If the plugin is not included with Fiji, it");
            log.println("       must be installed seperately.");
            log.println("####################################################");
            throw new IllegalArgumentException();
        }
        WekaSegmentation ws = new WekaSegmentation(dip.convert(ds, ij.ImagePlus.class));
        
        ws.loadClassifier(cfr.getPath());
        ws.applyClassifier(prob);
        out = ipd.convert(ws.getClassifiedImage(), Dataset.class);
    }
    
    
}
