local lgi = require("lgi")
local Gdk = lgi.Gdk

local Post = require("post")

local Start = {}

function Start.create(Gtk)

    local css = Gtk.CssProvider()

    css:load_from_data([[
        #startPage {
            background-color: #f4f6f8;
        }

        #startTitle {
            font-family: "Segoe UI", "Arial", sans-serif;
            font-size: 32px;
            font-weight: bold;
            color: #202124;
        }

        #startText {
            font-family: "Segoe UI", "Arial", sans-serif;
            font-size: 17px;
            color: #666666;
        }

        #postButton {
            background-image: none;
            background-color: #4a54ff;

            color: white;

            border: none;
            border-radius: 7px;

            padding: 10px 20px;

            font-size: 15px;
            font-weight: bold;
        }

        #postButton:hover {
            background-color: #3943e6;
        }

        #postButton:active {
            background-color: #2933cc;
        }
    ]])

    Gtk.StyleContext.add_provider_for_screen(
        Gdk.Screen.get_default(),
        css,
        Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
    )

    -- =====================================================
    -- START PAGE
    -- =====================================================

    local page = Gtk.Box {
        orientation = Gtk.Orientation.VERTICAL,
        spacing = 15,

        hexpand = true,
        vexpand = true,

        margin_top = 25,
        margin_bottom = 25,
        margin_start = 30,
        margin_end = 30,

        name = "startPage"
    }

    -- =====================================================
    -- TITLE
    -- =====================================================

    local title = Gtk.Label {
        label = "Welcome to Yourtime",
        xalign = 0,
        name = "startTitle"
    }

    -- =====================================================
    -- TEXT
    -- =====================================================

    local text = Gtk.Label {
        label = "Your feed will go here.",
        xalign = 0,
        name = "startText"
    }

    -- =====================================================
    -- CREATE POST BUTTON
    -- =====================================================

    local postButton = Gtk.Button {
        label = "Create Post",
        name = "postButton"
    }

    -- =====================================================
    -- BUTTON EVENT
    -- =====================================================

    postButton.on_clicked = function()

        Post.open(page)

    end

    -- =====================================================
    -- ADD WIDGETS
    -- =====================================================

    page:pack_start(
        title,
        false,
        false,
        0
    )

    page:pack_start(
        text,
        false,
        false,
        0
    )

    page:pack_start(
        postButton,
        false,
        false,
        0
    )

    return page
end

return Start