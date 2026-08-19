# App 3 — Java Game Starter

**Status:** Placeholder; compiles but is not runnable yet  
**Download:** [app-3.zip](app-3.zip)  
**Stack:** Java

[← All apps](README.md)

App 3 is the smallest Datacenter snapshot: a clean `apps.Game` class ready for game logic, rendering code, or a framework integration.

## Current source

```java
package apps;

public class Game {
}
```

## Build

```bash
unzip app-3.zip
mkdir -p build
javac -d build 3/Game.java
```

## Make it runnable

Add an entry point inside `Game`:

```java
public static void main(String[] args) {
    System.out.println("Game started");
}
```

Then compile and run:

```bash
javac -d build 3/Game.java
java -cp build apps.Game
```

## Suggested next steps

1. Choose Swing, JavaFX, LWJGL, or a console interface.
2. Add a game loop and explicit shutdown behavior.
3. Separate update logic from rendering.
4. Add a small automated test for non-graphical game rules.
