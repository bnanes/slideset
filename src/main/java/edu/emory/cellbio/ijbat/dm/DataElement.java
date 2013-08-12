package edu.emory.cellbio.ijbat.dm;

import edu.emory.cellbio.ijbat.ex.SlideSetException;

/**
 *
 * @author Benjamin Nanes
 */
public class DataElement {
     
     // -- Fields --
     
     private final Object underlying;
     private final String typeCode;
     private final Linker linker;
     
     // -- Constructor --
     
     public DataElement(Object underlying, String typeCode, Linker linker) {
          if(underlying == null || typeCode == null || linker == null )
               throw new IllegalArgumentException("Can't initialize DataElement with null items");
          this.underlying = underlying;
          this.typeCode = typeCode;
          this.linker = linker;
     }
     
     // -- Methods --
     
     public String getText() {
          return underlying.toString();
     }
     
     public Object getUnderlying() {
          return underlying;
     }
     
     public String getTypeCode() {
          return typeCode;
     }
     
     public Linker getLinker() {
          return linker;
     }
     
     public Object getProcessedUnderlying() throws SlideSetException {
          return linker.process(underlying);
     }
     
     public Class<?> getProcessedClass() {
          return linker.getProcessedClass(underlying);
     }
     
}
