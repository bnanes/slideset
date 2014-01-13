package edu.emory.cellbio.ijbat;

import ij.IJ;
import ij.plugin.PlugIn;
import imagej.legacy.DefaultLegacyService;
import imagej.legacy.LegacyService;
import org.scijava.Context;

/**
 * Entry point for ImageJ1 / Fiji
 * @author Benjamin Nanes
 */
public class SlideSetIJ1Entry implements PlugIn {

    public void run(String string) {
        IJ.runPlugIn(Context.class.getName(), null);
        final LegacyService legacyService = (LegacyService)
                IJ.runPlugIn(LegacyService.class.getName(), null);
        if (legacyService == null) {
                IJ.error("No LegacyService available!");
                return;
        }
        legacyService.toggleLegacyMode(false);
        imagej.ImageJ ij = new imagej.ImageJ(legacyService.getContext());
        ij.command().run(SlideSetIJ2Entry.class, true, (Object[]) null);
    }
    
    static {
		DefaultLegacyService.preinit();
	}
    
    public static void main(String... args) {
        ij.ImageJ.main(new String[0]);
        new SlideSetIJ1Entry().run("");
    }
    
}