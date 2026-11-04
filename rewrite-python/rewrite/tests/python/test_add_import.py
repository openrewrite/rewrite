# Copyright 2026 the original author or authors.
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

"""Tests for AddImport / maybe_add_import."""

from pathlib import Path

import pytest

from rewrite import ExecutionContext, InMemoryExecutionContext
from rewrite.java import J
from rewrite.python.add_import import AddImportOptions, maybe_add_import
from rewrite.java.tree import Import
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor
from rewrite.test import RecipeSpec, python, from_visitor

from ._java_import_arm import java_add_import
from ._parse_shape import parse_shape


def _assert_ids_accessible(source_file):
    """Touch every node's public `.id`; raises if any `_id` holds a UUID instead of an int."""
    class IdWalker(PythonVisitor[ExecutionContext]):
        def pre_visit(self, tree, p):
            assert tree.id is not None
            return tree

    IdWalker().visit(source_file, InMemoryExecutionContext())


@pytest.fixture(params=[
    pytest.param('native'),
    pytest.param('java', marks=pytest.mark.requires_java_rpc),
])
def arm(request):
    """Run each case against the Python visitor in-process and against the Java visitor over RPC."""
    if request.param == 'java':
        request.getfixturevalue('java_rpc')
    return request.param


def _add_import_visitor(arm, module, name=None, alias=None, only_if_referenced=False):
    """Build a visitor that registers a single add-import request."""
    if arm == 'java':
        return java_add_import(module, name, alias, only_if_referenced)

    class _V(PythonVisitor[ExecutionContext]):
        def visit_compilation_unit(self, cu: CompilationUnit, p: ExecutionContext) -> J:
            maybe_add_import(self, AddImportOptions(
                module=module,
                name=name,
                alias=alias,
                only_if_referenced=only_if_referenced,
            ))
            return super().visit_compilation_unit(cu, p)

    return _V()


class TestMaybeAddImport:
    """Tests for maybe_add_import scheduling via _after_visit."""

    def test_add_from_import(self, arm):
        """Add 'from os.path import join' to a file that uses join."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'join')))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                """
                from os.path import join
                x = 1
                """,
            )
        )

    def test_add_to_a_comment_only_file(self):
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor('native', 'os', 'path')))
        spec.rewrite_run(
            python(
                "# c\n",
                "from os import path\n# c\n",
                after_recipe=parse_shape("from os import path\n# c\n"),
            )
        )

    def test_add_direct_import(self, arm):
        """Add 'import os' to a file, as the bare `Import` a parse of it gives."""
        def bare_import(cu):
            assert isinstance(cu.statements[0], Import)

        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os')))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                """
                import os
                x = 1
                """,
                after_recipe=bare_import,
            )
        )

    def test_no_duplicate_import(self, arm):
        """Don't add an import that already exists."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os')))
        spec.rewrite_run(
            python(
                """
                import os
                x = 1
                """,
            )
        )

    def test_builtin_name_is_not_imported(self, arm):
        """A bare builtin name (e.g. from ChangeType retargeting to `list`) is not a module;
        adding an import for it is a no-op."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'list')))
        spec.rewrite_run(
            python(
                """
                x: list[int] = []
                """,
            )
        )

    def test_only_if_referenced(self, arm):
        """Don't add import when the name is not referenced and only_if_referenced=True."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', 'join', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
            )
        )

    def test_only_if_referenced_ignores_identifiers_in_imports(self, arm):
        """``pathlib`` occurs only as the module of the existing aliased import."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'pathlib', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                import pathlib as o

                x = o.sep
                """,
            )
        )

    def test_only_if_referenced_ignores_a_member_name_that_merely_matches(self, arm):
        """``json`` occurs only in slots naming a member, never as a reference."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'json', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                def handle(resp, url, body, item):
                    resp.json()
                    post(url, json=body)
                    return item.payload.json
                """,
            )
        )

    def test_only_if_referenced_adds_when_referenced(self, arm):
        """The name is used, so the import is added."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', 'join', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                x = join('a', 'b')
                """,
                """
                from os.path import join
                x = join('a', 'b')
                """,
            )
        )

    def test_only_if_referenced_adds_a_dotted_module_read_through_its_root(self, arm):
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                x = os.path.join('a', 'b')
                """,
                """
                import os.path
                x = os.path.join('a', 'b')
                """,
            )
        )

    @pytest.mark.parametrize('annotation_position, source', [
        ('variable', 'm: "Dict[Any, Any]" = {}'),
        ('parameter', 'def f(m: "Dict[Any, Any]") -> None: ...'),
        ('return', 'def f() -> "Dict[Any, Any]": ...'),
    ])
    def test_only_if_referenced_finds_a_reference_inside_a_string_annotation(
            self, arm, annotation_position, source):
        """A compound forward reference names the symbol, so it is a reference."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'typing', 'Any', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                f"""
                {source}
                """,
                f"""
                from typing import Any
                {source}
                """,
            )
        )

    def test_only_if_referenced_finds_a_reference_nested_inside_a_string_annotation(self, arm):
        """A forward reference nested in another one names the symbol just the same."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'typing', 'Any', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                m: "'Dict[str, Any]'" = {}
                """,
                """
                from typing import Any
                m: "'Dict[str, Any]'" = {}
                """,
            )
        )

    def test_only_if_referenced_finds_a_reference_in_a_comprehension(self, arm):
        """The only reference is inside a comprehension, a Python-specific node."""
        spec = RecipeSpec(recipe=from_visitor(
            _add_import_visitor(arm, 'os.path', 'join', only_if_referenced=True)))
        spec.rewrite_run(
            python(
                """
                paths = [join(p) for p in ps]
                """,
                """
                from os.path import join
                paths = [join(p) for p in ps]
                """,
            )
        )

    def test_merge_into_existing_from_import(self, arm):
        """Merge a new name into an existing 'from X import ...' statement.

        The new member is inserted in case-insensitive alphabetical position
        ('exists' < 'join'), not appended.
        """
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'exists')))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                from os.path import exists, join
                x = 1
                """,
            )
        )

    def test_merge_into_existing_import_keeps_ids_accessible(self, arm):
        """Merging into an existing from-import rebuilds the MultiImport from the
        public `.id` property; the rebuilt node's id must remain accessible
        (regression for the 8.84.8 int-id migration)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'exists')))
        spec.rewrite_run(
            python(
                """
                from os.path import join
                x = 1
                """,
                """
                from os.path import exists, join
                x = 1
                """,
                after_recipe=_assert_ids_accessible,
            )
        )

    def test_merge_inserts_member_in_sorted_position(self, arm):
        """Insert before an existing member when it sorts earlier (the reported bug)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'ClassVar')))
        spec.rewrite_run(
            python(
                """
                from typing import Final
                x = 1
                """,
                """
                from typing import ClassVar, Final
                x = 1
                """,
            )
        )

    def test_merge_inserts_member_in_middle(self, arm):
        """Insert a member between two existing, already-sorted members."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'ClassVar')))
        spec.rewrite_run(
            python(
                """
                from typing import Any, Final
                x = 1
                """,
                """
                from typing import Any, ClassVar, Final
                x = 1
                """,
            )
        )

    def test_merge_appends_when_alphabetically_last(self, arm):
        """Append a member when it sorts after all existing members."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'Final')))
        spec.rewrite_run(
            python(
                """
                from typing import Any, ClassVar
                x = 1
                """,
                """
                from typing import Any, ClassVar, Final
                x = 1
                """,
            )
        )

    def test_merge_is_case_insensitive(self, arm):
        """Ordering is case-insensitive: 'cast' sorts before 'Optional'."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'cast')))
        spec.rewrite_run(
            python(
                """
                from typing import Optional
                x = 1
                """,
                """
                from typing import cast, Optional
                x = 1
                """,
            )
        )

    def test_merge_into_unsorted_list_still_inserts_sorted(self, arm):
        """When existing members are unsorted, the new member is still placed at
        its first sorted position; existing members are not reordered."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'exists')))
        spec.rewrite_run(
            python(
                """
                from os.path import join, abspath
                x = 1
                """,
                """
                from os.path import exists, join, abspath
                x = 1
                """,
            )
        )

    def test_merge_aliased_member_sorted_by_alias(self, arm):
        """An aliased member is sorted by its alias (the bound name)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'Final', alias='abc')))
        spec.rewrite_run(
            python(
                """
                from typing import Optional
                x = 1
                """,
                """
                from typing import Final as abc, Optional
                x = 1
                """,
            )
        )

    def test_new_from_import_added_after_existing_imports(self, arm):
        """A brand-new 'from' import is placed after existing imports; import
        statements are not reordered among themselves."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'mmm', 'w')))
        spec.rewrite_run(
            python(
                """
                from aaa import x
                from zzz import y
                z = 1
                """,
                """
                from aaa import x
                from zzz import y
                from mmm import w
                z = 1
                """,
            )
        )

    def test_skips_builtins_import(self, arm):
        """Names from the 'builtins' module are always available, so no import
        is added (e.g. 'from builtins import list' is redundant)."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'builtins', 'list')))
        spec.rewrite_run(
            python(
                """
                x: list = []
                """,
            )
        )

    def test_skips_builtins_direct_import(self, arm):
        """A direct 'import builtins' is likewise not added."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'builtins')))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
            )
        )

    def test_adds_aliased_builtins_import(self, arm):
        """An explicit alias makes a builtins import meaningful, so it is added."""
        spec = RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'builtins', 'list', '_list')))
        spec.rewrite_run(
            python(
                """
                x = 1
                """,
                """
                from builtins import list as _list
                x = 1
                """,
            )
        )


class TestCanonicalAddImportDedup:
    """An existing import already satisfies a requested (module, name) when
    its canonical FQN matches (``posixpath.join`` is canonically ``os.path.join``),
    not just when its written path does."""

    def test_canonical_request_matches_written_from_import(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os.path', 'join'))).rewrite_run(
            python(
                """
                from posixpath import join
                x = join('a', 'b')
                """,
            )
        )

    def test_canonical_request_matches_written_class_import(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'typing', 'Iterable'))).rewrite_run(
            python(
                """
                from collections.abc import Iterable
                def f(x: Iterable): ...
                """,
            )
        )

    def test_canonical_match_requires_same_bound_name(self, arm):
        """An aliased binding does not satisfy a request for the plain name."""
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'posixpath', 'join'))).rewrite_run(
            python(
                """
                from os.path import join as j
                x = 1
                """,
                """
                from os.path import join as j
                from posixpath import join
                x = 1
                """,
            )
        )

    def test_canonical_mismatch_still_adds(self, arm):
        """os.path.exists is canonically genericpath.exists, so a posixpath
        request is a different symbol and gets its own import."""
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'posixpath', 'exists'))).rewrite_run(
            python(
                """
                from os.path import exists
                x = 1
                """,
                """
                from os.path import exists
                from posixpath import exists
                x = 1
                """,
            )
        )


class TestAddImportLineBreaks:
    """A synthesized line break is the one the file is written with."""

    @pytest.mark.parametrize("module, name, before, after", [
        pytest.param('os', 'path', "import sys\r\n\r\nx = 1\r\n",
                     "import sys\r\nfrom os import path\r\n\r\nx = 1\r\n", id='after-imports'),
        pytest.param('os', 'path', "x = 1\r\ny = 2\r\n",
                     "from os import path\r\nx = 1\r\ny = 2\r\n", id='first-statement'),
        pytest.param('os', 'path', "\r\nx = 1\r\n",
                     "from os import path\r\nx = 1\r\n", id='first-statement-after-a-blank-line'),
        pytest.param('os', None, '"""Doc."""\r\nx = 1\r\n',
                     '"""Doc."""\r\nimport os\r\nx = 1\r\n', id='after-docstring'),
        pytest.param('os', None, "# c\r\n", "import os\r\n# c\r\n", id='comment-only'),
    ])
    def test_crlf(self, arm, module, name, before, after):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, module, name))).rewrite_run(
            python(before, after))

    def test_lf_in_a_file_mostly_written_with_lf(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os', 'path'))).rewrite_run(
            python(
                "import sys\n\nx = 1\r\ny = 2\n",
                "import sys\nfrom os import path\n\nx = 1\r\ny = 2\n",
            )
        )
class TestAddImportGrouping:
    """Where the imports already sit in isort's sections, a new import joins its own."""

    GROUPED = (
        "import os\n"
        "import sys\n"
        "\n"
        "import requests\n"
        "\n"
        "from mypkg import util\n"
        "\n"
        "x = 1\n"
    )

    @pytest.mark.parametrize("module, name, after", [
        pytest.param('json', None,
                     "import json\nimport os\nimport sys\n\nimport requests\n\nfrom mypkg import util\n\nx = 1\n",
                     id='first-in-stdlib'),
        pytest.param('collections.abc', 'Mapping',
                     "import os\nimport sys\nfrom collections.abc import Mapping\n\nimport requests\n\n"
                     "from mypkg import util\n\nx = 1\n",
                     id='from-after-straight-in-stdlib'),
        pytest.param('attr', None,
                     "import os\nimport sys\n\nimport attr\nimport requests\n\nfrom mypkg import util\n\nx = 1\n",
                     id='first-in-third-party'),
        pytest.param('yaml', None,
                     "import os\nimport sys\n\nimport requests\nimport yaml\n\nfrom mypkg import util\n\nx = 1\n",
                     id='last-in-third-party'),
        pytest.param('mypkg.helpers', 'helper',
                     "import os\nimport sys\n\nimport requests\n\nfrom mypkg import util\n"
                     "from mypkg.helpers import helper\n\nx = 1\n",
                     id='local-by-path'),
        pytest.param('__future__', 'annotations',
                     "from __future__ import annotations\n\nimport os\nimport sys\n\nimport requests\n\n"
                     "from mypkg import util\n\nx = 1\n",
                     id='new-future-group'),
    ])
    def test_grouped(self, arm, module, name, after):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, module, name))).rewrite_run(
            python(self.GROUPED, after, path=Path('mypkg/sub/mod.py')))

    def test_a_section_the_file_lacks_gets_its_own_group(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'mypkg'))).rewrite_run(
            python(
                "import os\n\nimport requests\n\nx = 1\n",
                "import os\n\nimport requests\n\nimport mypkg\n\nx = 1\n",
                path=Path('mypkg/mod.py'),
            )
        )

    def test_a_group_ahead_of_the_first_keeps_the_header_above_it(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os', 'path'))).rewrite_run(
            python(
                '"""Doc."""\n\nimport requests  # c\n\nfrom . import util\n\nx = 1\n',
                '"""Doc."""\n\nfrom os import path\n\nimport requests  # c\n\nfrom . import util\n\nx = 1\n',
            )
        )

    def test_one_run_of_imports_shows_no_layout(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'json'))).rewrite_run(
            python(
                "import requests\nimport yaml\n\nx = 1\n",
                "import requests\nimport yaml\nimport json\n\nx = 1\n",
            )
        )

    def test_crlf(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'json'))).rewrite_run(
            python(
                "import os\r\n\r\nimport requests\r\n\r\nx = 1\r\n",
                "import json\r\nimport os\r\n\r\nimport requests\r\n\r\nx = 1\r\n",
            )
        )

    def test_ungrouped_imports_take_the_new_one_last(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'json'))).rewrite_run(
            python(
                "import os\nimport requests\nimport sys\n\nx = 1\n",
                "import os\nimport requests\nimport sys\nimport json\n\nx = 1\n",
            )
        )

    def test_a_trailing_comment_stays_on_its_line(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'json'))).rewrite_run(
            python(
                "import os\nimport requests  # pinned\n\nx = 1\n",
                "import os\nimport requests  # pinned\nimport json\n\nx = 1\n",
            )
        )

    def test_a_trailing_comment_stays_on_its_line_inside_a_group(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'pathlib'))).rewrite_run(
            python(
                "import os  # first\nimport sys\n\nimport requests\n\nx = 1\n",
                "import os  # first\nimport pathlib\nimport sys\n\nimport requests\n\nx = 1\n",
            )
        )

    def test_a_trailing_comment_ending_the_file_stays_on_its_line(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'yaml'))).rewrite_run(
            python(
                "import os\n\nimport requests  # pinned\n",
                "import os\n\nimport requests  # pinned\nimport yaml\n",
            )
        )

    def test_a_trailing_comment_stays_on_its_line_ahead_of_a_new_group(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'mypkg'))).rewrite_run(
            python(
                "import os\n\nimport requests  # pinned\n\nx = 1\n",
                "import os\n\nimport requests  # pinned\n\nimport mypkg\n\nx = 1\n",
                path=Path('mypkg/mod.py'),
            )
        )

    def test_a_future_import_precedes_every_statement_but_the_header(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, '__future__', 'annotations'))).rewrite_run(
            python(
                '#!/usr/bin/env python\n"""Doc."""\nimport os\nimport requests\n\nx = 1\n',
                '#!/usr/bin/env python\n"""Doc."""\nfrom __future__ import annotations\nimport os\nimport requests\n\n'
                'x = 1\n',
            )
        )

        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, '__future__', 'annotations'))).rewrite_run(
            python(
                '"""Doc."""\n__version__ = "1"\nimport os\n\nimport requests\n',
                '"""Doc."""\nfrom __future__ import annotations\n__version__ = "1"\nimport os\n\nimport requests\n',
            )
        )

    def test_a_comment_directly_above_an_import_stays_with_it_unless_it_heads_the_file(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'attr'))).rewrite_run(
            python(
                "import os\n\n# registers the codecs\nimport requests\n\nx = 1\n",
                "import os\n\nimport attr\n# registers the codecs\nimport requests\n\nx = 1\n",
            )
        )

        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'json'))).rewrite_run(
            python(
                "#!/usr/bin/env python\n# Copyright the authors\n# Licensed under the Apache License\n"
                "import os\n\nimport requests\n",
                "#!/usr/bin/env python\n# Copyright the authors\n# Licensed under the Apache License\n"
                "import json\nimport os\n\nimport requests\n",
            )
        )

    def test_an_import_goes_below_a_shebang(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os'))).rewrite_run(
            python(
                "#!/usr/bin/env python\nx = 1\n",
                "#!/usr/bin/env python\nimport os\nx = 1\n",
            )
        )

        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os'))).rewrite_run(
            python(
                "#!/usr/bin/env python\n\nx = 1\n",
                "#!/usr/bin/env python\n\nimport os\n\nx = 1\n",
            )
        )

        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os'))).rewrite_run(
            python(
                "#!/usr/bin/env python\n",
                "#!/usr/bin/env python\nimport os\n",
            )
        )

    def test_the_first_import_of_a_file_goes_below_its_header_comments_apart_from_the_code(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'os'))).rewrite_run(
            python(
                '"""Doc."""\n\n# set up logging first\nsetup()\n',
                '"""Doc."""\n\n# set up logging first\nimport os\n\nsetup()\n',
            )
        )

    def test_a_from_import_joins_the_from_run_it_sorts_into(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'collections.abc', 'Iterable'))).rewrite_run(
            python(
                "import os\nimport sys\n\nfrom typing import Any\n\nx = 1\n",
                "import os\nimport sys\n\nfrom collections.abc import Iterable\nfrom typing import Any\n\nx = 1\n",
            )
        )

        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'zlib', 'crc32'))).rewrite_run(
            python(
                "import os\n\nfrom abc import ABC\n\nfrom typing import Any\n\nimport requests\n",
                "import os\n\nfrom abc import ABC\n\nfrom typing import Any\nfrom zlib import crc32\n\n"
                "import requests\n",
            )
        )

    def test_first_party_packages_are_the_top_level_and_src_directories(self, arm):
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'google.cloud', 'storage'))).rewrite_run(
            python(
                "import os\n\nimport requests\n\nx = 1\n",
                "import os\n\nimport requests\nfrom google.cloud import storage\n\nx = 1\n",
                path=Path('providers/google/hook.py'),
            )
        )
        RecipeSpec(recipe=from_visitor(_add_import_visitor(arm, 'mypkg'))).rewrite_run(
            python(
                "import os\n\nimport requests\n\nx = 1\n",
                "import os\n\nimport requests\n\nimport mypkg\n\nx = 1\n",
                path=Path('src/mypkg/mod.py'),
            )
        )
