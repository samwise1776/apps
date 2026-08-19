package model;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;

/** Small dependency-free data store shared by every page. */
public final class AppStore {
    private static final int FILE_MAGIC = 0x50485542; // PHUB
    private static final int FILE_VERSION = 1;
    public record Project(String name, String description, String status, int progress) implements Serializable {}
    public record Task(String title, String project, String priority, String status, String due) implements Serializable {}
    public record Bug(String title, String project, String severity, String status) implements Serializable {}
    public record Version(String version, String project, String status, String date) implements Serializable {}

    private static final AppStore INSTANCE = new AppStore();
    private final List<Project> projects = new ArrayList<>();
    private final List<Task> tasks = new ArrayList<>();
    private final List<Bug> bugs = new ArrayList<>();
    private final List<Version> versions = new ArrayList<>();
    private final List<Runnable> listeners = new ArrayList<>();
    private final Path file = Path.of(System.getProperty("user.home"), ".projecthub", "data.ser");

    public static AppStore get() { return INSTANCE; }
    private AppStore() { load(); }
    public List<Project> projects() { return Collections.unmodifiableList(projects); }
    public List<Task> tasks() { return Collections.unmodifiableList(tasks); }
    public List<Bug> bugs() { return Collections.unmodifiableList(bugs); }
    public List<Version> versions() { return Collections.unmodifiableList(versions); }
    public void listen(Runnable listener) { listeners.add(listener); }

    public void addProject(Project value) { projects.add(value); changed(); }
    public void addTask(Task value) { tasks.add(value); changed(); }
    public void addBug(Bug value) { bugs.add(value); changed(); }
    public void addVersion(Version value) { versions.add(value); changed(); }
    public void removeProject(int i) { if (valid(i, projects)) { String name=projects.remove(i).name(); tasks.removeIf(x->x.project().equals(name)); bugs.removeIf(x->x.project().equals(name)); versions.removeIf(x->x.project().equals(name)); changed(); } }
    public void removeTask(int i) { if (valid(i, tasks)) { tasks.remove(i); changed(); } }
    public void removeBug(int i) { if (valid(i, bugs)) { bugs.remove(i); changed(); } }
    public void removeVersion(int i) { if (valid(i, versions)) { versions.remove(i); changed(); } }
    public void updateTask(int i, Task value) { if (valid(i, tasks)) { tasks.set(i, value); changed(); } }
    public void updateBug(int i, Bug value) { if (valid(i, bugs)) { bugs.set(i, value); changed(); } }
    private boolean valid(int i, List<?> list) { return i >= 0 && i < list.size(); }

    public void seedDemo() {
        if (!projects.isEmpty()) return;
        projects.add(new Project("ProjectHub", "Desktop project planning workspace", "Active", 65));
        tasks.add(new Task("Finish dashboard", "ProjectHub", "High", "In progress", LocalDate.now().plusDays(3).toString()));
        bugs.add(new Bug("Empty state alignment", "ProjectHub", "Low", "Open"));
        versions.add(new Version("1.0.0", "ProjectHub", "Planned", LocalDate.now().plusWeeks(2).toString()));
        changed();
    }

    public void clear() { projects.clear(); tasks.clear(); bugs.clear(); versions.clear(); changed(); }
    private void changed() { save(); List.copyOf(listeners).forEach(Runnable::run); }
    private void save() {
        try {
            Files.createDirectories(file.getParent());
            Path temporary = Files.createTempFile(file.getParent(), "data-", ".tmp");
            try {
                try (var out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
                    out.writeInt(FILE_MAGIC); out.writeInt(FILE_VERSION);
                    out.writeInt(projects.size()); for (Project x : projects) { text(out,x.name()); text(out,x.description()); text(out,x.status()); out.writeInt(x.progress()); }
                    out.writeInt(tasks.size()); for (Task x : tasks) { text(out,x.title()); text(out,x.project()); text(out,x.priority()); text(out,x.status()); text(out,x.due()); }
                    out.writeInt(bugs.size()); for (Bug x : bugs) { text(out,x.title()); text(out,x.project()); text(out,x.severity()); text(out,x.status()); }
                    out.writeInt(versions.size()); for (Version x : versions) { text(out,x.version()); text(out,x.project()); text(out,x.status()); text(out,x.date()); }
                }
                try { Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
                catch (AtomicMoveNotSupportedException e) { Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        }
        catch (IOException e) { System.err.println("Could not save ProjectHub data: " + e.getMessage()); }
    }
    private static void text(DataOutputStream out, String value) throws IOException { out.writeUTF(value == null ? "" : value); }
    private static String text(DataInputStream in) throws IOException { return in.readUTF(); }
    private static int count(DataInputStream in) throws IOException { int value=in.readInt(); if (value < 0 || value > 1_000_000) throw new IOException("Invalid item count"); return value; }
    @SuppressWarnings("unchecked") private void load() {
        if (!Files.exists(file)) return;
        try (var raw = new BufferedInputStream(Files.newInputStream(file))) {
            raw.mark(8); var data = new DataInputStream(raw);
            if (data.readInt() == FILE_MAGIC) {
                if (data.readInt() != FILE_VERSION) throw new IOException("Unsupported ProjectHub data version");
                for(int i=count(data);i>0;i--) projects.add(new Project(text(data),text(data),text(data),data.readInt()));
                for(int i=count(data);i>0;i--) tasks.add(new Task(text(data),text(data),text(data),text(data),text(data)));
                for(int i=count(data);i>0;i--) bugs.add(new Bug(text(data),text(data),text(data),text(data)));
                for(int i=count(data);i>0;i--) versions.add(new Version(text(data),text(data),text(data),text(data)));
            } else {
                raw.reset();
                try (var legacy = new ObjectInputStream(raw)) {
                    legacy.setObjectInputFilter(info -> {
                        Class<?> type=info.serialClass();
                        if (info.depth()>12 || info.references()>100_000 || info.arrayLength()>1_000_000) return ObjectInputFilter.Status.REJECTED;
                        if (type==null) return ObjectInputFilter.Status.UNDECIDED;
                        if (type.isArray() && !type.getComponentType().isPrimitive()) type=type.getComponentType();
                        String name=type.getName();
                        return name.equals("java.util.ArrayList") || name.equals("java.lang.Object") || name.equals("java.lang.String") || name.startsWith("model.AppStore$")
                            ? ObjectInputFilter.Status.ALLOWED : ObjectInputFilter.Status.REJECTED;
                    });
                    projects.addAll((List<Project>)legacy.readObject()); tasks.addAll((List<Task>)legacy.readObject()); bugs.addAll((List<Bug>)legacy.readObject()); versions.addAll((List<Version>)legacy.readObject());
                }
                save(); // one-time migration from the restricted legacy format
            }
        }
        catch (IOException | ClassNotFoundException e) { System.err.println("Could not load ProjectHub data: " + e.getMessage()); }
    }
}
