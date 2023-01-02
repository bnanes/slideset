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
 * Use Slide Set in scripts.
 * <br>
 * Instantiate and access the set of Slide Set services without
 * any GUI elements. Instances of this class provide a {@link DataTypeIDService},
 * {@link XMLService}, {@link CSVService}, {@link SlideSetLog}, and
 * {@link SlideSetPluginLoader}, functioning as a complete Slide Set application.
 * In contrast to the {@linkplain  org.nanes.slideset.SlideSetIJ2Entry ImageJ command},
 * {@code SlideSetScript} does not launch a {@link org.nanes.slideset.ui.SlideSetLauncher SlideSetLauncher}
 * window, and is thus more suitable for programmatic use. To run Slide Set
 * commands without any GUI interactivity, use in conjunction with {@link PluginInputMatcher} and {@link PluginOutputMatcher}.
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

    /**
     * Instantiate headless Slide Set services
     * @param ij {@link ImageJ} instance to attach
     * @param logTo Output options for log messages 
     * <ul><li> {@code LOGDEFAULT}, make a reasonable guess between {@code STDOIT} and the ImageJ console </li>
     * <li> {@code IJCONSOLE}, the ImageJ console (GUI) </li>
     * <li> {@code STDOUT} </li>
     * <li> {@code STDERR} </li></ul>
     * @param showConsole If {@code true} and logging to the ImageJ console,
     * open the console when starting Slide Set.
     */
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
    
    /**
     * Get the ImageJ instance bound to this class
     * @return 
     */
    public ImageJ getImaegJ() {
        return ij;
    }
    
    /**
     * Get the {@link DataTypeIDService} for managing and matching data types.
     * @return 
     */
    public DataTypeIDService getDataTypeIDService() {
        return dtids;
    }
    
    /**
     * Get the {@link XMLService} for reading and writing Slide Set data files.
     * @return 
     */
    public XMLService getXMLService() {
        return xmls;
    }
    
    /**
     * Get the {@link SlideSetPluginLoader} for preparing and running Slide Set commands.
     * @return 
     */
    public SlideSetPluginLoader getSlideSetPluginLoader() {
        return sspl;
    }
    
    /**
     * Load a Slide Set data file
     * @param file
     * @return 
     */
    public SlideSet loadSlideSet(File file) {
        SlideSet table;
        try {
            table = xmls.read(file);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
        return table;
    }
    
    /**
     * Load a slide set data file
     * @param fileName
     * @return 
     */
    public SlideSet loadSlideSet(String fileName) {
        return loadSlideSet(new File(fileName));
    }
    
    /**
     * Save a Slide Set data file
     * @param table Slide Set data table, possibly with children
     * @param file 
     */
    public void saveSlideSet(SlideSet table, File file) {
        try {
            xmls.write(table, file);
        } catch(Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    /**
     * Save a Slide Set data file
     * @param table Slide Set data table, possibly with children
     * @param fileName 
     */
    public void saveSlideSet(SlideSet table, String fileName) {
        saveSlideSet(table, new File(fileName));
    }
    
    /**
     * Run a Slide Set command programmatically
     * @param className Fully qualified name of the Slide Set command class. Ex: {@code org.nanes.slideset.pi.ROILengths}
     * @param table Slide Set table
     * @param pip Instance for selecting command inputs. {@link PluginInputMatcher} is recommended for programmatic use.
     * @param pop Instance for managing command results. {@link PluginOutputMatcher} is recommended for programmatic use.
     * @return The results table. Note that this will also be set as a child of the input table.
     */
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
    
    /**
     * Export a Slide Set data table as a CSV file
     * @param table
     * @param file 
     */
    public void saveCSV(SlideSet table, File file) {
        try {
            csvs.write(table, file, false);
        } catch (IOException e) {
            sslg.println("[SlideSetScript] Failed to write CSV file.");
            throw new IllegalArgumentException(e);
        }
    }
    
    /**
     * Export a Slide Set data table as a CSV file
     * @param table
     * @param fileName 
     */
    public void saveCSV(SlideSet table, String fileName) {
        saveCSV(table, new File(fileName));
    }
    
    /**
     * Get the contents of a Slide Set table column
     * @param table
     * @param index
     * @return 
     */
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
    
    /**
     * Get the contents of a Slide Set table column
     * @param table
     * @param colName
     * @return 
     */
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
