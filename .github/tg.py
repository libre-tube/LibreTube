import telegram
from tgconfig import TG_TOKEN
from json import load
from time import sleep

f = open('commit.json')
data = load(f)
  
title = f'''<b>Libretube {data['sha'][0:7]} // Alpha</b>

{data['commit']['message']}

Signed-off-by: {data['commit']['author']['name']} <{data['commit']['author']['email']}>
'''
  
f.close()

TG_CHAT_ID = "-1001537505605"
bot = telegram.Bot(TG_TOKEN)

bot.send_photo(TG_CHAT_ID, open('alpha.png', 'rb'), title, telegram.ParseMode.HTML)
bot.send_document(TG_CHAT_ID, open('app-arm64-v8a-debug.apk', 'rb'))
bot.send_document(TG_CHAT_ID, open('app-armeabi-v7a-debug.apk', 'rb'))
bot.send_document(TG_CHAT_ID, open('app-x86_64-debug.apk', 'rb'))
bot.send_document(TG_CHAT_ID, open('app-x86-debug.apk', 'rb'))