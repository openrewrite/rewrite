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
 * Yarn Berry's lockfile and resolver model, ported from Yarn 4.10.3 (yarnpkg/berry, tag @yarnpkg/cli/4.10.3):
 * packages/yarnpkg-core (Project, structUtils, Manifest, Configuration, configUtils, the lockfile, legacy migration,
 * multi, virtual and workspace resolvers, the multi and workspace fetchers, Workspace, CorePlugin, Cache, tgzUtils,
 * semverUtils, miscUtils, hashUtils), packages/yarnpkg-parsers (syml, resolution), packages/plugin-npm (the semver,
 * tag and remap resolvers, the semver and http fetchers, npmHttpUtils, npmConfigUtils), packages/plugin-patch (the
 * resolver, the fetcher, patchUtils), packages/plugin-link, plugin-git, plugin-http, plugin-file, plugin-exec and
 * plugin-jsr (their resolvers and fetchers), packages/plugin-compat, packages/plugin-catalog,
 * packages/yarnpkg-extensions (as data, in {@code yarn-releases.json.gz}) and packages/plugin-essentials (the install
 * command). It is distributed under the BSD 2-Clause License:
 * <pre>
 * Copyright (c) 2016-present, Yarn Contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </pre>
 * {@code internal.lock.JsYaml} ports the loader of js-yaml 3.14.1 (Copyright (C) 2011-2015 by Vitaly Puzrin);
 * {@code QueryString} Node.js {@code lib/querystring.js} and {@code lib/internal/querystring.js} (Copyright Joyent,
 * Inc. and other Node contributors); {@code FastGlob} fast-glob 3.3.2 (Copyright (c) Denis Malinochkin);
 * {@code internal.lock.Picomatch}, {@code internal.lock.PicomatchParse} and {@code internal.lock.PicomatchScan}
 * picomatch 2.3.1 (Copyright (c) 2017-present, Jon Schlinkert), as micromatch 4.0.5 calls it (Copyright (c)
 * 2014-present, Jon Schlinkert); {@code internal.lock.Braces} braces 3.0.2 (Copyright (c) 2014-2018, Jon
 * Schlinkert); {@code internal.lock.FillRange} fill-range 7.0.1 (Copyright (c) 2014-present, Jon Schlinkert);
 * {@code internal.lock.IsGlob} is-glob 4.0.3 (Copyright (c) 2014-2017, Jon Schlinkert) and
 * {@code internal.lock.IsExtglob} is-extglob 2.1.1 (Copyright (c) 2014-2016, Jon Schlinkert);
 * {@code Configuration} camelcase 5.3.1 (Copyright (c) Sindre Sorhus &lt;sindresorhus@gmail.com&gt;
 * (sindresorhus.com)); {@code internal.lock.SpecUtils} corepack 0.35.0's specUtils.ts and Engine.ts (Copyright (c)
 * Corepack contributors); and {@code Tinylogic} tinylogic 2.0.0 (Copyright (c) 2020 Mael Nison) with the parser
 * pegjs 0.10 generates for it (Copyright (c) 2010-2016 David Majda), all distributed under the MIT License:
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
 * {@code TgzUtils} reads tarballs as node-tar 6.1.11 parses them (Copyright (c) Isaac Z. Schlueter and
 * Contributors), and {@code internal.lock.GlobParent} ports glob-parent 5.1.2 (Copyright (c) 2015, 2019 Elan
 * Shanker), both distributed under the ISC License:
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
package org.openrewrite.javascript.internal.lock.berry;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
