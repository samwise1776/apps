# Datacenter internal data

The `.data` directory contains internal, generated, historical, or unfinished
Datacenter material that is not part of the active top-level application set.

## Layout

```text
.data/
├── LICENSE                 Project license
├── logs/                   Application, manager, and security logs
├── unfinished/             Work that is not production-ready
│   ├── apps/3/             Java game placeholder
│   ├── archives/           Packages of unfinished work
│   ├── big/                Experimental applications and AppManager
│   ├── compiled/           Compiled copies of unfinished applications
│   ├── logs/               Logs belonging only to unfinished or removed work
│   ├── mirrors/            Unfinished copies removed from repository mirrors
│   ├── placeholders/       Empty reserved directories for future work
│   ├── runtime/            Stale or failed generated runtime projects
│   └── security/           Permission system under development
├── Data/Compiled_folders/  Stored compiled output
└── resources/              Internal maintenance scripts
```

Items under `unfinished/` must not be treated as production-ready. Move an item
back into the active project layout only after it has working behavior,
documentation, tests, and an explicit status review.

Git's internal `.git/logs` and object databases remain inside their repositories
because they are required Git metadata, not active Datacenter files. Git history
may retain prior versions of unfinished files even after their working-tree
copies have been moved into `unfinished/mirrors`.
