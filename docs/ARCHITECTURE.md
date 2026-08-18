# Architecture

`config/apps.json` is the application source of truth. `infrastructure/repository.py` reads it for builds, validation, documentation, package metadata, backups, and status output. Thin app-specific scripts provide stable human entry points without duplicating metadata.

Applications own their UI and data. ProjectHub remains separated into `components`, `model`, `pages`, and `start`; it is not the company orchestrator. Registry, manager, logger, updater, identity broker, and targeted fixer are infrastructure. Runtime state and logs are not source artifacts. The fixer must diagnose a known condition, describe the exact target, and preserve important data before a repair.

Legacy locations remain supported while migration is incremental. Numeric `apps/1` and `apps/2` paths are documented in the registry instead of being silently renamed.
