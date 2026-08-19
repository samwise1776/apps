package pages;
import components.Ui; import javax.swing.*; import model.AppStore;
@SuppressWarnings("serial")
public class Tasks extends TablePage {
 public Tasks(){super("Tasks","Keep work visible and move it toward done.",new String[]{"Task","Project","Priority","Status","Due"},"+ New task"); table.addMouseListener(new java.awt.event.MouseAdapter(){public void mouseClicked(java.awt.event.MouseEvent e){if(e.getClickCount()==2)toggle(table.getSelectedRow());}});}
 protected void refresh(){model.setRowCount(0);for(var x:store.tasks())model.addRow(new Object[]{x.title(),x.project(),x.priority(),x.status(),x.due()});}
 protected void addItem(){String p=chooseProject();if(p==null)return;String t=Ui.ask(this,"Task title");if(t==null||t.isBlank())return;String[] ps={"Low","Medium","High"};String pr=(String)JOptionPane.showInputDialog(this,"Priority","New task",JOptionPane.PLAIN_MESSAGE,null,ps,ps[1]);String due=Ui.ask(this,"Due date (YYYY-MM-DD)");if(pr!=null&&due!=null)store.addTask(new AppStore.Task(t,p,pr,"To do",due));}
 private void toggle(int r){if(r<0)return;var x=store.tasks().get(r);String next=x.status().equals("To do")?"In progress":x.status().equals("In progress")?"Done":"To do";store.updateTask(r,new AppStore.Task(x.title(),x.project(),x.priority(),next,x.due()));}
 protected void deleteItem(int r){store.removeTask(r);}
}
