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
import traceback
import threading
from pathlib import Path
from typing import Dict, Any, Optional, List, Callable
from uuid import uuid4

# Configure logging - log to file by default to avoid filling stderr buffer
# (which can cause deadlock if parent process doesn't read stderr)
_default_log_file = '/tmp/python-rpc.log'
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
        if batch[-1].get('state') == 'END_OF_OBJECT':
            received_end = True

        return batch

    q = RpcReceiveQueue(remote_refs, source_file_type, pull_batch, trace=_trace_rpc)
    receiver = PythonRpcReceiver()

    # Get the "before" state - our understanding of what Java had
    # This is used to apply diffs from the GetObject response
    before = remote_objects.get(obj_id)

    # Receive and deserialize the object (applies diffs to before state)
    obj = receiver.receive(before, q)

    if obj is not None:
        # Update our understanding of what Java has
        remote_objects[obj_id] = obj
        # Also update local_objects for consistency
        local_objects[str(obj.id)] = obj

    return obj


def generate_id() -> str:
    """Generate a unique ID for objects."""
    return str(uuid4())


def parse_python_file(path: str) -> dict:
    """Parse a Python file and return its LST."""
    with open(path, 'r', encoding='utf-8') as f:
        source = f.read()
    return parse_python_source(source, path)


def parse_python_source(source: str, path: str = "<unknown>") -> dict:
    """Parse Python source code and return its LST."""
    try:
        # Import parser visitor
        from rewrite.python._parser_visitor import ParserVisitor
        from rewrite import random_id, Markers

        # Strip BOM before parsing (ParserVisitor handles it internally but ast.parse doesn't)
        source_for_ast = source[1:] if source.startswith('\ufeff') else source

        # Parse using Python AST
        tree = ast.parse(source_for_ast, path)

        # Convert to OpenRewrite LST
        cu = ParserVisitor(source, path).visit(tree)
        cu = cu.replace(source_path=Path(path))
        cu = cu.replace(markers=Markers.EMPTY)

        # Store and return
        obj_id = str(cu.id)
        local_objects[obj_id] = cu
        return {
            'id': obj_id,
            'sourceFileType': 'org.openrewrite.python.tree.Py$CompilationUnit'
        }
    except ImportError as e:
        logger.error(f"Failed to import parser: {e}")
        traceback.print_exc()
        return _create_parse_error(path, str(e))
    except SyntaxError as e:
        logger.error(f"Syntax error parsing {path}: {e}")
        return _create_parse_error(path, str(e))
    except Exception as e:
        logger.error(f"Error parsing {path}: {e}")
        traceback.print_exc()
        return _create_parse_error(path, str(e))


def _create_parse_error(path: str, message: str) -> dict:
    """Create a parse error result."""
    obj_id = generate_id()
    error = {
        'id': obj_id,
        'type': 'org.openrewrite.tree.ParseError',
        'sourcePath': path,
        'message': message,
    }
    local_objects[obj_id] = error
    return {'id': obj_id, 'sourceFileType': 'org.openrewrite.tree.ParseError'}


def handle_parse(params: dict) -> List[str]:
    """Handle a Parse RPC request."""
    inputs = params.get('inputs', [])
    results = []

    for input_item in inputs:
        if 'path' in input_item:
            # File input
            result = parse_python_file(input_item['path'])
        elif 'text' in input_item or 'source' in input_item:
            # String input - Java sends 'text' and 'sourcePath'
            source = input_item.get('text') or input_item.get('source')
            path = input_item.get('sourcePath') or input_item.get('relativePath', '<unknown>')
            result = parse_python_source(source, path)
        else:
            continue
        results.append(result['id'])

    return results


def handle_parse_project(params: dict) -> List[dict]:
    """Handle a ParseProject RPC request."""
    import fnmatch

    project_path = params.get('projectPath', '.')
    exclusions = params.get('exclusions', ['__pycache__', '.venv', 'venv', '.git', '.tox', '*.egg-info'])
    relative_to = params.get('relativeTo')

    results = []

    for root, dirs, files in os.walk(project_path):
        # Filter out excluded directories
        dirs[:] = [d for d in dirs if not any(fnmatch.fnmatch(d, excl) for excl in exclusions)]

        for file in files:
            if file.endswith('.py'):
                path = os.path.join(root, file)
                try:
                    result = parse_python_file(path)
                    results.append(result)
                except Exception as e:
                    logger.error(f"Error parsing {path}: {e}")

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
    logger.info(f"handle_get_object: id={obj_id}, type={type(obj).__name__ if obj else 'None'}")

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
        logger.debug(f"GetObject result: {len(result)} items")
        for i, item in enumerate(result[:10]):  # Log first 10 items
            logger.debug(f"  [{i}] {item}")

        # Update remote_objects to track that Java now has this version
        remote_objects[obj_id] = obj

        return result

    except Exception as e:
        logger.error(f"Error serializing object: {e}")
        import traceback as tb
        tb.print_exc()
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

    logger.info(f"handle_print: treeId={obj_id}, sourceFileType={source_file_type}")

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

    # Reset TyLspClient if it was initialized
    try:
        from rewrite.python.ty_client import TyLspClient
        TyLspClient.reset()
    except ImportError:
        pass  # ty not available

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

    logger.info(f"PrepareRecipe: id={recipe_name}, options={options}")

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

    logger.info(f"PrepareRecipe response: {response}")
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

    logger.info(f"Visit: visitor={visitor_name}, treeId={tree_id}, p={p_id}")

    # Get or create execution context
    if p_id and p_id in _execution_contexts:
        ctx = _execution_contexts[p_id]
    else:
        from rewrite import InMemoryExecutionContext
        ctx = InMemoryExecutionContext()
        if p_id:
            _execution_contexts[p_id] = ctx

    # Get the tree - fetch from Java if we don't have it locally
    tree = local_objects.get(tree_id)
    if tree is None:
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

    logger.info(f"Visit result: modified={modified}, tree_id={tree_id}, before.id={before.id}, after.id={after.id if after else None}")
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

    logger.info(f"Generate: id={recipe_id}, p={p_id}")

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
                logger.error(f"Error handling request: {e}")
                traceback.print_exc()
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
            logger.error(f"Fatal error: {e}")
            traceback.print_exc()
            break

    # Shutdown TyLspClient if it was initialized
    try:
        from rewrite.python.ty_client import TyLspClient
        TyLspClient.reset()
    except ImportError:
        pass  # ty not available

    logger.info("Python RPC server shutting down...")


if __name__ == '__main__':
    main()
