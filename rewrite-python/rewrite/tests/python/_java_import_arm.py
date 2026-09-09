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

"""Drive the Java-side import visitors over RPC, so their pre-dispatch predicate runs.

The Java visitor decides whether the file can change and, when it can, dispatches back to the
Python implementation over the same connection. Both arms of the import tests therefore end in
the same edit code, and only the Java predicate differs between them.
"""

from typing import Any, Optional

from rewrite.java import J
from rewrite.python.tree import CompilationUnit
from rewrite.python.visitor import PythonVisitor

_ADD = 'org.openrewrite.python.service.PythonAddImportVisitor'
_REMOVE = 'org.openrewrite.python.service.PythonRemoveImportVisitor'
_PY_CU = 'org.openrewrite.python.tree.Py$CompilationUnit'


class _JavaImportVisitor(PythonVisitor):
    def __init__(self, visitor_name: str, options: dict):
        super().__init__()
        self._visitor_name = visitor_name
        self._options = options

    def visit_compilation_unit(self, cu: CompilationUnit, p: Any) -> J:
        from rewrite.execution import ExecutionContext
        from rewrite.rpc.server import (get_object_from_java, local_object, local_objects,
                                        send_request)

        tree_id = str(cu.id)
        local_objects[tree_id] = cu
        params = {
            'visitor': self._visitor_name,
            'treeId': tree_id,
            'sourceFileType': _PY_CU,
            'visitorOptions': self._options,
        }
        if isinstance(p, ExecutionContext):
            params['p'] = local_object(p)

        result = send_request('Visit', params)
        if not (result and result.get('modified', False)):
            return cu
        after = get_object_from_java(tree_id, _PY_CU)
        return after if after is not None else cu


def java_add_import(module: str, name: Optional[str] = None, alias: Optional[str] = None,
                    only_if_referenced: bool = False) -> PythonVisitor:
    return _JavaImportVisitor(_ADD, {
        'module': module,
        'name': name,
        'alias': alias,
        'onlyIfReferenced': only_if_referenced,
    })


def java_remove_import(module: str, name: Optional[str] = None,
                       only_if_unused: bool = True) -> PythonVisitor:
    return _JavaImportVisitor(_REMOVE, {
        'module': module,
        'name': name,
        'onlyIfUnused': only_if_unused,
    })
