# Copyright 2026 the original author or authors.
# <p>
# Licensed under the Moderne Source Available License (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# <p>
# https://docs.moderne.io/licensing/moderne-source-available-license
# <p>
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


"""JSON encoding of RPC frame bodies, through orjson when it is installed and the stdlib otherwise."""
import json
from typing import Any

try:
    import orjson
except ImportError:
    orjson = None

# orjson reads an integer outside 64 bits as a float. Such a number has at least 19 digits, so a
# body holding a run that long is left to the stdlib, which keeps every integer exact. The check
# is a translate and a substring search, a small fraction of the decode they guard.
_DIGITS_TO_ZERO = bytes(0x30 if 0x30 <= b <= 0x39 else 0x20 for b in range(256))
_POSSIBLY_WIDE_INT = b'0' * 19

# Dataclasses and datetimes are refused, as the stdlib refuses them. Unlike the stdlib, orjson encodes
# a UUID and an enum's value, and writes NaN and infinity as null.
_ORJSON_OPTIONS = (orjson.OPT_PASSTHROUGH_DATACLASS | orjson.OPT_PASSTHROUGH_DATETIME) if orjson else 0


def dumps(obj: Any) -> bytes:
    if orjson is not None:
        try:
            return orjson.dumps(obj, option=_ORJSON_OPTIONS)
        except TypeError:
            # An integer past 64 bits, a non-str key and a lone surrogate all have a stdlib form.
            pass
    return json.dumps(obj).encode('utf-8')


def loads(body: bytes) -> Any:
    """Raises ``UnicodeDecodeError`` or ``json.JSONDecodeError`` on a malformed body."""
    if orjson is not None and _POSSIBLY_WIDE_INT not in body.translate(_DIGITS_TO_ZERO):
        try:
            return orjson.loads(body)
        except orjson.JSONDecodeError:
            # orjson rejects an escaped lone surrogate, which the stdlib reads.
            pass
    return json.loads(body.decode('utf-8'))
