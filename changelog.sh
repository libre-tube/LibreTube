## Usage: ./changelog.sh <text>

TEXT="$1"

# remove everything related to weblate and dependencies
TEXT=$(printf "${TEXT[@]}" | sed -e 's/.*Translations.*//g' -e 's/.*(fix|chore)\(deps\).*//g')

# go through all the lines inside the file
readarray -t y <<< "${TEXT[@]}"

trim() {
    local var="$*"
    # remove leading whitespace characters
    var="${var#"${var%%[![:space:]]*}"}"
    # remove trailing whitespace characters
    var="${var%"${var##*[![:space:]]}"}"
    printf '%s' "$var"
}

TRIMMED=`trim "$(echo "$TEXT" | sort)"`

CHANGELOG=()
CURRENTSECTION=""
while IFS= read -r line; do
    SECTION=$(echo "$line" | cut -d ":" -f 1 | tr -d "*" | tr -d " " | sed "s/(.*)//g")
    if [ "$SECTION" != "$CURRENTSECTION" ]; then
        CHANGELOG+="\n## ${SECTION^}\n"
        CURRENTSECTION="$SECTION"
    fi
    CHANGELOG+="$line\n"
done <<< "$TRIMMED"

# Output the generated changelog
echo -e "$CHANGELOG"
