from os import system as run
import tgconfig

if tgconfig.GH_REPO.lower() == "libre-tube/libretube":
    run("git add -f *")
    run('git commit -m "WORKFLOW: ALPHA BUILDS"')
    run("git push -u")
else:
    print("Official Repo not Detected")