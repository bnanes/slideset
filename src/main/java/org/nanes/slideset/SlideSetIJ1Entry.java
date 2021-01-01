package org.nanes.slideset;

import ij.IJ;
import ij.plugin.PlugIn;
import net.imagej.legacy.LegacyService;
import org.scijava.Context;

/**
 * Note: This plugin is probably no longer necessary, as Fiji recognizes IJ2 plugins
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
        net.imagej.ImageJ ij = new net.imagej.ImageJ(legacyService.getContext());
        ij.command().run(SlideSetIJ2Entry.class, true, (Object[]) null);
    }
    
    public static void main(String... args) {
        ij.ImageJ.main(new String[0]);
        new SlideSetIJ1Entry().run("");
    }
    
}
