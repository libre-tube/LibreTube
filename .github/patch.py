from os import system

patchRes = ["mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"]
for res in patchRes:
    system(f"rm -rf ../app/src/main/res/mipmap-{res}/ic_launcher.png")
    system(f"rm -rf ../app/src/main/res/mipmap-{res}/ic_launcher_foreground.png")
    system(f"rm -rf ../app/src/main/res/mipmap-{res}/ic_launcher_round.png")
    system(f"curl https://libre-tube.github.io/alpha-patch/mipmap-{res}/ic_launcher.png --output ../app/src/main/res/mipmap-{res}/ic_launcher.png")
    system(f"curl https://libre-tube.github.io/alpha-patch/mipmap-{res}/ic_launcher_foreground.png --output ../app/src/main/res/mipmap-{res}/ic_launcher_foreground.png")
    system(f"curl https://libre-tube.github.io/alpha-patch/mipmap-{res}/ic_launcher_round.png --output ../app/src/main/res/mipmap-{res}/ic_launcher_round.png")