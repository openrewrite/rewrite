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
using OpenRewrite.Recipes.Net9;
using OpenRewrite.Test;

namespace OpenRewrite.Tests.Recipe.Net9;

public class X509CertificateConstructorsSearchRecipeTest : RewriteTest
{
    [Fact]
    public void FindsX509Certificate2ByteArrayConstructor()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new X509CertificateConstructorsSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M(byte[] data)
                    {
                        var cert = new X509Certificate2(data);
                    }
                }
                """,
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M(byte[] data)
                    {
                        var cert = /*~~(X509Certificate/X509Certificate2 constructors for binary and file content are obsolete in .NET 9 (SYSLIB0057). Use X509CertificateLoader methods instead.)~~>*/new X509Certificate2(data);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindsX509Certificate2StringConstructor()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new X509CertificateConstructorsSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M()
                    {
                        var cert = new X509Certificate2("cert.pfx");
                    }
                }
                """,
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M()
                    {
                        var cert = /*~~(X509Certificate/X509Certificate2 constructors for binary and file content are obsolete in .NET 9 (SYSLIB0057). Use X509CertificateLoader methods instead.)~~>*/new X509Certificate2("cert.pfx");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindsX509CertificateConstructor()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new X509CertificateConstructorsSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M(byte[] data)
                    {
                        var cert = new X509Certificate(data);
                    }
                }
                """,
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M(byte[] data)
                    {
                        var cert = /*~~(X509Certificate/X509Certificate2 constructors for binary and file content are obsolete in .NET 9 (SYSLIB0057). Use X509CertificateLoader methods instead.)~~>*/new X509Certificate(data);
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void FindsX509Certificate2WithPasswordConstructor()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new X509CertificateConstructorsSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M(byte[] data)
                    {
                        var cert = new X509Certificate2(data, "password");
                    }
                }
                """,
                """
                using System.Security.Cryptography.X509Certificates;
                class Test
                {
                    void M(byte[] data)
                    {
                        var cert = /*~~(X509Certificate/X509Certificate2 constructors for binary and file content are obsolete in .NET 9 (SYSLIB0057). Use X509CertificateLoader methods instead.)~~>*/new X509Certificate2(data, "password");
                    }
                }
                """
            )
        );
    }

    [Fact]
    public void NoChangeForStaticLoaderMethod()
    {
        RewriteRun(
            spec => spec
                .SetRecipe(new X509CertificateConstructorsSearchRecipe())
                .SetReferenceAssemblies(Assemblies.Net90),
            CSharp(
                """
                class Test
                {
                    void M()
                    {
                        var x = new System.Collections.Generic.List<string>();
                    }
                }
                """
            )
        );
    }
}
