from os import system as run, listdir, remove
import tgconfig


files, signed_files, unsigned_files = listdir(), [], []
for file in files:
    if file.endswith("signed.apk"):
        signed_files.append(file)
    elif file.endswith(".apk"):
        unsigned_files.append(file)

if len(signed_files):
    for file in unsigned_files:
        remove(file)

if tgconfig.GH_REPO.lower() == "libre-tube/libretube":
    run("git add -f *")
    run('git commit -m "WORKFLOW: ALPHA BUILDS"')
    run("git push -u")
else:
    print("Official Repo not Detected")
