"""Velice Parser – recursive descent, produces AST from token stream."""
from __future__ import annotations
from velice.lexer import TT, Token, LexError
from velice import ast_nodes as A

class ParseError(Exception):
    def __init__(self, msg, line=0, col=0):
        super().__init__(msg); self.line = line; self.col = col

class Parser:
    def __init__(self, tokens: list[Token], source: str = ""):
        self.tokens = tokens; self.pos = 0; self.source = source

    def _peek(self, off=0):
        p = self.pos + off
        return self.tokens[p] if p < len(self.tokens) else Token(TT.EOF, None, 0, 0)

    def _advance(self):
        t = self.tokens[self.pos]; self.pos += 1; return t

    def _at(self, *types):
        return self._peek().type in types

    def _match(self, *types):
        if self._peek().type in types:
            return self._advance()
        return None

    def _expect(self, tt, msg=None):
        t = self._match(tt)
        if not t:
            p = self._peek()
            raise ParseError(msg or f"Expected {tt.name}, got {p.type.name} at line {p.line}", p.line, p.col)
        return t

    def _skip_newlines(self):
        while self._at(TT.NEWLINE): self._advance()

    def _block(self):
        self._skip_newlines()
        if self._match(TT.LBRACE):
            stmts = []
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                while self._match(TT.SEMICOLON): self._skip_newlines()
                if self._at(TT.RBRACE, TT.EOF): break
                stmts.append(self._stmt())
                self._match(TT.SEMICOLON)
                self._skip_newlines()
            self._expect(TT.RBRACE)
            t = stmts[0] if len(stmts) == 1 else A.Block(stmts[0].line if stmts else 0, stmts[0].col if stmts else 0, stmts)
            return t
        # indentation block
        t = self._expect(TT.INDENT)
        stmts = []
        while not self._at(TT.DEDENT, TT.EOF):
            stmts.append(self._stmt())
            self._skip_newlines()
        self._match(TT.DEDENT)
        if len(stmts) == 1: return stmts[0]
        return A.Block(t.line, t.col, stmts)

    def _param(self):
        t = self._peek(); mut = self._match(TT.MUT, TT.VAR)
        name = self._expect(TT.IDENT).value
        ann = None
        if self._match(TT.COLON):
            ann = self._type_expr()
        default = None
        if self._match(TT.ASSIGN):
            default = self._expr()
        return A.LetStmt(t.line, t.col, name, ann, default, mutable=bool(mut))

    def parse(self):
        self._skip_newlines()
        stmts = []
        while not self._at(TT.EOF):
            s = self._stmt(); stmts.append(s); self._skip_newlines()
        return A.Program(0, 0, stmts, self.source)

    def _stmt(self):
        t = self._peek()
        if t.type == TT.LET: return self._let_stmt()
        if t.type == TT.VAR: return self._var_stmt()
        if t.type == TT.CONST: return self._const_stmt()
        if t.type == TT.FN: return self._fn_decl()
        if t.type == TT.CLASS: return self._class_decl()
        if t.type == TT.STRUCT: return self._struct_decl()
        if t.type == TT.ENUM: return self._enum_decl()
        if t.type == TT.TRAIT: return self._trait_decl()
        if t.type == TT.IMPL: return self._impl_decl()
        if t.type == TT.RETURN: return self._return_stmt()
        if t.type == TT.BREAK: return self._break_stmt()
        if t.type == TT.CONTINUE: return self._continue_stmt()
        if t.type == TT.DEFER: return self._defer_stmt()
        if t.type == TT.IF: return self._if_stmt()
        if t.type == TT.WHILE: return self._while_stmt()
        if t.type == TT.FOR: return self._for_stmt()
        if t.type == TT.LOOP: return self._loop_stmt()
        if t.type == TT.MATCH: return self._match_stmt()
        if t.type == TT.THROW: return self._throw_stmt()
        if t.type == TT.TRY: return self._try_stmt()
        if t.type == TT.ASSERT: return self._assert_stmt()
        if t.type == TT.IMPORT or t.type == TT.USE or t.type == TT.FROM: return self._import_stmt()
        if t.type == TT.TYPE: return self._type_alias()
        if t.type == TT.MOD: return self._mod_decl()
        # GUI DSL: `window Name { ... }` / `run Name`
        if t.type == TT.IDENT:
            if t.value == "window" and self._peek(1).type == TT.IDENT:
                return self._window_decl()
            if t.value == "run" and self._peek(1).type == TT.IDENT:
                return self._run_stmt()
        # expression statement
        e = self._expr()
        return A.ExprStmt(e.line, e.col, e)

    # ── GUI DSL ────────────────────────────────────────────────────────
    def _window_decl(self):
        t = self._advance()
        name = self._expect(TT.IDENT).value
        props = []; children = []
        self._skip_newlines()
        self._expect(TT.LBRACE)
        self._skip_newlines()
        while not self._at(TT.RBRACE, TT.EOF):
            item = self._widget_block_item()
            if item is None:
                self._advance(); continue
            if item[0] == "prop":
                props.append((item[1], item[2]))
            else:
                children.append(item[1])
            self._match(TT.SEMICOLON)
            self._skip_newlines()
        self._expect(TT.RBRACE)
        return A.WindowDecl(t.line, t.col, name, props, children)

    def _run_stmt(self):
        t = self._advance()
        name = self._expect(TT.IDENT).value
        return A.RunStmt(t.line, t.col, name)

    def _widget_block_item(self):
        """One item inside a window/widget body.

        Returns ('prop', key, value) | ('widget', WidgetNode) | None.
        """
        if self._at(TT.IDENT):
            peek = self._peek(1).type
            # property:  key = expr
            if peek == TT.ASSIGN:
                key = self._advance().value
                self._advance()
                val = self._expr()
                return ("prop", key, val)
            # event:  onClick { ... }
            if peek == TT.LBRACE and self._peek().value.startswith("on"):
                ev = self._advance().value[2:]
                block = self._block()
                return ("widget", A.WidgetNode(0, 0, "event", ev, [], [], [block]))
            # widget child
            return ("widget", self._widget())
        if self._at(TT.STRING, TT.INT, TT.FLOAT):
            return None
        return None

    def _widget(self):
        t = self._peek()
        wtype = self._advance().value
        wname = None
        if self._at(TT.IDENT, TT.STRING):
            wname = self._advance().value
        props = []; events = []; children = []
        if self._at(TT.LBRACE):
            self._advance()
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                item = self._widget_block_item()
                if item is None:
                    self._advance(); continue
                if item[0] == "prop":
                    props.append((item[1], item[2]))
                else:
                    node = item[1]
                    if node.wtype == "event":
                        body = node.children[0] if node.children else A.Block(0, 0, [])
                        if isinstance(body, tuple):
                            body = body[1] if len(body) > 1 else body[0]
                        events.append((node.wname, body))
                    else:
                        children.append(node)
                self._match(TT.SEMICOLON)
                self._skip_newlines()
            self._expect(TT.RBRACE)
        return A.WidgetNode(t.line, t.col, wtype, wname, props, events, children)

    def _let_stmt(self):
        t = self._advance(); mut = self._match(TT.MUT); name = self._expect(TT.IDENT).value
        ann = None
        if self._match(TT.COLON): ann = self._type_expr()
        val = None
        if self._match(TT.ASSIGN): val = self._expr()
        return A.LetStmt(t.line, t.col, name, ann, val, mutable=bool(mut))

    def _const_stmt(self):
        t = self._advance(); name = self._expect(TT.IDENT).value
        ann = None
        if self._match(TT.COLON): ann = self._type_expr()
        self._expect(TT.ASSIGN); val = self._expr()
        return A.ConstStmt(t.line, t.col, name, ann, val)

    def _var_stmt(self):
        """`var name [: type] [= expr]` — mutable variable. When the value is a
        parenthesized expression `var f = (expr)`, it becomes a callable thunk."""
        t = self._advance(); name = self._expect(TT.IDENT).value
        ann = None
        if self._match(TT.COLON): ann = self._type_expr()
        val = None
        if self._match(TT.ASSIGN):
            val = self._expr()
            if isinstance(val, A.ParenExpr):
                val = A.ThunkExpr(val.line, val.col, val.inner)
        return A.LetStmt(t.line, t.col, name, ann, val, mutable=True)

    def _fn_decl(self):
        t = self._advance(); pub = False
        if self._match(TT.PUB): pub = True
        name = self._expect(TT.IDENT).value
        self._expect(TT.LPAREN); params = []
        if not self._at(TT.RPAREN):
            params.append(self._param())
            while self._match(TT.COMMA): params.append(self._param())
        self._expect(TT.RPAREN)
        ret = None
        if self._match(TT.ARROW): ret = self._type_expr()
        body = None
        if self._at(TT.LBRACE, TT.INDENT): body = self._block()
        return A.FnDecl(t.line, t.col, name, params, ret, body, pub=pub)

    def _class_decl(self):
        t = self._advance(); pub = self._match(TT.PUB)
        name = self._expect(TT.IDENT).value
        sup = None; traits = []
        if self._match(TT.EXTENDS): sup = self._type_expr()
        while self._match(TT.WITH): traits.append(self._type_expr())
        members = []
        if self._at(TT.LBRACE, TT.INDENT):
            self._skip_newlines()
            if self._match(TT.LBRACE):
                self._skip_newlines()
                while not self._at(TT.RBRACE, TT.EOF):
                    m = self._stmt(); members.append(m); self._skip_newlines()
                self._expect(TT.RBRACE)
            else:
                self._expect(TT.INDENT)
                while not self._at(TT.DEDENT, TT.EOF):
                    m = self._stmt(); members.append(m); self._skip_newlines()
                self._match(TT.DEDENT)
        return A.ClassDecl(t.line, t.col, name, sup, traits, members, pub=bool(pub))

    def _struct_decl(self):
        t = self._advance(); pub = self._match(TT.PUB)
        name = self._expect(TT.IDENT).value; fields = []; methods = []
        if self._match(TT.LBRACE):
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                if self._at(TT.FN): methods.append(self._fn_decl())
                else:
                    fn = self._expect(TT.IDENT).value
                    self._expect(TT.COLON); tp = self._type_expr()
                    fields.append(A.LetStmt(self._peek().line, self._peek().col, fn, tp, None))
                self._skip_newlines()
            self._expect(TT.RBRACE)
        return A.StructDecl(t.line, t.col, name, fields, methods, pub=bool(pub))

    def _enum_decl(self):
        t = self._advance(); pub = self._match(TT.PUB)
        name = self._expect(TT.IDENT).value; variants = []
        if self._match(TT.LBRACE):
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                vname = self._expect(TT.IDENT).value
                fields = []
                if self._match(TT.LPAREN):
                    if not self._at(TT.RPAREN):
                        fields.append(self._type_expr())
                        while self._match(TT.COMMA): fields.append(self._type_expr())
                    self._expect(TT.RPAREN)
                variants.append(A.FnDecl(t.line, t.col, vname, fields, None, None))
                self._skip_newlines(); self._match(TT.COMMA); self._skip_newlines()
            self._expect(TT.RBRACE)
        return A.EnumDecl(t.line, t.col, name, variants, [], pub=bool(pub))

    def _trait_decl(self):
        t = self._advance(); pub = self._match(TT.PUB)
        name = self._expect(TT.IDENT).value; members = []
        if self._match(TT.LBRACE):
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                members.append(self._fn_decl()); self._skip_newlines()
            self._expect(TT.RBRACE)
        return A.TraitDecl(t.line, t.col, name, members, pub=bool(pub))

    def _impl_decl(self):
        t = self._advance(); target = self._type_expr()
        trait_name = None
        if self._match(TT.FOR): trait_name = self._type_expr()
        methods = []
        if self._match(TT.LBRACE):
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                methods.append(self._fn_decl()); self._skip_newlines()
            self._expect(TT.RBRACE)
        return A.ImplDecl(t.line, t.col, target, trait_name, methods)

    def _return_stmt(self):
        t = self._advance(); val = None
        if not self._at(TT.NEWLINE, TT.EOF, TT.RBRACE, TT.DEDENT): val = self._expr()
        return A.ReturnStmt(t.line, t.col, val)

    def _break_stmt(self):
        t = self._advance(); val = None
        if not self._at(TT.NEWLINE, TT.EOF, TT.RBRACE, TT.DEDENT): val = self._expr()
        return A.BreakStmt(t.line, t.col, val)

    def _continue_stmt(self):
        t = self._advance(); return A.ContinueStmt(t.line, t.col)

    def _defer_stmt(self):
        t = self._advance(); body = self._block()
        return A.DeferStmt(t.line, t.col, body)

    def _if_stmt(self):
        t = self._advance(); cond = self._expr(); then = self._block()
        elifs = []
        while self._match(TT.ELIF):
            ec = self._expr(); eb = self._block(); elifs.append((ec, eb))
        else_ = None
        if self._match(TT.ELSE): else_ = self._block()
        return A.IfStmt(t.line, t.col, cond, then, elifs, else_)

    def _while_stmt(self):
        t = self._advance(); cond = self._expr(); body = self._block()
        return A.WhileStmt(t.line, t.col, cond, body)

    def _for_stmt(self):
        t = self._advance(); mut = self._match(TT.MUT)
        var = self._expect(TT.IDENT).value; self._expect(TT.IN)
        iterable = self._expr(); body = self._block()
        return A.ForInStmt(t.line, t.col, var, iterable, body, mutable=bool(mut))

    def _loop_stmt(self):
        t = self._advance(); body = self._block()
        return A.LoopStmt(t.line, t.col, body)

    def _match_stmt(self):
        t = self._advance(); expr = self._expr(); arms = []
        if self._match(TT.LBRACE):
            self._skip_newlines()
            while not self._at(TT.RBRACE, TT.EOF):
                pat = self._pattern()
                guard = None
                if self._match(TT.IF): guard = self._expr()
                self._expect(TT.FAT_ARROW)
                body = self._expr()
                arms.append(A.MatchArm(pat.line, pat.col, pat, guard, body))
                self._skip_newlines(); self._match(TT.COMMA); self._skip_newlines()
            self._expect(TT.RBRACE)
        return A.MatchStmt(t.line, t.col, expr, arms)

    def _throw_stmt(self):
        t = self._advance(); e = self._expr()
        return A.ThrowStmt(t.line, t.col, e)

    def _try_stmt(self):
        t = self._advance(); body = self._block(); catches = []; finally_ = None
        while self._match(TT.CATCH):
            cn = None
            if self._at(TT.IDENT): cn = self._advance().value
            cb = self._block(); catches.append(A.CatchClause(cb.line, cb.col, cn, cb))
        if self._match(TT.FINALLY): finally_ = self._block()
        return A.TryStmt(t.line, t.col, body, catches, finally_)

    def _assert_stmt(self):
        t = self._advance(); e = self._expr(); msg = None
        if self._match(TT.COMMA): msg = self._expr()
        return A.AssertStmt(t.line, t.col, e, msg)

    def _import_stmt(self):
        t = self._advance()
        # `from "path" import item` / `from name import item`
        if t.type == TT.FROM:
            if self._at(TT.STRING):
                path = self._advance().value
            else:
                path = self._expect(TT.IDENT).value
            self._expect(TT.IMPORT)
            items = []
            if self._match(TT.STAR):
                return A.ImportStmt(t.line, t.col, path, None, [], True)
            while True:
                items.append(self._expect(TT.IDENT).value)
                if not self._match(TT.COMMA): break
            return A.ImportStmt(t.line, t.col, path, None, items)
        # `import name`, `import a.b.c`, `import "file.velice"`, `import a as b`,
        # `import a.*`, `import a.Item`, `import {Item, Other}`?
        if t.type == TT.LBRACE:
            items = []
            while not self._at(TT.RBRACE):
                items.append(self._expect(TT.IDENT).value)
                if not self._match(TT.COMMA): break
            self._expect(TT.RBRACE)
            return A.ImportStmt(t.line, t.col, "", None, items)
        t = self._match(TT.STRING, TT.IDENT)
        if not t:
            p = self._peek()
            raise ParseError(f"Expected import target, got {p.type.name} at line {p.line}", p.line, p.col)
        first = t.value
        alias = None
        wildcard = False
        module_path = first
        if not first.endswith(".velice"):
            parts = [first]
            while self._match(TT.DOT):
                if self._at(TT.STAR):
                    self._advance(); wildcard = True; break
                if self._at(TT.LBRACE):
                    items = []
                    self._advance()
                    while not self._at(TT.RBRACE):
                        items.append(self._expect(TT.IDENT).value)
                        if not self._match(TT.COMMA): break
                    self._expect(TT.RBRACE)
                    return A.ImportStmt(t.line, t.col, first, None, items)
                parts.append(self._expect(TT.IDENT).value)
            module_path = ".".join(parts)
        if self._match(TT.AS):
            alias = self._expect(TT.IDENT).value
        return A.ImportStmt(t.line, t.col, first, alias, [], wildcard, module_path)

    def _type_alias(self):
        t = self._advance(); name = self._expect(TT.IDENT).value
        self._expect(TT.ASSIGN); target = self._type_expr()
        return A.TypeAlias(t.line, t.col, name, target)

    def _mod_decl(self):
        t = self._advance(); name = self._expect(TT.IDENT).value
        body = self._block() if self._at(TT.LBRACE, TT.INDENT) else None
        return A.Block(t.line, t.col, []) if body is None else body

    # ── Patterns ──────────────────────────────────────────────────────────
    def _pattern(self):
        return self._or_pattern()

    def _or_pattern(self):
        left = self._primary_pattern()
        if self._match(TT.PIPE):
            alts = [left]
            while True:
                alts.append(self._primary_pattern())
                if not self._match(TT.PIPE): break
            return A.OrPattern(left.line, left.col, alts)
        return left

    def _primary_pattern(self):
        t = self._peek()
        if t.type == TT.UNDERSCORE or t.value == "_": self._advance(); return A.WildcardPattern(t.line, t.col)
        if t.type == TT.INT or t.type == TT.STRING or t.type == TT.BOOL or t.type == TT.NIL:
            self._advance(); return A.LitPattern(t.line, t.col, t.value)
        if t.type == TT.LBRACKET:
            self._advance(); elems = []
            if not self._at(TT.RBRACKET):
                elems.append(self._pattern())
                while self._match(TT.COMMA): elems.append(self._pattern())
            self._expect(TT.RBRACKET); return A.ArrayPattern(t.line, t.col, elems)
        if t.type == TT.IDENT:
            self._advance(); return A.IdentPattern(t.line, t.col, t.value)
        self._advance(); return A.WildcardPattern(t.line, t.col)

    # ── Types ─────────────────────────────────────────────────────────────
    def _type_expr(self):
        t = self._peek()
        if self._match(TT.IDENT):
            name = t.value
            if self._match(TT.LBRACKET):
                elem = self._type_expr(); self._expect(TT.RBRACKET)
                return A.ArrayType(t.line, t.col, elem)
            return A.TypeName(t.line, t.col, name)
        if self._match(TT.LBRACKET):
            elem = self._type_expr(); self._expect(TT.RBRACKET)
            return A.ArrayType(t.line, t.col, elem)
        if self._match(TT.FN):
            self._expect(TT.LPAREN); params = []
            if not self._at(TT.RPAREN):
                params.append(self._type_expr())
                while self._match(TT.COMMA): params.append(self._type_expr())
            self._expect(TT.RPAREN); ret = None
            if self._match(TT.ARROW): ret = self._type_expr()
            return A.FuncType(t.line, t.col, params, ret)
        return A.TypeName(t.line, t.col, "any")

    # ── Expressions ───────────────────────────────────────────────────────
    def _expr(self):
        return self._pipe_expr()

    def _pipe_expr(self):
        left = self._assign_expr()
        if self._at(TT.PIPE_RIGHT):
            while self._match(TT.PIPE_RIGHT):
                right = self._assign_expr()
                left = A.PipeExpr(left.line, left.col, left, right)
        return left

    def _assign_expr(self):
        left = self._ternary_expr()
        t = self._peek()
        if t.type in (TT.ASSIGN, TT.PLUS_EQ, TT.MINUS_EQ, TT.STAR_EQ, TT.SLASH_EQ, TT.PERCENT_EQ):
            op = self._advance().type.value; val = self._assign_expr()
            return A.Assignment(left.line, left.col, left, val, op if op != "=" else None)
        return left

    def _ternary_expr(self):
        left = self._null_coalesce()
        if self._match(TT.QUESTION):
            then = self._expr(); self._expect(TT.COLON); else_ = self._expr()
            return A.TernaryExpr(left.line, left.col, left, then, else_)
        return left

    def _null_coalesce(self):
        left = self._or_expr()
        while self._match(TT.QUESTION_QUESTION):
            right = self._or_expr(); left = A.NullCoalesce(left.line, left.col, left, right)
        return left

    def _or_expr(self):
        left = self._and_expr()
        while self._at(TT.OR):
            op = self._advance().type.value; right = self._and_expr()
            left = A.BinaryOp(left.line, left.col, left, op, right)
        return left

    def _and_expr(self):
        left = self._comparison()
        while self._at(TT.AND):
            op = self._advance().type.value; right = self._comparison()
            left = A.BinaryOp(left.line, left.col, left, op, right)
        return left

    def _comparison(self):
        left = self._bitwise()
        while self._at(TT.EQ, TT.NEQ, TT.LT, TT.GT, TT.LTE, TT.GTE):
            op = self._advance().type.value; right = self._bitwise()
            left = A.BinaryOp(left.line, left.col, left, op, right)
        return left

    def _bitwise(self):
        left = self._range_expr()
        while self._at(TT.AMP, TT.PIPE, TT.CARET):
            op = self._advance().type.value; right = self._range_expr()
            left = A.BinaryOp(left.line, left.col, left, op, right)
        return left

    def _range_expr(self):
        left = self._add_expr()
        if self._match(TT.DOT_DOT):
            right = self._add_expr()
            return A.BinaryOp(left.line, left.col, left, "..", right)
        return left

    def _add_expr(self):
        left = self._mul_expr()
        while self._at(TT.PLUS, TT.MINUS):
            op = self._advance().type.value; right = self._mul_expr()
            left = A.BinaryOp(left.line, left.col, left, op, right)
        return left

    def _mul_expr(self):
        left = self._power_expr()
        while self._at(TT.STAR, TT.SLASH, TT.PERCENT):
            op = self._advance().type.value; right = self._power_expr()
            left = A.BinaryOp(left.line, left.col, left, op, right)
        return left

    def _power_expr(self):
        left = self._unary_expr()
        if self._match(TT.STARSTAR):
            right = self._power_expr()
            left = A.BinaryOp(left.line, left.col, left, "**", right)
        return left

    def _unary_expr(self):
        t = self._peek()
        if t.type in (TT.BANG, TT.MINUS, TT.PLUS, TT.NOT):
            op = self._advance().type.value; operand = self._unary_expr()
            return A.UnaryOp(t.line, t.col, op, operand, True)
        return self._postfix_expr()

    def _call_arg(self):
        """Parse one call argument. Returns ('kw', name, expr) for keyword
        arguments (``name=expr``) and ('pos', None, expr) otherwise."""
        if self._at(TT.IDENT) and self._peek(1).type == TT.ASSIGN:
            name = self._advance().value
            self._advance()
            return ("kw", name, self._expr())
        return ("pos", None, self._expr())

    def _postfix_expr(self):
        left = self._primary()
        while True:
            if self._match(TT.DOT):
                if self._at(TT.IDENT, TT.INT):
                    pt = self._advance()
                    prop = str(pt.value) if pt.type == TT.INT else pt.value
                    left = A.DotAccess(left.line, left.col, left, prop)
                else:
                    p = self._peek()
                    raise ParseError(f"Expected property name after '.', got {p.type.name} at line {p.line}", p.line, p.col)
            elif self._match(TT.LPAREN):
                args = []; kwargs = {}
                if not self._at(TT.RPAREN):
                    kind, a, v = self._call_arg()
                    if kind == "kw": kwargs[a] = v
                    else: args.append(v)
                    while self._match(TT.COMMA):
                        kind, a, v = self._call_arg()
                        if kind == "kw": kwargs[a] = v
                        else: args.append(v)
                self._expect(TT.RPAREN)
                left = A.Call(left.line, left.col, left, args, kwargs)
            elif self._match(TT.LBRACKET):
                idx = self._expr()
                if self._match(TT.COLON):
                    end = self._expr() if not self._at(TT.RBRACKET) else None
                    self._expect(TT.RBRACKET)
                    left = A.SliceAccess(left.line, left.col, left, idx, end)
                else:
                    self._expect(TT.RBRACKET)
                    left = A.IndexAccess(left.line, left.col, left, idx)
            elif self._match(TT.QUESTION):
                prop = self._expect(TT.IDENT).value
                left = A.DotAccess(left.line, left.col, left, prop)
            elif self._at(TT.LBRACE) and isinstance(left, A.DotAccess):
                # trailing-block call:  obj.prop { ... }
                body = self._block()
                left = A.BlockCall(left.line, left.col, left.obj, left.prop, body)
            else:
                break
        return left

    def _primary(self):
        t = self._peek()
        if t.type == TT.INT: self._advance(); return A.Literal(t.line, t.col, t.value, "int")
        if t.type == TT.FLOAT: self._advance(); return A.Literal(t.line, t.col, t.value, "float")
        if t.type == TT.STRING: self._advance(); return A.Literal(t.line, t.col, t.value, "string")
        if t.type == TT.CHAR: self._advance(); return A.Literal(t.line, t.col, t.value, "char")
        if t.type == TT.BOOL: self._advance(); return A.Literal(t.line, t.col, t.value, "bool")
        if t.type == TT.NIL: self._advance(); return A.Literal(t.line, t.col, None, "nil")
        if t.type == TT.IDENT: self._advance(); return A.Identifier(t.line, t.col, t.value)
        if t.type == TT.SELF: self._advance(); return A.Identifier(t.line, t.col, "self")
        if t.type == TT.ARGU: self._advance(); return A.Identifier(t.line, t.col, "argu")
        if t.type in (TT.TYPEOF, TT.SIZEOF):
            self._advance()
            self._expect(TT.LPAREN); arg = self._expr(); self._expect(TT.RPAREN)
            name = "typeof" if t.type == TT.TYPEOF else "sizeof"
            return A.Call(t.line, t.col, A.Identifier(t.line, t.col, name), [arg])
        if t.type == TT.LPAREN:
            self._advance()
            if self._at(TT.RPAREN): self._advance(); return A.TupleLit(t.line, t.col, [])
            elems = [self._expr()]
            while self._match(TT.COMMA): elems.append(self._expr())
            self._expect(TT.RPAREN)
            if len(elems) == 1: return A.ParenExpr(t.line, t.col, elems[0])
            return A.TupleLit(t.line, t.col, elems)
        if t.type == TT.LBRACKET:
            self._advance(); self._skip_newlines()
            elems = []
            if not self._at(TT.RBRACKET):
                elems.append(self._expr())
                while self._match(TT.COMMA):
                    self._skip_newlines()
                    if self._at(TT.RBRACKET): break
                    elems.append(self._expr())
                self._skip_newlines()
            self._expect(TT.RBRACKET)
            return A.ArrayLit(t.line, t.col, elems)
        if t.type == TT.LBRACE:
            self._advance()
            self._skip_newlines()
            if self._at(TT.RBRACE):
                self._advance()
                return A.MapLit(t.line, t.col, [], [])
            if (self._at(TT.IDENT) or self._at(TT.STRING) or self._at(TT.INT)) and self._peek(1).type == TT.COLON:
                keys = []; vals = []
                while not self._at(TT.RBRACE, TT.EOF):
                    k = self._expr(); self._expect(TT.COLON); v = self._expr()
                    keys.append(k); vals.append(v)
                    self._match(TT.COMMA); self._skip_newlines()
                self._expect(TT.RBRACE)
                return A.MapLit(t.line, t.col, keys, vals)
            if self._at(TT.IDENT) and self._peek(1).type in (TT.RBRACE,):
                self._advance(); self._expect(TT.RBRACE)
                return A.MapLit(t.line, t.col, [], [])
            stmts = []
            while not self._at(TT.RBRACE, TT.EOF):
                stmts.append(self._stmt()); self._skip_newlines()
            self._expect(TT.RBRACE)
            if len(stmts) == 1: return stmts[0]
            return A.Block(t.line, t.col, stmts)
        if t.type == TT.FN:
            self._advance(); self._expect(TT.LPAREN); params = []
            if not self._at(TT.RPAREN):
                params.append(self._param())
                while self._match(TT.COMMA): params.append(self._param())
            self._expect(TT.RPAREN)
            ret = None
            if self._match(TT.ARROW): ret = self._type_expr()
            body = self._block()
            return A.LambdaExpr(t.line, t.col, params, body)
        if t.type == TT.IF:
            return self._if_stmt()
        if t.type == TT.MATCH:
            return self._match_stmt()
        raise ParseError(f"Unexpected token {t.type.name} ({t.value!r}) at line {t.line}", t.line, t.col)
