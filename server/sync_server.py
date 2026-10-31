#!/usr/bin/env python3
"""
Tiny reference vault server for Idea.

Storage is one JSON document per device in ./vaults. Run:

    python server/sync_server.py --host 0.0.0.0 --port 8787

Then put http://<your-lan-ip>:8787 in Idea settings on every device and give
each device a distinct name. This is meant for a trusted LAN or inside a
reverse proxy; there is no authentication in this small reference server.
"""
from __future__ import annotations

import argparse
import json
import os
import re
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

ROOT = Path(__file__).resolve().parent / "vaults"
NAME = re.compile(r"^[A-Za-z0-9._-]{1,80}$")
MAX_BYTES = 64 * 1024 * 1024


class Handler(BaseHTTPRequestHandler):
    server_version = "IdeaVault/1.0"

    def log_message(self, fmt: str, *args: object) -> None:
        print(self.address_string(), fmt % args, flush=True)

    def _path(self) -> str | None:
        path = urlparse(self.path).path
        if not path.startswith("/vault/"):
            return None
        name = unquote(path[len("/vault/") :]).strip()
        if not NAME.fullmatch(name):
            return None
        return name

    def _reply(self, code: int, body: bytes = b"", content_type: str = "text/plain; charset=utf-8") -> None:
        self.send_response(code)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        if self.command != "HEAD":
            self.wfile.write(body)

    def do_GET(self) -> None:
        name = self._path()
        if name is None:
            self._reply(404, b"not found")
            return
        target = ROOT / (name + ".json")
        if not target.exists():
            self._reply(404, b"empty")
            return
        self._reply(200, target.read_bytes(), "application/json; charset=utf-8")

    def do_PUT(self) -> None:
        name = self._path()
        if name is None:
            self._reply(404, b"not found")
            return
        try:
            length = int(self.headers.get("Content-Length", "0"))
        except ValueError:
            self._reply(400, b"bad length")
            return
        if length <= 0 or length > MAX_BYTES:
            self._reply(413, b"bad size")
            return
        raw = self.rfile.read(length)
        try:
            json.loads(raw)
        except Exception:
            self._reply(400, b"invalid json")
            return
        ROOT.mkdir(parents=True, exist_ok=True)
        target = ROOT / (name + ".json")
        temp = target.with_suffix(".tmp")
        temp.write_bytes(raw)
        os.replace(temp, target)
        self._reply(204)

    def do_HEAD(self) -> None:
        self.do_GET()


def main() -> None:
    parser = argparse.ArgumentParser(description="Idea sync reference server")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=8787)
    args = parser.parse_args()
    ROOT.mkdir(parents=True, exist_ok=True)
    server = ThreadingHTTPServer((args.host, args.port), Handler)
    print(f"Idea vault listening on http://{args.host}:{args.port}/vault/<device>", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
