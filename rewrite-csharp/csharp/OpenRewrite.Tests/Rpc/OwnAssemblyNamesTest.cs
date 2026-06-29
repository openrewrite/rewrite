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
using OpenRewrite.CSharp.Rpc;

namespace OpenRewrite.Tests.Rpc;

/// <summary>
/// Verifies that <c>OwnAssemblyNames</c> returns only the direct package's
/// own assemblies from its <c>lib/&lt;tfm&gt;</c> folder in the global packages
/// cache — not transitive dependency DLLs.
/// </summary>
public class OwnAssemblyNamesTest
{
    [Fact]
    public void OwnAssemblyNames_ReturnsOnlyTheDirectPackagesOwnAssemblies()
    {
        var root = Path.Combine(Path.GetTempPath(), "own-asm-test", Guid.NewGuid().ToString("N")[..8]);
        try
        {
            // direct package: my.package 1.0.0 -> lib/net10.0/Primary.dll
            Directory.CreateDirectory(Path.Combine(root, "my.package", "1.0.0", "lib", "net10.0"));
            File.WriteAllText(Path.Combine(root, "my.package", "1.0.0", "lib", "net10.0", "Primary.dll"), "");
            // a transitive dependency that must NOT be reported
            Directory.CreateDirectory(Path.Combine(root, "dep.package", "2.0.0", "lib", "net10.0"));
            File.WriteAllText(Path.Combine(root, "dep.package", "2.0.0", "lib", "net10.0", "DepLib.dll"), "");

            var own = RewriteRpcServer.OwnAssemblyNamesForTest("My.Package", "1.0.0", root);

            Assert.Contains("Primary", own);
            Assert.DoesNotContain("DepLib", own);
        }
        finally { try { Directory.Delete(root, true); } catch { } }
    }
}
