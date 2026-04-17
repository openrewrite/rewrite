# Copyright 2026 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
"""
Wire codec for JavaType.Annotation constant element values.

Plain JSON cannot distinguish e.g. ``Integer 42`` from ``Long 42`` or
``Character 'c'`` from ``String "c"``, so each constant is encoded as a
tagged string on the wire.

Encoding: ``"<kind>:<lexical>"`` where ``<kind>`` is one of
``s`` (str), ``b`` (bool), ``i`` (int), ``l`` (long), ``S`` (short),
``B`` (byte), ``f`` (float), ``d`` (double), ``c`` (char). A null
constant is encoded as the literal string ``"n"`` (or ``None`` on the
wire — both are accepted on receive).

The Java parser only ever produces ``str``, ``Number``, ``bool``,
``Character`` (or ``None``) as constant element values; class literals
and enum constants flow through the ``referenceValue`` branch as
``JavaType`` references. Python has no native distinction between
short/byte/int/long/float/double — receivers simply produce a plain
``int`` / ``float`` / ``str`` / ``bool``, since Python's numeric tower
preserves the value semantically.
"""
from __future__ import annotations

from typing import Any, List, Optional


def encode(value: Optional[Any]) -> Optional[str]:
    if value is None:
        return None
    if isinstance(value, bool):
        return f"b:{'true' if value else 'false'}"
    if isinstance(value, int):
        return f"i:{value}"
    if isinstance(value, float):
        return f"d:{value}"
    if isinstance(value, str):
        if len(value) == 1:
            # Ambiguous — could be char or single-char string. Default to string;
            # callers that know it's a char must encode explicitly via _encode_char.
            return f"s:{value}"
        return f"s:{value}"
    raise ValueError(f"Unsupported annotation constant value type: {type(value).__name__}")


def decode(encoded: Optional[str]) -> Optional[Any]:
    if encoded is None or encoded == 'n':
        return None
    if len(encoded) < 2 or encoded[1] != ':':
        raise ValueError(f"Malformed annotation constant value envelope: {encoded!r}")
    kind = encoded[0]
    body = encoded[2:]
    if kind == 's':
        return body
    if kind == 'b':
        return body == 'true'
    if kind in ('i', 'l', 'S', 'B'):
        return int(body)
    if kind in ('f', 'd'):
        return float(body)
    if kind == 'c':
        if not body:
            raise ValueError("Malformed char envelope: empty body")
        return body[0]
    raise ValueError(f"Unknown annotation constant value kind: {kind!r}")


def encode_list(values: Optional[List[Any]]) -> Optional[List[Optional[str]]]:
    if values is None:
        return None
    return [encode(v) for v in values]


def decode_list(encoded: Optional[List[Optional[str]]]) -> Optional[List[Any]]:
    if encoded is None:
        return None
    return [decode(s) for s in encoded]
