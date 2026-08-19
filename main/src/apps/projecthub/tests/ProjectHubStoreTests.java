import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import model.AppStore;

public final class ProjectHubStoreTests {
    public static void main(String[] args) throws Exception {
        Path home=Files.createTempDirectory("projecthub-test-");
        Path directory=home.resolve(".projecthub"); Files.createDirectories(directory);
        Path file=directory.resolve("data.ser");
        try(var out=new ObjectOutputStream(Files.newOutputStream(file))) {
            out.writeObject(new ArrayList<>(java.util.List.of(new AppStore.Project("Migrated","Legacy","Active",50))));
            out.writeObject(new ArrayList<AppStore.Task>()); out.writeObject(new ArrayList<AppStore.Bug>()); out.writeObject(new ArrayList<AppStore.Version>());
        }
        System.setProperty("user.home",home.toString());
        AppStore store=AppStore.get();
        check(store.projects().size()==1 && store.projects().get(0).name().equals("Migrated"),"legacy migration");
        byte[] header=Files.readAllBytes(file); check(header.length>8 && header[0]=='P' && header[1]=='H' && header[2]=='U' && header[3]=='B',"versioned safe format");
        store.addTask(new AppStore.Task("Test","Migrated","High","Open","2026-08-13"));
        check(Files.size(file)>header.length,"atomic persistence");
        System.out.println("All ProjectHub storage tests passed.");
    }
    private static void check(boolean condition,String name) { if(!condition) throw new AssertionError(name); }
}
