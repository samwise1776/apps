# Contributing

Keep changes focused and preserve working paths. Register every app exactly once in `config/apps.json`; IDs and slugs must be unique and versions semantic. Add an app-specific build wrapper, tests appropriate to its logic, a changelog for released apps, and documentation for new dependencies.

Never place unfinished code in a production source directory. Never commit secrets or generated dependencies. Run `./checker.sh` before release and treat a failing check as a real failure rather than weakening the check.
