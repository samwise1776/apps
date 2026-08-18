# Packaging and Backups

A source backup contains source, docs, configuration, tests, and required assets. It excludes dependencies, build outputs, releases, logs, secrets, and unfinished history. Run `./scripts/backup.sh`; ZIP integrity is checked before success is reported.

A release package contains one active distributable application plus `RELEASE-MANIFEST.txt`. Run `./scripts/package.sh`. Each package is extracted to a temporary directory and validated: Java is compiled from the extracted source and Trestrio runs its tests from the extracted source. Failed packages are removed and are never reported as successful.

Every successful package also receives a `.sha256` checksum and a
`.provenance.json` record containing its application, version, build time,
source revision, builder, language, and artifact digest. An `unversioned`
revision is acceptable for local experiments but must not be published.

Rebuildable folders such as `node_modules`, `dist`, `build`, `bin`, and `obj` never belong in normal source archives.
