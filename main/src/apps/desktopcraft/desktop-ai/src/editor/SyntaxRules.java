package editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Lexes source text into styled tokens. A single lexer with per-language keyword,
 * type, and literal tables keeps the editor simple while remaining extensible —
 * add a language by extending {@link LanguageKind} and the tables below.
 */
public final class SyntaxRules {
    public enum TokenType { KEYWORD, TYPE, STRING, CHAR, NUMBER, COMMENT, ANNOTATION, PUNCTUATION, PLAIN }

    public static final class Token {
        private final TokenType type;
        private final String text;

        Token(TokenType type, String text) {
            this.type = type;
            this.text = text;
        }

        public TokenType type() {
            return type;
        }

        public String text() {
            return text;
        }
    }

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
            "interface", "long", "native", "new", "package", "private", "protected", "public",
            "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
            "throw", "throws", "transient", "try", "var", "void", "volatile", "while", "record",
            "yield", "sealed", "permits", "non-sealed", "true", "false", "null"));

    private static final Set<String> JAVA_TYPES = new HashSet<>(Arrays.asList(
            "String", "Object", "Integer", "Long", "Double", "Float", "Boolean", "Character",
            "Byte", "Short", "Void", "List", "ArrayList", "Map", "HashMap", "Set", "HashSet",
            "Optional", "Collection", "Iterator", "Exception", "RuntimeException", "Thread"));

    private static final Set<String> PYTHON_KEYWORDS = new HashSet<>(Arrays.asList(
            "and", "as", "assert", "async", "await", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for", "from", "global", "if", "import", "in",
            "is", "lambda", "nonlocal", "not", "or", "pass", "raise", "return", "try", "while",
            "with", "yield", "True", "False", "None", "self"));

    private static final Set<String> JS_KEYWORDS = new HashSet<>(Arrays.asList(
            "break", "case", "catch", "class", "const", "continue", "debugger", "default",
            "delete", "do", "else", "export", "extends", "finally", "for", "function", "if",
            "import", "in", "instanceof", "let", "new", "return", "super", "switch", "this",
            "throw", "try", "typeof", "var", "void", "while", "with", "yield", "async", "await",
            "null", "true", "false", "undefined"));

    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
            "select", "from", "where", "insert", "into", "values", "update", "set", "delete",
            "create", "table", "drop", "alter", "join", "left", "right", "inner", "outer", "on",
            "group", "by", "order", "having", "limit", "offset", "and", "or", "not", "null",
            "primary", "key", "foreign", "references", "distinct", "as", "count", "sum", "avg",
            "min", "max", "index", "view", "union", "case", "when", "then", "else", "end"));

    private static final Set<String> HTML_TAGS = new HashSet<>(Arrays.asList(
            "html", "head", "body", "div", "span", "p", "a", "img", "button", "input", "form",
            "table", "tr", "td", "th", "ul", "ol", "li", "h1", "h2", "h3", "h4", "h5", "h6",
            "nav", "header", "footer", "main", "section", "article", "aside", "script", "style",
            "link", "meta", "title", "select", "option", "textarea", "label", "svg", "canvas"));

    private static final Set<String> CSS_PROPERTIES = new HashSet<>(Arrays.asList(
            "color", "background", "background-color", "margin", "padding", "border", "border-radius",
            "display", "position", "top", "right", "bottom", "left", "width", "height", "max-width",
            "font", "font-size", "font-weight", "font-family", "line-height", "text-align", "flex",
            "grid", "gap", "overflow", "cursor", "opacity", "transition", "transform", "z-index"));

    private final LanguageKind language;
    private final Set<String> keywords;
    private final Set<String> types;
    private final Set<String> tagNames;
    private final Set<String> cssProperties;
    private final Set<String> cssValues = new HashSet<>(Arrays.asList(
            "red", "blue", "green", "black", "white", "gray", "transparent", "solid", "dashed",
            "none", "block", "inline", "flex", "grid", "absolute", "relative", "fixed", "center",
            "bold", "italic", "normal", "left", "right", "top", "bottom", "auto", "inherit"));

    public SyntaxRules(LanguageKind language) {
        this.language = language;
        this.keywords = keywordSet(language);
        this.types = typeSet(language);
        this.tagNames = language == LanguageKind.HTML ? HTML_TAGS : new HashSet<>();
        this.cssProperties = language == LanguageKind.CSS ? CSS_PROPERTIES : new HashSet<>();
    }

    public LanguageKind language() {
        return language;
    }

    public List<Token> tokenize(String text) {
        List<Token> tokens = new ArrayList<>();
        int i = 0;
        int length = text.length();
        while (i < length) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '/' && i + 1 < length) {
                if (text.charAt(i + 1) == '/') {
                    int end = text.indexOf('\n', i);
                    if (end < 0) end = length;
                    tokens.add(new Token(TokenType.COMMENT, text.substring(i, end)));
                    i = end;
                    continue;
                }
                if (text.charAt(i + 1) == '*') {
                    int end = text.indexOf("*/", i + 2);
                    int stop = end < 0 ? length : end + 2;
                    tokens.add(new Token(TokenType.COMMENT, text.substring(i, stop)));
                    i = stop;
                    continue;
                }
            }
            if (language == LanguageKind.PYTHON && c == '#') {
                int end = text.indexOf('\n', i);
                if (end < 0) end = length;
                tokens.add(new Token(TokenType.COMMENT, text.substring(i, end)));
                i = end;
                continue;
            }
            if (language == LanguageKind.SQL && c == '-' && i + 1 < length && text.charAt(i + 1) == '-') {
                int end = text.indexOf('\n', i);
                if (end < 0) end = length;
                tokens.add(new Token(TokenType.COMMENT, text.substring(i, end)));
                i = end;
                continue;
            }
            if (language == LanguageKind.HTML && c == '<' && i + 1 < length && text.charAt(i + 1) != '/') {
                int end = text.indexOf('>', i);
                int stop = end < 0 ? length : end + 1;
                tokens.add(new Token(TokenType.PUNCTUATION, text.substring(i, stop)));
                i = stop;
                continue;
            }
            if (c == '"' || ((language == LanguageKind.JAVA || language == LanguageKind.JAVASCRIPT) && c == '\'')) {
                char quote = c;
                int j = i + 1;
                StringBuilder value = new StringBuilder();
                value.append(c);
                while (j < length) {
                    char current = text.charAt(j);
                    value.append(current);
                    j++;
                    if (current == '\\' && j < length) {
                        value.append(text.charAt(j));
                        j++;
                    } else if (current == quote) {
                        break;
                    }
                }
                tokens.add(new Token(c == '"' ? TokenType.STRING : TokenType.CHAR, value.toString()));
                i = j;
                continue;
            }
            if (Character.isLetter(c) || c == '_' || (c >= 0x80)) {
                int start = i;
                while (i < length && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '_')) {
                    i++;
                }
                String word = text.substring(start, i);
                tokens.add(new Token(classifyWord(word), word));
                continue;
            }
            if (Character.isDigit(c)) {
                int start = i;
                while (i < length && (Character.isLetterOrDigit(text.charAt(i)) || text.charAt(i) == '.'
                        || text.charAt(i) == '_' || text.charAt(i) == 'x' || text.charAt(i) == 'X'
                        || text.charAt(i) == 'e' || text.charAt(i) == 'E' || text.charAt(i) == '+' || text.charAt(i) == '-')) {
                    i++;
                }
                tokens.add(new Token(TokenType.NUMBER, text.substring(start, i)));
                continue;
            }
            tokens.add(new Token(TokenType.PUNCTUATION, String.valueOf(c)));
            i++;
        }
        return tokens;
    }

    private TokenType classifyWord(String word) {
        String lower = word.toLowerCase(Locale.ROOT);
        if (keywords.contains(lower) || (language == LanguageKind.SQL && keywords.contains(lower))) {
            return TokenType.KEYWORD;
        }
        if (types.contains(word)) {
            return TokenType.TYPE;
        }
        if (language == LanguageKind.HTML && tagNames.contains(lower)) {
            return TokenType.KEYWORD;
        }
        if (language == LanguageKind.CSS && cssProperties.contains(lower)) {
            return TokenType.KEYWORD;
        }
        if (language == LanguageKind.CSS && cssValues.contains(lower)) {
            return TokenType.STRING;
        }
        if (word.startsWith("@") || (language == LanguageKind.JAVA && word.length() > 1 && Character.isUpperCase(word.charAt(0)) && !types.contains(word))) {
            return TokenType.ANNOTATION;
        }
        return TokenType.PLAIN;
    }

    private static Set<String> keywordSet(LanguageKind language) {
        switch (language) {
            case JAVA: return JAVA_KEYWORDS;
            case PYTHON: return PYTHON_KEYWORDS;
            case JAVASCRIPT:
            case TYPESCRIPT: return JS_KEYWORDS;
            case SQL: return SQL_KEYWORDS;
            default: return new HashSet<>();
        }
    }

    private static Set<String> typeSet(LanguageKind language) {
        switch (language) {
            case JAVA: return JAVA_TYPES;
            default: return new HashSet<>();
        }
    }
}
