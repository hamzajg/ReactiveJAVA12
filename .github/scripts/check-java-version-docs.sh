#!/usr/bin/env bash
# Fails when current-state docs or Dockerfiles claim a Java/JDK version other
# than 25. CI pins java-version: "25", so any other version in those files is
# drift.
#
# Deliberately exempt:
#   - docs/adr/     decision records narrate the upgrade path (JDK 11 -> 17 -> 25)
#   - specs/        dated specification artifacts
#   - MODERNIZATION.md, CHANGELOG*  migration logs
#   - .openharness/ local agent state
#   - any line carrying a history marker ("was", "moved", "→", ...)
set -o pipefail

exempt_paths=(
  "(^|/)node_modules/"
  "(^|/)\.git/"
  "(^|/)build/"
  "(^|/)target/"
  "(^|/)\.gradle/"
  "(^|/)\.openharness/"
  "(^|/)specs/"
  "(^|/)docs/adr/"
  "(^|/)MODERNIZATION\.md$"
  "(^|/)CHANGELOG"
)

# "java 17", "jdk-17", "jdk17", "temurin:17", "openjdk-17", "17-jdk", "1.8"
# The bare "java" form needs a separator so a repo name like ReactiveJAVA12 is
# not read as a version claim; "jdk"/"temurin" keep the compact jdk17 form.
version_pattern='java[[:space:]:_/-]{1,3}(1\.8|8|11|12|13|14|15|16|17|18|19|20|21|22|23|24|26)([^0-9]|$)|(jdk|jre|openjdk|temurin|corretto)[[:space:]:_/-]{0,3}(1\.8|8|11|12|13|14|15|16|17|18|19|20|21|22|23|24|26)([^0-9]|$)|(1\.8|8|11|17|21|22|23|24)[[:space:]:_-]+(java|jdk)|java[-_ ]?version[[:space:]:="'"'"']*(1\.8|8|11|17|21|22|23|24|26)([^0-9]|$)'
# Anything matching this is the allowed version.
# The book title is not a version claim about this build.
allowed_pattern='(java|jdk|jre|openjdk|temurin|corretto)[[:space:]:_/-]{0,3}25([^0-9]|$)|25[[:space:]:_-]+(java|jdk)|Hands-On Reactive Programming with Java'
# Narrative markers: a line describing what the stack used to be.
history_pattern='(\bwas\b|\bwere\b|\bhad\b|previously|previous|earlier|formerly|legacy|historical|history|no longer|moved|upgraded|migration|before that|predates|originally|\bold\b|→|->|Phase [0-9])'

fail=0
checked=0

while IFS= read -r file; do
  skip=0
  for p in "${exempt_paths[@]}"; do
    if printf '%s\n' "$file" | grep -qE "$p"; then skip=1; break; fi
  done
  [ "$skip" -eq 1 ] && continue

  checked=$((checked + 1))
  # A history marker often sits on the line before the version it explains
  # ("was Boot 2.1.9/Java 8," / "Gradle 5.6.1/JDK 11)"), so look back a little.
  context=''
  lineno=0
  while IFS= read -r line; do
    lineno=$((lineno + 1))
    if printf '%s\n' "$line" | grep -qiE "$version_pattern"; then
      if ! printf '%s\n' "$line" | grep -qiE "$allowed_pattern" \
        && ! printf '%s\n%s\n' "$context" "$line" | grep -qiE "$history_pattern"; then
        echo "drift: $file:$lineno: $line"
        fail=1
      fi
    fi
    context=$(printf '%s\n%s' "$context" "$line" | tail -n 2)
  done < "$file"
done < <(find . -type f \( -name '*.md' -o -name 'Dockerfile*' \) | sort)

if [ "$fail" -eq 1 ]; then
  echo
  echo 'Docs or Dockerfiles claim a Java version other than 25.'
  echo 'CI pins java-version: "25" - update the text, or mark the line as history.'
  exit 1
fi

echo "checked $checked files: no Java version drift (all claims are 25)"
