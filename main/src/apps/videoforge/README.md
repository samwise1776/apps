# VideoForge Studio

A professional desktop YouTube video editor and screen recorder for Linux
(JavaFX + FFmpeg). Multi-track timeline editing, real-time preview, effects,
keyframe animation, screen/microphone/webcam recording, FFmpeg rendering and
YouTube upload — no cloud dependency.

## Features

- **Multi-track timeline** — video, audio, text, image and effect tracks with
  z-order, drag-to-move, edge trimming, snapping, box selection, zoom, markers,
  in/out points, ripple delete.
- **Non-destructive editing** — trim/cut/split only adjust source in/out
  points; "Restore Clip (Uncut)" brings back the original media.
- **Real preview** — FFmpeg-decoded frame windows composited with text, images
  and effects in Java2D; play, pause, step, scrub, full/half/quarter quality.
- **Keyframes** — animate position, scale, rotation, opacity, volume and blur
  with linear / ease-in / ease-out / ease-in-out interpolation.
- **Effects** — blur, color correction (contrast/brightness/saturation/gamma/
  temperature/tint, B&W, sepia), crop, chroma key, transitions (fade/cross).
- **Screen recorder** — x11grab capture with optional microphone, system audio
  and webcam picture-in-picture; countdown timer; import recordings directly
  into the project.
- **Export** — presets for YouTube 1080p/1440p/4K, Shorts, web (VP9) and
  custom settings; H.264/H.265/VP9/AV1 with CRF or bitrate control; live
  progress and FFmpeg log.
- **YouTube upload** — OAuth device flow with your own Google client ID,
  resumable resumable upload of rendered videos with title/description/tags/
  privacy and live progress.
- **Project safety** — `.vforge` JSON projects, autosave with crash recovery,
  undo/redo with full history window, media library persistence.

## Requirements

- Linux with an X11 session
- JDK 21 or newer
- Maven 3.8+
- FFmpeg with `libx264`, `aac`, `x11grab`, `pulse`, `v4l2`, `drawtext`
  (Ubuntu/Debian: `sudo apt install ffmpeg`)
- For webcam capture: a `/dev/video0` device
- For system audio: a running PulseAudio/PipeWire-Pulse server

## Build & run

```bash
./run.sh          # compile and launch
./run.sh compile  # compile only
./run.sh package  # build a self-contained jlink runtime
```

Workspace data (projects, cache, recordings, exports, config, logs) defaults to
`~/.videoforge`. Override it with:

```bash
VIDEOF_BASE="$HOME/videoforge-data" ./run.sh
```

## Quick start

1. Launch `./run.sh`. If FFmpeg is missing, use Settings → Dependency Check.
2. Import media (drag files in, or File → Import Media), then double-click or
   drag a clip onto the timeline.
3. Split, trim, add text, effects and keyframes; preview with Space.
4. Recording → Screen Recorder (F9) to capture, or export:
   Export → Export Video (Ctrl+Shift+R), pick a preset, choose a file.
5. YouTube → Upload to render directly to your channel (Setup tab first).

## Keyboard shortcuts

| Keys | Action |
|------|--------|
| Space | Play / pause |
| Left / Right | Previous / next frame |
| Shift+Left / Right | Back / forward 10 frames |
| Home / End | Start / end of timeline |
| S | Split at playhead |
| J | Join selected clips |
| I / O | Set in / out point |
| M | Markers window |
| Ctrl+Z / Ctrl+Y | Undo / Redo |
| Ctrl+C / V / X | Copy / Paste / Cut |
| Ctrl+D | Duplicate |
| Ctrl+A | Select all |
| Ctrl+T | Add text clip |
| Ctrl+S / Ctrl+Shift+S | Save / Save As |
| Ctrl+O / Ctrl+N | Open / New |
| Ctrl+I | Import media |
| Ctrl+Shift+R | Export |
| F9 | Screen recorder |
| Ctrl+Scroll | Zoom timeline |

## Architecture

```
videoforge
├── app          Application entry point, crash recovery
├── ui           MainWindow, MediaPanel, PreviewPanel, TimelineView, InspectorPanel,
│                Toolbar, Export/Recorder/YouTube/Settings/History/Shortcuts/About/
│                Markers/DependencyCheck windows, theme
├── project      VideoProject, .vforge serialization, ProjectManager, ExportSettings
├── timeline     Timeline, Track, TimelineClip, Keyframe, Interpolation, Marker, Timecode
├── media        MediaLibrary (persistent), MediaFile, thumbnails
├── effects      Blur/Color/Crop/ChromaKey/Text/Background effects
├── editing      TimelineOperations, SnapEngine
├── rendering    FFmpegManager, PreviewEngine, RenderEngine (filter-graph composer)
├── recording    ScreenRecorder (x11grab + pulse + v4l2)
├── youtube      YouTubeManager (OAuth device flow + resumable upload)
├── undo         UndoManager (timeline & clip snapshots)
├── config       AppConfig, workspace layout
├── utils        Time/File/Process helpers
└── logging      AppLog
```

Timeline times are stored in microseconds. Rendering builds one FFmpeg filter
graph: background → video/image overlays (trim, speed, crop, color, blur,
chroma, transforms, keyframe expressions) → text overlays → audio mix
(adelay + amix), streamed through libx264/libx265/VP9/AV1.

## Notes & limitations

- Animated opacity is approximated with fades; typewriter text is approximated
  with fade/slide animations.
- The YouTube integration requires you to create your own OAuth client ID in
  Google Cloud Console (YouTube Data API v3). No credentials are bundled.
- Text rendering uses the first `.ttf` font found on the system.
