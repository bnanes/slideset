package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.SlideSet;

import imagej.ImageJ;
import org.scijava.plugin.PluginService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
     private ArrayList<LinkerRegistration> linkerIndex;
     
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
     public Map<String, String> getVisibleTypeCodes() {
          final HashMap<String,String> tCodes = new HashMap<String,String>(linkerIndex.size() + 5);
          for(LinkerRegistration r : linkerIndex) {
              if(!r.hidden)
                  tCodes.put(r.typeCode, r.name); 
          }
          return tCodes;
     }
     
     /**
      * Get the human-readable name of a {@code typeCode}.
      * Sub-menu paths may be indicated by "/".
      * <p>If there are multiple registrations for the {@typeCode},
      * prefers a non-hidden registration. If there are multiple
      * non-hidden registrations, makes no guarantee as to
      * which will be returned.
      * <p>Returns {@code null} if the {@code typeCode}
      * is not found.
      */
     public String getTypeCodeName(String typeCode) {
          String name = null;
          boolean hidden = true;
          for(LinkerRegistration r : linkerIndex) {
              if(r.typeCode.equals(typeCode) && (hidden || !r.hidden)) {
                  name = r.name;
                  hidden = r.hidden;
              }
          }
          return name;
     }
     
     /** Can this {@code TypeCode} specify a file reference? */
     public boolean isTypeCodeLinkLinker(String typeCode) {
          boolean result = false;
          for(LinkerRegistration r : linkerIndex) {
              if(LinkLinker.class.isAssignableFrom(r.linker)) {
                   result = true;
                   break;
              }
          }
          return result;
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
          for(final LinkerRegistration tcr : linkerIndex ) {
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
     
     /**
      * Create a Linker class for a DataElement.
      * No contract as to which is returned if multiple
      * linkers are available for the {@code typeCode}.
      */
     private Linker getLinker(String typeCode, SlideSet owner) {
          Class<? extends Linker> lClass = null;
          for(LinkerRegistration r : linkerIndex) {
               if(r.typeCode.equals(typeCode)) {
                   lClass = r.linker;
                   break;
               }
          }
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
          Class<?> c = null;
          for(LinkerRegistration r : linkerIndex) {
              if(r.typeCode.equals(typeCode)) {
                  c = r.underlyingClass;
                  break;
              }
          }
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
          linkerIndex = new ArrayList<LinkerRegistration>();
          // Primatives
          linkerIndex.add( new LinkerRegistration(
               "Byte", "Advanced/Byte", ObjectLinker.class, Byte.class, Byte.class));
          linkerIndex.add( new LinkerRegistration(
               "Short", "Advanced/Short", ObjectLinker.class, Short.class, Short.class));
          linkerIndex.add( new LinkerRegistration(
               "Integer", "Integer", ObjectLinker.class, Integer.class, Integer.class));
          linkerIndex.add( new LinkerRegistration(
               "Long", "Advanced/Long", ObjectLinker.class, Long.class, Long.class));
          linkerIndex.add( new LinkerRegistration(
               "Float", "Decimal", ObjectLinker.class, Float.class, Float.class));
          linkerIndex.add( new LinkerRegistration(
               "Boolean", "Logical", ObjectLinker.class, Boolean.class, Boolean.class));
          linkerIndex.add( new LinkerRegistration(
               "Double", "Advanced/Double", ObjectLinker.class, Double.class, Double.class));
          linkerIndex.add( new LinkerRegistration(
               "String", "Text", ObjectLinker.class, String.class, String.class));
          // Annotations
          for(final IndexItem<LinkerInfo, ?> i : 
                 Index.load(LinkerInfo.class, Void.class, 
                 Thread.currentThread().getContextClassLoader())) {
               try {
                    linkerIndex.add( new LinkerRegistration( 
                         i.annotation().typeCode(), 
                         i.annotation().name(), 
                         (Class<? extends Linker>) Class.forName(i.className()), 
                         Class.forName(i.annotation().underlying()), 
                         Class.forName(i.annotation().processed()) ));
               } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
               }
          }
     }
     
     // -- Container for typeCode list --
     
     private class LinkerRegistration {
          public String typeCode;
          public String name;
          public Class<? extends Linker> linker;
          public Class<?> underlyingClass;
          public Class<?> processedClass;
          public boolean hidden;
          
          public LinkerRegistration(String typeCode, String name, 
                  Class<? extends Linker> linker,
                  Class<?> underlyingClass, 
                  Class<?> processedClass) {
               this(typeCode, name, linker, underlyingClass, processedClass, false);
          }
          
          public LinkerRegistration(String typeCode, String name, 
                  Class<? extends Linker> linker,
                  Class<?> underlyingClass, 
                  Class<?> processedClass,
                  boolean hidden) {
               this.typeCode = typeCode;
               this.name = name;
               this.linker = linker;
               this.underlyingClass = underlyingClass;
               this.processedClass = processedClass;
               this.hidden = hidden;
          }
          
     }
     
}
