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

# Configure logging
logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(levelname)s - %(message)s', stream=sys.stderr)
logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)

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
    """
    # Request the object from Java - ALWAYS send this, don't short-circuit
    result = send_request('GetObject', {
        'id': obj_id,
        'sourceFileType': source_file_type
    })

    if not result:
        return None

    # Deserialize using PythonRpcReceiver
    from rewrite.rpc.receive_queue import RpcReceiveQueue
    from rewrite.rpc.python_receiver import PythonRpcReceiver

    # Create a batch iterator that yields the result data
    batch = list(result)  # result is already a list of RpcObjectData dicts

    def pull_batch() -> List[Dict[str, Any]]:
        nonlocal batch
        data = batch
        batch = []  # Clear for next pull (which shouldn't happen for a single object)
        return data

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

        # Parse using Python AST
        tree = ast.parse(source, path)

        # Convert to OpenRewrite LST
        cu = ParserVisitor(source).visit(tree)
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
        logger.error(f"Error printing object: {e}")
        traceback.print_exc()

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
    }

    handler = handlers.get(method)
    if handler:
        return handler(params)
    else:
        raise ValueError(f"Unknown method: {method}")


def read_message_with_timeout(timeout_seconds: float) -> Optional[dict]:
    """Read a JSON-RPC message from stdin with timeout.

    Uses select() to wait for data with a timeout, preventing indefinite blocking.

    Args:
        timeout_seconds: Maximum time to wait for data

    Returns:
        Parsed JSON message, or None on timeout/error
    """
    try:
        # Wait for stdin to be readable with timeout
        # Note: select works on file descriptors, stdin.fileno() gives us that
        readable, _, _ = select.select([sys.stdin], [], [], timeout_seconds)

        if not readable:
            logger.warning(f"Timeout waiting for RPC response after {timeout_seconds}s")
            return None

        # Read Content-Length header
        header_line = sys.stdin.readline()
        if not header_line:
            return None

        if not header_line.startswith('Content-Length:'):
            logger.error(f"Invalid header: {header_line}")
            return None

        content_length = int(header_line.split(':')[1].strip())

        # Read empty line
        sys.stdin.readline()

        # Read content
        content = sys.stdin.read(content_length)
        return json.loads(content)
    except Exception as e:
        logger.error(f"Error reading message with timeout: {e}")
        return None


def read_message() -> Optional[dict]:
    """Read a JSON-RPC message from stdin (blocking, no timeout)."""
    try:
        # Read Content-Length header
        header_line = sys.stdin.readline()
        if not header_line:
            return None

        if not header_line.startswith('Content-Length:'):
            logger.error(f"Invalid header: {header_line}")
            return None

        content_length = int(header_line.split(':')[1].strip())

        # Read empty line
        sys.stdin.readline()

        # Read content
        content = sys.stdin.read(content_length)
        return json.loads(content)
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
                response = {
                    'jsonrpc': '2.0',
                    'id': request_id,
                    'error': {
                        'code': -32603,
                        'message': str(e)
                    }
                }

            if args.trace_rpc_messages:
                logger.debug(f"Sending: {json.dumps(response)}")

            write_message(response)

        except Exception as e:
            logger.error(f"Fatal error: {e}")
            traceback.print_exc()
            break

    logger.info("Python RPC server shutting down...")


if __name__ == '__main__':
    main()
