"""Read-only release audit. Reports locations, never credential values. No network calls."""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parent.parent
parser = argparse.ArgumentParser()
parser.add_argument("--secret-env-file", type=Path, help="Optional authorized local file; never copied")
args = parser.parse_args()
NAMES = ("AI_API_KEY", "LLM_API_KEY", "DB_PASSWORD", "JWT_SECRET")
secrets = [os.environ[n].encode() for n in NAMES if len(os.environ.get(n, "")) >= 4
           and os.environ[n] not in ("disabled", "your_api_key")]
if args.secret_env_file:
    for line in args.secret_env_file.read_text(encoding="utf-8-sig").splitlines():
        match = re.match(r"\s*(?:export\s+)?(AI_API_KEY|LLM_API_KEY|DB_PASSWORD|JWT_SECRET)\s*=\s*(.*?)\s*$", line)
        if match:
            value = match.group(2).strip().strip("\"'")
            if len(value) >= 8:
                secrets.append(value.encode())

def git(*argv):
    return subprocess.check_output(["git", "-c", "safe.directory=" + ROOT.as_posix(), *argv], cwd=ROOT)

patterns = [
    re.compile(rb"\bsk-[A-Za-z0-9_-]{24,}"),
    re.compile(rb"\bgh[pousr]_[A-Za-z0-9]{25,}"),
    re.compile(rb"\beyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"),
]
findings = []
private_findings = []

def scan(data, location, private=False):
    if any(value in data for value in secrets) or any(pattern.search(data) for pattern in patterns):
        (private_findings if private else findings).append(location)

def forbidden(name):
    p = Path(name)
    return (any(x in ("target", "node_modules", "dist", "logs", "work") for x in p.parts)
            or (p.name.startswith(".env") and p.name != ".env.example")
            or p.suffix.lower() in (".env", ".log", ".pid", ".tmp")
            or p.name.endswith((".mv.db", ".h2.db")))

files = set(git("ls-files", "-z", "--cached", "--others", "--exclude-standard").decode().split("\0")) - {""}
bad_paths = sorted(name for name in files if forbidden(name))
large = []
for name in files:
    path = ROOT / name
    if path.is_file():
        if path.stat().st_size > 10 * 1024 * 1024:
            large.append(name)
        scan(path.read_bytes(), "workspace:" + name)

# Include ignored runtime text in the local audit, but never publish its contents.
runtime_count = 0
for folder in ("work", "logs"):
    base = ROOT / folder
    if not base.exists():
        continue
    for parent, dirs, names in os.walk(base):
        dirs[:] = [d for d in dirs if d not in ("node_modules", "target", "dist", ".git")]
        for name in names:
            path = Path(parent) / name
            if path.suffix.lower() in (".log", ".json", ".txt", ".env", ".yml", ".yaml", ".properties"):
                runtime_count += 1
                scan(path.read_bytes(), "ignored-runtime:" + path.relative_to(ROOT).as_posix(), True)

objects = {}
history_bad_paths = set()
for line in git("rev-list", "--objects", "--all", "--reflog").decode().splitlines():
    oid, _, name = line.partition(" ")
    if name and forbidden(name):
        history_bad_paths.add(name)
    objects.setdefault(oid, []).append("history:" + oid[:12] + ":" + name)
index_count = 0
for record in git("ls-files", "--stage", "-z").decode().split("\0"):
    if not record:
        continue
    metadata, name = record.split("\t", 1)
    oid = metadata.split()[1]
    objects.setdefault(oid, []).append("index:" + name)
    index_count += 1

# Scan all reachable history object contents (including commit/tag messages) and the index.
process = subprocess.Popen(["git", "-c", "safe.directory=" + ROOT.as_posix(), "cat-file", "--batch"],
                           cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE)
try:
    for oid, locations in objects.items():
        process.stdin.write((oid + "\n").encode())
        process.stdin.flush()
        header = process.stdout.readline().split()
        if len(header) != 3:
            raise RuntimeError("Git object unavailable; audit incomplete")
        size = int(header[2])
        data = process.stdout.read(size)
        process.stdout.read(1)
        for location in locations:
            scan(data, location)
finally:
    process.stdin.close()
    process.wait()

report = dict(workspaceFiles=len(files), indexFiles=index_count, historyAndIndexObjects=len(objects),
              ignoredRuntimeFiles=runtime_count, credentialFindings=findings,
              ignoredRuntimeFindings=private_findings, forbiddenPaths=bad_paths,
              historyForbiddenPaths=sorted(history_bad_paths), largeFiles=large)
print(json.dumps(report, ensure_ascii=False, indent=2))
raise SystemExit(1 if findings or bad_paths or history_bad_paths or large else 0)
