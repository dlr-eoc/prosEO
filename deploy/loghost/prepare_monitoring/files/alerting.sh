#!/bin/bash

MAIL_TO=("somereceiver@example.de" "otherreceiver@example.de")
SUBJECT_PREFIX="[CDSE CloudFerro]"

EXCLUDE_PATTERNS=(
  "CleanupProducts"
  "SomeOtherNoise"
)

content=$(curl --max-time 60 --fail --show-error -sG "http://localhost:3100/loki/api/v1/query_range" \
  -H "X-Scope-OrgID: 1" \
  --data-urlencode 'query={job="proseo"} |= "ERROR"' \
  --data-urlencode "start=$(date -d '30 minutes ago' +%s%N)" \
  --data-urlencode "end=$(date +%s%N)" \
  --data-urlencode "limit=100" \
  --data-urlencode "direction=backward" \
| jq -r '
  .data.result[] as $r
  | $r.values[]
  | "[\($r.stream.filename)] \(. [1])"
')

# apply filters
for p in "${EXCLUDE_PATTERNS[@]}"; do
  content=$(grep -vi "$p" <<< "$content")
done

count=$(echo "$content" | grep -c .)

if [ "$count" -gt 0 ]; then
  {
    echo "Note that at most 100 log lines are shown. Please refer to the source logs for more context."
    echo
    echo "$content"
  } | mail -s "$SUBJECT_PREFIX prosEO errors (last 30m, $count lines)" "${MAIL_TO[@]}"
fi