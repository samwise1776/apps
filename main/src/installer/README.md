# Datacenter AppCenter

AppCenter installs, prepares, launches, updates, and removes Datacenter's public
products without asking users to write code or use a terminal.

The native AppCenter package includes its own private Java runtime and compiler.
Users do not need to download, configure, or maintain Java. Java applications
are compiled automatically after their signed package checksum is verified.

Trestrio dependency restoration and ScrapZone builds also run inside AppCenter.
Those products still require their platform runtimes—Node.js 18+ and the .NET 10
SDK respectively—and AppCenter reports a clear message when one is unavailable.

Velice is available as a first-class AppCenter product. AppCenter verifies the
source archive, checks Python 3.10+, prepares the interpreter, runs a smoke test,
and provides a direct button to the complete 24-chapter online guide.

Build the native application with:

```bash
./installer/build-native.sh
```

The native application is written to `build/appcenter-native/AppCenter`.
