# Velice Language Specification

**Version:** 0.1.0 · **Status:** Draft · **Date:** 2026-08-14

Velice is a general-purpose, dynamic, multi-paradigm programming language with a
clean C-family syntax, first-class functions, classes, pattern matching, and a
small tree-walking interpreter written in pure Python.

---

## 1. Philosophy

Velice is designed for **clarity first**. The language prefers explicit,
readable constructs over cleverness; immutable-by-default bindings; and a
standard library that covers everyday needs without bloat. Code written in
Velice should read like a specification of what the program does.

### 1.1 Mission

Provide a pleasant, beginner-friendly yet expressive programming language that
is easy to embed, extend, and learn — with a pragmatic standard library and a
tiny, dependency-free toolchain.

### 1.2 Goals

- **Readability:** `let` over `var`, `fn` over `function`, `#` comments.
- **Immutability by default:** bindings are read-only unless marked `mut`.
- **Multi-paradigm:** procedural, functional, and object-oriented all first-class.
- **Errors as values + exceptions:** `??`, `try/catch`, and explicit `throw`.
- **Zero dependencies:** the interpreter runs on stock CPython ≥ 3.10.

### 1.3 Non-Goals

- System-level programming, raw pointers, manual memory management.
- Full static type inference and a sound type system (type *annotations* are
  accepted and stored; runtime remains dynamic).
- Native compilation in v0.1 (an ahead-of-time pipeline is planned).

### 1.4 Audience

Students learning programming, hobbyists scripting tools, and developers who
want an embeddable scripting language with a familiar syntax.

### 1.5 Versioning & Release Channels

- **Versioning:** semantic versioning — `MAJOR.MINOR.PATCH`.
- **Stable:** production-ready releases.
- **Beta:** feature-complete, in stabilization.
- **Nightly:** built from `main`.
- **LTS:** long-term support for legacy tooling.

---

## 2. Language Design Decisions

| Decision | Choice | Rationale |
| --- | --- | --- |
| Compilation | Interpreted (tree-walking) in v0.1 | Simple, dependency-free |
| Typing | Dynamic with optional annotations | Fast to write, easy to learn |
| Memory | Automatic GC (CPython) | Safety + simplicity |
| Bindings | Immutable by default | Fewer bugs |
| Concurrency | Async-style patterns, single thread | Deterministic |
| Error handling | Exceptions + nil-coalescing | Both styles supported |
| OOP | Classes with single inheritance + traits | Familiar, expressive |
| Functional | First-class functions, closures, pipes | Elegant pipelines |
| Modules | `import "file.velice"` | Transparent, file-based |

---

## 3. Lexical Structure

### 3.1 Source encoding

Source files are UTF-8. Identifiers may contain any letters, digits, and `_`,
and must not start with a digit.

### 3.2 Indentation

Indentation is **semantic** (like Python). Spaces and tabs both work; one tab
equals four spaces. Blocks may alternatively use explicit `{ ... }` braces,
which is the recommended style.

### 3.3 Comments

```velice
# line comment
/* block comment */
```

### 3.4 Recognized and reserved keywords

```
fn let mut var const class struct enum trait impl match if elif else while for
in loop break continue return defer yield async await spawn pub mod use from as
type where self super true false nil none some ok err panic try catch finally
throw assert unsafe extern inline is not and or guard new sizeof typeof import
extends with argu
```

Some keywords are reserved so current source remains compatible with planned
features. Version 0.1 reserves, but does not fully execute, `async`, `await`,
`spawn`, `yield`, `pub`, `mod`, `use`, `type`, `where`, `none`, `some`, `ok`,
`err`, `unsafe`, `extern`, `inline`, `is`, `guard`, and `new`.

### 3.5 Literals

- **Integers:** decimal, `0x` hex, `0b` binary, `0o` octal; `_` separators: `1_000_000`.
- **Floats:** `3.14`, `1e6`, `2.5e-3`.
- **Strings:** `"..."`, `'...'`, triple `"""..."""`, raw `r"..."` (no escapes).
- **Interpolation:** `"Hello ${name}!"` evaluates `name` at runtime.
- **Booleans:** `true`, `false`. **Nil:** `nil`.

### 3.6 Operators

| Category | Operators |
| --- | --- |
| Arithmetic | `+ - * / % **` |
| Comparison | `== != < <= > >=` |
| Logical | `and or not !` |
| Bitwise | `& \| ^` |
| Assignment | `= += -= *= /= %=` |
| Ranges | `a..b` (inclusive) |
| Other | `|>` pipe, `??` nil-coalescing, `?:` ternary, `.` access, `::` scope, `@` |

---

## 4. Grammar (EBNF)

```ebnf
program     := statement*
statement   := let_stmt | fn_decl | class_decl | struct_decl | enum_decl
             | trait_decl | impl_decl | if_stmt | while_stmt | for_stmt
             | loop_stmt | match_stmt | try_stmt | throw_stmt | assert_stmt
             | defer_stmt | import_stmt | return_stmt | expr_stmt
             | block
block       := '{' statement* '}' | indented_block
let_stmt    := 'let' 'mut'? IDENT ('=' expression)?
fn_decl     := 'fn' IDENT '(' params ')' ('->' type)? block
params      := (IDENT (',' IDENT)*)?
class_decl  := 'class' IDENT ('extends' IDENT)? '{' member* '}'
member      := fn_decl | let_stmt
if_stmt     := 'if' expression block ('elif' expression block)* ('else' block)?
match_stmt  := 'match' expression '{' match_arm (',' match_arm)* '}'
match_arm   := pattern ('if' expression)? '=>' expression
pattern     := literal | IDENT | '_' | '[' pattern* ']' | '(' pattern* ')' | pattern '|' pattern
expression  := pipe
pipe        := ternary ('|>' pipe)*
ternary     := or ('?' expression ':' ternary)?
or          := and ('or' and)*
and         := equality ('and' equality)*
equality    := comparison (('==' | '!=') comparison)*
comparison  := range (('<=' | '<' | '>=' | '>') range)*
range       := additive ('..' additive)?
additive    := multiplicative (('+' | '-') multiplicative)*
multiplicative := power (('*' | '/' | '%') power)*
power       := unary ('**' unary)?
unary       := ('-' | '!' | 'not') unary | postfix
postfix     := primary (call | '.' IDENT | index)*
primary     := literal | IDENT | 'self' | '(' expression ')' | list
             | map | lambda | if_expression | 'match' match_stmt
```

### 4.1 Precedence (low → high)

```
|>  ?:  ??  or  and  == != < <= > >=  & | ^  ..  + -  * / %  **  unary  . () []
```

---

## 5. Type System

### 5.1 Value types

| Type | Description | Example |
| --- | --- | --- |
| `int` | arbitrary-precision integer | `42` |
| `float` | IEEE-754 double | `3.14` |
| `string` | immutable UTF-8 text | `"hi"` |
| `bool` | `true` / `false` | `true` |
| `nil` | absence of value | `nil` |
| `array` | ordered, mutable list | `[1, 2, 3]` |
| `map` | string-keyed dictionary | `{"a": 1}` |
| `function` | callable closure | `fn(x) { x }` |
| `instance` | class instance | `Animal("x")` |

### 5.2 Type annotations

Optional: `let x: int = 5`, `fn f(a: string) -> bool`. Annotations are parsed
and retained in the AST but do not (yet) enforce static checking.

---

## 6. Variables

```velice
let x = 5             # immutable
let mut y = 5         # mutable
y = 6                 # ok
x = 6                 # runtime error: cannot assign to immutable binding
var v = 7             # `var` — shorthand for a mutable binding (`let mut`)
v = 8                 # ok
const MAX = 100       # constant
```

- `let` → immutable binding; `let mut` → reassignable; `const` → constant; `var` → reassignable.
- Reassigning an immutable binding raises a runtime error.

### Variable code (thunks)

A `var` whose initializer is a parenthesized expression holds deferred code:
the expression is not run at declaration time, but when the variable is
called.

```velice
var greet = (print("hello"))   # nothing prints yet
greet()                        # hello

var f = (5 * 4)
print(f())                     # 20

fn calc(var a, var b) { return a + b }   # `var` marks a mutable parameter
```

- `var f = (expr)` creates a zero-argument callable; `f()` runs `expr` and
  returns its value.
- Parameters may be marked `var` (or `mut`) to make them reassignable inside
  the function body.

---

## 7. Operators (reference)

See §3.6. Notable behaviors:

- `+` on arrays concatenates (`[1] + [2]` → `[1, 2]`).
- `+` on strings concatenates; string repetition via `"ab" * 3` → `"ababab"`.
- `a..b` builds the inclusive range `[a, b]`.
- `x ?? fallback` returns `x` unless `x` is `nil`, then `fallback`.
- `value |> fn` passes `value` as the first argument to `fn`.

---

## 8. Control Flow

```velice
if x > 0 { print("positive") }
elif x == 0 { print("zero") }
else { print("negative") }

# if-expression (value)
let label = if x > 0 { "pos" } else { "neg" }

# while
while n < 10 { n = n + 1 }

# for over iterable
for item in items { print(item) }
for i in 0..5 { print(i) }

# infinite loop
loop { if done { break } }

# match
match code {
    0 => "ok",
    n if n > 100 => "error",
    _ => "unknown",
}
```

- `break` / `continue` valid inside `loop`, `while`, and `for`.
- `match` is both a statement and an expression (value of matched arm).

---

## 9. Functions

```velice
fn add(a, b) -> int { return a + b }
fn greet(name, greeting = "Hello") { ... }   # default args
fn counter() {
    let mut c = 0
    return fn() { c = c + 1; c }             # closure
}
```

- Functions are first-class values and close over their defining scope.
- Recursion is fully supported.
- `return` with no value returns `nil`.

---

## 10. Object-Oriented Programming

```velice
class Animal {
    fn init(name) { self.name = name }
    fn speak() { print(self.name + " makes a sound") }
}

class Dog extends Animal {
    fn speak() { print(self.name + " barks!") }
}

let d = Dog("Rex")
d.speak()              # → Rex barks!
```

- `init` is the constructor; `self` refers to the instance.
- Single inheritance via `extends`; methods resolve up the chain.
- `trait` declares interface contracts; `struct` declares data records.
- `enum` declares named variants.

---

## 11. Functional Programming

```velice
fn compose(f, g) { return fn(x) { f(g(x)) } }
fn add1(x) { return x + 1 }
let inc_twice = compose(add1, add1)
print(inc_twice(5))    # → 7

print([1,2,3] |> map |> len)      # pipes
let double = map([1,2,3], fn(x) { x * 2 })
let evens  = filter([1,2,3,4], fn(x) { x % 2 == 0 })
```

---

## 12. Memory & Ownership

- Values are garbage-collected; there is **no manual memory management**.
- Arrays and maps are mutable by reference; strings and numbers by value.
- No pointers, no `unsafe`, no dangling-reference category of bugs.

---

## 13. Concurrency

- Single-threaded event-loop style; `sleep(sec)` yields.
- Parallel-friendly helpers (`map`, `filter`, `sum`) for data pipelines.
- `async` / `await` are reserved for a future cooperative model.

---

## 14. Error Handling

```velice
try {
    risky()
} catch e {
    print("caught: " + e)
} finally {
    cleanup()
}

throw "something went wrong"
```

- `throw` accepts any value (usually a string).
- `catch e` binds the error; `finally` always runs.
- `??` provides nil-safe defaults; `assert(cond, "msg")` for invariants.
- `defer` schedules cleanup to run when the enclosing block exits (best-effort).

---

## 15. Reflection & Introspection

```velice
typeof(x)    # → "int", "string", "array", "map", "function", "class", ...
sizeof(x)    # → element count (len of strings/arrays/maps)
```

- Values can be inspected with `keys(m)`, `values(m)`, `items(m)`.

---

## 16. Modules

```velice
import "math.velice"
from "math.velice" import square
```

- Imports load and execute another `.velice` file in a fresh global scope.
- Imported globals become available through the importing module's globals
  (single-file implementation in v0.1; namespaces planned).

---

## 17. Standard Library

| Area | Functions |
| --- | --- |
| IO | `print`, `println`, `input`, `readFileLines` |
| Collections | `len`, `range`, `sorted`, `reversed`, `append`, `push`, `pop`, `insert`, `remove`, `keys`, `values`, `items`, `enumerate`, `zip` |
| Higher-order | `map`, `filter`, `reduce` |
| Numeric | `min`, `max`, `sum`, `abs`, `sqrt`, `pow`, `floor`, `ceil`, `round`, `rand`, `rand_int`, `random` |
| Text & conversion | `str`, `int`, `float`, `bool`, `chr`, `ord`, `hex`, `oct`, `bin`, `join`, `split`, `replace`, `trim`, `lower`, `upper`, `parse_int`, `parse_float` |
| JSON | `json_parse`, `json_stringify` (aliases: `from_json`, `to_json`) |
| Crypto | `sha256` |
| Time | `now`, `time`, `sleep` |
| Misc | `typeof`, `sizeof`, `panic`, `exit` |

### 17.1 GUI (`import gui`)

`import gui` injects the native (Tk-backed) GUI library into scope. It supports
an imperative API and a declarative DSL:

```velice
# imperative
import gui
window = Window("App", 800, 600)
button = Button("Click Me")
button.on_click { print("Hello from Velice") }
window.add(button)
gui.run()

# declarative DSL
import gui
window Main {
    Button go {
        text = "Go"
        onClick { print("Go!") }
    }
}
run Main
```

- **Widgets**: `Window`, `Frame`, `ScrollPane`, `Button`, `Label`,
  `TextField`,
  `PasswordField`, `TextArea`, `Checkbox`, `RadioButton`, `ToggleSwitch`,
  `Slider`, `Spinner`, `ProgressBar`, `ComboBox`, `ListBox`, `Table`, `Image`,
  `Hyperlink`, `MenuBar`/`Menu`/`MenuItem`/`MenuSeparator`,
  `TabView`/`Tab`, `StatusBar`, `Canvas`.
  `ScrollPane` is a scrollable container (`ScrollPane(640, 400)`,
  `.add(child)`, `.scroll_to_top()`/`.scroll_to_bottom()`/`.scroll_to(x, y)`).
- **Events**: `widget.on_click { ... }` (or `onClick`); `event` is a map with
  `x`, `y`, `key`, `state`. See `docs/GUI.md` for the full event list.
- **Dialogs**: `alert`, `toast`, `confirm`, `input_box`, `open_file`,
  `save_file`, `choose_folder`, `choose_color`, `choose_font`,
  `FileChooser(title, mode, start_dir)` (mode: `open` | `save` | `folder`,
  returns the chosen path or `nil`), plus
  `gui.clipboard`.
- **Runtime**: `gui.run()`, `gui.update()`, `gui.set_theme(name)`,
  `gui.window(name)`, `gui.quit()`.
- **Headless**: without a display (or with `VELICE_GUI=none`) the runtime
  records calls instead of opening windows, so GUI programs still run.

Full reference: `docs/GUI.md`.

### 17.2 Command-line arguments (`argu`)

`argu` gives access to the arguments passed to the script.

```velice
file = argu.0        # first argument
print(argu.1)        # second argument
print(argu.len)      # number of arguments
print(argu)          # array of all arguments
for x in argu { ... }# iterate over arguments
```

- `velice run app.velice in.txt out.txt` → `argu.0` is `in.txt`, `argu.1` is `out.txt`.
- Index out of range is a runtime error.

### 17.3 String methods

```velice
"hello".contains("ell")        # true
"hello".starts("he")           # true
"hello".ends("lo")             # true
"print(\"Hello\")".inbetween("\"\"")  # "Hello"
"foo(bar)baz".inbetween("()")  # "bar"
```

- `str.contains(x)` → `true`/`false`
- `str.starts(x)`, `str.ends(x)` → `true`/`false`
- `str.inbetween(delims)` returns the text between the first and last
  delimiter characters; `""` when the delimiters are not found.
- `str.len` → character count.

### 17.4 Reading files (`readFileLines`)

```velice
lines = readFileLines("code.txt")
while lines.hasMore() {
    line = lines.currentLine()     # next line (nil when exhausted)
    if line.contains("print(") { parse(line) }
}
```

- `readFileLines(path)` returns a line reader over the file's lines
  (trailing newlines stripped, indentation preserved).
- `lines.contains(x)` → `true` if any line contains the substring `x`.
- `lines.currentLine()` returns the next line and advances the cursor.
- `lines.current()` returns the current line without advancing.
- `lines.hasMore()` → `true` while lines remain.
- `lines.reset()` rewinds to the start; `lines.position()` is the cursor.
- `lines.len` / `lines[i]` / `for x in lines` work like an array.
- Missing file is a runtime error.

### 17.5 Running commands (`import cmd`)

`import cmd` lets Velice run terminal commands and stream their output to the
console or capture it in a variable.

```velice
import cmd
cmd.write("echo Hello")           # standalone: prints: Hello
a = cmd.write("ls -la")           # captured: no printing, `a` holds the output
print(a)                          # prints the listing
files = cmd.capture("ls /home/${User.name}")   # always silent
```

- `cmd.write(command)` runs `command` through the shell.
  - Standalone as a statement, it prints the command's standard output to the
    console (and standard error to stderr).
  - When its result is used (assigned to a variable, passed as an argument,
    ...) it returns the output as a string instead of printing.
- `cmd.capture(command)` always runs `command` and returns its output as a
  string silently.
- `command` may be any string; the exact string is passed to the shell. Use
  string interpolation (e.g. `"ls /home/${User.name}"`) to build it.

### 17.6 Current user (`User`)

`User` is a built-in namespace describing the operating-system user running
the script. It works anywhere — including inside string interpolation:

```velice
print("Hello, ${User.name}!")   # e.g. Hello, ray!
print("in " + User.home)        # /home/ray
```

| Property | Meaning |
| --- | --- |
| `User.name` | login name (e.g. `ray`) |
| `User.home` | home directory |
| `User.id` | numeric user id |
| `User.gid` | numeric primary group id |
| `User.group` | primary group name |
| `User.shell` | login shell |
| `User.full` | full (display) name |

## 18. Toolchain & Runtime

| Tool | Purpose |
| --- | --- |
| `velice run file.velice` | Execute a script |
| `velice repl` | Interactive read–eval–print loop |
| `velice eval "expr"` | Evaluate one expression |
| `velice version` | Print version info |
| `python3 -m velice` | Same tools via module invocation |

Run via the Python package (`velice/`), which is `pip install -e .`-able with a
`velice` console script defined in `setup.py`.

---

## 19. Platform Support

- **Runtime:** CPython 3.10+ on Linux, macOS, Windows.
- **Editor:** VS Code extension (`editor/vscode/`) with highlighting, file
  icons, snippets, and run commands.

---

## 20. Examples

Complete runnable examples live in `examples/`:

```
hello_world.velice        # classic first program
variables.velice          # bindings, const, types
functions.velice          # defaults, closures, recursion
loops.velice              # for/while/loop, break/continue
arrays_collections.velice # arrays, maps, collection ops
classes.velice            # classes, inheritance, methods
inheritance.velice        # extends, traits, structs
generics.velice           # generic-style patterns
match.velice              # pattern matching
error_handling.velice     # try/catch/finally, ??
async.velice              # async-style patterns
functional.velice         # compose, pipes, currying
```

---

## 21. Documentation Plan

- `README.md` — quick start.
- `docs/SPEC.md` — this document.
- `examples/` — runnable demonstrations.
- Inline doc comments (`///`) reserved for a future doc generator.

---

## 22. Testing Strategy

- Unit tests in `tests/test_all.py` (lexer, parser, interpreter).
- Example programs double as integration tests (run, assert no error).

---

## 23. Future Work

- Bytecode compiler + VM for performance.
- Static type checking with the annotation system.
- Real module namespaces and a package registry.
- Cooperative async/await; threading.
- Native AOT compilation pipeline.
- Language server protocol (LSP) implementation.

---

## 24. License

MIT.
