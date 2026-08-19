import tkinter as tk
from pathlib import Path
import json
import time


# ============================================================
# SETTINGS
# ============================================================

OFFLINE_CLICKS_PER_SECOND = 1000000000000000000000000 * 100000000000000000000000 * 1000000000000000000000000 * 100000000000000000000000 * 1000000000000000000000000 * 100000000000000000000000 * 1000000000000000000000000 * 100000000000000000000000 * 1000000000000000000000000 * 100000000000000000000000 * 1000000000000000000000000 * 100000000000000000000000 * 1000000000000000000000000 * 100000000000000000000000

SAVE_FOLDER = Path("saves")
SAVE_FILE = SAVE_FOLDER / "clickrush.save"

SAVE_FOLDER.mkdir(
    exist_ok=True
)


# ============================================================
# COLORS
# ============================================================

BACKGROUND = "#0b1020"
CARD = "#151c33"
CARD_LIGHT = "#1d2745"

BLUE = "#5865f2"
BLUE_HOVER = "#7289ff"

WHITE = "#ffffff"
LIGHT_TEXT = "#aeb8d0"

GREEN = "#52d273"
GOLD = "#f6c453"


# ============================================================
# SAVE SYSTEM
# ============================================================

def load_save():

    if not SAVE_FILE.exists():
        return 0, 0

    try:

        text = SAVE_FILE.read_text().strip()

        # ----------------------------------------------------
        # SUPPORT OLD SAVE FORMAT
        #
        # Old save:
        # 123
        #
        # New save:
        # {
        #   "clicks": 123,
        #   "last_saved": 1234567890
        # }
        # ----------------------------------------------------

        try:
            old_clicks = int(text)

            return old_clicks, 0

        except ValueError:
            pass

        data = json.loads(text)

        saved_clicks = int(
            data.get(
                "clicks",
                0
            )
        )

        last_saved = float(
            data.get(
                "last_saved",
                0
            )
        )

        return saved_clicks, last_saved

    except (
        OSError,
        ValueError,
        json.JSONDecodeError
    ):

        return 0, 0


def save_game():

    try:

        data = {
            "clicks": clicks,
            "last_saved": time.time()
        }

        SAVE_FILE.write_text(
            json.dumps(
                data,
                indent=4
            )
        )

        save_label.config(
            text="✓ Saved",
            fg=GREEN
        )

    except OSError:

        save_label.config(
            text="Save failed",
            fg="red"
        )


# ============================================================
# LOAD GAME
# ============================================================

clicks, last_saved = load_save()

offline_clicks = 0
offline_seconds = 0


# ============================================================
# CALCULATE OFFLINE CLICKS
# ============================================================

if last_saved > 0:

    current_time = time.time()

    offline_seconds = max(
        0,
        current_time - last_saved
    )

    offline_clicks = int(
        offline_seconds
        * OFFLINE_CLICKS_PER_SECOND
    )

    clicks += offline_clicks


# ============================================================
# WINDOW
# ============================================================

win = tk.Tk()

win.title("ClickRush")
win.geometry("650x720")

win.minsize(
    520,
    600
)

win.configure(
    bg=BACKGROUND
)


# ============================================================
# FUNCTIONS
# ============================================================

def update_score():

    click_label.config(
        text=f"{clicks:,}"
    )


def addClick():

    global clicks

    clicks += 1

    update_score()

    save_game()


def button_enter(event):

    clickButton.config(
        bg=BLUE_HOVER
    )


def button_leave(event):

    clickButton.config(
        bg=BLUE
    )


def on_close():

    save_game()

    win.destroy()


# ============================================================
# HEADER
# ============================================================

header = tk.Frame(
    win,
    bg=BACKGROUND
)

header.pack(
    fill="x",
    padx=30,
    pady=(30, 15)
)


title = tk.Label(
    header,

    text="CLICKRUSH",

    font=(
        "Sans Serif",
        32,
        "bold"
    ),

    bg=BACKGROUND,
    fg=WHITE
)

title.pack()


subtitle = tk.Label(
    header,

    text="Click. Upgrade. Conquer.",

    font=(
        "Sans Serif",
        12
    ),

    bg=BACKGROUND,
    fg=LIGHT_TEXT
)

subtitle.pack(
    pady=(5, 0)
)


# ============================================================
# OFFLINE REWARD
# ============================================================

if offline_clicks > 0:

    offline_frame = tk.Frame(
        win,
        bg=CARD
    )

    offline_frame.pack(
        fill="x",
        padx=40,
        pady=(0, 10)
    )

    offline_title = tk.Label(
        offline_frame,

        text="WELCOME BACK!",

        font=(
            "Sans Serif",
            12,
            "bold"
        ),

        bg=CARD,
        fg=GOLD
    )

    offline_title.pack(
        pady=(12, 3)
    )

    offline_label = tk.Label(
        offline_frame,

        text=f"You earned {offline_clicks:,} clicks while offline!",

        font=(
            "Sans Serif",
            11
        ),

        bg=CARD,
        fg=WHITE
    )

    offline_label.pack(
        pady=(0, 12)
    )


# ============================================================
# MAIN CARD
# ============================================================

card = tk.Frame(
    win,

    bg=CARD,

    highlightthickness=1,
    highlightbackground=CARD_LIGHT
)

card.pack(
    fill="both",
    expand=True,

    padx=40,
    pady=20
)


# ============================================================
# SCORE TITLE
# ============================================================

score_title = tk.Label(
    card,

    text="TOTAL CLICKS",

    font=(
        "Sans Serif",
        12,
        "bold"
    ),

    bg=CARD,
    fg=LIGHT_TEXT
)

score_title.pack(
    pady=(60, 10)
)


# ============================================================
# SCORE
# ============================================================

click_label = tk.Label(
    card,

    text=f"{clicks:,}",

    font=(
        "Sans Serif",
        60,
        "bold"
    ),

    bg=CARD,
    fg=WHITE
)

click_label.pack(
    pady=(0, 35)
)


# ============================================================
# CLICK BUTTON
# ============================================================

clickButton = tk.Button(
    card,

    text="CLICK!",

    command=addClick,

    font=(
        "Sans Serif",
        21,
        "bold"
    ),

    bg=BLUE,
    fg=WHITE,

    activebackground=BLUE_HOVER,
    activeforeground=WHITE,

    borderwidth=0,

    padx=70,
    pady=22,

    cursor="hand2"
)

clickButton.pack(
    pady=10
)


clickButton.bind(
    "<Enter>",
    button_enter
)

clickButton.bind(
    "<Leave>",
    button_leave
)


# ============================================================
# OFFLINE RATE
# ============================================================

rate_label = tk.Label(
    card,

    text=f"Offline income: {OFFLINE_CLICKS_PER_SECOND} click/sec",

    font=(
        "Sans Serif",
        11
    ),

    bg=CARD,
    fg=GOLD
)

rate_label.pack(
    pady=(20, 5)
)


# ============================================================
# SAVE STATUS
# ============================================================

save_label = tk.Label(
    card,

    text="✓ Save loaded",

    font=(
        "Sans Serif",
        11
    ),

    bg=CARD,
    fg=GREEN
)

save_label.pack(
    pady=5
)


# ============================================================
# SAVE LOCATION
# ============================================================

save_path_label = tk.Label(
    card,

    text=f"Save: {SAVE_FILE}",

    font=(
        "Monospace",
        9
    ),

    bg=CARD,
    fg=LIGHT_TEXT
)

save_path_label.pack(
    pady=(5, 20)
)


# ============================================================
# FOOTER
# ============================================================

footer = tk.Label(
    win,

    text="ClickRush • Autosave + Offline Earnings",

    font=(
        "Sans Serif",
        10
    ),

    bg=BACKGROUND,
    fg=LIGHT_TEXT
)

footer.pack(
    pady=(0, 20)
)


# ============================================================
# SAVE OFFLINE REWARD IMMEDIATELY
#
# This prevents reopening the game repeatedly
# from giving the same offline reward.
# ============================================================

save_game()


# ============================================================
# WINDOW CLOSE
# ============================================================

win.protocol(
    "WM_DELETE_WINDOW",
    on_close
)


# ============================================================
# START
# ============================================================

win.mainloop()