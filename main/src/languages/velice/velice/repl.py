"""Velice REPL – interactive read-eval-print loop."""
import sys
from velice.lexer import Lexer, TT
from velice.parser import Parser
from velice.interpreter import Interpreter

def run_repl():
    interp = Interpreter()
    print("Velice REPL v0.1.0  (type 'exit' or Ctrl+D to quit)")
    print()
    while True:
        try:
            line = input("velice> ")
        except (EOFError, KeyboardInterrupt):
            print("\nBye!"); break
        if line.strip() in ("exit", "quit", "q"): break
        if not line.strip(): continue
        try:
            lexer = Lexer(line)
            tokens = lexer.tokenize()
            parser = Parser(tokens, line)
            ast = parser.parse()
            result = interp.run(ast)
            if result is not None:
                print(f"=> {interp._to_str(result)}")
        except Exception as e:
            print(f"\033[91mError: {e}\033[0m", file=sys.stderr)
