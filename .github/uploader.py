from os import system as run
import tgconfig

if GH_REPO.lower() == "libre-tube/libretube":
    run("git clone https://github.com/LibreTubeAlpha/Archive archive")
    run("rm -rf archive/*.apk")
    run("mv app/build/outputs/apk/debug/*.apk archive/")
    run("cd archive")
    run("git add -f *")
    run('git commit -m "WORKFLOW: ALPHA BUILDS"')
    run("git push -u")