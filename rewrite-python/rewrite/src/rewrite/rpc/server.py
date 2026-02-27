#!/usr/bin/env python3
"""
OpenRewrite Python RPC Server

This server handles JSON-RPC requests from the Java OpenRewrite process.
It parses Python source files using Python's AST module and returns the
results in the OpenRewrite LST format.

Usage:
    python -m rewrite.rpc.server [options]

Options:
    --log-file PATH          Log file path
    --metrics-csv PATH       Metrics CSV output path
    --trace-rpc-messages     Enable RPC message tracing

Bidirectional RPC:
    This server supports bidirectional communication with Java. When Python
    needs an object that Java holds (e.g., for printing), it can send a
    GetObject request to Java and receive the serialized object data.
"""

import argparse
import ast
import json
import logging
import os
import select
import sys
import tempfile
import traceback
import threading

from pathlib import Path
from typing import Dict, Any, Optional, List, Callable
from uuid import uuid4

# Deeply nested LST nodes (e.g., 256 implicitly concatenated strings) can
# overflow the default recursion limit (1000) during RPC serialization.
sys.setrecursionlimit(sys.getrecursionlimit() * 2)

# Configure logging - log to file by default to avoid filling stderr buffer
# (which can cause deadlock if parent process doesn't read stderr)
_default_log_file = os.path.join(tempfile.gettempdir(), 'python-rpc.log')
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(levelname)s - %(message)s',
    filename=_default_log_file,
    filemode='a'
)
logger = logging.getLogger(__name__)

# Local object storage - maps object IDs to parsed trees (objects Python created)
local_objects: Dict[str, Any] = {}
# Remote object storage - maps object IDs to objects fetched from Java
remote_objects: Dict[str, Any] = {}
# Remote refs - maps reference IDs to objects for cyclic graph handling
remote_refs: Dict[int, Any] = {}

# Request ID counter for outgoing requests
_request_id_counter = 0
_request_id_lock = threading.Lock()

# Pending requests waiting for responses
_pending_requests: Dict[int, Any] = {}
_pending_lock = threading.Lock()

# Flag for trace mode
_trace_rpc = False

# Python version to parse (read from environment, default to "3")
# Set REWRITE_PYTHON_VERSION to "2" or "2.7" to parse Python 2 code
_python_version = os.environ.get("REWRITE_PYTHON_VERSION", "3")


def _next_request_id() -> int:
    """Generate a unique request ID for outgoing requests."""
    global _request_id_counter
    with _request_id_lock:
        _request_id_counter += 1
        return _request_id_counter


def send_request(method: str, params: dict, timeout_seconds: float = 10.0) -> Any:
    """Send a JSON-RPC request to Java and wait for the response.

    This enables bidirectional communication - Python can request
    objects from Java while processing an incoming request.

    Args:
        method: The RPC method name
        params: The request parameters
        timeout_seconds: Maximum time to wait for response (default 10s)

    Returns:
        The result from the RPC response

    Raises:
        RuntimeError: If request times out or fails
    """
    request_id = _next_request_id()

    request = {
        'jsonrpc': '2.0',
        'id': request_id,
        'method': method,
        'params': params
    }

    if _trace_rpc:
        logger.debug(f"Sending request to Java: {json.dumps(request)}")

    # Send the request to Java via stdout
    write_message(request)

    # Read the response from Java with timeout
    response = read_message_with_timeout(timeout_seconds)

    if response is None:
        raise RuntimeError(f"No response received for {method} request (timeout after {timeout_seconds}s)")

    if _trace_rpc:
        logger.debug(f"Received response from Java: {json.dumps(response)}")

    # Check for errors
    if 'error' in response:
        error = response['error']
        raise RuntimeError(f"RPC error from Java: {error.get('message', 'Unknown error')}")

    return response.get('result')


def get_object_from_java(obj_id: str, source_file_type: Optional[str] = None) -> Any:
    """Fetch an object from Java and deserialize it using PythonRpcReceiver.

    This ALWAYS sends a GetObject RPC to Java, even if we have a local copy.
    The local copy (in remote_objects) represents our understanding of what
    Java had at some point, and the GetObject response will contain any diffs
    that need to be applied to get the current state.

    This is how the RPC protocol works - each side tracks local and remote
    state, and GetObject transmits only the changes (diffs) between states.

    IMPORTANT: GetObject returns data in batches. We must keep calling GetObject
    until we receive a batch containing END_OF_OBJECT marker.
    """
    from rewrite.rpc.receive_queue import RpcReceiveQueue
    from rewrite.rpc.python_receiver import PythonRpcReceiver

    # Track whether we've received the complete object
    received_end = False

    def pull_batch() -> List[Dict[str, Any]]:
        """Pull the next batch of RpcObjectData from Java.

        This is called by RpcReceiveQueue when it needs more data.
        We send GetObject requests repeatedly until END_OF_OBJECT is received.
        For large objects (>1000 items), Java sends data in multiple batches.

        IMPORTANT: We filter out END_OF_OBJECT from the returned batch to prevent
        it from being accidentally consumed during nested operations (like receive_list
        expecting positions). Java's RewriteRpc.java explicitly consumes END_OF_OBJECT
        after receive() completes (line 474), and we do the same by tracking received_end.
        """
        nonlocal received_end

        if received_end:
            return []

        # Request the next batch from Java
        batch = send_request('GetObject', {
            'id': obj_id,
            'sourceFileType': source_file_type
        })

        if not batch:
            received_end = True
            return []

        # Check if this batch contains END_OF_OBJECT (last item has state='END_OF_OBJECT')
        # The END_OF_OBJECT marker is always at the end of the final batch
        # We filter it out to prevent it from being consumed during nested operations
        if batch[-1].get('state') == 'END_OF_OBJECT':
            received_end = True
            batch = batch[:-1]  # Remove END_OF_OBJECT from the batch

        return batch

    q = RpcReceiveQueue(remote_refs, source_file_type, pull_batch, trace=_trace_rpc)
    receiver = PythonRpcReceiver()

    # Get the "before" state - our understanding of what Java had
    # This is used to apply diffs from the GetObject response
    before = remote_objects.get(obj_id)

    # Receive and deserialize the object (applies diffs to before state)
    try:
        obj = receiver.receive(before, q)

        # After receive() completes, END_OF_OBJECT may still be pending in a
        # separate batch (happens when data items are an exact multiple of the
        # handler's batchSize). Drain it — analogous to Java's explicit
        # q.take() after receive().
        if not received_end:
            pull_batch()
        if not received_end:
            raise RuntimeError(f"Did not receive END_OF_OBJECT marker for object {obj_id}")
    except Exception:
        # Reset our tracking of the remote state so the next interaction
        # forces a full object sync (ADD) instead of a delta (CHANGE).
        remote_objects.pop(obj_id, None)
        raise

    if obj is not None:
        # Update our understanding of what Java has
        remote_objects[obj_id] = obj
        # Also update local_objects for consistency
        local_objects[str(obj.id)] = obj

    return obj


def generate_id() -> str:
    """Generate a unique ID for objects."""
    return str(uuid4())


def parse_python_file(path: str, relative_to: Optional[str] = None, ty_client=None) -> dict:
    """Parse a Python file and return its LST."""
    with open(path, 'r', encoding='utf-8') as f:
        source = f.read()
    return parse_python_source(source, path, relative_to, ty_client)


def parse_python_source(source: str, path: str = "<unknown>", relative_to: Optional[str] = None, ty_client=None) -> dict:
    """Parse Python source code and return its LST.

    The parser used depends on the REWRITE_PYTHON_VERSION environment variable:
    - "2" or "2.7": Use parso-based Py2ParserVisitor for Python 2 code
    - "3" (default): Use ast-based ParserVisitor for Python 3 code
    """
    # Compute the source_path that will be stored on the LST
    source_path = Path(path)
    if relative_to is not None:
        try:
            source_path = source_path.relative_to(relative_to)
        except ValueError:
            pass  # path is not under relative_to, keep absolute

    try:
        from rewrite import Markers

        if _python_version.startswith("2"):
            # Python 2: Try Python 3 ast-based parser first (handles most Python 2 code),
            # fall back to parso-based parser for Python 2-specific syntax
            from rewrite.python._parser_visitor import ParserVisitor
            try:
                source_for_ast = source[1:] if source.startswith('\ufeff') else source
                tree = ast.parse(source_for_ast, path)
                cu = ParserVisitor(source, path, ty_client).visit(tree)
            except SyntaxError:
                from rewrite.python._py2_parser_visitor import Py2ParserVisitor
                cu = Py2ParserVisitor(source, path, _python_version).parse()
        else:
            # Python 3: Use standard ast-based parser
            from rewrite.python._parser_visitor import ParserVisitor

            # Strip BOM before parsing (ParserVisitor handles it internally but ast.parse doesn't)
            source_for_ast = source[1:] if source.startswith('\ufeff') else source

            # Parse using Python AST
            tree = ast.parse(source_for_ast, path)

            # Convert to OpenRewrite LST
            cu = ParserVisitor(source, path, ty_client).visit(tree)

        cu = cu.replace(source_path=source_path)
        cu = cu.replace(markers=Markers.EMPTY)

        # Store and return
        obj_id = str(cu.id)
        local_objects[obj_id] = cu
        return {
            'id': obj_id,
            'sourceFileType': 'org.openrewrite.python.tree.Py$CompilationUnit',
            'sourcePath': str(source_path)
        }
    except ImportError as e:
        logger.exception(f"Failed to import parser: {e}")
        return _create_parse_error(str(source_path), str(e), source)
    except SyntaxError as e:
        logger.error(f"Syntax error parsing {path}: {e}")
        return _create_parse_error(str(source_path), str(e), source)
    except Exception as e:
        logger.exception(f"Error parsing {path}: {e}")
        return _create_parse_error(str(source_path), str(e), source)


def _create_parse_error(path: str, message: str, source: str = '') -> dict:
    """Create a parse error result using the proper ParseError class.

    This creates a real ParseError SourceFile that can be properly serialized
    via the RPC protocol, rather than a dict that can't be handled.
    """
    from rewrite.parser import ParseError
    from rewrite.markers import Markers, ParseExceptionResult
    from rewrite import random_id

    # Create a ParseExceptionResult marker with the error info
    # We use 'PythonParser' as the parser type since we don't have a parser instance
    exception_marker = ParseExceptionResult(
        _id=random_id(),
        _parser_type='PythonParser',
        _exception_type='SyntaxError',
        _message=message
    )

    # Create the ParseError with the marker
    parse_error = ParseError(
        _id=random_id(),
        _markers=Markers(random_id(), [exception_marker]),
        _source_path=Path(path),
        _file_attributes=None,
        _charset_name='utf-8',
        _charset_bom_marked=False,
        _checksum=None,
        _text=source,
        _erroneous=None
    )

    obj_id = str(parse_error.id)
    local_objects[obj_id] = parse_error
    return {'id': obj_id, 'sourceFileType': 'org.openrewrite.tree.ParseError', 'sourcePath': path}


def _infer_project_root(inputs: list) -> Optional[str]:
    """Infer the project root from input paths.

    When relativeTo is not provided, look at the input paths to find a
    directory that contains a .venv or pyproject.toml — this is likely
    the project root that ty-types should use.
    """
    for item in inputs:
        path = None
        if isinstance(item, str):
            path = item
        elif isinstance(item, dict):
            path = item.get('sourcePath') or item.get('path')
        if path and os.path.isabs(path):
            parent = os.path.dirname(path)
            # Walk up looking for a project root marker
            for _ in range(10):
                if os.path.isdir(os.path.join(parent, '.venv')) or \
                   os.path.isfile(os.path.join(parent, 'pyproject.toml')):
                    return parent
                up = os.path.dirname(parent)
                if up == parent:
                    break
                parent = up
    return None


def handle_parse(params: dict) -> List[str]:
    """Handle a Parse RPC request."""
    import tempfile
    import shutil

    inputs = params.get('inputs', [])
    relative_to = params.get('relativeTo')
    results = []

    # If no relativeTo provided, try to infer from absolute input paths
    if not relative_to:
        relative_to = _infer_project_root(inputs)

    # Create a ty-types client for this parse batch
    ty_client = None
    tmpdir = None
    try:
        from rewrite.python.ty_client import TyTypesClient
        ty_client = TyTypesClient()
        if relative_to:
            ty_client.initialize(relative_to)
        else:
            # For inline text inputs without a project root, create a temp directory
            # so ty-types can still provide type attribution
            tmpdir = tempfile.mkdtemp(prefix='rewrite-parse-')
            ty_client.initialize(tmpdir)
    except (ImportError, RuntimeError):
        ty_client = None  # ty-types not available

    try:
        for i, input_item in enumerate(inputs):
            if isinstance(input_item, str):
                result = parse_python_file(input_item, relative_to, ty_client)
            elif 'path' in input_item:
                result = parse_python_file(input_item['path'], relative_to, ty_client)
            elif 'text' in input_item or 'source' in input_item:
                source = input_item.get('text') or input_item.get('source')
                path = input_item.get('sourcePath') or input_item.get('relativePath', '<unknown>')
                # For relative paths, write the source under the project root
                # (tmpdir or relative_to) so ty-types can resolve imports from
                # the project's .venv and dependencies.
                base_dir = tmpdir or relative_to
                if base_dir and not os.path.isabs(path):
                    disk_path = os.path.join(base_dir, path)
                    os.makedirs(os.path.dirname(disk_path), exist_ok=True)
                    with open(disk_path, 'w', encoding='utf-8') as f:
                        f.write(source)
                    result = parse_python_source(source, disk_path, base_dir, ty_client)
                else:
                    result = parse_python_source(source, path, relative_to, ty_client)
            else:
                logger.warning(f"  [{i}] unknown input type: {type(input_item)}")
                continue
            results.append(result['id'])
    finally:
        if ty_client is not None:
            ty_client.shutdown()
        if tmpdir is not None:
            shutil.rmtree(tmpdir, ignore_errors=True)

    return results


def handle_parse_project(params: dict) -> List[dict]:
    """Handle a ParseProject RPC request."""
    import fnmatch

    project_path = params.get('projectPath', '.')
    exclusions = params.get('exclusions', ['__pycache__', '.venv', 'venv', '.git', '.tox', '*.egg-info', '.moderne'])
    relative_to = params.get('relativeTo') or project_path

    results = []

    ty_client = None
    try:
        from rewrite.python.ty_client import TyTypesClient
        ty_client = TyTypesClient()
        ty_client.initialize(project_path)
    except (ImportError, RuntimeError):
        pass

    try:
        for root, dirs, files in os.walk(project_path):
            dirs[:] = [d for d in dirs if not any(fnmatch.fnmatch(d, excl) for excl in exclusions)]

            for file in files:
                if file.endswith('.py'):
                    path = os.path.join(root, file)
                    try:
                        result = parse_python_file(path, relative_to, ty_client)
                        results.append(result)
                    except Exception as e:
                        logger.error(f"Error parsing {path}: {e}")
    finally:
        if ty_client is not None:
            ty_client.shutdown()

    return results


def handle_get_object(params: dict) -> List[dict]:
    """Handle a GetObject RPC request.

    This serializes an object for RPC transfer as RpcObjectData[].
    Returns list of RpcObjectData objects that Java can deserialize.

    After sending, we update remote_objects to track that the remote (Java)
    now has this version of the object. This is essential for the diff-based
    RPC protocol to work correctly.
    """
    obj_id = params.get('id')
    source_file_type = params.get('sourceFileType')
    if obj_id is None:
        return [{'state': 'DELETE'}, {'state': 'END_OF_OBJECT'}]
    obj = local_objects.get(obj_id)
    logger.debug(f"handle_get_object: id={obj_id}, type={type(obj).__name__ if obj else 'None'}")

    if obj is None:
        return [
            {'state': 'DELETE'},
            {'state': 'END_OF_OBJECT'}
        ]

    try:
        from rewrite.rpc.send_queue import RpcSendQueue

        # Get the "before" state - what we previously sent to Java
        before = remote_objects.get(obj_id)

        q = RpcSendQueue(source_file_type)
        result = q.generate(obj, before)

        # Update remote_objects to track that Java now has this version
        remote_objects[obj_id] = obj

        return result

    except BaseException as e:
        source_path = getattr(obj, 'source_path', None)
        logger.exception(f"Error serializing object {obj_id} (type={type(obj).__name__}, path={source_path}): {e}")
        return [{'state': 'END_OF_OBJECT'}]


def _serialize_object_fallback(obj: Any) -> List[dict]:
    """Fallback serialization when sender is not available."""
    # For now, just return basic structure
    # This won't work for full LST transfer but is useful for debugging
    result = []

    if hasattr(obj, 'id'):
        result.append({'state': 'CHANGE', 'value': str(obj.id)})
    else:
        result.append({'state': 'NO_CHANGE'})

    result.append({'state': 'END_OF_OBJECT'})
    return result


def handle_get_languages(params: dict) -> List[str]:
    """Handle a GetLanguages RPC request."""
    return ['org.openrewrite.python.tree.Py$CompilationUnit']


def handle_print(params: dict) -> str:
    """Handle a Print RPC request.

    Fetches the tree from Java to ensure we have the latest version,
    including any modifications made by recipes. Java's GetObject handler
    runs on a separate thread (ForkJoinPool), so bidirectional RPC works.
    """
    # Java sends 'treeId', but also accept 'id' for compatibility
    obj_id = params.get('treeId') or params.get('id')
    source_file_type = params.get('sourceFileType')

    logger.debug(f"handle_print: treeId={obj_id}, sourceFileType={source_file_type}")

    if obj_id is None:
        logger.warning("No treeId or id provided")
        return ""

    # Fetch the object from Java to get the latest version (including recipe modifications)
    obj = get_object_from_java(obj_id, source_file_type)

    if obj is None:
        logger.warning(f"Object {obj_id} not found")
        return ""

    # If it's a CompilationUnit, use the printer
    try:
        from rewrite.python.printer import PythonPrinter
        printer = PythonPrinter()
        return printer.print(obj)
    except ImportError as e:
        logger.error(f"Failed to import PythonPrinter: {e}")
        pass
    except Exception as e:
        logger.exception(f"Error printing object: {e}")

    # Fallback: return stored source if available
    if hasattr(obj, 'source'):
        return obj.source
    if isinstance(obj, dict) and 'source' in obj:
        return obj['source']
    return ""


def handle_reset(params: dict) -> bool:
    """Handle a Reset RPC request - clears all cached state."""
    global local_objects, remote_objects, remote_refs
    local_objects.clear()
    remote_objects.clear()
    remote_refs.clear()
    _prepared_recipes.clear()
    _execution_contexts.clear()
    _recipe_accumulators.clear()
    _recipe_phases.clear()

    logger.info("Reset: cleared all cached state")
    return True


# Global marketplace instance (lazily initialized)
_marketplace = None


def _get_marketplace():
    """Get or create the global marketplace instance."""
    global _marketplace
    if _marketplace is None:
        from rewrite.discovery import discover_recipes
        from rewrite.marketplace import RecipeMarketplace
        from rewrite import activate

        # First try to discover from installed packages
        _marketplace = discover_recipes()

        # Also activate local recipes (in case package isn't installed)
        # This ensures recipes work during development
        activate(_marketplace)

    return _marketplace


def handle_install_recipes(params: dict) -> dict:
    """Handle an InstallRecipes RPC request.

    Activates a recipe package in the marketplace. The package should already be
    installed by the caller (e.g., via pip install --target). This handler discovers
    and activates the package's recipes.

    Args:
        params: Dict containing either:
            - 'recipes': str - A local file path (package already installed to target)
            - 'recipes': {'packageName': str, 'version': str|None} - A package spec

    Returns:
        Dict with 'recipesInstalled' count and 'version' (if resolved)
    """
    import importlib
    import importlib.util

    marketplace = _get_marketplace()
    before_count = len(list(marketplace.all_recipes()))

    recipes = params.get('recipes')
    installed_version = None

    if isinstance(recipes, str):
        # Local file path - package should already be installed by caller
        local_path = Path(recipes)
        logger.info(f"Activating recipes from local path: {recipes}")

        # Find and import the package
        # For local paths, we look for the package name from setup.py/pyproject.toml
        package_name = _find_package_name(local_path)
        if package_name:
            _import_and_activate_package(package_name, marketplace, local_path)

    elif isinstance(recipes, dict):
        # Package spec with name and optional version - package should already be installed
        package_name = recipes.get('packageName')
        version = recipes.get('version')

        if not package_name:
            raise ValueError("Package name is required")

        logger.info(f"Activating recipes package: {package_name}")

        # Get the installed version
        try:
            import importlib.metadata
            installed_version = importlib.metadata.version(package_name)
        except Exception:
            pass

        _import_and_activate_package(package_name, marketplace)
    else:
        raise ValueError(f"Invalid recipes parameter: {recipes}")

    after_count = len(list(marketplace.all_recipes()))
    recipes_installed = after_count - before_count

    logger.info(f"InstallRecipes: installed {recipes_installed} recipes")
    return {
        'recipesInstalled': recipes_installed,
        'version': installed_version
    }


def _add_source_to_path(local_path: Path) -> None:
    """Add the package source directory to sys.path so it can be imported.

    Reads [tool.setuptools.packages.find] 'where' from pyproject.toml to
    determine the source directory. Falls back to adding the local_path itself.
    """
    import sys
    if sys.version_info >= (3, 11):
        import tomllib
    else:
        try:
            import tomli as tomllib  # type: ignore[import-not-found]
        except ModuleNotFoundError:
            src_dir = str(local_path)
            if src_dir not in sys.path:
                sys.path.insert(0, src_dir)
            return

    source_dir = local_path
    pyproject_path = local_path / 'pyproject.toml'
    if pyproject_path.exists():
        try:
            with open(pyproject_path, 'rb') as f:
                data = tomllib.load(f)
                where = (data.get('tool', {}).get('setuptools', {})
                         .get('packages', {}).get('find', {}).get('where'))
                if where and isinstance(where, list) and len(where) > 0:
                    source_dir = local_path / where[0]
        except Exception as e:
            logger.warning(f"Failed to read source layout from pyproject.toml: {e}")

    src_str = str(source_dir)
    if src_str not in sys.path:
        logger.info(f"Adding to sys.path: {src_str}")
        sys.path.insert(0, src_str)


def _find_package_name(local_path: Path) -> Optional[str]:
    """Find the package name from a local path."""
    import sys
    if sys.version_info >= (3, 11):
        import tomllib
    else:
        try:
            import tomli as tomllib  # type: ignore[import-not-found]
        except ModuleNotFoundError:
            return None

    # Try pyproject.toml first
    pyproject_path = local_path / 'pyproject.toml'
    if pyproject_path.exists():
        try:
            with open(pyproject_path, 'rb') as f:
                data = tomllib.load(f)
                # Try [project] section first (PEP 621)
                if 'project' in data and 'name' in data['project']:
                    return data['project']['name']
                # Try [tool.poetry] section
                if 'tool' in data and 'poetry' in data['tool'] and 'name' in data['tool']['poetry']:
                    return data['tool']['poetry']['name']
        except Exception as e:
            logger.warning(f"Failed to parse pyproject.toml: {e}")

    # Try setup.py
    setup_py = local_path / 'setup.py'
    if setup_py.exists():
        # Simple heuristic: look for name= in setup.py
        try:
            content = setup_py.read_text()
            import re
            match = re.search(r'name\s*=\s*["\']([^"\']+)["\']', content)
            if match:
                return match.group(1)
        except Exception as e:
            logger.warning(f"Failed to parse setup.py: {e}")

    return None


def _import_and_activate_package(package_name: str, marketplace, local_path: Optional[Path] = None):
    """Import a package and call its activate function using entry points.

    Uses importlib.metadata to discover entry points registered under
    the 'openrewrite.recipes' group and calls their activate functions.
    Since matching package names to entry points is unreliable (hyphens vs
    underscores, different naming conventions), we activate ALL entry points
    but the marketplace handles deduplication.

    If entry points aren't found (e.g., package not pip-installed) and a
    local_path is provided, the source directory is added to sys.path
    as a fallback so the module can be imported directly.
    """
    from importlib.metadata import entry_points

    # Normalize package name for comparison
    def normalize(name: str) -> str:
        return name.replace('-', '_').replace('.', '_').lower()

    normalized_name = normalize(package_name)

    # Find all entry points in the openrewrite.recipes group
    eps = entry_points(group='openrewrite.recipes')
    activated = False

    for ep in eps:
        try:
            # Try to match this entry point to our package using the dist attribute
            dist_name = None
            if hasattr(ep, 'dist') and ep.dist is not None:
                dist_name = ep.dist.name if hasattr(ep.dist, 'name') else str(ep.dist)

            # Check if this entry point belongs to our package
            matches_package = False
            if dist_name and normalize(dist_name) == normalized_name:
                matches_package = True
            elif ep.name and normalize(ep.name) == normalized_name:
                matches_package = True
            elif ':' in ep.value:
                # Check module name in value (e.g., "sample_recipe:activate")
                module_name = ep.value.split(':')[0]
                if normalize(module_name) == normalized_name:
                    matches_package = True

            if matches_package:
                logger.info(f"Loading entry point: {ep.name} -> {ep.value} (dist: {dist_name})")
                activate_fn = ep.load()
                activate_fn(marketplace)
                activated = True
                logger.info(f"Successfully activated {ep.name}")
        except Exception as e:
            logger.warning(f"Failed to process entry point {ep.name}: {e}")

    if not activated:
        # Fallback: try direct module import (for packages without entry points)
        # If a local path was provided, add its source directory to sys.path
        if local_path is not None:
            _add_source_to_path(local_path)

        import importlib
        module_name = package_name.replace('-', '_')
        try:
            if module_name in sys.modules:
                importlib.reload(sys.modules[module_name])
            else:
                importlib.import_module(module_name)

            module = sys.modules.get(module_name)
            if module and hasattr(module, 'activate'):
                logger.info(f"Calling activate() on {module_name}")
                module.activate(marketplace)
            else:
                logger.warning(f"Package {package_name} does not have an activate() function")
        except ImportError as e:
            logger.warning(f"Could not import {module_name}: {e}")


def handle_get_marketplace(params: dict) -> List[dict]:
    """Handle a GetMarketplace RPC request.

    Returns all recipes organized by category in a format compatible with Java.

    Returns:
        List of dicts with 'descriptor' and 'categoryPaths' for each recipe.
    """
    from dataclasses import asdict

    marketplace = _get_marketplace()
    rows: List[dict] = []

    def collect_recipes(category, category_path: List[dict]):
        """Recursively collect recipes from a category and its subcategories."""
        current_path = [*category_path, _category_descriptor_to_dict(category.descriptor)]

        for recipe_name, (recipe_desc, _recipe_class) in category.recipes.items():
            # Check if we already have this recipe (it can appear in multiple categories)
            existing = next((r for r in rows if r['descriptor']['name'] == recipe_desc.name), None)
            if existing:
                existing['categoryPaths'].append(current_path)
            else:
                rows.append({
                    'descriptor': _recipe_descriptor_to_dict(recipe_desc),
                    'categoryPaths': [current_path]
                })

        for subcategory in category.categories:
            collect_recipes(subcategory, current_path)

    # Start from the root's children (skip the root itself)
    for category in marketplace.categories():
        collect_recipes(category, [])

    logger.info(f"GetMarketplace: returning {len(rows)} recipes")
    return rows


def _category_descriptor_to_dict(descriptor) -> dict:
    """Convert a CategoryDescriptor to a dict for JSON serialization."""
    return {
        'displayName': descriptor.display_name,
        'packageName': descriptor.package_name,
        'description': descriptor.description,
        'tags': list(descriptor.tags),
        'root': descriptor.root,
        'priority': descriptor.priority,
        'synthetic': descriptor.synthetic,
    }


def _recipe_descriptor_to_dict(descriptor) -> dict:
    """Convert a RecipeDescriptor to a dict for JSON serialization."""
    return {
        'name': descriptor.name,
        'displayName': descriptor.display_name,
        'description': descriptor.description,
        'tags': descriptor.tags,
        'estimatedEffortPerOccurrence': descriptor.estimated_effort_per_occurrence,
        'options': [
            {
                'name': name,
                'value': _serialize_value(value),
                'displayName': opt.display_name,
                'description': opt.description,
                'example': opt.example,
                'required': opt.required,
                'valid': opt.valid,
            }
            for name, value, opt in descriptor.options
        ],
        'dataTables': descriptor.data_tables,
        'recipeList': [_recipe_descriptor_to_dict(r) for r in descriptor.recipe_list],
    }


def _serialize_value(value) -> Any:
    """Serialize a value to JSON-compatible format."""
    if value is None or isinstance(value, (str, int, float, bool)):
        return value
    if isinstance(value, (list, tuple)):
        return [_serialize_value(v) for v in value]
    if isinstance(value, dict):
        return {k: _serialize_value(v) for k, v in value.items()}
    return str(value)


# Prepared recipes storage - maps recipe IDs to recipe instances
_prepared_recipes: Dict[str, Any] = {}
# Execution contexts storage - maps context IDs to ExecutionContext instances
_execution_contexts: Dict[str, Any] = {}
# Accumulator storage for ScanningRecipes - maps recipe IDs to accumulators
_recipe_accumulators: Dict[str, Any] = {}
# Phase tracking for recipes - maps recipe IDs to 'scan' or 'edit'
_recipe_phases: Dict[str, str] = {}
# Data table output directory - if set, data tables will be written to CSV files
_data_table_output_dir: Optional[str] = None


def handle_prepare_recipe(params: dict) -> dict:
    """Handle a PrepareRecipe RPC request.

    Prepares a recipe for execution by:
    1. Looking up the recipe in the marketplace
    2. Instantiating it with the provided options
    3. Storing it with a unique ID
    4. Returning the descriptor and visitor info

    Args:
        params: dict with 'id' (recipe name), optional 'options', and optional 'dataTableOutputDir'

    Returns:
        dict with 'id', 'descriptor', 'editVisitor', and precondition info
    """
    global _data_table_output_dir

    recipe_name = params.get('id')
    if recipe_name is None:
        raise ValueError("Recipe 'id' is required")
    options = params.get('options', {})

    # Set up data table output directory if specified
    if 'dataTableOutputDir' in params:
        _data_table_output_dir = params['dataTableOutputDir']
        logger.info(f"Data table output directory set to: {_data_table_output_dir}")

    logger.debug(f"PrepareRecipe: id={recipe_name}, options={options}")

    marketplace = _get_marketplace()

    # Look up the recipe - returns (RecipeDescriptor, Type[Recipe]) tuple
    recipe_info = marketplace.find_recipe(recipe_name)
    if recipe_info is None:
        raise ValueError(f"Recipe not found: {recipe_name}")

    _descriptor, recipe_class = recipe_info
    if recipe_class is None:
        raise ValueError(f"Recipe class not found for: {recipe_name}")

    # Instantiate the recipe with options
    if options:
        recipe = recipe_class(**options)
    else:
        recipe = recipe_class()

    # Generate a unique ID for this prepared recipe
    prepared_id = generate_id()
    _prepared_recipes[prepared_id] = recipe

    # Build the response
    descriptor = recipe.descriptor()

    # Determine if this is a scanning recipe
    from rewrite.recipe import ScanningRecipe
    is_scanning = isinstance(recipe, ScanningRecipe)

    response = {
        'id': prepared_id,
        'descriptor': _recipe_descriptor_to_dict(descriptor),
        'editVisitor': f'edit:{prepared_id}',
        'editPreconditions': _get_preconditions(recipe, 'edit'),
        'scanVisitor': f'scan:{prepared_id}' if is_scanning else None,
        'scanPreconditions': _get_preconditions(recipe, 'scan') if is_scanning else [],
    }

    logger.debug(f"PrepareRecipe response: {response}")
    return response


def _get_preconditions(recipe, phase: str) -> List[dict]:
    """Get preconditions for a recipe phase.

    For now, we add a type precondition to ensure only Python files are visited.
    """
    # Add precondition to only visit Python source files
    return [{
        'visitorName': 'org.openrewrite.rpc.internal.FindTreesOfType',
        'visitorOptions': {'type': 'org.openrewrite.python.tree.Py'}
    }]


def handle_visit(params: dict) -> dict:
    """Handle a Visit RPC request.

    Applies a visitor to a tree and returns whether it was modified.

    Args:
        params: dict with 'visitor', 'sourceFileType', 'treeId', 'p' (context id), 'cursor'

    Returns:
        dict with 'modified' boolean
    """
    visitor_name = params.get('visitor')
    source_file_type = params.get('sourceFileType')
    tree_id = params.get('treeId')
    p_id = params.get('p')
    cursor_ids = params.get('cursor')

    if visitor_name is None:
        raise ValueError("'visitor' is required")
    if tree_id is None:
        raise ValueError("'treeId' is required")

    logger.debug(f"Visit: visitor={visitor_name}, treeId={tree_id}, p={p_id}")

    # Get or create execution context
    if p_id and p_id in _execution_contexts:
        ctx = _execution_contexts[p_id]
    else:
        from rewrite import InMemoryExecutionContext
        ctx = InMemoryExecutionContext()
        # Set up data table store if output directory is configured
        if _data_table_output_dir:
            from rewrite.data_table import CsvDataTableStore, DATA_TABLE_STORE
            store = CsvDataTableStore(_data_table_output_dir)
            store.accept_rows(True)
            ctx.put_message(DATA_TABLE_STORE, store)
        if p_id:
            _execution_contexts[p_id] = ctx

    # Always fetch the tree from Java to ensure we have the latest version.
    # Java may have modified the tree (e.g., via a Java-side recipe) since our last sync.
    tree = get_object_from_java(tree_id, source_file_type)

    if tree is None:
        raise ValueError(f"Tree not found: {tree_id}")

    # Instantiate the visitor
    visitor = _instantiate_visitor(visitor_name, ctx)

    # Apply the visitor
    from rewrite.visitor import Cursor
    cursor = Cursor(None, Cursor.ROOT_VALUE)

    before = tree
    after = visitor.visit(tree, ctx, cursor)

    # Update local objects with the result and determine if modified
    # Use referential equality (identity comparison) to detect modifications
    if after is None:
        # Tree was deleted
        if tree_id in local_objects:
            del local_objects[tree_id]
        modified = True
    elif after is not before:
        # Tree object changed - update both the tree_id entry and the new id entry
        local_objects[tree_id] = after
        if str(after.id) != tree_id:
            local_objects[str(after.id)] = after
        modified = True
    else:
        modified = False

    logger.debug(f"Visit result: modified={modified}, tree_id={tree_id}, before.id={before.id}, after.id={after.id if after else None}")
    return {'modified': modified}


def _instantiate_visitor(visitor_name: str, ctx):
    """Instantiate a visitor from its name.

    Visitor names can be:
    - 'edit:<recipe_id>' - get the editor from a prepared recipe
    - 'scan:<recipe_id>' - get the scanner from a prepared scanning recipe

    For ScanningRecipes, the accumulator is persisted across calls so that
    data collected during the scan phase is available during the edit and
    generate phases.
    """
    if visitor_name.startswith('edit:'):
        recipe_id = visitor_name[5:]
        recipe = _prepared_recipes.get(recipe_id)
        if recipe is None:
            raise ValueError(f"Prepared recipe not found: {recipe_id}")

        # Track phase transition
        _recipe_phases[recipe_id] = 'edit'

        # For ScanningRecipe, use the accumulated data from scan phase
        from rewrite.recipe import ScanningRecipe
        if isinstance(recipe, ScanningRecipe):
            # Get existing accumulator or create new one
            if recipe_id not in _recipe_accumulators:
                _recipe_accumulators[recipe_id] = recipe.initial_value(ctx)
            acc = _recipe_accumulators[recipe_id]
            return recipe.editor_with_data(acc)

        return recipe.editor()

    elif visitor_name.startswith('scan:'):
        recipe_id = visitor_name[5:]
        recipe = _prepared_recipes.get(recipe_id)
        if recipe is None:
            raise ValueError(f"Prepared recipe not found: {recipe_id}")
        from rewrite.recipe import ScanningRecipe
        if not isinstance(recipe, ScanningRecipe):
            raise ValueError(f"Recipe is not a scanning recipe: {recipe_id}")

        # Check for phase transition (edit -> scan = new cycle)
        # If we're transitioning from edit back to scan, clear the accumulator
        if _recipe_phases.get(recipe_id) == 'edit':
            _recipe_accumulators.pop(recipe_id, None)
        _recipe_phases[recipe_id] = 'scan'

        # Get existing accumulator or create new one
        if recipe_id not in _recipe_accumulators:
            _recipe_accumulators[recipe_id] = recipe.initial_value(ctx)
        acc = _recipe_accumulators[recipe_id]

        return recipe.scanner(acc)

    else:
        raise ValueError(f"Unknown visitor name format: {visitor_name}")


def handle_generate(params: dict) -> dict:
    """Handle a Generate RPC request.

    Called by the recipe run cycle to generate new source files from scanning recipes.
    For non-scanning recipes, returns an empty list.

    The accumulator used here is the same one that was populated during the scan phase,
    allowing recipes to generate files based on data collected across all source files.

    Args:
        params: dict with 'id' (prepared recipe id) and 'p' (context id)

    Returns:
        dict with 'ids' list and 'sourceFileTypes' list
    """
    recipe_id = params.get('id')
    p_id = params.get('p')

    if recipe_id is None:
        raise ValueError("'id' is required")

    logger.debug(f"Generate: id={recipe_id}, p={p_id}")

    recipe = _prepared_recipes.get(recipe_id)
    if recipe is None:
        raise ValueError(f"Prepared recipe not found: {recipe_id}")

    # Get or create execution context
    if p_id and p_id in _execution_contexts:
        ctx = _execution_contexts[p_id]
    else:
        from rewrite import InMemoryExecutionContext
        ctx = InMemoryExecutionContext()
        # Set up data table store if output directory is configured
        if _data_table_output_dir:
            from rewrite.data_table import CsvDataTableStore, DATA_TABLE_STORE
            store = CsvDataTableStore(_data_table_output_dir)
            store.accept_rows(True)
            ctx.put_message(DATA_TABLE_STORE, store)
        if p_id:
            _execution_contexts[p_id] = ctx

    # Only scanning recipes can generate files
    from rewrite.recipe import ScanningRecipe
    if isinstance(recipe, ScanningRecipe):
        # Use the persisted accumulator from the scan phase, or create new one if not available
        if recipe_id in _recipe_accumulators:
            acc = _recipe_accumulators[recipe_id]
        else:
            acc = recipe.initial_value(ctx)
            _recipe_accumulators[recipe_id] = acc

        generated = recipe.generate(acc, ctx)

        ids = []
        source_file_types = []
        for sf in generated:
            sf_id = str(sf.id)
            local_objects[sf_id] = sf
            ids.append(sf_id)
            source_file_types.append(sf.__class__.__module__ + '.' + sf.__class__.__name__)

        return {'ids': ids, 'sourceFileTypes': source_file_types}

    # Non-scanning recipes don't generate files
    return {'ids': [], 'sourceFileTypes': []}


def handle_request(method: str, params: dict) -> Any:
    """Handle an RPC request."""
    handlers = {
        'Parse': handle_parse,
        'ParseProject': handle_parse_project,
        'GetObject': handle_get_object,
        'GetLanguages': handle_get_languages,
        'Print': handle_print,
        'Reset': handle_reset,
        'InstallRecipes': handle_install_recipes,
        'GetMarketplace': handle_get_marketplace,
        'PrepareRecipe': handle_prepare_recipe,
        'Visit': handle_visit,
        'Generate': handle_generate,
    }

    handler = handlers.get(method)
    if handler:
        return handler(params)
    else:
        raise ValueError(f"Unknown method: {method}")


def read_message_with_timeout(timeout_seconds: float) -> Optional[dict]:
    """Read a JSON-RPC message from stdin with timeout.

    Uses select() on the raw file descriptor and unbuffered binary I/O
    to avoid issues with Python's buffered TextIOWrapper.

    Args:
        timeout_seconds: Maximum time to wait for complete message

    Returns:
        Parsed JSON message, or None on timeout/error
    """
    import time
    import os
    start_time = time.time()
    fd = sys.stdin.fileno()

    def remaining_timeout() -> float:
        elapsed = time.time() - start_time
        return max(0, timeout_seconds - elapsed)

    def read_bytes_with_timeout(n: int) -> Optional[bytes]:
        """Read exactly n bytes from stdin fd with timeout."""
        result = []
        remaining = n
        while remaining > 0:
            timeout = remaining_timeout()
            if timeout <= 0:
                return None
            readable, _, _ = select.select([fd], [], [], timeout)
            if not readable:
                return None
            chunk = os.read(fd, remaining)
            if not chunk:
                return None  # EOF
            result.append(chunk)
            remaining -= len(chunk)
        return b''.join(result)

    def read_line_with_timeout() -> Optional[bytes]:
        """Read a line (up to \n) from stdin fd with timeout."""
        result = []
        while True:
            timeout = remaining_timeout()
            if timeout <= 0:
                return None
            readable, _, _ = select.select([fd], [], [], timeout)
            if not readable:
                return None
            byte = os.read(fd, 1)
            if not byte:
                return None  # EOF
            result.append(byte)
            if byte == b'\n':
                break
        return b''.join(result)

    try:
        # Read Content-Length header
        header_line = read_line_with_timeout()
        if not header_line:
            logger.warning(f"Timeout waiting for RPC response header after {timeout_seconds}s")
            return None

        header_str = header_line.decode('utf-8').strip()
        if not header_str.startswith('Content-Length:'):
            logger.error(f"Invalid header: {header_str}")
            return None

        content_length = int(header_str.split(':')[1].strip())

        # Read empty line (separator)
        separator = read_line_with_timeout()
        if separator is None:
            logger.warning(f"Timeout waiting for header separator")
            return None

        # Read content
        content_bytes = read_bytes_with_timeout(content_length)
        if content_bytes is None:
            logger.warning(f"Timeout waiting for message content")
            return None

        return json.loads(content_bytes.decode('utf-8'))
    except Exception as e:
        logger.error(f"Error reading message with timeout: {e}")
        return None


def read_message() -> Optional[dict]:
    """Read a JSON-RPC message from stdin (blocking, no timeout).

    Uses unbuffered binary I/O (os.read) to be consistent with
    read_message_with_timeout() and avoid buffering conflicts.
    """
    import os
    fd = sys.stdin.fileno()

    def read_line() -> Optional[bytes]:
        """Read a line (up to \n) from stdin fd."""
        result = []
        while True:
            byte = os.read(fd, 1)
            if not byte:
                return None  # EOF
            result.append(byte)
            if byte == b'\n':
                break
        return b''.join(result)

    def read_bytes(n: int) -> Optional[bytes]:
        """Read exactly n bytes from stdin fd."""
        result = []
        remaining = n
        while remaining > 0:
            chunk = os.read(fd, remaining)
            if not chunk:
                return None  # EOF
            result.append(chunk)
            remaining -= len(chunk)
        return b''.join(result)

    try:
        # Read Content-Length header
        header_line = read_line()
        if not header_line:
            return None

        header_str = header_line.decode('utf-8').strip()
        if not header_str.startswith('Content-Length:'):
            logger.error(f"Invalid header: {header_str}")
            return None

        content_length = int(header_str.split(':')[1].strip())

        # Read empty line (separator)
        read_line()

        # Read content
        content_bytes = read_bytes(content_length)
        if not content_bytes:
            return None
        return json.loads(content_bytes.decode('utf-8'))
    except Exception as e:
        logger.error(f"Error reading message: {e}")
        return None


def write_message(response: dict):
    """Write a JSON-RPC message to stdout."""
    content = json.dumps(response)
    content_bytes = content.encode('utf-8')

    sys.stdout.write(f"Content-Length: {len(content_bytes)}\r\n")
    sys.stdout.write("\r\n")
    sys.stdout.write(content)
    sys.stdout.flush()


def main():
    """Main entry point for the RPC server."""
    global _trace_rpc

    parser = argparse.ArgumentParser(description='OpenRewrite Python RPC Server')
    parser.add_argument('--log-file', help='Log file path')
    parser.add_argument('--metrics-csv', help='Metrics CSV output path')
    parser.add_argument('--trace-rpc-messages', action='store_true', help='Enable RPC message tracing')
    args = parser.parse_args()

    if args.log_file:
        file_handler = logging.FileHandler(args.log_file)
        file_handler.setFormatter(logging.Formatter('%(asctime)s - %(levelname)s - %(message)s'))
        logger.addHandler(file_handler)

    if args.trace_rpc_messages:
        logger.setLevel(logging.DEBUG)
        _trace_rpc = True

    logger.info("Python RPC server starting...")

    while True:
        try:
            message = read_message()
            if message is None:
                break

            if args.trace_rpc_messages:
                logger.debug(f"Received: {json.dumps(message)}")

            request_id = message.get('id')
            method = message.get('method')
            params = message.get('params', {})

            if method is None:
                logger.error("Missing 'method' in request")
                continue

            try:
                result = handle_request(method, params)
                response = {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'result': result
                }
            except Exception as e:
                logger.exception(f"Error handling request: {e}")
                # Include full stack trace in error response for debugging
                tb_str = traceback.format_exc()
                response = {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'error': {
                        'code': -32603,
                        'message': str(e),
                        'data': tb_str
                    }
                }

            if args.trace_rpc_messages:
                logger.debug(f"Sending: {json.dumps(response)}")

            write_message(response)

        except Exception as e:
            logger.exception(f"Fatal error: {e}")
            break

    # No ty-types cleanup needed here — clients are scoped per parse batch

    logger.info("Python RPC server shutting down...")


if __name__ == '__main__':
    main()
