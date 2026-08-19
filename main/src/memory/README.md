# Memory

Memory is the private company-wide account and download ledger. It records who
created an account, successful and failed sign-in attempts, and which registered
Datacenter applications were downloaded.

## Files

- `downloads.txt` is an append-only human-readable event log.
- `app_downloads.json` is the authoritative structured account/download store.
- `username.txt` is a generated username and email directory.
- `sa/` is reserved for reviewed Suspicious Activity records.

Passwords are never stored. Memory stores a unique salt and a PBKDF2-SHA256
verifier. All records are owner-only (`0600`). Email addresses are private and
must not be included in public analytics or release packages.

## Commands

```bash
python3 memory/memory.py register alice alice@example.com
python3 memory/memory.py sign-in alice
python3 memory/memory.py download alice learner
python3 memory/memory.py report
```

Password entry uses a hidden terminal prompt. A download is recorded only after
a successful sign-in and only for an application in `config/apps.json`.
