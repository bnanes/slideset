package edu.emory.cellbio.ijbat.dm;
import edu.emory.cellbio.ijbat.dm.write.ElementWriterMetadata;
import edu.emory.cellbio.ijbat.dm.write.ElementWriter;
import edu.emory.cellbio.ijbat.dm.read.ElementReaderMetadata;
import edu.emory.cellbio.ijbat.dm.read.ElementReader;
import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import net.imagej.ImageJ;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.scijava.annotations.Index;
import org.scijava.annotations.IndexItem;
import org.scijava.plugin.PluginService;

/**
 * <h2> Linking Slide Set tables to commands</h2>
 * 
 * The DataTypeIDService maintains indices of available
 * {@link DataElement} types, {@link ElementReader}s, 
 * and {@link ElementWriter}s, and facilitates links 
 * between {@link SlideSet} table columns and command 
 * input and output parameters. It also maintains an 
 * index of human-readable MIME type names which can 
 * be used by the user interface. A single instance of 
 * {@code DataTypeIDService} is created when Slide Set 
 * is run and is shared by all the Slide Set components.</p>
 * 
 * <h3> DataElement index </h3>
 * 
 * This list contains available {@code DataElement} 
 * classes and their metadata. Along with the MIME 
 * type index, it allows DataTypeIDService to create 
 * human-readable labels for column data types. The 
 * complement of {@code DataElement} types is currently 
 * fixed, though extension may be possible in future 
 * releases. The available types are: <ol>
 * <li> {@link BooleanElement} ("Logical"; {@code Boolean} underlying value)
 * <li> {@link IntegerElement} ("Integer"; {@code Integer} underlying value)
 * <li> {@link DoubleElement} ("Numeric"; {@code Double} underlying value)
 * <li> {@link StringElement} ("Text"; {@code String} underlying value)
 * <li> {@link FileLinkElement} ("~ File"*; {@code String} underlying value)
 * </ol> *DataTypeIDService will replace a "~" character 
 * in the element name with the MIME type name.
 * 
 * <h3> ElementReader and ElementWriter indeces </h3>
 * 
 * These lists contains available {@code ElementReader}
 * classes used to link {@code DataElement} "underlying"
 * data to the "processed" data needed for command inputs
 * and available {@code ElementWriter} classes used to create
 * {@code DataElement} "underlying" data from "processed"
 * data in command results. They are populated when 
 * {@code DataTypeIDService} is instantiated. Readers and 
 * writers in Slide Set core include: <ol>
 * <li> Direct transfer of underlying types 
 * (primitives using {@code java.lang} wrapper classes
 * and {@code String})
 * <li> For primitive types not implemented as an 
 * underlying type in one of the {@code DataElement}
 * classes ({@code byte}, {@code short}, {@code long},
 * and {@code float}), casts to and from {@code int} or 
 * {@code double}
 * <li> Image file reader and writer (using any file 
 * format compatible with ImageJ)
 * <li> Region of interest (ROI) set file reader and writer
 * </ol>
 * 
 * <h3> Linking SlideSet columns to command parameters </h3>
 * 
 * {@code DataTypeIDService} will identify table columns 
 * and {@code ElementReader}s appropriate for a command 
 * input by searching the index for readers which produce a 
 * "processed" data type cast-compatible with the input type, 
 * and the table for columns with DataElement classes and 
 * MIME types that are appropriate for the selected reader. 
 * A convenience class binding an instance of the reader to 
 * the appropriate column ({@link ColumnBoundReader}) is returned 
 * so that once the match has been made, the component requesting 
 * the match has no need to interact directly with the 
 * {@code SlideSet} table or manage "underlying" data 
 * conversion to produce "processed" data for the command input. 
 * {@code ColumnBoundReader}s can also wrap constant data, 
 * as if they were table columns with identical values in each row.
 * 
 * <p> Similarly for command results, {@code DataTypeIDService}
 * will identify {@code ElementWriter}s that can be used to 
 * convert "processed" data from the command to "underlying"
 * data in a {@code DataElement}. An empty {@code SlideSet}
 * can be created with columns appropriate for the {@code DataElement}
 * classes produced by the selected {@code ElementWriter}s,
 * along with convenience classes binding writer instances 
 * to the appropriate columns ({@link ColumnBoundWriter}s).
 * 
 * <p> Lastly, {@code DataTypeIDService} can search for 
 * {@code ElementReader} &mdash; {@code ElementWriter}
 * pairs which can be used to read and write data from 
 * a {@code SlideSet} table column without altering its format. 
 * This type of match facilitates in situ editing of table 
 * "processed" data without creating a separate results table. 
 * For example, the {@link edu.emory.cellbio.ijbat.ui.RoiEditor}
 * uses this match to read and write ROI set file data that 
 * are included in a table as path references.
 * 
 * @author Benjamin Nanes
 */
public class DataTypeIDService {
     
    // -- Fields --
    
    private ImageJ ij;
    private PluginService ps;
    
    /** Index of available {@link DataElement} types */
    private ArrayList<DataElementRecord> dataElementIndex;
    /** Index of available {@link ElementReader}s */
    private ArrayList<ReaderRecord> elementReaderIndex;
    /** Index of available {@link ElementWriter}s */
    private ArrayList<WriterRecord> elementWriterIndex;
    /** Index associating MIME types with human-readable names:
     *  &lt;MIME type, readable name&gt; */
    private LinkedHashMap<String, String> mimeReadableIndex;
    /** Index of aliased types:
     *  &lt;real type, alias type&gt;
     *  @see TypeAlias */
    private LinkedHashMap<Class<?>, Class<? extends TypeAlias>> typeAliasIndex;
    
    // -- Constructor --
    
    public DataTypeIDService(ImageJ ij) {
        this.ij = ij;
        ps = ij.get(PluginService.class);
        buildDataElementIndex();
        buildElementReaderIndex();
        buildElementWriterIndex();
        buildMimeReadableIndex();
        buildTypeAliasIndex();
    }
    
    // -- Methods --
    
    /**
     * Get the list of {@link ElementReader}s that will
     * read to the specified type.
     * 
     * @param type Selected {@link ElementReader}s will return objects
     *             that are assignment-compatible with this class
     * @param readers List to receive the compatible {@link ElementReader} classes
     * @param names List to receive the human-readable names of the
     *              compatible {@code ElementReader}s
     * @param filterHidden If true, will not select {@link ElementReader}s
     *              that have been marked as {@code hidden} in their
     *              {@link ElementReaderMetadata} annotations.
     */
    public void getCompatableReaders(
            Class<?> type,
            ArrayList<Class<? extends ElementReader>> readers,
            ArrayList<String> names,
            boolean filterHidden) {
        type = getPrimitiveWrapper(type);
        type = getTypeAlias(type);
        readers.clear();
        names.clear();
        for(ReaderRecord r : elementReaderIndex) {
            if(type.isAssignableFrom(r.processedType)
                    && (!filterHidden || !r.hidden)) {
                readers.add(r.reader);
                names.add(r.name + " constant");
            }
        }
    }
    
    /**
     * Get a list of {@link ColumnBoundReader}s that will read
     * data from the given table that are assignment-compatible with
     * the specified class. If not compatible {@code CoulmnBoundReader}s
     * can be created, returns an empty list.
     * 
     * @param type Selected readers will return values assignment-compatible
     *             with this class
     * @param data Table to which the selected {@link ColumnBoundReader}s
     *             will be bound
     */
    public ArrayList<ColumnBoundReader> getCompatableColumnReaders(
            Class<?> type, SlideSet data)
            throws SlideSetException {
        type = getPrimitiveWrapper(type);
        type = getTypeAlias(type);
        ArrayList<ColumnBoundReader> finalList
                = new ArrayList<ColumnBoundReader>();
        ArrayList<ReaderRecord> firstList = new ArrayList<ReaderRecord>();
        for(ReaderRecord r : elementReaderIndex) {
            if(type.isAssignableFrom(r.processedType))
                firstList.add(r);
        }
        for(int i=0; i<data.getNumCols(); i++) {
            for(ReaderRecord r : firstList) {
                if(r.elementType
                        .isAssignableFrom(data.getColumnElementType(i))
                        && ( r.mimeTypes.isEmpty() 
                             || r.mimeTypes.contains(data.getColumnMimeType(i))))
                    try {
                        finalList.add(
                                new ColumnBoundReader(data, i,
                                (ElementReader)r.reader.newInstance()));
                    } catch(Exception e) {
                        throw new SlideSetException(e);
                    }
            }
        }
        return finalList;
    }
    
    /**
     * Get a list of {@link ElementWriter}s compatible with a class.
     * 
     * @param type Selected {@code ElementWriters} will accept this
     *             class as input for their {@code write()} methods
     * @param writers List to receive the selected {@code ElementWriter}s.
     *             If no appropriate writers are found, the list will be emptied.
     * @param names List to receive human-readable names of the selected
     *             {@code ElementWriter}s
     */
    public void getCompatableWriters(
            Class<?> type,
            ArrayList<Class<? extends ElementWriter>> writers,
            ArrayList<String> names,
            ArrayList<String> linkExt) {
        type = getPrimitiveWrapper(type);
        type = getTypeAlias(type);
        writers.clear();
        names.clear();
        for(WriterRecord r : elementWriterIndex) {
            if(r.processedType.isAssignableFrom(type)) {
                writers.add(r.writer);
                names.add(r.name);
                linkExt.add(r.linkExt);
            }
        }
    }
       
    /**
     * Create a table with column types matching the specified list
     * of {@link ElementWriter} classes.
     * <p> Specifically, for each
     * {@code ElementWriter} type provided, the table will contain
     * one column, set with {@link DataElement} class and MIME type
     * to match the output of the {@code ElementWriter} type.
     * A {@link ColumnBoundWriter} will be instantiated for each
     * column in the table, binding an instance of the specified
     * {@code ElementWriter} to it's corresponding column.
     * <p> Note that the created {@code SlideSet} will have 0 rows.
     * Rows must be added to the table before the created
     * {@code ColumnBoundWriter}s can be used.
     * 
     * @param columnNames List of labels for the table columns
     * @param writerTypes List of {@code ElementWriter} types that
     *           will be used to define the table column data types.
     * @param table Empty {@link SlideSet} that will receive the new table
     * @param writers List to receive the {@code ColumnBoundWriter}s.
     *           This list will be in the same order as the provided
     *           {@code ElementWriter} types.
     */
    public void getTableForWriters(
            ArrayList<String> columnNames,
            ArrayList<Class<? extends ElementWriter>> writerTypes,
            SlideSet table,
            ArrayList<ColumnBoundWriter> writers)
            throws SlideSetException {
        if(table.getNumCols() != 0 || table.getNumRows() != 0)
            throw new SlideSetException("Need empty table to prepare for output");
        for(Class<? extends ElementWriter> wt : writerTypes) {
            for(WriterRecord r : elementWriterIndex) {
                if(r.writer != wt)
                    continue;
                table.addColumn(".", r.elementType, r.mimeType);
                break;
            }
        }
        if(table.getNumCols() != columnNames.size())
            throw new SlideSetException("Different number of column names and successfully created columns!");
        writers.clear();
        try {
            for(int i=0; i<columnNames.size(); i++) {
                table.setColumnName(i, columnNames.get(i));
                writers.add(new ColumnBoundWriter(table, i, writerTypes.get(i).newInstance()));
            }
        } catch(Exception e) {
            throw new SlideSetException("Unable to instantiate writer: ", e);
        }
    }
    
    /**
     * Get {@link ColumnBoundReader}&mdash;{@link ColumnBoundWriter} pairs
     * that can be used to read and write the specified type to a column
     * in the provided {@link SlideSet} table.
     * 
     * <p>One {@link ColumnBoundReader} and one {@link ColumnBoundWriter}
     * will be instantiated for each case where:<ol>
     * <li>An {@link ElementReader} and {@link ElementWriter} both
     *     support the specified processed type;
     * <li>The reader and writer also both support the same
     *     {@link DataElement} type and MIME type; and
     * <li>A column in the table contains that {@code DataElement} type
     *     and MIME type.</ol>
     * The {@link ColumnBoundReader}&mdash;{@link ColumnBoundWriter} pairs
     * are expected to support reciprocal read&mdash;write operations, i.e.
     * editing <em>processed data</em> stored in a table (ex. ROI data), rather than
     * the more straightforward situation of editing the table's
     * <em>underlying</em> values (ex. ROI file references). If no appropriate
     * reader&mdeash;writer pairs can be created, returns empty lists.
     * 
     * @param type Processed type used to select reader&mdash;writer pairs
     * @param data Table to which the returned readers and writers will be bound
     * @param readers List to receive the {@link ColumnBoundReader}s
     * @param writers List to receive the {@link ColumnBoundWriter}s
     */
    public void getColumnReadWritePairs(
            Class<?> type,
            SlideSet data,
            ArrayList<ColumnBoundReader> readers,
            ArrayList<ColumnBoundWriter> writers)
            throws SlideSetException {
        type = getPrimitiveWrapper(type);
        type = getTypeAlias(type);
        ArrayList<ReaderRecord> rrs = new ArrayList<ReaderRecord>();
        ArrayList<WriterRecord> wrs = new ArrayList<WriterRecord>();
        for(ReaderRecord r : elementReaderIndex)
            if(type.isAssignableFrom(r.processedType))
                rrs.add(r);
        for(WriterRecord r : elementWriterIndex)
            if(r.processedType.isAssignableFrom(type))
                wrs.add(r);
        ArrayList<ReaderRecord> rrp = new ArrayList<ReaderRecord>();
        ArrayList<WriterRecord> wrp = new ArrayList<WriterRecord>();
        for(ReaderRecord rr : rrs) {
            for(WriterRecord wr : wrs) {
                if(rr.elementType == wr.elementType
                        && rr.mimeTypes.contains(wr.mimeType)) {
                    rrp.add(rr);
                    wrp.add(wr);
                    break;
                }
            }
        }
        readers.clear();
        writers.clear();
        for(int i = 0; i < data.getNumCols(); i++) {
            for(int j = 0; j < rrp.size(); j++) {
                final ReaderRecord rr = rrp.get(j);
                final WriterRecord wr = wrp.get(j);
                if(data.getColumnElementType(i) == rr.elementType
                        && data.getColumnMimeType(i).equals(wr.mimeType)) {
                    try {
                        final ColumnBoundReader cbr
                                = new ColumnBoundReader(data, i,
                                (ElementReader) rr.reader.newInstance());
                        final ColumnBoundWriter cbw
                                = new ColumnBoundWriter(data, i,
                                (ElementWriter) wr.writer.newInstance());
                        readers.add(cbr);
                        writers.add(cbw);
                        break;
                    } catch(Exception e) {
                        throw new SlideSetException(e);
                    }
                }
            }
        }
    }
    
    /**
     * Get the {@link DataElement} type read by an {@link ElementReader} type.
     */
    public Class<? extends DataElement> getReaderElementType
            (Class<? extends ElementReader> reader)
            throws SlideSetException {
        for(ReaderRecord r : elementReaderIndex) {
            if(r.reader == reader)
                return r.elementType;
        }
        throw new SlideSetException(
                "Reader is not in index: " + reader.getName());
    }
    
    /**
     * Get the {@link DataElement} type written by an {@link ElementWriter} type.
     */
    public Class<? extends DataElement> getWriterElementType
            (Class<? extends ElementWriter> writer)
            throws SlideSetException {
        for(WriterRecord r : elementWriterIndex) {
            if(r.writer == writer)
                return r.elementType;
        }
        throw new SlideSetException(
                "Writer is not in index: " + writer.getName());
    }
    
    /**
     * Get the <em>underlying</em> type read by an {@link ElementReader} type.
     */
    public Class<?> getReaderUnderlyingType(
            Class<? extends ElementReader> reader)
            throws SlideSetException {
        Class<? extends DataElement> el = getReaderElementType(reader);
        for(DataElementRecord r : dataElementIndex) {
            if(r.dataElement == el)
                return r.underlyingClass;
        }
        throw new SlideSetException(
                "DataElement type is not in index: " + el.getName());
    }
    
    /**
     * Get the list of MIME types that can be read by
     * an {@link ElementReader} type.
     */
    public ArrayList<String> getReaderMimeTypes(
            Class<? extends ElementReader> reader)
            throws SlideSetException {
        for(ReaderRecord r : elementReaderIndex) {
            if(r.reader == reader)
                return r.mimeTypes;
        }
        throw new SlideSetException(
                "Reader is not in index: " + reader.getName());
    }
    
    /**
     * Create a {@link ColumnBoundReader} that will always read from
     * a constant {@link DataElement}. This contrasts with the usual
     * behavior of {@code ColumnBoundReader}s, to bind an
     * {@link ElementReader} to one column of a {@link SlideSet} table.
     * The method facilitates the use of constants
     * for command inputs instead of values from the input table.
     * 
     * @param readerType The {@link ElementReader} type that will
     *    be used to read the constant data. Note that the reader
     *    must support underlying data of the same type as the
     *    {@code data} parameter passed to this method.
     * @param data The underlying data that will be wrapped in an
     *    appropriate {@code DataElement} and read by the returned
     *    {@code ColumnBoundReader}
     * @param table Table that will be the 'owner' of the created
     *    {@code DataElement}. This table's working directory will
     *    be used to resolve any relative paths in file links.
     * @throws SlideSetException If the requested {@code ElementReader}
     *    is not in the index or cannot be instantiated; if an
     *    appropriate {@code DataElement} cannot be found; of if
     *    the {@code ColumnBoundReader} cannot be created.
     */
    public ColumnBoundReader<?, ?> makeColumnBoundConstantReader(
            Class<? extends ElementReader> readerType,
            Object data,
            SlideSet table)
            throws SlideSetException {
        Class<?> u = getReaderUnderlyingType(readerType);
        Class<? extends DataElement> v = getReaderElementType(readerType);
        boolean treatDataAsString = false;
        if(!u.isInstance(data)) {
            if(data instanceof String)
                treatDataAsString = true;
            else throw new SlideSetException(
                    "The provided data cannot be cast to the"
                    + "appropriate underlying type!"
                    + "\nData: " + data.getClass().getName()
                    + "\nExpected: " + u.getName());
        }
        ArrayList<String> mimes = getReaderMimeTypes(readerType);
        String mime = mimes.isEmpty() ? null : mimes.get(0);
        DataElement el;
        ElementReader reader;
        try {
            el = v.newInstance();
            reader = readerType.newInstance();
            el.setMimeType(mime);
            el.setOwner(table);
            if(!treatDataAsString)
                el.setUnderlying(data);
            else
                el.setUnderlyingText((String)data);
        } catch(Exception e) {
            throw new SlideSetException(e);
        }
        return new ColumnBoundReader(el, reader, this);
    }
    
    /**
     * Get a human-readable name for a {@link DataElement} type&mdash;
     * MIME type pair
     */
    public String getReadableElementType(
            Class<? extends DataElement> type,
            String mimeType) {
        String n = null;
        for(DataElementRecord r : dataElementIndex) {
            if(r.dataElement == type) {
                n = r.name;
                break;
            }
        }
        if(n == null)
            throw new IllegalArgumentException(
                    "Element type not in index: " + type.getName());
        int q = n.indexOf("~");
        if(q < 0)
            return n;
        String m = getMimeReadableName(mimeType);
        if(q == 0)
            return m + n.substring(1);
        if(q == n.length()-1)
            return n.substring(0, n.length()-2) + m;
        String[] ns = n.split("~", 2);
        return ns[0] + m + ns[1];
    }
    
    /**
     * Get a list of {@link DataElement} types in the index.
     * @param includeHidden If {@code true}, include types
     *    marked as 'hidden' in their {@link DataElementMetadata annotations}.
     */
    public ArrayList<Class<? extends DataElement>> getElementTypes(
            boolean includeHidden) {
        ArrayList<Class<? extends DataElement>> types
                = new ArrayList<Class<? extends DataElement>>();
        for(DataElementRecord r : dataElementIndex) {
            if(includeHidden || !r.hidden)
                types.add(r.dataElement);
        }
        return types;
    }
    
    /**
     * Get the human-readable name of an MIME type, if
     * it is registered in the index. If there is no
     * matching type in the index, returns the MIME type itself.
     */
    public String getMimeReadableName(String mimeType) {
        if(mimeType == null)
            mimeType = "Unknown";
        String x = mimeReadableIndex.get(mimeType);
        return x == null ? mimeType : x;
    }
    
    /**
     * Get a list of MIME types from the index
     */
    public ArrayList<String> getMimeTypes() {
        ArrayList<String> mimes = new ArrayList<String>();
        for(String mime : mimeReadableIndex.keySet())
            mimes.add(mime);
        return mimes;
    }
    
    // -- Helper methods --
    
    /** Set-up the {@link DataElement} index */
    private void buildDataElementIndex() {
        dataElementIndex = new ArrayList<DataElementRecord>();
        dataElementIndex.add(new DataElementRecord(
                BooleanElement.class, "Logical", Boolean.class, false));
        dataElementIndex.add(new DataElementRecord(
                IntegerElement.class, "Integer", Integer.class, false));
        dataElementIndex.add(new DataElementRecord(
                DoubleElement.class, "Numeric", Double.class, false));
        dataElementIndex.add(new DataElementRecord(
                StringElement.class, "Text", String.class, false));
        dataElementIndex.add(new DataElementRecord(
                FileLinkElement.class, "~ File", String.class, false));
    }
    
    /** Set-up the {@link ElementReader} index */
    private void buildElementReaderIndex() {
        elementReaderIndex = new ArrayList<ReaderRecord>();
        for(IndexItem<ElementReaderMetadata> item :
                Index.load(ElementReaderMetadata.class, getClass().getClassLoader())) {
            ElementReaderMetadata a = item.annotation();
            try {
                ReaderRecord r = new ReaderRecord(
                        item.className(),
                        a.name(),
                        a.processedType(),
                        a.elementType(),
                        a.mimeTypes(),
                        a.hidden());
                elementReaderIndex.add(r);
            } catch(ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
    
    /** Set-up the {@link ElementWriter} index */
    private void buildElementWriterIndex() {
        elementWriterIndex = new ArrayList<WriterRecord>();
        for(IndexItem<ElementWriterMetadata> item :
                Index.load(ElementWriterMetadata.class, getClass().getClassLoader())) {
            ElementWriterMetadata a = item.annotation();
            try {
                WriterRecord r = new WriterRecord(
                        item.className(),
                        a.name(),
                        a.processedType(),
                        a.elementType(),
                        a.mimeType(),
                        a.linkExt());
                if(r.mimeType.equals("null"))
                    r.mimeType = null;
                if(r.linkExt.equals("null"))
                    r.linkExt = null;
                elementWriterIndex.add(r);
            } catch(ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
    
    /** Set-up the MIME type index */
    private void buildMimeReadableIndex() {
        // For now this is hard-coded, but these could be loaded from annotations...
        mimeReadableIndex = new LinkedHashMap<String, String>();
        mimeReadableIndex.put(MIME.IMAGE, "Image");
        mimeReadableIndex.put(MIME.SVG, "ROI Set (SVG)");
        mimeReadableIndex.put(MIME.ROI2, "ROI Set (.roiset)");
        mimeReadableIndex.put(MIME.WEKA, "Trainable Segmentation Classifier");
    }
    
    /** Set-up the {@link TypeAlias} index */
    private void buildTypeAliasIndex() {
        for(IndexItem<TypeAliasMetadata> item :
                Index.load(TypeAliasMetadata.class, getClass().getClassLoader())) {
            typeAliasIndex = new
                    LinkedHashMap<Class<?>, Class<? extends TypeAlias>>();
            try {
                TypeAlias ta = (TypeAlias) Class.forName(
                        item.className()).newInstance();
                typeAliasIndex.put(
                        ta.getRealType(),
                        ta.getClass());
            } catch (Exception e) {
                throw new IllegalStateException(
                        "Couldn't load type aliases: ", e);
            }
        }
    }
    
    /** Returns the wrapper class for a primitive type.  If the type
     *  is not a primitive, returns the input */
    private Class<?> getPrimitiveWrapper(Class<?> type) {
        if(type.equals(Byte.TYPE))
            return Byte.class;
        if(type.equals(Short.TYPE))
            return Short.class;
        if(type.equals(Integer.TYPE))
            return Integer.class;
        if(type.equals(Long.TYPE))
            return Long.class;
        if(type.equals(Float.TYPE))
            return Float.class;
        if(type.equals(Double.TYPE))
            return Double.class;
        if(type.equals(Boolean.TYPE))
            return Boolean.class;
        return type;
    }
    
    /** Is the type a wrapper class for a java primitive? */
    private boolean isPrimativeWrapper(Class<?> type) {
        if(Byte.class.isAssignableFrom(type))
            return true;
        if(Short.class.isAssignableFrom(type))
            return true;
        if(Integer.class.isAssignableFrom(type))
            return true;
        if(Long.class.isAssignableFrom(type))
            return true;
        if(Float.class.isAssignableFrom(type))
            return true;
        if(Double.class.isAssignableFrom(type))
            return true;
        if(Boolean.class.isAssignableFrom(type))
            return true;
        return false;
    }
    
    /**
     * Replace a {@link TypeAlias} type with it's 'real' type.
     * Returns the provided type unchanged if it is not
     * included in the alias index.
     */
    private Class<?> getTypeAlias(Class<?> type) {
        Class<?> alias = typeAliasIndex.get(type);
        return alias == null ? type : alias;
    }
    
    // -- Private classes for indexing --
    
    private class DataElementRecord<T> {
        /** {@link DataElement} type */
        public Class<? extends DataElement<T>> dataElement;
        /** Element human-readable name */
        public String name;
        /** Underlying type used by the element */
        public Class<T> underlyingClass;
        /** Is this a hidden element type? */
        public boolean hidden;

        public DataElementRecord(
                Class<? extends DataElement<T>> dataElement,
                String name,
                Class<T> underlyingClass,
                boolean hidden) {
            this.dataElement = dataElement;
            this.name = name;
            this.underlyingClass = underlyingClass;
            this.hidden = hidden;
        }
    }
    
    private class ReaderRecord<E extends DataElement, P> {
        /** {@link ElementReader} type */
        public Class<? extends ElementReader<E, P>> reader;
        /** Human-readable name for the reader */
        public String name;
        /** Type returned by this reader's {@code read()} method */
        public Class<P> processedType;
        /** {@code DataElement} type read by this reader */
        public Class<E> elementType;
        /** MIME types compatible with this reader */
        public ArrayList<String> mimeTypes;
        /** Is this a hidden reader? */
        public boolean hidden;

        public ReaderRecord(
                String reader,
                String name,
                Class<P> processedType,
                Class<E> elementType,
                String[] mimeTypes,
                boolean hidden)
                throws ClassNotFoundException {
            this.reader = (Class<? extends ElementReader<E, P>>)
                    Class.forName(reader);
            this.name = name;
            this.processedType = processedType;
            this.elementType = elementType;
            this.mimeTypes = new ArrayList<String>(Arrays.asList(mimeTypes));
            this.hidden = hidden;
        }
    }
    
    private class WriterRecord<E extends DataElement, P> {
        /** {@link ElementWriter} type */
        public Class<? extends ElementWriter<E, P>> writer;
        /** Human-readable name for the writer */
        public String name;
        /** Type accepted by this writer's {@code write()} method */
        public Class<P> processedType;
        /** {@code DataElement} type written by this writer */
        public Class<E> elementType;
        /** MIME type written by this writer */
        public String mimeType;
        /** For file links, the default file extension to use with this writer */
        public String linkExt;

        public WriterRecord(
                String writer,
                String name,
                Class<P> processedType,
                Class<E> elementType,
                String mimeType,
                String linkExt)
                throws ClassNotFoundException {
            this.writer = (Class<? extends ElementWriter<E, P>>)
                    Class.forName(writer);
            this.name = name;
            this.processedType = processedType;
            this.elementType = elementType;
            this.mimeType = mimeType;
            this.linkExt = linkExt;
        }
    }
    
}
