__path__ = __import__('pkgutil').extend_path(__path__, __name__)

# helps pytest to rewrite the assert statements in test.py
try:
    import pytest
    pytest.register_assert_rewrite("rewrite.test")
except ImportError:
    pass  # pytest not available, skip assert rewriting

from .execution import ExecutionContext, DelegatingExecutionContext, InMemoryExecutionContext, \
    RecipeRunException, LargeSourceSet, InMemoryLargeSourceSet, Result
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
