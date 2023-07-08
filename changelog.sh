# check whether there's a file passed as argument
! [ -f "$1" ] && echo "File doesn't exist" && exit 1

# read the file
TEXT=$(<"$1")

# the link containing the full commit history
FULLCHANGELOG=$(echo "$TEXT{@}" | tail -n 1)
NEWCONTRIBUTORS=$(echo "$TEXT{@}" | grep 'first contribution')

# remove everything related to weblate and dependencies
TEXT=$(printf "${TEXT[@]}" | sed -e 's/.*Weblate.*//g' -e 's/.*dependency.*//g' -e 's/^\*\*Full//g' -e 's/.*first contribution.*//g' -e 's/##.*//g')

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

# Print all the found and categorized changes
create_changelog() {
  trim "$(echo "$TEXT" | sort)"
  [ "$NEWCONTRIBUTORS" ] && echo -e "\n\n## New contributors\n$NEWCONTRIBUTORS"
  echo -e "\n\n$FULLCHANGELOG"
}

# generate a new changelog
CHANGELOG=$(create_changelog)

# Output the generated changelog
echo "$CHANGELOG"
