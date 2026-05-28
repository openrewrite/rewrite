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
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

public partial interface Cs
{
    /// <summary>
    /// Extract the simple name from a <see cref="NameTree"/> node.
    /// Works with <see cref="Identifier"/> and <see cref="FieldAccess"/>.
    /// </summary>
    public static string? GetSimpleName(NameTree node) => node switch
    {
        Identifier id => id.SimpleName,
        FieldAccess fa => fa.Name.Element.SimpleName,
        _ => null
    };

    /// <summary>
    /// Check if a <see cref="MethodInvocation"/> has zero real arguments.
    /// Handles the <see cref="Empty"/> sentinel that represents an empty argument list.
    /// </summary>
    public static bool HasNoArguments(MethodInvocation mi)
    {
        var elems = mi.Arguments.Elements;
        return elems.Count == 0 || (elems.Count == 1 && elems[0].Element is Empty);
    }
}
