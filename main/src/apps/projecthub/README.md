# ProjectHub

A dependency-free Java Swing desktop workspace for tracking projects, tasks, bugs, and releases.

## Run

Requires Java 17 or newer.

```bash
mkdir -p out
javac -d out $(find src -name '*.java')
java -cp out App
```

ProjectHub saves automatically to `~/.projecthub/data.ser`. Double-click a task to advance its status, or a bug to toggle it between open and resolved. Demo data and the clear-data action are available under Settings.
