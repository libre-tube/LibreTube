import subprocess

while True:
    outputStr = subprocess.call("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-x86-debug.apk --output ~/app-x86-debug.apk", shell=True)
    if "Not Found" not in outputStr: 
        print("Build [x86] Downloaded")
        break

while True:
    outputStr = subprocess.call("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-x86_64-debug.apk --output ~/app-x86_64-debug.apk", shell=True)
    if "Not Found" not in outputStr: 
        print("Build [x86_64] Downloaded")
        break

while True:
    outputStr = subprocess.call("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-armeabi-v7a-debug.apk --output ~/app-armeabi-v7a-debug.apk", shell=True)
    if "Not Found" not in outputStr: 
        print("Build [arm7] Downloaded")
        break

while True:
    outputStr = subprocess.call("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-armeabi-v8a-debug.apk --output ~/app-armeabi-v8a-debug.apk", shell=True)
    if "Not Found" not in outputStr: 
        print("Build [arm8] Downloaded")
        break