# Versioning and Releases

Public versions use `MAJOR.MINOR.PATCH`: major for incompatible redesigns, minor for compatible features, and patch for fixes. Line counts are diagnostics, never release versions. Versions live only in `config/apps.json`; legacy files under `versions/` are retained for history and are not authoritative.

Release workflow: develop → app tests → checker → version update and changelog → build → package → extracted-package validation → immutable versioned release. Never overwrite an existing release without explicitly documenting why.
