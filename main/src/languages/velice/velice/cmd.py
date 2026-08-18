"""Velice ``cmd`` module: run terminal commands from Velice.

    import cmd
    cmd.write("echo Hello")     # runs `echo Hello`, prints its output
    a = cmd.write("ls -la")     # captures the output instead (no printing)
    print(a)

``write`` runs a command and, when the call stands alone as a statement,
prints its output to the console. When the result is captured (assigned to a
variable, passed as an argument, ...) the output is returned as a string
instead of being printed. ``capture`` always returns the output string
silently.
"""
import subprocess
import sys

from velice.interpreter import VLFunction, VeliceError


def _stream(text, file=None):
    if not text:
        return
    if not text.endswith("\n"):
        text += "\n"
    print(text, end="", file=file)


def _run(command):
    if command is None:
        return 0, "", ""
    try:
        result = subprocess.run(str(command), shell=True, capture_output=True, text=True)
    except OSError as error:
        raise VeliceError(f"cmd: command failed: {error}") from error
    return result.returncode, result.stdout, result.stderr


def write(interp, command):
    """Run ``command`` through the shell.

    Standalone call: prints the output to the console. Captured (assigned to a
    variable, used inside an expression): returns the output string without
    printing.
    """
    _code, stdout, stderr = _run(command)
    if getattr(interp, "_discard_result", False):
        _stream(stdout)
        _stream(stderr, file=sys.stderr)
    return stdout + stderr


def capture(command):
    """Run ``command`` through the shell and return its output silently."""
    _code, stdout, stderr = _run(command)
    return stdout + stderr


write._wants_interp = True

EXPORTS = {"write": write, "capture": capture}


def make_builtin(interp, name, obj):
    """Wrap a Python callable so Velice can call it as a native function."""
    if not callable(obj):
        return obj

    def run(i, args, kwargs):
        if getattr(obj, "_wants_interp", False):
            return obj(i, *args, **kwargs)
        return obj(*args, **kwargs)

    return VLFunction(name, [], None, None, is_native=True, native_fn=run)


__all__ = list(EXPORTS) + ["make_builtin", "EXPORTS"]
