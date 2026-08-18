# Vexa

Vexa is an experimental interpreted language. Files use the `.vexa` extension.

Implemented syntax:

```vexa
text greeting = "Hello"
print(greeting)
print("literal text")
```

Currently supported: quoted text variables, identifiers, comments beginning with `#`, and printing literals or variables. Numbers, operators, functions, and other types are not implemented. Invalid syntax reports a line number and exits unsuccessfully.

Build and run:

```bash
./scripts/build/vexa.sh
```
