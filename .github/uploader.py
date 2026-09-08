from os import listdir, remove, popen
from json import load
import config_file
import hashlib
import re
import subprocess

with open("../.github/commit.json") as f:
    data = load(f)

sha = data.get('sha', popen("git rev-parse --short HEAD").read())[0:7]
# sanitize the author name to prevent shell injection / broken commands
author = re.sub(r'[^A-Za-z0-9 ._\-]', '', data.get('commit', {}).get('author', {}).get('name', 'Unknown'))[:60]
message = f"Commit {sha}, signed off by: {author}"

files, signed_files, unsigned_files = listdir(), [], []
for file in files:
    if file.endswith("signed.apk"):
        signed_files.append(file)
    elif file.endswith(".apk"):
        unsigned_files.append(file)

if len(signed_files):
    for file in unsigned_files:
        remove(file)

with open("checksums", "w") as checksums:
    for file in signed_files or unsigned_files:
        with open(file, "rb") as apk:
            bytes = apk.read()
            sha256hash = hashlib.sha256(bytes).hexdigest()
            checksums.write(sha256hash + "  " + apk.name + "\n")

if config_file.GH_REPO.lower() == "libre-tube/libretube":
    subprocess.run(["git", "add", "-f", "*"], check=False)
    subprocess.run(["git", "commit", "-m", message], check=False)
    subprocess.run(["git", "push", "-u"], check=False)
else:
    print("Official Repo not Detected")