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
using System.Reflection;
using System.Security.Cryptography.Pkcs;
using System.Security.Cryptography.Xml;

namespace OpenRewrite.Tests;

public class VulnerableDependencyTests
{
    // 10.0.13 fixes CVE-2026-96958, -96959, -96960 (Xml) and CVE-2026-84008, -70162 (Pkcs).
    [Theory]
    [InlineData(typeof(SignedXml))]
    [InlineData(typeof(SignedCms))]
    public void CryptographyPackagesArePatched(Type type)
    {
        var informational = type.Assembly
            .GetCustomAttribute<AssemblyInformationalVersionAttribute>()!.InformationalVersion;
        var version = Version.Parse(informational.Split('+')[0]);
        Assert.True(version >= new Version(10, 0, 13),
            $"{type.Assembly.GetName().Name} resolved to {informational}");
    }
}
