import difflib
from pathlib import Path


class Result:
    @staticmethod
    def diff(before: str, after: str, path: Path) -> str:
        old_lines = before.splitlines(keepends=True)
        new_lines = after.splitlines(keepends=True)
        diff = difflib.unified_diff(old_lines, new_lines, fromfile=str(path), tofile=str(path), lineterm='')
        return ''.join(diff)
