/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The names Python's {@code builtins} module binds, generated from {@code dir(builtins)} on CPython
 * 3.12.5. The set holds across the 3.10+ the package supports: a name only a newer release binds is
 * no more importable as a module, and no realistic module is named {@code ExceptionGroup} or
 * {@code BaseExceptionGroup} (3.11+), which the set keeps.
 */
public final class PythonBuiltins {

    private static final Set<String> NAMES = new HashSet<>(Arrays.asList(
            "ArithmeticError", "AssertionError", "AttributeError", "BaseException", "BaseExceptionGroup",
            "BlockingIOError", "BrokenPipeError", "BufferError", "BytesWarning", "ChildProcessError",
            "ConnectionAbortedError", "ConnectionError", "ConnectionRefusedError", "ConnectionResetError",
            "DeprecationWarning", "EOFError", "Ellipsis", "EncodingWarning", "EnvironmentError", "Exception",
            "ExceptionGroup", "False", "FileExistsError", "FileNotFoundError", "FloatingPointError",
            "FutureWarning", "GeneratorExit", "IOError", "ImportError", "ImportWarning", "IndentationError",
            "IndexError", "InterruptedError", "IsADirectoryError", "KeyError", "KeyboardInterrupt", "LookupError",
            "MemoryError", "ModuleNotFoundError", "NameError", "None", "NotADirectoryError", "NotImplemented",
            "NotImplementedError", "OSError", "OverflowError", "PendingDeprecationWarning", "PermissionError",
            "ProcessLookupError", "RecursionError", "ReferenceError", "ResourceWarning", "RuntimeError",
            "RuntimeWarning", "StopAsyncIteration", "StopIteration", "SyntaxError", "SyntaxWarning", "SystemError",
            "SystemExit", "TabError", "TimeoutError", "True", "TypeError", "UnboundLocalError",
            "UnicodeDecodeError", "UnicodeEncodeError", "UnicodeError", "UnicodeTranslateError", "UnicodeWarning",
            "UserWarning", "ValueError", "Warning", "ZeroDivisionError", "__build_class__", "__debug__", "__doc__",
            "__import__", "__loader__", "__name__", "__package__", "__spec__", "abs", "aiter", "all", "anext",
            "any", "ascii", "bin", "bool", "breakpoint", "bytearray", "bytes", "callable", "chr", "classmethod",
            "compile", "complex", "copyright", "credits", "delattr", "dict", "dir", "divmod", "enumerate", "eval",
            "exec", "exit", "filter", "float", "format", "frozenset", "getattr", "globals", "hasattr", "hash",
            "help", "hex", "id", "input", "int", "isinstance", "issubclass", "iter", "len", "license", "list",
            "locals", "map", "max", "memoryview", "min", "next", "object", "oct", "open", "ord", "pow", "print",
            "property", "quit", "range", "repr", "reversed", "round", "set", "setattr", "slice", "sorted",
            "staticmethod", "str", "sum", "super", "tuple", "type", "vars", "zip"));

    private PythonBuiltins() {
    }

    public static boolean contains(String name) {
        return NAMES.contains(name);
    }
}
