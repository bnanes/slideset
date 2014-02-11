package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.ColumnBoundReader;
import edu.emory.cellbio.ijbat.dm.ColumnBoundWriter;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.dm.read.ElementReader;
import edu.emory.cellbio.ijbat.dm.write.ElementWriter;
import edu.emory.cellbio.ijbat.dm.FileLink;
import edu.emory.cellbio.ijbat.ex.NoPluginInputSourceException;
import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

import imagej.ImageJ;
import imagej.command.Command;
import imagej.command.CommandService;
import imagej.command.CommandInfo;
import imagej.module.Module;
import imagej.module.ModuleItem;

import imagej.module.process.ServicePreprocessor;
import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.text.DateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import org.scijava.Context;
import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.plugin.Plugin;

/**
 * <h2> Managing command execution </h2>
 * 
 * {@code SlideSetPluginLoader} maintains an index of available
 * {@link SlideSetPlugin}s and manages command execution. 
 * The same methods are used to run both <code>SlideSetPlugins</code>
 * and general ImageJ plugins; the few differences between them 
 * are described below. A single instance is shared by other 
 * Slide Set components and can be used repeatedly to run different 
 * commands. Command execution proceeds in four phases:
 * 
 * <h3> Preparation of command inputs </h3>
 * 
 * SlideSetPluginLoader first attempts to determine if any of 
 * the input parameters declared by the command should are requests 
 * for ImageJ services (ex: {@code Context}, {@code ImageJ},
 * {@code DatasetService}) or Slide Set components (ex:
 * {@code SlideSetLog}, {@code DataTypeIDService}) that should be 
 * filled without consulting the user. For the remaining inputs, 
 * two lists are requested from {@link DataTypeIDService}:
 * <ol>
 * <li> {@link ColumnBoundReader}s that can produce a cast-compatible 
 * type from the input SlideSet table; and
 * <li> {@link ElementReader} types that can produce a cast-compatible 
 * type from a constant value.
 * </ol>
 * If both lists are empty for any input parameter, plugin
 * execution is aborted. An instance of {@link PluginInputPicker}
 * is then used to select the readers or constant values to use. 
 * In Slide Set core, {@code PluginInputPicker} is implemented by 
 * {@link edu.emory.cellbio.ijbat.ui.PluginInputMatcherFrame}, 
 * which prompts the user to select the desired inputs, although 
 * custom implementations are also possible, including schemes for 
 * automated selection. If a constant value is specified for an 
 * input parameter, it is used to instantiate a 
 * "constant" {@code ColumnBoundReader}.
 * 
 * <h3> Preparation for command results </h3>
 * 
 * Next, SlideSetPluginLoader determines how plugin output 
 * values should be recorded and prepares a {@code SlideSet}
 * table in which to save them. For each output, a list of
 * {@link ElementWriter}s with "processed" types cast-compatible 
 * with the output type is requested from {@code DataTypeIDService}.
 * These lists are passed to an instance of {@link PluginOutputPicker}
 * to determine which {@code ElementWriter} should be used,
 * or if the output value should be discarded. In Slide Set core,
 * {@code PluginOutputPicker} is implemented by 
 * {@link edu.emory.cellbio.ijbat.ui.PluginOutputMatcherFrame},
 * which uses a dialog box to prompt the user for choices,
 * although, as with {@code PluginInputPicker}, custom 
 * implementations are possible, including schemes for automated selection.
 * {@code PluginOutputPicker} also provides selections for columns
 * from the input table that should be copied into the output table.
 * 
 * <p> Given the selected {@code ElementWriter}s,
 * {@code SlideSetPluginLoader} than requests a {@code SlideSet}
 * table from {@code DataTypeIDService} containing columns using 
 * {@code DataElement} classes and MIME types matching those writers.
 * The writers are then wrapped in {@link ColumnBoundWriter} instances 
 * which can be used to write data without interacting with the 
 * table directly. Columns for the data selected for copying from 
 * the input table are appended to the results table.
 * 
 * <h3> Command execution loop </h3>
 * 
 * For each row in the input table, plugin input values are
 * read using the selected {@code ColumnBoundReader}s, and the
 * plugin's {@code run()} method is invoked. Once the method returns, the
 * plugin output parameters are recorded by the selected 
 * {@code ColumnBoundWriter}s. Each plugin run occurs in sequence 
 * using one instance of the plugin class on a single thread, 
 * though multithreading may be implemented in future versions. 
 * An important consequence of this is that plugin output parameter 
 * values from the previous run are retained when the subsequent 
 * run begins. Therefore, initialization of output parameters should
 * occur at the beginning of the {@code run()} method, rather than 
 * in the constructor.
 * 
 * <h3> Finalization of results table </h3>
 * 
 * Once command execution is complete, the results table is 
 * registered as a child of the input table and the input table 
 * is registered as the parent of the results table. Additionally,
 * a name for the results table is stored in the table metadata,
 * along with a list of the input parameters used to run the plugin.
 * 
 * 
 * @author Benjamin Nanes
 */
public class SlideSetPluginLoader {
     
     // -- Fileds --
     
     private final DataTypeIDService dtid;
     private final SlideSetLog log;
     private final ImageJ ij;
     private final CommandService cs;
     List<CommandInfo> plugins;
     
     // -- Constructor --
     
     public SlideSetPluginLoader(ImageJ context, 
             DataTypeIDService dtid, SlideSetLog log) {
          if(context == null || dtid == null || log == null)
               throw new IllegalStateException("Cannot initialize "
                       + "plugin loader without other SlideSet components");
          this.ij = context;
          this.cs = ij.get(CommandService.class);
          this.dtid = dtid;
          this.log = log;
          this.plugins = loadPlugins();
     }
     
     // -- Methods --
     
     /**
      * Run a {@code SlideSetPlugin}
      * @param className Class name specifying the command to run
      * @param data A {@code SlideSet} list of data to be made available as inputs for the plugin
      * @return A {@code SlideSet} list of results
      */
     public SlideSet runPlugin(
             String className,
             SlideSet data,
             PluginInputPicker pip,
             PluginOutputPicker pop)
             throws SlideSetException{
          CommandInfo plugin = cs.getCommand(className);
          if(plugin == null) { // Because CommandService doesn't play nice with Fiji, we'll search our own index too.
              for(CommandInfo ci : plugins)
                  if(ci.getDelegateClassName().equals(className)) {
                      plugin = ci;
                      break;
                  }
          }
          if(plugin == null)
               throw new IllegalArgumentException("No command with specified class name: " + className);
          return runPlugin(plugin, data, pip, pop);
     }
     
     /**
      * Run a {@code SlideSetPlugin}
      * @param plugin {@code CommandInfo} of the plugin to run
      * @param data A {@code SlideSet} list of data to be made available as inputs for the plugin
      * @return A {@code SlideSet} list of results
      */
     public SlideSet runPlugin(
             CommandInfo plugin,
             SlideSet data,
             PluginInputPicker pip,
             PluginOutputPicker pop)
             throws SlideSetException {
          // Load the plugin
          final Module module;
          try { module = plugin.createModule(); }
          catch(Throwable t) { throw new IllegalArgumentException(t); }
          final String  pDesc = plugin.getDescription();
          log.println("\n----------------");
          log.println("Running command: ");
          log.println(plugin.getTitle());
          log.print(" on input table: \n ");
          log.println(data.getName());
          final Date timeStart = new Date();
          log.println(" " + DateFormat.getDateTimeInstance().format(timeStart));
          log.println("----------------");
          
          // Pre-load any requested services so they won't show up in the dialog
          final Iterable<ModuleItem<?>> inputItems = plugin.inputs();
          fillServices(module, inputItems);
          
          // Match SlideSet columns to plugin inputs
          ArrayList<ModuleItem<?>> readInputs
                  = getUnfilledInputs(module, inputItems);
          ArrayList<ColumnBoundReader> readers
                  = getReaders(readInputs, data, pip);
          LinkedHashMap<String, String> creationParams
                  = new LinkedHashMap<String, String>();
          creationParams.put("Command run", plugin.getTitle());
          creationParams.put("Run on", DateFormat.getDateTimeInstance().format(timeStart));
          for(int i = 0; i < readInputs.size(); i++) {
              String key = readInputs.get(i).getLabel();
              if(key == null || key.isEmpty())
                  key = readInputs.get(i).getName();
              creationParams.put(key, readers.get(i).getColumnName());
          }
          
          // Match plugin outputs to result SlideSet columns
          final boolean reduce
                  = MultipleResults.class.isAssignableFrom(plugin.getPluginClass());
          ArrayList<ModuleItem<?>> outputItems = new ArrayList<ModuleItem<?>>();
          for(ModuleItem<?> i : plugin.outputs())
              outputItems.add(i);
          ArrayList<ColumnBoundWriter> writers
                  = new ArrayList<ColumnBoundWriter>();
          ArrayList<Integer> parentFields = new ArrayList<Integer>();
          ArrayList<String> linkDir = new ArrayList<String>();
          ArrayList<String> linkPre = new ArrayList<String>();
          ArrayList<String> linkExt = new ArrayList<String>();
          final String[] parentLabels = new String[data.getNumCols()];
          SlideSet resultsTable = new SlideSet(ij, dtid);
          resultsTable.setWorkingDirectory(data.getWorkingDirectory());
          for(int i = 0; i < parentLabels.length; i++)
              parentLabels[i] = data.getColumnName(i);
          getWriters(outputItems, reduce, pop, writers,
                  resultsTable, parentLabels, parentFields, linkDir, linkPre, linkExt);
          addColumnsForParentFields(parentFields, data, resultsTable);
          setupFileLinkColumns(resultsTable, linkDir, linkPre, linkExt);
          
          // Loop through the plugin
          log.println("Setup complete. Beginning processing.");
          for(int i=0; i<data.getNumRows(); i++) {
               log.println("Processing row " + String.valueOf(i+1) + "...");
               for(int j=0; j<readInputs.size(); j++) {
                    try {
                         module.setInput(
                               readInputs.get(j).getName(),
                               readers.get(j).read(i));
                    } catch(SlideSetException e) { 
                         log.println("~~~~~~~~~~~~");
                         log.println("Fatal error:");
                         log.println(e.getMessage());
                         log.println("~~~~~~~~~~~~");
                         e.printStackTrace(System.out);
                         return null; 
                    }
               }
               module.run();
               log.print("...done. Saving results...");
               saveResults(module, outputItems, reduce, writers,
                       parentFields, resultsTable, data, i);
               log.println(" ok.");
          }
          
          // Prepare result
          final long runTime;
          synchronized(resultsTable) {
               data.addChild(resultsTable);
               resultsTable.setParent(data);
               resultsTable.setName("Result of " + plugin.getTitle());
               runTime = new Date().getTime() - timeStart.getTime();
               creationParams.put("Run time", String.valueOf(runTime/1000) + "s");
               resultsTable.setCreationParams(creationParams);
          }
          log.println("Command excecution complete!");
          log.println("(Run time: " + String.valueOf(runTime/1000) + "s)");
          return resultsTable;
     }
     
     /** Get a list of available {@link SlideSetPlugin}s */
     public List<CommandInfo> getPluginInfo() {
          return plugins;
     }
     
     /** Get a list of the plugin paths, i.e. for a menu */
     public ArrayList<String> getPluginMenuPaths() {
          ArrayList<String> paths = new ArrayList<String>(plugins.size());
          for(CommandInfo plugin : plugins) {
               String path = plugin.getAnnotation().menuPath();
               if(path == null || path.equals(""))
                    path = plugin.getAnnotation().label();
               if(path == null || path.equals(""))
                    path = plugin.getAnnotation().name();
               if(path == null || path.equals(""))
                    path = plugin.getPluginClass().getSimpleName();
               paths.add(path);
          }
          return paths;
     }
     
     /** Redo the search for plugins */
     public void refreshList() {
          plugins = cs.getCommandsOfType(SlideSetPlugin.class);
     }
     
     // -- Helper methods --
     
     /** Prefill service inputs */
     private void fillServices(Module module, Iterable<ModuleItem<?>> inputs)
             throws SlideSetException {
         log.println("Pre-loading services...");  // For some reason this doesn't work in 7.5
         ServicePreprocessor sp;
         try {
             try {
                module.initialize();
             } catch(Exception ex) {}
             sp = (ServicePreprocessor) ij.plugin()
                    .getPlugin(ServicePreprocessor.class).createInstance();
             ij.getContext().inject(sp);
             try {
                ij.getContext().inject(module);
             } catch(IllegalStateException ex) {}
             try {
                ij.getContext().inject(module.getDelegateObject());
             } catch(IllegalStateException ex) {}
             sp.process(module);
         } catch(Exception e) {
             throw new SlideSetException("Unable to load services.", e);
         }
         for(ModuleItem<?> item : inputs) {
             String name = item.getName();
             if(!module.isResolved(name)) {
                 if(item.getType() == ImageJ.class) {
                     module.setInput(name, ij);
                     module.setResolved(name, true);
                 }
                 else if(item.getType() == Context.class) {
                     module.setInput(name, cs.getContext());
                     module.setResolved(name, true);
                 }
                 else if(item.getType() == SlideSetPluginLoader.class) {
                     module.setInput(name, this);
                     module.setResolved(name, true);
                 }
                 else if(item.getType() == SlideSetLog.class) {
                     module.setInput(name, log);
                     module.setResolved(name, true);
                 }
             }
         }
     }
     
     /** Get a list of inputs that have not been filled */
     private ArrayList<ModuleItem<?>> getUnfilledInputs(
             Module module,
             Iterable<ModuleItem<?>> inputs) {
         ArrayList<ModuleItem<?>> val = new ArrayList<ModuleItem<?>>();
         for(ModuleItem<?> i : inputs) {
             if(!module.isResolved(i.getName()))
                 val.add(i);
         }
         return val;
     }
     
     /**
      * Create a list of {@link ColumnBoundReader}s for each input
      * 
      * @param inputs The list if inputs which need readers
      * @param data The table from which inputs should be drawn
      * @param pip UI module to allow user selection of inputs
      * @see PluginInputPicker
      */
     private ArrayList<ColumnBoundReader> getReaders(
             Iterable<ModuleItem<?>> inputs,
             SlideSet data,
             PluginInputPicker pip)
             throws SlideSetException {
         ArrayList<ColumnBoundReader> boundReaders
                 = new ArrayList<ColumnBoundReader>();
         ArrayList<ArrayList<String>> choices
                 = new ArrayList<ArrayList<String>>();
         ArrayList<ArrayList<ColumnBoundReader>> goodColumnReaders
                 = new ArrayList<ArrayList<ColumnBoundReader>>();
         ArrayList<ArrayList<Class<? extends ElementReader>>> goodConstantReaders
                 = new ArrayList<ArrayList<Class<? extends ElementReader>>>();
         ArrayList<ArrayList<Object>> constantRequest
                 = new ArrayList<ArrayList<Object>>();
         ArrayList<Integer> readerChoices = new ArrayList<Integer>();
         ArrayList<Object> constantChoices = new ArrayList<Object>();
         int pos = 0;
         for(ModuleItem<?> input : inputs) {
             String name = input.getName();
             String label = input.getLabel();
             if(label == null || label.equals("")) label = name;
             choices.add(new ArrayList<String>());
             constantRequest.add(new ArrayList<Object>());
             goodColumnReaders.add(dtid.getCompatableColumnReaders(
                     input.getType(), data));
             for(ColumnBoundReader r : goodColumnReaders.get(pos)) {
                 choices.get(pos).add(r.getColumnName()
                         + " (" + r.getColumnTypeName() + ")");
                 constantRequest.get(pos).add(null);
             }
             goodConstantReaders.add(new ArrayList<Class<? extends ElementReader>>());
             ArrayList<String> cNames = new ArrayList<String>();
             dtid.getCompatableReaders(input.getType(),
                     goodConstantReaders.get(pos), cNames, true);
             for(int i = 0; i < goodConstantReaders.get(pos).size(); i++) {
                 Class<? extends ElementReader> r = goodConstantReaders.get(pos).get(i);
                 try {
                     constantRequest.get(pos).add(
                             dtid.getReaderElementType(r)
                             .newInstance().getUnderlying());
                 } catch(Exception e) {
                     continue;
                 }
                 choices.get(pos).add(cNames.get(i));
             }
             if(choices.get(pos).isEmpty())
                 throw new NoPluginInputSourceException(
                         "Input of type " + input.getType().getName()
                         + " cannot be filled with available readers!");
             pip.addInput(label,
                     choices.get(pos).toArray(new String[0]),
                     constantRequest.get(pos).toArray());
             pos++;
         }
         log.println("Awaiting input selections...");
         pip.getInputChoices(readerChoices, constantChoices);
         for(int i=0; i<pos; i++) {
             ArrayList<ColumnBoundReader> cbr = goodColumnReaders.get(i);
             ArrayList<Class<? extends ElementReader>> er
                     = goodConstantReaders.get(i);
             int cbrs = cbr.size();
             int choice = readerChoices.get(i);
             if(choice < cbrs) {
                 boundReaders.add(cbr.get(choice));
                 continue;
             }
             choice = choice - cbrs;
             boundReaders.add(dtid.makeColumnBoundConstantReader(
                     er.get(choice), constantChoices.get(i), data));
         }
         return boundReaders;
     }
     
     /**
      * Prepare for plugin results be creating a results table
      * and appropriate {@link ColumnBoundWriter}s to write the
      * results to it
      * 
      * @param outputs List of plugin outputs that might need
      *    to be written to the output table. If the user
      *    chooses to discard an output, it will be removed
      *    from the list.
      * @param reduce Should array outputs be split into
      *    their individual elements?
      * @param pop UI module to allow user specification of
      *    output handling.
      * @param writers List to be filled with {@code ColumnBoundWriters}
      *    for the results that will be saved
      * @param resultsTable Empty {@code SlideSet} table that will be
      *    configured to store the results
      * @param parentLabels List of labels for the fields from the input table
      *    that the user may choose to copy to the output table
      * @param parentFields List to be filled with the users
      *    choices for which fields from the input table to be
      *    copied to the results table
      * @param linkDir List to be filled with default link path
      *    to use, for results that use {@link edu.emory.cellbio.ijbat.dm.FileLinkElement}s
      * @param linkPre List to be filled with file name root to use,
      *    for results that use {@code FileLinkElement}s
      * @param linkExt List to be filled with file extensions
      *    to use, for results that use {@code FileLinkElement}s
      * @see PluginOutputPicker
      */
     private void getWriters (
             ArrayList<ModuleItem<?>> outputs,
             boolean reduce,
             PluginOutputPicker pop,
             ArrayList<ColumnBoundWriter> writers,
             SlideSet resultsTable,
             String[] parentLabels,
             ArrayList<Integer> parentFields,
             ArrayList<String> linkDir,
             ArrayList<String> linkPre,
             ArrayList<String> linkExt)
             throws SlideSetException {
         ArrayList<ArrayList<Class<? extends ElementWriter>>> goodWriters
                 = new ArrayList<ArrayList<Class<? extends ElementWriter>>>();
         for(ModuleItem<?> output : outputs) {
             String label = output.getLabel();
             String name = output.getName();
             if(label == null || label.isEmpty()) label = name;
             ArrayList<Class<? extends ElementWriter>> er
                     = new ArrayList<Class<? extends ElementWriter>>();
             ArrayList<String> ern = new ArrayList<String>();
             ArrayList<String> ext = new ArrayList<String>();
             Class<?> type = reduce ? reduceClass(output.getType()) : output.getType();
             dtid.getCompatableWriters(type, er, ern, ext);
             goodWriters.add(er);
             boolean[] fileLink = new boolean[er.size() + 1];
             for(int i=0; i<er.size(); i++)
                 fileLink[i] = FileLink.class.isAssignableFrom(
                         dtid.getWriterElementType(er.get(i)));
             ern.add("Discard");
             fileLink[fileLink.length - 1] = false;
             pop.addOutput(label, ern.toArray(new String[0]),
                     fileLink, new String[fileLink.length],
                     new String[fileLink.length],
                     ext.toArray(new String[fileLink.length]));
         }
         pop.setParentFieldLabels(parentLabels);
         ArrayList<Integer> choices = new ArrayList<Integer>();
         ArrayList<Class<? extends ElementWriter>> chosen
                 = new ArrayList<Class<? extends ElementWriter>>();
         log.println("Awaiting output settings...");
         parentFields.clear();
         linkDir.clear();
         linkPre.clear();
         linkExt.clear();
         pop.getOutputChoices(choices, parentFields, linkDir, linkPre, linkExt);
         if(outputs.size() != choices.size())
             throw new SlideSetException(
                     "Different number of output parameters and selected writers!");
         int rm = 0;
         for(int i=0; i<choices.size(); i++) {
             int x = choices.get(i);
             if(x < 0 || x >= goodWriters.get(i).size()) {
                 int k = i - rm;
                 outputs.remove(k);
                 linkDir.remove(k);
                 linkPre.remove(k);
                 linkExt.remove(k);
                 rm++;
             }
             else
                 chosen.add(goodWriters.get(i).get(x));
         }
         ArrayList<String> columnNames = new ArrayList<String>();
         for(ModuleItem<?> i : outputs) {
             String label = i.getLabel();
             String name = i.getName();
             if(label == null || label.isEmpty()) label = name;
             columnNames.add(label);
         }
         dtid.getTableForWriters(columnNames, chosen, resultsTable, writers);
     }
     
     /**
      * Add columns to the results table for included fields from
      * the input table
      * @param includedFields List if indeces of fields to include
      * @param parent Input table
      * @param results Results table that will be modified
      */
     private void addColumnsForParentFields (
             ArrayList<Integer> includedFields,
             SlideSet parent,
             SlideSet results)
             throws SlideSetException {
         for(Integer i : includedFields) {
             LinkedHashMap<String, String> newProps
                     = new LinkedHashMap<String, String>();
             newProps.put("name", parent.getColumnName(i));
             newProps.put("mimeType", parent.getColumnMimeType(i));
             newProps.put("elementClass", parent.getColumnProperties(i)
                     .get("elementClass"));
             results.addColumn(newProps);
         }
     }
     
     /** Generate default path links as needed */
     private void setupFileLinkColumns(
             SlideSet table,
             ArrayList<String> linkDir,
             ArrayList<String> linkPre,
             ArrayList<String> linkExt)
             throws SlideSetException {
         int nc = table.getNumCols();
         if(  linkDir.size() != linkPre.size()
           || linkPre.size() != linkExt.size()
           || nc < linkExt.size())
             throw new SlideSetException(
                     "Wrong number of auto path generation parameters!");
         for(int i = 0; i < linkDir.size(); i++) {
             table.setColumnDefaultPath(i, linkDir.get(i));
             table.setDefaultLinkPrefix(i, linkPre.get(i));
             table.setDefaultLinkExtension(i, linkExt.get(i));
             table.setDefaultLinkCount(i, 0);
         }
     }
     
     /**
      * Save results from a command execution
      */
     private void saveResults(
             Module m,
             ArrayList<ModuleItem<?>> outputs,
             boolean reduce,
             ArrayList<ColumnBoundWriter> writers,
             ArrayList<Integer> parentFields,
             SlideSet resultsTable,
             SlideSet parentTable,
             int parentRow)
             throws SlideSetException {
        if(outputs.size() != writers.size())
            throw new SlideSetException("Different number of outputs and writers!");
        if(resultsTable.getNumCols() != writers.size() + parentFields.size())
            throw new SlideSetException("Results table is the wrong size!");
        Map<String, Object> oMap = m.getOutputs();
        int numres = reduce ? getNumResults(oMap) : 1;
        for(int i = 0; i < numres; i++) {
            int r = resultsTable.addRow();
            int offset = outputs.size();
            for(int c = 0; c < offset; c++) {
                ColumnBoundWriter w = writers.get(c);
                if(FileLink.class.isAssignableFrom(
                        resultsTable.getColumnElementType(w.getColumnNum())))
                    resultsTable.makeDefaultLink(w.getColumnNum(), r);
                Object data = m.getOutput(outputs.get(c).getName());
                if(reduce)
                    data = reduce(data, i);
                w.write(data, r);
            }
            for(int c = offset; c < resultsTable.getNumCols(); c++) {
                resultsTable.setUnderlying(c, r,
                        parentTable.getUnderlying(
                        parentFields.get(c - offset), parentRow));
            }
        }
    }
     
     /**
      * Checks to make sure that all results are either length 1
      * (i.e. not arrays or {@code List}s, or arrays or
      * {@code List}s of length 1) or length <em>N</em>, where <em>N</em> is
      * some constant value.  For example, three results with lengths
      * {1, 5, 5} or {3, 3, 3} are acceptable, but results with
      * lengths {3, 5, 5} are not.
      * 
      * @return <em>N</em>, the length of each result that does not have
      * length 1
      */
     private int getNumResults(Map<String, Object> outputs) {
          Collection<Object> vals = outputs.values();
          int num = 1;
          int vNum;
          for(Object o : vals) {
               vNum = 1;
               if(o.getClass().isArray())
                    vNum = Array.getLength(o);
               else if(List.class.isAssignableFrom(o.getClass()))
                    vNum = ((List)o).size();
               else if(Iterable.class.isAssignableFrom(o.getClass()))
                    throw new IllegalArgumentException(
                         "Cannot handle result that is Iterable but not List -- needs to be ordered");
               if(num == 1) num = vNum;
               else if(vNum != 1 && vNum != num)
                    throw new IllegalArgumentException(
                         "Cannot handle results of unequal length (except length 1)");
          }
          return num;
     }
     
     /**
      * Get element {@code result} from an output {@code Object}
      * which may or may not be an array or a {@code List}.
      * If the output is not an array or a {@code List}, or
      * is shorter than the requested index, returns
      * the first (or only) element.
      */
     private Object reduce(Object output, int index) {
          if(output.getClass().isArray()){
               if(Array.getLength(output) <= index)
                    return Array.get(output, 0);
               return Array.get(output, index);
          }
          if(List.class.isAssignableFrom(output.getClass())) {
               if(((List)output).size() <= index)
                    return ((List)output).get(0);
               return ((List)output).get(index);
          }
          return output;
     }
     
     /**
      * If a class is an array or a {@link List}, returns the class of the
      * array or list element.
      */
     private Class<?> reduceClass(Class<?> c) {
          if(c.isArray())
               return c.getComponentType();
          if(List.class.isAssignableFrom(c)) {
               TypeVariable<?>[] ts = c.getTypeParameters();
               if(ts == null || ts.length < 1)
                    throw new IllegalArgumentException("Can't get List type for " + c.getName());
               return (Class<?>) ts[1].getGenericDeclaration();
          } 
          return c;
     }
     
     /**
      * Because the {@code CommandService} is persnickety
      * when launched through Fiji, we'll do this manually.
      */
     private List<CommandInfo> loadPlugins() {
         ArrayList<CommandInfo> ci = new ArrayList<CommandInfo>();
         for(IndexItem<Plugin> p : Index.load(Plugin.class, getClass().getClassLoader())) {
             if(p.annotation().type() == SlideSetPlugin.class)
                 try {
                     ci.add(new CommandInfo(
                             (Class<? extends Command>) Class.forName(
                             p.className(), true,
                             getClass().getClassLoader()), p.annotation()));
                 } catch(Exception e) {
                     throw new IllegalArgumentException(
                             "Unable to load SlideSet command: ", e);
                 }
         }
         return ci;
     }
     
}
