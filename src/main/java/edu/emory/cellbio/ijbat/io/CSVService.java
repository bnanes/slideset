package edu.emory.cellbio.ijbat.io;

import edu.emory.cellbio.ijbat.SlideSet;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Class for writing {@code SlideSet} data as a CSV file.
 * Does not save enough data to permit reading.
 * 
 * @author Benjamin Nanes
 */
public class CSVService {
     
     private FileOutputStream fos;
     private PrintWriter pw;
     
     /** Write a {@code SlideSet} to a CSV file */
     public void write(SlideSet data, File file) throws IOException {
          write(data, file, true);
     }
     public void write(SlideSet data, File file, boolean includeParent) throws IOException {
          if(!file.exists() && !file.createNewFile()) throw
               new IllegalArgumentException("Could not create file: " + file.getPath());
          if(data == null || !file.canWrite()) throw
               new IllegalArgumentException("Could not write to file: " + file.getPath());
          final SlideSet parent = data.getParent();
          boolean incp = includeParent && parent != null;
          try {
               fos = new FileOutputStream(file);
               pw = new PrintWriter(fos);
               pw.print(incp ? colNames(parent) + "," + colNames(data) : colNames(data));
               for(int i=0; i<data.getNumRows(); i++) {
                    pw.println();
                    if(incp) {
                         int pr = (Integer)data.getUnderlying(0, i); // Assumes the first column is the parent row index
                         pw.print(row(parent, pr) + "," + row(data, i));
                    }
                    else pw.print(row(data, i));
               }
               pw.flush();
               if(pw.checkError()) throw new IOException("Error in the print writer");
          }
          finally {
               pw.close();
               fos.close();
          }
     }
     
     // -- Helper methods --
     
     private String colNames(SlideSet data) {
          String result = "";
          for(int j=0; j<data.getNumCols(); j++) {
               result += data.getColumnName(j);
               if(j < data.getNumCols() - 1)
                    result += ",";
          }
          return result;
     }
     
     private String row(SlideSet data, int row) {
          if(!(row < data.getNumRows()))
               throw new IllegalArgumentException("Bad row index");
          String result = "";
          for(int j=0; j<data.getNumCols(); j++) {
               result += data.getItemText(j, row);
               if(j < data.getNumCols() - 1)
                    result += ",";
          }
          return result;
     }
     
     // -- Test methods --
     
}