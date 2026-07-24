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
import csv
import json
import logging
import os
import select
import sys
import tempfile
import time
import traceback
import threading

from datetime import datetime, timezone
from pathlib import Path
from typing import Dict, Any, Optional, List, Callable, Set
from uuid import uuid4

try:
    import resource
except ImportError:  # not available on Windows
    resource = None

from rewrite.discovery import RecipeAttribution, RecipeName

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
# Per-source-file remote_refs high-water, captured before a file is first visited so
# handle_evict can roll back exactly the refs that file introduced. Keyed by tree id.
_ref_checkpoints: Dict[str, int] = {}

# Per-call metrics CSV (--metrics-csv), same schema as Go: cache-size ramp vs per-file-Evict sawtooth.
_metrics_file = None
_metrics_writer = None
_metrics_lock = threading.Lock()

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

# Set via --recipe-install-dir; an InstallRecipes RPC for a not-yet-importable
# package pip installs it here before activating.
_recipe_install_dir: Optional[Path] = None

# Set via --child-bundle: the marketplace is scoped to exactly this distribution's recipes.
_child_bundle: Optional[str] = None

# The identity the host keys a local bundle by (its supplied path); None for a registry spec.
_attribution_name: Optional[str] = None

_facade = None


def _next_request_id() -> int:
    """Generate a unique request ID for outgoing requests."""
    global _request_id_counter
    with _request_id_lock:
        _request_id_counter += 1
        return _request_id_counter


def send_request(method: str, params: dict, timeout_seconds: float = 30.0) -> Any:
    """Send a JSON-RPC request to Java and wait for the response.

    This enables bidirectional communication - Python can request
    objects from Java while processing an incoming request.

    Args:
        method: The RPC method name
        params: The request parameters
        timeout_seconds: Maximum time to wait for response (default 30s)

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


def _require_tree(tree: Any, source_file_type: Optional[str]) -> Any:
    """Validate that ``tree`` is a real Tree, not a generic dict fallback.

    When the receiver encounters a ``value_type`` it has no codec for, it
    falls back to returning a ``{'kind': value_type, ...}`` dict (see
    ``RpcReceiveQueue._new_obj`` / ``_do_change``). That fallback is
    appropriate for nested fragments the visitor framework never inspects
    directly, but a top-level SourceFile *must* be a Tree — otherwise the
    visitor crashes with a confusing ``AttributeError: 'dict' object has
    no attribute 'is_acceptable'``. Raise the same "No RPC codec" error
    that the ADD path raises so the failure mode is consistent regardless
    of which RPC message shape Java used.
    """
    from rewrite import Tree
    if isinstance(tree, Tree):
        return tree
    raise RuntimeError(
        f"No RPC codec registered on the Python side for '{source_file_type}'. "
        "The remote side has a codec and sent property messages that will not be consumed, "
        "causing RPC queue desynchronization."
    )


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
        local_objects[obj_id] = obj

    return obj


def generate_id() -> str:
    """Generate a unique ID for objects."""
    return str(uuid4())


def parse_python_file(path: str, relative_to: Optional[str] = None, ty_client=None,
                      language_level: Optional[str] = None,
                      project_language_level: Optional[str] = None) -> dict:
    """Parse a Python file and return its LST."""
    with open(path, 'r', encoding='utf-8') as f:
        source = f.read()
    return parse_python_source(source, path, relative_to, ty_client,
                               language_level=language_level,
                               project_language_level=project_language_level)


def parse_python_source(source: str, path: str = "<unknown>", relative_to: Optional[str] = None, ty_client=None,
                        language_level: Optional[str] = None,
                        project_language_level: Optional[str] = None) -> dict:
    """Parse Python source code and return its LST.

    The parser used depends on the effective language version, resolved in
    this order (first non-empty wins):

    1. ``language_level`` — explicit per-parse override (from the RPC request).
    2. In-source signals: PEP-263-style ``# -*- python: 2 -*-`` magic comment,
       then ``#!/usr/bin/env python2`` shebang.
    3. ``project_language_level`` — project metadata (pyproject.toml /
       setup.cfg classifier or ``requires-python``); resolved once per RPC
       call by the handler.
    4. ``REWRITE_PYTHON_VERSION`` environment variable (process-wide default).

    Values starting with "2" select the parso-based Py2ParserVisitor; anything
    else uses the ast-based Python 3 parser.
    """
    from rewrite.python._version_detect import detect_from_source

    effective_version = (
        language_level
        or detect_from_source(source)
        or project_language_level
        or _python_version
    )

    # Compute the source_path that will be stored on the LST
    source_path = Path(path)
    if relative_to is not None:
        try:
            source_path = source_path.relative_to(relative_to)
        except ValueError:
            pass  # path is not under relative_to, keep absolute

    try:
        from rewrite import Markers

        if effective_version.startswith("2"):
            # Python 2: Try Python 3 ast-based parser first (handles most Python 2 code),
            # fall back to parso-based parser for Python 2-specific syntax
            from rewrite.python._parser_visitor import ParserVisitor
            try:
                source_for_ast = source[1:] if source.startswith('\ufeff') else source
                tree = ast.parse(source_for_ast, path)
                cu = ParserVisitor(source, path, ty_client).visit(tree)
            except SyntaxError:
                from rewrite.python._py2_parser_visitor import Py2ParserVisitor
                cu = Py2ParserVisitor(source, path, effective_version).parse()
        else:
            # Python 3: Use standard ast-based parser
            from rewrite.python._parser_visitor import ParserVisitor

            # Strip BOM before parsing (ParserVisitor handles it internally but ast.parse doesn't)
            source_for_ast = source[1:] if source.startswith('\ufeff') else source

            # Parse using Python AST
            tree = ast.parse(source_for_ast, path)

            # Convert to OpenRewrite LST
            cu = ParserVisitor(source, path, ty_client).visit(tree)

        cu = cu.replace(source_path=source_path, markers=Markers.EMPTY)

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


# Files larger than this are recorded as Quarks rather than parsed into an AST.
# Matches the 1 MB cap in the JVM JavaScriptParser and the other RPC engines.
MAX_PARSEABLE_SIZE_BYTES = 1024 * 1024


def _create_quark(path: str, relative_to: Optional[str]) -> dict:
    """Represent a file that won't be parsed (currently: too large) as a Quark.

    No object is registered in ``local_objects``: the Java side builds the Quark
    from ``sourcePath`` locally, so no content crosses the wire.
    """
    from rewrite import random_id
    source_path = Path(path)
    if relative_to is not None:
        try:
            source_path = source_path.relative_to(relative_to)
        except ValueError:
            pass  # path is not under relative_to, keep absolute
    return {
        'id': str(random_id()),
        'sourceFileType': 'org.openrewrite.quark.Quark',
        'sourcePath': str(source_path),
    }


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
    # Per-parse options forwarded from the client (e.g. {"languageLevel": "2.7"}).
    # Absent for older clients; absent or unknown keys are silently ignored.
    options = params.get('options') or {}
    language_level = options.get('languageLevel')
    # Path to a virtual environment with the project's dependencies installed,
    # provisioned and forwarded by the caller (the CLI build step in production;
    # a test/template helper in-repo). Points ty-types at the deps so supertypes
    # reaching into third-party packages resolve (e.g. a first-party class
    # extending pydantic.BaseModel). The handler never provisions deps itself.
    dependency_path = params.get('dependencyPath')
    results = []

    # If no relativeTo provided, try to infer from absolute input paths
    if not relative_to:
        relative_to = _infer_project_root(inputs)

    # Resolve project-level language version once per request; per-file
    # detection (shebang / magic comment) can still override this inside
    # parse_python_source.
    from rewrite.python._version_detect import detect_from_project
    project_language_level = detect_from_project(relative_to) if relative_to else None

    # Create a ty-types client for this parse batch
    ty_client = None
    tmpdir = None
    try:
        from rewrite.python.ty_client import TyTypesClient
        # Point ty-types at the caller-provisioned dependency environment (if any)
        # so supertypes reaching into third-party packages resolve.
        ty_client = TyTypesClient(virtual_env=dependency_path)
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
                result = parse_python_file(input_item, relative_to, ty_client,
                                           language_level=language_level,
                                           project_language_level=project_language_level)
            elif 'path' in input_item:
                result = parse_python_file(input_item['path'], relative_to, ty_client,
                                           language_level=language_level,
                                           project_language_level=project_language_level)
            elif 'text' in input_item or 'source' in input_item:
                source = input_item.get('text') if 'text' in input_item else input_item.get('source')
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
                    result = parse_python_source(source, disk_path, base_dir, ty_client,
                                                 language_level=language_level,
                                                 project_language_level=project_language_level)
                else:
                    result = parse_python_source(source, path, relative_to, ty_client,
                                                 language_level=language_level,
                                                 project_language_level=project_language_level)
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
    # Per-request explicit override (mirror of the Parse RPC options carrier).
    options = params.get('options') or {}
    language_level = options.get('languageLevel')
    # Caller-provisioned dependency environment for ty-types (see handle_parse).
    dependency_path = params.get('dependencyPath')

    # Resolve project-level language version once for the whole walk; each
    # file may still override it via in-source signals inside parse_python_source.
    from rewrite.python._version_detect import detect_from_project
    project_language_level = detect_from_project(project_path)

    results = []

    ty_client = None
    try:
        from rewrite.python.ty_client import TyTypesClient
        # Point ty-types at the caller-provisioned dependency environment (if any)
        # so supertypes reaching into third-party packages resolve.
        ty_client = TyTypesClient(virtual_env=dependency_path)
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
                        oversize = os.path.getsize(path) > MAX_PARSEABLE_SIZE_BYTES
                    except OSError:
                        oversize = False  # let the normal path surface the read error
                    if oversize:
                        results.append(_create_quark(path, relative_to))
                        continue
                    try:
                        result = parse_python_file(path, relative_to, ty_client,
                                                   language_level=language_level,
                                                   project_language_level=project_language_level)
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

    # Honor the requested marker printer: FENCED emits the {{uuid}} fences the diff
    # reader expects; SANITIZED strips markers; DEFAULT/unknown use default rendering.
    name = params.get('markerPrinter')
    try:
        from rewrite.python.printer import PythonPrinter, PrintOutputCapture
        from rewrite.tree import PrintOutputCapture as CorePrintOutputCapture
        marker_printer = {
            'FENCED': CorePrintOutputCapture.MarkerPrinter.FENCED,
            'SANITIZED': CorePrintOutputCapture.MarkerPrinter.SANITIZED,
            'SEARCH_MARKERS_ONLY': CorePrintOutputCapture.MarkerPrinter.SEARCH_MARKERS_ONLY,
        }.get(name)
        # A FENCED typo would otherwise silently fall back to /*~~>*/ and corrupt the diff.
        if marker_printer is None and name not in (None, 'DEFAULT'):
            logger.warning(f"Unknown markerPrinter '{name}'; using default rendering")
        printer = PythonPrinter()
        return printer.print(obj, PrintOutputCapture(marker_printer))
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
    _delegating_recipes.clear()
    _prepared_editor_overrides.clear()
    _prepared_edit_preconditions.clear()
    _execution_contexts.clear()
    _recipe_accumulators.clear()
    _recipe_phases.clear()
    _ref_checkpoints.clear()

    logger.info("Reset: cleared all cached state")
    return True


def handle_evict(params: dict) -> bool:
    """Handle an Evict RPC notification - drop one source file's tree and roll back the
    refs it introduced, bounding memory to roughly one source file at a time. Recipe,
    accumulator, and execution-context state (keyed separately) is left intact.
    """
    obj_id = params.get('id')
    if obj_id is None:
        return True
    local_objects.pop(obj_id, None)
    remote_objects.pop(obj_id, None)
    checkpoint = _ref_checkpoints.pop(obj_id, None)
    if checkpoint is not None:
        for ref_id in [k for k in remote_refs if k > checkpoint]:
            del remote_refs[ref_id]
    return True


# Global marketplace instance (lazily initialized)
_marketplace = None

# Tracks which distribution's entry point activated which recipes. Used by
# handle_install_recipes to scope its response to the requested distribution
# and by handle_get_marketplace to filter the singleton marketplace down to
# a single package.
_attribution = RecipeAttribution()


def _get_marketplace():
    """Get or create the global marketplace instance.

    Discovery populates ``_attribution`` so each recipe is attributed to the
    distribution whose entry point activated it. Without that attribution, a
    later GetMarketplace or InstallRecipes for package X would incorrectly
    return every recipe in the singleton, including the built-in
    ``org.openrewrite.python.*`` recipes activated by the ``openrewrite``
    distribution itself.
    """
    global _marketplace
    if _marketplace is None:
        from rewrite.marketplace import RecipeMarketplace
        _marketplace = RecipeMarketplace()

        if _child_bundle:
            from rewrite.discovery import discover_root_recipes
            discover_root_recipes(_child_bundle, marketplace=_marketplace, attribution=_attribution,
                                  attribution_name=_attribution_name)
        else:
            from rewrite.discovery import discover_recipes, recipe_name_set
            from rewrite import activate

            discover_recipes(marketplace=_marketplace, attribution=_attribution)

            before = recipe_name_set(_marketplace)
            activate(_marketplace)
            _attribution.record("openrewrite", recipe_name_set(_marketplace) - before)

    return _marketplace


def _is_package_installed(package_name: str, version: Optional[str]) -> bool:
    try:
        import importlib.metadata
        installed = importlib.metadata.version(package_name)
    except Exception:
        return False
    return version is None or installed == version


def _pip_install_recipe_package(package_name: str, version: Optional[str], target_dir: Path) -> None:
    import importlib
    import subprocess

    target_dir.mkdir(parents=True, exist_ok=True)
    # Accept a full PEP 440 specifier (">=1.0", "~=1.4", "==1.0", …). A bare
    # version (no comparator) defaults to an exact "==" match.
    if version:
        spec = f"{package_name}{version}" if version[0] in "=<>!~" else f"{package_name}=={version}"
    else:
        spec = package_name
    # --upgrade is required: `pip install --target` refuses to replace an
    # already-populated package directory without it, otherwise leaving stale
    # files from a previously-installed version. The caller only reaches here
    # for a version not already present (see handle_install_recipes), so this
    # is the version-change path that must overwrite cleanly.
    cmd = [sys.executable, "-m", "pip", "install", "--upgrade", "--target", str(target_dir), spec]
    logger.info(f"pip install: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(
            f"pip install failed for {spec} (target={target_dir}):\n{result.stderr}"
        )

    target_str = str(target_dir.resolve())
    if target_str not in sys.path:
        sys.path.insert(0, target_str)
    importlib.invalidate_caches()


def _pip_install_local_path(local_path: Path, target_dir: Path) -> None:
    import importlib
    import subprocess

    target_dir.mkdir(parents=True, exist_ok=True)
    # Local recipe sources are mutable, so --force-reinstall picks up changed
    # content even when the version is unchanged, and --upgrade lets pip replace
    # the existing files under --target (which it otherwise refuses to do).
    # `pip install <path>` also resolves and installs the local package's
    # dependencies — a Python source dir, unlike a packaged npm/NuGet artifact,
    # does not carry its dependencies alongside it.
    cmd = [
        sys.executable, "-m", "pip", "install",
        "--force-reinstall", "--upgrade",
        "--target", str(target_dir), str(local_path),
    ]
    logger.info(f"pip install: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        raise RuntimeError(
            f"pip install failed for {local_path} (target={target_dir}):\n{result.stderr}"
        )

    target_str = str(target_dir.resolve())
    if target_str not in sys.path:
        sys.path.insert(0, target_str)
    importlib.invalidate_caches()


def handle_install_recipes(params: dict) -> dict:
    """Handle an InstallRecipes RPC request.

    Activates a recipe package in the marketplace. When `--recipe-install-dir`
    is configured, the package is pip-installed into that directory before
    activation — a named spec that isn't already installed, or a local path
    together with its dependencies; otherwise the package must have been
    installed by the caller.

    Args:
        params: Dict containing either:
            - 'recipes': str - A local file path (installed into the recipe-install
              dir, with its dependencies, when one is configured)
            - 'recipes': {'packageName': str, 'version': str|None} - A package spec

    Returns:
        Dict with:
            - 'recipesInstalled': count of recipes added to the marketplace by
              this call (zero on idempotent reinstalls).
            - 'version': resolved version (if known).
            - 'recipes': cumulative list of {descriptor, categoryPaths} rows
              for recipes attributed to this distribution. Stable across
              reinstalls; this is what the caller binds to its bundle.
    """
    import importlib
    import importlib.util
    from rewrite.discovery import recipe_name_set

    marketplace = _get_marketplace()

    recipes = params.get('recipes')
    installed_version = None
    package_name: Optional[str] = None
    recipes_added = 0

    if isinstance(recipes, str):
        # Local file path. When a recipe-install dir is configured, install the
        # local package (and its dependencies) into it before activating — a
        # Python source dir doesn't carry its deps, so a direct import would fail
        # for any recipe with third-party dependencies. Without an install dir,
        # the package must have been provisioned by the caller.
        local_path = Path(recipes)
        if _recipe_install_dir is not None:
            logger.info(f"Installing recipes from local path: {recipes}")
            _pip_install_local_path(local_path, _recipe_install_dir)
        else:
            logger.info(f"Activating recipes from local path: {recipes}")

        # Attribution is the supplied path, not the distribution name — the identity the host
        # keys a local bundle by.
        package_name = _find_package_name(local_path)
        if package_name:
            before = recipe_name_set(marketplace)
            _import_and_activate_package(package_name, marketplace, local_path)
            added = recipe_name_set(marketplace) - before
            _attribution.record(recipes, added)
            recipes_added = len(added)

    elif isinstance(recipes, dict):
        package_name = recipes.get('packageName')
        version = recipes.get('version')

        if not package_name:
            raise ValueError("Package name is required")

        if _recipe_install_dir is not None and not _is_package_installed(package_name, version):
            _pip_install_recipe_package(package_name, version, _recipe_install_dir)

        logger.info(f"Activating recipes package: {package_name}")

        try:
            import importlib.metadata
            installed_version = importlib.metadata.version(package_name)
        except Exception:
            pass

        before = recipe_name_set(marketplace)
        _import_and_activate_package(package_name, marketplace)
        added = recipe_name_set(marketplace) - before
        _attribution.record(package_name, added)
        recipes_added = len(added)
    else:
        raise ValueError(f"Invalid recipes parameter: {recipes}")

    logger.info(f"InstallRecipes: {recipes_added} new for {package_name}")
    return {
        'recipesInstalled': recipes_added,
        'version': installed_version,
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
    from rewrite.discovery import distribution_name_from_source
    return distribution_name_from_source(local_path)


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

    Returns recipes organized by category in a format compatible with Java.

    Args:
        params: Optional dict with a 'packageName' key. When supplied, the
            response is filtered to recipes attributed to that distribution,
            so callers requesting a specific bundle don't get every recipe
            in the singleton marketplace.

    Returns:
        List of dicts with 'descriptor' and 'categoryPaths' for each recipe.
    """
    marketplace = _get_marketplace()

    recipe_filter: Optional[Set[RecipeName]] = None
    if isinstance(params, dict):
        package_name = params.get('packageName')
        if isinstance(package_name, str) and package_name:
            recipe_filter = _attribution.recipes_for(package_name)

    rows = _collect_marketplace_rows(marketplace, recipe_filter=recipe_filter)
    logger.info(f"GetMarketplace: returning {len(rows)} recipes")
    return rows


def _collect_marketplace_rows(
    marketplace,
    recipe_filter: Optional[Set[RecipeName]] = None,
) -> List[dict]:
    """Walk the marketplace and return recipe rows in GetMarketplaceResponse shape.

    A recipe that appears in multiple categories produces one row whose
    ``categoryPaths`` lists each path. When ``recipe_filter`` is provided,
    recipes whose name isn't in the set are skipped.
    """
    rows: List[dict] = []

    def collect(category, category_path: List[dict]) -> None:
        current_path = [*category_path, _category_descriptor_to_dict(category.descriptor)]

        for _recipe_name, (recipe_desc, _recipe_class) in category.recipes.items():
            if recipe_filter is not None and recipe_desc.name not in recipe_filter:
                continue
            existing = next((r for r in rows if r['descriptor']['name'] == recipe_desc.name), None)
            if existing:
                existing['categoryPaths'].append(current_path)
            else:
                rows.append({
                    'descriptor': _recipe_descriptor_to_dict(recipe_desc),
                    'categoryPaths': [current_path],
                    'packageName': _attribution.package_for(recipe_desc.name),
                })

        for subcategory in category.categories:
            collect(subcategory, current_path)

    for category in marketplace.categories():
        collect(category, [])

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


def _delegate_descriptor(name: str) -> dict:
    """Minimal stand-in descriptor for a recipe the host resolves locally via a delegatesTo
    response. The host reads only delegatesTo for delegated recipes and ignores this descriptor,
    but the response shape requires one."""
    return {
        'name': name,
        'displayName': name,
        'description': '',
        'tags': [],
        'estimatedEffortPerOccurrence': None,
        'options': [],
        'preconditions': [],
        'recipeList': [],
        'dataTables': [],
        'maintainers': [],
        'contributors': [],
        'examples': [],
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
        'preconditions': [],
        'recipeList': [_recipe_descriptor_to_dict(r) for r in descriptor.recipe_list],
        'dataTables': descriptor.data_tables,
        'maintainers': [],
        'contributors': [],
        'examples': [],
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
# RpcRecipe references discovered in a composite's recipe_list(), keyed by the
# referenced recipe id. An RpcRecipe has no Python implementation and no no-arg
# constructor, so it cannot live in the marketplace. We register it here when a
# composite is prepared; when the JVM later round-trips PrepareRecipe for the
# referenced id (from its RpcRecipe.getRecipeList()), handle_prepare_recipe
# resolves it here and answers with a delegatesTo response so the JVM runs the
# real recipe natively.
_delegating_recipes: Dict[str, Any] = {}
# Cached bare-editor visitors keyed by prepared recipe id. Populated at
# PrepareRecipe time when recipe.editor() returns a Preconditions.check
# wrapper: we strip the wrapper, register the precondition's wire identity
# in editPreconditions (so the host evaluates it Java-side), and store the
# bare editor here. When BatchVisit / Visit dispatches edit:<id> back, we
# return this bare editor so the precondition does not double-run.
_prepared_editor_overrides: Dict[str, Any] = {}
# Per-recipe-edit-phase precondition wire entries collected at PrepareRecipe
# time from a Preconditions.check(...) wrapper around recipe.editor().
_prepared_edit_preconditions: Dict[str, List[Dict[str, Any]]] = {}
# Execution contexts storage - maps context IDs to ExecutionContext instances
_execution_contexts: Dict[str, Any] = {}
# Accumulator storage for ScanningRecipes - maps recipe IDs to accumulators
_recipe_accumulators: Dict[str, Any] = {}
# Phase tracking for recipes - maps recipe IDs to 'scan' or 'edit'
_recipe_phases: Dict[str, str] = {}
# Host-configured store (via SetDataTableStore); None means per-context in-memory default.
_configured_data_table_store: Optional[Any] = None

# Registry mapping fully-qualified visitor names to visitor classes.
# Used to instantiate visitors by name when dispatched via RPC (e.g., auto-format).
# Lazily initialized to avoid circular imports.
_VISITOR_REGISTRY: Optional[Dict[str, Any]] = None


def _get_visitor_registry() -> Dict[str, Any]:
    """Built-in visitors Java can dispatch by name, each a factory taking the request's
    ``visitorOptions`` dict so an argument-taking visitor (``AddImport``) is built from the wire."""
    global _VISITOR_REGISTRY
    if _VISITOR_REGISTRY is None:
        from rewrite.python.add_import import AddImport, AddImportOptions
        from rewrite.python.remove_import import RemoveImport, RemoveImportOptions
        from rewrite.python.format.auto_format import AutoFormatVisitor

        def _add_import(options: Dict[str, Any]):
            return AddImport(AddImportOptions(
                module=options['module'],
                name=options.get('name'),
                alias=options.get('alias'),
                only_if_referenced=bool(options.get('only_if_referenced', True)),
            ))

        def _remove_import(options: Dict[str, Any]):
            return RemoveImport(RemoveImportOptions(
                module=options['module'],
                name=options.get('name'),
                only_if_unused=bool(options.get('only_if_unused', True)),
            ))

        _VISITOR_REGISTRY = {
            'org.openrewrite.python.AddImport': _add_import,
            'org.openrewrite.python.RemoveImport': _remove_import,
            'org.openrewrite.python.format.AutoFormatVisitor': lambda options: AutoFormatVisitor(),
        }
    return _VISITOR_REGISTRY


def _prepare_instance(recipe, marketplace) -> dict:
    """Prepare a recipe instance and, recursively, its whole child tree — storing every node in
    _prepared_recipes and returning the response with ``recipeList`` populated, so the host builds
    the tree locally instead of a PrepareRecipe round trip per child.

    Mirrors the C# and JS servers. Required options are validated as each node is prepared, which
    covers the whole tree (the root against the caller's options and each child against the values
    its parent set). A child hosted on the peer (an RpcRecipe) carries only ``delegatesTo`` for the
    host to resolve; every other child carries its own ``recipeList``. Delegating recipes forward
    validation to the recipe they delegate to, so they are not validated here.
    """
    from rewrite.recipe import ScanningRecipe
    from rewrite.rpc.rpc_recipe import RpcRecipe

    prepared_id = generate_id()
    _prepared_recipes[prepared_id] = recipe

    descriptor = recipe.descriptor()
    is_delegating = hasattr(recipe, 'java_recipe_name')

    if not is_delegating:
        for name, value, opt in descriptor.options:
            if opt.required and value is None:
                raise ValueError(
                    f"Missing required option `{name}` for recipe `{descriptor.name}`."
                )

    is_scanning = isinstance(recipe, ScanningRecipe)

    # Introspect recipe.editor() once at prepare time. If the recipe wrapped its editor in
    # Preconditions.check(...), we extract the precondition's wire identity (so the Java host can
    # evaluate it locally and skip the visit RPC for non-matching files) and cache the bare editor
    # (so a subsequent dispatch via _instantiate_visitor returns the unwrapped editor — otherwise
    # the precondition would also run Python-side and double the cost).
    edit_preconditions: List[Dict[str, Any]] = list(_get_preconditions(recipe, 'edit'))
    if not is_scanning:
        try:
            editor_visitor = recipe.editor()
        except Exception:
            editor_visitor = None
        if editor_visitor is not None:
            extracted = _extract_preconditions_from_editor(editor_visitor)
            if extracted is not None:
                bare_editor, wire_entries = extracted
                _prepared_editor_overrides[prepared_id] = bare_editor
                edit_preconditions.extend(wire_entries)
    _prepared_edit_preconditions[prepared_id] = edit_preconditions

    response = {
        'id': prepared_id,
        'descriptor': _recipe_descriptor_to_dict(descriptor),
        'editVisitor': f'edit:{prepared_id}',
        'editPreconditions': edit_preconditions,
        'scanVisitor': f'scan:{prepared_id}' if is_scanning else None,
        'scanPreconditions': _get_preconditions(recipe, 'scan') if is_scanning else [],
    }

    if is_delegating:
        response['delegatesTo'] = {
            'recipeName': recipe.java_recipe_name,
            'options': getattr(recipe, 'delegates_to_options', {}),
        }
        return response

    # Whole-tree: prepare each child here so the host builds the tree locally. A child hosted on
    # the peer (an RpcRecipe) is emitted as delegatesTo for the host to resolve; the rest are
    # prepared recursively. Children are also registered (in the marketplace, or _delegating_recipes
    # for an RpcRecipe) so a peer that re-prepares children by name — rather than consuming
    # recipeList — can still resolve them.
    child_responses: List[dict] = []
    for child in recipe.recipe_list():
        if isinstance(child, RpcRecipe):
            _delegating_recipes[child.java_recipe_name] = child
            child_id = generate_id()
            child_responses.append({
                'id': child_id,
                'descriptor': _delegate_descriptor(child.java_recipe_name),
                'editVisitor': f'edit:{child_id}',
                'editPreconditions': [],
                'scanVisitor': None,
                'scanPreconditions': [],
                'delegatesTo': {
                    'recipeName': child.java_recipe_name,
                    'options': getattr(child, 'delegates_to_options', {}),
                },
            })
        else:
            if not marketplace.find_recipe(child.name):
                marketplace.install(type(child), [])
            child_responses.append(_prepare_instance(child, marketplace))
    response['recipeList'] = child_responses

    return response


def handle_prepare_recipe(params: dict) -> dict:
    """Handle a PrepareRecipe RPC request.

    Prepares a recipe for execution by:
    1. Looking up the recipe in the marketplace
    2. Instantiating it with the provided options
    3. Storing it with a unique ID
    4. Returning the descriptor and visitor info

    Args:
        params: dict with 'id' (recipe name) and optional 'options'

    Returns:
        dict with 'id', 'descriptor', 'editVisitor', and precondition info
    """
    recipe_name = params.get('id')
    if recipe_name is None:
        raise ValueError("Recipe 'id' is required")
    options = params.get('options', {})

    logger.debug(f"PrepareRecipe: id={recipe_name}, options={options}")

    # An RpcRecipe referenced from a composite's recipe_list() has no Python
    # implementation: answer with a delegatesTo response so the JVM instantiates
    # and runs the real recipe natively (full ScanningRecipe lifecycle,
    # non-Python source files). See _install_sub_recipes / _delegating_recipes.
    if recipe_name in _delegating_recipes:
        recipe = _delegating_recipes[recipe_name]
        prepared_id = generate_id()
        _prepared_recipes[prepared_id] = recipe
        return {
            'id': prepared_id,
            'descriptor': _recipe_descriptor_to_dict(recipe.descriptor()),
            'editVisitor': f'edit:{prepared_id}',
            'editPreconditions': [],
            'scanVisitor': None,
            'scanPreconditions': [],
            'delegatesTo': {
                'recipeName': recipe.java_recipe_name,
                'options': recipe.delegates_to_options,
            },
        }

    marketplace = _get_marketplace()

    # Look up the recipe - returns (RecipeDescriptor, Type[Recipe]) tuple
    recipe_info = marketplace.find_recipe(recipe_name)
    if recipe_info is None:
        # The host re-prepares every sub-recipe of a composite by id while building
        # RpcRecipe.getRecipeList(). A sub-recipe that delegates to a Java recipe and was not
        # captured in _delegating_recipes is not in this marketplace, so a miss means the host
        # owns this recipe: answer with a delegatesTo response so the JVM resolves the id locally
        # (the Java recipe is on its classpath), rather than failing with "Recipe not found".
        prepared_id = generate_id()
        return {
            'id': prepared_id,
            'descriptor': _delegate_descriptor(recipe_name),
            'editVisitor': f'edit:{prepared_id}',
            'editPreconditions': [],
            'scanVisitor': None,
            'scanPreconditions': [],
            'delegatesTo': {
                'recipeName': recipe_name,
                'options': options,
            },
        }

    _descriptor, recipe_class = recipe_info
    if recipe_class is None:
        raise ValueError(f"Recipe class not found for: {recipe_name}")

    # Instantiate the recipe with options, then prepare it and its whole child tree.
    recipe = recipe_class(**options) if options else recipe_class()

    response = _prepare_instance(recipe, marketplace)
    logger.debug(f"PrepareRecipe response: {response}")
    return response


def _get_preconditions(recipe, phase: str) -> List[dict]:
    """Baseline preconditions for a recipe phase.

    Always includes the language gate (only visit Python source files). Recipe-
    declared preconditions from a ``Preconditions.check(...)`` wrapper around
    ``recipe.editor()`` are added on top by ``handle_prepare_recipe`` — see
    :func:`_extract_preconditions_from_editor`.
    """
    return [{
        'visitorName': 'org.openrewrite.rpc.internal.FindTreesOfType',
        'visitorOptions': {'type': 'org.openrewrite.python.tree.Py'}
    }]


def _extract_preconditions_from_editor(editor_visitor):
    """Walk a ``Preconditions.check(...)`` wrapper chain on an editor result.

    If ``editor_visitor`` is a :class:`rewrite.preconditions.Check`, returns a
    tuple ``(bare_editor, [precondition_wire_entry, ...])`` where ``bare_editor``
    is the innermost non-Check visitor and the wire entries describe each
    precondition in evaluation order. Returns ``None`` if no Check wrapper
    is present.

    Each precondition wire entry has shape ``{'visitorName': str,
    'visitorOptions': dict | None}`` matching ``PrepareRecipeResponse.Precondition``.

    Supported precondition shapes:
      * ``RecipeCheck`` — uses the wrapped recipe's wire identity
        (``edit:<id>``) when known.
      * ``Check`` wrapping a :class:`PreparedJavaRecipe` — uses its
        ``edit_visitor`` directly.
      * ``Check`` wrapping a recipe with ``java_recipe_name`` — emits the
        Java recipe name and options for Java-side instantiation.

    Anything else (a generic in-process ``TreeVisitor`` for unit tests) is
    not propagated to the wire — those would have to round-trip back to
    Python anyway, which defeats the point of the precondition optimization.
    For the in-process / test path the wrapper still works because Python
    keeps using the wrapper (no override cached); see :class:`Check.visit`.
    """
    from rewrite.preconditions import Check, RecipeCheck

    if not isinstance(editor_visitor, Check):
        return None

    wire_entries: List[Dict[str, Any]] = []
    inner: Any = editor_visitor
    while isinstance(inner, Check):
        wire = _check_wire_entry(inner)
        if wire is None:
            # Cannot serialize this check to the wire; abort the optimization
            # for this editor. Returning None leaves the wrapper intact so
            # the precondition runs Python-side as a fallback.
            return None
        wire_entries.append(wire)
        inner = inner.wrapped
    return inner, wire_entries


def _check_wire_entry(check) -> Optional[Dict[str, Any]]:
    """Translate a single :class:`Check` to a precondition wire entry."""
    from rewrite.preconditions import RecipeCheck

    if isinstance(check, RecipeCheck):
        recipe = check.recipe
        prepared_id = _find_prepared_id(recipe)
        if prepared_id is not None:
            return {'visitorName': f'edit:{prepared_id}', 'visitorOptions': None}
        java_name = getattr(recipe, 'java_recipe_name', None)
        if java_name is not None:
            options = getattr(recipe, 'delegates_to_options', {})
            return {'visitorName': java_name, 'visitorOptions': dict(options)}
        return None

    return _condition_wire_entry(check.check)


def _condition_wire_entry(condition) -> Optional[Dict[str, Any]]:
    """Translate a precondition condition (operand) to a wire entry.

    Mirrors ``PrepareRecipeResponse.Precondition``: leaves carry
    ``visitorName`` + ``visitorOptions``; composites carry ``op`` +
    ``operands`` (a list of nested wire entries). Returns ``None`` when
    the condition can't be serialized — the caller leaves the wrapper
    intact so the gate runs Python-side as a fallback.
    """
    from rewrite.preconditions import CompositePrecondition, RecipeRef
    from rewrite.rpc.rpc_recipe import PreparedJavaRecipe

    if isinstance(condition, CompositePrecondition):
        operands: List[Dict[str, Any]] = []
        for operand in condition.operands:
            entry = _condition_wire_entry(operand)
            if entry is None:
                return None
            operands.append(entry)
        return {'op': condition.op, 'operands': operands}

    # Common case: helpers like uses_method/uses_type return a lightweight
    # RecipeRef so the recipe author can declare a precondition without firing
    # an RPC at editor() time. Java's PreparedRecipeCache.instantiateVisitor
    # constructs the named Recipe via Jackson and uses its visitor.
    if isinstance(condition, RecipeRef):
        return {
            'visitorName': condition.recipe_name,
            'visitorOptions': dict(condition.options),
        }
    if isinstance(condition, PreparedJavaRecipe):
        return {'visitorName': condition.edit_visitor, 'visitorOptions': None}
    java_name = getattr(condition, 'java_recipe_name', None)
    if java_name is not None:
        options = getattr(condition, 'delegates_to_options', {})
        return {'visitorName': java_name, 'visitorOptions': dict(options)}
    return None


def _find_prepared_id(recipe) -> Optional[str]:
    for prep_id, prep_recipe in _prepared_recipes.items():
        if prep_recipe is recipe:
            return prep_id
    return None


def handle_set_data_table_store(params: dict) -> bool:
    """Install the host-configured store: ``CSV`` writes raw CSV at ``outputDir``, else in-memory."""
    global _configured_data_table_store

    from rewrite.data_table import CsvDataTableStore, InMemoryDataTableStore

    kind = params.get('kind')
    output_dir = params.get('outputDir')
    if kind == 'CSV' and output_dir:
        prefix_columns = params.get('prefixColumns') or {}
        suffix_columns = params.get('suffixColumns') or {}
        _configured_data_table_store = CsvDataTableStore(
            output_dir, prefix_columns, suffix_columns)
        logger.info(f"SetDataTableStore: CSV store at {output_dir}")
    else:
        _configured_data_table_store = InMemoryDataTableStore()
        logger.info("SetDataTableStore: in-memory (NOOP) store")

    return True


def _install_data_table_store(ctx) -> None:
    """Install the host-configured store on a recipe-run context (no-op when unconfigured)."""
    if _configured_data_table_store is not None:
        from rewrite.data_table import DATA_TABLE_STORE
        ctx.put_message(DATA_TABLE_STORE, _configured_data_table_store)


def _context_for(p_id: Optional[str]):
    """The execution context the host identified by ``p``, created and remembered on first use."""
    if p_id and p_id in _execution_contexts:
        ctx = _execution_contexts[p_id]
    else:
        from rewrite import InMemoryExecutionContext
        ctx = InMemoryExecutionContext()
        if p_id:
            _execution_contexts[p_id] = ctx
            local_objects[p_id] = ctx
    _install_data_table_store(ctx)
    return ctx


def _build_cursor(cursor_ids: Optional[List[str]], source_file_type: Optional[str]):
    """Rebuild the host's cursor chain, consuming the innermost-to-outermost IDs in reverse to build
    it from the root inward (matching the JS implementation)."""
    from rewrite.visitor import Cursor
    cursor = Cursor(None, Cursor.ROOT_VALUE)
    for cursor_id in reversed(cursor_ids or []):
        cursor_obj = get_object_from_java(cursor_id, source_file_type)
        if cursor_obj is not None:
            cursor = Cursor(cursor, cursor_obj)
    return cursor


def handle_visit(params: dict) -> dict:
    """Handle a Visit RPC request.

    Applies a visitor to a tree and returns whether it was modified.

    Args:
        params: dict with 'visitor', 'sourceFileType', 'treeId', 'p' (context id), 'cursor'

    Returns:
        dict with 'modified' boolean
    """
    visitor_name = params.get('visitor')
    visitor_options = params.get('visitorOptions')
    source_file_type = params.get('sourceFileType')
    tree_id = params.get('treeId')
    p_id = params.get('p')
    cursor_ids = params.get('cursor')

    if visitor_name is None:
        raise ValueError("'visitor' is required")
    if tree_id is None:
        raise ValueError("'treeId' is required")

    logger.debug(f"Visit: visitor={visitor_name}, treeId={tree_id}, p={p_id}")

    ctx = _context_for(p_id)

    # Snapshot the remote_refs high-water for this file before fetching its tree (first visit wins).
    _ref_checkpoints.setdefault(tree_id, max(remote_refs.keys(), default=-1))

    # Always fetch the tree from Java to ensure we have the latest version.
    # Java may have modified the tree (e.g., via a Java-side recipe) since our last sync.
    tree = get_object_from_java(tree_id, source_file_type)

    if tree is None:
        raise ValueError(f"Tree not found: {tree_id}")

    tree = _require_tree(tree, source_file_type)

    # Instantiate the visitor
    visitor = _instantiate_visitor(visitor_name, ctx, visitor_options)

    cursor = _build_cursor(cursor_ids, source_file_type)

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


def handle_batch_visit(params: dict) -> dict:
    """Handle a BatchVisit RPC request.

    Runs multiple visitors in sequence on the same tree, collecting
    per-visitor metadata (modified, deleted, new search result IDs).
    """
    tree_id = params.get('treeId')
    source_file_type = params.get('sourceFileType')
    p_id = params.get('p')
    visitors = params.get('visitors', [])

    if not tree_id:
        raise ValueError("'treeId' is required")

    logger.debug(f"BatchVisit: treeId={tree_id}, visitors={len(visitors)}")

    ctx = _context_for(p_id)

    # Snapshot the remote_refs high-water for this file before fetching its tree.
    _ref_checkpoints.setdefault(tree_id, max(remote_refs.keys(), default=-1))

    # Fetch tree once from Java
    tree = get_object_from_java(tree_id, source_file_type)
    if tree is None:
        raise ValueError(f"Tree not found: {tree_id}")

    tree = _require_tree(tree, source_file_type)

    from rewrite.visitor import Cursor
    from rewrite.markers import SearchResult
    cursor = Cursor(None, Cursor.ROOT_VALUE)

    results = []
    known_ids = _collect_search_result_ids(tree)

    for item in visitors:
        visitor_name = item.get('visitor', '')

        # Instantiate and run visitor
        visitor = _instantiate_visitor(visitor_name, ctx, item.get('visitorOptions'))
        before = tree
        after = visitor.visit(tree, ctx, cursor)

        modified = after is not before
        deleted = after is None

        # Diff SearchResult IDs against the running set. When the visitor
        # didn't modify the tree, no new SearchResult markers were added
        # — skip the full-tree walk in that case.
        if deleted or not modified:
            search_result_ids = []
        else:
            after_ids = _collect_search_result_ids(after)
            search_result_ids = list(after_ids - known_ids)
            known_ids.update(search_result_ids)

        results.append({
            'modified': modified,
            'deleted': deleted,
            'hasNewMessages': False,
            'searchResultIds': search_result_ids,
        })

        if deleted:
            if tree_id in local_objects:
                del local_objects[tree_id]
            break

        if modified:
            tree = after

    # Store final tree in localObjects for subsequent GetObject
    if tree is not None:
        local_objects[str(tree.id)] = tree
        if str(tree.id) != tree_id:
            local_objects[tree_id] = tree

    return {'results': results}


def _collect_search_result_ids(tree) -> set:
    """Collect all SearchResult marker UUIDs from a tree."""
    from rewrite.markers import SearchResult
    ids = set()
    if tree is None:
        return ids

    from rewrite.python.visitor import PythonVisitor
    class _Collector(PythonVisitor):
        def visit_marker(self, marker, p):
            if isinstance(marker, SearchResult):
                p.add(str(marker.id))
            return marker
    _Collector().visit(tree, ids)
    return ids


def _instantiate_visitor(visitor_name: str, ctx, visitor_options: Optional[Dict[str, Any]] = None):
    """Instantiate a visitor from its name.

    Visitor names can be:
    - 'edit:<recipe_id>' - get the editor from a prepared recipe
    - 'scan:<recipe_id>' - get the scanner from a prepared scanning recipe
    - a fully-qualified built-in visitor name, constructed from ``visitor_options``

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

        # If the recipe wrapped its editor in Preconditions.check(...) and we
        # captured the bare editor at PrepareRecipe time, return it here so
        # the precondition does not double-run on the Python side. The host
        # has already evaluated it via the editPreconditions wire slot.
        override = _prepared_editor_overrides.get(recipe_id)
        if override is not None:
            return override
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
        # Look up visitor by fully-qualified name from registry
        factory = _get_visitor_registry().get(visitor_name)
        if factory is None:
            raise ValueError(f"Unknown visitor name format: {visitor_name}")
        return factory(visitor_options or {})


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
        if p_id:
            _execution_contexts[p_id] = ctx
            local_objects[p_id] = ctx

    _install_data_table_store(ctx)

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


# The facade owns the working source file and keeps one RPC ref table per connection: its own
# facade<->Java table (remote_objects/remote_refs) plus one per child. Each bundle runs as a
# "sub-BatchVisit": the child is served the current tree over that child's table, runs its visitors,
# and its edit is pulled back as a diff and applied before the next bundle starts.
#
# Deserializing and re-generating per child keeps every hop a diff between a matched send/receive
# pair, so one child's ref numbering never has to mean anything to another child. Relaying a
# child's stream directly to a sibling is unsafe.
_hub_tree: Dict[str, Any] = {}              # obj_id -> the facade's authoritative tree
_hub_send_refs: Dict[str, Dict] = {}        # bundle -> send ref map      (facade -> child)
_hub_send_next: Dict[str, int] = {}         # bundle -> next send ref number
_hub_served: Dict[tuple, Any] = {}          # (bundle, obj_id) -> what that child was last served
_hub_send_checkpoint: Dict[tuple, int] = {}  # (bundle, obj_id) -> send ref counter before this file

def _hub_acquire(obj_id: str, source_file_type: Optional[str]):
    """The facade's copy of the in-flight tree, fetched from Java (over the facade<->Java table) the
    first time it is needed and owned by the facade from then on."""
    if obj_id is None:
        return None
    tree = _hub_tree.get(obj_id)
    if tree is None:
        tree = get_object_from_java(obj_id, source_file_type)
        if tree is not None:
            _hub_tree[obj_id] = tree
            local_objects[obj_id] = tree
    return tree


def _hub_serve_child(bundle: str, obj_id: str, source_file_type: Optional[str]) -> Any:
    """Serve a child its view of the facade's tree over that child's ref table: a diff against
    whatever that child was last served, or a full object the first time."""
    from rewrite.rpc.send_queue import RpcSendQueue

    # Serve only from what the facade already holds. Fetching from Java here would deadlock: we are
    # inside a child's callback, the child is blocked on us, and Java is blocked on the request that
    # got us here. handle_request acquires the tree at the top level before dispatching instead.
    tree = _hub_tree.get(obj_id)
    if tree is None:
        return [{'state': 'DELETE'}, {'state': 'END_OF_OBJECT'}]

    q = RpcSendQueue(source_file_type)
    q.refs = _hub_send_refs.setdefault(bundle, {})
    q.next_ref = _hub_send_next.get(bundle, 0)
    # Remember where this child's ref numbering stood before this file, so Evict can roll it back
    # in lockstep with the child's own rollback (see _hub_release).
    _hub_send_checkpoint.setdefault((bundle, obj_id), q.next_ref)
    data = q.generate(tree, _hub_served.get((bundle, obj_id)))
    _hub_send_next[bundle] = q.next_ref
    _hub_served[(bundle, obj_id)] = tree
    return data


def _hub_pull_child_edit(children, bundle: str, obj_id: str, source_file_type: Optional[str]) -> None:
    """Pull a child's edit back as a diff against what it was served and apply it to the facade's
    tree, so the next bundle starts from this bundle's result."""
    from rewrite.rpc.receive_queue import RpcReceiveQueue

    served = _hub_served.get((bundle, obj_id))
    remaining = [children.request(bundle, 'GetObject',
                                  {'id': obj_id, 'sourceFileType': source_file_type})]

    def pull():
        if not remaining:
            return []
        return [d for d in remaining.pop(0) if d.get('state') != 'END_OF_OBJECT']

    edited = RpcReceiveQueue({}, source_file_type, pull).receive(served, None)
    if edited is not None:
        _hub_tree[obj_id] = edited
        _hub_served[(bundle, obj_id)] = edited
        local_objects[obj_id] = edited
        if str(getattr(edited, 'id', obj_id)) != obj_id:
            local_objects[str(edited.id)] = edited


def _hub_release(obj_id: str) -> None:
    """The rollback must be symmetric with the child's own Evict: the child drops the refs this file
    introduced from its receive map, so if the facade kept them in its send map it would emit a
    GET_REF for a ref the child no longer has ("Received reference to unknown object").
    """
    _hub_tree.pop(obj_id, None)
    for key in [k for k in _hub_served if k[1] == obj_id]:
        del _hub_served[key]
    for key in [k for k in _hub_send_checkpoint if k[1] == obj_id]:
        bundle = key[0]
        checkpoint = _hub_send_checkpoint.pop(key)
        refs = _hub_send_refs.get(bundle)
        if refs is not None:
            for ref_key in [k for k, (_, num) in refs.items() if num > checkpoint]:
                del refs[ref_key]
        _hub_send_next[bundle] = checkpoint


def _hub_is_builtin_visitor(visitor_name: Optional[str]) -> bool:
    """True for a visitor the server implements itself rather than one a recipe bundle owns."""
    return bool(visitor_name) and visitor_name in _get_visitor_registry()


def _hub_local_visit(visitor_items: List[dict], params: dict) -> List[dict]:
    """Run the built-in service visitors (no recipe bundle owns them) against the facade's own tree
    in order, threading each result into the next as a child would sequence a BatchVisit."""
    tree_id = params.get('treeId')
    source_file_type = params.get('sourceFileType')
    tree = _hub_acquire(tree_id, source_file_type)
    if tree is None:
        raise ValueError(f"Tree not found: {tree_id}")
    tree = _require_tree(tree, source_file_type)

    ctx = _context_for(params.get('p'))
    cursor = _build_cursor(params.get('cursor'), source_file_type)

    results = []
    for item in visitor_items:
        before = tree
        visitor = _instantiate_visitor(item['visitor'], ctx, item.get('visitorOptions'))
        after = visitor.visit(before, ctx, cursor)
        results.append({
            'modified': after is not before,
            'deleted': after is None,
            'hasNewMessages': False,
            'searchResultIds': [],
        })
        if after is None:
            break
        tree = after

    if tree is not None and tree is not _hub_tree.get(tree_id):
        _hub_tree[tree_id] = tree
        local_objects[tree_id] = tree
        if str(tree.id) != tree_id:
            local_objects[str(tree.id)] = tree
    return results


def _serve_child_object(method: str, params: dict, bundle: Optional[str] = None) -> Any:
    """A child's upstream callback: GetObject is answered from the facade's tree, the rest relays
    to Java."""
    if method != 'GetObject':
        return send_request(method, params)

    obj_id = params.get('id')
    if obj_id is None:
        return [{'state': 'DELETE'}, {'state': 'END_OF_OBJECT'}]
    if bundle is None:
        return send_request('GetObject', params)
    return _hub_serve_child(bundle, obj_id, params.get('sourceFileType'))


def _facade_mode() -> bool:
    return _recipe_install_dir is not None and _child_bundle is None


def _get_facade():
    global _facade
    if _facade is None:
        from rewrite.rpc import venv_manager
        from rewrite.rpc.bundle_children import BundleChildren
        from rewrite.rpc.facade import Facade
        removed = venv_manager.purge_non_venv_entries(_recipe_install_dir)
        if removed:
            logger.info("Cleared %d pre-venv recipe artifact(s) from %s: %s",
                        len(removed), _recipe_install_dir, ", ".join(removed))
        _facade = Facade(BundleChildren(sys.executable, _recipe_install_dir, upstream=_serve_child_object),
                         hub_pull=_hub_pull_child_edit,
                         local_visit=_hub_local_visit,
                         is_local_visitor=_hub_is_builtin_visitor)
    return _facade


def handle_request(method: str, params: dict) -> Any:
    """Handle an RPC request."""
    if _facade_mode():
        facade = _get_facade()

        if method == 'Evict':
            facade.evict(params)
            _hub_release(params.get('id'))
            return handle_evict(params)
        facade_handlers = {
            'InstallRecipes': facade.install_recipes,
            'GetMarketplace': facade.get_marketplace,
            'PrepareRecipe': facade.prepare_recipe,
            'SetDataTableStore': facade.set_data_table_store,
            'Visit': facade.visit,
            'BatchVisit': facade.batch_visit,
            'Generate': facade.generate,
        }
        facade_handler = facade_handlers.get(method)
        if facade_handler:
            # Acquire at the top level, before any child runs — acquiring inside a child's
            # GetObject callback would deadlock (the child waits on us, Java on this request).
            if method in ('Visit', 'BatchVisit'):
                _hub_acquire(params.get('treeId'), params.get('sourceFileType'))
            return facade_handler(params)
        if method in ('Print', 'GetObject'):
            obj_id = params.get('treeId') or params.get('id')
            source_file_type = params.get('sourceFileType')
            # Java fetches non-tree objects by id as well (the execution context, cursors).
            # `GetObject.sourceFileType` is nullable and only set for trees, so it is what tells
            # the two apart. Acquiring a non-tree would hand its property messages to a receiver
            # that has no codec for them, desynchronizing the queue for every later object.
            if obj_id is not None and source_file_type and obj_id not in _hub_tree:
                _hub_acquire(obj_id, source_file_type)

    handlers = {
        'Parse': handle_parse,
        'ParseProject': handle_parse_project,
        'GetObject': handle_get_object,
        'GetLanguages': handle_get_languages,
        'Print': handle_print,
        'Reset': handle_reset,
        'Evict': handle_evict,
        'InstallRecipes': handle_install_recipes,
        'GetMarketplace': handle_get_marketplace,
        'PrepareRecipe': handle_prepare_recipe,
        'SetDataTableStore': handle_set_data_table_store,
        'Visit': handle_visit,
        'BatchVisit': handle_batch_visit,
        'Generate': handle_generate,
    }

    handler = handlers.get(method)
    if handler:
        return handler(params)
    else:
        raise ValueError(f"Unknown method: {method}")


class _StdinBuffer:
    """Buffered reader for raw stdin file descriptor.

    Reads in chunks to reduce syscall overhead.  A single module-level
    instance is shared by read_message() and read_message_with_timeout().
    """

    _CHUNK_SIZE = 8192

    def __init__(self):
        self._fd: Optional[int] = None
        self._buf = bytearray()

    def _get_fd(self) -> int:
        fd = self._fd
        if fd is None:
            fd = sys.stdin.fileno()
            self._fd = fd
        return fd

    def read_line(self, deadline: Optional[float] = None) -> Optional[bytes]:
        """Read until ``\\n``.  Returns the line including the newline,
        or ``None`` on EOF/timeout."""
        while True:
            idx = self._buf.find(b'\n')
            if idx >= 0:
                line = bytes(self._buf[:idx + 1])
                del self._buf[:idx + 1]
                return line
            if not self._fill(deadline):
                return None

    def read_bytes(self, n: int, deadline: Optional[float] = None) -> Optional[bytes]:
        """Read exactly *n* bytes.  Returns ``None`` on EOF/timeout."""
        while len(self._buf) < n:
            if not self._fill(deadline):
                return None
        result = bytes(self._buf[:n])
        del self._buf[:n]
        return result

    def _fill(self, deadline: Optional[float] = None) -> bool:
        """Read more data into the internal buffer.

        When *deadline* is set, uses ``select()`` to respect the timeout
        on Unix, or a background-thread read on Windows (where ``select()``
        does not work on pipes).  Returns ``False`` on EOF or timeout.
        """
        if deadline is not None:
            remaining = deadline - time.time()
            if remaining <= 0:
                return False
            if os.name == 'nt':
                # Windows: select() doesn't support pipes, use a thread
                result: list = []

                def _read():
                    try:
                        data = os.read(self._get_fd(), self._CHUNK_SIZE)
                        result.append(data)
                    except OSError:
                        result.append(b'')

                t = threading.Thread(target=_read, daemon=True)
                t.start()
                t.join(timeout=remaining)
                if not result:
                    return False
                chunk = result[0]
            else:
                readable, _, _ = select.select([self._get_fd()], [], [], remaining)
                if not readable:
                    return False
                chunk = os.read(self._get_fd(), self._CHUNK_SIZE)
        else:
            chunk = os.read(self._get_fd(), self._CHUNK_SIZE)
        if not chunk:
            return False
        self._buf += chunk
        return True


_stdin_buffer = _StdinBuffer()


def read_message_with_timeout(timeout_seconds: float) -> Optional[dict]:
    """Read a JSON-RPC message from stdin with timeout.

    Args:
        timeout_seconds: Maximum time to wait for complete message

    Returns:
        Parsed JSON message, or None on timeout/error
    """
    deadline = time.time() + timeout_seconds

    try:
        # Read Content-Length header
        header_line = _stdin_buffer.read_line(deadline)
        if not header_line:
            logger.warning(f"Timeout waiting for RPC response header after {timeout_seconds}s")
            return None

        header_str = header_line.decode('utf-8').strip()
        if not header_str.startswith('Content-Length:'):
            logger.error(f"Invalid header: {header_str}")
            return None

        content_length = int(header_str.split(':')[1].strip())

        # Read empty line (separator)
        separator = _stdin_buffer.read_line(deadline)
        if separator is None:
            logger.warning(f"Timeout waiting for header separator")
            return None

        # Read content
        content_bytes = _stdin_buffer.read_bytes(content_length, deadline)
        if content_bytes is None:
            logger.warning(f"Timeout waiting for message content")
            return None

        return json.loads(content_bytes.decode('utf-8'))
    except Exception as e:
        logger.error(f"Error reading message with timeout: {e}")
        return None


def read_message() -> Optional[dict]:
    """Read a JSON-RPC message from stdin (blocking, no timeout)."""
    try:
        # Read Content-Length header
        header_line = _stdin_buffer.read_line()
        if not header_line:
            return None

        header_str = header_line.decode('utf-8').strip()
        if not header_str.startswith('Content-Length:'):
            logger.error(f"Invalid header: {header_str}")
            return None

        content_length = int(header_str.split(':')[1].strip())

        # Read empty line (separator)
        _stdin_buffer.read_line()

        # Read content
        content_bytes = _stdin_buffer.read_bytes(content_length)
        if not content_bytes:
            return None
        return json.loads(content_bytes.decode('utf-8'))
    except Exception as e:
        logger.error(f"Error reading message: {e}")
        return None


def write_message(response: dict):
    """Write a JSON-RPC message to stdout.

    Uses unbuffered binary I/O to avoid line-ending translation on Windows
    that would corrupt the JSON-RPC protocol headers. Mirrors the pattern
    used by read_message() which uses os.read() on the read side.
    """
    content_bytes = json.dumps(response).encode('utf-8')
    header = f"Content-Length: {len(content_bytes)}\r\n\r\n".encode('utf-8')
    os.write(sys.stdout.fileno(), header + content_bytes)


def _init_pyroscope() -> None:
    """Start continuous profiling when PYROSCOPE_SERVER_ADDRESS is set.

    Tags inherited via PYROSCOPE_TAGS (k=v,k=v) are forwarded verbatim; a
    `runtime=python` tag is added so flame graphs in the shared `modcli`
    application can be sliced by which RPC subprocess produced them.
    """
    server = os.environ.get("PYROSCOPE_SERVER_ADDRESS")
    if not server:
        return
    try:
        import pyroscope  # type: ignore[import-not-found]
    except ImportError:
        logger.warning("PYROSCOPE_SERVER_ADDRESS set but pyroscope-io not installed; profiling disabled")
        return
    tags: Dict[str, str] = {"runtime": "python"}
    for pair in os.environ.get("PYROSCOPE_TAGS", "").split(","):
        if "=" in pair:
            k, v = pair.split("=", 1)
            tags[k.strip()] = v.strip()
    pyroscope.configure(
        application_name=os.environ.get("PYROSCOPE_APPLICATION_NAME", "modcli"),
        server_address=server,
        tags=tags,
    )


_METRICS_HEADER = ['timestamp', 'method', 'duration_ms', 'error', 'memory_used_bytes',
                   'memory_max_bytes', 'local_objects', 'remote_objects', 'refs']


def _init_metrics_csv(path: str) -> None:
    """Open the per-call metrics CSV and write its header. Best-effort: a failure here
    disables metrics but never blocks the server."""
    global _metrics_file, _metrics_writer
    try:
        _metrics_file = open(path, 'w', newline='')
        _metrics_writer = csv.writer(_metrics_file)
        _metrics_writer.writerow(_METRICS_HEADER)
        _metrics_file.flush()
    except OSError as e:
        logger.warning(f"metrics-csv: cannot open {path!r}: {e} - metrics disabled")
        _metrics_file = _metrics_writer = None


def _rss_bytes():
    """Best-effort (current, peak) RSS in bytes; blank without /proc or the resource module (Windows).
    The RewriteRpcProcess RSS sampler is the source of truth; these columns just annotate each row."""
    current = ''
    peak = ''
    try:
        with open('/proc/self/statm') as f:
            current = int(f.read().split()[1]) * os.sysconf('SC_PAGE_SIZE')
    except (OSError, ValueError, IndexError):
        pass
    if resource is not None:
        maxrss = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
        # macOS reports ru_maxrss in bytes; Linux and most others in kilobytes.
        peak = maxrss if sys.platform == 'darwin' else maxrss * 1024
        if current == '':
            current = peak
    return current, peak


def _record_metric(method: str, duration_ms: float, error: str) -> None:
    """Append one row of timing + cache residency. refs counts remote_refs only: Python's send-side
    refs live on a per-call RpcSendQueue, the only cross-call ref cache handle_evict rolls back."""
    if _metrics_writer is None:
        return
    used, peak = _rss_bytes()
    with _metrics_lock:
        if _metrics_writer is None:
            return
        try:
            _metrics_writer.writerow([
                datetime.now(timezone.utc).isoformat(), method, f"{duration_ms:.0f}", error,
                used, peak, len(local_objects), len(remote_objects), len(remote_refs)])
            _metrics_file.flush()
        except OSError as e:
            logger.warning(f"metrics-csv: write failed: {e}")


def _close_metrics() -> None:
    global _metrics_file, _metrics_writer
    with _metrics_lock:
        if _metrics_file is not None:
            try:
                _metrics_file.flush()
                _metrics_file.close()
            except OSError:
                pass
        _metrics_file = _metrics_writer = None


def main():
    """Main entry point for the RPC server."""
    global _trace_rpc

    parser = argparse.ArgumentParser(description='OpenRewrite Python RPC Server')
    parser.add_argument('--log-file', help='Log file path')
    parser.add_argument('--metrics-csv', help='Metrics CSV output path')
    parser.add_argument('--trace-rpc-messages', action='store_true', help='Enable RPC message tracing')
    parser.add_argument('--recipe-install-dir', help='Directory where recipe pip packages are installed')
    parser.add_argument('--child-bundle',
                        help='Run as a single-bundle child scoped to this distribution name')
    parser.add_argument('--attribution-name',
                        help='Label this child\'s recipes with this identity (a local install\'s '
                             'supplied path) instead of the distribution name')
    args = parser.parse_args()

    _init_pyroscope()

    if args.metrics_csv:
        _init_metrics_csv(args.metrics_csv)

    if args.recipe_install_dir:
        global _recipe_install_dir
        _recipe_install_dir = Path(args.recipe_install_dir)

    if args.child_bundle:
        global _child_bundle
        _child_bundle = args.child_bundle

    if args.attribution_name:
        global _attribution_name
        _attribution_name = args.attribution_name

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

            metric_start = time.monotonic()
            metric_error = ''
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
                metric_error = str(e)
                response = {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'error': {
                        'code': -32603,
                        'message': str(e),
                        'data': tb_str
                    }
                }
            _record_metric(method, (time.monotonic() - metric_start) * 1000.0, metric_error)

            if args.trace_rpc_messages:
                logger.debug(f"Sending: {json.dumps(response)}")

            # Notifications (no id, e.g. Evict) get no reply — a null-id response would
            # fail every in-flight request on the Java reader.
            if request_id is not None:
                write_message(response)

        except Exception as e:
            logger.exception(f"Fatal error: {e}")
            break

    # No ty-types cleanup needed here — clients are scoped per parse batch

    _close_metrics()
    logger.info("Python RPC server shutting down...")


if __name__ == '__main__':
    main()
