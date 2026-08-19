package pages;
import components.Ui; import javax.swing.*; import model.AppStore;
@SuppressWarnings("serial")
public class Bugs extends TablePage {
 public Bugs(){super("Bugs","Triage issues before they surprise you.",new String[]{"Issue","Project","Severity","Status"},"+ Report bug");table.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2)toggle(table.getSelectedRow());}});}
 protected void refresh(){model.setRowCount(0);for(var x:store.bugs())model.addRow(new Object[]{x.title(),x.project(),x.severity(),x.status()});}
 protected void addItem(){String p=chooseProject();if(p==null)return;String t=Ui.ask(this,"Bug summary");if(t==null||t.isBlank())return;String[] ss={"Low","Medium","High","Critical"};String s=(String)JOptionPane.showInputDialog(this,"Severity","Report bug",JOptionPane.PLAIN_MESSAGE,null,ss,ss[1]);if(s!=null)store.addBug(new AppStore.Bug(t,p,s,"Open"));}
 private void toggle(int r){if(r<0)return;var x=store.bugs().get(r);store.updateBug(r,new AppStore.Bug(x.title(),x.project(),x.severity(),x.status().equals("Open")?"Resolved":"Open"));}
 protected void deleteItem(int r){store.removeBug(r);}
}
