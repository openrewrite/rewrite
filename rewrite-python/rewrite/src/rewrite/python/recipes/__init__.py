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

"""Python recipes for OpenRewrite."""

from rewrite.category import CategoryDescriptor
from rewrite.marketplace import Python

from .remove_pass import RemovePass
from .change_import import ChangeImport

# Category constants for Python recipes
# Use these when decorating recipes with @recipe(category=...)
# Note: Also defined in remove_pass.py to avoid circular imports
Cleanup = [*Python, CategoryDescriptor(display_name="Cleanup")]
Imports = [*Python, CategoryDescriptor(display_name="Imports")]

__all__ = [
    "RemovePass",
    "ChangeImport",
    # Category constants
    "Python",
    "Cleanup",
    "Imports",
]
