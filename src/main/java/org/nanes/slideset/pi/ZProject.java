package org.nanes.slideset.pi;

import ij.ImagePlus;
import ij.plugin.ZProjector;
import net.imagej.Dataset;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.convert.ConvertService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Maximum-intensity Z-projection
 * @author Benjamin Nanes
 */
@HelpPath(path = "plugins/stacks.html")
@Plugin( type=SlideSetPlugin.class,
         name="Z-Project",
         label="Z-Project", visible = false,
         menuPath="Plugins > Slide Set > Commands > Stacks > Z-Project")
public class ZProject extends SlideSetPlugin {

    // -- Parameters --
    
    @Parameter(label = "Context", type = ItemIO.INPUT)
    private Context context;
    
    @Parameter(label = "Image", type = ItemIO.INPUT)
    private ImagePlus image;
    
    @Parameter(label = "Z-Projection", type = ItemIO.OUTPUT)
    private Dataset zProj;

    @Override
    public void run() {
        ImagePlus zProjIp = ZProjector.run(image, "max");
        ConvertService cs = context.getService(ConvertService.class);
        zProj = cs.convert(zProjIp, Dataset.class);
    }
    
}
