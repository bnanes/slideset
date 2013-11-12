package edu.emory.cellbio.ijbat.pi;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.DefaultPathNotSetException;
import edu.emory.cellbio.ijbat.ex.OperationCanceledException;
import edu.emory.cellbio.ijbat.ui.PluginInputMatcherFrame;
import edu.emory.cellbio.ijbat.ui.PluginOutputMatcherFrame;
import edu.emory.cellbio.ijbat.ui.SlideSetLog;
import edu.emory.cellbio.ijbat.ex.SlideSetException;

import imagej.ImageJ;
import imagej.command.CommandService;
import imagej.command.CommandInfo;
import imagej.module.Module;
import imagej.module.ModuleItem;

import java.lang.reflect.Array;
import java.lang.reflect.TypeVariable;
import java.text.DateFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import org.scijava.Context;

/**
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
          this.plugins = cs.getCommandsOfType(SlideSetPlugin.class);
     }
     
     /*public SlideSetPluginLoader(CommandService cs, DataTypeIDService dts) {
          this.cs = cs;
          this.dtid = dts;
          ij = cs.getContext();
          plugins = cs.getCommandsOfType(SlideSetPlugin.class);
     }*/
     
     // -- Methods --
     
     /**
      * Run a {@code SlideSetPlugin}
      * @param className Class name specifying the command to run
      * @param data A {@code SlideSet} list of data to be made available as inputs for the plugin
      * @return A {@code SlideSet} list of results
      */
     public SlideSet runPlugin(String className, SlideSet data) {
          CommandInfo plugin = cs.getCommand(className);
          if(plugin == null)
               throw new IllegalArgumentException("No command with specified class name: " + className);
          return runPlugin(plugin, data);
     }
     
     /**
      * Run a {@code SlideSetPlugin}
      * @param index This index of the desired plugin
      * @param data A {@code SlideSet} list of data to be made available as inputs for the plugin
      * @return A {@code SlideSet} list of results
      */
     public SlideSet runPlugin(int index, SlideSet data) {
          // Load the plugin
          if(index < 0 || index >= plugins.size())
               throw new IllegalArgumentException("Plugin index out of bounds!");
          CommandInfo plugin = plugins.get(index);
          return runPlugin(plugin, data);
     }
     
     /**
      * Run a {@code SlideSetPlugin}
      * @param plugin {@code CommandInfo} of the plugin to run
      * @param data A {@code SlideSet} list of data to be made available as inputs for the plugin
      * @return A {@code SlideSet} list of results
      */
     public SlideSet runPlugin(CommandInfo plugin, SlideSet data) {
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
          /*if(pDesc != null && !pDesc.trim().isEmpty())
               log.println(pDesc);*/
          log.println(" " + DateFormat.getDateTimeInstance().format(new Date()));
          log.println("----------------");
          
          // Pre-load any requested services so they won't show up in the dialog
          /*log.println("Pre-loading services...");
          ServicePreprocessor sp = new ServicePreprocessor();
          sp.setContext(cs.getContext());
          sp.process(module);*/
          
          // Match SlideSet columns to plugin inputs
          final PluginInputMatcherFrame pmf = new PluginInputMatcherFrame(data, ij, dtid);
          final Iterable<ModuleItem<?>> inputItems = plugin.inputs();
          final ArrayList<String> inputNames = new ArrayList<String>();
          //final ArrayList<String> inputLabels = new ArrayList<String>();
          for(final ModuleItem<?> item : inputItems) {
               String label = item.getLabel();
               String name = item.getName();
               if(label == null || label.equals("")) label = name;
               if(item.getType() == ImageJ.class)
                    module.setInput(name, ij);
               else if(item.getType() == Context.class)
                    module.setInput(name, cs.getContext());
               else if(item.getType() == SlideSetPluginLoader.class)
                    module.setInput(name, this);
               else if(item.getType() == SlideSetLog.class)
                    module.setInput(name, log);
               else if(!module.isResolved(name)) {
                    inputNames.add(name);
                    //inputLabels.add(label);
                    pmf.addInput(item.getType() , label);
               }
          }
          log.println("Awaiting input selections...");
          pmf.showAndWait();
          if(!pmf.wasOKed()) {
               log.println("Canceled by ueser.");
               return null;
          }
          final DataElement[][] inputs;
          try { inputs = pmf.getValues(); }
          catch(OperationCanceledException e) { 
               log.println(e.getMessage());
               return null; 
          }
          
          // Match plugin outputs to result SlideSet columns
          final boolean reduce = MultipleResults.class.isAssignableFrom(plugin.getPluginClass());
          final PluginOutputMatcherFrame pomf 
               = new PluginOutputMatcherFrame(ij, dtid);
          final Iterable<ModuleItem<?>> outputItems = plugin.outputs();
          final ArrayList<String> outputNames = new ArrayList<String>();
          for(final ModuleItem<?> item : outputItems) {
               String label = item.getLabel();
               String name = item.getName();
               if(label == null || label.equals("")) label = name;
               Class<?> type = item.getType();
               if(reduce)
                    type = reduceClass(type);
               pomf.addOutput(type, label);
               outputNames.add(name);
          }
          final ArrayList<String> parentLabels = 
                  new ArrayList<String>(data.getNumCols());
          for(int i=0; i<data.getNumCols(); i++)
               parentLabels.add(data.getColumnName(i));
          pomf.addParentFieldsToResults(parentLabels);
          log.println("Awaiting output settings...");
          pomf.showAndWait();
          final SlideSet result;
          final int[] outputIndex;
          try { 
               result = pomf.getOutputTemplate();
               outputIndex = pomf.getOutputIndex();
          } catch(OperationCanceledException e) { 
               log.println(e.getMessage());
               return null; 
          } 
          
          // Loop through the plugin
          log.println("Setup complete. Beginning processing.");
          for(int i=0; i<inputs.length; i++) {
               log.println("Processing row " + String.valueOf(i+1) + "...");
               for(int j=0; j<inputs[i].length; j++) {
                    try {
                         module.setInput(inputNames.get(j), inputs[i][j].getProcessedUnderlying());
                    } catch(SlideSetException e) { 
                         log.println(e.getMessage());
                         log.println(e.toString());
                         return null; 
                    }
               }
               module.run();
               log.print("...done. Saving results...");
               processResults(module, outputNames, result, 
                    outputIndex, i, reduce);
               log.println(" ok.");
          }
          
          // Copy requested inputs to the results table 
          log.println("Finalizing results table...");
          final List<Integer> inList;
          try { inList = pomf.getIncludedParentFields(); }
          catch(OperationCanceledException e) { return null; }
          if(!inList.isEmpty()) {
               final int offset = result.getNumCols();
               for(final int j : inList)
                    result.addColumn(parentLabels.get(j), 
                         data.getColumnTypeCode(j));
               for(int i=0; i<result.getNumRows(); i++)
                    for(int j=0; j<inList.size(); j++) {
                         final int parRow = (Integer) result.getUnderlying(0,i);
                         result.setUnderlying(j+offset, i, data.getUnderlying(inList.get(j), parRow));
                    }
          }
          
          // Prepare result
          synchronized(result) {
               data.addChild(result);
               result.setParent(data);
               result.setName("Result of " + plugin.getTitle());
               result.setCreationParams(pmf.getInputMap());
               result.setWorkingDirectory(data.getWorkingDirectory());
          }
          log.println("Command excecution complete!");
          return result;
     }
     
     /** Get the number of plugins in the list */
     public int getPluginNumber() {
          return plugins.size();
     }
     
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
     
     private void processResults(final Module m, final List<String> resultNames, 
             final SlideSet results, final int[] outputIndex, final int parentRow, 
             final boolean reduce) {
          Map<String, Object> outputs = m.getOutputs();
          final int rows = reduce ? getNumResults(outputs) : 1;
          try { 
               for(int i=0; i<rows; i++) {
                    final int cRow = results.addRow();
                    results.setProcessedUnderlying(0, cRow, parentRow);
                    for(int j=0; j<outputIndex.length; j++) {
                         if(outputIndex[j] < 0)
                              continue;
                         Object v = outputs.get(resultNames.get(j));
                         if(reduce)
                              v = reduce(v, i);
                         results.setProcessedUnderlying(outputIndex[j], cRow, v);
                    }
               }
          } catch(DefaultPathNotSetException e) { 
               throw new IllegalArgumentException(e);
          } catch(SlideSetException e) {
               throw new IllegalArgumentException(e);
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
      * Set up the column metadata for a {@code SlideSet} based on the results
      * 
      * @param outputs The results
      * @param colNames will be overwritten with a list of column names
      * @param colTypeCodes will be overwritten with a list of {@code typeCode}s
      */
     private void getOutputColumns(Map<String, Object> outputs,
          ArrayList<String> colNames, ArrayList<String> colTypeCodes) {
          colNames.clear();
          colTypeCodes.clear();
          Set<Map.Entry<String, Object>> outputSet = outputs.entrySet();
          colTypeCodes.add("Integer");
          colNames.add("Parent Row");
          for(Map.Entry<String, Object> o : outputSet) {
               colTypeCodes.add(dtid.suggestTypeCode(reduce(o.getValue(), 0)));
               colNames.add(o.getKey());
          }
     }
     
     // -- Tests --
     
     /*public static void main(String... args) {
          imagej.ImageJ ij = new ImageJ();
          DataTypeIDService dtid = new DataTypeIDService(ij);
          edu.emory.cellbio.ijbat.io.XMLService xs = new edu.emory.cellbio.ijbat.io.XMLService(ij, dtid);
          edu.emory.cellbio.ijbat.io.CSVService cs = new edu.emory.cellbio.ijbat.io.CSVService();
          
          SlideSet s = new SlideSet(ij, dtid);
          s.addColumn("Image", "Image2");
          s.addColumn("Text2", "String");
          s.addColumn("Number 1", "Integer");
          s.addColumn("# 2", "Double");
          for(int i=0; i<2; i++) {
               s.addRow();
               s.setUnderlying(0, i, "C:/Users/Ben/Documents/Data/121211 VE-cad DEE mutant is not resistant to K5 - IF/121211-2-1_" + String.valueOf(i+1) + ".tif");
               s.setUnderlying(1, i, "This is row #" + String.valueOf(i));
               s.setUnderlying(2, i, i+1);
          }
          s.setWorkingDirectory("C:/Users/Ben/Pictures");
          new edu.emory.cellbio.ijbat.ui.SlideSetViewer(s,ij,dtid).setVisible(true);
          
          SlideSet r = spl.runPlugin(0, s);
          SlideSet q;
          try{
               xs.write(r, new java.io.File("C:/Users/Ben/Desktop/test.xml"));
               cs.write(r, new java.io.File("C:/Users/Ben/Desktop/test.csv"));
          }
          catch(Throwable t) { throw new IllegalArgumentException("File error"); }
          try{ q = xs.read(new java.io.File("C:/Users/Ben/Desktop/test.xml")); }
          catch(java.io.IOException t) { throw new IllegalArgumentException("File error"); }
          catch(javax.xml.stream.XMLStreamException t) { throw new IllegalArgumentException("File error"); }
          new edu.emory.cellbio.ijbat.ui.SlideSetViewer(r,ij).setVisible(true);
          new edu.emory.cellbio.ijbat.ui.SlideSetViewer(q,ij).setVisible(true);
          
          //System.exit(0);
     }*/
     
}