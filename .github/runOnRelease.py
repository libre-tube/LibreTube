from pyrogram import Client
from pyrogram.types import InlineKeyboardButton, InlineKeyboardMarkup, InputMediaDocument
from requests import get
from config_file import TG_API_HASH, TG_API_ID, TG_TOKEN


req = get("https://api.github.com/repos/Libre-Tube/LibreTube/releases/latest").json()
TG_CHANNEL = "LibreTube"


def get_changelog():
    data = get("https://api.github.com/repos/libre-tube/LibreTube/contents/fastlane/metadata/android/en-US/changelogs").json()
    last_log = max([int(file["name"].replace(".txt", "")) for file in data])
    log = get(
        f"https://github.com/libre-tube/LibreTube/raw/master/fastlane/metadata/android/en-US/changelogs/{last_log}.txt"
    ).text
    return log


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


assets, files = req.get("assets"), list()

for file in assets:
    with open(name, "wb") as f:
        f.write(get(file.get("browser_download_url"), stream=True).content)

    files.append(
        InputMediaDocument(
            file.get("name")
        )
    )


caption = f"**LibreTube {tag} // Privacy Simplified**\n\n<u>What's Changed?</u>\n```{get_changelog()}```"

with Client("bot", TG_API_ID, TG_API_HASH, bot_token=TG_TOKEN) as app:
    app.send_photo(
        TG_CHANNEL,
        "https://i.ibb.co/LJ9r4hP/LT.jpg",
        caption,
        reply_markup=buttons,
    )
    app.send_media_group(TG_CHANNEL, files)
