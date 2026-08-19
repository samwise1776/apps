# Learner

Learner is a local Java Swing learning application with a dark-blue visual identity and Nernal, an animated `Graphics2D` guide. It includes 10 subjects and exactly 10,000 structured lessons (1,000 per subject).

## Features

- Local profiles with display names; no email or personal information required
- Dashboard, levels, points, progress bars, continue-learning shortcut
- Math, Coding, Spelling, Science, History, Geography, Reading, Writing, Art, and Life Skills
- Ten meaningful curriculum topics per subject with increasing difficulty
- Search by keyword, ID, title, subject, topic, and difficulty
- Completed/not-completed filters, favorites, and recent history
- Multiple-choice quizzes with explanations and best-score tracking
- One-time lesson points and quiz-improvement points to prevent farming
- Local leaderboard containing only actual profiles on this computer
- Achievements and persistent settings
- Safe local saves in `~/.learner/`
- Animated Nernal moods; animation can be disabled

## Project structure

```text
src/
├── Learner.java                 Main window and navigation
├── Lessons.java                 Legacy compatibility catalog
├── character/                   Nernal drawing, moods, animation
├── lessons/                     Curriculum, lesson model/generator/catalog
├── profile/                     Profile, progress, settings
├── quiz/                        Questions and quiz generation
├── storage/                     Safe local persistence
└── ui/                          Shared theme
tests/
└── LearnerTests.java            Non-GUI logic tests
```

## Requirements

Java 17 or newer.

## Build and run

From the project directory:

```bash
mkdir -p out
rg --files src -g '*.java' | xargs javac -d out
java -cp out Learner
```

If `find` is unavailable, replace its output with the list from `rg --files src -g '*.java'`.

## Run tests

```bash
mkdir -p out
(rg --files src -g '*.java'; printf '%s\n' tests/LearnerTests.java) | xargs javac -d out
java -cp out LearnerTests
```

## Saving and privacy

Profiles and settings are stored as readable property files in `~/.learner/`. Writes use a temporary file followed by an atomic replacement when the operating system supports it. Missing or damaged values fall back to safe defaults rather than stopping startup.

## Current limitations

- Quiz interaction is multiple-choice; the question engine varies content and difficulty but does not yet include free-text grading.
- Sound is an intentionally labeled placeholder and defaults to off.
- The leaderboard is local only; there is no networking or account system.
