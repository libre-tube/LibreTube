from pyrogram import Client
from pyrogram.types import (
    InlineKeyboardButton,
    InlineKeyboardMarkup,
    InputMediaDocument,
)
from requests import get
from tgconfig import TG_API_HASH, TG_API_ID, TG_CID, TG_TOKEN

url = "https://api.github.com/repos/Libre-Tube/LibreTube/releases/latest"

req = get(url).json()

if TG_CID.isdecimal():
    TG_CID = int(TG_CID)


def get_changelog():
    url = "https://api.github.com/repos/libre-tube/LibreTube/contents/fastlane/metadata/android/en-US/changelogs"
    data = get(url).json()
    last_log = max([int(file["name"].replace(".txt", "")) for file in data])
    log = get(
        f"https://github.com/libre-tube/LibreTube/raw/master/fastlane/metadata/android/en-US/changelogs/{last_log}.txt"
    ).text
    return log


def download(url, name):
    with open(name, "wb") as f:
        f.write(get(url, stream=True).content)
    return name


buttons = InlineKeyboardMarkup(
    [
        [
            InlineKeyboardButton("Release", url=req.get("html_url")),
            InlineKeyboardButton(
                "F-droid", url="https://www.f-droid.org/packages/com.github.libretube/"
            ),
            InlineKeyboardButton(
                "IzzyOnDroid",
                url="https://apt.izzysoft.de/fdroid/index/apk/com.github.libretube",
            ),
        ]
    ]
)


_files, files, result, tag = req.get("assets"), [], req.get("body"), req.get("tag_name")
for file in _files:
    files.append(
        InputMediaDocument(download(file.get("browser_download_url"), file.get("name")))
    )

caption = f"**LibreTube {tag} // Privacy Simplified**\n\n<u>What's Changed?</u>\n```{get_changelog()}```"
with Client("bot", TG_API_ID, TG_API_HASH, bot_token=TG_TOKEN) as app:
    app.send_photo(
        TG_CID,
        "https://i.ibb.co/LJ9r4hP/LT.jpg",
        caption,
        reply_markup=buttons,
    )
    app.send_media_group(TG_CID, files)
