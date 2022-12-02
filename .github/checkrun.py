from json import load
from os import system

with open("commit.json", "r") as f:
    data = load(f)["commit"]["message"]

if "\n\n" in data:
    if data.split("\n\n", 1)[-1].split()[0] == "[SILENT]":
        system("killall -9 python")
    else:
        print("Silence not found")
else:
    print("Empty Description")
