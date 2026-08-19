local lgi = require("lgi")
local Gdk = lgi.Gdk

local Start = require("start")

local Content = {}

function Content.create(Gtk)

    local css = Gtk.CssProvider()

    css:load_from_data([[
        #contentRoot {
            background-color: #f4f6f8;
        }

        #title {
            font-family: "Segoe UI", "Arial", sans-serif;
            font-size: 42px;
            font-weight: bold;
            color: #202124;
        }

        #subtitle {
            font-family: "Segoe UI", "Arial", sans-serif;
            font-size: 18px;
            color: #666666;
        }

        #startButton {
            background-image: none;
            background-color: #4a54ff;

            color: white;

            border: none;
            border-radius: 8px;

            padding: 12px 28px;

            font-family: "Segoe UI", "Arial", sans-serif;
            font-size: 17px;
            font-weight: bold;
        }

        #startButton:hover {
            background-color: #3943e6;
        }

        #startButton:active {
            background-color: #2933cc;
        }
    ]])

    Gtk.StyleContext.add_provider_for_screen(
        Gdk.Screen.get_default(),
        css,
        Gtk.STYLE_PROVIDER_PRIORITY_APPLICATION
    )

    local main = Gtk.Box {
        orientation = Gtk.Orientation.VERTICAL,
        spacing = 20,

        margin_top = 40,
        margin_bottom = 40,
        margin_start = 40,
        margin_end = 40,

        name = "contentRoot"
    }

    local title = Gtk.Label {
        label = "Yourtime",
        xalign = 0,
        name = "title"
    }

    local subtitle = Gtk.Label {
        label = "Share your time with the world.",
        xalign = 0,
        name = "subtitle"
    }

    local startButton = Gtk.Button {
        label = "Start",
        name = "startButton"
    }

    main:pack_start(
        title,
        false,
        false,
        0
    )

    main:pack_start(
        subtitle,
        false,
        false,
        0
    )

    main:pack_start(
        startButton,
        false,
        false,
        0
    )

    startButton.on_clicked = function()

        -- Remove everything currently inside main
        local children = main:get_children()

        for _, child in ipairs(children) do
            main:remove(child)
        end

        -- Load the Start screen
        local startPage = Start.create(Gtk)

        main:pack_start(
            startPage,
            true,
            true,
            0
        )

        main:show_all()
    end

    return main
end

return Content