# Datacenter private application IDs

Each application directory contains a hidden `.id.txt` file. Raw ID values are
private and must not be printed, logged, committed, or returned to applications.
They are also excluded from complete ZIP/JAR release snapshots. When a restored
workspace retains an application ID directory but has no `.id.txt`, the broker
creates a new random owner-only ID before accepting clients.

`IdSocketServer` loads and validates the IDs, restricts the ID tree and Unix
socket to the current operating-system owner, and issues random two-minute
session tokens. `IdSocketClient` resolves a token to a normalized application
key without receiving the raw stored ID.

Start the broker from a terminal:

```bash
/home/ray/Data/ids/start-id-socket.sh
```

Run every active build, identity, and permission regression check with:

```bash
/home/ray/Data/scripts/checker.sh
```

Compile and run its regression tests:

```bash
build_dir="$(mktemp -d)"
javac -Xlint:all -d "$build_dir" \
  /home/ray/Data/ids/IdSocketServer.java \
  /home/ray/Data/ids/IdSocketClient.java \
  /home/ray/Data/ids/IdSocketTest.java
java -ea -cp "$build_dir" ids.IdSocketTest
```

The permission checker also provides `checkToken(...)` and `requireToken(...)`.
These methods resolve the application through the socket and fail closed when
the broker is offline or the token is invalid.

The Unix socket protects against other operating-system users. It does not
isolate mutually untrusted processes running as the same user. Stronger
same-user isolation requires separate OS accounts, containers, or a sandbox.
