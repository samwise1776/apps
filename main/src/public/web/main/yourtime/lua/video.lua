local lgi = require("lgi")

local Gtk = lgi.require("Gtk", "3.0")
local Gst = lgi.require("Gst", "1.0")
local Gio = lgi.Gio

Gtk.init()
Gst.init()

-- =========================================================
-- VIDEO PATH FROM JAVA
-- =========================================================

local videoPath = arg[1]

if not videoPath then
    print("ERROR: No video was provided.")
    return
end

print(
    "Lua received video: "
    .. videoPath
)

-- =========================================================
-- WINDOW
-- =========================================================

local win = Gtk.Window {
    title = "Yourtime - Video",
    default_width = 900,
    default_height = 650
}

-- =========================================================
-- MAIN LAYOUT
-- =========================================================

local main = Gtk.Box {
    orientation = Gtk.Orientation.VERTICAL,
    spacing = 10,

    margin_top = 10,
    margin_bottom = 10,
    margin_start = 10,
    margin_end = 10
}

-- =========================================================
-- CREATE GSTREAMER PLAYER
-- =========================================================

local player = Gst.ElementFactory.make(
    "playbin",
    "player"
)

if not player then
    print("ERROR: Could not create playbin.")
    return
end

-- =========================================================
-- CREATE GTK VIDEO SINK
-- =========================================================

local videoSink = Gst.ElementFactory.make(
    "gtksink",
    "videoSink"
)

if not videoSink then
    print("ERROR: Could not create gtksink.")
    print("Install gstreamer1.0-gtk3.")
    return
end

-- =========================================================
-- IMPORTANT FIX
-- =========================================================
--
-- WRONG:
--
-- videoSink:get_property("widget")
--
-- LGI lets us access GObject properties directly.
-- =========================================================

local videoWidget = videoSink.widget

if not videoWidget then
    print("ERROR: gtksink did not provide a GTK widget.")
    return
end

-- Make video fill available space
videoWidget.hexpand = true
videoWidget.vexpand = true

-- =========================================================
-- CONNECT VIDEO SINK TO PLAYER
-- =========================================================

player.video_sink = videoSink

-- =========================================================
-- CONVERT FILE PATH TO URI
-- =========================================================

local videoFile = Gio.File.new_for_path(
    videoPath
)

local videoUri = videoFile:get_uri()

print(
    "Video URI: "
    .. videoUri
)

player.uri = videoUri

-- =========================================================
-- CONTROLS
-- =========================================================

local controls = Gtk.Box {
    orientation = Gtk.Orientation.HORIZONTAL,
    spacing = 10
}

local playButton = Gtk.Button {
    label = "Play"
}

local pauseButton = Gtk.Button {
    label = "Pause"
}

local stopButton = Gtk.Button {
    label = "Stop"
}

-- =========================================================
-- PLAY
-- =========================================================

playButton.on_clicked = function()

    player:set_state(
        Gst.State.PLAYING
    )

end

-- =========================================================
-- PAUSE
-- =========================================================

pauseButton.on_clicked = function()

    player:set_state(
        Gst.State.PAUSED
    )

end

-- =========================================================
-- STOP
-- =========================================================

stopButton.on_clicked = function()

    player:set_state(
        Gst.State.NULL
    )

end

-- =========================================================
-- BUILD CONTROLS
-- =========================================================

controls:pack_start(
    playButton,
    false,
    false,
    0
)

controls:pack_start(
    pauseButton,
    false,
    false,
    0
)

controls:pack_start(
    stopButton,
    false,
    false,
    0
)

-- =========================================================
-- BUILD WINDOW
-- =========================================================

main:pack_start(
    videoWidget,
    true,
    true,
    0
)

main:pack_start(
    controls,
    false,
    false,
    0
)

win:add(main)

-- =========================================================
-- CLOSE
-- =========================================================

win.on_destroy = function()

    player:set_state(
        Gst.State.NULL
    )

    Gtk.main_quit()

end

-- =========================================================
-- SHOW
-- =========================================================

win:show_all()

-- Start automatically
player:set_state(
    Gst.State.PLAYING
)

Gtk.main()