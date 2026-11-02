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
 * pnpm's lockfile model and resolver, ported from pnpm 11.2.2 (pnpm/pnpm, tag v11.2.2): lockfile/types,
 * lockfile/fs (read, write, the format converters, sortLockfileKeys, the env document), lockfile/merger,
 * lockfile/settings-checker, lockfile/verification, lockfile/preferred-versions, lockfile/utils, deps/path,
 * object/key-sorting, config/reader (the .npmrc, pnpm-workspace.yaml and environment settings), config/matcher,
 * config/version-policy, config/pick-registry-for-package, config/parse-overrides, catalogs/*,
 * workspace/workspace-manifest-reader, workspace/workspace-manifest-writer, yaml/document-sync (patchDocument, in
 * {@code internal.lock.YamlDocument}), workspace/project-manifest-reader, workspace/projects-reader,
 * pkg-manifest/utils, resolving/resolver-base, resolving/parse-wanted-dependency, resolving/npm-resolver
 * (pickPackage, pickPackageFromMeta, the resolution verifier, trustChecks), resolving/registry/pkg-metadata-filter,
 * crypto/object-hasher, crypto/hash, installing/env-installer, installing/deps-installer (the lock-only install and
 * the lockfile's verification), installing/deps-resolver, installing/package-requester, resolving/default-resolver
 * with the local, git and tarball resolvers' lock-only paths, engine/runtime/node-resolver, deno-resolver and
 * bun-resolver, crypto/shasums-file, lockfile/pruner, config/package-is-installable, cli/utils' packageIsInstallable,
 * workspace/range-resolver, workspace/spec-parser, workspace/projects-graph, workspace/projects-sorter,
 * deps/graph-sequencer and the pnpm command's own checks before an install. pnpm is distributed under the MIT
 * License:
 * <pre>
 * Copyright (c) 2015-2016 Rico Sta. Cruz and other contributors
 * Copyright (c) 2016-2026 Zoltan Kochan and other contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * </pre>
 * The same license covers what pnpm 11.2.2 bundles and this package ports: {@code internal.lock.JsYaml} and
 * {@code internal.lock.JsYamlDumper}, @zkochan/js-yaml 0.0.11 (Copyright (C) 2011-2015 by Vitaly Puzrin);
 * {@code KeySorting}, sort-keys 6.0.0 and is-plain-obj 4.1.0, and {@code internal.lock.StripBom}, strip-bom 5.0.0
 * (Copyright (c) Sindre Sorhus &lt;sindresorhus@gmail.com&gt;
 * (https://sindresorhus.com)) with @pnpm/util.lex-comparator 3.0.2 and comver-to-semver 2.0.0 (Copyright (c)
 * 2022-2026 Zoltan Kochan); {@code ObjectHasher}, object-hash 3.0.0 (Copyright (c) 2014 object-hash contributors);
 * version-selector-type 3.0.0 (Copyright (c) 2017-2020 Zoltan Kochan &lt;z@kochan.io&gt;); parse-npm-tarball-url
 * 5.0.0 and normalize-registry-url 2.0.1 (Copyright (c) 2017-2026 Zoltan Kochan); get-npm-tarball-url 2.1.0
 * (Copyright (c) 2017-2023 Zoltan Kochan &lt;z@kochan.io&gt;); @pnpm/config.env-replace 4.1.0;
 * {@code SemverRangeIntersect}, semver-range-intersect 0.3.1 (Copyright (c) 2019 sounisi5011); {@code GraphCycles},
 * graph-cycles 3.0.0 and rotated-array-set 3.0.0 (Copyright (c) 2021 Gustaf Räntilä); {@code Tinyglobby},
 * tinyglobby 0.2.16 (Copyright (c) 2024 Madeline Gurriarán); {@code internal.lock.Picomatch},
 * {@code internal.lock.PicomatchParse} and {@code internal.lock.PicomatchScan}, picomatch 4.0.4 (Copyright (c)
 * 2017-present, Jon Schlinkert); and the {@code parseRange} of semver-utils 1.1.4
 * (Copyright 2013 AJ ONeal, offered under the MIT License or Apache-2.0, used here under the MIT License).
 * <p>
 * {@code internal.lock.Ssri} ports ssri 13.0.1 (Copyright (c) npm, Inc.), {@code internal.lock.ValidateNpmPackageName}
 * validate-npm-package-name 4.0.0 and 7.0.2 (Copyright (c) 2015, npm, Inc), {@code internal.lock.Spec} the
 * npm-package-arg fork @pnpm/npm-package-arg 2.0.0 (Copyright (c) Isaac Z. Schlueter), {@code internal.lock.GitHost}
 * hosted-git-info 4.1.0 (Copyright (c) 2015, Rebecca Turner) and {@code internal.lock.YamlDocument} yaml 2.9.0
 * (Copyright Eemeli Aro &lt;eemeli@gmail.com&gt;), each distributed under the ISC License:
 * <pre>
 * Copyright (c) 2015, npm, Inc
 *
 * Permission to use, copy, modify, and/or distribute this software for any purpose with or without fee is hereby
 * granted, provided that the above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
 * INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
 * AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 * </pre>
 * <p>
 * The compatibility database pnpm applies, {@code compat-db.json}, is generated from @yarnpkg/extensions 2.0.6, and
 * {@code internal.lock.YarnpkgSemverUtils} ports {@code satisfiesWithPrereleases} of @yarnpkg/core's semverUtils, both
 * distributed under the BSD 2-Clause License:
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
 */
@NullMarked
@NonNullFields
package org.openrewrite.javascript.internal.lock.pnpm;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
