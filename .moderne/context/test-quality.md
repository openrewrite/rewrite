# Test Quality

## Test quality issues that may cause flakiness or silent failures

Issues found in test code that may cause flakiness, silent failures, or maintenance burden. Includes static waits, unmocked external calls, missing assertions, ghost tests, and test code smells.

## Data Tables

### Test quality issues

**File:** [`test-quality-issues.csv`](test-quality-issues.csv)

Issues found in test code that may cause flakiness, silent failures, or maintenance burden. Each row includes a rich evidence message with what was found, why it matters, and how to fix it.

| Column | Description |
|--------|-------------|
| Source path | Path to the test source file. |
| Class name | Fully qualified class name of the test class. |
| Method name | Test method name, if the issue is method-level. Null for class-level issues. |
| Issue type | Category of the issue: static wait, shared mutable state, unmocked http, unmocked db, unmocked network, java assert in test, swallowed exception, missing assertion, hardcoded date, timing assertion, hardcoded port/path, missing annotation, skipped without reason, broad matcher, ignored error, deprecated test api, magic number, poor test name, prototype mutation, empty catch. |
| Severity | Issue severity: high, medium, or low. |
| Language | Source language: java, javascript, or python. |
| Evidence | What was found, why it matters, and how to fix it. |

