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
/**
 * Bun's lockfile model and install, ported from the Zig sources of Bun 1.3.10 (oven-sh/bun, tag bun-v1.3.10):
 * src/install (the lockfile, bun.lock, the hoisted tree, packages, dependencies, resolutions, overrides,
 * catalogs, workspaces, the root's diff), src/semver, src/glob/match.zig, src/wyhash.zig and
 * {@code default-trusted-dependencies.txt}; and from the Zig standard library at the commit Bun 1.3.10 builds
 * with (oven-sh/zig c031cbebf5b063210473ff5204a24ebfb2492c72): {@code std.hash.Wyhash}, the layout of
 * {@code std.HashMapUnmanaged} and the base64 decoder. Both are distributed under the MIT License:
 * <pre>
 * Copyright (c) Bun contributors
 * Copyright (c) Zig contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * </pre>
 * bun's hosted_git_info.zig follows hosted-git-info (Copyright (c) 2015, Rebecca Turner), distributed under the
 * ISC License.
 */
@NullMarked
@NonNullFields
package org.openrewrite.javascript.internal.lock.bun;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
