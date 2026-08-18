"""Velice AST node definitions – all nodes are frozen dataclasses."""
from __future__ import annotations
from dataclasses import dataclass, field
from typing import Any, Optional

@dataclass(frozen=True)
class Node:
    line: int; col: int

# ── Expressions ──────────────────────────────────────────────────────────
@dataclass(frozen=True)
class Literal(Node):
    value: Any; kind: str = "auto"

@dataclass(frozen=True)
class Identifier(Node):
    name: str

@dataclass(frozen=True)
class BinaryOp(Node):
    left: Node; op: str; right: Node

@dataclass(frozen=True)
class UnaryOp(Node):
    op: str; operand: Node; prefix: bool = True

@dataclass(frozen=True)
class Assignment(Node):
    target: Node; value: Node; op: Optional[str] = None

@dataclass(frozen=True)
class Call(Node):
    func: Node; args: list = field(default_factory=list); kwargs: dict = field(default_factory=dict)

@dataclass(frozen=True)
class DotAccess(Node):
    obj: Node; prop: str

@dataclass(frozen=True)
class BlockCall(Node):
    """`obj.prop { stmts }` – trailing-block method call (GUI events)."""
    obj: Node; prop: str; body: list

@dataclass(frozen=True)
class IndexAccess(Node):
    obj: Node; index: Node

@dataclass(frozen=True)
class SliceAccess(Node):
    obj: Node; start: Optional[Node] = None; end: Optional[Node] = None; step: Optional[Node] = None

@dataclass(frozen=True)
class LambdaExpr(Node):
    params: list = field(default_factory=list); body: Node = None

@dataclass(frozen=True)
class TernaryExpr(Node):
    cond: Node; then: Node; else_: Node

@dataclass(frozen=True)
class NullCoalesce(Node):
    left: Node; right: Node

@dataclass(frozen=True)
class ArrayLit(Node):
    elems: list = field(default_factory=list)

@dataclass(frozen=True)
class MapLit(Node):
    keys: list = field(default_factory=list); vals: list = field(default_factory=list)

@dataclass(frozen=True)
class TupleLit(Node):
    elems: list = field(default_factory=list)

@dataclass(frozen=True)
class InterpString(Node):
    parts: list = field(default_factory=list)

@dataclass(frozen=True)
class PipeExpr(Node):
    left: Node; right: Node

# ── Statements ───────────────────────────────────────────────────────────
@dataclass(frozen=True)
class LetStmt(Node):
    name: str; type_ann: Optional[Node] = None; value: Optional[Node] = None; mutable: bool = False

@dataclass(frozen=True)
class ConstStmt(Node):
    name: str; type_ann: Optional[Node] = None; value: Optional[Node] = None

@dataclass(frozen=True)
class ExprStmt(Node):
    expr: Node

@dataclass(frozen=True)
class ReturnStmt(Node):
    value: Optional[Node] = None

@dataclass(frozen=True)
class BreakStmt(Node):
    value: Optional[Node] = None

@dataclass(frozen=True)
class ContinueStmt(Node):
    pass

@dataclass(frozen=True)
class DeferStmt(Node):
    body: Node

@dataclass(frozen=True)
class Block(Node):
    stmts: list = field(default_factory=list)

@dataclass(frozen=True)
class IfStmt(Node):
    cond: Node; then: Node; elifs: list = field(default_factory=list); else_: Optional[Node] = None

@dataclass(frozen=True)
class WhileStmt(Node):
    cond: Node; body: Node

@dataclass(frozen=True)
class ForInStmt(Node):
    var: str; iterable: Node; body: Node; mutable: bool = False

@dataclass(frozen=True)
class LoopStmt(Node):
    body: Node

@dataclass(frozen=True)
class MatchStmt(Node):
    expr: Node; arms: list = field(default_factory=list)

@dataclass(frozen=True)
class MatchArm(Node):
    pattern: Node; guard: Optional[Node] = None; body: Optional[Node] = None

@dataclass(frozen=True)
class ThrowStmt(Node):
    expr: Node

@dataclass(frozen=True)
class TryStmt(Node):
    body: Node; catches: list = field(default_factory=list); finally_: Optional[Node] = None

@dataclass(frozen=True)
class CatchClause(Node):
    name: Optional[str]; body: Node

@dataclass(frozen=True)
class AssertStmt(Node):
    expr: Node; msg: Optional[Node] = None

@dataclass(frozen=True)
class FnDecl(Node):
    name: str; params: list = field(default_factory=list)
    ret: Optional[Node] = None; body: Optional[Node] = None
    pub: bool = False; async_: bool = False; generics: list = field(default_factory=list)

@dataclass(frozen=True)
class ClassDecl(Node):
    name: str; superclass: Optional[Node] = None; traits: list = field(default_factory=list)
    members: list = field(default_factory=list); pub: bool = False

@dataclass(frozen=True)
class StructDecl(Node):
    name: str; fields: list = field(default_factory=list); methods: list = field(default_factory=list); pub: bool = False

@dataclass(frozen=True)
class EnumDecl(Node):
    name: str; variants: list = field(default_factory=list); methods: list = field(default_factory=list); pub: bool = False

@dataclass(frozen=True)
class TraitDecl(Node):
    name: str; members: list = field(default_factory=list); pub: bool = False

@dataclass(frozen=True)
class ImplDecl(Node):
    target: Node; trait_name: Optional[Node] = None; methods: list = field(default_factory=list)

@dataclass(frozen=True)
class ImportStmt(Node):
    path: str; alias: Optional[str] = None; items: list = field(default_factory=list)
    wildcard: bool = False; module_path: str = ""

@dataclass(frozen=True)
class TypeAlias(Node):
    name: str; target: Node

@dataclass(frozen=True)
class Program(Node):
    stmts: list = field(default_factory=list); source: str = ""

# ── Expressions ───────────────────────────────────────────────────────────
@dataclass(frozen=True)
class ParenExpr(Node):
    inner: Node

@dataclass(frozen=True)
class ThunkExpr(Node):
    """Deferred callable value: `var f = (expr)` -> `f()` runs expr."""
    expr: Node

# ── GUI ────────────────────────────────────────────────────────────────
@dataclass(frozen=True)
class WindowDecl(Node):
    name: str; props: list = field(default_factory=list); children: list = field(default_factory=list)

@dataclass(frozen=True)
class WidgetNode(Node):
    wtype: str; wname: Any = None
    props: list = field(default_factory=list)
    events: list = field(default_factory=list)
    children: list = field(default_factory=list)

@dataclass(frozen=True)
class RunStmt(Node):
    name: str

# ── Patterns ─────────────────────────────────────────────────────────────
@dataclass(frozen=True)
class LitPattern(Node):
    value: Any

@dataclass(frozen=True)
class IdentPattern(Node):
    name: str

@dataclass(frozen=True)
class WildcardPattern(Node):
    pass

@dataclass(frozen=True)
class ArrayPattern(Node):
    elems: list = field(default_factory=list)

@dataclass(frozen=True)
class TuplePattern(Node):
    elems: list = field(default_factory=list)

@dataclass(frozen=True)
class OrPattern(Node):
    alts: list = field(default_factory=list)

# ── Types (used in annotations, stored as strings for now) ───────────────
@dataclass(frozen=True)
class TypeName(Node):
    name: str

@dataclass(frozen=True)
class ArrayType(Node):
    elem: Node

@dataclass(frozen=True)
class MapType(Node):
    key: Node; val: Node

@dataclass(frozen=True)
class FuncType(Node):
    params: list = field(default_factory=list); ret: Optional[Node] = None

@dataclass(frozen=True)
class NullableType(Node):
    inner: Node
