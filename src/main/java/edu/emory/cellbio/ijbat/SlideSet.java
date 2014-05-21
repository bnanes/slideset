package edu.emory.cellbio.ijbat;

import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.ex.DefaultPathNotSetException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import net.imagej.ImageJ;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import org.scijava.Context;

/**
 * <h1> Slide Set API documentation </h1>
 * 
 * <h2> Introduction </h2> 
 * 
 * Slide Set is a framework for batch processing and analysis
 * of image data using ImageJ. It consists of interconnected
 * components at four levels:
 * <ol>
 * <li> A data model storing analysis inputs and 
 *      results as a hierarchy of tables 
 *      ({@code SlideSet} class);</li>
 * <li> Connections linking the data model to Slide
 *      Set-specific analysis commands and general
 *      ImageJ plugins ({@code dm} package);</li>
 * <li> A set of core Slide Set analysis commands ({@code pi} package); and</li>
 * <li> A graphical user interface wrapper (using Java Swing; {@code ui} package).</li>
 * </ol>
 * The Slide Set core provides a complete workflow for basic image
 * analysis tasks repeated over multiple images and associated data,
 * such as regions of interest and treatment group identifiers. However,
 * Slide Set is also designed to be extensible, and can be leveraged to
 * facilitate the automation and recording of custom image analysis tasks.</p>
 * 
 * <p> Extensions are possible at levels 2 through 4. Slide Set commands
 * are discovered at run-time, and custom commands can be added using a
 * simple {@linkplain edu.emory.cellbio.ijbat.pi.SlideSetPlugin variant}
 * of the ImageJ version 2 Command API. Many general
 * ImageJ plugins developed using the version 2 API can also be used with
 * Slide Set, provided appropriate {@link edu.emory.cellbio.ijbat.dm.read.ElementReader}s
 * and {@link edu.emory.cellbio.ijbat.dm.write.ElementWriter}s are
 * available to link the data types of the plugins' input and output values
 * to the Slide Set data model. If appropriate linkers are not available
 * in the Slide Set core, custom {@code ElementReader}s and
 * {@code ElementWriter}s can be
 * developed, and, as with custom Slide Set commands, loaded at run-time.
 * Finally, the data model and plugins have no dependencies on the user
 * interface, and can be used programmatically or wrapped with different
 * user controls. The set of {@link DataElement}s used in Slide Set tables is
 * currently fixed, but dynamically loaded extensions may be possible
 * in future releases.</p>
 * 
 * <h2> The Slide Set data model </h2>
 * 
 * Slide Set projects organize data into tables, represented by
 * the {@code SlideSet} class. Fields are represented as table columns,
 * and entries are represented as table rows. Commands are run by
 * matching table columns to the command inputs, then repeating the
 * command with the inputs set to each row of values in the table.
 * Command results are then stored in a new table, linked as a "child"
 * to the input table "parent." Columns are therefore typed; each
 * element in the column must represent the same kind of data.</p>
 * <p> {@code SlideSet} table elements are represented by descendants
 * of the {@link DataElement} class. Each element contains "underlying"
 * data, methods for converting the underlying data to and from a 
 * {@code String} (used to save Slide Set table data as XML and CSV files),
 * and an additional parameter representing the data's MIME type.
 * The underlying type and String conversion methods are determined
 * by the {@code DataElement} class, while the MIME type String is
 * independent. Thus, elements representing image files and ROI set
 * files may use the same DataElement class, with an underlying data
 * {@code String} containing the path to the relevant file, but they
 * will have different values for their MIME type {@code String}s.
 * In order for two {@code DataElements} to be of the same overall data type
 * (and therefore column-compatible), they must both be instances
 * of the same {@code DataElement} class and have the same value
 * set for their MIME type.</p>
 * 
 * <p>The underlying data is not passed directly to or from commands.
 * Rather, it is interpreted through {@link edu.emory.cellbio.ijbat.dm.read.ElementReader}s
 * and {@link edu.emory.cellbio.ijbat.dm.write.ElementWriter}s which
 * translate between the data representation in the table
 * (ex.: an image file path {@code String}) and the actual data
 * used by the command (ex.: a {@code Dataset} representing the
 * actual image data). This data form used by commands is called
 * the "processed" type to distinguish it from the {@code DataElement}'s
 * underlying data type. {@code ElementReader}s and {@code ElementWriter}s
 * contain annotations specifying the {@code DataElement} classes and 
 * MIME types with which they are compatible, allowing a relatively 
 * limited number of {@code DataElement} types to wrap many different
 * kinds of information.</p>
 * 
 * <p> In addition to the DataElements, {@code SlideSet}s
 * contain metadata specifying the name, {@code DataElement}
 * class, and MIME type associated with each column, 
 * the "working directory" of the table (used to resolve relative
 * paths; generally the directory where the project XML file is saved),
 * a list of parameters used to create the table (set only for 
 * tables which are created to store the results of commands), 
 * a table name, a reference to the parent table, 
 * and references to any child tables.
 * 
 * <h2>Further information</h2>
 * 
 * <ul>
 * <li> Linking Slide Set tables to commands ({@link DataTypeIDService})</li>
 * <li> Slide Set commands ({@link edu.emory.cellbio.ijbat.pi.SlideSetPlugin})</li>
 * <li> Managing command execution ({@link edu.emory.cellbio.ijbat.pi.SlideSetPluginLoader})</li>
 * </ul>
 * 
 * @author Benjamin Nanes
 */
public class SlideSet {
     
     // -- Fields --
     
     /** Columns in the SlideSet */
     private ArrayList<ArrayList<DataElement>> columns =
          new ArrayList<ArrayList<DataElement>>(16);
     /**
      * Index of column properties: <ul>
      * <li> elementClass - Name of DataElement subclass used
      * <li> mimeType - MIME type used
      * <li> name - Name associated with the column (required)
      * <li> defaultPath - Default relative path for this column (relevant for links only)
      * <li> defaultLinkPrefix - Prefix for generating default links (ex. <em>file</em>-01.xml)
      * <li> defaultLinkCount - Index counter for generating default links (ex. file-<em>01</em>.xml)
      * <li> defaultLinkExtension - Extension for generating default links (ex. file-01.<em>xml</em>)
      * </ul>
      */
     private ArrayList<LinkedHashMap<String, String>> columnProperties =
          new ArrayList<LinkedHashMap<String, String>>(16);
     /** Number of rows in the SlideSet */
     private int numRows = 0;
     
     /** Parent of this {@code SlideSet} */
     private SlideSet parent;
     /** Children of this {@code SlideSet} */
     private ArrayList<SlideSet> children = new ArrayList<SlideSet>();
     /** The input parameters used in the creation of this {@code SlideSet} */
     private LinkedHashMap<String, String> creationParams = new LinkedHashMap<String, String>();
     /** Name of this {@code SlideSet} */
     private String name = "Data";
     /** Working directory of this {@code SlideSet} */
     private String dir;
     
     /** ImageJ context reference */
     private ImageJ ij;
     /** DataTypeIDService reference */
     private DataTypeIDService dtid;
     
     // -- Constructors --
     
     public SlideSet(ImageJ context, DataTypeIDService dtid) {
          ij = context;
          this.dtid = dtid;
     }
     
     // -- Methods --
     
     /** Get the number of columns */
     public int getNumCols() {
          if(columns == null) return 0;
          return columns.size();
     }
     
     /** Get the number of rows */
     public int getNumRows() {
          return numRows;
     }
     
     /** Get a {@link DataElement} */
     public DataElement getDataElement(int column, int row) {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          return columns.get(column).get(row);
     }
     
     /** Get an item as the underlying data object, i.e. for editing in a table */
     public Object getUnderlying(int column, int row) {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          return columns.get(column).get(row).getUnderlying();
     }
     
     /** Get a text representation of an item, i.e. for writing to a file */
     public String getItemText(int column, int row) {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          return columns.get(column).get(row).getUnderlyingText();
     }
     
     /** Get the properties of a column */
     public LinkedHashMap<String, String> getColumnProperties(int index) {
          if(index < 0 || index >= columnProperties.size())
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index);
     }
     
     /** Get the name of a column */
     public String getColumnName(int index) {
          if(index < 0 || index >= columnProperties.size())
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index).get("name");
     }
     
     /** Set the name of a column */
     public void setColumnName(int index, String name) {
          if(index < 0 || index >= columnProperties.size())
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          columnProperties.get(index).put("name", name);
     }
     
     /** Get the column MIME type */
     public String getColumnMimeType(int index) {
          if(index < 0 || index >= columnProperties.size())
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index).get("mimeType");
     }
     
     /** Set the column MIME type */
     public void setColumnMimeType(int index, String type) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          columnProperties.get(index).put("mimeType", type);
          for(DataElement<?> el : columns.get(index))
              el.setMimeType(type);
     }
     
     /**
      * Get a human-readable name for the data type of a column.
      */
     public String getColumnTypeName(int index) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          Class<? extends DataElement> type;
          String mime;
          try {
               type = getColumnElementClass(index);
               mime = getColumnMimeType(index);
          } catch(SlideSetException e) {
              throw new IllegalStateException(e);
          }
          return dtid.getReadableElementType(type, mime);
     }
     
     /**
      * Get the underlying type used to store data in a column.
      */
     public Class<?> getColumnUnderlyingType(int index)
             throws SlideSetException {
          return getNewColumnElement(index).getUnderlying().getClass();
     }
     
     /**
      * Get the {@link DataElement} type used in a column.
      */
     public Class<? extends DataElement> getColumnElementType(int index)
             throws SlideSetException {
         return getColumnElementClass(index);
     }
     
     /** Get the index of the first column with a matching name, or -1 if no such column */
     public int getColumnIndex(String name) {
          int result = -1;
          for(int i=0; i<getNumCols() && result<0; i++) {
               if(getColumnName(i).equals(name))
                    result = i;
          }
          return result;
     }
     
     /**
      * Get the default relative path associated with the column.  May be {@code null}.
      * <p> This property is used for auto-generating new links.  Not relevant for columns
      * in which the data is stored directly (i.e. {@code String}, {@code int}, etc.).
      */
     public String getColumnDefaultPath(int index) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index).get("defaultPath");
     }
     
     /**
      * Set the default relative path associated with the column.  May be {@code null}.
      * <p> This property is used for auto-generating new links.  Not relevant for columns
      * in which the data is stored directly (i.e. {@code String}, {@code int}, etc.).
      */
     public void setColumnDefaultPath(int index, String path) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          columnProperties.get(index).put("defaultPath", path);
     }
     
     /** Change the underlying table-stored value of an item.
      * Can use Object of matching class. */
     public void setUnderlying(int column, int row, Object underlying)
             throws SlideSetException {
          if(!checkBounds(column, row))
               throw new SlideSetException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          if(!checkUnderlyingCompatability(column, underlying))
              throw new SlideSetException("Item is not compatible with column type!");
          columns.get(column).get(row).setUnderlying(underlying);
     }
          
     /**
      * Add an empty column
      * @param properties Column properties
      * @return The column index of the new column
      */
     public int addColumn(LinkedHashMap<String, String> properties)
             throws SlideSetException {
          if( properties.get("name") == null ||
              properties.get("elementClass") == null )
                throw new SlideSetException("Missing column name or element class");
          columnProperties.add(properties);
          int myCol = columnProperties.size() - 1;
          try {
              ArrayList<DataElement> col = new ArrayList<DataElement>(Math.max(2*numRows, 16));
              for(int i=0; i<numRows; i++) {
                  DataElement<?> element = getNewColumnElement(myCol);
                  col.add(element);
              }
              columns.add(col);
              if(!checkColumnLengths()) throw
                  new IndexOutOfBoundsException("Could not balance columns when adding " + name);
          } catch(Exception e) {
              if(columns.size() == columnProperties.size())
                  columns.remove(myCol);
              columnProperties.remove(myCol);
              throw new SlideSetException(e);
          }
          return myCol;
     }
     
     /**
      * Add an empty column with no ({@code null}) MIME type.
      * @param name Label for the column
      * @param elementClass Name of the {@linkplain DataElement}
      *        type to use for this column
      * @return Index of the newly created column
      */
     public int addColumn(String name, String elementClass)
             throws SlideSetException {
          return addColumn(name, elementClass, null);
     }
     
     /**
      * Add an empty column with no ({@code null}) MIME type.
      * @param name Label for the column
      * @param elementClass {@linkplain DataElement}
      *        type to use for this column
      * @return Index of the newly created column
      */
     public int addColumn(String name,
             Class<? extends DataElement> elementClass)
             throws SlideSetException {
          return addColumn(name, elementClass.getName());
     }
     
     /**
      * Add an empty column.
      * @param name Label for the column
      * @param elementClass Name of the {@linkplain DataElement}
      *        type to use for this column
      * @param mimeType MIME type to associate with the column
      * @return Index of the newly created column
      */
     public int addColumn(String name,
             String elementClass,
             String mimeType)
             throws SlideSetException {
          LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
          props.put("name", name);
          props.put("elementClass", elementClass);
          props.put("mimeType", mimeType);
          return addColumn(props);
     }
     
     /**
      * Add an empty column.
      * @param name Label for the column
      * @param elementClass {@linkplain DataElement}
      *        type to use for this column
      * @param mimeType MIME type to associate with the column
      * @return Index of the newly created column
      */
     public int addColumn(String name,
             Class<? extends DataElement> elementClass,
             String mimeType)
             throws SlideSetException {
          return addColumn(name, elementClass.getName(), mimeType);
     }
     
     /**
      * Add a column with data
      * @param properties Column properties
      * @param data An {@code ArrayList} of the {@code DataElement}s in the column
      * @return The column index of the new column
      */
     public int addColumn(
             LinkedHashMap<String, String> properties,
             ArrayList<DataElement<?>> data) throws SlideSetException {
          if(data.size() != getNumRows() && !(getNumRows() == 0 && getNumCols() == 0))
               throw new SlideSetException("Cannot add a column with " +
                    String.valueOf(data.size()) + " elements cannot be added to a table with " +
                    String.valueOf(getNumRows()) + " existing rows and " +
                    String.valueOf(getNumCols()) + " existing columns");
          int newCol = addColumn(properties);
          for(int i = 0; i<data.size(); i++) {
              if(!checkDataElementCompatibility(newCol, data.get(i)))
                  throw new SlideSetException("Provided data does not"
                          + " match column type! (item "
                          + String.valueOf(i) + ")");
              columns.get(newCol).remove(i);
              columns.get(newCol).add(i, data.get(i));
          }
          if(!checkColumnElementConsistency(newCol) || !checkColumnLengths()) {
              columns.remove(newCol);
              throw new SlideSetException("New column is malformed!");
          }
          return newCol;
     }
     
     /** Remove a column <p> Note that columns are re-indexed after a deletion, so if this
         function is called repeatedly, indeces should be given from high to low. */
     public void removeColumn(int index) {
          if(!checkColumnBounds(index))
               throw new IllegalArgumentException("Column index out of bounds");
          columnProperties.remove(index);
          columns.remove(index);
     }
     
     /**
      * Convert the data type stored in a column.
      * If the column contains any {@linkplain DataElement}s,
      * they are converted through {@code String} intermediates.
      * @param index Column to convert
      * @param elementType Target {@link DataElement}
      * @param mimeType Target MIME type
      * @throws SlideSetException If the conversion is aborted, usually
      *         because the target data type is not compatible
      *         with pre-existing data.
      * @see DataElement#getUnderlyingText()
      * @see DataElement#setUnderlyingText(java.lang.String)
      */
     public void convertColumn(
             int index,
             Class<? extends DataElement<?>> elementType,
             String mimeType )
             throws SlideSetException {
         Class<? extends DataElement<?>> from = getColumnElementClass(index);
         final String oldType = columnProperties.get(index).get("elementClass");
         if(!elementType.equals(from)) {
             ArrayList<DataElement> colNew;
             try {
                columnProperties.get(index).put("elementClass", elementType.getName());
                ArrayList<DataElement> colOld = columns.get(index);
                colNew = new ArrayList<DataElement>(numRows + 2);
                for(int r=0; r<numRows; r++) {
                    DataElement<?> el = getNewColumnElement(index);
                    el.setUnderlyingText(colOld.get(r).getUnderlyingText());
                    colNew.add(el);
                }
             } catch(Exception e) {
                 columnProperties.get(index).put("elementClass", oldType);
                 throw new SlideSetException(e);
             }
             synchronized(this) {
                 columns.remove(index);
                 columns.add(index, colNew);
             }
         }
         setColumnMimeType(index, mimeType);
     }
     
     /**
      * Add a row with default values
      * @return The row index of the new row
      */
     public int addRow() throws SlideSetException {
          for(int i=0; i<columns.size(); i++) {
               columns.get(i).add(getNewColumnElement(i));
          }
          numRows++;
          if(!checkColumnLengths()) throw
               new IndexOutOfBoundsException("Could not balance columns when adding row");
          return numRows - 1;
     }
     
     /**
      * Add a row from a {@code List} of {@code DataElement}s.
      * The {@code DataElement} types must match the column types.
      * @return The row index of the new row
      * @see #checkUnderlyingCompatability(int, java.lang.Object) 
      */
     public int addRow(List<DataElement> data) throws SlideSetException {
          if(data.size() != getNumCols())
               throw new IllegalArgumentException("Length of DataElement list does not match number of columns");
          for(int i=0; i<data.size(); i++) {
               DataElement e = data.get(i);
               if(!checkDataElementCompatibility(i, e))
                   throw new SlideSetException("Datum does not match "
                           + "column type (column " + String.valueOf(i)
                           + ").");
          }
          for(int i=0; i<data.size(); i++) {
               DataElement e = data.get(i);
               columns.get(i).add(e);
          }
          numRows++;
          if(!checkColumnLengths()) throw
               new IndexOutOfBoundsException("Could not balance columns when adding row");
          return numRows - 1;
     }
     
     /** Remove a row <p> Note that rows are re-indexed after a deletion, so if this
         function is called repeatedly, indeces should be given from hight to low. */
     public void removeRow(int index) {
          if(!checkRowBounds(index))
               throw new IllegalArgumentException("Row index out of bounds");
          for(ArrayList<DataElement> col : columns)
               col.remove(index);
          numRows--;
          if(!checkColumnLengths())
               throw new IllegalArgumentException("Could not balance columns after row removal!");
     }
     
     /** Get the parent of this {@code SlideSet} */
     public SlideSet getParent() {
          return parent;
     }
     
     /** Register a {@code SlideSet} as the parent of this {@code SlideSet} */
     public void setParent(SlideSet parent) {
          if(parent == null) throw new IllegalArgumentException("Cannot register null parent");
          this.parent = parent;
     }
     
     /** Get a list of this {@code SlideSet}'s children */
     public ArrayList<SlideSet> getChildren() {
          return children;
     }
     
     /** Register a {@code SlideSet} as the child of this {@code SlideSet} */
     public void addChild(SlideSet child) {
          if(child == null) throw new IllegalArgumentException("Cannot register null child");
          children.add(child);
     }
     
     /** Remove a {@code SlideSet} as a child of this {@code SlideSet} */
     public void removeChild(SlideSet child) throws SlideSetException {
         if(child == null)
             throw new IllegalArgumentException("Cannot remove null child");
         if(children.isEmpty())
             throw new SlideSetException("No children registered");
         if(!children.contains(child))
             throw new SlideSetException("Provided object is not registered as a child SlideSet");
         children.remove(child);
     }
     
     /** Get the name of this {@code SlideSet} */
     public String getName() {
          return name;
     }

     /** Get the name of this {@code SlideSet} */
     @Override
     public String toString() {
          return getName();
     }
     
     /** Set the name of this {@code SlideSet} */
     public void setName(String name) {
          this.name = name;
     }
     
     /** Get the creation parameters of this {@code SlideSet} */
     public LinkedHashMap<String, String> getCreationParams() {
          return creationParams;
     }
     
     /** Set the creation parameters of this {@code SlideSet} */
     public void setCreationParams(LinkedHashMap parameters) {
          creationParams = parameters;
     }
     
     /** Get the working directory of this {@code SlideSet} */
     public String getWorkingDirectory() {
          return dir;
     }
     
     /** Set the working directory of this {@code SlideSet} */
     public void setWorkingDirectory(String path) {
          dir = path;
     }
     
     /** Get the SciJava application {@code Context} */
     public Context getContext() {
          return ij.getContext();
     }
     
     /**
      * Get the prefix for default links for a column. {@code null} if unset.
      * <p> ex. <em>file</em>-01.xml
      */
     public String getDefaultLinkPrefix(int column) {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          return columnProperties.get(column).get("defaultLinkPrefix");
     }
     
     /**
      * Set the prefix for default links for a column.
      * <p> ex. <em>file</em>-01.xml
      */
     public void setDefaultLinkPrefix(int column, String prefix) {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          columnProperties.get(column).put("defaultLinkPrefix", prefix);
     }
     
     /**
      * Get the counter for default links for a column. {@code null} if unset.
      * <p> ex. file-<em>01</em>.xml
      */
     public int getDefaultLinkCount(int column) {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          String x = columnProperties.get(column).get("defaultLinkCount");
          x = x == null ? "0" : x;
          return Integer.valueOf(x);
     }
     
     /**
      * Set the counter for default links for a column.
      * <p> ex. file-<em>01</em>.xml
      */
     public void setDefaultLinkCount(int column, int counter) {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          columnProperties.get(column).
                  put("defaultLinkCount", String.valueOf(counter));
     }
     
     /**
      * Get the default link extension for a column. {@code null} if unset.
      * <p> ex. file-01.<em>xml</em>
      */
     public String getDefaultLinkExtension(int column) {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          return columnProperties.get(column).get("defaultLinkExtension");
     }
     
     /**
      * Set the default link extension for a column.
      * <p> ex. file-01.<em>xml</em>
      */
     public void setDefaultLinkExtension(int column, String extension) {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          columnProperties.get(column).put("defaultLinkExtension", extension);
     }  
     
     /** Resolve a possibly abstract path using this
      *  {@code SlideSet}'s working directory. */
     public String resolvePath(String path) {
          File f = new File(path);
          if(f.isAbsolute())
               return path;
          return getWorkingDirectory() + File.separator + path;
     }
     
     /**
      * Set a cell to an auto-generated link
      * @param column
      * @param row
      * @throws DefaultPathNotSetException 
      */
     public void makeDefaultLink(int column, int row)
             throws SlideSetException {
          if(!checkColumnBounds(column))
               throw new SlideSetException("Column index out of bounds");
          setUnderlying(column, row, generateDefaultLinkPath(column, row));
     }
     
     // -- Helper methods --
     
     /** Check validity of a column index */
     private boolean checkColumnBounds(int index) {
          if(columns == null) return false;
          return index < columns.size();
     }
     
     /** Check validity of a row index */
     private boolean checkRowBounds(int index) {
          return index < numRows;
     }
     
     /** Check validity of an index pair */
     private boolean checkBounds(int column, int row) {
          return checkColumnBounds(column) && checkRowBounds(row);
     }
     
     /** Check to make sure each column is of equal length */
     private boolean checkColumnLengths() {
          if(columns == null) return numRows == 0 ? true : false;
          for(int i=0; i<columns.size(); i++)
               if(columns.get(i).size() != numRows) return false;
          return true;
     }
          
     /** Generate a default link path for a specified cell. */
     private String generateDefaultLinkPath(int column, int row)
             throws DefaultPathNotSetException {
          if(!checkRowBounds(row))
               throw new IllegalArgumentException("Row index out of bounds");
          String a = getColumnDefaultPath(column);
          String b = getDefaultLinkPrefix(column);
          int c = getDefaultLinkCount(column);
          String d = getDefaultLinkExtension(column);
          if(a == null)
               throw new DefaultPathNotSetException(
                       "No default link path for column " + String.valueOf(column));
          b = b == null ? getColumnName(column) : b;
          d = d == null ? "" : "." + d;
          String path = null;
          do {
               path = a + File.separator + b + "-" + String.format("%1$03d", c) + d;
               c++;
          } while( new File(path).exists() ); // Keep counting until we find a file name the doesn't exist.
          setDefaultLinkCount(column, c);
          return path;
     }
     
     private Class<? extends DataElement<?>> getColumnElementClass(int index) throws SlideSetException {
          if(index >= columnProperties.size() || index < 0)
               throw new SlideSetException("Invalid column: " + String.valueOf(index));
          String cn = columnProperties.get(index).get("elementClass");
          try {
              if(cn == null)
                  throw new SlideSetException("Element class not saved in table!");
              Class c = Class.forName(cn);
              if(!(DataElement.class.isAssignableFrom(c)))
                  throw new SlideSetException("Class is not a DataElement: " + c.getName());
              return c;
          } catch(ClassNotFoundException e) {
              throw new SlideSetException(e);
          }
     }
     
     /** Generate a new {@link DataElement} appropriate
      *  for the selected column. The element is initialized
      *  with the type's default value. */
     private DataElement<?> getNewColumnElement(int index)
             throws SlideSetException {
          Class<? extends DataElement<?>> c = getColumnElementClass(index);
          try {
              DataElement<?> el = c.newInstance();
              el.setMimeType(getColumnMimeType(index));
              el.setOwner(this);
              return el;
          } catch(Exception e) {
              throw new SlideSetException(e);
          }
     }
     
     /**
      * Check if an object may be stored in a column directly
      * using a {@code setUnderlying()} method (i.e. without
      * using an {@link edu.emory.cellbio.ijbat.dm.write.ElementWriter}).
      */
     private boolean checkUnderlyingCompatability(int column, Object underlying)
             throws SlideSetException {
          Class stored;
          if(numRows > 0) {
              stored = getUnderlying(column, numRows-1).getClass();
          } else {
              stored = getNewColumnElement(column).getUnderlying().getClass();
          }
          return stored.isInstance(underlying);
     }
     
     /**
      * Check if the class and MIME type of a {@link DataElement}
      * match those of a column in this table.
      */
     private boolean checkDataElementCompatibility(
             int column, DataElement<?> element) throws SlideSetException {
          Class stored = getColumnElementClass(column);
          boolean ok = stored.isInstance(element)
                  && element.getClass().isAssignableFrom(stored);
          if(ok)
              ok = element.getMimeType().equals(getColumnMimeType(column));
          return ok;
     }
     
     /**
      * Check if all the {@link DataElement}s in this column
      * are of the same type and have the same MIME type set.
      */
     private boolean checkColumnElementConsistency(int column) throws SlideSetException {
          boolean ok = true;
          for(int i=0; i<numRows; i++) {
              ok = checkDataElementCompatibility(column, getDataElement(column, i));
              if(!ok)
                  break;
          }
          return ok;
     }
     
}
