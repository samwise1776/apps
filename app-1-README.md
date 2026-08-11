# App 1 — Datacenter App Console

Archive: [`app-1.zip`](app-1.zip)

App 1 contains `Info.java`, a Java Swing desktop console for inspecting, compiling, repairing, logging, and launching applications in the Datacenter workspace.

## Requirements

- Java Development Kit
- Graphical desktop session
- A Datacenter folder at `~/Data`

## Build and run

```bash
unzip app-1.zip
javac -d build 1/Info.java
java -cp build apps.Info
```
