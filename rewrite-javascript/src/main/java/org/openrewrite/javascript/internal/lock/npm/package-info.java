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
 * npm's installed-tree model and resolver, ported from the @npmcli/arborist that npm 11.17.0 ships.
 * <p>
 * This package is a port of code distributed under the ISC License:
 * <ul>
 *     <li>@npmcli/arborist: Copyright npm, Inc.</li>
 *     <li>npm-pick-manifest: Copyright (c) npm, Inc.</li>
 *     <li>npm-package-arg: Copyright (c) npm, Inc.</li>
 *     <li>hosted-git-info: Copyright (c) 2015, Rebecca Turner</li>
 *     <li>validate-npm-package-name: Copyright (c) 2015, npm, Inc</li>
 *     <li>@npmcli/package-json: Copyright GitHub Inc.</li>
 *     <li>@npmcli/name-from-folder: Copyright npm, Inc.</li>
 *     <li>walk-up-path: Copyright (c) 2020-2023 Isaac Z. Schlueter</li>
 * </ul>
 * <pre>
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 * </pre>
 */
@NullMarked
@NonNullFields
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
