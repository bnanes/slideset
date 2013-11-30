package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import edu.emory.cellbio.ijbat.ex.SlideSetException;
import javax.swing.table.AbstractTableModel;

/**
 * Swing {@code TableModel} for the {@code SlideSet} class.
 * @see javax.swing.JTable
 * @author Benjamin Nanes
 */
public class SlideSetTableModel extends AbstractTableModel {
     
     // -- Fields --
     
     /** The underlying data */
     private SlideSet slideSet;
     
     // -- Constructors --
     
     public SlideSetTableModel(SlideSet data) {
          slideSet = data;
     }
     
     /*public SlideSetTableModel() {
          this(new SlideSet());
     }*/
     
     // -- Methods --
     
     @Override
     public Object getValueAt(int row, int column) {
          return slideSet.getUnderlying(column, row);
     }
     
     @Override
     public int getColumnCount() {
          return slideSet.getNumCols();
     }
     
     @Override
     public int getRowCount() {
          return slideSet.getNumRows();
     }
     
     @Override
     public String getColumnName(int column) {
          return "<html><center>" 
               + slideSet.getColumnName(column) 
               + "<br>[" 
               + slideSet.getColumnTypeName(column)
               + "]</center></html>";
     }
     
     @Override
     public boolean isCellEditable(int rowIndex, int columnIndex) {
          return true;
     }
     
     @Override
     public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
          try {
              slideSet.setUnderlying(columnIndex, rowIndex, aValue);
          } catch(SlideSetException e) {
              throw new IllegalArgumentException("Error changing cell value: ", e);
          }
     }
     
     @Override
     public Class<?> getColumnClass(int columnIndex) {
          if(slideSet.getNumRows() == 0)
               return Object.class;
          return slideSet.getUnderlying(columnIndex, 0).getClass();
     }
     
}
