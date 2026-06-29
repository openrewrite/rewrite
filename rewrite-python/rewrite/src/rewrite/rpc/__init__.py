"""
OpenRewrite Python RPC module.

This module provides JSON-RPC server infrastructure for communicating with Java
OpenRewrite processes, as well as client infrastructure for calling Java recipes.
"""

__path__ = __import__('pkgutil').extend_path(__path__, __name__)

from .java_rpc_client import (
    JavaRpcClient,
    get_java_rpc_client,
    install_java_rpc_hooks,
    set_java_rpc_client,
    uninstall_java_rpc_hooks,
)
from .python_receiver import PythonRpcReceiver
from .receive_queue import RpcObjectData, RpcReceiveQueue, register_receive_codec
from .rpc_recipe import RpcRecipe
from .send_queue import RpcObjectState, RpcSendQueue
from .server import main

__all__ = [
    'main',
    'RpcSendQueue',
    'RpcObjectState',
    'RpcReceiveQueue',
    'RpcObjectData',
    'register_receive_codec',
    'PythonRpcReceiver',
    # Recipe delegation over RPC
    'RpcRecipe',
    # Java RPC client (for testing)
    'JavaRpcClient',
    'get_java_rpc_client',
    'set_java_rpc_client',
    'install_java_rpc_hooks',
    'uninstall_java_rpc_hooks',
]
