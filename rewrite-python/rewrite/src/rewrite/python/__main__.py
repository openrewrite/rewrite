# Copyright 2025 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""``python -m rewrite.python <file.py>`` — print an LST with its type attribution."""

import argparse
import sys
from typing import List, Optional

from rewrite.python.type_report import diff_ty, parse_for_types, print_tree, print_types


def main(argv: Optional[List[str]] = None) -> int:
    parser = argparse.ArgumentParser(
        prog="python -m rewrite.python",
        description="Print a parsed Python LST annotated with its type attribution.",
    )
    parser.add_argument("path", help="the Python file to parse")
    parser.add_argument("--ty", action="store_true",
                        help="attribute types with a ty client (off by default, so the "
                             "zero-config invocation still runs)")
    parser.add_argument("--project-root", metavar="DIR",
                        help="workspace ty resolves imports against (default: the file's directory)")
    parser.add_argument("--only-missing", action="store_true",
                        help="list only the nodes whose type is missing")
    parser.add_argument("--all", action="store_true", dest="all_nodes",
                        help="list every type-bearing node, not just calls and declarations")
    parser.add_argument("--tree", action="store_true",
                        help="print the nested structure with prefixes instead of the listing")
    parser.add_argument("--json", action="store_true", dest="as_json",
                        help="print the listing as JSON")
    parser.add_argument("--diff-ty", action="store_true", dest="diff",
                        help="parse twice, with and without ty, and report the nodes that differ")
    args = parser.parse_args(argv)

    # Each mode reads its own subset of the flags, so one it does not read is a
    # mistake worth naming rather than a table where the caller wants JSON.
    mode = "--diff-ty" if args.diff else "--tree" if args.tree else None
    unread = {
        "--diff-ty": (("only_missing", "--only-missing"), ("as_json", "--json"), ("ty", "--ty")),
        "--tree": (("only_missing", "--only-missing"), ("all_nodes", "--all"), ("as_json", "--json")),
    }
    for dest, flag in unread.get(mode, ()):
        if getattr(args, dest):
            parser.error(f"{mode} does not take {flag}")

    if args.diff:
        diff_ty(args.path, project_root=args.project_root, all_nodes=args.all_nodes)
        return 0

    cu = parse_for_types(args.path, with_types=args.ty, project_root=args.project_root)
    if args.tree:
        print_tree(cu)
        return 0

    print_types(cu, only_missing=args.only_missing, all_nodes=args.all_nodes,
                as_json=args.as_json)
    return 0


if __name__ == "__main__":
    sys.exit(main())
