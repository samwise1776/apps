package project;

import java.util.ArrayList;
import java.util.List;

/**
 * Captures the code context around a cursor: the enclosing class/method, nearby
 * lines, and the symbols visible at that location. Used by completion and AI
 * prompting.
 */
public final class CodeContext {
    private final String file;
    private final int cursorLine;
    private final int cursorColumn;
    private final List<String> lines;
    private final Symbol enclosingClass;
    private final Symbol enclosingMethod;

    public CodeContext(String file, int cursorLine, int cursorColumn, List<String> lines,
                       Symbol enclosingClass, Symbol enclosingMethod) {
        this.file = file;
        this.cursorLine = cursorLine;
        this.cursorColumn = cursorColumn;
        this.lines = lines;
        this.enclosingClass = enclosingClass;
        this.enclosingMethod = enclosingMethod;
    }

    public static CodeContext from(ParsedFile parsed, int cursorLine, int cursorColumn) {
        Symbol enclosingClass = null;
        Symbol enclosingMethod = null;
        for (Symbol symbol : parsed.symbols()) {
            if (symbol.isType() && symbol.line() <= cursorLine + 1 && cursorLine + 1 <= symbol.endLine()) {
                if (enclosingClass == null || symbol.line() > enclosingClass.line()) {
                    enclosingClass = symbol;
                }
            }
        }
        for (Symbol symbol : parsed.symbols()) {
            if ((symbol.kind() == Symbol.Kind.METHOD || symbol.kind() == Symbol.Kind.CONSTRUCTOR)
                    && symbol.line() <= cursorLine + 1 && cursorLine + 1 <= symbol.endLine()) {
                if (enclosingMethod == null || symbol.line() > enclosingMethod.line()) {
                    enclosingMethod = symbol;
                }
            }
        }
        return new CodeContext(parsed.path(), cursorLine, cursorColumn, parsed.lines(),
                enclosingClass, enclosingMethod);
    }

    public String file() {
        return file;
    }

    public int cursorLine() {
        return cursorLine;
    }

    public int cursorColumn() {
        return cursorColumn;
    }

    public Symbol enclosingClass() {
        return enclosingClass;
    }

    public Symbol enclosingMethod() {
        return enclosingMethod;
    }

    /** Lines before and after the cursor (default 10 each side). */
    public List<String> surroundingLines(int radius) {
        List<String> result = new ArrayList<>();
        int start = Math.max(0, cursorLine - radius);
        int end = Math.min(lines.size(), cursorLine + radius + 1);
        for (int i = start; i < end; i++) {
            result.add((i + 1) + ": " + lines.get(i));
        }
        return result;
    }

    public String describe() {
        StringBuilder out = new StringBuilder();
        out.append("File: ").append(file).append('\n');
        out.append("Cursor: line ").append(cursorLine + 1).append(", column ").append(cursorColumn + 1).append('\n');
        if (enclosingClass != null) {
            out.append("In class: ").append(enclosingClass.name()).append(" (").append(enclosingClass.kind()).append(")\n");
        }
        if (enclosingMethod != null) {
            out.append("In method: ").append(enclosingMethod.signature()).append('\n');
        }
        return out.toString();
    }

    /** Compact prompt block: enclosing scope plus nearby source lines. */
    public String promptBlock() {
        StringBuilder out = new StringBuilder();
        if (enclosingClass != null) {
            out.append("enclosing class: ").append(enclosingClass.name()).append('\n');
        }
        if (enclosingMethod != null) {
            out.append("enclosing method: ").append(enclosingMethod.signature()).append('\n');
        }
        out.append("```\n");
        for (String line : surroundingLines(10)) {
            out.append(line).append('\n');
        }
        out.append("```");
        return out.toString();
    }
}
