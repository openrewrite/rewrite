using System.Text.RegularExpressions;
using OpenRewrite.Core;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Prints C# LST back to source code.
/// Extends CSharpVisitor for cursor tracking, enabling context-aware printing
/// (e.g., distinguishing pattern operators from boolean operators).
/// </summary>
public class CSharpPrinter<P> : CSharpVisitor<PrintOutputCapture<P>>
{
    private int _patternDepth;
    public string Print(Tree tree)
    {
        var capture = new PrintOutputCapture<P>(default!);
        Visit(tree, capture);
        return capture.ToString();
    }

    /// <summary>
    /// Visits a statement within a JRightPadded context, printing the statement,
    /// the After space, and the statement terminator (semicolon if applicable).
    /// </summary>
    protected void VisitStatement(JRightPadded<Statement> paddedStat, PrintOutputCapture<P> p)
    {
        Visit(paddedStat.Element, p);
        VisitSpace(paddedStat.After, p);
        PrintStatementTerminator(paddedStat.Element, p);
    }

    /// <summary>
    /// Prints a semicolon for statement types that require one.
    /// Matches Java's printStatementTerminator pattern.
    /// </summary>
    protected void PrintStatementTerminator(Statement statement, PrintOutputCapture<P> p)
    {
        switch (statement)
        {
            // Statements that always end with ';'
            case ExpressionStatement:
            case Return:
            case VariableDeclarations:
            case Empty:
            case Throw:
            case Break:
            case Continue:
            case UsingDirective:
            case ExternAlias:
            case GotoStatement:
            case DelegateDeclaration:
            case Yield:
                p.Append(';');
                break;

            // DoWhileLoop ends with ';' after while(condition)
            case DoWhileLoop:
                p.Append(';');
                break;

            // Package (file-scoped namespace) ends with ';'
            case Package:
                p.Append(';');
                break;

            // MethodDeclaration: check for Semicolon marker on body (abstract/extern)
            case MethodDeclaration md when md.Body is { } body && body.Markers.FindFirst<Semicolon>() != null:
                break; // semicolon already printed by VisitMethodDeclaration's body handling

            // ClassDeclaration: check for Semicolon marker on body (record X;)
            case ClassDeclaration cd when cd.Body.Markers.FindFirst<Semicolon>() != null:
                break; // semicolon already printed by VisitClassDeclaration

            // FieldAccess used as statement (like event accessor declarations)
            case FieldAccess:
                break;

            default:
                // Most other statements (If, While, For, ForEach, Try, Switch, Block, etc.)
                // do not have statement terminators
                break;
        }
    }

    public override J VisitCompilationUnit(CompilationUnit compilationUnit, PrintOutputCapture<P> p)
    {
        BeforeSyntax(compilationUnit, p);

        foreach (var member in compilationUnit.Members)
        {
            Visit(member, p);
            PrintStatementTerminator(member, p);
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

        if (usingDirective.Alias != null)
        {
            Visit(usingDirective.Alias.Element, p);
            VisitSpace(usingDirective.Alias.After, p);
            p.Append('=');
        }

        Visit(usingDirective.NamespaceOrType, p);
        // Semicolon is printed by PrintStatementTerminator via VisitStatement

        AfterSyntax(usingDirective, p);
        return usingDirective;
    }

    public override J VisitPackage(Package pkg, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pkg, p);
        p.Append("namespace");
        Visit(pkg.Expression.Element, p);
        VisitSpace(pkg.Expression.After, p);
        // Semicolon printed by PrintStatementTerminator
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
            VisitStatement(member, p);
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

        // Check for PointerMemberAccess marker on target PointerDereference - if present, print -> instead of .
        var isPointerMemberAccess = fieldAccess.Target is PointerDereference pd
            && pd.Markers.FindFirst<PointerMemberAccess>() != null;
        // Check for NullSafe marker on the name - if present, print ?. instead of .
        var nullSafe = fieldAccess.Name.Element.Markers.FindFirst<NullSafe>();
        if (isPointerMemberAccess)
        {
            p.Append("->");
        }
        else if (nullSafe != null)
        {
            p.Append('?');
            VisitSpace(nullSafe.DotPrefix, p);
            p.Append('.');
        }
        else
        {
            p.Append('.');
        }

        Visit(fieldAccess.Name.Element, p);

        AfterSyntax(fieldAccess, p);
        return fieldAccess;
    }

    public override J VisitMemberReference(MemberReference memberRef, PrintOutputCapture<P> p)
    {
        BeforeSyntax(memberRef, p);
        Visit(memberRef.Containing.Element, p);
        VisitSpace(memberRef.Containing.After, p);
        p.Append('.');
        Visit(memberRef.Reference.Element, p);
        if (memberRef.TypeParameters != null)
        {
            PrintTypeArguments(memberRef.TypeParameters, p);
        }
        AfterSyntax(memberRef, p);
        return memberRef;
    }

    public override J VisitNullableType(NullableType nullableType, PrintOutputCapture<P> p)
    {
        BeforeSyntax(nullableType, p);
        VisitRightPadded(nullableType.TypeTreePadded, null, p);
        p.Append('?');
        AfterSyntax(nullableType, p);
        return nullableType;
    }

    public override J VisitParameterizedType(ParameterizedType type, PrintOutputCapture<P> p)
    {
        BeforeSyntax(type, p);
        Visit(type.Clazz, p);
        if (type.TypeParameters != null)
        {
            VisitContainer("<", type.TypeParameters, ",", ">", p);
        }
        AfterSyntax(type, p);
        return type;
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
                // Check for PointerMemberAccess marker on select PointerDereference - if present, print -> instead of .
                var nullSafe = mi.Name.Markers.FindFirst<NullSafe>();
                var isPointerDeref = mi.Select.Element is PointerDereference selectPd
                    && selectPd.Markers.FindFirst<PointerMemberAccess>() != null;
                if (isPointerDeref)
                {
                    p.Append("->");
                }
                else if (nullSafe != null)
                {
                    p.Append('?');
                    VisitSpace(nullSafe.DotPrefix, p);
                    p.Append('.');
                }
                else
                {
                    p.Append('.');
                }
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
            PrintTypeArguments(mi.TypeParameters, p);
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
            // Check if body wraps an InitializerExpression (C# object/collection initializer)
            if (nc.Body.Statements.Count == 1 &&
                nc.Body.Statements[0].Element is ExpressionStatement es &&
                es.Expression is InitializerExpression initExpr)
            {
                VisitSpace(nc.Body.Prefix, p);
                Visit(initExpr, p);
            }
            else
            {
                VisitBlock(nc.Body, p);
            }
        }

        AfterSyntax(nc, p);
        return nc;
    }

    public override J VisitNewArray(NewArray na, PrintOutputCapture<P> p)
    {
        BeforeSyntax(na, p);

        // Don't print "new" when inside a stackalloc expression
        if (Cursor.GetParentTreeCursor().Value is not StackAllocExpression)
        {
            p.Append("new");
        }

        // Print type expression if present
        if (na.TypeExpression != null)
        {
            Visit(na.TypeExpression, p);
        }

        // Print dimensions: [size], [], [,], etc.
        for (int i = 0; i < na.Dimensions.Count; i++)
        {
            var dim = na.Dimensions[i];
            bool isContinuation = dim.Markers.FindFirst<MultiDimensionContinuation>() != null;

            if (isContinuation)
            {
                // Within the same rank specifier — emit comma separator
                p.Append(',');
                VisitSpace(dim.Prefix, p);
            }
            else
            {
                // Start of a new rank specifier
                VisitSpace(dim.Prefix, p);
                p.Append('[');
            }

            // Print the index/size if not empty
            if (dim.Index.Element is not Empty)
            {
                Visit(dim.Index.Element, p);
            }

            VisitSpace(dim.Index.After, p);

            // Close bracket if next dim is not a continuation in the same rank specifier
            bool nextIsContinuation = i + 1 < na.Dimensions.Count &&
                na.Dimensions[i + 1].Markers.FindFirst<MultiDimensionContinuation>() != null;
            if (!nextIsContinuation)
            {
                p.Append(']');
            }
        }

        // Print initializer if present: { 1, 2, 3 }
        if (na.Initializer != null)
        {
            VisitSpace(na.Initializer.Before, p);
            p.Append('{');

            var elements = na.Initializer.Elements;
            if (elements.Count == 1 && elements[0].Element is Empty)
            {
                // Sentinel Empty — print its After space to preserve "{ }"
                VisitSpace(elements[0].After, p);
            }
            else
            {
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
        if (de.TypeExpression != null)
        {
            Visit(de.TypeExpression, p);
        }
        Visit(de.Variables, p);
        AfterSyntax(de, p);
        return de;
    }

    public override J VisitSingleVariableDesignation(SingleVariableDesignation svd, PrintOutputCapture<P> p)
    {
        BeforeSyntax(svd, p);
        Visit(svd.Name, p);
        AfterSyntax(svd, p);
        return svd;
    }

    public override J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation pvd, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pvd, p);
        VisitContainer("(", pvd.Variables, ",", ")", p);
        AfterSyntax(pvd, p);
        return pvd;
    }

    public override J VisitDiscardVariableDesignation(DiscardVariableDesignation dvd, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dvd, p);
        Visit(dvd.Discard, p);
        AfterSyntax(dvd, p);
        return dvd;
    }

    public override J VisitTypeParameter(TypeParameter typeParameter, PrintOutputCapture<P> p)
    {
        BeforeSyntax(typeParameter, p);
        if (typeParameter.Bounds?.Elements.Count > 0 &&
            typeParameter.Bounds.Elements[0].Element is ConstrainedTypeParameter ctp)
        {
            PrintConstrainedTypeParameterDecl(ctp, p);
        }
        else
        {
            Visit(typeParameter.Name, p);
        }
        AfterSyntax(typeParameter, p);
        return typeParameter;
    }

    public override J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ctp, p);
        PrintConstrainedTypeParameterDecl(ctp, p);
        if (ctp.WhereConstraint != null)
        {
            PrintConstrainedTypeParameterConstraints(ctp, p);
        }
        AfterSyntax(ctp, p);
        return ctp;
    }

    /// <summary>
    /// Prints the declaration part of a ConstrainedTypeParameter: attributes, variance, name.
    /// Used in the type parameter list between &lt; and &gt;.
    /// </summary>
    private void PrintConstrainedTypeParameterDecl(ConstrainedTypeParameter ctp, PrintOutputCapture<P> p)
    {
        // Print attributes
        foreach (var attr in ctp.AttributeLists)
        {
            Visit(attr, p);
        }

        // Print variance (in/out)
        if (ctp.Variance != null)
        {
            VisitSpace(ctp.Variance.Before, p);
            p.Append(ctp.Variance.Element == ConstrainedTypeParameter.VarianceKind.In ? "in" : "out");
        }

        // Print name
        Visit(ctp.Name, p);
    }

    /// <summary>
    /// Prints all where clauses from a type parameter container in source order.
    /// </summary>
    private void PrintTypeParameterConstraintsInSourceOrder(JContainer<TypeParameter>? typeParameters, PrintOutputCapture<P> p)
    {
        if (typeParameters == null) return;

        // Collect all type parameters that have where clauses
        var ctpsWithWhere = new List<ConstrainedTypeParameter>();
        foreach (var typeParam in typeParameters.Elements)
        {
            if (typeParam.Element.Bounds?.Elements.Count > 0 &&
                typeParam.Element.Bounds.Elements[0].Element is ConstrainedTypeParameter ctp &&
                ctp.WhereConstraint != null)
            {
                ctpsWithWhere.Add(ctp);
            }
        }

        // Sort by WhereClauseOrder marker if present
        ctpsWithWhere.Sort((a, b) =>
        {
            var orderA = a.Markers.FindFirst<WhereClauseOrder>()?.Order ?? int.MaxValue;
            var orderB = b.Markers.FindFirst<WhereClauseOrder>()?.Order ?? int.MaxValue;
            return orderA.CompareTo(orderB);
        });

        foreach (var ctp in ctpsWithWhere)
        {
            PrintConstrainedTypeParameterConstraints(ctp, p);
        }
    }

    /// <summary>
    /// Prints the constraint part of a ConstrainedTypeParameter: where T : constraint1, constraint2
    /// Used after the base type list.
    /// </summary>
    private void PrintConstrainedTypeParameterConstraints(ConstrainedTypeParameter ctp, PrintOutputCapture<P> p)
    {
        if (ctp.WhereConstraint == null) return;

        VisitSpace(ctp.WhereConstraint.Before, p);
        p.Append("where");
        Visit(ctp.WhereConstraint.Element, p);

        // Print constraints: the container's Before holds the space before ":"
        if (ctp.Constraints != null)
        {
            VisitSpace(ctp.Constraints.Before, p);
            p.Append(':');
            for (var i = 0; i < ctp.Constraints.Elements.Count; i++)
            {
                var constraint = ctp.Constraints.Elements[i];
                Visit(constraint.Element, p);
                if (i < ctp.Constraints.Elements.Count - 1)
                {
                    VisitSpace(constraint.After, p);
                    p.Append(',');
                }
            }
        }
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

    public override J VisitCsBinary(CsBinary csb, PrintOutputCapture<P> p)
    {
        BeforeSyntax(csb, p);
        Visit(csb.Left, p);
        VisitSpace(csb.Operator.Before, p);
        p.Append(csb.Operator.Element switch
        {
            CsBinary.OperatorType.As => "as",
            CsBinary.OperatorType.NullCoalescing => "??",
            _ => throw new InvalidOperationException($"Unknown CsBinary operator: {csb.Operator.Element}")
        });
        Visit(csb.Right, p);
        AfterSyntax(csb, p);
        return csb;
    }

    public override J VisitStatementExpression(StatementExpression se, PrintOutputCapture<P> p)
    {
        BeforeSyntax(se, p);
        Visit(se.Statement, p);
        AfterSyntax(se, p);
        return se;
    }

    public override J VisitSizeOf(SizeOf sizeOf, PrintOutputCapture<P> p)
    {
        BeforeSyntax(sizeOf, p);
        p.Append("sizeof");
        p.Append('(');
        Visit(sizeOf.Expression, p);
        p.Append(')');
        AfterSyntax(sizeOf, p);
        return sizeOf;
    }

    public override J VisitUnsafeStatement(UnsafeStatement unsafeStatement, PrintOutputCapture<P> p)
    {
        BeforeSyntax(unsafeStatement, p);
        p.Append("unsafe");
        Visit(unsafeStatement.Block, p);
        AfterSyntax(unsafeStatement, p);
        return unsafeStatement;
    }

    public override J VisitPointerType(PointerType pointerType, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pointerType, p);
        Visit(pointerType.ElementType.Element, p);
        VisitSpace(pointerType.ElementType.After, p);
        p.Append('*');
        AfterSyntax(pointerType, p);
        return pointerType;
    }

    public override J VisitFixedStatement(FixedStatement fixedStatement, PrintOutputCapture<P> p)
    {
        BeforeSyntax(fixedStatement, p);
        p.Append("fixed");
        // Print ControlParentheses<VariableDeclarations> manually
        VisitSpace(fixedStatement.Declarations.Prefix, p);
        p.Append('(');
        VisitVariableDeclarationsWithoutSemicolon(fixedStatement.Declarations.Tree.Element, p);
        VisitSpace(fixedStatement.Declarations.Tree.After, p);
        p.Append(')');
        Visit(fixedStatement.Block, p);
        AfterSyntax(fixedStatement, p);
        return fixedStatement;
    }

    public override J VisitExternAlias(ExternAlias externAlias, PrintOutputCapture<P> p)
    {
        BeforeSyntax(externAlias, p);
        p.Append("extern");
        VisitSpace(externAlias.Identifier.Before, p);
        p.Append("alias");
        Visit(externAlias.Identifier.Element, p);
        // Semicolon printed by PrintStatementTerminator
        AfterSyntax(externAlias, p);
        return externAlias;
    }

    public override J VisitInitializerExpression(InitializerExpression initializerExpression, PrintOutputCapture<P> p)
    {
        BeforeSyntax(initializerExpression, p);
        var elements = initializerExpression.Expressions.Elements;
        VisitSpace(initializerExpression.Expressions.Before, p);
        p.Append("{");
        if (elements.Count == 1 && elements[0].Element is Empty)
        {
            // Sentinel empty element preserves the space inside empty braces: { }
            VisitSpace(elements[0].After, p);
        }
        else
        {
            VisitRightPadded(elements, ",", p);
        }
        p.Append("}");
        AfterSyntax(initializerExpression, p);
        return initializerExpression;
    }

    public override J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, PrintOutputCapture<P> p)
    {
        BeforeSyntax(nullSafeExpression, p);
        VisitRightPadded(nullSafeExpression.ExpressionPadded, null, p);
        p.Append('!');
        AfterSyntax(nullSafeExpression, p);
        return nullSafeExpression;
    }

    public override J VisitDefaultExpression(DefaultExpression defaultExpression, PrintOutputCapture<P> p)
    {
        BeforeSyntax(defaultExpression, p);
        p.Append("default");
        if (defaultExpression.TypeOperator != null)
        {
            VisitSpace(defaultExpression.TypeOperator.Before, p);
            p.Append('(');
            foreach (var paddedType in defaultExpression.TypeOperator.Elements)
            {
                Visit(paddedType.Element, p);
                VisitSpace(paddedType.After, p);
            }
            p.Append(')');
        }
        AfterSyntax(defaultExpression, p);
        return defaultExpression;
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
        if (pp.Subpatterns.Elements.Count == 1 && pp.Subpatterns.Elements[0].Element is Empty empty)
        {
            // Empty property pattern: { } — J.Empty holds whitespace before }
            VisitSpace(pp.Subpatterns.Before, p);
            p.Append('{');
            VisitSpace(empty.Prefix, p);
            p.Append('}');
        }
        else
        {
            VisitContainer("{", pp.Subpatterns, ",", "}", p);
        }

        // Print optional designation (variable name, e.g., "varBlock" in "{ } varBlock")
        if (pp.Designation != null)
        {
            Visit(pp.Designation, p);
        }

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
            p.Append(GetModifierString(mod));
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
            p.Append(GetModifierString(mod));
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
        var omitBraces = block.Markers.FindFirst<OmitBraces>() != null;
        if (!omitBraces)
            p.Append('{');

        foreach (var stmt in block.Statements)
        {
            // Skip primary constructor MethodDeclaration — already printed by VisitClassDeclaration
            if (stmt.Element is MethodDeclaration md && md.Markers.FindFirst<PrimaryConstructor>() != null)
                continue;
            VisitStatement(stmt, p);
        }

        VisitSpace(block.End, p);
        if (!omitBraces)
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
            p.Append(GetModifierString(mod));
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

        // Print type parameters (generics) — only print <attrs variance name, ...>
        if (classDecl.TypeParameters != null)
        {
            VisitSpace(classDecl.TypeParameters.Before, p);
            p.Append('<');
            for (var i = 0; i < classDecl.TypeParameters.Elements.Count; i++)
            {
                var typeParam = classDecl.TypeParameters.Elements[i];
                // The ConstrainedTypeParameter is in Bounds[0]
                if (typeParam.Element.Bounds?.Elements.Count > 0 &&
                    typeParam.Element.Bounds.Elements[0].Element is ConstrainedTypeParameter ctp)
                {
                    PrintConstrainedTypeParameterDecl(ctp, p);
                }
                else
                {
                    Visit(typeParam.Element.Name, p);
                }
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
        // Find the synthesized MethodDeclaration marked with PrimaryConstructor in the body
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
            VisitSpace(classDecl.Implements.Before, p);

            for (var i = 0; i < classDecl.Implements.Elements.Count; i++)
            {
                var impl = classDecl.Implements.Elements[i];
                p.Append(',');
                Visit(impl.Element, p);
                VisitSpace(impl.After, p);
            }
        }

        // Print type parameter constraints (where clauses) in source order
        PrintTypeParameterConstraintsInSourceOrder(classDecl.TypeParameters, p);

        // Print body (check for semicolon-terminated record)
        if (classDecl.Body.Markers.FindFirst<Semicolon>() != null)
        {
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

    // CsClassDeclaration and CsTypeParameter have been replaced by J.ClassDeclaration
    // with ConstrainedTypeParameter in J.TypeParameter.Bounds. Printing is handled by
    // VisitClassDeclaration above and VisitConstrainedTypeParameter.

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
            p.Append(GetModifierString(mod));
        }

        // Print return type
        if (method.ReturnTypeExpression != null)
        {
            Visit(method.ReturnTypeExpression, p);
        }

        // Print name
        VisitSpace(method.Name.Prefix, p);
        p.Append(method.Name.SimpleName);

        // Print type parameters (e.g., <T, U>)
        if (method.TypeParameters != null)
        {
            VisitContainer("<", method.TypeParameters, ",", ">", p);
        }

        // Print parameters
        VisitSpace(method.Parameters.Before, p);
        p.Append('(');
        for (int i = 0; i < method.Parameters.Elements.Count; i++)
        {
            var paddedParam = method.Parameters.Elements[i];
            var param = paddedParam.Element;

            if (param is Empty)
            {
                // Empty element for interior space in empty parens
                VisitSpace(paddedParam.After, p);
            }
            else
            {
                // Parameters are VariableDeclarations but without semicolon
                if (param is VariableDeclarations varDecl)
                {
                    VisitVariableDeclarationsWithoutSemicolon(varDecl, p);
                }
                else
                {
                    Visit(param, p);
                }

                VisitSpace(paddedParam.After, p);
                if (i < method.Parameters.Elements.Count - 1)
                {
                    p.Append(',');
                }
            }
        }
        p.Append(')');

        // Print type parameter constraints (where clauses) in source order
        PrintTypeParameterConstraintsInSourceOrder(method.TypeParameters, p);

        // Print constructor initializer if present (: base(...) or : this(...))
        // Stored in defaultValue as JLeftPadded<MethodInvocation>
        if (method.DefaultValue != null)
        {
            VisitSpace(method.DefaultValue.Before, p);
            p.Append(':');
            Visit(method.DefaultValue.Element, p);
        }

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
        else if (method.Body != null && method.Body.Markers.FindFirst<Semicolon>() != null)
        {
            // Abstract/extern method — body is Block(Semicolon) holding space before ';'
            VisitSpace(method.Body.Prefix, p);
            p.Append(';');
        }
        else if (method.Body != null)
        {
            VisitBlock(method.Body, p);
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
        // Semicolon is printed by PrintStatementTerminator via VisitStatement
        AfterSyntax(ret, p);
        return ret;
    }

    public override J VisitIf(If iff, PrintOutputCapture<P> p)
    {
        BeforeSyntax(iff, p);
        p.Append("if");
        VisitControlParentheses(iff.Condition, p);
        VisitStatement(iff.ThenPart, p);

        if (iff.ElsePart != null)
        {
            VisitSpace(iff.ElsePart.Prefix, p);
            p.Append("else");
            VisitStatement(iff.ElsePart.Body, p);
        }

        AfterSyntax(iff, p);
        return iff;
    }

    public override J VisitWhileLoop(WhileLoop whl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(whl, p);
        p.Append("while");
        VisitControlParentheses(whl.Condition, p);
        VisitStatement(whl.Body, p);
        AfterSyntax(whl, p);
        return whl;
    }

    public override J VisitDoWhileLoop(DoWhileLoop dwl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dwl, p);
        p.Append("do");
        VisitStatement(dwl.Body, p);
        VisitSpace(dwl.Condition.Before, p);
        p.Append("while");
        VisitControlParentheses(dwl.Condition.Element, p);
        // semicolon is printed by PrintStatementTerminator when DoWhileLoop is visited as a statement
        AfterSyntax(dwl, p);
        return dwl;
    }

    public override J VisitLabel(Label label, PrintOutputCapture<P> p)
    {
        BeforeSyntax(label, p);
        Visit(label.LabelName.Element, p);
        VisitSpace(label.LabelName.After, p);
        p.Append(':');
        Visit(label.Statement, p);
        PrintStatementTerminator(label.Statement, p);
        AfterSyntax(label, p);
        return label;
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
            else if (paddedInit.Element is ExpressionStatement es)
            {
                // Don't visit ExpressionStatement (which appends ';') — for-loop
                // initializers are comma-separated, not semicolon-terminated
                Visit(es.Expression, p);
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
            else
            {
                VisitSpace(paddedUpdate.After, p);
            }
        }

        p.Append(')');
        VisitStatement(fl.Body, p);
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
        VisitStatement(fel.Body, p);
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
        // Don't visit Empty exception (re-throw)
        if (thr.Exception is not Empty)
        {
            Visit(thr.Exception, p);
        }
        // Semicolon printed by PrintStatementTerminator via VisitStatement
        AfterSyntax(thr, p);
        return thr;
    }

    public override J VisitInstanceOf(InstanceOf instanceOf, PrintOutputCapture<P> p)
    {
        BeforeSyntax(instanceOf, p);
        if (instanceOf.Expression.Element is Empty)
        {
            // typeof(T) — space between typeof and ( is in Expression.After
            p.Append("typeof");
            VisitSpace(instanceOf.Expression.After, p);
            p.Append('(');
            Visit(instanceOf.Clazz, p);
            p.Append(')');
        }
        else
        {
            Visit(instanceOf.Expression.Element, p);
            VisitSpace(instanceOf.Expression.After, p);
            p.Append("is");
            Visit(instanceOf.Clazz, p);
            if (instanceOf.Pattern != null)
            {
                Visit(instanceOf.Pattern, p);
            }
        }
        AfterSyntax(instanceOf, p);
        return instanceOf;
    }

    public override J VisitBreak(Break brk, PrintOutputCapture<P> p)
    {
        BeforeSyntax(brk, p);
        p.Append("break");
        // Semicolon printed by PrintStatementTerminator
        AfterSyntax(brk, p);
        return brk;
    }

    public override J VisitContinue(Continue cont, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cont, p);
        p.Append("continue");
        // Semicolon printed by PrintStatementTerminator
        AfterSyntax(cont, p);
        return cont;
    }

    public override J VisitEmpty(Empty emp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(emp, p);
        // Semicolon printed by PrintStatementTerminator when used as a statement
        AfterSyntax(emp, p);
        return emp;
    }

    public override J VisitControlParentheses(ControlParentheses<Expression> cp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cp, p);
        var omitParens = cp.Markers.FindFirst<OmitParentheses>() != null;
        if (!omitParens) p.Append('(');
        Visit(cp.Tree.Element, p);
        VisitSpace(cp.Tree.After, p);
        if (!omitParens) p.Append(')');
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

        // Print the closing delimiter (for raw strings this includes \n + indentation before """)
        p.Append(istr.EndDelimiter);

        AfterSyntax(istr, p);
        return istr;
    }

    public override J VisitInterpolation(Interpolation interp, PrintOutputCapture<P> p)
    {
        // Determine brace count from parent InterpolatedString delimiter (e.g., $$""" → 2 braces)
        int braceCount = 1;
        var parentCursor = Cursor.GetParentTreeCursor();
        if (parentCursor.Value is InterpolatedString istr)
        {
            int dollarCount = 0;
            foreach (var c in istr.Delimiter)
            {
                if (c == '$') dollarCount++;
            }
            if (dollarCount > 1) braceCount = dollarCount;
        }

        // Opening brace(s)
        p.Append(new string('{', braceCount));

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

        // Closing brace(s)
        p.Append(new string('}', braceCount));

        return interp;
    }

    public override J VisitAwaitExpression(AwaitExpression ae, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ae, p);
        p.Append("await");
        Visit(ae.Expression, p);
        AfterSyntax(ae, p);
        return ae;
    }

    public override J VisitYield(Yield yield, PrintOutputCapture<P> p)
    {
        BeforeSyntax(yield, p);
        p.Append("yield");
        Visit(yield.ReturnOrBreakKeyword, p);
        if (yield.Expression != null)
        {
            Visit(yield.Expression, p);
        }
        // Semicolon printed by PrintStatementTerminator
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
        // Pattern combinator marker means this Binary came from a BinaryPatternSyntax (and/or keywords)
        if (binary.Markers.FindFirst<PatternCombinator>() != null &&
            op is Binary.OperatorType.And or Binary.OperatorType.Or)
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
        if (_patternDepth > 0)
            return true;
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
                _patternDepth++;
                if (label is VariableDeclarations vd)
                {
                    VisitVariableDeclarationsWithoutSemicolon(vd, p);
                }
                else
                {
                    Visit(label, p);
                }
                _patternDepth--;

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
                    _patternDepth++;
                    if (label is VariableDeclarations vd)
                    {
                        VisitVariableDeclarationsWithoutSemicolon(vd, p);
                    }
                    else
                    {
                        Visit(label, p);
                    }
                    _patternDepth--;
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
                VisitStatement(stmtPadded, p);
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

        // Print selector expression
        Visit(se.ExpressionPadded.Element, p);

        // Space before 'switch' keyword
        VisitSpace(se.ExpressionPadded.After, p);
        p.Append("switch");

        // Print arms container
        VisitSpace(se.Arms.Before, p);
        p.Append('{');

        var armElements = se.Arms.Elements;
        for (int i = 0; i < armElements.Count; i++)
        {
            var armPadded = armElements[i];
            Visit(armPadded.Element, p);
            VisitSpace(armPadded.After, p);
            VisitMarkers(armPadded.Markers, p);
            if (i < armElements.Count - 1)
            {
                p.Append(',');
            }
        }
        p.Append('}');

        AfterSyntax(se, p);
        return se;
    }

    public override J VisitSwitchExpressionArm(SwitchExpressionArm arm, PrintOutputCapture<P> p)
    {
        BeforeSyntax(arm, p);

        // Print pattern (in pattern context for and/or/not keywords)
        _patternDepth++;
        Visit(arm.Pattern, p);
        _patternDepth--;

        // Print when clause if present
        if (arm.WhenExpression != null)
        {
            VisitSpace(arm.WhenExpression.Before, p);
            p.Append("when");
            Visit(arm.WhenExpression.Element, p);
        }

        // Print => expression
        VisitSpace(arm.ExpressionPadded.Before, p);
        p.Append("=>");
        Visit(arm.ExpressionPadded.Element, p);

        AfterSyntax(arm, p);
        return arm;
    }

    public override J VisitConstantPattern(ConstantPattern cp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cp, p);
        Visit(cp.Value, p);
        AfterSyntax(cp, p);
        return cp;
    }

    public override J VisitDiscardPattern(DiscardPattern dp, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dp, p);
        p.Append('_');
        AfterSyntax(dp, p);
        return dp;
    }

    public override J VisitEnumDeclaration(EnumDeclaration ed, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ed, p);

        // Print modifiers
        foreach (var mod in ed.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
        }

        // Print 'enum' keyword (stored as left padding of name)
        VisitSpace(ed.NamePadded.Before, p);
        p.Append("enum");

        // Print name
        Visit(ed.NamePadded.Element, p);

        // Print base type if present
        if (ed.BaseType != null)
        {
            VisitSpace(ed.BaseType.Before, p);
            p.Append(':');
            Visit(ed.BaseType.Element, p);
        }

        // Print members
        if (ed.Members != null)
        {
            VisitSpace(ed.Members.Before, p);
            p.Append('{');
            var elements = ed.Members.Elements;
            for (int i = 0; i < elements.Count; i++)
            {
                Visit(elements[i].Element, p);
                VisitSpace(elements[i].After, p);
                VisitMarkers(elements[i].Markers, p);
                if (i < elements.Count - 1)
                {
                    p.Append(',');
                }
            }
            p.Append('}');
        }

        AfterSyntax(ed, p);
        return ed;
    }

    public override J VisitEnumMemberDeclaration(EnumMemberDeclaration emd, PrintOutputCapture<P> p)
    {
        BeforeSyntax(emd, p);
        foreach (var attr in emd.AttributeLists)
        {
            Visit(attr, p);
        }
        Visit(emd.Name, p);
        if (emd.Initializer != null)
        {
            VisitSpace(emd.Initializer.Before, p);
            p.Append('=');
            Visit(emd.Initializer.Element, p);
        }
        AfterSyntax(emd, p);
        return emd;
    }

    public override J VisitCheckedExpression(CheckedExpression ce, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ce, p);
        Visit(ce.CheckedOrUncheckedKeyword, p);
        VisitControlParentheses(ce.ExpressionValue, p);
        AfterSyntax(ce, p);
        return ce;
    }

    public override J VisitCheckedStatement(CheckedStatement cs2, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cs2, p);
        Visit(cs2.KeywordValue, p);
        Visit(cs2.Block, p);
        AfterSyntax(cs2, p);
        return cs2;
    }

    public override J VisitKeyword(Keyword kw, PrintOutputCapture<P> p)
    {
        BeforeSyntax(kw, p);
        p.Append(kw.Kind switch
        {
            KeywordKind.Ref => "ref",
            KeywordKind.Out => "out",
            KeywordKind.Await => "await",
            KeywordKind.Base => "base",
            KeywordKind.This => "this",
            KeywordKind.Break => "break",
            KeywordKind.Return => "return",
            KeywordKind.Not => "not",
            KeywordKind.Default => "default",
            KeywordKind.Case => "case",
            KeywordKind.Checked => "checked",
            KeywordKind.Unchecked => "unchecked",
            KeywordKind.Operator => "operator",
            _ => throw new InvalidOperationException($"Unknown keyword kind: {kw.Kind}")
        });
        AfterSyntax(kw, p);
        return kw;
    }

    public override J VisitRangeExpression(RangeExpression re, PrintOutputCapture<P> p)
    {
        BeforeSyntax(re, p);
        if (re.Start != null)
        {
            Visit(re.Start.Element, p);
            VisitSpace(re.Start.After, p);
        }
        p.Append("..");
        if (re.End != null)
        {
            Visit(re.End, p);
        }
        AfterSyntax(re, p);
        return re;
    }

    public override J VisitStackAllocExpression(StackAllocExpression sae, PrintOutputCapture<P> p)
    {
        BeforeSyntax(sae, p);
        p.Append("stackalloc");
        Visit(sae.Expression, p);
        AfterSyntax(sae, p);
        return sae;
    }

    public override J VisitCollectionExpression(CollectionExpression ce, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ce, p);
        p.Append('[');
        for (int i = 0; i < ce.Elements.Count; i++)
        {
            var rp = ce.Elements[i];
            if (rp.Element is Empty)
            {
                // Empty element holds interior space (e.g., comments in an otherwise-empty collection)
                // Don't visit Empty (which would print ';'), just emit the After space
                VisitSpace(rp.After, p);
            }
            else
            {
                Visit(rp.Element, p);
                VisitSpace(rp.After, p);
                if (i < ce.Elements.Count - 1)
                {
                    p.Append(',');
                }
                else
                {
                    VisitMarkers(rp.Markers, p);
                }
            }
        }
        p.Append(']');
        AfterSyntax(ce, p);
        return ce;
    }

    // TypeParameterConstraintClause and TypeConstraint are deleted —
    // constraint printing now handled by PrintConstrainedTypeParameterConstraints

    public override J VisitAllowsConstraintClause(AllowsConstraintClause acc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(acc, p);
        p.Append("allows");
        VisitContainer("", acc.Expressions, ",", "", p);
        AfterSyntax(acc, p);
        return acc;
    }

    public override J VisitRefStructConstraint(RefStructConstraint rsc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(rsc, p);
        p.Append("ref struct");
        AfterSyntax(rsc, p);
        return rsc;
    }

    public override J VisitClassOrStructConstraint(ClassOrStructConstraint cosc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cosc, p);
        p.Append(cosc.Kind == ClassOrStructConstraint.TypeKind.Class ? "class" : "struct");
        if (cosc.Nullable)
        {
            p.Append("?");
        }
        AfterSyntax(cosc, p);
        return cosc;
    }

    public override J VisitConstructorConstraint(ConstructorConstraint cc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(cc, p);
        p.Append("new()");
        AfterSyntax(cc, p);
        return cc;
    }

    public override J VisitDefaultConstraint(DefaultConstraint dc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(dc, p);
        p.Append("default");
        AfterSyntax(dc, p);
        return dc;
    }

    public override J VisitLambda(Lambda lambda, PrintOutputCapture<P> p)
    {
        var isAnonymousMethod = lambda.Markers.FindFirst<AnonymousMethod>() != null;

        BeforeSyntax(lambda, p);

        if (isAnonymousMethod)
        {
            p.Append("delegate");
        }

        // Print parameters
        var @params = lambda.Params;

        if (@params.Parenthesized)
        {
            if (isAnonymousMethod)
            {
                // For anonymous methods, Prefix is the space between 'delegate' and '('
                VisitSpace(@params.Prefix, p);
            }
            p.Append('(');
            if (!isAnonymousMethod)
            {
                // For regular lambdas, Prefix is the space inside '(' (after open paren)
                VisitSpace(@params.Prefix, p);
            }
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
        else if (!isAnonymousMethod)
        {
            // Single unparenthesized parameter (not for anonymous methods)
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

        if (!isAnonymousMethod)
        {
            // Print arrow (not for anonymous methods)
            VisitSpace(lambda.Arrow, p);
            p.Append("=>");
        }

        // Print body
        Visit(lambda.Body, p);

        AfterSyntax(lambda, p);
        return lambda;
    }

    public override J VisitCsLambda(CsLambda csLambda, PrintOutputCapture<P> p)
    {
        BeforeSyntax(csLambda, p);

        // Print attribute lists (e.g., [return: Description("...")])
        foreach (var attrList in csLambda.AttributeLists)
        {
            Visit(attrList, p);
        }

        // Print modifiers (async, static)
        foreach (var mod in csLambda.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
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

    public override J VisitCsUnary(CsUnary csUnary, PrintOutputCapture<P> p)
    {
        BeforeSyntax(csUnary, p);

        var op = csUnary.Operator.Element;
        bool isPostfix = op == CsUnary.OperatorKind.SuppressNullableWarning;

        if (!isPostfix)
        {
            p.Append(GetCsUnaryOperatorString(op));
        }

        Visit(csUnary.Expression, p);

        if (isPostfix)
        {
            VisitSpace(csUnary.Operator.Before, p);
            p.Append(GetCsUnaryOperatorString(op));
        }

        AfterSyntax(csUnary, p);
        return csUnary;
    }

    private static string GetCsUnaryOperatorString(CsUnary.OperatorKind op)
    {
        return op switch
        {
            CsUnary.OperatorKind.SuppressNullableWarning => "!",
            CsUnary.OperatorKind.PointerType => "*",
            CsUnary.OperatorKind.AddressOf => "&",
            CsUnary.OperatorKind.Spread => "..",
            CsUnary.OperatorKind.FromEnd => "^",
            _ => throw new InvalidOperationException($"Unknown CsUnary operator: {op}")
        };
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
            Binary.OperatorType.UnsignedRightShift => ">>>",
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
        // Semicolon is printed by PrintStatementTerminator via VisitStatement
        return expressionStatement;
    }

    public override J VisitVariableDeclarations(VariableDeclarations varDecl, PrintOutputCapture<P> p)
    {
        BeforeSyntax(varDecl, p);
        VisitVariableDeclarationsContent(varDecl, p);
        // Semicolon is printed by PrintStatementTerminator via VisitStatement
        AfterSyntax(varDecl, p);
        return varDecl;
    }

    /// <summary>
    /// Prints a parameter list container (JContainer of Statement or Expression) with given delimiters,
    /// using VisitVariableDeclarationsWithoutSemicolon for VariableDeclarations elements.
    /// </summary>
    private void PrintParameterList<T>(string open, JContainer<T>? container, string close, PrintOutputCapture<P> p) where T : J
    {
        if (container == null) return;
        VisitSpace(container.Before, p);
        p.Append(open);
        for (int i = 0; i < container.Elements.Count; i++)
        {
            var padded = container.Elements[i];
            if (padded.Element is VariableDeclarations vd)
            {
                VisitVariableDeclarationsWithoutSemicolon(vd, p);
            }
            else if (padded.Element is StatementExpression { Statement: VariableDeclarations svd })
            {
                VisitVariableDeclarationsWithoutSemicolon(svd, p);
            }
            else
            {
                Visit(padded.Element, p);
            }

            VisitSpace(padded.After, p);
            if (i < container.Elements.Count - 1)
            {
                p.Append(',');
            }
        }
        p.Append(close);
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
            p.Append(GetModifierString(mod));
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

    private static string GetModifierString(Modifier mod)
    {
        return mod.Type switch
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
            Modifier.ModifierType.LanguageExtension => mod.Keyword ?? "",
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

    private static readonly Regex GhostCommentPattern = new(@"//DIRECTIVE:(\d+)\r?\n?", RegexOptions.Compiled);

    public override J VisitConditionalDirective(ConditionalDirective cd, PrintOutputCapture<P> p)
    {
        // 1. Print each branch to a separate buffer.
        //    Ghost comments (//DIRECTIVE:N) in whitespace appear verbatim in the output.
        var branchOutputs = new string[cd.Branches.Count];
        for (int i = 0; i < cd.Branches.Count; i++)
        {
            var capture = new PrintOutputCapture<P>(p.Context);
            Visit(cd.Branches[i].Element, capture);
            branchOutputs[i] = capture.ToString();
        }

        // 2. Split each branch output by ghost comment sentinels into sections.
        //    Each branch produces N+1 sections for N directives, with matching directive indices.
        var branchSections = new List<string>[branchOutputs.Length];
        var branchTrailingNewlines = new List<bool>[branchOutputs.Length];
        int[]? directiveOrder = null;

        for (int b = 0; b < branchOutputs.Length; b++)
        {
            var sections = new List<string>();
            var trailingNewlines = new List<bool>();
            var dirIndices = new List<int>();
            var matches = GhostCommentPattern.Matches(branchOutputs[b]);

            int lastEnd = 0;
            foreach (Match m in matches)
            {
                sections.Add(branchOutputs[b][lastEnd..m.Index]);
                dirIndices.Add(int.Parse(m.Groups[1].Value));
                trailingNewlines.Add(m.Value.EndsWith('\n'));
                lastEnd = m.Index + m.Length;
            }
            sections.Add(branchOutputs[b][lastEnd..]);

            branchSections[b] = sections;
            branchTrailingNewlines[b] = trailingNewlines;
            directiveOrder ??= dirIndices.ToArray();
        }

        // If no ghost comments found (shouldn't happen), fall back to primary branch output
        if (directiveOrder == null || directiveOrder.Length == 0)
        {
            p.Append(branchOutputs[0]);
            return cd;
        }



        // 3. Assemble output by interleaving sections with directive text.
        //    Stack tracks the active branch index (starts with primary branch).
        var stack = new Stack<int>();
        stack.Push(0);

        // Section before the first directive — always from primary branch
        p.Append(branchSections[0][0]);

        for (int d = 0; d < directiveOrder.Length; d++)
        {
            int directiveIndex = directiveOrder[d];
            var directive = cd.DirectiveLines[directiveIndex];

            // Emit original directive text (e.g., "#if DEBUG")
            p.Append(directive.Text);

            // Restore the newline that the ghost comment occupied
            if (branchTrailingNewlines[0][d])
                p.Append('\n');

            // Update the active branch stack
            switch (directive.Kind)
            {
                case PreprocessorDirectiveKind.If:
                    stack.Push(directive.ActiveBranchIndex);
                    break;
                case PreprocessorDirectiveKind.Elif:
                case PreprocessorDirectiveKind.Else:
                    stack.Pop();
                    stack.Push(directive.ActiveBranchIndex);
                    break;
                case PreprocessorDirectiveKind.Endif:
                    stack.Pop();
                    break;
            }

            // Emit next section from the active branch
            int activeBranch = stack.Peek();
            // Fall back to primary branch when no branch activates this directive
            if (activeBranch < 0) activeBranch = 0;
            int sectionIndex = d + 1;
            if (activeBranch < branchSections.Length &&
                sectionIndex < branchSections[activeBranch].Count)
            {
                p.Append(branchSections[activeBranch][sectionIndex]);
            }
        }

        return cd;
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

    public override J VisitPragmaChecksumDirective(PragmaChecksumDirective pragmaChecksumDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pragmaChecksumDirective, p);
        p.Append("#pragma checksum");
        p.Append(pragmaChecksumDirective.Arguments);
        AfterSyntax(pragmaChecksumDirective, p);
        return pragmaChecksumDirective;
    }

    public override J VisitNullableDirective(NullableDirective nullableDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(nullableDirective, p);
        p.Append('#');
        p.Append(nullableDirective.HashSpacing);
        p.Append("nullable");
        p.Append(' ');
        p.Append(nullableDirective.Setting.ToString().ToLower());
        if (nullableDirective.Target != null)
        {
            p.Append(' ');
            p.Append(nullableDirective.Target.Value.ToString().ToLower());
        }
        if (nullableDirective.TrailingComment.Length > 0)
        {
            p.Append(nullableDirective.TrailingComment);
        }
        AfterSyntax(nullableDirective, p);
        return nullableDirective;
    }

    public override J VisitRegionDirective(RegionDirective regionDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(regionDirective, p);
        p.Append('#');
        p.Append(regionDirective.HashSpacing);
        p.Append("region");
        if (regionDirective.Name != null)
        {
            p.Append(regionDirective.Name);
        }
        AfterSyntax(regionDirective, p);
        return regionDirective;
    }

    public override J VisitEndRegionDirective(EndRegionDirective endRegionDirective, PrintOutputCapture<P> p)
    {
        BeforeSyntax(endRegionDirective, p);
        p.Append('#');
        p.Append(endRegionDirective.HashSpacing);
        p.Append("endregion");
        if (endRegionDirective.Name != null)
        {
            p.Append(endRegionDirective.Name);
        }
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

    private static readonly Func<string, string> CSharpMarkerWrapper =
        content => "/*~~" + content + (content.Length == 0 ? "" : "~~") + ">*/";

    /// <summary>
    /// Called at the start of each visit method. Handles prefix space and markers.
    /// </summary>
    protected void BeforeSyntax(J j, PrintOutputCapture<P> p)
    {
        BeforeSyntax(j.Prefix, j.Markers, p);
    }

    /// <summary>
    /// Called at the start of each visit method. Handles prefix space, markers,
    /// and MarkerPrinter hooks (beforePrefix and beforeSyntax).
    /// </summary>
    protected void BeforeSyntax(Space prefix, Markers markers, PrintOutputCapture<P> p)
    {
        foreach (var marker in markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.BeforePrefix(marker, new Cursor(Cursor, marker), CSharpMarkerWrapper));
        }

        VisitSpace(prefix, p);
        VisitMarkers(markers, p);

        foreach (var marker in markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.BeforeSyntax(marker, new Cursor(Cursor, marker), CSharpMarkerWrapper));
        }
    }

    /// <summary>
    /// Called at the end of each visit method. Handles markers after syntax.
    /// </summary>
    protected void AfterSyntax(J j, PrintOutputCapture<P> p)
    {
        AfterSyntax(j.Markers, p);
    }

    /// <summary>
    /// Called at the end of each visit method. Calls MarkerPrinter.AfterSyntax
    /// for each marker on the element.
    /// </summary>
    protected void AfterSyntax(Markers markers, PrintOutputCapture<P> p)
    {
        foreach (var marker in markers.MarkerList)
        {
            p.Append(p.MarkerPrinter.AfterSyntax(marker, new Cursor(Cursor, marker), CSharpMarkerWrapper));
        }
    }

    /// <summary>
    /// Visits all markers on an element.
    /// C# semantic markers (NullSafe, DelegateInvocation, etc.) affect how other
    /// elements print rather than being printed themselves. SearchResult and Markup
    /// markers are handled by the MarkerPrinter hooks in BeforeSyntax/AfterSyntax.
    /// </summary>
    protected void VisitMarkers(Markers markers, PrintOutputCapture<P> p)
    {
        var trailingComma = markers.FindFirst<TrailingComma>();
        if (trailingComma != null)
        {
            p.Append(',');
            VisitSpace(trailingComma.Suffix, p);
        }
    }

    /// <summary>
    /// Prints generic type arguments: &lt;T1, T2&gt;
    /// Used by MethodInvocation (typeParameters) and MemberReference (typeParameters).
    /// </summary>
    protected void PrintTypeArguments(JContainer<Expression> typeArgs, PrintOutputCapture<P> p)
    {
        VisitSpace(typeArgs.Before, p);
        p.Append('<');
        for (int i = 0; i < typeArgs.Elements.Count; i++)
        {
            var paddedTypeArg = typeArgs.Elements[i];
            Visit(paddedTypeArg.Element, p);
            VisitSpace(paddedTypeArg.After, p);
            if (i < typeArgs.Elements.Count - 1)
            {
                p.Append(',');
            }
        }
        p.Append('>');
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
            VisitMarkers(node.Markers, p);
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

    public override J VisitGotoStatement(GotoStatement gotoStatement, PrintOutputCapture<P> p)
    {
        BeforeSyntax(gotoStatement, p);
        p.Append("goto");

        if (gotoStatement.CaseOrDefaultKeyword != null)
        {
            VisitSpace(gotoStatement.CaseOrDefaultKeyword.Prefix, p);
            p.Append(gotoStatement.CaseOrDefaultKeyword.Kind == KeywordKind.Case ? "case" : "default");
        }

        if (gotoStatement.Target != null)
        {
            Visit(gotoStatement.Target, p);
        }

        // Semicolon printed by PrintStatementTerminator
        AfterSyntax(gotoStatement, p);
        return gotoStatement;
    }

    public override J VisitUsingStatement(UsingStatement usingStatement, PrintOutputCapture<P> p)
    {
        BeforeSyntax(usingStatement, p);
        p.Append("using");

        // Print parenthesized expression (left-padding = open paren prefix)
        VisitSpace(usingStatement.ExpressionPadded.Before, p);
        p.Append('(');

        if (usingStatement.ExpressionPadded.Element is StatementExpression { Statement: VariableDeclarations varDecl })
        {
            VisitVariableDeclarationsWithoutSemicolon(varDecl, p);
        }
        else
        {
            Visit(usingStatement.ExpressionPadded.Element, p);
        }

        p.Append(')');

        Visit(usingStatement.Statement, p);
        AfterSyntax(usingStatement, p);
        return usingStatement;
    }

    public override J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, PrintOutputCapture<P> p)
    {
        BeforeSyntax(implicitElementAccess, p);
        VisitContainer("[", implicitElementAccess.ArgumentList, ",", "]", p);
        AfterSyntax(implicitElementAccess, p);
        return implicitElementAccess;
    }

    public override J VisitSlicePattern(SlicePattern slicePattern, PrintOutputCapture<P> p)
    {
        BeforeSyntax(slicePattern, p);
        p.Append("..");
        AfterSyntax(slicePattern, p);
        return slicePattern;
    }

    public override J VisitListPattern(ListPattern listPattern, PrintOutputCapture<P> p)
    {
        BeforeSyntax(listPattern, p);
        VisitContainer("[", listPattern.Patterns, ",", "]", p);

        if (listPattern.Designation != null)
        {
            Visit(listPattern.Designation, p);
        }

        AfterSyntax(listPattern, p);
        return listPattern;
    }

    public override J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, PrintOutputCapture<P> p)
    {
        BeforeSyntax(aliasQualifiedName, p);
        Visit(aliasQualifiedName.Alias.Element, p);
        VisitSpace(aliasQualifiedName.Alias.After, p);
        p.Append("::");
        Visit(aliasQualifiedName.Name, p);
        AfterSyntax(aliasQualifiedName, p);
        return aliasQualifiedName;
    }

    public override J VisitRefType(RefType refType, PrintOutputCapture<P> p)
    {
        BeforeSyntax(refType, p);
        p.Append("ref");

        if (refType.ReadonlyKeyword != null)
        {
            VisitSpace(refType.ReadonlyKeyword.Prefix, p);
            p.Append("readonly");
        }

        Visit(refType.TypeIdentifier, p);
        AfterSyntax(refType, p);
        return refType;
    }

    public override J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression anonymousObject, PrintOutputCapture<P> p)
    {
        BeforeSyntax(anonymousObject, p);
        p.Append("new");
        var elements = anonymousObject.Initializers.Elements;
        VisitSpace(anonymousObject.Initializers.Before, p);
        p.Append("{");
        if (elements.Count == 1 && elements[0].Element is Empty)
        {
            // Sentinel Empty — print its After space to preserve "{ }"
            VisitSpace(elements[0].After, p);
        }
        else
        {
            VisitRightPadded(elements, ",", p);
        }
        p.Append("}");
        AfterSyntax(anonymousObject, p);
        return anonymousObject;
    }

    public override J VisitWithExpression(WithExpression withExpression, PrintOutputCapture<P> p)
    {
        BeforeSyntax(withExpression, p);
        Visit(withExpression.Target, p);
        VisitSpace(withExpression.InitializerPadded.Before, p);
        p.Append("with");
        Visit(withExpression.Initializer, p);
        AfterSyntax(withExpression, p);
        return withExpression;
    }

    public override J VisitSpreadExpression(SpreadExpression spreadExpression, PrintOutputCapture<P> p)
    {
        BeforeSyntax(spreadExpression, p);
        p.Append("..");
        Visit(spreadExpression.Expression, p);
        AfterSyntax(spreadExpression, p);
        return spreadExpression;
    }

    public override J VisitFunctionPointerType(FunctionPointerType functionPointerType, PrintOutputCapture<P> p)
    {
        BeforeSyntax(functionPointerType, p);
        p.Append("delegate*");

        if (functionPointerType.CallingConvention != null)
        {
            VisitSpace(functionPointerType.CallingConvention.Before, p);
            p.Append(functionPointerType.CallingConvention.Element == CallingConventionKind.Managed ? "managed" : "unmanaged");
        }

        if (functionPointerType.UnmanagedCallingConventionTypes != null)
        {
            VisitContainer("[", functionPointerType.UnmanagedCallingConventionTypes, ",", "]", p);
        }

        VisitContainer("<", functionPointerType.ParameterTypes, ",", ">", p);
        AfterSyntax(functionPointerType, p);
        return functionPointerType;
    }

    public override J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, PrintOutputCapture<P> p)
    {
        BeforeSyntax(delegateDeclaration, p);

        foreach (var attr in delegateDeclaration.Attributes)
        {
            Visit(attr, p);
        }

        foreach (var mod in delegateDeclaration.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
        }

        // ReturnType is JLeftPadded — the before space holds 'delegate' keyword prefix
        VisitSpace(delegateDeclaration.ReturnType.Before, p);
        p.Append("delegate");
        Visit(delegateDeclaration.ReturnType.Element, p);

        // Identifier (name)
        Visit(delegateDeclaration.IdentifierName, p);

        // Type parameters
        VisitContainer("<", delegateDeclaration.TypeParameters, ",", ">", p);

        // Parameters (without semicolons after each VariableDeclarations)
        PrintParameterList("(", delegateDeclaration.Parameters, ")", p);

        // Type parameter constraints (where clauses) in source order
        PrintTypeParameterConstraintsInSourceOrder(delegateDeclaration.TypeParameters, p);

        // Semicolon printed by PrintStatementTerminator
        AfterSyntax(delegateDeclaration, p);
        return delegateDeclaration;
    }

    public override J VisitEventDeclaration(EventDeclaration eventDeclaration, PrintOutputCapture<P> p)
    {
        BeforeSyntax(eventDeclaration, p);

        foreach (var attr in eventDeclaration.AttributeLists)
        {
            Visit(attr, p);
        }

        foreach (var mod in eventDeclaration.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
        }

        // TypeExpression is JLeftPadded — before space holds 'event' keyword prefix
        VisitSpace(eventDeclaration.TypeExpressionPadded.Before, p);
        p.Append("event");
        Visit(eventDeclaration.TypeExpressionPadded.Element, p);

        // Explicit interface specifier (e.g., IFoo.)
        if (eventDeclaration.InterfaceSpecifier != null)
        {
            Visit(eventDeclaration.InterfaceSpecifier.Element, p);
            VisitSpace(eventDeclaration.InterfaceSpecifier.After, p);
            p.Append('.');
        }

        // Name
        Visit(eventDeclaration.Name, p);

        // Accessors block
        if (eventDeclaration.Accessors != null)
        {
            VisitContainer("{", eventDeclaration.Accessors, "", "}", p);
        }

        AfterSyntax(eventDeclaration, p);
        return eventDeclaration;
    }

    public override J VisitIndexerDeclaration(IndexerDeclaration indexerDeclaration, PrintOutputCapture<P> p)
    {
        BeforeSyntax(indexerDeclaration, p);

        foreach (var mod in indexerDeclaration.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
        }

        // Type
        Visit(indexerDeclaration.TypeExpression, p);

        // Explicit interface specifier
        if (indexerDeclaration.ExplicitInterfaceSpecifier != null)
        {
            Visit(indexerDeclaration.ExplicitInterfaceSpecifier.Element, p);
            VisitSpace(indexerDeclaration.ExplicitInterfaceSpecifier.After, p);
            p.Append('.');
        }

        // 'this' keyword
        Visit(indexerDeclaration.Indexer, p);

        // Parameters in brackets (without semicolons)
        PrintParameterList("[", indexerDeclaration.Parameters, "]", p);

        // Expression body or accessor block
        if (indexerDeclaration.ExpressionBody != null)
        {
            VisitSpace(indexerDeclaration.ExpressionBody.Before, p);
            p.Append("=>");
            Visit(indexerDeclaration.ExpressionBody.Element, p);
            p.Append(';');
        }
        else if (indexerDeclaration.Accessors != null)
        {
            VisitBlock(indexerDeclaration.Accessors, p);
        }

        AfterSyntax(indexerDeclaration, p);
        return indexerDeclaration;
    }

    public override J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, PrintOutputCapture<P> p)
    {
        BeforeSyntax(operatorDeclaration, p);

        foreach (var attr in operatorDeclaration.AttributeLists)
        {
            Visit(attr, p);
        }

        foreach (var mod in operatorDeclaration.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
        }

        // Return type
        Visit(operatorDeclaration.ReturnType, p);

        // Explicit interface specifier
        if (operatorDeclaration.ExplicitInterfaceSpecifier != null)
        {
            Visit(operatorDeclaration.ExplicitInterfaceSpecifier.Element, p);
            VisitSpace(operatorDeclaration.ExplicitInterfaceSpecifier.After, p);
            p.Append('.');
        }

        // 'operator' keyword
        VisitSpace(operatorDeclaration.OperatorKeyword.Prefix, p);
        p.Append("operator");

        // Optional 'checked' keyword
        if (operatorDeclaration.CheckedKeyword != null)
        {
            VisitSpace(operatorDeclaration.CheckedKeyword.Prefix, p);
            p.Append("checked");
        }

        // Operator token
        VisitSpace(operatorDeclaration.OperatorToken.Before, p);
        p.Append(GetOperatorTokenString(operatorDeclaration.OperatorToken.Element));

        // Parameters (without semicolons)
        PrintParameterList("(", operatorDeclaration.Parameters, ")", p);

        // Body — expression-bodied (=> expr;) or block body ({...})
        if (operatorDeclaration.Markers.FindFirst<ExpressionBodied>() != null)
        {
            VisitSpace(operatorDeclaration.Body.Prefix, p);
            p.Append("=>");
            var returnStmt = (Return)operatorDeclaration.Body.Statements[0].Element;
            Visit(returnStmt.Expression, p);
            p.Append(';');
        }
        else
        {
            VisitBlock(operatorDeclaration.Body, p);
        }

        AfterSyntax(operatorDeclaration, p);
        return operatorDeclaration;
    }

    private static string GetOperatorTokenString(OperatorDeclaration.OperatorKind kind)
    {
        return kind switch
        {
            OperatorDeclaration.OperatorKind.Plus => "+",
            OperatorDeclaration.OperatorKind.Minus => "-",
            OperatorDeclaration.OperatorKind.Bang => "!",
            OperatorDeclaration.OperatorKind.Tilde => "~",
            OperatorDeclaration.OperatorKind.PlusPlus => "++",
            OperatorDeclaration.OperatorKind.MinusMinus => "--",
            OperatorDeclaration.OperatorKind.Star => "*",
            OperatorDeclaration.OperatorKind.Division => "/",
            OperatorDeclaration.OperatorKind.Percent => "%",
            OperatorDeclaration.OperatorKind.LeftShift => "<<",
            OperatorDeclaration.OperatorKind.RightShift => ">>",
            OperatorDeclaration.OperatorKind.UnsignedRightShift => ">>>",
            OperatorDeclaration.OperatorKind.LessThan => "<",
            OperatorDeclaration.OperatorKind.GreaterThan => ">",
            OperatorDeclaration.OperatorKind.LessThanEquals => "<=",
            OperatorDeclaration.OperatorKind.GreaterThanEquals => ">=",
            OperatorDeclaration.OperatorKind.Equals => "==",
            OperatorDeclaration.OperatorKind.NotEquals => "!=",
            OperatorDeclaration.OperatorKind.Ampersand => "&",
            OperatorDeclaration.OperatorKind.Bar => "|",
            OperatorDeclaration.OperatorKind.Caret => "^",
            OperatorDeclaration.OperatorKind.True => "true",
            OperatorDeclaration.OperatorKind.False => "false",
            _ => throw new InvalidOperationException($"Unknown operator kind: {kind}")
        };
    }

    public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, PrintOutputCapture<P> p)
    {
        BeforeSyntax(conversion, p);

        foreach (var mod in conversion.Modifiers)
        {
            VisitSpace(mod.Prefix, p);
            p.Append(GetModifierString(mod));
        }

        // implicit/explicit kind
        VisitSpace(conversion.Kind.Before, p);
        p.Append(conversion.Kind.Element == ConversionOperatorDeclaration.ExplicitImplicit.Implicit ? "implicit" : "explicit");

        // 'operator' keyword + return type
        VisitSpace(conversion.ReturnType.Before, p);
        p.Append("operator");
        Visit(conversion.ReturnType.Element, p);

        // Parameters (without semicolons)
        PrintParameterList("(", conversion.Parameters, ")", p);

        // Expression body or block body
        if (conversion.ExpressionBody != null)
        {
            VisitSpace(conversion.ExpressionBody.Before, p);
            p.Append("=>");
            Visit(conversion.ExpressionBody.Element, p);
            p.Append(';');
        }
        else if (conversion.Body != null)
        {
            VisitBlock(conversion.Body, p);
        }

        AfterSyntax(conversion, p);
        return conversion;
    }

    public override J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, PrintOutputCapture<P> p)
    {
        BeforeSyntax(forEachVariableLoop, p);
        p.Append("foreach");

        // Control: open paren, variable, 'in', iterable, close paren
        VisitSpace(forEachVariableLoop.ControlElement.Prefix, p);
        p.Append('(');

        Visit(forEachVariableLoop.ControlElement.Variable.Element, p);
        VisitSpace(forEachVariableLoop.ControlElement.Variable.After, p);
        p.Append("in");

        Visit(forEachVariableLoop.ControlElement.Iterable.Element, p);
        VisitSpace(forEachVariableLoop.ControlElement.Iterable.After, p);

        p.Append(')');

        VisitStatement(forEachVariableLoop.Body, p);
        AfterSyntax(forEachVariableLoop, p);
        return forEachVariableLoop;
    }

    public override J VisitPointerDereference(PointerDereference pd, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pd, p);
        if (pd.Markers.FindFirst<PointerMemberAccess>() == null)
            p.Append('*');
        Visit(pd.Expression, p);
        AfterSyntax(pd, p);
        return pd;
    }

    public override J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, PrintOutputCapture<P> p)
    {
        BeforeSyntax(pointerFieldAccess, p);
        Visit(pointerFieldAccess.Target, p);
        VisitSpace(pointerFieldAccess.NamePadded.Before, p);
        p.Append("->");
        Visit(pointerFieldAccess.NamePadded.Element, p);
        AfterSyntax(pointerFieldAccess, p);
        return pointerFieldAccess;
    }

    #endregion

    #region LINQ

    public override J VisitQueryExpression(QueryExpression qe, PrintOutputCapture<P> p)
    {
        BeforeSyntax(qe, p);
        Visit(qe.FromClause, p);
        Visit(qe.Body, p);
        AfterSyntax(qe, p);
        return qe;
    }

    public override J VisitQueryBody(QueryBody qb, PrintOutputCapture<P> p)
    {
        BeforeSyntax(qb, p);
        foreach (var clause in qb.Clauses)
        {
            Visit(clause, p);
        }
        if (qb.SelectOrGroup != null)
        {
            Visit(qb.SelectOrGroup, p);
        }
        if (qb.Continuation != null)
        {
            Visit(qb.Continuation, p);
        }
        AfterSyntax(qb, p);
        return qb;
    }

    public override J VisitFromClause(FromClause fc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(fc, p);
        p.Append("from");
        if (fc.TypeIdentifier != null)
        {
            Visit(fc.TypeIdentifier, p);
        }
        VisitRightPadded(fc.IdentifierPadded, "in", p);
        Visit(fc.Expression, p);
        AfterSyntax(fc, p);
        return fc;
    }

    public override J VisitLetClause(LetClause lc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(lc, p);
        p.Append("let");
        VisitRightPadded(lc.IdentifierPadded, "=", p);
        Visit(lc.Expression, p);
        AfterSyntax(lc, p);
        return lc;
    }

    public override J VisitJoinClause(JoinClause jc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jc, p);
        p.Append("join");
        VisitRightPadded(jc.IdentifierPadded, "in", p);
        VisitRightPadded(jc.InExpression, "on", p);
        VisitRightPadded(jc.LeftExpression, "equals", p);
        Visit(jc.RightExpression, p);
        if (jc.Into != null)
        {
            VisitSpace(jc.Into.Before, p);
            Visit(jc.Into.Element, p);
        }
        AfterSyntax(jc, p);
        return jc;
    }

    public override J VisitJoinIntoClause(JoinIntoClause jic, PrintOutputCapture<P> p)
    {
        BeforeSyntax(jic, p);
        p.Append("into");
        Visit(jic.Identifier, p);
        AfterSyntax(jic, p);
        return jic;
    }

    public override J VisitWhereClause(WhereClause wc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(wc, p);
        p.Append("where");
        Visit(wc.Condition, p);
        AfterSyntax(wc, p);
        return wc;
    }

    public override J VisitOrderByClause(OrderByClause obc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(obc, p);
        p.Append("orderby");
        VisitRightPadded(obc.Orderings, ",", p);
        AfterSyntax(obc, p);
        return obc;
    }

    public override J VisitOrdering(Ordering ord, PrintOutputCapture<P> p)
    {
        BeforeSyntax(ord, p);
        Visit(ord.Expression, p);
        if (ord.Direction != null)
        {
            VisitSpace(ord.ExpressionPadded.After, p);
            p.Append(ord.Direction == DirectionKind.Ascending ? "ascending" : "descending");
        }
        AfterSyntax(ord, p);
        return ord;
    }

    public override J VisitSelectClause(SelectClause sc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(sc, p);
        p.Append("select");
        Visit(sc.Expression, p);
        AfterSyntax(sc, p);
        return sc;
    }

    public override J VisitGroupClause(GroupClause gc, PrintOutputCapture<P> p)
    {
        BeforeSyntax(gc, p);
        p.Append("group");
        VisitRightPadded(gc.GroupExpression, "by", p);
        Visit(gc.Key, p);
        AfterSyntax(gc, p);
        return gc;
    }

    public override J VisitQueryContinuation(QueryContinuation qcont, PrintOutputCapture<P> p)
    {
        BeforeSyntax(qcont, p);
        p.Append("into");
        Visit(qcont.Identifier, p);
        Visit(qcont.Body, p);
        AfterSyntax(qcont, p);
        return qcont;
    }

    #endregion
}
