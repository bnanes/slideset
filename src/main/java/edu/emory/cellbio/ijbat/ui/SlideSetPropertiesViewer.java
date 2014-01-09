package edu.emory.cellbio.ijbat.ui;

import edu.emory.cellbio.ijbat.SlideSet;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * View the properties of a {@code SlideSet} table
 * 
 * @author Benjamin Nanes
 */
public class SlideSetPropertiesViewer extends JDialog implements ActionListener {
    
    // Fields
    
    private final SlideSet table;
    private JTextArea textArea;
    
    // Constructor
    
    SlideSetPropertiesViewer(Frame parent, SlideSet table) {
        super(parent, "Table Properties", true);
        this.table = table;
        buildLayout();
        setLocationRelativeTo(parent);
        populateText();
    }
    
    // Methods
    
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals("done")) {
            setVisible(false);
            dispose();
        }
    }
    
    // Helper methods
    
    private void buildLayout() {
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(325, 260));
        textArea = new JTextArea();
        JScrollPane sp = new JScrollPane(textArea);
        add(sp);
        textArea.setEditable(false);
        textArea.setMargin(new Insets(5,5,5,5));
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        textArea.setText("Hello world!");
        add(Box.createVerticalStrut(5));
        JButton done = new JButton("OK");
        done.setActionCommand("done");
        done.addActionListener(this);
        Box row = Box.createHorizontalBox();
        row.add(Box.createHorizontalGlue());
        row.add(done);
        row.add(Box.createHorizontalGlue());
        add(row);
        add(Box.createVerticalStrut(5));
        pack();
    }
    
    private void populateText() {
        textArea.setText(table.getName() + "\n");
        for(int i=0; i<table.getName().length(); i++)
            textArea.append("-");
        textArea.append("\nParent table:  ");
        textArea.append(table.getParent() == null
                ? "(none)\n" 
                : table.getParent().getName() + "\n");
        LinkedHashMap<String, String> props = table.getCreationParams();
        if(props.isEmpty())
            textArea.append("(no creation parameters)\n");
        for(String key : props.keySet()) {
            textArea.append(key + ":  ");
            textArea.append(props.get(key) + "\n");
        }
        textArea.setCaretPosition(0);
    }
    
}
