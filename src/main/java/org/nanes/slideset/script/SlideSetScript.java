package org.nanes.slideset.script;

import ij.IJ;
import java.io.File;
import java.io.IOException;
import net.imagej.ImageJ;
import org.nanes.slideset.SlideSet;
import org.nanes.slideset.dm.DataTypeIDService;
import org.nanes.slideset.ex.OperationCanceledException;
import org.nanes.slideset.ex.SlideSetException;
import org.nanes.slideset.io.CSVService;
import org.nanes.slideset.io.XMLService;
import org.nanes.slideset.pi.PluginInputPicker;
import org.nanes.slideset.pi.PluginOutputPicker;
import org.nanes.slideset.pi.SlideSetPluginLoader;
import org.nanes.slideset.ui.HttpHelpLoader;
import org.nanes.slideset.ui.LogListener;
import org.nanes.slideset.ui.SlideSetLog;

/**
 *
 * @author Benjamin Nanes
 */
public class SlideSetScript implements LogListener {
    
    private static final int LOGDEFAULT = 0;
    private static final int IJCONSOLE = 1;
    private static final int STDOUT = 2;
    private static final int STDERR = 3;
    
    private ImageJ ij;
    private DataTypeIDService dtids;
    private XMLService xmls;
    private CSVService csvs;
    private SlideSetLog sslg;
    private HttpHelpLoader hhl;
    private SlideSetPluginLoader sspl;
    private int logTo;

    public SlideSetScript(ImageJ ij, int logTo, boolean showConsole) {
        if(ij == null)
            throw new IllegalArgumentException("ImageJ context required.");
        if(logTo == LOGDEFAULT)
            logTo = showConsole ? IJCONSOLE : STDOUT;
        if(logTo != IJCONSOLE && logTo != STDOUT && logTo != STDERR)
            throw new IllegalArgumentException("Invalid log destination.");
        this.ij = ij;
        dtids = new DataTypeIDService(ij);
        xmls = new XMLService(ij, dtids);
        csvs = new CSVService();
        this.logTo = logTo;
        sslg = new SlideSetLog();
        sslg.registerListener(this);
        hhl = new HttpHelpLoader(ij);
        sspl = new SlideSetPluginLoader(ij, dtids, sslg, hhl);        
        if(showConsole && logTo == IJCONSOLE)
            IJ.run("Console", "");
    }
    
    public SlideSetScript(ImageJ ij) {
        this(ij, IJCONSOLE, true);
    }
    
    public SlideSetScript(ImageJ ij, int logTo) {
        this(ij, logTo, true);
    }
    
    public SlideSetScript(ImageJ ij, boolean showConsole) {
        this(ij, LOGDEFAULT, showConsole);
    }
    
    public ImageJ getImaegJ() {
        return ij;
    }
    
    public DataTypeIDService getDataTypeIDService() {
        return dtids;
    }
    
    public XMLService getXMLService() {
        return xmls;
    }
    
    public SlideSetPluginLoader getSlideSetPluginLoader() {
        return sspl;
    }
    
    public SlideSet loadSlideSet(File file) {
        SlideSet table;
        try {
            table = xmls.read(file);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
        return table;
    }
    
    public SlideSet loadSlideSet(String fileName) {
        return loadSlideSet(new File(fileName));
    }
    
    public void saveSlideSet(SlideSet table, File file) {
        try {
            xmls.write(table, file);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public void saveSlideSet(SlideSet table, String fileName) {
        saveSlideSet(table, new File(fileName));
    }
    
    public SlideSet runPlugin(String className, SlideSet table, PluginInputPicker pip, PluginOutputPicker pop) {
        SlideSet resultTable;
        try {
            resultTable = sspl.runPlugin(className, table, pip, pop);
        } catch(OperationCanceledException e) {
            sslg.println("[SlideSetScript] Command cancelled, returning null.");
            return null;
        } catch(SlideSetException e) {
            throw new IllegalArgumentException(e);
        }
        return resultTable;
    }
    
    public void saveCSV(SlideSet table, File file) {
        try {
            csvs.write(table, file, false);
        } catch (IOException e) {
            sslg.println("[SlideSetScript] Failed to write CSV file.");
            throw new IllegalArgumentException(e);
        }
    }
    
    public void saveCSV(SlideSet table, String fileName) {
        saveCSV(table, new File(fileName));
    }
    
    public String[] getColumnText(SlideSet table, int index) {
        if(index < 0 | index >= table.getNumCols()) {
            sslg.println("[SlideSetScript] Invalid column index.");
            throw new IllegalArgumentException("[SlideSetScript] Invalid column index.");
        }
        int nrow = table.getNumRows();
        String[] val = new String[nrow];
        for(int i = 0; i < nrow; i++)
            val[i] = table.getItemText(index, i);
        return val;
    }
    
    public String[] getColumnText(SlideSet table, String colName) {
        int colInd = table.getColumnIndex(colName);
        if(colInd < 0) {
            sslg.println("[SlideSetScript] Invalid column name: " + colName);
            throw new IllegalArgumentException("[SlideSetScript] Invalid column name: " + colName);
        }
        return getColumnText(table, colInd);
    }

    @Override
    public void logMessage(String message) {
        switch(logTo) {
            case IJCONSOLE:
                ij.log().info(message);
                break;
            case STDOUT:
                System.out.print(message);
                break;
            case STDERR:
                System.err.print(message);
        }
    }
    
}
