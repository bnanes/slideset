package edu.emory.cellbio.ijbat.io;

import edu.emory.cellbio.ijbat.SlideSet;

import ij.gui.Roi;
import ij.io.RoiEncoder;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utility methods related to IO
 * @author Benjamin Nanes
 */
public class Util {
     
     protected Util() { }
     
     /** Set the working directory for a {@code SlideSet}, along with
      *  all of its relatives  */
     public static void setPathForTree(SlideSet data, String workingDirectory) {
          while(data.getParent() != null)
               data = data.getParent();
          setPath(data, workingDirectory);
     }
     
     private static void setPath(SlideSet data, String dir) {
          data.setWorkingDirectory(dir);
          for(SlideSet child : data.getChildren())
               setPath(child, dir);
     }
     
     /** Make {@code path} relative to {@code workingDirectory}, if possible */
     public static String makePathRelative(String path, String workingDirectory) {
          if(path == null || workingDirectory == null ||
               path.equals("") || workingDirectory.equals(""))
               return path;
          String[] p = path.split("[/\\\\]");
          String[] wd = workingDirectory.split("[\\\\/]");
          int i = 0;
          for( ; i<p.length; i++) {
               if(i >= wd.length)
                    break;
               if(!p[i].equals(wd[i]))
                    return path;
          }
          String result = "";
          for(int j=i; j<p.length; j++) {
               result += p[j];
               if(j < p.length-1)
                    result += "/";
          }
          return result;
     }
     
     /** Utility function to write an ROI set file.  Overwrites any existing file. */
     public static boolean writeRoiSetFile(Roi[] roi, File file) {
          if(file.isFile()) file.delete();
          if(roi==null || roi.length==0) return true;
		 try {
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(zos));
            RoiEncoder re = new RoiEncoder(out);
            for (int i=0; i<roi.length; i++) {
                String label = "ROI_" + String.valueOf(i);
                if (!label.endsWith(".roi")) label += ".roi";
                zos.putNextEntry(new ZipEntry(label));
                re.write(roi[i]);
                out.flush();
            }
            out.close();
          }
          catch (IOException e) { return false; }
          return true;
     }
     
}
