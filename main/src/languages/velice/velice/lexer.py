"""Velice Lexer – tokenizes source code into tokens with indentation tracking."""
from __future__ import annotations
import enum, re, unicodedata
from dataclasses import dataclass
from typing import List, Optional, Any

class TT(enum.Enum):
    # Literals
    INT = "INT"; FLOAT = "FLOAT"; STRING = "STRING"; CHAR = "CHAR"; BOOL = "BOOL"; NIL = "NIL"
    # Identifier
    IDENT = "IDENT"
    # Keywords
    FN="fn"; LET="let"; VAR="var"; MUT="mut"; CONST="const"; CLASS="class"; STRUCT="struct"
    ENUM="enum"; TRAIT="trait"; IMPL="impl"; MATCH="match"; IF="if"; ELIF="elif"
    ELSE="else"; WHILE="while"; FOR="for"; IN="in"; LOOP="loop"; BREAK="break"
    CONTINUE="continue"; RETURN="return"; DEFER="defer"; YIELD="yield"
    ASYNC="async"; AWAIT="await"; SPAWN="spawn"; PUB="pub"; MOD="mod"; USE="use"
    FROM="from"; AS="as"; TYPE="type"; WHERE="where"; SELF="self"; SUPER="super"
    TRUE="true"; FALSE="false"; NIL_LIT="nil"; NONE="none"; SOME="some"
    OK="ok"; ERR="err"; PANIC="panic"; TRY="try"; CATCH="catch"; FINALLY="finally"
    THROW="throw"; ASSERT="assert"; UNSAFE="unsafe"; EXTERN="extern"; INLINE="inline"
    IS="is"; NOT="not"; AND="and"; OR="or"; GUARD="guard"; NEW="new"; SIZEOF="sizeof"
    TYPEOF="typeof"; IMPORT="import"; EXTENDS="extends"; WITH="with"; ARGU="argu"
    # Operators
    PLUS="+"; MINUS="-"; STAR="*"; SLASH="/"; PERCENT="%"; STARSTAR="**"
    EQ="=="; NEQ="!="; LT="<"; GT=">"; LTE="<="; GTE=">="
    ASSIGN="="; PLUS_EQ="+="; MINUS_EQ="-="; STAR_EQ="*="; SLASH_EQ="/="; PERCENT_EQ="%="
    AMP_AMP="&&"; PIPE_PIPE="||"; BANG="!"; QUESTION="?"
    QUESTION_QUESTION="??"; FAT_ARROW="=>"; PIPE_RIGHT="|>"
    COLON_COLON="::"; DOT="."; DOT_DOT=".."; DOT_DOT_DOT="..."; ARROW="->"
    AT="@"; HASH="#"; TILDE="~"; CARET="^"; PIPE="|"; AMP="&"
    AMP_EQ="&="; PIPE_EQ="|="; CARET_EQ="^="; SHL=" shl "; SHR=" shr "
    # Delimiters
    LPAREN="("; RPAREN=")"; LBRACE="{"; RBRACE="}"; LBRACKET="["; RBRACKET="]"
    COMMA=","; SEMICOLON=";"; COLON=":"; UNDERSCORE="_"
    # Special
    INDENT="INDENT"; DEDENT="DEDENT"; NEWLINE="NEWLINE"; EOF="EOF"; ERROR="ERROR"

KEYWORDS = {
    "fn": TT.FN, "let": TT.LET, "mut": TT.MUT, "var": TT.VAR, "const": TT.CONST,
    "class": TT.CLASS, "struct": TT.STRUCT, "enum": TT.ENUM, "trait": TT.TRAIT,
    "impl": TT.IMPL, "match": TT.MATCH, "if": TT.IF, "elif": TT.ELIF,
    "else": TT.ELSE, "while": TT.WHILE, "for": TT.FOR, "in": TT.IN,
    "loop": TT.LOOP, "break": TT.BREAK, "continue": TT.CONTINUE,
    "return": TT.RETURN, "defer": TT.DEFER, "yield": TT.YIELD,
    "async": TT.ASYNC, "await": TT.AWAIT, "spawn": TT.SPAWN,
    "pub": TT.PUB, "mod": TT.MOD, "use": TT.USE, "from": TT.FROM,
    "as": TT.AS, "type": TT.TYPE, "where": TT.WHERE,
    "self": TT.SELF, "super": TT.SUPER,
    "true": TT.BOOL, "false": TT.BOOL,
    "nil": TT.NIL, "none": TT.NONE, "some": TT.SOME,
    "ok": TT.OK, "err": TT.ERR, "panic": TT.PANIC,
    "try": TT.TRY, "catch": TT.CATCH, "finally": TT.FINALLY,
    "throw": TT.THROW, "assert": TT.ASSERT, "unsafe": TT.UNSAFE,
    "extern": TT.EXTERN, "inline": TT.INLINE, "is": TT.IS,
    "not": TT.NOT, "and": TT.AND, "or": TT.OR, "guard": TT.GUARD,
    "new": TT.NEW, "sizeof": TT.SIZEOF, "typeof": TT.TYPEOF,
    "import": TT.IMPORT, "extends": TT.EXTENDS, "with": TT.WITH,
    "argu": TT.ARGU,
}

MULTI_OPS = {
    "**": TT.STARSTAR, "==": TT.EQ, "!=": TT.NEQ, "<=": TT.LTE, ">=": TT.GTE,
    "+=": TT.PLUS_EQ, "-=": TT.MINUS_EQ, "*=": TT.STAR_EQ, "/=": TT.SLASH_EQ,
    "%=": TT.PERCENT_EQ, "&&": TT.AMP_AMP, "||": TT.PIPE_PIPE,
    "??": TT.QUESTION_QUESTION, "=>": TT.FAT_ARROW, "|>": TT.PIPE_RIGHT,
    "::": TT.COLON_COLON, "..": TT.DOT_DOT, "...": TT.DOT_DOT_DOT,
    "->": TT.ARROW, "&=": TT.AMP_EQ, "|=": TT.PIPE_EQ, "^=": TT.CARET_EQ,
}

@dataclass(frozen=True)
class Token:
    type: TT
    value: Any
    line: int
    col: int
    def __repr__(self):
        return f"Token({self.type.name}, {self.value!r}, L{self.line}:C{self.col})"

class LexError(Exception):
    def __init__(self, msg, line, col):
        super().__init__(msg)
        self.line = line; self.col = col

_ESCAPE = {"n":"\n","t":"\t","r":"\r","\\":"\\","'":"'",'"':'"', "0":"\0"}

class Lexer:
    def __init__(self, source: str, filename: str = "<stdin>"):
        self.source = source; self.filename = filename
        self.pos = 0; self.line = 1; self.col = 1
        self.tokens: List[Token] = []
        self.indent_stack: list[int] = [0]
        self.paren_depth = 0
        self.at_line_start = True

    def _peek(self, off=0):
        p = self.pos + off
        return self.source[p] if p < len(self.source) else ""

    def _advance(self):
        ch = self.source[self.pos]; self.pos += 1
        if ch == "\n": self.line += 1; self.col = 1
        else: self.col += 1
        return ch

    def _emit(self, tt, value=None):
        if isinstance(tt, Token):
            self.tokens.append(tt)
        else:
            self.tokens.append(Token(tt, value, self.line, self.col))

    def _error(self, msg):
        return LexError(msg, self.line, self.col)

    def _skip_line_comment(self):
        while self.pos < len(self.source) and self._peek() != "\n":
            self._advance()

    def _skip_block_comment(self):
        depth = 1
        while self.pos < len(self.source) and depth > 0:
            if self._peek() == "/" and self._peek(1) == "*":
                self._advance(); self._advance(); depth += 1
            elif self._peek() == "*" and self._peek(1) == "/":
                self._advance(); self._advance(); depth -= 1
            else:
                self._advance()
        if depth > 0: raise self._error("Unterminated block comment")

    def _skip_whitespace_and_comments(self):
        while self.pos < len(self.source):
            ch = self._peek()
            if ch in " \t\r":
                self._advance()
            elif ch == "/" and self._peek(1) == "/":
                self._advance(); self._advance(); self._skip_line_comment()
            elif ch == "/" and self._peek(1) == "*":
                self._advance(); self._advance(); self._skip_block_comment()
            elif ch == "#" and self._peek(1) == "#" and self._peek(2) == "#":
                self._advance(); self._advance(); self._advance()
                while self.pos < len(self.source) and self._peek() != "\n":
                    self._advance()
            elif ch == "#":
                self._skip_line_comment()
            else:
                break

    def _handle_indentation(self):
        if self.paren_depth > 0 or not self.at_line_start:
            return
        self.at_line_start = False
        indent = 0
        while self.pos < len(self.source) and self._peek() in " \t":
            indent += 4 if self._peek() == "\t" else 1
            self._advance()
        if self._peek() == "#":
            self._skip_line_comment()
            if self._peek() == "\n": self._advance()
            self.at_line_start = True; return
        if self._peek() in "\n\r":
            if self._peek() == "\n": self._advance()
            self.at_line_start = True; return
        if self._peek() == "":
            indent = 0
        top = self.indent_stack[-1]
        if indent > top:
            self.indent_stack.append(indent); self._emit(TT.INDENT)
        elif indent < top:
            while self.indent_stack[-1] > indent:
                self.indent_stack.pop(); self._emit(TT.DEDENT)
            if self.indent_stack[-1] != indent:
                raise self._error(f"Bad indentation at column {indent}")

    def _escape_char(self):
        ch = self._advance()
        if ch == "x":
            h = ""; 
            for _ in range(2):
                if self._peek() in "0123456789abcdefABCDEF": h += self._advance()
                else: raise self._error("Invalid hex escape")
            return chr(int(h, 16))
        if ch == "u" and self._peek() == "{":
            self._advance(); h = ""
            while self._peek() != "}": h += self._advance()
            self._advance(); return chr(int(h, 16))
        return _ESCAPE.get(ch, ch)

    def _read_string(self, quote):
        parts = []
        if self._peek(1) == quote and self._peek(2) == quote:
            self._advance(); self._advance(); self._advance()
            while self.pos < len(self.source):
                if self._peek() == quote and self._peek(1) == quote and self._peek(2) == quote:
                    self._advance(); self._advance(); self._advance(); return "".join(parts)
                if self._peek() == "\\": self._advance(); parts.append(self._escape_char())
                else: parts.append(self._advance())
            raise self._error("Unterminated triple-quoted string")
        self._advance()
        while self.pos < len(self.source):
            ch = self._peek()
            if ch == quote: self._advance(); return "".join(parts)
            if ch == "\n": raise self._error("Unterminated string")
            if ch == "\\": self._advance(); parts.append(self._escape_char())
            else: parts.append(self._advance())
        raise self._error("Unterminated string")

    def _read_number(self):
        sl, sc = self.line, self.col
        if self._peek() == "0" and self._peek(1) in ("x", "X"):
            self._advance(); self._advance(); n = ""
            while self._peek() in "0123456789abcdefABCDEF_": n += self._advance()
            return Token(TT.INT, int(n.replace("_",""), 16), sl, sc)
        if self._peek() == "0" and self._peek(1) in ("o", "O"):
            self._advance(); self._advance(); n = ""
            while self._peek() in "01234567_": n += self._advance()
            return Token(TT.INT, int(n.replace("_",""), 8), sl, sc)
        if self._peek() == "0" and self._peek(1) in ("b", "B"):
            self._advance(); self._advance(); n = ""
            while self._peek() in "01_": n += self._advance()
            return Token(TT.INT, int(n.replace("_",""), 2), sl, sc)
        n = ""; is_float = False
        while self._peek().isdigit() or self._peek() == "_": n += self._advance()
        if self._peek() == "." and self._peek(1).isdigit():
            is_float = True; n += self._advance()
            while self._peek().isdigit() or self._peek() == "_": n += self._advance()
        if self._peek() in ("e", "E"):
            is_float = True; n += self._advance()
            if self._peek() in ("+", "-"): n += self._advance()
            while self._peek().isdigit(): n += self._advance()
        return Token(TT.FLOAT if is_float else TT.INT, float(n.replace("_","")) if is_float else int(n.replace("_","")), sl, sc)

    def tokenize(self) -> List[Token]:
        while self.pos < len(self.source):
            self._skip_whitespace_and_comments()
            if self.pos >= len(self.source): break
            if self.at_line_start and self.paren_depth == 0:
                self._handle_indentation()
                if self.at_line_start: continue
            ch = self._peek()
            if ch == "\n":
                self._advance(); self._emit(TT.NEWLINE); self.at_line_start = True; continue
            if ch in " \t\r": self._advance(); continue
            sl, sc = self.line, self.col
            if ch == "(" or ch == "[" or ch == "{":
                self.paren_depth += 1
                m = {"(":TT.LPAREN,"[":TT.LBRACKET,"{":TT.LBRACE}[ch]
                self._advance(); self._emit(m, sl); continue
            if ch == ")" or ch == "]" or ch == "}":
                self.paren_depth = max(0, self.paren_depth - 1)
                m = {")":TT.RPAREN,"]":TT.RBRACKET,"}":TT.RBRACE}[ch]
                self._advance(); self._emit(m); continue
            if ch in "+-*/%=<>!&|^~#@?;:,.": 
                op = self._advance()
                if ch == "." and self._peek() == ".":
                    self._advance()
                    if self._peek() == ".": self._advance(); self._emit(TT.DOT_DOT_DOT); continue
                    self._emit(TT.DOT_DOT); continue
                if op + self._peek() in MULTI_OPS:
                    op2 = op + self._peek(); self._advance()
                    self._emit(MULTI_OPS[op2]); continue
                if ch == "-" and self._peek() == ">": self._advance(); self._emit(TT.ARROW); continue
                SINGLE = {"+":TT.PLUS,"-":TT.MINUS,"*":TT.STAR,"/":TT.SLASH,"%":TT.PERCENT,
                    "=":TT.ASSIGN,"<":TT.LT,">":TT.GT,"!":TT.BANG,"?":TT.QUESTION,
                    "&":TT.AMP,"|":TT.PIPE,"^":TT.CARET,"~":TT.TILDE,"@":TT.AT,
                    "#":TT.HASH,";":TT.SEMICOLON,":":TT.COLON,",":TT.COMMA,".":TT.DOT}
                if ch in SINGLE: self._emit(SINGLE[ch]); continue
                raise self._error(f"Unexpected character: {ch!r}")
            if ch == '"' or ch == "'":
                s = self._read_string(ch)
                if len(s) == 1 and ch == "'": self._emit(TT.CHAR, s); continue
                else: self._emit(TT.STRING, s); continue
            if ch.isdigit(): self._emit(self._read_number()); continue
            if ch == "r" and self._peek(1) == '"':
                self._advance()
                parts = []
                self._advance()
                while self.pos < len(self.source) and self._peek() != '"':
                    parts.append(self._advance())
                if self._peek() != '"': raise self._error("Unterminated raw string")
                self._advance()
                self._emit(TT.STRING, "".join(parts))
                continue
            if ch.isalpha() or ch == "_":
                ident = ""
                while self._peek().isalnum() or self._peek() == "_": ident += self._advance()
                if ident in KEYWORDS:
                    tt = KEYWORDS[ident]
                    if tt == TT.BOOL: self._emit(TT.BOOL, ident == "true")
                    elif tt == TT.NIL: self._emit(TT.NIL, None)
                    else: self._emit(tt)
                else: self._emit(TT.IDENT, ident)
                continue
            raise self._error(f"Unexpected character: {ch!r}")
        while len(self.indent_stack) > 1:
            self.indent_stack.pop(); self._emit(TT.DEDENT)
        self._emit(TT.EOF)
        return self.tokens
