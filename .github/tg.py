import telegram
from tgconfig import TG_TOKEN
from json import load

f = open('commit.json')
data = load(f)
f.close()

TG_CHAT_ID = "-1001537505605"
bot = telegram.Bot(TG_TOKEN)

bot.send_photo(TG_CHAT_ID, open('alpha.png', 'rb'), f'''*Libretube {data['sha'][0:7]} // Alpha*

{data['commit']['message']}

Signed-off-by: {data['commit']['author']['name']}
''', parse_mode=telegram.ParseMode.MARKDOWN)
bot.send_document(TG_CHAT_ID, open('app-arm64-v8a-debug.apk', 'rb'))
bot.send_document(TG_CHAT_ID, open('app-armeabi-v7a-debug.apk', 'rb'))
bot.send_document(TG_CHAT_ID, open('app-x86_64-debug.apk', 'rb'))
bot.send_document(TG_CHAT_ID, open('app-x86-debug.apk', 'rb'))
