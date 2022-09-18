import os
from json import load

data = load(open('commit.json'))
while True:
    os.system('curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/sign.txt --output sign.txt')
    f = open('sign.txt')
    if f == data['sha'][0:7]:
        break

os.system("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-x86-debug.apk --output app-x86-debug.apk")
print("Build [x86] Downloaded")

os.system("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-x86_64-debug.apk --output app-x86_64-debug.apk")
print("Build [x86_64] Downloaded")

os.system("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-armeabi-v7a-debug.apk --output app-armeabi-v7a-debug.apk")
print("Build [arm7] Downloaded")

os.system("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-armeabi-v8a-debug.apk --output app-armeabi-v8a-debug.apk")
print("Build [arm8] Downloaded")

os.system("curl https://raw.githubusercontent.com/LibreTubeAlpha/Archive/main/latestBuild/app-universal-debug.apk --output app-armeabi-v8a-debug.apk")
print("Build [universal] Downloaded")