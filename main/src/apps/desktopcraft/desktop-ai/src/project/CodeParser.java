package project;

import editor.LanguageKind;
import util.Text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight structural parser for Java (and best-effort for other languages).
 * Extracts packages, imports, types, methods, constructors, and fields with a
 * string/comment-aware brace scanner so symbol line ranges are accurate.
 */
public final class CodeParser {
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+([\\w.]+)\\s*;");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^\\s*import\\s+(?:static\\s+)?([\\w.*]+)\\s*;");
    private static final Pattern TYPE_PATTERN = Pattern.compile(
            "^\\s*(?:public\\s+|private\\s+|protected\\s+|final\\s+|abstract\\s+|sealed\\s+|non-sealed\\s+|static\\s+)*"
                    + "(class|interface|enum|record)\\s+([A-Za-z_$][\\w$]*)"
                    + "(?:\\s+extends\\s+([A-Za-z_$][\\w.$]*))?"
                    + "(?:\\s+implements\\s+([A-Za-z_$][\\w.$]*))?\\s*\\{?");
    private static final Pattern METHOD_PATTERN = Pattern.compile(
            "^\\s*((?:public|private|protected|static|final|abstract|synchronized|native|default|transient|volatile|@\\w+\\s*)*)"
                    + "([\\w$<>\\[\\].\\s]+?)\\s+([A-Za-z_$][\\w$]*)\\s*\\(([^)]*)\\)\\s*(?:throws\\s+[\\w.,\\s]+)?\\s*\\{?");
    private static final Pattern FIELD_PATTERN = Pattern.compile(
            "^\\s*((?:public|private|protected|static|final|volatile|transient|@\\w+\\s*)*)"
                    + "([\\w$<>\\[\\].\\s]+?)\\s+([A-Za-z_$][\\w$]*)\\s*(?:=|;)\\s*;?$");

    private CodeParser() {}

    /** A symbol opened by a { whose body must close back to {@code returnDepth}. */
    private static final class OpenScope {
        final Symbol symbol;
        final int returnDepth;

        OpenScope(Symbol symbol, int returnDepth) {
            this.symbol = symbol;
            this.returnDepth = returnDepth;
        }
    }

    public static ParsedFile parse(String path, String content, LanguageKind language) {
        List<String> lines = Text.lines(content == null ? "" : content);
        String packageName = "";
        List<String> imports = new ArrayList<>();
        List<Symbol> symbols = new ArrayList<>();
        boolean java = language == LanguageKind.JAVA;

        Deque<OpenScope> scopeStack = new ArrayDeque<>();
        BraceCounter counter = new BraceCounter();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int startDepth = counter.depth();
            String enclosingType = enclosingTypeName(scopeStack);

            if (java) {
                Matcher packageMatcher = PACKAGE_PATTERN.matcher(line);
                if (packageMatcher.matches()) {
                    packageName = packageMatcher.group(1);
                    symbols.add(new Symbol(Symbol.Kind.PACKAGE, packageName, path, i + 1,
                            line.indexOf(packageName), i + 1, "", packageName, null));
                }
                Matcher importMatcher = IMPORT_PATTERN.matcher(line);
                if (importMatcher.matches()) {
                    String imported = importMatcher.group(1);
                    imports.add(imported);
                    symbols.add(new Symbol(Symbol.Kind.IMPORT, imported, path, i + 1, 0, i + 1, "", imported, null));
                    counter.consume(line);
                    continue;
                }
                Matcher typeMatcher = TYPE_PATTERN.matcher(line);
                if (typeMatcher.matches()) {
                    Symbol.Kind kind = toTypeKind(typeMatcher.group(1));
                    String name = typeMatcher.group(2);
                    Symbol symbol = new Symbol(kind, name, path, i + 1, line.indexOf(name), i + 1,
                            modifiersOf(line), line.trim(), enclosingType);
                    symbols.add(symbol);
                    scopeStack.push(new OpenScope(symbol, startDepth));
                    counter.consume(line);
                    closeScopes(scopeStack, counter.depth(), i + 1);
                    continue;
                }
                if (startDepth == 1 && !scopeStack.isEmpty()
                        && scopeStack.peek().symbol.isType()) {
                    Matcher methodMatcher = METHOD_PATTERN.matcher(line);
                    Matcher fieldMatcher = FIELD_PATTERN.matcher(line);
                    if (methodMatcher.matches() && line.contains("(") && !line.trim().endsWith(");")) {
                        String name = methodMatcher.group(3);
                        String returnType = methodMatcher.group(2).trim();
                        Symbol.Kind kind = name.equals(enclosingType) ? Symbol.Kind.CONSTRUCTOR : Symbol.Kind.METHOD;
                        Symbol symbol = new Symbol(kind, name, path, i + 1, line.indexOf(name), i + 1,
                                methodMatcher.group(1).trim(), returnType + " " + name + "(" + methodMatcher.group(4).trim() + ")",
                                enclosingType);
                        symbols.add(symbol);
                        scopeStack.push(new OpenScope(symbol, startDepth));
                    } else if (fieldMatcher.matches() && !line.contains("(")) {
                        String name = fieldMatcher.group(3);
                        symbols.add(new Symbol(Symbol.Kind.FIELD, name, path, i + 1, line.indexOf(name), i + 1,
                                fieldMatcher.group(1).trim(), fieldMatcher.group(2).trim() + " " + name, enclosingType));
                    }
                }
            }

            counter.consume(line);
            closeScopes(scopeStack, counter.depth(), i + 1);
        }
        return new ParsedFile(path, packageName, imports, symbols, lines, language.id());
    }

    private static void closeScopes(Deque<OpenScope> scopes, int depth, int line) {
        while (!scopes.isEmpty() && depth <= scopes.peek().returnDepth) {
            OpenScope closed = scopes.pop();
            closed.symbol.endLine(line);
        }
    }

    /** Name of the nearest open type scope, or null if outside any type. */
    private static String enclosingTypeName(Deque<OpenScope> scopes) {
        for (OpenScope scope : scopes) {
            if (scope.symbol.isType()) {
                return scope.symbol.name();
            }
        }
        return null;
    }

    private static Symbol.Kind toTypeKind(String raw) {
        switch (raw) {
            case "interface": return Symbol.Kind.INTERFACE;
            case "enum": return Symbol.Kind.ENUM;
            case "record": return Symbol.Kind.RECORD;
            default: return Symbol.Kind.CLASS;
        }
    }

    private static String modifiersOf(String line) {
        StringBuilder out = new StringBuilder();
        for (String word : line.split("\\s+")) {
            if (word.equals("public") || word.equals("private") || word.equals("protected")
                    || word.equals("final") || word.equals("abstract") || word.equals("sealed")
                    || word.equals("non-sealed") || word.equals("static")) {
                if (out.length() > 0) out.append(' ');
                out.append(word);
            }
        }
        return out.toString();
    }

    /**
     * Scans lines for braces while ignoring braces inside string/char literals
     * and comments. Block-comment state persists across lines.
     */
    private static final class BraceCounter {
        private int depth;
        private boolean blockComment;

        int depth() {
            return depth;
        }

        void consume(String line) {
            int i = 0;
            int n = line.length();
            while (i < n) {
                char c = line.charAt(i);
                char next = i + 1 < n ? line.charAt(i + 1) : '\0';
                if (blockComment) {
                    if (c == '*' && next == '/') {
                        blockComment = false;
                        i += 2;
                    } else {
                        i++;
                    }
                    continue;
                }
                if (c == '/' && next == '/') {
                    break;
                }
                if (c == '/' && next == '*') {
                    blockComment = true;
                    i += 2;
                    continue;
                }
                if (c == '"') {
                    i = skipString(line, i, '"');
                    continue;
                }
                if (c == '\'') {
                    i = skipString(line, i, '\'');
                    continue;
                }
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                }
                i++;
            }
        }

        private static int skipString(String line, int start, char quote) {
            int i = start + 1;
            int n = line.length();
            while (i < n) {
                char c = line.charAt(i);
                if (c == '\\') {
                    i += 2;
                    continue;
                }
                if (c == quote) {
                    return i + 1;
                }
                i++;
            }
            return n;
        }
    }
}
