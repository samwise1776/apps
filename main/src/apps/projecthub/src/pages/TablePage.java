package pages;

import components.Ui;
import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import model.AppStore;

@SuppressWarnings("serial")
abstract class TablePage extends JPanel {
    protected final AppStore store=AppStore.get();
    protected final DefaultTableModel model;
    protected final JTable table;
    protected TablePage(String title,String subtitle,String[] columns,String addLabel) {
        setLayout(new BorderLayout(0,20)); setBackground(Ui.BG); setBorder(BorderFactory.createEmptyBorder(28,30,28,30));
        JPanel top=new JPanel(new BorderLayout()); top.setOpaque(false); top.add(Ui.header(title,subtitle)); JButton add=Ui.button(addLabel); add.addActionListener(e->addItem()); top.add(add,BorderLayout.EAST); add(top,BorderLayout.NORTH);
        model=new DefaultTableModel(columns,0){ public boolean isCellEditable(int r,int c){return false;} };
        table=new JTable(model); Ui.styleTable(table); JScrollPane scroll=new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(new Color(226,228,236))); scroll.getViewport().setBackground(Color.WHITE); add(scroll);
        JButton remove=Ui.danger("Delete selected"); remove.addActionListener(e->removeSelected()); JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT,0,0)); bottom.setOpaque(false); bottom.add(remove); add(bottom,BorderLayout.SOUTH);
        store.listen(this::refresh); refresh();
    }
    protected abstract void refresh(); protected abstract void addItem(); protected abstract void deleteItem(int row);
    private void removeSelected(){ int row=table.getSelectedRow(); if(row<0){JOptionPane.showMessageDialog(this,"Select a row first.");return;} if(JOptionPane.showConfirmDialog(this,"Delete the selected item?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION) deleteItem(row); }
    protected void rows(Object[]... rows){ model.setRowCount(0); for(Object[] row:rows) model.addRow(row); }
    protected String chooseProject(){ if(store.projects().isEmpty()){JOptionPane.showMessageDialog(this,"Create a project first.");return null;} Object[] names=store.projects().stream().map(AppStore.Project::name).toArray(); Object v=JOptionPane.showInputDialog(this,"Project","Choose project",JOptionPane.PLAIN_MESSAGE,null,names,names[0]); return v==null?null:v.toString(); }
}
