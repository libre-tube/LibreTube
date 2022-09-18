from json import load
from os import system

f = open('commit.json')
data = load(f)
f.close()

data = data['commit']['message']

if "\n\n" in data:
    if data.split("\n\n",1)[-1].split()[0] == "[SILENT]":
        system('killall -9 python')
    else:
        print("Silence not found")
else:
    print("Empty Description")