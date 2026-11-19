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
using System.Runtime.ExceptionServices;
using System.Runtime.Loader;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;

namespace OpenRewrite.Test;

/// <summary>
/// Runs the code of a compilation that the <c>inspect</c> callback of <see cref="RecipeSpec.ExpectInputCompiles"/> or
/// <see cref="RecipeSpec.ExpectOutputCompiles"/> gets, such as to compare what the code does before and after the
/// recipe. The code runs on the runtime the tests run on, whatever framework it compiled against.
/// </summary>
public static class CompiledCode
{
    /// <summary>
    /// Calls the method of the type, by their names, with the arguments, on a new instance of the type when the method
    /// isn't static, and returns what it returns. What the method throws, the call throws.
    /// </summary>
    public static object? Invoke(CSharpCompilation compilation, string typeName, string methodName,
        params object?[] arguments)
    {
        using var loaded = Load(compilation);
        return loaded.Invoke(typeName, methodName, arguments);
    }

    /// <summary>
    /// Loads the compilation, and the compilations it references as projects, in a load context of their own, so that
    /// the input and the output of a test, which share an assembly name, load side by side. Disposing it unloads them.
    /// </summary>
    public static LoadedCode Load(CSharpCompilation compilation)
    {
        var context = new AssemblyLoadContext($"{compilation.AssemblyName} {Guid.NewGuid()}", isCollectible: true);
        try
        {
            return new LoadedCode(context, LoadInto(context, compilation, []));
        }
        catch
        {
            context.Unload();
            throw;
        }
    }

    private static Assembly LoadInto(AssemblyLoadContext context, CSharpCompilation compilation,
        Dictionary<string, Assembly> loaded)
    {
        foreach (var reference in compilation.References.OfType<CompilationReference>())
        {
            if (reference.Compilation is CSharpCompilation referenced &&
                !loaded.ContainsKey(referenced.AssemblyName ?? ""))
            {
                LoadInto(context, referenced, loaded);
            }
        }
        using var image = new MemoryStream();
        var emitted = compilation.Emit(image);
        if (!emitted.Success)
        {
            Assert.Fail($"{compilation.AssemblyName} doesn't emit:\n" + string.Join("\n",
                emitted.Diagnostics.Where(d => d.Severity == DiagnosticSeverity.Error).Select(d => $"  {d}")));
        }
        image.Position = 0;
        var assembly = context.LoadFromStream(image);
        loaded[compilation.AssemblyName ?? ""] = assembly;
        return assembly;
    }
}

/// <summary>A compilation <see cref="CompiledCode.Load"/> loaded, until it is disposed.</summary>
public sealed class LoadedCode : IDisposable
{
    private readonly AssemblyLoadContext _context;

    internal LoadedCode(AssemblyLoadContext context, Assembly assembly)
    {
        _context = context;
        Assembly = assembly;
    }

    public Assembly Assembly { get; }

    /// <inheritdoc cref="CompiledCode.Invoke"/>
    public object? Invoke(string typeName, string methodName, params object?[] arguments)
    {
        const BindingFlags flags = BindingFlags.Public | BindingFlags.NonPublic | BindingFlags.Static |
                                   BindingFlags.Instance;
        var type = Assembly.GetType(typeName) ?? throw new ArgumentException(
            $"{Assembly.GetName().Name} declares no type {typeName}, and declares " +
            $"{string.Join(", ", Assembly.GetTypes().Select(t => t.FullName))}.");
        var methods = type.GetMethods(flags)
            .Where(m => m.Name == methodName && m.GetParameters().Length == arguments.Length)
            .ToList();
        if (methods.Count != 1)
        {
            throw new ArgumentException(
                $"{typeName} declares {(methods.Count == 0 ? "no" : methods.Count.ToString())} methods {methodName} " +
                $"of {arguments.Length} parameters.");
        }
        var method = methods[0];
        var target = method.IsStatic ? null : Activator.CreateInstance(type, nonPublic: true);
        try
        {
            return method.Invoke(target, arguments);
        }
        catch (TargetInvocationException e) when (e.InnerException != null)
        {
            ExceptionDispatchInfo.Capture(e.InnerException).Throw();
            throw;
        }
    }

    public void Dispose() => _context.Unload();
}
