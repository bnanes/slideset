package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;

import imagej.ImageJ;
import org.scijava.plugin.PluginService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;

/**
 * 
 * @author Benjamin Nanes
 */
public class DataTypeIDService {
     
     // -- Fields --
     
     private ImageJ ij;
     private PluginService ps;
     
     /** Index of available {@code TypeCode}s */
     private ArrayList<TypeCodeRegistration> typeCodeIndex;
     /** Hash map for lookup of human-readable {@code TypeCode}
      *  names: &lt;{@code TypeCode}, name> */
     private LinkedHashMap<String, String> typeCodeNameHash;
     /** Hash map for lookup of underlying class associated
      *  with a {@code TypeCode} */
     private HashMap<String, Class<?>> typeCodeUClassHash;
     /** Hash map for lookup of {@link Linker} class associated
      *  with a {@code TypeCode} */
     private HashMap<String, Class<? extends Linker>> typeCodeLinkerHash;
     /** Hash map for lookup of {@code TypeCode}s compatible
      *  with a processed data type. */
     private HashMap<Class<?>, ArrayList<String>> pClassTypeCodeHash;
     
     // -- Constructor --
     
     public DataTypeIDService(ImageJ ij) {
          this.ij = ij;
          ps = ij.get(PluginService.class);
          buildTypeCodeIndex();
     }
     
     // -- Methods --
     
     public DataElement createDataElement(String typeCode, SlideSet owner) {
          return createDataElement((Object)null, typeCode, owner);
     }
     
     public DataElement createDataElement(Object underlying, SlideSet owner) {
          return createDataElement(underlying, null, owner);
     }
     
     public DataElement createDataElement(String data, String typeCode, SlideSet owner) {
          Object underlying = makeFromString(data, typeCode);
          return createDataElement(underlying, typeCode, owner);
     }
     
     public DataElement createDataElement(Object underlying, String typeCode, SlideSet owner) {
          if(owner == null)
               throw new IllegalArgumentException("Can't create ownerless DataElement");
          if(underlying == null && typeCode != null)
               underlying = getDefaultUnderlying(typeCode);
          if(typeCode == null && underlying != null)
               typeCode = suggestTypeCode(underlying);
          if(underlying == null)
               throw new IllegalArgumentException("Can't create DataElement without underlying Object or typeCode");
          Linker linker = getLinker(typeCode, owner);
          return new DataElement(underlying, typeCode, linker);
     }
     
     /** 
      * Get a list of indexes representing columns from the SlideSet that
      * contain data which could be assigned to a parameter of the given class
      */
     public ArrayList<Integer> getMatchingColumns(Class<?> type, SlideSet table) {
          ArrayList<Integer> okColumns = new ArrayList<Integer>();
          if(table.getNumRows() < 1) return okColumns;
          if(type.isPrimitive())
               type = getPrimitiveWrapper(type);
          for(int col=0; col<table.getNumCols(); col++)
               if(type.isAssignableFrom(table.getProcessedClass(col, 0)))
                    okColumns.add(col);
          return okColumns;
     }
     
     /**
      * @return A list of key-value pairs, where the key represents
      * the {@code TypeCode} and the value represents a human-readable
      * name for the type.
      */
     public Map<String, String> getAvailableTypeCodes() {
          return typeCodeNameHash;
     }
     
     /** Get the human-readable name of a {@code typeCode}.
      *  Sub-menu paths may be indicated by "/". */
     public String getTypeCodeName(String typeCode) {
          return typeCodeNameHash.get(typeCode);
     }
     
     /** Does this {@code TypeCode} specify a file reference? */
     public boolean isTypeCodeLinkLinker(String typeCode) {
          final Class<? extends Linker> l 
               = typeCodeLinkerHash.get(typeCode);
          return LinkLinker.class.isAssignableFrom(l);
     }
     
     /** Suggest a typeCode based on the processed class */
     public String suggestTypeCode(Object data) {
          return suggestTypeCode(data.getClass());
     }
     public String suggestTypeCode(Class<?> c) {
          final ArrayList<String> l = 
               getAppropriateTypeCodes(c);
          return l.isEmpty() ? "Object" : l.get(0);
     }
     
     /**
      * Get a list of {@code TypeCode}s compatible with
      * a given processed data class. If none are available,
      * returns an empty list.
      */
     public ArrayList<String> getAppropriateTypeCodes(Class<?> c) {
          final ArrayList<String> l = new ArrayList<String>(3);
          final Class<?> cWrapped = c.isPrimitive() ? getPrimitiveWrapper(c) : Object.class;
          for(final TypeCodeRegistration tcr : typeCodeIndex ) {
               if(tcr.processedClass.isAssignableFrom(c) 
                    || tcr.processedClass.isAssignableFrom(cWrapped))
                    l.add(tcr.typeCode);
          }
          return l == null ? new ArrayList<String>(2) : l;
     }
     
     /**
      * Check if it would be appropriate to store a piece of data
      * directly in a data table without any processing (i.e.
      * save to file using a {@code LinkLinker} class).
      * @param data The object to check
      * @return {@code true} if the object can reasonably be stored in
      *   a data table. Basically just primatives and {@code String}s
      * <br> {@code false} otherwise
      */
     public boolean canStoreInTable(Object data) {
          Class<?> c = data.getClass();
          return canStoreInTable(c);
     }
     public boolean canStoreInTable(Class<?> c) {
          if(c.isPrimitive())
               return true;
          if(getPrimitiveWrapper(c) != null)
               return true;
          if(isPrimativeWrapper(c))
               return true;
          if(String.class.isAssignableFrom(c))
               return true;
          /*if(java.io.File.class.isAssignableFrom(c))
               return true;*/
          return false;
     }
     
     // -- Helper methods --
     
     /** Create a Linker class for a DataElement */
     private Linker getLinker(String typeCode, SlideSet owner) {
          final Class<? extends Linker> lClass = typeCodeLinkerHash.get(typeCode);
          if(lClass == null)
               return new ObjectLinker(ij, owner);
          try { 
               return lClass.getConstructor(ImageJ.class, SlideSet.class).newInstance(ij, owner);
          } catch(Throwable t) { throw new IllegalArgumentException(t); }
     }
     
     /** Create an underlying Object from a String */
     private Object makeFromString(String data, String typeCode) {
          Object result;
          if(typeCode.equals("String"))
               result = data;
          else {
               Class<?> c = getDefaultUnderlying(typeCode).getClass();
               try {
                    result = c.getConstructor(String.class).newInstance(data);
               } catch(Throwable t) {
                    throw new IllegalArgumentException(
                         "Can't create " + c.getSimpleName() + " from String");
               }
          }
          return result;
     }
     
     /** Get the default underlying object for a typeCode */
     private Object getDefaultUnderlying(String typeCode) {
          return getDefaultUnderlying(getTypeUnderlyingClass(typeCode));
     }
     private Object getDefaultUnderlying(Class<?> underlyingClass) {
          try {
               if(underlyingClass.isAssignableFrom(String.class))
                    return "";
               if(underlyingClass.isAssignableFrom(Boolean.class))
                    return false;
               if(underlyingClass.isAssignableFrom(Byte.class))
                    return (byte)0;
               if(underlyingClass.isAssignableFrom(Short.class))
                    return (short)0;
               if(underlyingClass.isAssignableFrom(Integer.class))
                    return (int)0;
               if(underlyingClass.isAssignableFrom(Long.class))
                    return (long)0;
               if(underlyingClass.isAssignableFrom(Float.class))
                    return (float)0;
               if(underlyingClass.isAssignableFrom(Double.class))
                    return (double)0;
          } catch (Throwable t) {
               throw new IllegalArgumentException("Problem creating default instance of class "
                    + underlyingClass.getName(), t);
          }
          return underlyingClass.cast(null);
     }
     
     /**
      * Get the underlying class associated with a {@code TypeCode}
      */
     private Class<?> getTypeUnderlyingClass(String typeCode) {
          final Class<?> c = typeCodeUClassHash.get(typeCode);
          return c == null ? Object.class : c;
     }
     
     /** Returns the wrapper class for a primitive type.  If the type
      *  is not a primitive, returns null */
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
          return null;
     }
     
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
      * Generate an index of available {@code TypeCode}s.
      * Primatives are hard-coded.  Others are looked up
      * using the {@link LinkerInfo} annotation.
      */
     private void buildTypeCodeIndex() {
          typeCodeIndex = new ArrayList<TypeCodeRegistration>();
          // Primatives
          typeCodeIndex.add( new TypeCodeRegistration(
               "Byte", "Advanced/Byte", ObjectLinker.class, Byte.class, Byte.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "Short", "Advanced/Short", ObjectLinker.class, Short.class, Short.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "Integer", "Integer", ObjectLinker.class, Integer.class, Integer.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "Long", "Advanced/Long", ObjectLinker.class, Long.class, Long.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "Float", "Decimal", ObjectLinker.class, Float.class, Float.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "Boolean", "Logical", ObjectLinker.class, Boolean.class, Boolean.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "Double", "Advanced/Double", ObjectLinker.class, Double.class, Double.class));
          typeCodeIndex.add( new TypeCodeRegistration(
               "String", "Text", ObjectLinker.class, String.class, String.class));
          // Annotations
          for(final IndexItem<LinkerInfo, ?> i : 
                 Index.load(LinkerInfo.class, Void.class, 
                 Thread.currentThread().getContextClassLoader())) {
               try {
                    typeCodeIndex.add( new TypeCodeRegistration( 
                         i.annotation().typeCode(), 
                         i.annotation().name(), 
                         (Class<? extends Linker>) Class.forName(i.className()), 
                         Class.forName(i.annotation().underlying()), 
                         Class.forName(i.annotation().processed()) ));
               } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
               }
          }
          // Generate hash maps
          typeCodeNameHash = new LinkedHashMap<String, String>(typeCodeIndex.size());
          typeCodeUClassHash = new HashMap<String, Class<?>>(typeCodeIndex.size());
          typeCodeLinkerHash = new HashMap<String, Class<? extends Linker>>(typeCodeIndex.size());
          //pClassTypeCodeHash = new HashMap<Class<?>, ArrayList<String>>(typeCodeIndex.size());
          for(TypeCodeRegistration r : typeCodeIndex) {
               if(typeCodeNameHash.containsKey(r.typeCode))
                    throw new IllegalArgumentException("Duplicate TypeCode: " + r.typeCode);
               typeCodeNameHash.put(r.typeCode, r.name);
               typeCodeUClassHash.put(r.typeCode, r.underlyingClass);
               typeCodeLinkerHash.put(r.typeCode, r.linker);
               /*if(!pClassTypeCodeHash.containsKey(r.processedClass)) {
                    final ArrayList<String> l = new ArrayList<String>(2);
                    l.add(r.typeCode);
                    pClassTypeCodeHash.put(r.processedClass, l);
               }
               else
                    pClassTypeCodeHash.get(r.processedClass).add(r.typeCode);*/
          }
     }
     
     // -- Container for typeCode list --
     
     private class TypeCodeRegistration {
          public String typeCode;
          public String name;
          public Class<? extends Linker> linker;
          public Class<?> underlyingClass;
          public Class<?> processedClass;

          public TypeCodeRegistration(String typeCode, String name, 
                  Class<? extends Linker> linker,
                  Class<?> underlyingClass, 
                  Class<?> processedClass) {
               this.typeCode = typeCode;
               this.name = name;
               this.linker = linker;
               this.underlyingClass = underlyingClass;
               this.processedClass = processedClass;
          }
          
     }
     
}
