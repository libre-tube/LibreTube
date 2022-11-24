import asyncio
from json import load
from os import listdir

from pyrogram import Client
from pyrogram.types import InputMediaDocument
from tgconfig import *

files = listdir()

mediadocuments = [
    InputMediaDocument(file) for file in files if file.endswith("signed.apk")
]

with open("commit.json") as f:
    data = load(f)

caption = f"""**Libretube {data['sha'][0:7]} // Alpha**

<a href="{data['html_url']}">{data['commit']['message']}</a>

Signed-off-by: {data['commit']['author']['name']}
"""


async def main():
    async with Client("libretube", TG_API_ID, TG_API_HASH, bot_token=TG_TOKEN) as app:
        await app.send_photo(
            int(TG_POST_ID), "https://libre-tube.github.io/images/Alpha.png", caption
        )
        await app.send_media_group(int(TG_POST_ID), mediadocuments)


asyncio.run(main())
