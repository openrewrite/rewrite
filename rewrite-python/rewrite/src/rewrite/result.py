import difflib
from pathlib import Path


class Result:
    @staticmethod
    def diff(before: str, after: str, path: Path) -> str:
        old_lines = before.splitlines(keepends=True)
        new_lines = after.splitlines(keepends=True)
        # The lines keep their own terminators, so the headers need theirs too
        # (the difflib default) or they run into the content that follows.
        diff = difflib.unified_diff(old_lines, new_lines, fromfile=str(path), tofile=str(path))
        return ''.join(diff)
