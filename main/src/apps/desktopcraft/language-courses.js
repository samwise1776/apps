(function () {
  const languageTracks = [
    {
      id: "kotlin-compose", title: "Kotlin Compose Desktop Apps", shortTitle: "Compose Desktop",
      language: "kotlin", languageLabel: "Kotlin", fileName: "Main.kt",
      description: "Build modern Kotlin desktop applications with Compose, state, coroutines, files, and terminal tooling.",
      apis: ["application", "Window", "Text", "Button", "TextField", "Column", "Row", "LazyColumn", "remember", "mutableStateOf", "MaterialTheme", "LaunchedEffect", "Dialog", "MenuBar", "Canvas", "CoroutineScope", "File", "Gradle", "kotlinc", "java -jar"]
    },
    {
      id: "rust-egui", title: "Rust egui Desktop Apps", shortTitle: "Rust egui",
      language: "rust", languageLabel: "Rust", fileName: "src/main.rs",
      description: "Create fast native tools in Rust with egui, ownership-aware state, Cargo, testing, and terminal workflows.",
      apis: ["eframe::App", "egui::CentralPanel", "ui.label", "ui.button", "TextEdit", "ComboBox", "ScrollArea", "Grid", "Window", "Context", "Response", "Vec", "Option", "Result", "serde", "std::fs", "Command", "cargo run", "cargo test", "cargo build --release"]
    },
    {
      id: "go-fyne", title: "Go Fyne Desktop Apps", shortTitle: "Go Fyne",
      language: "go", languageLabel: "Go", fileName: "main.go",
      description: "Build portable Go desktop utilities with Fyne widgets, layouts, goroutines, files, and command-line tools.",
      apis: ["app.New", "NewWindow", "widget.NewLabel", "widget.NewButton", "widget.NewEntry", "container.NewVBox", "container.NewHBox", "container.NewGrid", "widget.NewList", "widget.NewTable", "dialog.ShowInformation", "canvas", "binding", "goroutine", "channel", "os.ReadFile", "exec.Command", "go run", "go test", "go build"]
    },
    {
      id: "dart-flutter", title: "Dart Flutter Desktop Apps", shortTitle: "Flutter Desktop",
      language: "dart", languageLabel: "Dart", fileName: "lib/main.dart",
      description: "Develop polished Flutter desktop software with widgets, state, async data, tests, and terminal commands.",
      apis: ["runApp", "MaterialApp", "Scaffold", "Text", "ElevatedButton", "TextField", "Column", "Row", "ListView", "DataTable", "StatefulWidget", "setState", "FutureBuilder", "StreamBuilder", "Navigator", "showDialog", "File", "flutter run", "flutter test", "flutter build"]
    },
    {
      id: "swift-swiftui", title: "Swift SwiftUI Desktop Apps", shortTitle: "SwiftUI",
      language: "swift", languageLabel: "Swift", fileName: "App.swift",
      description: "Create native macOS apps with SwiftUI views, bindings, concurrency, persistence, and terminal builds.",
      apis: ["App", "WindowGroup", "Text", "Button", "TextField", "VStack", "HStack", "List", "Table", "NavigationStack", "@State", "@Binding", "Observable", "Task", "async/await", "FileManager", "Menu", "swift run", "swift test", "swift build"]
    },
    {
      id: "ruby-shoes", title: "Ruby Shoes Desktop Apps", shortTitle: "Ruby Shoes",
      language: "ruby", languageLabel: "Ruby", fileName: "app.rb",
      description: "Learn expressive Ruby desktop development with Shoes controls, events, files, tests, and terminal habits.",
      apis: ["Shoes.app", "para", "button", "edit_line", "edit_box", "stack", "flow", "list_box", "check", "radio", "progress", "image", "timer", "alert", "ask", "File", "JSON", "ruby", "bundle exec", "rake test"]
    },
    {
      id: "php-nativephp", title: "PHP NativePHP Desktop Apps", shortTitle: "NativePHP",
      language: "php", languageLabel: "PHP", fileName: "app/Providers/NativeAppServiceProvider.php",
      description: "Turn PHP and Laravel skills into desktop applications with NativePHP, events, storage, testing, and Artisan.",
      apis: ["Window", "Menu", "Dialog", "Notification", "Clipboard", "Shell", "Process", "Event", "Listener", "Route", "Blade", "Livewire", "Model", "Validation", "Storage", "SQLite", "Pest", "php artisan native:serve", "php artisan test", "php artisan native:build"]
    },
    {
      id: "typescript-electron", title: "TypeScript Electron Desktop Apps", shortTitle: "Electron TypeScript",
      language: "typescript", languageLabel: "TypeScript", fileName: "src/main.ts",
      description: "Build type-safe Electron apps with secure IPC, modern UI patterns, tests, packaging, and terminal scripts.",
      apis: ["BrowserWindow", "app.whenReady", "HTMLElement", "HTMLButtonElement", "HTMLInputElement", "addEventListener", "contextBridge", "ipcRenderer.invoke", "ipcMain.handle", "Menu", "Tray", "Notification", "dialog", "fs/promises", "Result type", "Vitest", "Electron Forge", "npm run dev", "npm test", "npm run make"]
    },
    {
      id: "lua-love", title: "Lua LÖVE Desktop Apps", shortTitle: "Lua LÖVE",
      language: "lua", languageLabel: "Lua", fileName: "main.lua",
      description: "Create interactive Lua desktop experiences with LÖVE callbacks, state, files, testing, and terminal commands.",
      apis: ["love.load", "love.update", "love.draw", "love.keypressed", "love.mousepressed", "love.graphics.print", "love.graphics.rectangle", "love.graphics.newFont", "love.window.setTitle", "love.filesystem", "table", "metatable", "module", "timer", "state machine", "JSON", "busted", "love .", "lua", "zip"]
    },
    {
      id: "racket-gui", title: "Racket GUI Desktop Apps", shortTitle: "Racket GUI",
      language: "racket", languageLabel: "Racket", fileName: "main.rkt",
      description: "Build functional desktop utilities with Racket GUI, callbacks, data, tests, packages, and terminal tools.",
      apis: ["frame%", "message%", "button%", "text-field%", "editor-canvas%", "vertical-panel%", "horizontal-panel%", "list-box%", "choice%", "check-box%", "radio-box%", "menu-bar%", "dialog%", "timer%", "class", "send", "file", "racket", "raco test", "raco exe"]
    }
  ];

  const modules = [
    "Language and window foundations",
    "Controls and responsive layout",
    "Events, state, and validation",
    "Collections, files, and persistence",
    "Async work, quality, and accessibility",
    "Terminal tools and project delivery"
  ];
  const projects = ["utility dashboard", "notes desk", "task tracker", "budget tool", "file organizer", "timer", "contact book", "study planner", "media queue", "system monitor"];
  const skills = ["Create", "Configure", "Connect", "Validate", "Update", "Organize", "Test", "Debug", "Refactor", "Ship"];
  const escape = (value) => String(value).replaceAll("\\", "\\\\").replaceAll('"', '\\"').replaceAll("\n", " ");

  function sampleCode(course, title, api, starter) {
    const values = { title: escape(title), api: escape(api), starter: escape(starter) };
    const samples = {
      kotlin: `import androidx.compose.material.*\nimport androidx.compose.runtime.*\nimport androidx.compose.ui.window.*\n\nfun main() = application {\n    var status by remember { mutableStateOf("Ready") }\n    Window(onCloseRequest = ::exitApplication, title = "${values.title}") {\n        Button(onClick = { status = "${values.starter}" }) { Text("Try ${values.api}") }\n        Text(status)\n    }\n}`,
      rust: `use eframe::egui;\n\n#[derive(Default)]\nstruct DesktopApp { status: String }\nimpl eframe::App for DesktopApp {\n    fn update(&mut self, ctx: &egui::Context, _: &mut eframe::Frame) {\n        egui::CentralPanel::default().show(ctx, |ui| {\n            if ui.button("Try ${values.api}").clicked() { self.status = "${values.starter}".into(); }\n            ui.label(&self.status);\n        });\n    }\n}`,
      go: `package main\n\nimport (\n    "fyne.io/fyne/v2/app"\n    "fyne.io/fyne/v2/container"\n    "fyne.io/fyne/v2/widget"\n)\nfunc main() {\n    a := app.New(); w := a.NewWindow("${values.title}")\n    status := widget.NewLabel("Ready")\n    action := widget.NewButton("Try ${values.api}", func() { status.SetText("${values.starter}") })\n    w.SetContent(container.NewVBox(action, status)); w.ShowAndRun()\n}`,
      dart: `import 'package:flutter/material.dart';\nvoid main() => runApp(const MaterialApp(home: DesktopPage()));\nclass DesktopPage extends StatefulWidget { const DesktopPage({super.key}); State<DesktopPage> createState() => _DesktopPageState(); }\nclass _DesktopPageState extends State<DesktopPage> {\n  String status = 'Ready';\n  Widget build(context) => Scaffold(appBar: AppBar(title: const Text('${values.title}')), body: Column(children: [\n    ElevatedButton(onPressed: () => setState(() => status = '${values.starter}'), child: const Text('Try ${values.api}')), Text(status)\n  ]));\n}`,
      swift: `import SwiftUI\n@main struct DesktopApp: App { var body: some Scene { WindowGroup { ContentView() } } }\nstruct ContentView: View {\n    @State private var status = "Ready"\n    var body: some View { VStack {\n        Button("Try ${values.api}") { status = "${values.starter}" }\n        Text(status)\n    }.padding().frame(minWidth: 420, minHeight: 220) }\n}`,
      ruby: `require 'green_shoes'\nShoes.app(title: "${values.title}", width: 430, height: 230) do\n  status = para "Ready"\n  button "Try ${values.api}" do\n    status.text = "${values.starter}"\n  end\nend`,
      php: `<?php\nuse Native\\Laravel\\Facades\\Window;\nuse Native\\Laravel\\Facades\\Notification;\n\nWindow::open()->title('${values.title}')->width(430)->height(230);\n\n$status = '${values.starter}';\nNotification::title('Try ${values.api}')->message($status)->show();`,
      typescript: `const title: string = "${values.title}";\ndocument.title = title;\nconst app = document.querySelector<HTMLElement>("#app")!;\napp.innerHTML = '<button id="action">Try ${values.api}</button><p id="status">Ready</p>';\ndocument.querySelector<HTMLButtonElement>("#action")!.addEventListener("click", () => {\n  document.querySelector<HTMLElement>("#status")!.textContent = "${values.starter}";\n});`,
      lua: `local status = "Ready"\nfunction love.load() love.window.setTitle("${values.title}") end\nfunction love.draw()\n  love.graphics.print("Try ${values.api}", 30, 40)\n  love.graphics.print(status, 30, 80)\nend\nfunction love.mousepressed() status = "${values.starter}" end`,
      racket: `#lang racket/gui\n(define frame (new frame% [label "${values.title}"] [width 430] [height 230]))\n(define status (new message% [parent frame] [label "Ready"]))\n(new button% [parent frame] [label "Try ${values.api}"]\n     [callback (lambda (_button _event) (send status set-label "${values.starter}"))])\n(send frame show #t)`
    };
    return samples[course.language];
  }

  function lesson(course, lessonNumber) {
    const offset = lessonNumber - 1;
    const api = course.apis[offset % course.apis.length];
    const moduleIndex = Math.floor(offset / Math.ceil(500 / modules.length)) % modules.length;
    const module = modules[moduleIndex];
    const project = projects[(offset * 3 + moduleIndex) % projects.length];
    const skill = skills[(offset + moduleIndex) % skills.length];
    const terminalLesson = moduleIndex === modules.length - 1 || /run|test|build|make|raco|cargo|gradle|artisan|npm|bundle|rake|kotlinc|java -jar|love \.|zip/i.test(api);
    const starter = terminalLesson ? `Terminal lab ${lessonNumber} ready` : `Lab ${lessonNumber} ready`;
    const goal = terminalLesson ? `${course.languageLabel} terminal lab ${lessonNumber} complete` : `${api} lab ${lessonNumber} complete`;
    const title = terminalLesson ? `${skill} with ${api} in the terminal` : `${skill} ${api} in a ${project}`;
    return {
      module, moduleIndex: moduleIndex + 1,
      navTitle: `${title} · ${lessonNumber}`, navSubtitle: terminalLesson ? "Terminal" : api,
      time: `${6 + (offset % 5)} MIN`, title: `${title} — Lab ${lessonNumber}`,
      description: terminalLesson
        ? `Practice ${api} from the terminal, read its output, diagnose failures, and connect the command to a repeatable ${course.shortTitle} workflow.`
        : `Practice how ${api} supports a ${project}, then connect it to one clear piece of interface state.`,
      tags: [api, course.languageLabel, terminalLesson ? "Terminal" : "Desktop"],
      conceptTitle: terminalLesson ? `${api}: a repeatable terminal workflow` : `${api} has one clear interface job`,
      conceptBody: terminalLesson
        ? [`Run <code>${api}</code> from the project directory and inspect both its normal output and exit status.`, "Change one input at a time, rerun the command, and record the smallest useful diagnostic when it fails."]
        : [`Identify the data ${api} displays or changes and the event that activates it.`, `Trace input to callback to visible feedback, then make one focused change in the ${project}.`],
      mentorNote: terminalLesson
        ? `Keep terminal commands reproducible: work from the project root, quote paths, read the first useful error, and rerun after one focused fix.`
        : `Keep ${course.languageLabel} callbacks small: read input, update state, and show useful feedback immediately.`,
      points: terminalLesson
        ? [["Command", `Explain what ${api} does before running it.`], ["Output", "Separate useful output from warnings and errors."], ["Recovery", "Fix one cause, rerun, and confirm the exit result."]]
        : [["Purpose", `Explain why the ${project} uses ${api}.`], ["Event", "Locate the callback that responds to the action."], ["Feedback", "Confirm the new state in the simulated window."]],
      challengeTitle: terminalLesson ? `Complete terminal lab ${lessonNumber}` : `Complete ${api} lab ${lessonNumber}`,
      challengeText: `Replace <code>${starter}</code> with <code>${goal}</code>, then run the lesson.`,
      challengeTest: (code) => code.includes(goal),
      challengeSolution: (code) => code.replaceAll(starter, goal),
      code: sampleCode(course, title, api, starter)
    };
  }

  const previousBuilder = window.buildDesktopCourses;
  window.buildDesktopCourses = function () {
    const existing = typeof previousBuilder === "function" ? previousBuilder() : [];
    const additions = languageTracks.map((course) => ({
      id: course.id,
      title: course.title,
      shortTitle: course.shortTitle,
      language: course.language,
      languageLabel: course.languageLabel,
      fileName: course.fileName,
      description: course.description,
      lessons: Array.from({ length: 500 }, (_, index) => lesson(course, index + 1))
    }));
    return [...existing, ...additions];
  };
})();
