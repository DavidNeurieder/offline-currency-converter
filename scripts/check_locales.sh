#!/bin/bash

# Locale Completeness Checker
# Verifies that all locales have the same string keys
# Usage: ./scripts/check_locales.sh

set -e

APP_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RES_DIR="$APP_DIR/app/src/main/res"
VALUES_DIR="$RES_DIR/values"

echo "=========================================="
echo "Locale String Completeness Check"
echo "=========================================="
echo ""

# Get base English strings (excluding comments and empty lines)
BASE_STRINGS=$(mktemp)
grep -oP '(?<=<string name=")[^"]+' "$VALUES_DIR/strings.xml" | sort > "$BASE_STRINGS"
BASE_COUNT=$(wc -l < "$BASE_STRINGS")

echo "Base locale: English"
echo "Base strings count: $BASE_COUNT"
echo ""

# Check each locale
TOTAL_LOCALES=0
MISSING_STRINGS=0

for LOCALE_DIR in "$RES_DIR"/values-*/; do
    if [ -f "$LOCALE_DIR/strings.xml" ]; then
        LOCALE=$(basename "$LOCALE_DIR" | sed 's/values-//')
        TOTAL_LOCALES=$((TOTAL_LOCALES + 1))
        
        LOCALE_STRINGS=$(mktemp)
        grep -oP '(?<=<string name=")[^"]+' "$LOCALE_DIR/strings.xml" | sort > "$LOCALE_STRINGS"
        LOCALE_COUNT=$(wc -l < "$LOCALE_STRINGS")
        
        # Find missing strings
        MISSING=$(comm -23 "$BASE_STRINGS" "$LOCALE_STRINGS")
        MISSING_COUNT=$(echo "$MISSING" | grep -c . || true)
        
        if [ -n "$MISSING" ]; then
            MISSING_STRINGS=$((MISSING_STRINGS + 1))
            echo "❌ $LOCALE ($LOCALE_COUNT strings)"
            echo "   Missing strings:"
            echo "$MISSING" | sed 's/^/      /'
            echo ""
        else
            echo "✅ $LOCALE ($LOCALE_COUNT strings) - Complete"
        fi
        
        rm -f "$LOCALE_STRINGS"
    fi
done

# Clean up
rm -f "$BASE_STRINGS"

echo ""
echo "=========================================="
echo "Summary"
echo "=========================================="
echo "Total locales checked: $TOTAL_LOCALES"
echo "Locales with missing strings: $MISSING_STRINGS"

if [ "$MISSING_STRINGS" -eq 0 ]; then
    echo ""
    echo "✅ All locales are complete!"
    exit 0
else
    echo ""
    echo "❌ Some locales have missing translations"
    exit 1
fi
