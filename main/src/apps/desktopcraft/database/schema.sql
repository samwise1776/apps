PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE COLLATE NOCASE,
    display_name TEXT NOT NULL,
    password_hash TEXT NOT NULL,
    password_salt TEXT NOT NULL,
    lesson_creation_banned INTEGER NOT NULL DEFAULT 0 CHECK (lesson_creation_banned IN (0, 1)),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS sessions (
    token_hash TEXT PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS course_progress (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id TEXT NOT NULL,
    active_lesson INTEGER NOT NULL DEFAULT 0 CHECK (active_lesson BETWEEN 0 AND 499),
    completed_count INTEGER NOT NULL DEFAULT 0 CHECK (completed_count BETWEEN 0 AND 500),
    total_lessons INTEGER NOT NULL DEFAULT 500 CHECK (total_lessons BETWEEN 1 AND 500),
    xp INTEGER NOT NULL DEFAULT 0 CHECK (xp BETWEEN 0 AND 50000),
    updated_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, course_id)
);

CREATE TABLE IF NOT EXISTS lesson_completions (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id TEXT NOT NULL,
    lesson_index INTEGER NOT NULL CHECK (lesson_index BETWEEN 0 AND 499),
    completed_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, course_id, lesson_index)
);

CREATE TABLE IF NOT EXISTS quiz_attempts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id TEXT NOT NULL,
    lesson_index INTEGER NOT NULL CHECK (lesson_index BETWEEN 0 AND 499),
    score INTEGER NOT NULL CHECK (score BETWEEN 0 AND 20),
    question_count INTEGER NOT NULL DEFAULT 20 CHECK (question_count = 20),
    attempted_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS custom_lessons (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    course_id TEXT NOT NULL,
    title TEXT NOT NULL,
    starter_code TEXT NOT NULL,
    answer_code TEXT NOT NULL,
    expected_output TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    deleted_at INTEGER
);

CREATE TABLE IF NOT EXISTS forum_topics (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category TEXT NOT NULL,
    title TEXT NOT NULL,
    body TEXT NOT NULL,
    code_sample TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS forum_replies (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    topic_id INTEGER NOT NULL REFERENCES forum_topics(id) ON DELETE CASCADE,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    body TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS forum_likes (
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id INTEGER NOT NULL REFERENCES forum_topics(id) ON DELETE CASCADE,
    created_at INTEGER NOT NULL,
    PRIMARY KEY (user_id, topic_id)
);

CREATE TABLE IF NOT EXISTS community_apps (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    toolkit TEXT NOT NULL CHECK (toolkit IN ('java', 'python', 'csharp', 'cpp', 'electron')),
    file_name TEXT NOT NULL,
    source_code TEXT NOT NULL,
    package_file_name TEXT,
    package_data BLOB,
    download_count INTEGER NOT NULL DEFAULT 0 CHECK (download_count >= 0),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS sessions_expiry_idx ON sessions(expires_at);
CREATE INDEX IF NOT EXISTS progress_rank_idx ON course_progress(xp DESC, completed_count DESC);
CREATE INDEX IF NOT EXISTS completions_user_idx ON lesson_completions(user_id, course_id);
CREATE INDEX IF NOT EXISTS quiz_attempts_user_idx ON quiz_attempts(user_id, course_id, lesson_index);
CREATE INDEX IF NOT EXISTS forum_topics_recent_idx ON forum_topics(updated_at DESC);
CREATE INDEX IF NOT EXISTS forum_replies_topic_idx ON forum_replies(topic_id, created_at);
CREATE INDEX IF NOT EXISTS community_apps_recent_idx ON community_apps(created_at DESC);
CREATE INDEX IF NOT EXISTS community_apps_creator_idx ON community_apps(user_id, created_at DESC);
