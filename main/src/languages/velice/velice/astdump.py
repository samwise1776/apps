"""Serialize the Velice AST into JSON-friendly maps.

Used by the ``parse_source`` builtin so Velice programs can re-read
source code they (or the user) wrote — powering the Code -> Design
direction of Descr1be without fragile regex parsing.
"""
from __future__ import annotations
from velice import ast_nodes as A


def _base(n):
    return {"line": getattr(n, "line", 0), "col": getattr(n, "col", 0)}


def node_to_data(n):
    """Convert an AST node to a JSON-friendly dict (or None)."""
    if n is None:
        return None
    t = type(n)
    b = _base(n)
    if t is A.Literal:
        return {"kind": "literal", "value": n.value, "type": n.kind, **b}
    if t is A.Identifier:
        return {"kind": "ident", "name": n.name, **b}
    if t is A.ParenExpr:
        return {"kind": "paren", "inner": node_to_data(n.inner), **b}
    if t is A.ThunkExpr:
        return {"kind": "thunk", "expr": node_to_data(n.expr), **b}
    if t is A.BinaryOp:
        return {"kind": "binop", "op": n.op, "left": node_to_data(n.left),
                "right": node_to_data(n.right), **b}
    if t is A.UnaryOp:
        return {"kind": "unary", "op": n.op, "operand": node_to_data(n.operand), **b}
    if t is A.Assignment:
        return {"kind": "assign", "target": node_to_data(n.target),
                "value": node_to_data(n.value), "op": n.op, **b}
    if t is A.Call:
        return {"kind": "call", "func": node_to_data(n.func),
                "args": [node_to_data(a) for a in n.args],
                "kwargs": {k: node_to_data(v) for k, v in n.kwargs.items()}, **b}
    if t is A.DotAccess:
        return {"kind": "dot", "obj": node_to_data(n.obj), "prop": n.prop, **b}
    if t is A.BlockCall:
        return {"kind": "blockcall", "obj": node_to_data(n.obj), "prop": n.prop,
                "body": [node_to_data(s) for s in n.body], **b}
    if t is A.IndexAccess:
        return {"kind": "index", "obj": node_to_data(n.obj),
                "index": node_to_data(n.index), **b}
    if t is A.SliceAccess:
        return {"kind": "slice", "obj": node_to_data(n.obj),
                "start": node_to_data(n.start), "end": node_to_data(n.end),
                "step": node_to_data(n.step), **b}
    if t is A.TernaryExpr:
        return {"kind": "ternary", "cond": node_to_data(n.cond),
                "then": node_to_data(n.then), "else": node_to_data(n.else_), **b}
    if t is A.NullCoalesce:
        return {"kind": "coalesce", "left": node_to_data(n.left),
                "right": node_to_data(n.right), **b}
    if t is A.ArrayLit:
        return {"kind": "array", "items": [node_to_data(e) for e in n.elems], **b}
    if t is A.MapLit:
        return {"kind": "map",
                "entries": [{"key": node_to_data(k), "value": node_to_data(v)}
                            for k, v in zip(n.keys, n.vals)], **b}
    if t is A.TupleLit:
        return {"kind": "tuple", "items": [node_to_data(e) for e in n.elems], **b}
    if t is A.InterpString:
        return {"kind": "interp",
                "parts": [p if isinstance(p, str) else node_to_data(p)
                          for p in n.parts], **b}
    if t is A.PipeExpr:
        return {"kind": "pipe", "left": node_to_data(n.left),
                "right": node_to_data(n.right), **b}
    if t is A.LambdaExpr:
        return {"kind": "lambda", "params": [p.name for p in n.params],
                "body": node_to_data(n.body), **b}
    # ── statements ─────────────────────────────────────────────────────
    if t is A.LetStmt:
        return {"kind": "decl", "name": n.name, "mutable": bool(n.mutable),
                "value": node_to_data(n.value), **b}
    if t is A.ConstStmt:
        return {"kind": "const", "name": n.name, "value": node_to_data(n.value), **b}
    if t is A.ExprStmt:
        return {"kind": "expr", "expr": node_to_data(n.expr), **b}
    if t is A.ReturnStmt:
        return {"kind": "return", "value": node_to_data(n.value), **b}
    if t is A.BreakStmt:
        return {"kind": "break", "value": node_to_data(n.value), **b}
    if t is A.ContinueStmt:
        return {"kind": "continue", **b}
    if t is A.Block:
        return {"kind": "block", "statements": [node_to_data(s) for s in n.stmts], **b}
    if t is A.IfStmt:
        return {"kind": "if", "cond": node_to_data(n.cond),
                "then": node_to_data(n.then),
                "elifs": [[node_to_data(c), node_to_data(x)] for c, x in n.elifs],
                "else": node_to_data(n.else_), **b}
    if t is A.WhileStmt:
        return {"kind": "while", "cond": node_to_data(n.cond),
                "body": node_to_data(n.body), **b}
    if t is A.ForInStmt:
        return {"kind": "for", "var": n.var, "iterable": node_to_data(n.iterable),
                "body": node_to_data(n.body), **b}
    if t is A.MatchStmt:
        return {"kind": "match", "expr": node_to_data(n.expr),
                "arms": [{"pattern": node_to_data(a.pattern),
                          "guard": node_to_data(a.guard),
                          "body": node_to_data(a.body)} for a in n.arms], **b}
    if t is A.ThrowStmt:
        return {"kind": "throw", "value": node_to_data(n.expr), **b}
    if t is A.TryStmt:
        return {"kind": "try", "body": node_to_data(n.body),
                "catches": [{"name": c.name, "body": node_to_data(c.body)}
                            for c in n.catches],
                "finally": node_to_data(n.finally_), **b}
    if t is A.AssertStmt:
        return {"kind": "assert", "value": node_to_data(n.expr),
                "msg": node_to_data(n.msg), **b}
    if t is A.FnDecl:
        return {"kind": "fn", "name": n.name,
                "params": [p.name for p in n.params],
                "ret": getattr(n.ret, "name", None) if n.ret else None,
                "body": node_to_data(n.body), **b}
    if t is A.ClassDecl:
        return {"kind": "class", "name": n.name,
                "superclass": getattr(n.superclass, "name", None)
                              if n.superclass else None,
                "body": [node_to_data(m) for m in n.members], **b}
    if t is A.StructDecl:
        return {"kind": "struct", "name": n.name,
                "fields": [f.name for f in n.fields],
                "body": [node_to_data(m) for m in n.methods], **b}
    if t is A.EnumDecl:
        return {"kind": "enum", "name": n.name,
                "variants": [v.name for v in n.variants], **b}
    if t is A.ImportStmt:
        return {"kind": "import", "path": n.path, "alias": n.alias,
                "items": list(n.items), "wildcard": bool(n.wildcard),
                "module_path": n.module_path, **b}
    if t is A.Program:
        return {"kind": "program",
                "statements": [node_to_data(s) for s in n.stmts],
                "source": n.source, **b}
    if t is A.WindowDecl:
        return {"kind": "window", "name": n.name,
                "props": [[k, node_to_data(v)] for k, v in n.props],
                "children": [node_to_data(c) for c in n.children], **b}
    if t is A.WidgetNode:
        return {"kind": "widget", "wtype": n.wtype, "wname": n.wname,
                "props": [[k, node_to_data(v)] for k, v in n.props],
                "events": [[ev, node_to_data(body)] for ev, body in n.events],
                "children": [node_to_data(c) for c in n.children], **b}
    if t is A.RunStmt:
        return {"kind": "run", "name": n.name, **b}
    return {"kind": "unknown", "node": t.__name__, **b}


def parse_source(code):
    """Parse Velice source and return a JSON-friendly AST structure."""
    from velice.lexer import Lexer, LexError
    from velice.parser import Parser, ParseError
    try:
        tokens = Lexer(code).tokenize()
        ast = Parser(tokens, code).parse()
    except LexError as e:
        raise ParseError(f"parse error at line {e.line}, col {e.col}: {e}")
    except ParseError as e:
        raise ParseError(f"parse error at line {e.line}, col {e.col}: {e}")
    return node_to_data(ast)
