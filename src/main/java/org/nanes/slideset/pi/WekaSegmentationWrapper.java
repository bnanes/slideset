package org.nanes.slideset.pi;

import ij.ImagePlus;
import ij.plugin.RGBStackConverter;
import org.nanes.slideset.ui.SlideSetLog;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.legacy.convert.ImagePlusToDatasetConverter;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
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
    private ImagePlus imp;
    
    @Parameter(label="Classifier", type=ItemIO.INPUT)
    private WekaClassifierFile cfr;
    
    @Parameter(label="Probabilities", type=ItemIO.INPUT)
    private Boolean prob;
    
    @Parameter(label="RGB Model?", type=ItemIO.INPUT)
    private Boolean is_rgb;
    
    @Parameter(label="Classification", type=ItemIO.OUTPUT)
    private Dataset out;
    
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
        
        if(is_rgb) {
            RGBStackConverter.convertToRGB(imp);
        }
        
        ipd = new ImagePlusToDatasetConverter();

        WekaSegmentation ws = new WekaSegmentation(imp);
        
        ws.loadClassifier(cfr.getPath());
        ws.applyClassifier(prob);
        
        ImagePlus impOut = ws.getClassifiedImage();
        out = ij.getContext().getService(ConvertService.class).convert(impOut, Dataset.class);
    }
}
