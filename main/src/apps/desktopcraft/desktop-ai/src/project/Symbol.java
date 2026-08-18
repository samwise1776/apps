package project;

/** A declared symbol (class, method, field, import) found in source. */
public final class Symbol {
    public enum Kind { CLASS, INTERFACE, ENUM, RECORD, METHOD, CONSTRUCTOR, FIELD, IMPORT, PACKAGE }

    private final Kind kind;
    private final String name;
    private final String file;
    private final int line;
    private final int column;
    private int endLine;
    private final String modifiers;
    private final String signature;
    private final String parentClass;

    public Symbol(Kind kind, String name, String file, int line, int column, int endLine,
                  String modifiers, String signature, String parentClass) {
        this.kind = kind;
        this.name = name;
        this.file = file;
        this.line = line;
        this.column = column;
        this.endLine = endLine;
        this.modifiers = modifiers == null ? "" : modifiers;
        this.signature = signature == null ? "" : signature;
        this.parentClass = parentClass;
    }

    public Kind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public String file() {
        return file;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int endLine() {
        return endLine;
    }

    /** Extends the symbol to end on the given line. Used while parsing bodies. */
    void endLine(int line) {
        this.endLine = line;
    }

    public String modifiers() {
        return modifiers;
    }

    public String signature() {
        return signature;
    }

    public String parentClass() {
        return parentClass;
    }

    public boolean isType() {
        return kind == Kind.CLASS || kind == Kind.INTERFACE || kind == Kind.ENUM || kind == Kind.RECORD;
    }

    @Override
    public String toString() {
        return kind + " " + name + " @ " + file + ":" + line;
    }
}
