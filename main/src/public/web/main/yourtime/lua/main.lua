local lgi = require("lgi")

local Gtk = lgi.require("Gtk", "3.0")
local Gdk = lgi.Gdk

local Content = require("content")

Gtk.init()

-- =========================================================
-- WINDOW
-- =========================================================

local win = Gtk.Window {
    title = "Yourtime",
    default_width = 1100,
    default_height = 750
}

-- We are making our own title bar
win:set_decorated(false)

-- Optional icon
pcall(function()
    win:set_icon_from_file("yourtime-icon.png")
end)

-- =========================================================
-- WINDOW STATE
-- =========================================================

local maximized = false

-- =========================================================
-- CSS
-- =========================================================

local css = Gtk.CssProvider()

css:load_from_data([[
window {
    background-color: #f5f5f7;
}

#windowRoot {
    background-color: #f5f5f7;
}

/* =========================================================
   TITLE BAR
   ========================================================= */

#titleBar {
    background-color: #ffffff;

    border-bottom: 1px solid #dddddd;

    min-height: 42px;
}

#appTitle {
    font-family: "Segoe UI", "Arial", sans-serif;

    font-size: 15px;
    font-weight: bold;

    color: #202020;
}

/* =========================================================
   WINDOW BUTTONS
   ========================================================= */

#windowButton {
    background-image: none;
    background-color: transparent;

    border: none;
    border-radius: 0;

    min-width: 46px;
    min-height: 40px;

    padding: 0px;

    font-family: "Segoe UI", "Arial", sans-serif;

    font-size: 16px;

    color: #202020;
}

#windowButton:hover {
    background-color: #e9e9e9;
}

#windowButton:active {
    background-color: #dddddd;
}

/* =========================================================
   CLOSE BUTTON
   ========================================================= */

#closeButton {
    background-image: none;
    background-color: transparent;

    border: none;
    border-radius: 0;

    min-width: 46px;
    min-height: 40px;

    padding: 0px;

    font-family: "Segoe UI", "Arial", sans-serif;

    font-size: 18px;

    color: #202020;
}

#closeButton:hover {
    background-color: #e81123;
    color: white;
}

#closeButton:active {
    background-color: #c50f1f;
    color: white;
}
]])

Gtk.StyleContext.add_provider_for_screen(
    Gdk.Screen.get_default(),
    css,
    Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
)

-- =========================================================
-- ROOT
-- =========================================================

local root = Gtk.Box {
    orientation = Gtk.Orientation.VERTICAL,
    spacing = 0,
    name = "windowRoot"
}

-- =========================================================
-- TITLE BAR
-- =========================================================

local titleBar = Gtk.Box {
    orientation = Gtk.Orientation.HORIZONTAL,
    spacing = 0,
    name = "titleBar"
}

-- This area lets us drag the window
local titleArea = Gtk.EventBox()

titleArea:add_events(
    Gdk.EventMask.BUTTON_PRESS_MASK
)

local titleLabel = Gtk.Label {
    label = "  Yourtime",
    xalign = 0,
    name = "appTitle"
}

titleArea:add(titleLabel)

-- =========================================================
-- WINDOW BUTTONS
-- =========================================================

local minimizeButton = Gtk.Button {
    label = "—",
    name = "windowButton"
}

local maximizeButton = Gtk.Button {
    label = "□",
    name = "windowButton"
}

local closeButton = Gtk.Button {
    label = "×",
    name = "closeButton"
}

-- =========================================================
-- MAXIMIZE FUNCTION
-- =========================================================

local function toggleMaximize()

    if maximized then

        win:unmaximize()

        maximized = false

        maximizeButton.label = "□"

    else

        win:maximize()

        maximized = true

        maximizeButton.label = "❐"

    end

end

-- =========================================================
-- MINIMIZE
-- =========================================================

minimizeButton.on_clicked = function()

    win:iconify()

end

-- =========================================================
-- MAXIMIZE / RESTORE
-- =========================================================

maximizeButton.on_clicked = function()

    toggleMaximize()

end

-- =========================================================
-- CLOSE
-- =========================================================

closeButton.on_clicked = function()

    win:destroy()

end

-- =========================================================
-- TITLE BAR MOUSE EVENTS
-- =========================================================

titleArea.on_button_press_event = function(widget, event)

    -- -----------------------------------------------------
    -- Double click title bar
    -- -----------------------------------------------------

    if event.type == Gdk.EventType.DOUBLE_BUTTON_PRESS then

        toggleMaximize()

        return true
    end

    -- -----------------------------------------------------
    -- Drag window with left mouse
    -- -----------------------------------------------------

    if event.button == 1 then

        -- Don't try to drag while maximized
        if not maximized then

            win:begin_move_drag(
                event.button,
                math.floor(event.x_root),
                math.floor(event.y_root),
                event.time
            )

        end

        return true
    end

    return false
end

-- =========================================================
-- BUILD TITLE BAR
-- =========================================================

titleBar:pack_start(
    titleArea,
    true,
    true,
    0
)

titleBar:pack_end(
    closeButton,
    false,
    false,
    0
)

titleBar:pack_end(
    maximizeButton,
    false,
    false,
    0
)

titleBar:pack_end(
    minimizeButton,
    false,
    false,
    0
)

-- =========================================================
-- YOURTIME CONTENT
-- =========================================================

local content = Content.create(Gtk)

-- =========================================================
-- BUILD MAIN WINDOW
-- =========================================================

root:pack_start(
    titleBar,
    false,
    false,
    0
)

root:pack_start(
    content,
    true,
    true,
    0
)

win:add(root)

-- =========================================================
-- CLOSE APPLICATION
-- =========================================================

win.on_destroy = function()

    Gtk.main_quit()

end

-- =========================================================
-- SHOW APPLICATION
-- =========================================================

win:show_all()

Gtk.main()