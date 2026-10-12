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
 *     <li>pacote: Copyright (c) Isaac Z. Schlueter, Kat Marchán, npm, Inc., and Contributors</li>
 *     <li>ssri, which {@code internal.lock.Ssri} ports: Copyright (c) npm, Inc.</li>
 *     <li>npm-package-arg, which {@code internal.lock.Spec} ports: Copyright (c) npm, Inc.</li>
 *     <li>hosted-git-info, which {@code internal.lock.GitHost} ports: Copyright (c) 2015, Rebecca Turner</li>
 *     <li>validate-npm-package-name, which {@code internal.lock.ValidateNpmPackageName} ports: Copyright (c) 2015,
 *     npm, Inc</li>
 *     <li>@npmcli/package-json: Copyright GitHub Inc.</li>
 *     <li>@npmcli/name-from-folder: Copyright npm, Inc.</li>
 *     <li>@npmcli/map-workspaces: Copyright npm, Inc.</li>
 *     <li>walk-up-path: Copyright (c) 2020-2023 Isaac Z. Schlueter</li>
 *     <li>treeverse: Copyright (c) npm, Inc. and Contributors</li>
 *     <li>json-stringify-nice: Copyright (c) Isaac Z. Schlueter and Contributors</li>
 *     <li>ini, which {@code internal.lock.Ini} ports: Copyright (c) Isaac Z. Schlueter and Contributors</li>
 *     <li>@npmcli/config: Copyright (c) npm, Inc.</li>
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
 * {@code NpmInstallChecks} is a port of npm-install-checks, distributed under the BSD 2-Clause License:
 * <pre>
 * Copyright (c) Robert Kowalski and Isaac Z. Schlueter ("Authors")
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </pre>
 * {@code ExplainEresolve} (npm's lib/utils/explain-eresolve.js and explain-dep.js) and the {@code devEngines} check
 * of {@code NpmLockRegenerator} (npm's lib/base-cmd.js) are ports of the npm application, Copyright (c) npm, Inc. and
 * Contributors, licensed on the terms of The Artistic License 2.0
 * (https://www.perlfoundation.org/artistic-license-20.html).
 * <p>
 * {@code internal.lock.PosixPath} is a port of Node.js {@code lib/path.js}, {@code JsonParseEvenBetterErrors} of
 * json-parse-even-better-errors (Copyright 2017 Kat Marchán, Copyright npm, Inc.) and
 * {@code internal.lock.BraceExpansion} of brace-expansion and balanced-match (Copyright Julian Gruber, TypeScript
 * port Copyright Isaac Z. Schlueter), all distributed under the MIT License:
 * <pre>
 * Copyright Joyent, Inc. and other Node contributors.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE
 * USE OR OTHER DEALINGS IN THE SOFTWARE.
 * </pre>
 * {@code internal.lock.Minimatch} is a port of minimatch 10.2.5, distributed under the Blue Oak Model License
 * 1.0.0 (https://blueoakcouncil.org/license/1.0.0): each contributor licenses you to do everything with this
 * software that would otherwise infringe that contributor's copyright in it, provided that everyone who
 * gets a copy of any part of it from you also gets the text of that license or a link to it; as far as the
 * law allows, this software comes as is, without any warranty or condition, and no contributor will be
 * liable to anyone for any damages related to this software or this license, under any kind of legal claim.
 * <p>
 * {@code internal.lock.LocaleCompare} reads data derived from the Unicode Character Database and the CLDR root
 * collation, distributed under the Unicode License V3:
 * <pre>
 * Copyright &copy; 1991-2026 Unicode, Inc.
 *
 * NOTICE TO USER: Carefully read the following legal agreement. BY
 * DOWNLOADING, INSTALLING, COPYING OR OTHERWISE USING DATA FILES, AND/OR
 * SOFTWARE, YOU UNEQUIVOCALLY ACCEPT, AND AGREE TO BE BOUND BY, ALL OF THE
 * TERMS AND CONDITIONS OF THIS AGREEMENT. IF YOU DO NOT AGREE, DO NOT
 * DOWNLOAD, INSTALL, COPY, DISTRIBUTE OR USE THE DATA FILES OR SOFTWARE.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of data files and any associated documentation (the "Data Files") or
 * software and any associated documentation (the "Software") to deal in the
 * Data Files or Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, and/or sell
 * copies of the Data Files or Software, and to permit persons to whom the
 * Data Files or Software are furnished to do so, provided that either (a)
 * this copyright and permission notice appear with all copies of the Data
 * Files or Software, or (b) this copyright and permission notice appear in
 * associated Documentation.
 *
 * THE DATA FILES AND SOFTWARE ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF
 * THIRD PARTY RIGHTS.
 *
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR HOLDERS INCLUDED IN THIS NOTICE
 * BE LIABLE FOR ANY CLAIM, OR ANY SPECIAL INDIRECT OR CONSEQUENTIAL DAMAGES,
 * OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS,
 * WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
 * ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THE DATA
 * FILES OR SOFTWARE.
 *
 * Except as contained in this notice, the name of a copyright holder shall
 * not be used in advertising or otherwise to promote the sale, use or other
 * dealings in these Data Files or Software without prior written
 * authorization of the copyright holder.
 * </pre>
 */
@NullMarked
@NonNullFields
package org.openrewrite.javascript.internal.lock.npm;

import org.jspecify.annotations.NullMarked;
import org.openrewrite.internal.lang.NonNullFields;
