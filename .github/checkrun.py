from json import load
from os import system

f = open('commit.json')
data = load(f)
f.close()

message = data['commit']['message']

if "\n\n" in message:
    if message.split("\n\n",1)[-1].split()[0] == "[SILENT]":
        system('killall -9 python')
        
