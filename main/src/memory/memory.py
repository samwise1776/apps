#!/usr/bin/env python3
"""Private, dependency-free account and application-download ledger."""
from __future__ import annotations

import argparse
import base64
import datetime as dt
import getpass
import hashlib
import hmac
import json
import os
import re
import secrets
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
COMPANY = HERE.parent
STORE = HERE / "app_downloads.json"
DIRECTORY = HERE / "username.txt"
LOG = HERE / "downloads.txt"
REGISTRY = COMPANY / "config" / "apps.json"
ITERATIONS = 600_000
USERNAME = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]{2,31}")
EMAIL = re.compile(r"[^@\s]+@[^@\s]+\.[^@\s]+")


def now() -> str:
    return dt.datetime.now(dt.UTC).isoformat(timespec="seconds")


def private(path: Path) -> None:
    path.chmod(0o600)


def load() -> dict:
    data = json.loads(STORE.read_text(encoding="utf-8"))
    if data.get("schema_version") != 1 or not isinstance(data.get("accounts"), dict) or not isinstance(data.get("downloads"), list):
        raise RuntimeError("Unsupported or damaged Memory store")
    return data


def save(data: dict) -> None:
    # Keep the temporary file beside the destination so os.replace remains
    # atomic even when the system temporary directory is on another filesystem.
    fd, name = tempfile.mkstemp(prefix="memory-", suffix=".tmp", dir=STORE.parent)
    temporary = Path(name)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(data, handle, indent=2, sort_keys=True)
            handle.write("\n")
            handle.flush()
            os.fsync(handle.fileno())
        private(temporary)
        os.replace(temporary, STORE)
        private(STORE)
    finally:
        temporary.unlink(missing_ok=True)
    render_directory(data)


def render_directory(data: dict) -> None:
    lines = ["# username\temail"]
    lines.extend(f"{name}\t{account['email']}" for name, account in sorted(data["accounts"].items()))
    DIRECTORY.write_text("\n".join(lines) + "\n", encoding="utf-8")
    private(DIRECTORY)


def event(kind: str, username: str, detail: str) -> None:
    safe = detail.replace("\t", " ").replace("\n", " ")
    with LOG.open("a", encoding="utf-8") as handle:
        handle.write(f"{now()}\t{kind}\t{username}\t{safe}\n")
    private(LOG)


def derive(password: str, salt: bytes) -> bytes:
    return hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt, ITERATIONS, dklen=32)


def register(username: str, email: str, password: str) -> None:
    if not USERNAME.fullmatch(username):
        raise ValueError("Username must be 3–32 characters using letters, numbers, dot, dash, or underscore")
    email = email.strip().lower()
    if not EMAIL.fullmatch(email):
        raise ValueError("Invalid email address")
    if len(password) < 12:
        raise ValueError("Password must contain at least 12 characters")
    data = load()
    if username in data["accounts"] or any(x["email"] == email for x in data["accounts"].values()):
        raise ValueError("Username or email is already registered")
    salt = secrets.token_bytes(16)
    data["accounts"][username] = {
        "email": email,
        "created_at": now(),
        "password": {
            "algorithm": "pbkdf2-sha256",
            "iterations": ITERATIONS,
            "salt": base64.b64encode(salt).decode("ascii"),
            "verifier": base64.b64encode(derive(password, salt)).decode("ascii"),
        },
    }
    save(data)
    event("account_registered", username, "account created")


def authenticate(username: str, password: str) -> bool:
    data = load()
    account = data["accounts"].get(username)
    valid = False
    if account:
        record = account["password"]
        salt = base64.b64decode(record["salt"], validate=True)
        expected = base64.b64decode(record["verifier"], validate=True)
        valid = hmac.compare_digest(derive(password, salt), expected)
    else:
        derive(password, bytes(16))  # reduce username-enumeration timing signal
    event("sign_in_success" if valid else "sign_in_failed", username, "credentials checked")
    return valid


def registered_apps() -> dict[str, dict]:
    applications = json.loads(REGISTRY.read_text(encoding="utf-8"))["applications"]
    return {app["slug"]: app for app in applications if app["status"] != "UNFINISHED"}


def record_download(username: str, password: str, slug: str) -> None:
    if not authenticate(username, password):
        raise PermissionError("Sign-in failed")
    app = registered_apps().get(slug)
    if not app:
        raise ValueError("Unknown or unfinished application")
    data = load()
    item = {
        "download_id": secrets.token_urlsafe(18),
        "username": username,
        "app_id": app["id"],
        "app_slug": app["slug"],
        "app_version": app["version"],
        "downloaded_at": now(),
    }
    data["downloads"].append(item)
    save(data)
    event("app_download", username, f"{app['slug']} {app['version']} {item['download_id']}")


def report() -> None:
    data = load()
    totals: dict[str, int] = {}
    for item in data["downloads"]:
        totals[item["app_slug"]] = totals.get(item["app_slug"], 0) + 1
    print(f"Accounts: {len(data['accounts'])}\nDownloads: {len(data['downloads'])}")
    for slug, count in sorted(totals.items(), key=lambda x: (-x[1], x[0])):
        print(f"{slug}: {count}")


def password(confirm: bool = False) -> str:
    value = getpass.getpass("Password: ")
    if confirm and value != getpass.getpass("Confirm password: "):
        raise ValueError("Passwords do not match")
    return value


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    create = commands.add_parser("register"); create.add_argument("username"); create.add_argument("email")
    signin = commands.add_parser("sign-in"); signin.add_argument("username")
    download = commands.add_parser("download"); download.add_argument("username"); download.add_argument("app")
    commands.add_parser("report")
    args = parser.parse_args()
    try:
        if args.command == "register": register(args.username, args.email, password(True)); print("Account created")
        elif args.command == "sign-in":
            if not authenticate(args.username, password()): raise PermissionError("Sign-in failed")
            print("Sign-in successful")
        elif args.command == "download": record_download(args.username, password(), args.app); print("Download recorded")
        else: report()
        return 0
    except (ValueError, PermissionError, RuntimeError) as error:
        print(f"Memory: {error}", file=os.sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
