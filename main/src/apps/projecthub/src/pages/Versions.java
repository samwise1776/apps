package pages;
import components.Ui; import model.AppStore;
@SuppressWarnings("serial")
public class Versions extends TablePage {
 public Versions(){super("Versions","Plan milestones and release with confidence.",new String[]{"Version","Project","Status","Target date"},"+ New version");}
 protected void refresh(){model.setRowCount(0);for(var x:store.versions())model.addRow(new Object[]{x.version(),x.project(),x.status(),x.date()});}
 protected void addItem(){String p=chooseProject();if(p==null)return;String v=Ui.ask(this,"Version (for example 1.2.0)");if(v==null||v.isBlank())return;String d=Ui.ask(this,"Target date (YYYY-MM-DD)");if(d!=null)store.addVersion(new AppStore.Version(v,p,"Planned",d));}
 protected void deleteItem(int r){store.removeVersion(r);}
}
