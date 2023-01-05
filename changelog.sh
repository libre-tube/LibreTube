# check whether there's a file passed as argument
! [ -f "$1" ] && echo "File doesn't exist" && exit 1

# read the file
TEXT=$(<"$1")

# remove weblate and dependencies
TEXT=$(printf "${TEXT[@]}" | sed 's/.*Weblate.*//g' | sed 's/.*dependency.*//g')

# go through all the lines inside the file
readarray -t y <<< "${TEXT[@]}"

echoall() {
for line in "$@"; do
    echo "$line"
done
}

FIXES=()
FEATURES=()
IMPROVEMENTS=()
REPOCHANGES=()
for line in "${y[@]}" ; do
  if [[ "$line" =~ [Ff]ix ]]; then
    FIXES+=("$line")
  elif [[ "$line" =~ [Oo]ption ]] || [[ "$line" =~ [Mm]inor ]]; then
    IMPROVEMENTS+=("$line")
  elif [[ "$line" =~ [Rr][Ee][Aa][Dd][Mm][Ee] ]] || [[ "$line" =~ [Ss]creenshots ]] || [[ "$line" =~ Clean( )?up ]]; then
    REPOCHANGES+=("$line")
  elif [[ "$line" =~ ^\*\*Full ]]; then
    FULLCHANGELOG="$line"
  elif ! [[ -z "$line" ]]; then
    FEATURES+=("$line")
  fi
done

# Print all the found and categorized changes
echo "## New features"
echoall "${FEATURES[@]}"
echo ""

echo "## Minor changes"
echoall "${IMPROVEMENTS[@]}"
echo ""

echo "## Bug fixes"
echoall "${FIXES[@]}"
echo ""

echo "## Repo changes"
echoall "${REPOCHANGES[@]}"
echo ""

echo "$FULLCHANGELOG"