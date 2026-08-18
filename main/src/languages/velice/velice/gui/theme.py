"""Theme engine for Velice GUI."""
from dataclasses import dataclass, field


@dataclass
class Theme:
    name: str
    background: str = "#1e1e2e"
    surface: str = "#2a2a3e"
    primary: str = "#7c6cf0"
    secondary: str = "#38d9a9"
    accent: str = "#ffd166"
    text: str = "#e6e6f0"
    text_muted: str = "#9a9ab0"
    error: str = "#f45b69"
    warning: str = "#ffd166"
    success: str = "#38d9a9"
    border: str = "#3a3a52"
    button: str = "#3a3a52"
    button_text: str = "#e6e6f0"
    hover: str = "#45456a"
    input: str = "#23233a"
    font_size: int = 12

    def colors(self):
        return {k: v for k, v in self.__dict__.items()}


LIGHT = Theme(
    name="Light", background="#f5f5f7", surface="#ffffff", text="#1c1c28",
    text_muted="#6a6a7a", border="#d0d0da", button="#e8e8f0",
    button_text="#1c1c28", hover="#dcdcf0", input="#ffffff",
)

DARK = Theme(name="Dark", background="#1e1e2e", surface="#2a2a3e",
             text="#e6e6f0", text_muted="#9a9ab0", border="#3a3a52",
             button="#3a3a52", button_text="#e6e6f0", hover="#45456a",
             input="#23233a")

MIDNIGHT = Theme(name="Midnight", background="#0b1021", surface="#141b33",
                 text="#dbe4ff", text_muted="#7a86b8", border="#223052",
                 button="#1c2a52", button_text="#dbe4ff", hover="#2a3b6e",
                 input="#0f1730", primary="#6f8cff", secondary="#54e0c0")

OCEAN = Theme(name="Ocean", background="#e8f4fb", surface="#ffffff",
              text="#0f2a3d", text_muted="#5a7d92", border="#bfdcec",
              button="#d9eef9", button_text="#0f2a3d", hover="#b8e0f5",
              input="#ffffff", primary="#0f7fb7", secondary="#1ba37d")

FOREST = Theme(name="Forest", background="#eef5ec", surface="#ffffff",
               text="#1c3318", text_muted="#5f7f58", border="#c6dcc0",
               button="#dcebd6", button_text="#1c3318", hover="#c8e2be",
               input="#ffffff", primary="#2f7d32", secondary="#8fbf3f")

SOLARIZED = Theme(name="Solarized", background="#fdf6e3", surface="#eee8d5",
                  text="#073642", text_muted="#657b83", border="#d9cfa8",
                  button="#e6ddbf", button_text="#073642", hover="#e6ddbf",
                  input="#fdf6e3", primary="#268bd2", secondary="#2aa198",
                  accent="#b58900")

NEON = Theme(name="Neon", background="#0d0d1a", surface="#14142a",
             text="#e0e0ff", text_muted="#8a8ac0", border="#33335c",
             button="#1c1c3c", button_text="#e0e0ff", hover="#2a2a66",
             input="#10102a", primary="#00ffd5", secondary="#ff3d81",
             accent="#faff00")

GLASS = Theme(name="Glass", background="#eef0f6", surface="#ffffff",
              text="#333a52", text_muted="#98a0b8", border="#d8dce8",
              button="#eef0f6", button_text="#333a52", hover="#e2e6f2",
              input="#ffffff")

MATERIAL = Theme(name="Material", background="#fafafa", surface="#ffffff",
                 text="#212121", text_muted="#757575", border="#e0e0e0",
                 button="#e0e0e0", button_text="#212121", hover="#d0d0d0",
                 input="#ffffff", primary="#6200ee", secondary="#03dac6",
                 accent="#ffab40")

FLUENT = Theme(name="Fluent", background="#f3f3f3", surface="#ffffff",
               text="#1b1b1b", text_muted="#717171", border="#e5e5e5",
               button="#f3f3f3", button_text="#1b1b1b", hover="#e6e6e6",
               input="#ffffff", primary="#0078d4", secondary="#00b7c3")

THEMES = {
    t.name: t.colors()
    for t in (LIGHT, DARK, MIDNIGHT, OCEAN, FOREST, SOLARIZED, NEON, GLASS,
              MATERIAL, FLUENT,
              Theme("macOS", background="#ececec", surface="#ffffff",
                    text="#1d1d1f", text_muted="#8e8e93", border="#d6d6d6",
                    button="#ffffff", button_text="#1d1d1f", hover="#e8e8ed",
                    input="#ffffff", primary="#007aff", secondary="#34c759"),
              Theme("Windows", background="#f3f3f3", surface="#ffffff",
                    text="#171717", text_muted="#616161", border="#dcdcdc",
                    button="#e1e1e1", button_text="#171717", hover="#d5d5d5",
                    input="#ffffff", primary="#0078d4", secondary="#005fb8"),
              Theme("Linux", background="#ececec", surface="#fafafa",
                    text="#2e3436", text_muted="#888a85", border="#d3d7cf",
                    button="#fafafa", button_text="#2e3436", hover="#e0e0e0",
                    input="#ffffff", primary="#73d216", secondary="#4e9a06"),
              )
}


def names():
    return sorted(THEMES.keys())
