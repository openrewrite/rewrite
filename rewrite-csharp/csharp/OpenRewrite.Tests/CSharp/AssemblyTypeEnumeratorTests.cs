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
using OpenRewrite.CSharp;
using OpenRewrite.Java;

namespace OpenRewrite.Tests.CSharp;

/// <summary>
/// Proves the per-GAV assembly enumeration underlying the DependencyTypes RPC: loading a
/// NuGet package's own dll produces complete JavaType.Class entries (with methods) for its
/// public types, while BCL references stay shallow (FQN-only) for the caller to resolve.
/// </summary>
public class AssemblyTypeEnumeratorTests
{
    private static string NewtonsoftJsonDll()
    {
        var home = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var nugetPackages = Environment.GetEnvironmentVariable("NUGET_PACKAGES")
            ?? Path.Combine(home, ".nuget", "packages");
        return Path.Combine(nugetPackages, "newtonsoft.json", "13.0.3", "lib", "net6.0", "Newtonsoft.Json.dll");
    }

    // The runtime assemblies this test process is running on — a complete BCL reference closure.
    private static List<string> RuntimeAssemblies()
    {
        var runtimeDir = Path.GetDirectoryName(typeof(object).Assembly.Location)!;
        return Directory.GetFiles(runtimeDir, "*.dll").ToList();
    }

    [Fact]
    public void EnumeratesNewtonsoftJsonConvertWithMethods()
    {
        var dll = NewtonsoftJsonDll();
        Assert.True(File.Exists(dll), $"Newtonsoft.Json.dll not found at {dll}");

        var types = AssemblyTypeEnumerator.Enumerate([dll], RuntimeAssemblies());

        var jsonConvert = types.OfType<JavaType.Class>()
            .FirstOrDefault(c => c.FullyQualifiedName == "Newtonsoft.Json.JsonConvert");
        Assert.NotNull(jsonConvert);

        Assert.NotNull(jsonConvert!.Methods);
        Assert.Contains(jsonConvert.Methods!, m => m.Name == "SerializeObject");
        Assert.Contains(jsonConvert.Methods!, m => m.Name == "DeserializeObject");
    }

    [Fact]
    public void OwnTypesAreComplete_BclReferencesAreShallow()
    {
        var types = AssemblyTypeEnumerator.Enumerate([NewtonsoftJsonDll()], RuntimeAssemblies());
        var classes = types.OfType<JavaType.Class>().ToList();

        // Every own type is a distinct FQN entry (no duplicate FQNs a downstream table writer would reject).
        Assert.Equal(classes.Count, classes.Select(c => c.FullyQualifiedName).Distinct().Count());
        var byFqn = classes.ToDictionary(c => c.FullyQualifiedName, c => c);

        // An own type carries its methods.
        Assert.True(byFqn.ContainsKey("Newtonsoft.Json.JsonSerializer"));

        // The BCL is not enumerated as a top-level own type.
        Assert.False(byFqn.ContainsKey("System.Object"),
            "System.Object should stay a shallow reference, not a defined table entry");

        // A referenced BCL supertype resolves to a ShallowClass (FQN only, no body).
        var jsonException = byFqn.GetValueOrDefault("Newtonsoft.Json.JsonException");
        Assert.NotNull(jsonException);
        var supertype = Assert.IsType<JavaType.ShallowClass>(jsonException!.Supertype);
        Assert.Equal("System.Exception", supertype.FullyQualifiedName);
        Assert.Null(supertype.Methods);
    }

    /// <summary>
    /// The dependency type table is where a referenced type's attributes have to come from: an
    /// LST resolves a dependency type through the table, not through the parse of the file that
    /// mentions it. Newtonsoft's <c>JsonConvert</c> carries <c>[NullableContext]</c>/<c>[Nullable]</c>
    /// compiler attributes; <c>JsonPropertyAttribute</c> carries an <c>[AttributeUsage]</c> with
    /// both a positional and a named argument, which exercises both argument forms.
    /// </summary>
    [Fact]
    public void OwnTypeAttributesAreEnumerated()
    {
        var types = AssemblyTypeEnumerator.Enumerate([NewtonsoftJsonDll()], RuntimeAssemblies());
        var byFqn = types.OfType<JavaType.Class>().ToDictionary(c => c.FullyQualifiedName, c => c);

        var jsonProperty = byFqn["Newtonsoft.Json.JsonPropertyAttribute"];
        Assert.NotNull(jsonProperty.Annotations);

        var usage = Assert.Single(jsonProperty.Annotations!.OfType<JavaType.Annotation>(),
            a => a.AnnotationType is JavaType.Class
            {
                FullyQualifiedName: "System.AttributeUsageAttribute"
            });

        // [AttributeUsage(AttributeTargets.Property | AttributeTargets.Field, AllowMultiple = false)]
        // The positional argument resolves to the ValidOn property it feeds; the flags combination
        // matches no single enum member, so the underlying value is kept rather than mis-named.
        var validOn = Assert.Single(usage.Values!.OfType<JavaType.Annotation.SingleElementValue>(),
            v => v.Element is JavaType.Variable { Name: "ValidOn" });
        Assert.NotNull(validOn.ConstantValue);

        var allowMultiple = Assert.Single(usage.Values!.OfType<JavaType.Annotation.SingleElementValue>(),
            v => v.Element is JavaType.Variable { Name: "AllowMultiple" });
        Assert.Equal(false, allowMultiple.ConstantValue);
    }
}
