using Rewrite.Core;
using Rewrite.Java;

namespace Rewrite.CSharp;

/// <summary>
/// Prints C# LST back to source code.
/// Extends CSharpVisitor for cursor tracking, enabling context-aware printing
/// (e.g., distinguishing pattern operators from boolean operators).
/// </summary>
public class CSharpPrinter<P> : CSharpVisitor<PrintOutputCapture<P>>
{
    public string Print(Tree tree)
    {
        var capture = new PrintOutputCapture<P>(default!);
        Visit(tree, capture);
        return capture.ToString();
    }

    public override J VisitCompilationUnit(CompilationUnit compilationUnit, PrintOutputCapture<P> p)
    {
        BeforeSyntax(compilationUnit, p);

        foreach (var member in compilationUnit.Members)
        {
            Visit(member, p);
        }

        VisitSpace(compilationUnit.Eof, p);

        AfterSyntax(compilationUnit, p);
        return compilationUnit;
    }

    public override J VisitUsingDirective(UsingDirective usingDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(usingDirective, p);

        if (usingDirective.IsGlobal)
        {
            p.Append("global");
            VisitSpace(usingDirective.Global.After, p);
        }

        p.Append("using");

        if (usingDirective.IsStatic)
        {
            VisitSpace(usingDirective.Static.Before, p);
            p.Append("static");
        }
        else if (usingDirective.Alias != null)
        {
            Visit(usingDirective.Alias.Element, p);
            VisitSpace(usingDirective.Alias.After, p);
            p.Append('=');
        }

        Visit(usingDirective.NamespaceOrType, p);
        p.Append(';');

        AfterSyntax(usingDirective, p);
        return usingDirective;
    }

    public override J VisitPackage(Package pkg, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pkg, p);
        p.Append("namespace");
        Visit(pkg.Expression.Element, p);
        VisitSpace(pkg.Expression.After, p);
        p.Append(';');
        AfterSyntax(pkg, p);
        return pkg;
    }

    public override J VisitNamespaceDeclaration(NamespaceDeclaration ns, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ns, p);
        p.Append("namespace");
        Visit(ns.Name.Element, p);
        VisitSpace(ns.Name.After, p);
        p.Append('{');

        foreach (var member in ns.Members)
        {
            Visit(member.Element, p);
            VisitSpace(member.After, p);
        }

        VisitSpace(ns.End, p);
        p.Append('}');
        AfterSyntax(ns, p);
        return ns;
    }

    public override J VisitTupleType(TupleType tupleType, PrintOutputCapture<P> p)
    {
        BeforeSyntax(tupleType, p);

        // Print tuple type elements manually to avoid semicolons
        VisitSpace(tupleType.Elements.Before, p);
        p.Append('(');

        for (int i = 0; i < tupleType.Elements.Elements.Count; i++)
        {
            var element = tupleType.Elements.Elements[i];
            VisitVariableDeclarationsWithoutSemicolon(element.Element, p);

            if (i < tupleType.Elements.Elements.Count - 1)
            {
                VisitSpace(element.After, p);
                p.Append(',');
            }
            else
            {
                VisitSpace(element.After, p);
            }
        }

        p.Append(')');
        AfterSyntax(tupleType, p);
        return tupleType;
    }

    public override J VisitTupleExpression(TupleExpression tupleExpr, PrintOutputCapture<P> p)
    {
        BeforeSyntax(tupleExpr, p);
        VisitContainer("(", tupleExpr.Arguments, ",", ")", p);
        AfterSyntax(tupleExpr, p);
        return tupleExpr;
    }

    public override J VisitFieldAccess(FieldAccess fieldAccess, PrintOutputCapture<P> p)
    {
        BeforeSyntax(fieldAccess, p);
        Visit(fieldAccess.Target, p);
        VisitSpace(fieldAccess.Name.Before, p);

        // Check for NullSafe marker on the name - if present, print ?. instead of .
        var isNullSafe = fieldAccess.Name.Element.Markers.FindFirst<NullSafe>() != null;
        p.Append(isNullSafe ? "?." : ".");

        Visit(fieldAccess.Name.Element, p);
        AfterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    public override J VisitArrayAccess(ArrayAccess arrayAccess, PrintOutputCapture<P> p)
    {
        var isMultiDim = arrayAccess.Markers.FindFirst<MultiDimensionalArray>() != null;

        if (isMultiDim && arrayAccess.Indexed is ArrayAccess inner)
        {
            // Multi-dimensional: print inner without closing bracket, then comma, then our index
            PrintArrayAccessWithoutClosingBracket(inner, p);
            BeforeSyntax(arrayAccess, p); // space before comma
            p.Append(',');
            VisitSpace(arrayAccess.Dimension.Prefix, p); // space after comma
            Visit(arrayAccess.Dimension.Index.Element, p);
            VisitSpace(arrayAccess.Dimension.Index.After, p);
            p.Append(']');
        }
        else
        {
            // Normal single-dimension access
            BeforeSyntax(arrayAccess, p);
            Visit(arrayAccess.Indexed, p);
            VisitArrayDimension(arrayAccess.Dimension, p);
        }
        AfterSyntax(arrayAccess, p);
        return arrayAccess;
    }

    private void PrintArrayAccessWithoutClosingBracket(ArrayAccess aa, PrintOutputCapture<P> p)
    {
        var isMultiDim = aa.Markers.FindFirst<MultiDimensionalArray>() != null;

        if (isMultiDim && aa.Indexed is ArrayAccess inner)
        {
            // Recursive: this is also multi-dimensional
            PrintArrayAccessWithoutClosingBracket(inner, p);
            VisitSpace(aa.Prefix, p); // space before comma
            p.Append(',');
            VisitSpace(aa.Dimension.Prefix, p); // space after comma
            Visit(aa.Dimension.Index.Element, p);
            VisitSpace(aa.Dimension.Index.After, p);
            // Don't print ] - parent will
        }
        else
        {
            // Base case: innermost, print up to but not including ]
            VisitSpace(aa.Prefix, p);
            Visit(aa.Indexed, p);
            VisitSpace(aa.Dimension.Prefix, p);
            // Check for NullSafe marker - if present, print ?[ instead of [
            var isNullSafe = aa.Dimension.Markers.FindFirst<NullSafe>() != null;
            p.Append(isNullSafe ? "?[" : "[");
            Visit(aa.Dimension.Index.Element, p);
            VisitSpace(aa.Dimension.Index.After, p);
            // Don't print ] - parent will
        }
    }

    public override J VisitArrayDimension(ArrayDimension dimension, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dimension, p);
        // Check for NullSafe marker - if present, print ?[ instead of [
        var isNullSafe = dimension.Markers.FindFirst<NullSafe>() != null;
        p.Append(isNullSafe ? "?[" : "[");
        Visit(dimension.Index.Element, p);
        VisitSpace(dimension.Index.After, p);
        p.Append(']');
        AfterSyntax(dimension, p);
        return dimension;
    }

    public override J VisitMethodInvocation(MethodInvocation mi, PrintOutputCapture<P> p)
    {
        BeforeSyntax(mi, p);

        // Check for DelegateInvocation marker - if present, skip .Name
        var isDelegateInvocation = mi.Markers.FindFirst<DelegateInvocation>() != null;

        if (mi.Select != null)
        {
            Visit(mi.Select.Element, p);
            VisitSpace(mi.Select.After, p);

            // For delegate invocation, skip the dot and name (it's syntactic sugar for .Invoke())
            if (!isDelegateInvocation)
            {
                // Check for NullSafe marker on the name - if present, print ?. instead of .
                var isNullSafe = mi.Name.Markers.FindFirst<NullSafe>() != null;
                p.Append(isNullSafe ? "?." : ".");
                Visit(mi.Name, p);
            }
        }
        else
        {
            // No select - always print name for unqualified calls
            Visit(mi.Name, p);
        }

        // Print type parameters if present (C# puts these after name)
        if (mi.TypeParameters != null)
        {
            VisitSpace(mi.TypeParameters.Before, p);
            p.Append('<');
            for (int i = 0; i < mi.TypeParameters.Elements.Count; i++)
            {
                var paddedTypeArg = mi.TypeParameters.Elements[i];
                Visit(paddedTypeArg.Element, p);
                if (i < mi.TypeParameters.Elements.Count - 1)
                {
                    VisitSpace(paddedTypeArg.After, p);
                    p.Append(',');
                }
                else
                {
                    VisitSpace(paddedTypeArg.After, p);
                }
            }
            p.Append('>');
        }

        // Print arguments
        VisitArguments(mi.Arguments, p);

        AfterSyntax(mi, p);
        return mi;
    }

    public override J VisitNewClass(NewClass nc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(nc, p);

        // Print enclosing if present (rare in C#)
        if (nc.Enclosing != null)
        {
            Visit(nc.Enclosing.Element, p);
            VisitSpace(nc.Enclosing.After, p);
            p.Append('.');
        }

        p.Append("new");
        VisitSpace(nc.New, p);

        // Print type
        if (nc.Clazz != null)
        {
            Visit(nc.Clazz, p);
        }

        // Print arguments (unless OmitParentheses marker is present)
        var omitParens = nc.Arguments.Markers.FindFirst<OmitParentheses>() != null;
        if (!omitParens)
        {
            VisitArguments(nc.Arguments, p);
        }

        // Print body/initializer if present
        if (nc.Body != null)
        {
            VisitBlock(nc.Body, p);
        }

        AfterSyntax(nc, p);
        return nc;
    }

    public override J VisitNewArray(NewArray na, PrintOutputCapture<P> p)
    {
        BeforeSyntax(na, p);

        p.Append("new");

        // Print type expression if present
        if (na.TypeExpression != null)
        {
            Visit(na.TypeExpression, p);
        }

        // Print dimensions: [size], [], [,], etc.
        foreach (var dim in na.Dimensions)
        {
            VisitSpace(dim.Prefix, p);
            p.Append('[');

            // Print the index/size if not empty
            if (dim.Index.Element is not Empty)
            {
                Visit(dim.Index.Element, p);
            }

            VisitSpace(dim.Index.After, p);
            p.Append(']');
        }

        // Print initializer if present: { 1, 2, 3 }
        if (na.Initializer != null)
        {
            VisitSpace(na.Initializer.Before, p);
            p.Append('{');

            var elements = na.Initializer.Elements;
            for (int i = 0; i < elements.Count; i++)
            {
                var elem = elements[i];
                Visit(elem.Element, p);
                VisitSpace(elem.After, p);
                if (i < elements.Count - 1)
                {
                    p.Append(',');
                }
            }

            p.Append('}');
        }

        AfterSyntax(na, p);
        return na;
    }

    public override J VisitArrayType(ArrayType at, PrintOutputCapture<P> p)
    {
        BeforeSyntax(at, p);

        // Print element type
        Visit(at.ElementType, p);

        // Print dimension: []
        if (at.Dimension != null)
        {
            VisitSpace(at.Dimension.Before, p);
            p.Append('[');
            VisitSpace(at.Dimension.Element, p);
            p.Append(']');
        }

        AfterSyntax(at, p);
        return at;
    }

    public override J VisitNamedExpression(NamedExpression ne, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ne, p);
        Visit(ne.Name.Element, p);
        VisitSpace(ne.Name.After, p);
        p.Append(':');
        Visit(ne.Expression, p);
        AfterSyntax(ne, p);
        return ne;
    }

    public override J VisitRefExpression(RefExpression re, PrintOutputCapture<P> p)
    {
        BeforeSyntax(re, p);
        p.Append(GetRefKindString(re.Kind));
        Visit(re.Expression, p);
        AfterSyntax(re, p);
        return re;
    }

    private static string GetRefKindString(RefKind kind)
    {
        return kind switch
        {
            RefKind.Out => "out",
            RefKind.Ref => "ref",
            RefKind.In => "in",
            _ => throw new InvalidOperationException($"Unknown ref kind: {kind}")
        };
    }

    public override J VisitDeclarationExpression(DeclarationExpression de, PrintOutputCapture<P> p)
    {
        BeforeSyntax(de, p);
        Visit(de.Type, p);
        VisitSpace(de.Variable.Before, p);
        Visit(de.Variable.Element, p);
        AfterSyntax(de, p);
        return de;
    }

    public override J VisitTypeParameterBound(TypeParameterBound tpb, PrintOutputCapture<P> p)
    {
        BeforeSyntax(tpb, p);

        if (tpb.Name != null)
        {
            // First bound: print "where T :"
            p.Append("where");
            Visit(tpb.Name, p);
            VisitSpace(tpb.BeforeColon!, p);
            p.Append(':');
        }

        // Print the constraint type
        Visit(tpb.Bound, p);

        AfterSyntax(tpb, p);
        return tpb;
    }

    public override J VisitIsPattern(IsPattern ip, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ip, p);
        Visit(ip.Expression, p);
        VisitSpace(ip.Pattern.Before, p);
        p.Append("is");
        Visit(ip.Pattern.Element, p);
        AfterSyntax(ip, p);
        return ip;
    }

    public override J VisitStatementExpression(StatementExpression se, PrintOutputCapture<P> p)
    {
        BeforeSyntax(se, p);
        Visit(se.Statement, p);
        AfterSyntax(se, p);
        return se;
    }

    public override J VisitRelationalPattern(RelationalPattern rp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(rp, p);

        // Print the operator with its space before
        VisitSpace(rp.Operator.Before, p);
        var opString = rp.Operator.Element switch
        {
            RelationalPattern.Type.LessThan => "<",
            RelationalPattern.Type.LessThanOrEqual => "<=",
            RelationalPattern.Type.GreaterThan => ">",
            RelationalPattern.Type.GreaterThanOrEqual => ">=",
            _ => throw new InvalidOperationException($"Unknown relational operator: {rp.Operator.Element}")
        };
        p.Append(opString);

        // Print the value expression
        Visit(rp.Value, p);

        AfterSyntax(rp, p);
        return rp;
    }

    public override J VisitPropertyPattern(PropertyPattern pp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pp, p);

        // Print optional type qualifier (e.g., "string" in "string { Length: > 5 }")
        if (pp.TypeQualifier != null)
        {
            Visit(pp.TypeQualifier, p);
        }

        // Print subpatterns using JContainer: { subpattern, subpattern }
        VisitContainer("{", pp.Subpatterns, ",", "}", p);

        AfterSyntax(pp, p);
        return pp;
    }

    public override J VisitDeconstructionPattern(DeconstructionPattern dp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dp, p);

        // Print deconstructor type if present (not Empty)
        // For tuple patterns like (int x, int y), deconstructor is Empty
        if (dp.Deconstructor is not Empty)
        {
            Visit(dp.Deconstructor, p);
        }

        // Print nested patterns: ( pattern, pattern )
        // Need special handling for VariableDeclarations to avoid semicolons
        VisitSpace(dp.Nested.Before, p);
        p.Append('(');

        for (int i = 0; i < dp.Nested.Elements.Count; i++)
        {
            var element = dp.Nested.Elements[i];

            // Use VisitVariableDeclarationsWithoutSemicolon for type patterns like "int x"
            if (element.Element is VariableDeclarations vd)
            {
                VisitVariableDeclarationsWithoutSemicolon(vd, p);
            }
            else
            {
                Visit(element.Element, p);
            }

            // Print comma for non-last, or just the padding for last
            if (i < dp.Nested.Elements.Count - 1)
            {
                VisitSpace(element.After, p);
                p.Append(',');
            }
            else
            {
                VisitSpace(element.After, p);
            }
        }

        p.Append(')');

        AfterSyntax(dp, p);
        return dp;
    }

    public override J VisitPropertyDeclaration(PropertyDeclaration prop, PrintOutputCapture<P> p)
    {
        BeforeSyntax(prop, p);

        // Print modifiers
        foreach (var mod in prop.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod.Type));
        }

        // Print type
        Visit(prop.TypeExpression, p);

        // Print name
        Visit(prop.Name, p);

        // Print expression body or accessor block
        if (prop.ExpressionBody != null)
        {
            VisitSpace(prop.ExpressionBody.Before, p);
            p.Append("=>");
            Visit(prop.ExpressionBody.Element, p);
            p.Append(';');
        }
        else if (prop.Accessors != null)
        {
            VisitBlock(prop.Accessors, p);
        }

        AfterSyntax(prop, p);
        return prop;
    }

    public override J VisitAccessorDeclaration(AccessorDeclaration accessor, PrintOutputCapture<P> p)
    {
        BeforeSyntax(accessor, p);

        // Print modifiers
        foreach (var mod in accessor.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod.Type));
        }

        // Print keyword (get/set/init)
        VisitSpace(accessor.Kind.Before, p);
        p.Append(GetAccessorKindString(accessor.Kind.Element));

        // Print expression body, block body, or semicolon
        if (accessor.ExpressionBody != null)
        {
            VisitSpace(accessor.ExpressionBody.Before, p);
            p.Append("=>");
            Visit(accessor.ExpressionBody.Element, p);
            p.Append(';');
        }
        else if (accessor.Body != null)
        {
            VisitBlock(accessor.Body, p);
        }
        else
        {
            p.Append(';');
        }

        AfterSyntax(accessor, p);
        return accessor;
    }

    private static string GetAccessorKindString(AccessorKind kind)
    {
        return kind switch
        {
            AccessorKind.Get => "get",
            AccessorKind.Set => "set",
            AccessorKind.Init => "init",
            AccessorKind.Add => "add",
            AccessorKind.Remove => "remove",
            _ => throw new InvalidOperationException($"Unknown accessor kind: {kind}")
        };
    }

    public override J VisitAttributeList(AttributeList attrList, PrintOutputCapture<P> p)
    {
        BeforeSyntax(attrList, p);
        p.Append('[');

        // Print target if present
        if (attrList.Target != null)
        {
            Visit(attrList.Target.Element, p);
            VisitSpace(attrList.Target.After, p);
            p.Append(':');
        }

        // Print attributes
        for (int i = 0; i < attrList.Attributes.Count; i++)
        {
            var paddedAttr = attrList.Attributes[i];
            Visit(paddedAttr.Element, p);
            if (i < attrList.Attributes.Count - 1)
            {
                VisitSpace(paddedAttr.After, p);
                p.Append(',');
            }
        }

        p.Append(']');
        AfterSyntax(attrList, p);
        return attrList;
    }

    public override J VisitAnnotation(Annotation annotation, PrintOutputCapture<P> p)
    {
        BeforeSyntax(annotation, p);

        // Print the annotation type name
        Visit(annotation.AnnotationType, p);

        // Print arguments if present
        if (annotation.Arguments != null)
        {
            VisitArguments(annotation.Arguments, p);
        }

        AfterSyntax(annotation, p);
        return annotation;
    }

    public override J VisitBlock(Block block, PrintOutputCapture<P> p)
    {
        BeforeSyntax(block, p);
        p.Append('{');

        foreach (var stmt in block.Statements)
        {
            Visit(stmt.Element, p);
        }

        VisitSpace(block.End, p);
        p.Append('}');
        AfterSyntax(block, p);
        return block;
    }

    public override J VisitClassDeclaration(ClassDeclaration classDecl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(classDecl, p);

        // Print modifiers
        foreach (var mod in classDecl.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod.Type));
        }

        // Print class kind (class, struct, record, interface, etc.)
        VisitSpace(classDecl.ClassKind.Prefix, p);
        var isStruct = classDecl.Markers.FindFirst<Struct>() != null;
        var isRecordClass = classDecl.Markers.FindFirst<RecordClass>() != null;
        var isRecord = classDecl.ClassKind.Type == ClassDeclaration.KindType.Record;

        if (isRecord && isStruct)
        {
            // record struct
            p.Append("record struct");
        }
        else if (isRecord && isRecordClass)
        {
            // record class (explicit class keyword)
            p.Append("record class");
        }
        else if (isStruct)
        {
            // plain struct
            p.Append("struct");
        }
        else
        {
            p.Append(GetClassKindString(classDecl.ClassKind.Type));
        }

        // Print name
        VisitSpace(classDecl.Name.Prefix, p);
        p.Append(classDecl.Name.SimpleName);

        // Print type parameters (generics)
        if (classDecl.TypeParameters != null)
        {
            VisitSpace(classDecl.TypeParameters.Before, p);
            p.Append('<');
            for (var i = 0; i < classDecl.TypeParameters.Elements.Count; i++)
            {
                var typeParam = classDecl.TypeParameters.Elements[i];
                Visit(typeParam.Element.Name, p);
                if (i < classDecl.TypeParameters.Elements.Count - 1)
                {
                    VisitSpace(typeParam.After, p);
                    p.Append(',');
                }
                else
                {
                    VisitSpace(typeParam.After, p);
                }
            }
            p.Append('>');
        }

        // Print primary constructor (C# 12) if present
        // Find the synthesized MethodDeclaration marked with PrimaryConstructor
        if (classDecl.Markers.FindFirst<PrimaryConstructor>() != null)
        {
            foreach (var stmt in classDecl.Body.Statements)
            {
                if (stmt.Element is MethodDeclaration method &&
                    method.Markers.FindFirst<PrimaryConstructor>() != null)
                {
                    // Print the parameters
                    var paramsContainer = method.Parameters;
                    VisitSpace(paramsContainer.Before, p);
                    p.Append('(');
                    for (var i = 0; i < paramsContainer.Elements.Count; i++)
                    {
                        var param = paramsContainer.Elements[i];
                        // Use VisitVariableDeclarationsWithoutSemicolon for parameters
                        if (param.Element is VariableDeclarations vd)
                        {
                            VisitVariableDeclarationsWithoutSemicolon(vd, p);
                        }
                        else
                        {
                            Visit(param.Element, p);
                        }
                        if (i < paramsContainer.Elements.Count - 1)
                        {
                            VisitSpace(param.After, p);
                            p.Append(',');
                        }
                        else
                        {
                            VisitSpace(param.After, p);
                        }
                    }
                    p.Append(')');
                    break;
                }
            }
        }

        // Print base types (inheritance)
        // C# syntax: `class Foo : Base, IFoo, IBar`
        if (classDecl.Extends != null)
        {
            VisitSpace(classDecl.Extends.Before, p);
            p.Append(':');
            Visit(classDecl.Extends.Element, p);
        }

        if (classDecl.Implements != null)
        {
            // Container.Before = space before first comma
            VisitSpace(classDecl.Implements.Before, p);

            for (var i = 0; i < classDecl.Implements.Elements.Count; i++)
            {
                var impl = classDecl.Implements.Elements[i];
                p.Append(',');
                Visit(impl.Element, p);
                VisitSpace(impl.After, p);
            }
        }

        // Print type parameter constraints (C# uses 'where T : class' syntax)
        if (classDecl.TypeParameters != null)
        {
            foreach (var typeParam in classDecl.TypeParameters.Elements)
            {
                if (typeParam.Element.Bounds != null)
                {
                    for (var i = 0; i < typeParam.Element.Bounds.Elements.Count; i++)
                    {
                        var bound = typeParam.Element.Bounds.Elements[i];
                        Visit(bound.Element, p);
                        if (i < typeParam.Element.Bounds.Elements.Count - 1)
                        {
                            VisitSpace(bound.After, p);
                            p.Append(',');
                        }
                    }
                }
            }
        }

        // Print body (check for semicolon-terminated record)
        if (classDecl.Body.Markers.FindFirst<Semicolon>() != null)
        {
            // Positional record with semicolon: record Person(string Name);
            VisitSpace(classDecl.Body.Prefix, p);
            p.Append(';');
        }
        else
        {
            VisitBlock(classDecl.Body, p);
        }

        AfterSyntax(classDecl, p);
        return classDecl;
    }

    private static string GetClassKindString(ClassDeclaration.KindType type)
    {
        return type switch
        {
            ClassDeclaration.KindType.Class => "class",
            ClassDeclaration.KindType.Interface => "interface",
            ClassDeclaration.KindType.Record => "record",
            ClassDeclaration.KindType.Enum => "enum",
            ClassDeclaration.KindType.Annotation => "@interface",
            ClassDeclaration.KindType.Value => "value",
            _ => "class"
        };
    }

    public override J VisitEnumValueSet(EnumValueSet enumValueSet, PrintOutputCapture<P> p)
    {
        BeforeSyntax(enumValueSet, p);
        for (int i = 0; i < enumValueSet.Enums.Count; i++)
        {
            var paddedValue = enumValueSet.Enums[i];
            Visit(paddedValue.Element, p);
            if (i < enumValueSet.Enums.Count - 1)
            {
                VisitSpace(paddedValue.After, p);
                p.Append(',');
            }
        }
        AfterSyntax(enumValueSet, p);
        return enumValueSet;
    }

    public override J VisitEnumValue(EnumValue enumValue, PrintOutputCapture<P> p)
    {
        BeforeSyntax(enumValue, p);
        Visit(enumValue.Name, p);
        if (enumValue.Initializer != null)
        {
            VisitSpace(enumValue.Initializer.Before, p);
            p.Append('=');
            Visit(enumValue.Initializer.Element, p);
        }
        AfterSyntax(enumValue, p);
        return enumValue;
    }

    public override J VisitMethodDeclaration(MethodDeclaration method, PrintOutputCapture<P> p)
    {
        // Skip synthesized primary constructor methods (printed inline with class declaration)
        if (method.Markers.FindFirst<PrimaryConstructor>() != null)
        {
            return method;
        }

        BeforeSyntax(method, p);

        // Print modifiers
        foreach (var mod in method.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod.Type));
        }

        // Print return type
        if (method.ReturnTypeExpression != null)
        {
            Visit(method.ReturnTypeExpression, p);
        }

        // Print name
        VisitSpace(method.Name.Prefix, p);
        p.Append(method.Name.SimpleName);

        // Print parameters
        VisitSpace(method.Parameters.Before, p);
        p.Append('(');
        for (int i = 0; i < method.Parameters.Elements.Count; i++)
        {
            var paddedParam = method.Parameters.Elements[i];
            var param = paddedParam.Element;

            // Parameters are VariableDeclarations but without semicolon
            if (param is VariableDeclarations varDecl)
            {
                VisitVariableDeclarationsWithoutSemicolon(varDecl, p);
            }
            else
            {
                Visit(param, p);
            }

            if (i < method.Parameters.Elements.Count - 1)
            {
                VisitSpace(paddedParam.After, p);
                p.Append(',');
            }
        }
        p.Append(')');

        // Print body or semicolon (for interface methods without body)
        if (method.Markers.FindFirst<ExpressionBodied>() != null && method.Body != null)
        {
            // Expression-bodied: print => expr;
            VisitSpace(method.Body.Prefix, p);
            p.Append("=>");
            var returnStmt = (Return)method.Body.Statements[0].Element;
            Visit(returnStmt.Expression, p);
            p.Append(';');
        }
        else if (method.Body != null)
        {
            VisitBlock(method.Body, p);
        }
        else
        {
            p.Append(';');
        }

        AfterSyntax(method, p);
        return method;
    }

    public override J VisitReturn(Return ret, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ret, p);
        p.Append("return");
        if (ret.Expression != null)
        {
            Visit(ret.Expression, p);
        }
        p.Append(';');
        AfterSyntax(ret, p);
        return ret;
    }

    public override J VisitIf(If iff, PrintOutputCapture<P> p)
    {
        BeforeSyntax(iff, p);
        p.Append("if");
        VisitControlParentheses(iff.Condition, p);
        Visit(iff.ThenPart.Element, p);

        if (iff.ElsePart != null)
        {
            VisitSpace(iff.ElsePart.Prefix, p);
            p.Append("else");
            Visit(iff.ElsePart.Body.Element, p);
        }

        AfterSyntax(iff, p);
        return iff;
    }

    public override J VisitWhileLoop(WhileLoop whl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(whl, p);
        p.Append("while");
        VisitControlParentheses(whl.Condition, p);
        Visit(whl.Body.Element, p);
        AfterSyntax(whl, p);
        return whl;
    }

    public override J VisitDoWhileLoop(DoWhileLoop dwl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dwl, p);
        p.Append("do");
        Visit(dwl.Body.Element, p);
        VisitSpace(dwl.Condition.Before, p);
        p.Append("while");
        VisitControlParentheses(dwl.Condition.Element, p);
        p.Append(';');
        AfterSyntax(dwl, p);
        return dwl;
    }

    public override J VisitSynchronized(Synchronized sync, PrintOutputCapture<P> p)
    {
        BeforeSyntax(sync, p);
        p.Append("lock");  // C# uses "lock" instead of "synchronized"
        VisitControlParentheses(sync.Lock, p);
        Visit(sync.Body, p);
        AfterSyntax(sync, p);
        return sync;
    }

    public override J VisitForLoop(ForLoop fl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(fl, p);
        p.Append("for");
        VisitSpace(fl.LoopControl.Prefix, p);
        p.Append('(');

        // Print initializers
        for (int i = 0; i < fl.LoopControl.Init.Count; i++)
        {
            var paddedInit = fl.LoopControl.Init[i];
            if (paddedInit.Element is VariableDeclarations vd)
            {
                VisitVariableDeclarationsWithoutSemicolon(vd, p);
            }
            else
            {
                Visit(paddedInit.Element, p);
            }
            if (i < fl.LoopControl.Init.Count - 1)
            {
                VisitSpace(paddedInit.After, p);
                p.Append(',');
            }
        }

        p.Append(';');

        // Print condition
        if (fl.LoopControl.Condition.Element is not Empty)
        {
            Visit(fl.LoopControl.Condition.Element, p);
        }
        VisitSpace(fl.LoopControl.Condition.After, p);
        p.Append(';');

        // Print incrementors
        for (int i = 0; i < fl.LoopControl.Update.Count; i++)
        {
            var paddedUpdate = fl.LoopControl.Update[i];
            if (paddedUpdate.Element is Empty empty)
            {
                // Empty in update list just holds trailing space, don't print semicolon
                VisitSpace(empty.Prefix, p);
            }
            else if (paddedUpdate.Element is ExpressionStatement es)
            {
                Visit(es.Expression, p);
            }
            else
            {
                Visit(paddedUpdate.Element, p);
            }
            if (i < fl.LoopControl.Update.Count - 1)
            {
                VisitSpace(paddedUpdate.After, p);
                p.Append(',');
            }
        }

        p.Append(')');
        Visit(fl.Body.Element, p);
        AfterSyntax(fl, p);
        return fl;
    }

    public override J VisitForEachLoop(ForEachLoop fel, PrintOutputCapture<P> p)
    {
        BeforeSyntax(fel, p);
        p.Append("foreach");
        VisitSpace(fel.LoopControl.Prefix, p);
        p.Append('(');

        // Print variable
        VisitVariableDeclarationsWithoutSemicolon(fel.LoopControl.Variable.Element, p);
        VisitSpace(fel.LoopControl.Variable.After, p);
        p.Append("in");

        // Print iterable
        Visit(fel.LoopControl.Iterable.Element, p);
        VisitSpace(fel.LoopControl.Iterable.After, p);

        p.Append(')');
        Visit(fel.Body.Element, p);
        AfterSyntax(fel, p);
        return fel;
    }

    public override J VisitTry(Try tr, PrintOutputCapture<P> p)
    {
        BeforeSyntax(tr, p);
        p.Append("try");
        VisitBlock(tr.Body, p);

        foreach (var c in tr.Catches)
        {
            VisitSpace(c.Prefix, p);
            p.Append("catch");

            if (c.Parameter.Tree.Element.TypeExpression != null || c.Parameter.Tree.Element.Variables.Count > 0)
            {
                VisitSpace(c.Parameter.Prefix, p);
                p.Append('(');
                VisitVariableDeclarationsWithoutSemicolon(c.Parameter.Tree.Element, p);
                VisitSpace(c.Parameter.Tree.After, p);
                p.Append(')');
            }

            VisitBlock(c.Body, p);
        }

        if (tr.Finally != null)
        {
            VisitSpace(tr.Finally.Before, p);
            p.Append("finally");
            VisitBlock(tr.Finally.Element, p);
        }

        AfterSyntax(tr, p);
        return tr;
    }

    public override J VisitThrow(Throw thr, PrintOutputCapture<P> p)
    {
        BeforeSyntax(thr, p);
        p.Append("throw");
        Visit(thr.Exception, p);
        p.Append(';');
        AfterSyntax(thr, p);
        return thr;
    }

    public override J VisitBreak(Break brk, PrintOutputCapture<P> p)
    {
        BeforeSyntax(brk, p);
        p.Append("break");
        p.Append(';');
        AfterSyntax(brk, p);
        return brk;
    }

    public override J VisitContinue(Continue cont, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cont, p);
        p.Append("continue");
        p.Append(';');
        AfterSyntax(cont, p);
        return cont;
    }

    public override J VisitEmpty(Empty emp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(emp, p);
        p.Append(';');
        AfterSyntax(emp, p);
        return emp;
    }

    public override J VisitControlParentheses(ControlParentheses<Expression> cp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cp, p);
        p.Append('(');
        Visit(cp.Tree.Element, p);
        VisitSpace(cp.Tree.After, p);
        p.Append(')');
        AfterSyntax(cp, p);
        return cp;
    }

    public override J VisitLiteral(Literal literal, PrintOutputCapture<P> p)
    {
        BeforeSyntax(literal, p);
        p.Append(literal.ValueSource ?? literal.Value?.ToString());
        AfterSyntax(literal, p);
        return literal;
    }

    public override J VisitInterpolatedString(InterpolatedString istr, PrintOutputCapture<P> p)
    {
        BeforeSyntax(istr, p);

        // Print the opening delimiter
        p.Append(istr.Delimiter);

        // Print each part (literals and interpolations)
        foreach (var part in istr.Parts)
        {
            Visit(part, p);
        }

        // Print the closing delimiter (match the quote count from the opening)
        p.Append(GetClosingDelimiter(istr.Delimiter));

        AfterSyntax(istr, p);
        return istr;
    }

    public override J VisitInterpolation(Interpolation interp, PrintOutputCapture<P> p)
    {
        // Opening brace
        p.Append('{');

        // Space after opening brace
        VisitSpace(interp.Prefix, p);

        // Expression
        Visit(interp.Expression, p);

        // Optional alignment
        if (interp.Alignment != null)
        {
            VisitSpace(interp.Alignment.Before, p);
            p.Append(',');
            Visit(interp.Alignment.Element, p);
        }

        // Optional format
        if (interp.Format != null)
        {
            VisitSpace(interp.Format.Before, p);
            p.Append(':');
            Visit(interp.Format.Element, p);
        }

        // Space before closing brace
        VisitSpace(interp.After, p);

        // Closing brace
        p.Append('}');

        return interp;
    }

    private static string GetClosingDelimiter(string openDelimiter)
    {
        // Count the quote characters in the delimiter
        int quoteCount = 0;
        foreach (char c in openDelimiter)
        {
            if (c == '"') quoteCount++;
        }

        // For raw string literals, match the quote count
        return new string('"', quoteCount > 0 ? quoteCount : 1);
    }

    public override J VisitAwaitExpression(AwaitExpression ae, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ae, p);
        p.Append("await");
        Visit(ae.Expression, p);
        AfterSyntax(ae, p);
        return ae;
    }

    public override J VisitYieldStatement(YieldStatement yield, PrintOutputCapture<P> p)
    {
        BeforeSyntax(yield, p);
        p.Append("yield");
        VisitSpace(yield.ReturnOrBreakKeyword.Before, p);
        p.Append(yield.ReturnOrBreakKeyword.Element == YieldStatementKind.Return ? "return" : "break");
        if (yield.Value != null)
        {
            Visit(yield.Value, p);
        }
        p.Append(';');
        AfterSyntax(yield, p);
        return yield;
    }

    public override J VisitIdentifier(Identifier identifier, PrintOutputCapture<P> p)
    {
        BeforeSyntax(identifier, p);
        p.Append(identifier.SimpleName);
        AfterSyntax(identifier, p);
        return identifier;
    }

    public override J VisitBinary(Binary binary, PrintOutputCapture<P> p)
    {
        BeforeSyntax(binary, p);
        Visit(binary.Left, p);
        VisitSpace(binary.Operator.Before, p);

        var op = binary.Operator.Element;
        // In pattern context, And/Or print as keywords instead of &&/||
        if (IsInPatternContext() && op is Binary.OperatorType.And or Binary.OperatorType.Or)
        {
            p.Append(op == Binary.OperatorType.And ? "and" : "or");
        }
        else
        {
            p.Append(GetOperatorString(op));
        }

        Visit(binary.Right, p);
        AfterSyntax(binary, p);
        return binary;
    }

    private bool IsInPatternContext()
    {
        var c = Cursor;
        while (c != null)
        {
            if (c.Value is IsPattern)
                return true;
            c = c.Parent;
        }
        return false;
    }

    public override J VisitTernary(Ternary ternary, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ternary, p);
        Visit(ternary.Condition, p);

        // Check for NullCoalescing marker - prints as "a ?? b" instead of "a ? b : c"
        var isNullCoalescing = ternary.Markers.FindFirst<NullCoalescing>() != null;

        if (isNullCoalescing)
        {
            // Print as: condition ?? falsePart
            VisitSpace(ternary.FalsePart.Before, p);
            p.Append("??");
            Visit(ternary.FalsePart.Element, p);
        }
        else
        {
            // Print as: condition ? truePart : falsePart
            VisitSpace(ternary.TruePart.Before, p);
            p.Append('?');
            Visit(ternary.TruePart.Element, p);
            VisitSpace(ternary.FalsePart.Before, p);
            p.Append(':');
            Visit(ternary.FalsePart.Element, p);
        }

        AfterSyntax(ternary, p);
        return ternary;
    }

    public override J VisitSwitch(Switch @switch, PrintOutputCapture<P> p)
    {
        BeforeSyntax(@switch, p);
        p.Append("switch");

        // Print selector (expression in parentheses)
        VisitControlParentheses(@switch.Selector, p);

        // Print the cases block
        VisitBlock(@switch.Cases, p);

        AfterSyntax(@switch, p);
        return @switch;
    }

    public override J VisitCase(Case @case, PrintOutputCapture<P> p)
    {
        // Check if we're inside a switch expression (no 'case' keyword needed)
        var inSwitchExpression = Cursor.FirstEnclosing<SwitchExpression>() != null;

        // The Case prefix contains whitespace before the 'case'/'default' keyword (or pattern for switch expr)
        BeforeSyntax(@case, p);

        // Each Case has exactly one label in C# (unlike Java's comma-separated multi-value cases)
        if (@case.CaseLabels.Elements.Count > 0)
        {
            var labelPadded = @case.CaseLabels.Elements[0];
            var label = labelPadded.Element;

            if (inSwitchExpression)
            {
                // Switch expression arm: pattern => result (no 'case' keyword)
                // Type patterns (like 'int i') are stored as VariableDeclarations
                if (label is VariableDeclarations vd)
                {
                    VisitVariableDeclarationsWithoutSemicolon(vd, p);
                }
                else
                {
                    Visit(label, p);
                }

                // Space before 'when' or '=>' is in label's After
                VisitSpace(labelPadded.After, p);

                // Print guard if present
                if (@case.Guard != null)
                {
                    p.Append("when");
                    Visit(@case.Guard, p);  // Guard prefix has space after 'when'
                }

                // Print '=>' and result
                // Body.After stores space before '=>' (when there's a guard)
                if (@case.Body != null)
                {
                    VisitSpace(@case.Body.After, p);
                    p.Append("=>");
                    Visit(@case.Body.Element, p);
                }
            }
            else
            {
                // Switch statement case: case pattern: (with 'case' keyword)
                // Check if this is a 'default' label
                if (label is Identifier id && id.SimpleName == "default")
                {
                    p.Append("default");
                }
                else
                {
                    p.Append("case");
                    // Type patterns (like 'case int i:') are stored as VariableDeclarations
                    // but should NOT have a semicolon in case labels
                    if (label is VariableDeclarations vd)
                    {
                        VisitVariableDeclarationsWithoutSemicolon(vd, p);
                    }
                    else
                    {
                        Visit(label, p);
                    }
                }

                // Print colon for Statement type (After space comes before 'when' or colon)
                if (@case.CaseKind == CaseType.Statement)
                {
                    VisitSpace(labelPadded.After, p);

                    // Print guard if present
                    if (@case.Guard != null)
                    {
                        p.Append("when");
                        Visit(@case.Guard, p);  // Guard prefix has space after 'when'
                    }

                    p.Append(':');
                }
            }
        }

        // Print statements for Statement type (switch statement only)
        if (!inSwitchExpression && @case.CaseKind == CaseType.Statement)
        {
            foreach (var stmtPadded in @case.Statements.Elements)
            {
                Visit(stmtPadded.Element, p);
            }
        }

        // Print body for Rule type (switch statement with arrow - Java-style, not typically used in C#)
        if (!inSwitchExpression && @case.CaseKind == CaseType.Rule && @case.Body != null)
        {
            p.Append("=>");
            Visit(@case.Body.Element, p);
        }

        AfterSyntax(@case, p);
        return @case;
    }

    public override J VisitSwitchExpression(SwitchExpression se, PrintOutputCapture<P> p)
    {
        BeforeSyntax(se, p);

        // Print selector expression (no parentheses for C#)
        Visit(se.Selector.Tree.Element, p);

        // Space before 'switch' keyword
        VisitSpace(se.Selector.Tree.After, p);
        p.Append("switch");

        // Space before '{'
        VisitSpace(se.Cases.Prefix, p);
        p.Append('{');

        // Print each arm (Case elements - VisitCase checks context to skip "case" keyword)
        for (int i = 0; i < se.Cases.Statements.Count; i++)
        {
            var armPadded = se.Cases.Statements[i];
            Visit(armPadded.Element, p);

            // Print comma separator (except for last arm)
            if (i < se.Cases.Statements.Count - 1)
            {
                VisitSpace(armPadded.After, p);
                p.Append(',');
            }
        }

        // Space before '}'
        VisitSpace(se.Cases.End, p);
        p.Append('}');

        AfterSyntax(se, p);
        return se;
    }

    public override J VisitLambda(Lambda lambda, PrintOutputCapture<P> p)
    {
        BeforeSyntax(lambda, p);

        // Print parameters
        var @params = lambda.Params;

        if (@params.Parenthesized)
        {
            p.Append('(');
            // Print space inside parens (Prefix for parenthesized params is space after '(')
            VisitSpace(@params.Prefix, p);
            for (int i = 0; i < @params.Elements.Count; i++)
            {
                var paddedParam = @params.Elements[i];
                // Use VisitVariableDeclarationsWithoutSemicolon for typed parameters
                if (paddedParam.Element is VariableDeclarations vd)
                {
                    VisitVariableDeclarationsWithoutSemicolon(vd, p);
                }
                else
                {
                    Visit(paddedParam.Element, p);
                }
                if (i < @params.Elements.Count - 1)
                {
                    VisitSpace(paddedParam.After, p);
                    p.Append(',');
                }
                else
                {
                    VisitSpace(paddedParam.After, p);
                }
            }
            p.Append(')');
        }
        else
        {
            // Single unparenthesized parameter
            VisitSpace(@params.Prefix, p);
            foreach (var paddedParam in @params.Elements)
            {
                // Use VisitVariableDeclarationsWithoutSemicolon for typed parameters
                if (paddedParam.Element is VariableDeclarations vd)
                {
                    VisitVariableDeclarationsWithoutSemicolon(vd, p);
                }
                else
                {
                    Visit(paddedParam.Element, p);
                }
            }
        }

        // Print arrow
        VisitSpace(lambda.Arrow, p);
        p.Append("=>");

        // Print body
        Visit(lambda.Body, p);

        AfterSyntax(lambda, p);
        return lambda;
    }

    public override J VisitCsLambda(CsLambda csLambda, PrintOutputCapture<P> p)
    {
        BeforeSyntax(csLambda, p);

        // Print modifiers (async, static)
        foreach (var mod in csLambda.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod.Type));
        }

        // Print optional return type
        if (csLambda.ReturnType != null)
        {
            Visit(csLambda.ReturnType, p);
        }

        // Delegate to J.Lambda printing
        VisitLambda(csLambda.LambdaExpression, p);

        AfterSyntax(csLambda, p);
        return csLambda;
    }

    public override J VisitAssignment(Assignment assignment, PrintOutputCapture<P> p)
    {
        BeforeSyntax(assignment, p);
        Visit(assignment.Variable, p);
        VisitSpace(assignment.AssignmentValue.Before, p);
        p.Append('=');
        Visit(assignment.AssignmentValue.Element, p);
        AfterSyntax(assignment, p);
        return assignment;
    }

    public override J VisitAssignmentOperation(AssignmentOperation assignment, PrintOutputCapture<P> p)
    {
        BeforeSyntax(assignment, p);
        Visit(assignment.Variable, p);
        VisitSpace(assignment.Operator.Before, p);
        p.Append(GetAssignmentOperatorString(assignment.Operator.Element));
        Visit(assignment.AssignmentValue, p);
        AfterSyntax(assignment, p);
        return assignment;
    }

    private static string GetAssignmentOperatorString(AssignmentOperation.OperatorType op)
    {
        return op switch
        {
            AssignmentOperation.OperatorType.Addition => "+=",
            AssignmentOperation.OperatorType.Subtraction => "-=",
            AssignmentOperation.OperatorType.Multiplication => "*=",
            AssignmentOperation.OperatorType.Division => "/=",
            AssignmentOperation.OperatorType.Modulo => "%=",
            AssignmentOperation.OperatorType.BitAnd => "&=",
            AssignmentOperation.OperatorType.BitOr => "|=",
            AssignmentOperation.OperatorType.BitXor => "^=",
            AssignmentOperation.OperatorType.LeftShift => "<<=",
            AssignmentOperation.OperatorType.RightShift => ">>=",
            AssignmentOperation.OperatorType.UnsignedRightShift => ">>>=",
            AssignmentOperation.OperatorType.Coalesce => "??=",
            _ => throw new InvalidOperationException($"Unknown assignment operator: {op}")
        };
    }

    public override J VisitUnary(Unary unary, PrintOutputCapture<P> p)
    {
        BeforeSyntax(unary, p);

        var op = unary.Operator.Element;

        // In pattern context, Not prints as 'not' keyword instead of '!'
        if (op == Unary.OperatorType.Not && IsInPatternContext())
        {
            VisitSpace(unary.Operator.Before, p);
            p.Append("not");
            Visit(unary.Expression, p);
            AfterSyntax(unary, p);
            return unary;
        }

        bool isPostfix = op == Unary.OperatorType.PostIncrement || op == Unary.OperatorType.PostDecrement;

        if (!isPostfix)
        {
            p.Append(GetUnaryOperatorString(op));
        }

        Visit(unary.Expression, p);

        if (isPostfix)
        {
            VisitSpace(unary.Operator.Before, p);
            p.Append(GetUnaryOperatorString(op));
        }

        AfterSyntax(unary, p);
        return unary;
    }

    private static string GetUnaryOperatorString(Unary.OperatorType op)
    {
        return op switch
        {
            Unary.OperatorType.PreIncrement => "++",
            Unary.OperatorType.PreDecrement => "--",
            Unary.OperatorType.PostIncrement => "++",
            Unary.OperatorType.PostDecrement => "--",
            Unary.OperatorType.Positive => "+",
            Unary.OperatorType.Negative => "-",
            Unary.OperatorType.Complement => "~",
            Unary.OperatorType.Not => "!",
            _ => throw new InvalidOperationException($"Unknown unary operator: {op}")
        };
    }

    public override J VisitParentheses(Parentheses<Expression> parens, PrintOutputCapture<P> p)
    {
        BeforeSyntax(parens, p);
        p.Append('(');
        Visit(parens.Tree.Element, p);
        VisitSpace(parens.Tree.After, p);
        p.Append(')');
        AfterSyntax(parens, p);
        return parens;
    }

    public override J VisitTypeCast(TypeCast cast, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cast, p);
        // Print (type) part
        VisitSpace(cast.Clazz.Prefix, p);
        p.Append('(');
        Visit(cast.Clazz.Tree.Element, p);
        VisitSpace(cast.Clazz.Tree.After, p);
        p.Append(')');
        // Print expression
        Visit(cast.Expression, p);
        AfterSyntax(cast, p);
        return cast;
    }

    private static string GetOperatorString(Binary.OperatorType op)
    {
        return op switch
        {
            Binary.OperatorType.Addition => "+",
            Binary.OperatorType.Subtraction => "-",
            Binary.OperatorType.Multiplication => "*",
            Binary.OperatorType.Division => "/",
            Binary.OperatorType.Modulo => "%",
            Binary.OperatorType.LessThan => "<",
            Binary.OperatorType.GreaterThan => ">",
            Binary.OperatorType.LessThanOrEqual => "<=",
            Binary.OperatorType.GreaterThanOrEqual => ">=",
            Binary.OperatorType.Equal => "==",
            Binary.OperatorType.NotEqual => "!=",
            Binary.OperatorType.BitAnd => "&",
            Binary.OperatorType.BitOr => "|",
            Binary.OperatorType.BitXor => "^",
            Binary.OperatorType.LeftShift => "<<",
            Binary.OperatorType.RightShift => ">>",
            Binary.OperatorType.Or => "||",
            Binary.OperatorType.And => "&&",
            _ => throw new InvalidOperationException($"Unknown operator: {op}")
        };
    }

    public override J VisitExpressionStatement(ExpressionStatement expressionStatement, PrintOutputCapture<P> p)
    {
        // Note: ExpressionStatement delegates Prefix/Markers to its Expression,
        // so we don't call BeforeSyntax here - the expression handles its own prefix
        Visit(expressionStatement.Expression, p);
        p.Append(';');
        // AfterSyntax would use the same delegated markers, so skip it too
        return expressionStatement;
    }

    public override J VisitVariableDeclarations(VariableDeclarations varDecl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(varDecl, p);
        VisitVariableDeclarationsContent(varDecl, p);
        if (!IsInPatternContext())
            p.Append(';');
        AfterSyntax(varDecl, p);
        return varDecl;
    }

    private void VisitVariableDeclarationsWithoutSemicolon(VariableDeclarations varDecl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(varDecl, p);
        VisitVariableDeclarationsContent(varDecl, p);
        AfterSyntax(varDecl, p);
    }

    private void VisitVariableDeclarationsContent(VariableDeclarations varDecl, PrintOutputCapture<P> p)
    {
        // Print modifiers
        foreach (var mod in varDecl.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod.Type));
        }

        // Print type
        if (varDecl.TypeExpression != null)
        {
            Visit(varDecl.TypeExpression, p);
        }

        // Print variables
        for (int i = 0; i < varDecl.Variables.Count; i++)
        {
            var paddedVar = varDecl.Variables[i];
            var namedVar = paddedVar.Element;

            VisitSpace(namedVar.Prefix, p);
            VisitSpace(namedVar.Name.Prefix, p);
            p.Append(namedVar.Name.SimpleName);

            if (namedVar.Initializer != null)
            {
                VisitSpace(namedVar.Initializer.Before, p);
                p.Append('=');
                Visit(namedVar.Initializer.Element, p);
            }

            if (i < varDecl.Variables.Count - 1)
            {
                VisitSpace(paddedVar.After, p);
                p.Append(',');
            }
        }
    }

    public override J VisitPrimitive(Primitive primitive, PrintOutputCapture<P> p)
    {
        BeforeSyntax(primitive, p);
        p.Append(GetPrimitiveString(primitive.Kind));
        AfterSyntax(primitive, p);
        return primitive;
    }

    private static string GetPrimitiveString(JavaType.PrimitiveKind kind)
    {
        return kind switch
        {
            JavaType.PrimitiveKind.Int => "int",
            JavaType.PrimitiveKind.Long => "long",
            JavaType.PrimitiveKind.Short => "short",
            JavaType.PrimitiveKind.Byte => "byte",
            JavaType.PrimitiveKind.Float => "float",
            JavaType.PrimitiveKind.Double => "double",
            JavaType.PrimitiveKind.Boolean => "bool",
            JavaType.PrimitiveKind.Char => "char",
            JavaType.PrimitiveKind.String => "string",
            JavaType.PrimitiveKind.Void => "void",
            _ => ""
        };
    }

    private static string GetModifierString(Modifier.ModifierType type)
    {
        return type switch
        {
            Modifier.ModifierType.Public => "public",
            Modifier.ModifierType.Private => "private",
            Modifier.ModifierType.Protected => "protected",
            Modifier.ModifierType.Internal => "internal",
            Modifier.ModifierType.Static => "static",
            Modifier.ModifierType.Abstract => "abstract",
            Modifier.ModifierType.Sealed => "sealed",
            Modifier.ModifierType.Virtual => "virtual",
            Modifier.ModifierType.Override => "override",
            Modifier.ModifierType.Readonly => "readonly",
            Modifier.ModifierType.Const => "const",
            Modifier.ModifierType.New => "new",
            Modifier.ModifierType.Extern => "extern",
            Modifier.ModifierType.Unsafe => "unsafe",
            Modifier.ModifierType.Partial => "partial",
            Modifier.ModifierType.Async => "async",
            Modifier.ModifierType.Volatile => "volatile",
            Modifier.ModifierType.Ref => "ref",
            Modifier.ModifierType.Out => "out",
            Modifier.ModifierType.In => "in",
            _ => ""
        };
    }

    protected void VisitSpace(Space space, PrintOutputCapture<P> p)
    {
        foreach (var comment in space.Comments)
        {
            if (comment.Multiline)
            {
                p.Append("/*").Append(comment.Text).Append("*/");
            }
            else
            {
                p.Append("//").Append(comment.Text);
            }
            p.Append(comment.Suffix);
        }
        p.Append(space.Whitespace);
    }

    #region Preprocessor Directives

    public override J VisitConditionalBlock(ConditionalBlock conditionalBlock, PrintOutputCapture<P> p)
    {
        BeforeSyntax(conditionalBlock, p);
        Visit(conditionalBlock.IfBranch, p);
        foreach (var elif in conditionalBlock.ElifBranches)
        {
            Visit(elif, p);
        }
        if (conditionalBlock.ElseBranch != null)
        {
            Visit(conditionalBlock.ElseBranch, p);
        }
        VisitSpace(conditionalBlock.BeforeEndif, p);
        p.Append("#endif");
        AfterSyntax(conditionalBlock, p);
        return conditionalBlock;
    }

    public override J VisitIfDirective(IfDirective ifDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ifDirective, p);
        p.Append("#if");
        Visit(ifDirective.Condition, p);
        foreach (var stmt in ifDirective.Body)
        {
            Visit(stmt.Element, p);
            VisitSpace(stmt.After, p);
        }
        AfterSyntax(ifDirective, p);
        return ifDirective;
    }

    public override J VisitElifDirective(ElifDirective elifDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(elifDirective, p);
        p.Append("#elif");
        Visit(elifDirective.Condition, p);
        foreach (var stmt in elifDirective.Body)
        {
            Visit(stmt.Element, p);
            VisitSpace(stmt.After, p);
        }
        AfterSyntax(elifDirective, p);
        return elifDirective;
    }

    public override J VisitElseDirective(ElseDirective elseDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(elseDirective, p);
        p.Append("#else");
        foreach (var stmt in elseDirective.Body)
        {
            Visit(stmt.Element, p);
            VisitSpace(stmt.After, p);
        }
        AfterSyntax(elseDirective, p);
        return elseDirective;
    }

    public override J VisitPragmaWarningDirective(PragmaWarningDirective pragmaWarningDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pragmaWarningDirective, p);
        p.Append("#pragma warning");
        p.Append(pragmaWarningDirective.Action == PragmaWarningAction.Disable ? " disable" : " restore");
        VisitRightPadded(pragmaWarningDirective.WarningCodes, ",", p);
        AfterSyntax(pragmaWarningDirective, p);
        return pragmaWarningDirective;
    }

    public override J VisitNullableDirective(NullableDirective nullableDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(nullableDirective, p);
        p.Append("#nullable");
        p.Append(' ');
        p.Append(nullableDirective.Setting.ToString().ToLower());
        if (nullableDirective.Target != null)
        {
            p.Append(' ');
            p.Append(nullableDirective.Target.Value.ToString().ToLower());
        }
        AfterSyntax(nullableDirective, p);
        return nullableDirective;
    }

    public override J VisitRegionDirective(RegionDirective regionDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(regionDirective, p);
        p.Append("#region");
        if (regionDirective.Name != null)
        {
            p.Append(' ');
            p.Append(regionDirective.Name);
        }
        AfterSyntax(regionDirective, p);
        return regionDirective;
    }

    public override J VisitEndRegionDirective(EndRegionDirective endRegionDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(endRegionDirective, p);
        p.Append("#endregion");
        AfterSyntax(endRegionDirective, p);
        return endRegionDirective;
    }

    public override J VisitDefineDirective(DefineDirective defineDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(defineDirective, p);
        p.Append("#define");
        Visit(defineDirective.Symbol, p);
        AfterSyntax(defineDirective, p);
        return defineDirective;
    }

    public override J VisitUndefDirective(UndefDirective undefDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(undefDirective, p);
        p.Append("#undef");
        Visit(undefDirective.Symbol, p);
        AfterSyntax(undefDirective, p);
        return undefDirective;
    }

    public override J VisitErrorDirective(ErrorDirective errorDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(errorDirective, p);
        p.Append("#error");
        p.Append(' ');
        p.Append(errorDirective.Message);
        AfterSyntax(errorDirective, p);
        return errorDirective;
    }

    public override J VisitWarningDirective(WarningDirective warningDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(warningDirective, p);
        p.Append("#warning");
        p.Append(' ');
        p.Append(warningDirective.Message);
        AfterSyntax(warningDirective, p);
        return warningDirective;
    }

    public override J VisitLineDirective(LineDirective lineDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(lineDirective, p);
        p.Append("#line");
        switch (lineDirective.Kind)
        {
            case LineKind.Hidden:
                p.Append(" hidden");
                break;
            case LineKind.Default:
                p.Append(" default");
                break;
            case LineKind.Numeric:
                Visit(lineDirective.Line, p);
                break;
        }
        if (lineDirective.File != null)
        {
            Visit(lineDirective.File, p);
        }
        AfterSyntax(lineDirective, p);
        return lineDirective;
    }

    #endregion

    #region BeforeSyntax/AfterSyntax Pattern

    /// <summary>
    /// Called at the start of each visit method. Handles prefix space and markers.
    /// </summary>
    protected void BeforeSyntax(J j, PrintOutputCapture<P> p)
    {
        BeforeSyntax(j.Prefix, j.Markers, p);
    }

    /// <summary>
    /// Called at the start of each visit method. Handles prefix space and markers.
    /// </summary>
    protected void BeforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p)
    {
        VisitSpace(prefix, p);
        VisitMarkers(markers, p);
    }

    /// <summary>
    /// Called at the end of each visit method. Handles markers after syntax.
    /// </summary>
    protected void AfterSyntax(J j, PrintOutputCapture<P> p)
    {
        AfterSyntax(j.Markers, p);
    }

    /// <summary>
    /// Called at the end of each visit method. Handles markers after syntax.
    /// </summary>
    protected void AfterSyntax(Markers markers, PrintOutputCapture<P> p)
    {
        // No-op for now. C# markers (NullSafe, DelegateInvocation, etc.) are semantic
        // markers that affect how OTHER elements are printed, rather than markers that
        // need to be printed themselves like JS's Semicolon/TrailingComma.
    }

    /// <summary>
    /// Visits all markers on an element.
    /// C# markers are semantic (affect other element printing) rather than syntactic
    /// (needing their own printed representation). Individual visit methods check for
    /// markers like NullSafe/DelegateInvocation and adjust their output accordingly.
    /// </summary>
    protected void VisitMarkers(Markers markers, PrintOutputCapture<P> p)
    {
        // C# markers are currently all semantic - they affect how other elements print
        // rather than being printed themselves. See NullSafe, DelegateInvocation, etc.
    }

    #endregion

    #region Padded Element Helpers

    /// <summary>
    /// Visits an argument list container with proper Empty element handling.
    /// Empty elements (from empty argument lists) just contribute their After space.
    /// </summary>
    protected void VisitArguments(JContainer<Expression> arguments, PrintOutputCapture<P> p)
    {
        VisitSpace(arguments.Before, p);
        p.Append('(');
        for (int i = 0; i < arguments.Elements.Count; i++)
        {
            var paddedArg = arguments.Elements[i];

            // Skip Empty elements but still print their trailing space
            if (paddedArg.Element is not Empty)
            {
                Visit(paddedArg.Element, p);
                VisitSpace(paddedArg.After, p);
                if (i < arguments.Elements.Count - 1)
                {
                    p.Append(',');
                }
            }
            else
            {
                // Empty element just holds trailing space
                VisitSpace(paddedArg.After, p);
            }
        }
        p.Append(')');
    }

    /// <summary>
    /// Visits a list of right-padded elements with a suffix between them.
    /// </summary>
    protected void VisitRightPadded<T>(IList<JRightPadded<T>> nodes, string suffixBetween, PrintOutputCapture<P> p) where T : J
    {
        for (int i = 0; i < nodes.Count; i++)
        {
            var node = nodes[i];
            Visit(node.Element, p);
            VisitSpace(node.After, p);
            if (i < nodes.Count - 1)
            {
                p.Append(suffixBetween);
            }
        }
    }

    /// <summary>
    /// Visits a single right-padded element with an optional suffix.
    /// </summary>
    protected void VisitRightPadded<T>(JRightPadded<T>? rightPadded, string? suffix, PrintOutputCapture<P> p) where T : J
    {
        if (rightPadded != null)
        {
            Visit(rightPadded.Element, p);
            VisitSpace(rightPadded.After, p);
            if (suffix != null)
            {
                p.Append(suffix);
            }
        }
    }

    /// <summary>
    /// Visits a left-padded element with an optional prefix keyword.
    /// </summary>
    protected void VisitLeftPadded<T>(string? prefix, JLeftPadded<T>? leftPadded, PrintOutputCapture<P> p) where T : J
    {
        if (leftPadded != null)
        {
            VisitSpace(leftPadded.Before, p);
            if (prefix != null)
            {
                p.Append(prefix);
            }
            Visit(leftPadded.Element, p);
        }
    }

    /// <summary>
    /// Visits a container with before/after delimiters and suffix between elements.
    /// </summary>
    protected void VisitContainer<T>(string before, JContainer<T>? container, string suffixBetween, string? after, PrintOutputCapture<P> p) where T : J
    {
        if (container == null)
        {
            return;
        }
        VisitSpace(container.Before, p);
        p.Append(before);
        VisitRightPadded(container.Elements, suffixBetween, p);
        p.Append(after ?? "");
    }

    #endregion
}
