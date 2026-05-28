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
using System.Text.RegularExpressions;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Matches method invocations and declarations against AspectJ-style method patterns.
/// C# equivalent of Java's <c>org.openrewrite.java.MethodMatcher</c>.
///
/// Pattern syntax:
/// <code>
///   DeclaringType MethodName(ArgType1, ArgType2, ..)
///
///   Examples:
///     System.Threading.Tasks.Task Delay(..)          — any overload
///     System.String *(bool)                          — any method taking a bool
///     *..* *(..)                                     — any method on any type
///     System.IO.File WriteAllTextAsync(string, ..)   — first arg is string
/// </code>
///
/// Wildcards:
/// <list type="bullet">
///   <item><c>*</c> — matches any single name segment</item>
///   <item><c>..</c> — in arguments: zero or more arguments of any type</item>
///   <item><c>*..*</c> — in declaring type: any type in any namespace</item>
/// </list>
/// </summary>
public class MethodMatcher
{
    private readonly Regex _declaringTypePattern;
    private readonly Regex _methodNamePattern;
    private readonly ArgumentMatcher[] _argMatchers;
    private readonly bool _matchOverrides;
    private readonly bool _hasWildcardArgs;

    public MethodMatcher(string methodPattern, bool matchOverrides = false)
    {
        _matchOverrides = matchOverrides;
        var (declaringType, methodName, args) = ParsePattern(methodPattern);

        _declaringTypePattern = GlobToRegex(declaringType);
        _methodNamePattern = GlobToRegex(methodName);

        if (args == "..")
        {
            _argMatchers = [];
            _hasWildcardArgs = true;
        }
        else if (string.IsNullOrEmpty(args))
        {
            _argMatchers = [];
            _hasWildcardArgs = false;
        }
        else
        {
            var parts = args.Split(',').Select(p => p.Trim()).ToArray();
            var matchers = new List<ArgumentMatcher>();
            var hasWild = false;
            foreach (var part in parts)
            {
                if (part == "..")
                {
                    hasWild = true;
                    matchers.Add(new WildcardArgMatcher());
                }
                else
                {
                    matchers.Add(new TypeArgMatcher(GlobToRegex(part), part));
                }
            }
            _argMatchers = matchers.ToArray();
            _hasWildcardArgs = hasWild;
        }
    }

    /// <summary>
    /// Match against a <see cref="MethodInvocation"/>.
    /// </summary>
    public bool Matches(MethodInvocation mi)
    {
        if (mi.MethodType != null)
            return Matches(mi.MethodType);

        // Fallback: name-only matching when type info is unavailable
        return _methodNamePattern.IsMatch(mi.Name.SimpleName);
    }

    /// <summary>
    /// Match against a <see cref="JavaType.Method"/>.
    /// </summary>
    public bool Matches(JavaType.Method methodType)
    {
        // Check method name
        if (!_methodNamePattern.IsMatch(methodType.Name))
            return false;

        // Check declaring type
        if (!MatchesDeclaringType(methodType))
            return false;

        // Check arguments
        if (!MatchesArguments(methodType))
            return false;

        return true;
    }

    /// <summary>
    /// Match against a <see cref="MethodDeclaration"/>.
    /// </summary>
    public bool Matches(MethodDeclaration md)
    {
        if (md.MethodType != null)
            return Matches(md.MethodType);

        return _methodNamePattern.IsMatch(md.Name.SimpleName);
    }

    private bool MatchesDeclaringType(JavaType.Method methodType)
    {
        var declaring = methodType.DeclaringType;
        if (declaring == null)
            return true; // Can't check — allow match

        var fqn = TypeUtils.GetFullyQualifiedName(declaring);
        if (fqn == null)
            return true;

        if (_declaringTypePattern.IsMatch(fqn))
            return true;

        // Check supertypes if matchOverrides is enabled
        if (_matchOverrides)
        {
            var cls = TypeUtils.AsClass(declaring);
            if (cls != null)
                return MatchesTypeHierarchy(cls, new HashSet<string>());
        }

        return false;
    }

    private bool MatchesTypeHierarchy(JavaType.Class cls, HashSet<string> seen)
    {
        if (!seen.Add(cls.FullyQualifiedName))
            return false;

        if (_declaringTypePattern.IsMatch(cls.FullyQualifiedName))
            return true;

        var super = TypeUtils.AsClass(cls.Supertype);
        if (super != null && MatchesTypeHierarchy(super, seen))
            return true;

        if (cls.Interfaces != null)
        {
            foreach (var iface in cls.Interfaces)
            {
                var ifaceCls = TypeUtils.AsClass(iface);
                if (ifaceCls != null && MatchesTypeHierarchy(ifaceCls, seen))
                    return true;
            }
        }

        return false;
    }

    private bool MatchesArguments(JavaType.Method methodType)
    {
        if (_hasWildcardArgs && _argMatchers.Length == 0)
            return true; // (..) matches anything

        var paramTypes = methodType.ParameterTypes;
        var paramCount = paramTypes?.Count ?? 0;

        if (!_hasWildcardArgs)
        {
            // Exact argument count match required
            if (_argMatchers.Length != paramCount)
                return false;

            for (var i = 0; i < _argMatchers.Length; i++)
            {
                if (!_argMatchers[i].Matches(paramTypes![i]))
                    return false;
            }
            return true;
        }

        // Has at least one ".." wildcard — use backtracking
        return MatchArgsWithWildcard(paramTypes, 0, 0);
    }

    private bool MatchArgsWithWildcard(IList<JavaType>? paramTypes, int mi, int pi)
    {
        var paramCount = paramTypes?.Count ?? 0;

        if (mi >= _argMatchers.Length)
            return pi >= paramCount;

        if (_argMatchers[mi] is WildcardArgMatcher)
        {
            // ".." matches zero or more args — try greedy to lazy
            for (var consume = paramCount - pi; consume >= 0; consume--)
            {
                if (MatchArgsWithWildcard(paramTypes, mi + 1, pi + consume))
                    return true;
            }
            return false;
        }

        if (pi >= paramCount)
            return false;

        if (!_argMatchers[mi].Matches(paramTypes![pi]))
            return false;

        return MatchArgsWithWildcard(paramTypes, mi + 1, pi + 1);
    }

    private static (string declaringType, string methodName, string args) ParsePattern(string pattern)
    {
        // Pattern: "DeclaringType MethodName(args)"
        var parenStart = pattern.IndexOf('(');
        var parenEnd = pattern.LastIndexOf(')');

        string args;
        string beforeArgs;
        if (parenStart >= 0 && parenEnd > parenStart)
        {
            args = pattern[(parenStart + 1)..parenEnd].Trim();
            beforeArgs = pattern[..parenStart].Trim();
        }
        else
        {
            args = "..";
            beforeArgs = pattern.Trim();
        }

        // Split "DeclaringType MethodName" on last space
        var lastSpace = beforeArgs.LastIndexOf(' ');
        if (lastSpace < 0)
        {
            // No declaring type — match any type
            return ("*..*", beforeArgs, args);
        }

        var declaringType = beforeArgs[..lastSpace].Trim();
        var methodName = beforeArgs[(lastSpace + 1)..].Trim();
        return (declaringType, methodName, args);
    }

    private static Regex GlobToRegex(string glob)
    {
        // Convert AspectJ-style glob to regex:
        // "*..*" → ".*"  (any type in any namespace)
        // "*"    → "[^.]*" (any single segment)
        // "Foo"  → "Foo" (exact match)
        var pattern = Regex.Escape(glob)
            .Replace(@"\*\.\.\*", ".*")
            .Replace(@"\.\.", ".+")
            .Replace(@"\*", "[^.]*");

        return new Regex($"^{pattern}$", RegexOptions.Compiled);
    }

    // C# keyword aliases to fully-qualified names
    private static readonly Dictionary<string, string> KeywordToFqn = new(StringComparer.Ordinal)
    {
        ["bool"] = "System.Boolean",
        ["byte"] = "System.Byte",
        ["sbyte"] = "System.SByte",
        ["char"] = "System.Char",
        ["decimal"] = "System.Decimal",
        ["double"] = "System.Double",
        ["float"] = "System.Single",
        ["int"] = "System.Int32",
        ["uint"] = "System.UInt32",
        ["long"] = "System.Int64",
        ["ulong"] = "System.UInt64",
        ["short"] = "System.Int16",
        ["ushort"] = "System.UInt16",
        ["string"] = "System.String",
        ["object"] = "System.Object",
    };

    private abstract class ArgumentMatcher
    {
        public abstract bool Matches(JavaType paramType);
    }

    private class TypeArgMatcher : ArgumentMatcher
    {
        private readonly Regex _pattern;
        private readonly Regex? _fqnPattern;

        public TypeArgMatcher(Regex pattern, string originalGlob)
        {
            _pattern = pattern;
            // If the pattern is a C# keyword, also match the fully-qualified name
            if (KeywordToFqn.TryGetValue(originalGlob, out var fqn))
                _fqnPattern = GlobToRegex(fqn);
        }

        public override bool Matches(JavaType paramType)
        {
            var fqn = TypeUtils.GetFullyQualifiedName(paramType);
            if (fqn != null)
            {
                if (_pattern.IsMatch(fqn)) return true;
                if (_fqnPattern != null && _fqnPattern.IsMatch(fqn)) return true;

                // Match the simple name (last segment) against the pattern.
                // This allows patterns like "String" to match "System.String".
                var lastDot = fqn.LastIndexOf('.');
                if (lastDot >= 0)
                {
                    var simpleName = fqn[(lastDot + 1)..];
                    if (_pattern.IsMatch(simpleName)) return true;
                }
            }

            // Also match simple name for primitives/keywords
            if (paramType is JavaType.Primitive prim)
            {
                if (_pattern.IsMatch(prim.Keyword)) return true;
            }

            return false;
        }
    }

    private class WildcardArgMatcher : ArgumentMatcher
    {
        public override bool Matches(JavaType paramType) => true;
    }
}
