# Yarn libzip on Java 8

`libzip-3.0.0.class.gz` is a deterministic, standalone Java-8 translation of the
WebAssembly embedded in npm `@yarnpkg/libzip@3.0.0/lib/libzipSync.js`.
It is loaded lazily only for compressed archives. There is no production compiler,
interpreter, native library, Node, package-manager process, or additional dependency.
The level-0 writer remains unchanged.

Upstream WASM SHA-256: `04b0c8ef0f679cbf8114d96ed00426ed5093eea8560a277ce53d780354c51788`.
Translated class SHA-256: `4e3e4ce44b255f243a56311e1dea106f610300439be6442af623aa02fda3fb11`.
Classfile major version: 52. The published libzip 3.0.0, 3.1.0 and 3.2.2 contain
identical WASM bytes. Yarn 4.0.2, 4.6.0, 4.12.0 and 4.18.1 all build zlib-ng 2.1.2
with Emscripten 2.0.22 and arcanis/libzip commit
`664462465d2730d51f04437c90ed7ebcbe19a36f`.

Regenerate using `src/test/resources/yarn-berry/generate/libzip.py` with
`JAVA8_HOME` set. It downloads hash-pinned Asmble 0.4.0 compiler dependencies and
checks both input and output hashes. Asmble is a development tool only. Its age is
a maintenance tradeoff; the translation is pinned, reproducible and tested against
actual Yarn archives rather than accepting arbitrary WASM at runtime.

The bridge implements only the six Emscripten imports this exact module uses.
Time is deterministic and UTC, matching ZipFS SAFE_TIME. Each archive owns its
WASM memory, bounded from the source size; no global mutable libzip heap is shared.
This retains zlib-ng's byte stream, including libzip's mixed-mode decisions, rather
than substituting the host JDK's zlib. Java 8 execution is explicitly verified.

Oracle fixtures: npm prettier 3.9.10 and ms 2.1.3, converted by actual Yarn 4.0.2
with enableGlobalCache:false and each recorded compressionLevel. Tests compare
complete ZIP bytes, including entry order, metadata, and compressed streams.

Upstream sources:
- https://github.com/yarnpkg/berry/blob/@yarnpkg/cli/4.0.2/packages/yarnpkg-libzip/artifacts/build.sh
- https://github.com/yarnpkg/berry/blob/@yarnpkg/cli/4.0.2/packages/yarnpkg-libzip/sources/ZipFS.ts
- https://github.com/cretz/asmble/tree/0.4.0

Third-party notices follow.

## yarn

BSD 2-Clause License

Copyright (c) 2016-present, Yarn Contributors.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


## libzip

Copyright (C) 1999-2019 Dieter Baron and Thomas Klausner

The authors can be contacted at <libzip@nih.at>

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in
  the documentation and/or other materials provided with the
  distribution.

3. The names of the authors may not be used to endorse or promote
  products derived from this software without specific prior
  written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


## zlib-ng

(C) 1995-2013 Jean-loup Gailly and Mark Adler

This software is provided 'as-is', without any express or implied
warranty. In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required.

2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.

3. This notice may not be removed or altered from any source distribution.


## asmble

MIT License

Copyright (c) 2018 Chad Retz

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
