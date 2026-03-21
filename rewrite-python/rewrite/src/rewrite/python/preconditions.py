# Copyright 2025 the original author or authors.
#
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://docs.moderne.io/licensing/moderne-source-available-license
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""
Precondition helpers that delegate to Java search recipes via RPC.

These functions provide preconditions for Python recipes that need to check
if source files match certain criteria before applying transformations.
When RPC is available, they delegate to the Java implementations for efficiency.

Example:
    from rewrite.python.preconditions import uses_type, uses_method

    class MyRecipe(Recipe):
        def preconditions(self):
            return uses_type("datetime.datetime")
"""

from typing import Any, Optional, Union

from rewrite.rpc.java_recipe import prepare_java_recipe, PreparedJavaRecipe


def has_source_path(file_pattern: str) -> PreparedJavaRecipe:
    """
    Create a precondition that matches source files by path pattern.

    Delegates to org.openrewrite.FindSourceFiles.

    Args:
        file_pattern: Glob pattern to match file paths (e.g., "**/*.py")

    Returns:
        A prepared recipe that can be used as a precondition
    """
    return prepare_java_recipe("org.openrewrite.FindSourceFiles", {
        "filePattern": file_pattern
    })


def uses_method(method_pattern: str, match_overrides: bool = False) -> PreparedJavaRecipe:
    """
    Create a precondition that matches files using a specific method.

    Delegates to org.openrewrite.java.search.HasMethod.

    Args:
        method_pattern: Method pattern to search for (e.g., "datetime.datetime utcnow()")
        match_overrides: Whether to also match method overrides

    Returns:
        A prepared recipe that can be used as a precondition
    """
    return prepare_java_recipe("org.openrewrite.java.search.HasMethod", {
        "methodPattern": method_pattern,
        "matchOverrides": match_overrides
    })


def uses_type(fully_qualified_type: str, check_assignability: bool = False) -> PreparedJavaRecipe:
    """
    Create a precondition that matches files using a specific type.

    Delegates to org.openrewrite.java.search.HasType.

    Args:
        fully_qualified_type: Fully qualified type name (e.g., "datetime.datetime")
        check_assignability: Whether to check type assignability

    Returns:
        A prepared recipe that can be used as a precondition
    """
    return prepare_java_recipe("org.openrewrite.java.search.HasType", {
        "fullyQualifiedTypeName": fully_qualified_type,
        "checkAssignability": check_assignability
    })


def find_methods(method_pattern: str, match_overrides: bool = False) -> PreparedJavaRecipe:
    """
    Create a recipe that finds and marks methods matching a pattern.

    Delegates to org.openrewrite.java.search.FindMethods.

    Args:
        method_pattern: Method pattern to search for
        match_overrides: Whether to also match method overrides

    Returns:
        A prepared recipe that marks matching methods
    """
    return prepare_java_recipe("org.openrewrite.java.search.FindMethods", {
        "methodPattern": method_pattern,
        "matchOverrides": match_overrides
    })


def find_types(fully_qualified_type: str) -> PreparedJavaRecipe:
    """
    Create a recipe that finds and marks usages of a type.

    Delegates to org.openrewrite.java.search.FindTypes.

    Args:
        fully_qualified_type: Fully qualified type name to find

    Returns:
        A prepared recipe that marks matching type usages
    """
    return prepare_java_recipe("org.openrewrite.java.search.FindTypes", {
        "fullyQualifiedTypeName": fully_qualified_type
    })
