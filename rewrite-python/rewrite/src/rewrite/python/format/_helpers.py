# Copyright 2025 the original author or authors.
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

from __future__ import annotations

from typing import cast, TypeVar, List

from rewrite.java import J, Space
from rewrite.python import PyComment
from rewrite.utils import list_map

J2 = TypeVar('J2', bound=J)


def common_margin(s1, s2):
    if s1 is None:
        s = str(s2)
        return s[s.rfind('\n') + 1:]

    min_length = min(len(s1), len(s2))
    for i in range(min_length):
        if s1[i] != s2[i] or not s1[i].isspace():
            return s1[:i]

    return s2 if len(s2) < len(s1) else s1


def concatenate_prefix(j: J2, prefix: Space) -> J2:
    if prefix.is_empty():
        return j

    shift = common_margin(None, j.prefix.whitespace)

    def modify_comment(c: PyComment) -> PyComment:
        if len(shift) == 0:
            return c
        c = c.replace(text=c.text.replace('\n', '\n' + shift))
        if '\n' in c.suffix:
            c = c.replace(suffix=c.suffix.replace('\n', '\n' + shift))
        return c

    comments = j.prefix.comments + list_map(modify_comment, cast(List[PyComment], prefix.comments))

    new_prefix = j.prefix
    new_prefix = new_prefix.replace(whitespace=new_prefix.whitespace + prefix.whitespace)
    if comments:
        new_prefix = new_prefix.replace(comments=comments)

    return j.replace(prefix=new_prefix)
