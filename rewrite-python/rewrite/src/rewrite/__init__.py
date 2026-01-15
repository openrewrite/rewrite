__path__ = __import__('pkgutil').extend_path(__path__, __name__)

# helps pytest to rewrite the assert statements in test.py
try:
    import pytest
    pytest.register_assert_rewrite("rewrite.test")
except ImportError:
    pass  # pytest not available, skip assert rewriting

from .category import CategoryDescriptor, LOWEST_PRECEDENCE, DEFAULT_PRECEDENCE, HIGHEST_PRECEDENCE
from .decorators import categorize, get_recipe_category
from .discovery import discover_recipes, discover_decorated_recipes_in_module
from .execution import ExecutionContext, DelegatingExecutionContext, InMemoryExecutionContext, \
    RecipeRunException, LargeSourceSet, InMemoryLargeSourceSet, Result
from .marketplace import RecipeMarketplace, Python
from .recipe import Recipe, ScanningRecipe, option, OptionDescriptor, RecipeDescriptor
from .markers import *
from .parser import *
from .result import *
from .style import *
from .tree import Checksum, FileAttributes, SourceFile, Tree, PrintOutputCapture, PrinterFactory
from .utils import random_id, list_find, list_map, list_map_last
from .visitor import Cursor, TreeVisitor

__all__ = [
    # Tree
    'Checksum',
    'FileAttributes',
    'SourceFile',
    'Tree',
    'PrintOutputCapture',
    'PrinterFactory',

    # Utils
    'random_id',
    'list_find',
    'list_map',
    'list_map_last',

    # Visitor
    'Cursor',
    'TreeVisitor',

    # Execution
    'ExecutionContext',
    'DelegatingExecutionContext',
    'InMemoryExecutionContext',
    'RecipeRunException',
    'LargeSourceSet',
    'InMemoryLargeSourceSet',
    'Result',

    # Recipe
    'Recipe',
    'ScanningRecipe',
    'option',
    'OptionDescriptor',
    'RecipeDescriptor',

    # Categories and Marketplace
    'CategoryDescriptor',
    'LOWEST_PRECEDENCE',
    'DEFAULT_PRECEDENCE',
    'HIGHEST_PRECEDENCE',
    'RecipeMarketplace',
    'Python',
    'categorize',
    'get_recipe_category',
    'discover_recipes',
    'discover_decorated_recipes_in_module',
    'activate',

    # Markers
    'Marker',
    'Markers',
    'SearchResult',
    'UnknownJavaMarker',
    'ParseExceptionResult',

    # Parser
    'Parser',
    'ParserInput',
    'ParseError',
    'ParseErrorVisitor',
    'ParserBuilder',

    # Style
    'Style',
    'NamedStyles',
    'GeneralFormatStyle',
]


def activate(marketplace: RecipeMarketplace) -> None:
    """
    Install all recipes in this package into the marketplace.

    This function is called by the discovery mechanism when the
    openrewrite package is found via entry points. It installs all
    the built-in Python recipes into the provided marketplace.

    Args:
        marketplace: The RecipeMarketplace to install recipes into
    """
    from rewrite.decorators import get_recipe_category
    from rewrite.python.recipes import RemovePass

    # Install all Python recipes with their categories
    category = get_recipe_category(RemovePass)
    if category is not None:
        marketplace.install(RemovePass, category)
