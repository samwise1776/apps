package pages;
import components.Ui; import javax.swing.*; import model.AppStore;
@SuppressWarnings("serial")
public class Projects extends TablePage {
 public Projects(){super("Projects","Create and track everything you are building.",new String[]{"Name","Description","Status","Progress"},"+ New project");}
 protected void refresh(){model.setRowCount(0); for(var p:store.projects())model.addRow(new Object[]{p.name(),p.description(),p.status(),p.progress()+"%"});}
 protected void addItem(){String n=Ui.ask(this,"Project name");if(n==null||n.isBlank())return;String d=Ui.ask(this,"Short description");if(d==null)return;String[] s={"Planning","Active","On hold","Complete"};String st=(String)JOptionPane.showInputDialog(this,"Status","New project",JOptionPane.PLAIN_MESSAGE,null,s,s[1]);if(st!=null)store.addProject(new AppStore.Project(n,d,st,st.equals("Complete")?100:0));}
 protected void deleteItem(int r){store.removeProject(r);}
}
