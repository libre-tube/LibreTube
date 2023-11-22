from os import system as run, listdir, remove
from json import load
import config_fie
import hashlib

with open("../.github/commit.json") as f:
    data = load(f)

message = f"Commit {data['sha'][0:7]}, signed off by: {data['commit']['author']['name']}"

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

if tgconfig.GH_REPO.lower() == "libre-tube/libretube":
    run("git add -f *")
    run(f'git commit -m "{message}"')
    run("git push -u")
else:
    print("Official Repo not Detected")
