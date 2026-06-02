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
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp.Format;

/// <summary>
/// Ensures minimum viable spacing between AST elements so that the printed
/// output is parseable C#. This visitor ONLY inserts single spaces where needed
/// to prevent token merging (e.g., "publicvoid" → "public void").
///
/// This visitor must NOT insert indentation or general structural formatting.
/// Its sole purpose is preventing adjacent tokens from fusing into unparseable output.
/// All formatting beyond token separation is Roslyn's responsibility — with one
/// exception documented inline as a workaround for a Roslyn formatter bug.
/// </summary>
public class MinimumViableSpacingVisitor : CSharpVisitor<int>
{
    public override J VisitClassDeclaration(ClassDeclaration classDecl, int p)
    {
        var c = (ClassDeclaration)base.VisitClassDeclaration(classDecl, p);

        var first = c.LeadingAnnotations.Count == 0;

        // Space between annotations and modifiers, and between modifiers
        if (c.Modifiers.Count > 0)
        {
            c = c.WithModifiers(EnsureModifierSpacing(c.Modifiers, first));
            first = false;
        }

        // Space before class/struct/record keyword
        if (!first && c.ClassKind.Prefix.IsEmpty)
        {
            c = c.WithClassKind(c.ClassKind.WithPrefix(c.ClassKind.Prefix.WithWhitespace(" ")));
        }
        // After the keyword, we always need space before the next token
        first = false;

        // Space before class name
        if (c.Name.Prefix.IsEmpty)
        {
            c = c.WithName(c.Name.WithPrefix(c.Name.Prefix.WithWhitespace(" ")));
        }

        // Space before : in extends (base class)
        if (c.Extends != null && c.Extends.Before.IsEmpty)
        {
            var ext = c.Extends.WithBefore(c.Extends.Before.WithWhitespace(" "));
            if (c.Extends.Element.Prefix.IsEmpty)
            {
                ext = ext.WithElement((TypeTree)EnsureSpace(ext.Element));
            }
            c = c.WithExtends(ext);
        }

        // Space before : in implements (interfaces)
        if (c.Implements != null && c.Implements.Before.IsEmpty)
        {
            var impl = c.Implements.WithBefore(c.Implements.Before.WithWhitespace(" "));
            if (impl.Elements.Count > 0 && impl.Elements[0].Element.Prefix.IsEmpty)
            {
                var elements = ReplaceAt(impl.Elements, 0,
                    impl.Elements[0].WithElement((TypeTree)EnsureSpace(impl.Elements[0].Element)));
                impl = impl.WithElements(elements);
            }
            c = c.WithImplements(impl);
        }

        return c;
    }

    public override J VisitMethodDeclaration(MethodDeclaration method, int p)
    {
        var m = (MethodDeclaration)base.VisitMethodDeclaration(method, p);

        var first = m.LeadingAnnotations.Count == 0;

        // Space between annotations and modifiers, and between modifiers
        if (m.Modifiers.Count > 0)
        {
            m = m.WithModifiers(EnsureModifierSpacing(m.Modifiers, first));
            first = false;
        }

        // Space before return type
        if (!first && m.ReturnTypeExpression != null && m.ReturnTypeExpression.Prefix.IsEmpty)
        {
            m = m.WithReturnTypeExpression((TypeTree)EnsureSpace(m.ReturnTypeExpression));
        }
        if (m.ReturnTypeExpression != null)
        {
            first = false;
        }

        // Space before method name
        if (!first && m.Name.Prefix.IsEmpty)
        {
            m = m.WithName(m.Name.WithPrefix(m.Name.Prefix.WithWhitespace(" ")));
        }

        // Space before : in constructor initializer (DefaultValue)
        if (m.DefaultValue != null && m.DefaultValue.Before.IsEmpty)
        {
            var dv = m.DefaultValue.WithBefore(m.DefaultValue.Before.WithWhitespace(" "));
            if (dv.Element.Prefix.IsEmpty)
            {
                dv = dv.WithElement((Expression)EnsureSpace(dv.Element));
            }
            m = m.WithDefaultValue(dv);
        }

        // Workaround for https://github.com/dotnet/roslyn/issues/82974:
        // Roslyn's Formatter.Format does not expand empty constructor bodies to
        // Allman style when a constructor initializer (`: base(...)`) is present.
        // Insert newlines so the printed source has multi-line braces that Roslyn
        // can then indent correctly.
        if (m.DefaultValue != null &&
            m.Body is { Statements.Count: 0 } body &&
            body.Prefix.IsEmpty && body.End.IsEmpty)
        {
            m = m.WithBody(body
                .WithPrefix(body.Prefix.WithWhitespace("\n"))
                .WithEnd(body.End.WithWhitespace("\n")));
        }

        return m;
    }

    public override J VisitVariableDeclarations(VariableDeclarations varDecl, int p)
    {
        var v = (VariableDeclarations)base.VisitVariableDeclarations(varDecl, p);

        var hasLeadingAnnotations = v.LeadingAnnotations.Count > 0;
        var hasModifiers = v.Modifiers.Count > 0;

        // Space between modifiers
        if (hasModifiers)
        {
            v = v.WithModifiers(EnsureModifierSpacing(v.Modifiers, !hasLeadingAnnotations));
        }

        // Space before type expression (after modifiers or annotations)
        if ((hasLeadingAnnotations || hasModifiers) && v.TypeExpression != null && v.TypeExpression.Prefix.IsEmpty)
        {
            v = v.WithTypeExpression((TypeTree)EnsureSpace(v.TypeExpression));
        }

        // Space before variable name (after type)
        // The space between type and variable can be on NamedVariable.Prefix or NamedVariable.Name.Prefix
        if (v.TypeExpression != null && v.Variables.Count > 0)
        {
            var firstVar = v.Variables[0].Element;
            if (firstVar.Prefix.IsEmpty && firstVar.Name.Prefix.IsEmpty)
            {
                var variables = ReplaceAt(v.Variables, 0,
                    v.Variables[0].WithElement(
                        firstVar.WithName(firstVar.Name.WithPrefix(firstVar.Name.Prefix.WithWhitespace(" ")))));
                v = v.WithVariables(variables);
            }
        }

        return v;
    }

    public override J VisitReturn(Return ret, int p)
    {
        var r = (Return)base.VisitReturn(ret, p);
        if (r.Expression != null && r.Expression.Prefix.IsEmpty)
        {
            r = r.WithExpression((Expression)EnsureSpace(r.Expression));
        }
        return r;
    }

    public override J VisitNewClass(NewClass nc, int p)
    {
        var n = (NewClass)base.VisitNewClass(nc, p);
        // The printer outputs: "new" + nc.New + Clazz.Prefix + ClassName
        // Need space between "new" and the type name: either in New or Clazz.Prefix
        if (n.Clazz != null && n.New.IsEmpty && n.Clazz.Prefix.IsEmpty)
        {
            n = n.WithNew(n.New.WithWhitespace(" "));
        }
        return n;
    }

    public override J VisitAwaitExpression(AwaitExpression ae, int p)
    {
        var a = (AwaitExpression)base.VisitAwaitExpression(ae, p);
        if (a.Expression.Prefix.IsEmpty)
        {
            a = a.WithExpression((Expression)EnsureSpace(a.Expression));
        }
        return a;
    }

    public override J VisitYield(Yield yield, int p)
    {
        var y = (Yield)base.VisitYield(yield, p);
        // yield return <expr> or yield break
        if (y.ReturnOrBreakKeyword.Prefix.IsEmpty)
        {
            y = y.WithReturnOrBreakKeyword(
                y.ReturnOrBreakKeyword.WithPrefix(y.ReturnOrBreakKeyword.Prefix.WithWhitespace(" ")));
        }
        if (y.Expression != null && y.Expression.Prefix.IsEmpty)
        {
            y = y.WithExpression((Expression)EnsureSpace(y.Expression));
        }
        return y;
    }

    public override J VisitThrow(Throw thr, int p)
    {
        var t = (Throw)base.VisitThrow(thr, p);
        if (t.Exception.Prefix.IsEmpty)
        {
            t = t.WithException((Expression)EnsureSpace(t.Exception));
        }
        return t;
    }

    // ---- using directive: "using" [static] [unsafe] [alias =] NamespaceOrType ----
    public override J VisitUsingDirective(UsingDirective usingDirective, int p)
    {
        var ud = (UsingDirective)base.VisitUsingDirective(usingDirective, p);
        // The printer outputs: "using" + optionally Static.Before+"static" + optionally Unsafe.Before+"unsafe"
        //   + optionally Alias + "=" + NamespaceOrType
        // The NamespaceOrType.Prefix (or Static.Before/Unsafe.Before/Alias prefix) must carry the space
        // after the last keyword.
        if (ud.IsStatic && ud.Static.Before.IsEmpty)
        {
            ud = ud.WithStatic(ud.Static.WithBefore(ud.Static.Before.WithWhitespace(" ")));
        }
        if (ud.NamespaceOrType.Prefix.IsEmpty)
        {
            ud = ud.WithNamespaceOrType((TypeTree)EnsureSpace(ud.NamespaceOrType));
        }
        return ud;
    }

    // ---- namespace (file-scoped): "namespace" Expression ----
    public override J VisitPackage(Package pkg, int p)
    {
        var k = (Package)base.VisitPackage(pkg, p);
        if (k.Expression.Prefix.IsEmpty)
        {
            k = k.WithExpression((Expression)EnsureSpace(k.Expression));
        }
        return k;
    }

    // ---- namespace { } block: "namespace" Name ----
    public override J VisitNamespaceDeclaration(NamespaceDeclaration ns, int p)
    {
        var n = (NamespaceDeclaration)base.VisitNamespaceDeclaration(ns, p);
        if (n.Name.Element.Prefix.IsEmpty)
        {
            n = n.WithName(n.Name.WithElement(
                (Expression)EnsureSpace(n.Name.Element)));
        }
        return n;
    }

    // ---- property: [modifiers] Type Name { get; set; } ----
    public override J VisitPropertyDeclaration(PropertyDeclaration prop, int p)
    {
        var pd = (PropertyDeclaration)base.VisitPropertyDeclaration(prop, p);

        var first = pd.AttributeLists.Count == 0;

        if (pd.Modifiers.Count > 0)
        {
            pd = pd.WithModifiers(EnsureModifierSpacing(pd.Modifiers, first));
            first = false;
        }

        // Space before type
        if (!first && pd.TypeExpression.Prefix.IsEmpty)
        {
            pd = pd.WithTypeExpression((TypeTree)EnsureSpace(pd.TypeExpression));
        }
        first = false;

        // Space before property name
        if (pd.Name.Prefix.IsEmpty)
        {
            pd = pd.WithName(pd.Name.WithPrefix(pd.Name.Prefix.WithWhitespace(" ")));
        }

        return pd;
    }

    // ---- foreach control: "in" Iterable ----
    public override J VisitForEachControl(ForEachLoop.Control control, int p)
    {
        var c = (ForEachLoop.Control)base.VisitForEachControl(control, p);
        // The printer outputs: Variable + Variable.After + "in" + Iterable.Element
        // The Iterable.Element.Prefix must carry space after "in"
        if (c.Iterable.Element.Prefix.IsEmpty)
        {
            c = c.WithIterable(c.Iterable.WithElement((Expression)EnsureSpace(c.Iterable.Element)));
        }
        return c;
    }

    /// <summary>
    /// Ensures that a J element has at least a single space as its prefix whitespace.
    /// Uses dynamic dispatch since WithPrefix is defined on concrete types, not on the J interface.
    /// </summary>
    private static J EnsureSpace(J element)
    {
        if (!element.Prefix.IsEmpty)
            return element;
        return (J)((dynamic)element).WithPrefix(element.Prefix.WithWhitespace(" "));
    }

    /// <summary>
    /// Ensures spaces between modifiers in a list. If <paramref name="firstIsFirst"/> is true,
    /// the first modifier doesn't need spacing (it's the first token in the declaration).
    /// </summary>
    private static IList<Modifier> EnsureModifierSpacing(IList<Modifier> modifiers, bool firstIsFirst)
    {
        var changed = false;
        IList<Modifier> result = modifiers;

        for (var i = 0; i < modifiers.Count; i++)
        {
            if ((i > 0 || !firstIsFirst) && modifiers[i].Prefix.IsEmpty)
            {
                if (!changed)
                {
                    result = new List<Modifier>(modifiers);
                    changed = true;
                }
                result[i] = modifiers[i].WithPrefix(modifiers[i].Prefix.WithWhitespace(" "));
            }
        }

        return result;
    }

    /// <summary>
    /// Replaces an element at a given index in an immutable list, returning a new list.
    /// </summary>
    private static IList<T> ReplaceAt<T>(IList<T> list, int index, T replacement)
    {
        var newList = new List<T>(list);
        newList[index] = replacement;
        return newList;
    }
}
