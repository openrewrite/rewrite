"""
OpenRewrite Python RPC module.

This module provides JSON-RPC server infrastructure for communicating with Java
OpenRewrite processes, as well as client infrastructure for calling Java recipes.
"""

__path__ = __import__('pkgutil').extend_path(__path__, __name__)

from .server import main
from .send_queue import RpcSendQueue, RpcObjectState
from .receive_queue import RpcReceiveQueue, RpcObjectData, register_receive_codec
from .python_receiver import PythonRpcReceiver
from .java_recipe import (
    prepare_java_recipe,
    visit_with_java_recipe,
    JavaRecipeVisitor,
    PreparedJavaRecipe,
)
from .java_rpc_client import (
    JavaRpcClient,
    get_java_rpc_client,
    set_java_rpc_client,
    install_java_rpc_hooks,
    uninstall_java_rpc_hooks,
)

__all__ = [
    'main',
    'RpcSendQueue',
    'RpcObjectState',
    'RpcReceiveQueue',
    'RpcObjectData',
    'register_receive_codec',
    'PythonRpcReceiver',
    # Java recipe delegation
    'prepare_java_recipe',
    'visit_with_java_recipe',
    'JavaRecipeVisitor',
    'PreparedJavaRecipe',
    # Java RPC client (for testing)
    'JavaRpcClient',
    'get_java_rpc_client',
    'set_java_rpc_client',
    'install_java_rpc_hooks',
    'uninstall_java_rpc_hooks',
]
