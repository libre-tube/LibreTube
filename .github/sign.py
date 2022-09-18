from json import load

data = load(open("commit.json"))
f = open('sign.txt', "w")
f.write(data['sha'][0:7])