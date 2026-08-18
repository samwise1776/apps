# Desktopcraft quality standard

A release is acceptable only when `npm run publish:check` exits successfully from a clean checkout with Node 18+, Python 3.12+, and Java 17+.

The gate enforces:

- all required public files and downloads exist;
- every internal link resolves and every JavaScript file parses;
- every page has language, viewport, description, title, main landmark, H1, unique IDs, named buttons, image alternatives, and keyboard bypass navigation;
- static site code remains below per-file budgets and the non-download payload remains below 3 MB;
- Netlify and Vercel configurations contain the required browser security headers;
- generated curricula have stable course counts, unique titles, substantive teaching content, substantial code samples, and solvable challenges;
- browser authentication, customization, projects, and review behavior pass focused tests;
- database accounts, sessions, progress, feedback, AI fallback, authenticated agent responses, tool traces, community ownership, packages, and downloads pass integration tests;
- the AI agent's mocked Responses API loop preserves function calls and call IDs, executes only allowlisted read-only tools, and rejects unknown tools;
- the Java desktop package compiles and its non-graphical verification passes.

Performance budgets are deliberately checked before compression so accidental source growth is visible. Raising a budget requires documenting the user-facing value and the measured cost in the change description.

Automated checks complement manual review. Before a major release, test keyboard-only navigation, screen-reader landmarks, responsive layouts at 320/768/1280 pixels, a real server deployment with secure cookies, and all three desktop installer paths.
