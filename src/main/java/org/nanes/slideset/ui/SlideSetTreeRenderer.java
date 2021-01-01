package org.nanes.slideset.ui;

import org.nanes.slideset.SlideSet;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * {@code TreeCellRenderer} that will change the display style
 * of a locked table in the tree.
 * 
 * @author Benjamin Nanes
 */
public class SlideSetTreeRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree jtree, Object o, boolean bln, boolean bln1, boolean bln2, int i, boolean bln3) {
        
        super.getTreeCellRendererComponent(jtree, o, bln, bln1, bln2, i, bln3);
        
        if(o instanceof DefaultMutableTreeNode)
            if(((DefaultMutableTreeNode)o).getUserObject() instanceof SlideSet)
                if(((SlideSet)((DefaultMutableTreeNode)o).getUserObject()).isLocked()) {
                    setFont(jtree.getFont().deriveFont(Font.ITALIC));
                    return this;
                }
        
        setFont(null);
        return this;
        
    }
    
}
