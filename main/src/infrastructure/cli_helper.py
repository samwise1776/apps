#!/usr/bin/env python3
"""CLI helper for datacenter command output formatting."""
import json
import sys

def list_apps(path):
    with open(path) as f:
        data = json.load(f)
    for app in data['applications']:
        print(f"{app['id']:15} {app['slug']:15} {app['name']:20} {app['status']:15} {app['language']}")

def info_app(path, slug):
    with open(path) as f:
        data = json.load(f)
    for app in data['applications']:
        if app['slug'] == slug:
            for k, v in app.items():
                print(f"  {k:20s}: {v}")
            return 0
    print(f"Unknown application: {slug}", file=sys.stderr)
    return 1

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: cli-helper.py list|info <registry> [slug]", file=sys.stderr)
        sys.exit(1)
    cmd = sys.argv[1]
    if cmd == "list":
        list_apps(sys.argv[2])
    elif cmd == "info":
        sys.exit(info_app(sys.argv[2], sys.argv[3]))
