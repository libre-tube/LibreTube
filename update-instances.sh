#!/bin/bash

# fetch instances from public api
INSTANCES=$(curl -s 'https://piped-instances.kavin.rocks')

# generate instances list for settings
echo -e "\nContent for res/values/array.xml\n"

echo '<string-array name="instances">'
echo $INSTANCES | jq '.[].name' | while read name; do
  echo "  <item>$name</item>" | tr -d '"'
done

echo -e '</string-array>\n\n<string-array name="instancesValue">'
echo $INSTANCES | jq '.[].api_url' | while read url; do
  echo "  <item>$url</item>" | tr -d '"'
done
echo -e '</string-array>'

# generate android url schemes
echo -e "\n\nContent for AndroidManifest.xml to be replaced\n"

gen_frontends() {
  echo $INSTANCES | jq '.[].api_url' | while read url; do
    _url=$(echo "$url" | tr -d '"')
    _frontend_url=$(curl -Ls -o /dev/null -w %{url_effective} "$_url")
    _host=$(echo ${_frontend_url/https:\/\//} | tr -d '/')
    echo "  <data android:host=\"$_host\" />"
  done
}
echo "$(gen_frontends)" | sort | uniq
