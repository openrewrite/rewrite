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

"""Regenerate the standalone Java 8 translation of Yarn's exact upstream libzip WASM.
Development tool only. Requires JAVA8_HOME and network access for the pinned compiler jars.
Run: python3 libzip.py <rewrite-javascript module directory> <scratch directory>
No compiler, Kotlin, WASM interpreter, Node, or package manager is required in production.
"""
import base64, gzip, hashlib, io, os, pathlib, re, subprocess, sys, tarfile, urllib.request

ARTIFACTS = [('com/github/cretz/asmble', 'asmble-compiler', '0.4.0', '7d68e03aa0791f03384a54f9ba8d975ab2c9b6080569a9988cb1a2d3d566b5c5'), ('com/github/cretz/asmble', 'asmble-annotations', '0.4.0', '6bf729783b3565e942ee3d2f7537e9ad3841b20dae95f699edc7112d37c7e9a4'), ('org/jetbrains/kotlin', 'kotlin-stdlib', '1.2.61', '62eaf9cc6e746cef4593abe7cdb4dd48694ef5f817c852e0d9fbbd11fcfc564e'), ('org/jetbrains/kotlin', 'kotlin-reflect', '1.2.61', 'a4f1ed542390f9bf967fb9aff2c232b90988f7ce138a4b7bcc70b754821a8943'), ('org/ow2/asm', 'asm', '6.2.1', '1460db6c33cc99c84e5cb30e46b017e4d1cc9a7fbc174101d6f84829bb64c085'), ('org/ow2/asm', 'asm-tree', '6.2.1', 'a520b54c7be4e07e533db8420ddf936fe8341ff56a5df255bab584478dd90aab'), ('org/ow2/asm', 'asm-util', '6.2.1', 'f747f70912e918872c0fe07ef227ff03b2b38fde94778502df7af133f8c8fcc6'), ('org/ow2/asm', 'asm-commons', '6.2.1', '3f578d31ef30f94b6d1f44812f41fe4f98a7cd42af35335f5d4866ab3b901865'), ('org/ow2/asm', 'asm-analysis', '6.2.1', '4c9342c98e746e9c2d7f2cdc6896f7348317e9b1e5a6c591047fc8969def4b23')]
module, scratch = map(pathlib.Path, sys.argv[1:])
resource = module / "src/main/resources/org/openrewrite/javascript/internal/lock/berry"
scratch.mkdir(parents=True, exist_ok=True)
lib = scratch / "lib"
lib.mkdir(exist_ok=True)
for group, artifact, version, digest in ARTIFACTS:
    jar = lib / (artifact + ".jar")
    if not jar.exists():
        jar.write_bytes(urllib.request.urlopen("https://repo.maven.apache.org/maven2/" + group + "/" + artifact + "/" + version + "/" + artifact + "-" + version + ".jar").read())
    assert hashlib.sha256(jar.read_bytes()).hexdigest() == digest, artifact
package = urllib.request.urlopen("https://registry.npmjs.org/@yarnpkg/libzip/-/libzip-3.0.0.tgz").read()
with tarfile.open(fileobj=io.BytesIO(package)) as archive:
    source = archive.extractfile("package/lib/libzipSync.js").read().decode()
wasm = base64.b64decode(max(re.findall(r"[A-Za-z0-9+/=]{10000,}", source), key=len))
assert hashlib.sha256(wasm).hexdigest() == "04b0c8ef0f679cbf8114d96ed00426ed5093eea8560a277ce53d780354c51788"
wasm_path = scratch / "libzip.wasm"
wasm_path.write_bytes(wasm)
output = scratch / "LibzipWasm.class"
java = pathlib.Path(os.environ["JAVA8_HOME"]) / "bin/java"
subprocess.run([str(java), "-Xmx2g", "-cp", str(lib / "*"), "asmble.cli.MainKt", "compile", str(wasm_path),
               "org.openrewrite.javascript.internal.lock.berry.LibzipWasm", "-out", str(output)], check=True)
translated = output.read_bytes()
assert translated[:8] == bytes.fromhex("cafebabe00000034"), "Must be Java 8 bytecode"
assert hashlib.sha256(translated).hexdigest() == "4e3e4ce44b255f243a56311e1dea106f610300439be6442af623aa02fda3fb11"
(resource / "libzip-3.0.0.class.gz").write_bytes(gzip.compress(translated, mtime=0))
print("Reproduced standalone Java 8 libzip: " + str(len(translated)) + " bytes")
