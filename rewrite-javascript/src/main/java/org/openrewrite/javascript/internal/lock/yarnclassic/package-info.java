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
 * Yarn classic's lockfile and resolver model, ported from yarn 1.22.22 (yarnpkg/yarn, tag v1.22.22): src/lockfile,
 * src/package-resolver.js, src/package-request.js, src/package-reference.js, src/resolution-map.js,
 * src/workspace-layout.js, src/package-compatibility.js, src/package-fetcher.js, src/resolvers,
 * src/util/normalize-manifest, src/util/normalize-pattern.js, the config that reaches a lock (src/config.js, src/rc.js,
 * src/registries) and src/cli/commands/install.js. It is distributed under
 * the BSD 2-Clause License:
 * <pre>
 * Copyright (c) 2016-present, Yarn Contributors. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </pre>
 * {@code Ssri} ports ssri 5.3.0, {@code Ini} ports ini 1.3.5 and {@code Minimatch} ports minimatch 3.0.4, as yarn
 * 1.22.22 bundles them, all distributed under the ISC License:
 * <ul>
 *     <li>ssri: Copyright (c) npm, Inc.</li>
 *     <li>ini: Copyright (c) Isaac Z. Schlueter and Contributors</li>
 *     <li>minimatch: Copyright (c) Isaac Z. Schlueter and Contributors</li>
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
 * {@code DeepEqual} ports deep-equal 1.0.1, {@code JsYaml} the loader of js-yaml 3.13.1, {@code ObjectPath}
 * object-path 0.11.4 and {@code BraceExpansion} brace-expansion 1.1.11 with balanced-match 1.0.0, as yarn 1.22.22
 * bundles them, and {@code ExoticResolvers.LegacyUrl} ports Node.js {@code lib/url.js}, all distributed under the
 * MIT License:
 * <ul>
 *     <li>deep-equal: James Halliday</li>
 *     <li>brace-expansion, balanced-match: Copyright (c) 2013 Julian Gruber</li>
 *     <li>js-yaml: Copyright (C) 2011-2015 by Vitaly Puzrin</li>
 *     <li>object-path: Copyright (c) 2015 Mario Casciaro</li>
 *     <li>Node.js: Copyright Joyent, Inc. and other Node contributors</li>
 * </ul>
 * <pre>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * </pre>
 */
@NullMarked
@NonNullFields
package org.openrewrite.javascript.internal.lock.yarnclassic;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
