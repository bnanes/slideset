package edu.emory.cellbio.ijbat;

import edu.emory.cellbio.ijbat.dm.DataElement;
import edu.emory.cellbio.ijbat.dm.DataTypeIDService;
import edu.emory.cellbio.ijbat.dm.LinkLinker;
import edu.emory.cellbio.ijbat.dm.Linker;
import edu.emory.cellbio.ijbat.ex.DefaultPathNotSetException;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import imagej.ImageJ;
import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Core data set index model for SlideSet batch tools.
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
      * <li> type - {@code typeCode} associated with the column (required)
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
     
     /** Get item as actual data, i.e. for passing to a plugin */
     public Object getProcessedUnderlying(int column, int row)
             throws SlideSetException {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          return columns.get(column).get(row).getProcessedUnderlying();
     }
     
     /** Get the class of the actual data of an item */
     public Class<?> getProcessedClass(int column, int row) {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          return columns.get(column).get(row).getProcessedClass();
     }
     
     /** Get a text representation of an item, i.e. for writing to a file */
     public String getItemText(int column, int row) {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          return columns.get(column).get(row).getText();
     }
     
     /** Get the type of a column */
     public String getColumnTypeCode(int index) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index).get("type");
     }
     
     /** Get the readable name of a column {@code TypeCode} */
     public String getColumnTypeCodeName(int index) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return dtid.getTypeCodeName(getColumnTypeCode(index));
     }
     
     /** Get the properties of a column */
     public LinkedHashMap<String, String> getColumnProperties(int index) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index);
     }
     
     /**
      * Attempt a column type conversion by getting the text value of
      * the existing data, then creating new {@code DataElement}s
      * from the {@code String}s and {@code TypeCode}s.
      * 
      * @param index Column to convert
      * @param typeCode Type column should be converted to
      * @throws IllegalArgumentException If column conversion cannot be completed
      *         successfully, an exception is thrown and the original column
      *         data remains unchanged.
      */
     public void convertColumnTypeCode(int index, String typeCode)
          throws IllegalArgumentException {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          ArrayList<DataElement> newCol = new ArrayList<DataElement>(numRows);
          try {
               for(int row=0; row<numRows; row++) {
                    String data = getItemText(index, row);
                    newCol.add(dtid.createDataElement(data, typeCode, this));
               }
          }
          catch(IllegalArgumentException e) { throw e; }
          columns.remove(index);
          columns.add(index, newCol);
          /*columnTypes.remove(index);
          columnTypes.add(index, typeCode);*/
          columnProperties.get(index).put("type", typeCode);
     }
     
     /** Get the name of a column */
     public String getColumnName(int index) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          return columnProperties.get(index).get("name");
     }
     
     /** Set the name of a column */
     public void setColumnName(int index, String name) {
          if(!checkColumnBounds(index))
               throw new IndexOutOfBoundsException("Invalid column: " + String.valueOf(index));
          /*columnNames.remove(index);
          columnNames.add(index, name);*/
          columnProperties.get(index).put("name", name);
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
      * Can use Object of matching class or a String. */
     public void setUnderlying(int column, int row, Object underlying) {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          ArrayList<DataElement> col = columns.get(column);
          String typeCode = getColumnTypeCode(column);
          DataElement old = col.get(row);
          DataElement toSet;
          if(String.class.isInstance(underlying))
               toSet = dtid.createDataElement((String)underlying, typeCode, this);
          else {
               if(old.getUnderlying().getClass() != underlying.getClass())
                    throw new IllegalArgumentException(
                         "Cannot assign object of class " +
                         underlying.getClass().getSimpleName() +
                         " to column of class " +
                         old.getUnderlying().getClass().getSimpleName());
               toSet = dtid.createDataElement(underlying, typeCode, this);
          }
          col.remove(row);
          col.add(row, toSet);
          if(!checkColumnLengths())
               throw new IndexOutOfBoundsException("Problem changing item");
     }
     
     /**
      * Change the actual data, i.e. the result from a plug-in.
      * <p> Uses the following priority list to deal with the data: <ul>
      * <li> If the selected cell has an associated {@code LinkLinker},
      *        attempts to use it to write the data to a file.  Assumes
      *        that the table-stored value is the path to write to.
      *        If the table-stored value is either {@code null} or an
      *        empty string, tries to generate a default file name.
      *        See {@link #generateDefaultLinkPath(int, int) generateDefaultLinkPath}.
      * <li> If the selected cell <em>does not</em> have an associated
      *        {@code LinkLinker}, uses the
      *        {@link DataTypeIDService#canStoreInTable(java.lang.Object) DataTypeIDService}
      *        to determine if the data can be stored directly in the table.
      *        If it can, invokes
      *        {@link #setUnderlying(int, int, java.lang.Object) setUnderlying}.
      * <li> If not, throws a runtime exception.
      * </ul>
      * @throws DefaultPathNotSetException if an attempt to generate a default
      *        file name fails because there is no default link path set for
      *        the column.
      */
     public void setProcessedUnderlying(int column, int row, Object data)
             throws DefaultPathNotSetException, SlideSetException {
          if(!checkBounds(column, row))
               throw new IndexOutOfBoundsException("Invalid index: C" +
               String.valueOf(column) + ",R" + String.valueOf(row));
          DataElement e = columns.get(column).get(row);
          Linker l = e.getLinker();
          if(LinkLinker.class.isInstance(l)) {
               String path = (String) e.getUnderlying();
               if(path == null || path.trim().equals("")) {
                    path = generateDefaultLinkPath(column, row);
                    setUnderlying(column, row, path);
               }
               ((LinkLinker) l).write(resolvePath(path), data);
               return;
          }
          else if(dtid.canStoreInTable(data)) {
               setUnderlying(column, row, data);
               return;
          }
          throw new IllegalArgumentException("Cannot save data of type " + data.getClass().getName());
     }
     
     /**
      * Add an empty column
      * @param properties Column properties
      * @return The column index of the new column
      */
     public int addColumn(LinkedHashMap<String, String> properties) {
          columnProperties.add(properties);
          ArrayList<DataElement> col = new ArrayList<DataElement>(Math.max(2*numRows, 16));
          for(int i=0; i<numRows; i++) {
               DataElement element = dtid.createDataElement(properties.get("type"), this);
               col.add(element);
          }
          columns.add(col);
          if(!checkColumnLengths()) throw
               new IndexOutOfBoundsException("Could not balance columns when adding " + name);
          return columns.size() - 1;
     }
     
     /**
      * Add an empty column
      * @param name Name of the column
      * @param typeCode {@code typeCode} of the column to create
      * @return The column index of the new column
      */
     public int addColumn(String name, String typeCode) {
          LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
          props.put("name", name);
          props.put("type", typeCode);
          return addColumn(props);
     }
     
     /**
      * Add a column with data
      * @param properties Column properties
      * @param data An {@code ArrayList} of the {@code DataElement}s in the column
      * @return The column index of the new column
      */
     public int addColumn(LinkedHashMap<String, String> properties, ArrayList<DataElement> data) {
          if(data.size() != getNumRows() && !(getNumRows() == 0 && getNumCols() == 0))
               throw new IllegalArgumentException("Cannot add a column with " +
                    String.valueOf(data.size()) + " elements cannot be added to a table with " +
                    String.valueOf(getNumRows()) + " existing rows and " +
                    String.valueOf(getNumCols()) + " existing columns");
          for(DataElement e : data)
               if(!e.getTypeCode().equals(properties.get("type")))
                    throw new IllegalArgumentException("TypeCodes in data are not equal");
          columnProperties.add(properties);
          columns.add(data);
          if(numRows == 0)
               numRows = data.size();
          if(!checkColumnLengths()) throw
               new IndexOutOfBoundsException("Could not balance columns when adding " + name);
          return columns.size() - 1;
     }
     
     /**
      * Add a column with data
      * @param name Name of the column
      * @param typeCode {@code typeCode} of the column to create
      * @param data An {@code ArrayList} of the {@code DataElement}s in the column
      * @return The column index of the new column
      */
     public int addColumn(String name, String typeCode, ArrayList<DataElement> data) {
          LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();
          props.put("name", name);
          props.put("type", typeCode);
          return addColumn(props, data);
     }
     
     /** Multiple input version of {@link  #addColumn} */
     public void addColumns(List<String> names, List<String> typeCodes) {
          if(names.size() != typeCodes.size())
               throw new IllegalArgumentException("Column name and prototype lists are unequal");
          for(int i=0; i<names.size(); i++)
               addColumn(names.get(i), typeCodes.get(i));
     }
     
     /** Multiple input version of {@link  #addColumn} */
     public void addColumns(List<String> names, List<String> typeCodes, List<ArrayList<DataElement>> data) {
          if(names.size() != typeCodes.size() || typeCodes.size() != data.size())
               throw new IllegalArgumentException("Column name, prototype, and data lists are unequal");
          for(int i=0; i<names.size(); i++)
               addColumn(names.get(i), typeCodes.get(i), data.get(i));
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
      * Add a row with default values
      * @return The row index of the new row
      */
     public int addRow() {
          for(int i=0; i<columns.size(); i++) {
               DataElement element = dtid.createDataElement(getColumnTypeCode(i), this);
               columns.get(i).add(element);
          }
          numRows++;
          if(!checkColumnLengths()) throw
               new IndexOutOfBoundsException("Could not balance columns when adding row");
          return numRows - 1;
     }
     
     /**
      * Add a row from a {@code List} of {@code DataElement}s.
      * The {@code DataElement typeCode}s must match the row {@code typeCodes}s.
      * @return The row index of the new row
      */
     public int addRow(List<DataElement> data) {
          if(data.size() != getNumCols())
               throw new IllegalArgumentException("Length of DataElement list does not match number of columns");
          for(int i=0; i<data.size(); i++) {
               DataElement e = data.get(i);
               if(!e.getTypeCode().equals(getColumnTypeCode(i)))
                    throw new IllegalArgumentException(
                         "DataElement[" + String.valueOf(i) +
                         "] does not have the same typeCode as column " +
                         String.valueOf(i));
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
      * Set a cell to an auto-generated link, <em>if</em> the cell
      * is associated with a {@link LinkLinker}.
      * @param column
      * @param row
      * @throws DefaultPathNotSetException 
      */
     public void makeDefaultLink(int column, int row)
             throws DefaultPathNotSetException {
          if(!checkColumnBounds(column))
               throw new IllegalArgumentException("Column index out of bounds");
          if(!LinkLinker.class.isInstance(columns.get(column).get(row).getLinker()))
               return;
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
     
}
