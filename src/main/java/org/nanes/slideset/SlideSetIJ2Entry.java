package org.nanes.slideset;

import org.nanes.slideset.dm.DataTypeIDService;
import org.nanes.slideset.io.CSVService;
import org.nanes.slideset.io.XMLService;
import org.nanes.slideset.ui.SlideSetLauncher;
import org.nanes.slideset.ui.SlideSetLog;
import ij.IJ;
import net.imagej.ImageJ;
import net.imagej.legacy.LegacyService;
import org.scijava.command.Command;
import org.scijava.Context;
import org.scijava.ItemIO;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Entry point for ImageJ2/Fiji
 * @author Benjamin Nanes
 */
@Plugin(type=Command.class,
     menuPath="Plugins > Slide Set",
     description="Batch processing for ImageJ")
public class SlideSetIJ2Entry implements Command {
     
     @Parameter(label="ImageJ", type=ItemIO.INPUT)
     private ImageJ ij;
     
     @Parameter(label="Context", type=ItemIO.INPUT)
     private Context c;
     
     @Override
     public void run() {
          IJ.runPlugIn(Context.class.getName(), null);
          final LegacyService legacyService = (LegacyService)
                IJ.runPlugIn(LegacyService.class.getName(), null);
          final DataTypeIDService dtid = new DataTypeIDService(ij);
          final SlideSetLog log = new SlideSetLog();
          final XMLService xs = new XMLService(ij, dtid);
          final CSVService cs = new CSVService();
          final SlideSetLauncher win = new SlideSetLauncher(ij, dtid, xs, cs, log);
          if(!legacyService.isLegacyMode())
              legacyService.toggleLegacyMode(true);
          win.setVisible(true);   
     }
     
}
