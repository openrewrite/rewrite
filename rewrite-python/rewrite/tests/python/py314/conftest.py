import sys

# Use collect_ignore rather than pytest_runtest_setup because t-string
# syntax (t"...") causes a SyntaxError at parse time on Python < 3.14,
# so test files must be excluded from collection entirely.
collect_ignore = []
if sys.version_info < (3, 14):
    collect_ignore = ["test_tstring_template.py"]
