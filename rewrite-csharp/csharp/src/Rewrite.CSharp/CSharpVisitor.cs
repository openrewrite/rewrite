using Rewrite.Core;
using Rewrite.Java;

namespace Rewrite.CSharp;

/// <summary>
/// Visitor for C# LST elements. Extends JavaVisitor to handle J types,
/// adding dispatch for C#-specific (Cs) types.
/// </summary>
public class CSharpVisitor<P> : JavaVisitor<P>
{
    public override J? Visit(Tree? tree, P p)
    {
        if (tree == null) return null;

        // J types are handled by the base JavaVisitor
        if (tree is not Cs)
        {
            return base.Visit(tree, p);
        }

        Cursor = new Cursor(Cursor, tree);

        var result = tree switch
        {
            CompilationUnit cu => VisitCompilationUnit(cu, p),
            UsingDirective ud => VisitUsingDirective(ud, p),
            PropertyDeclaration pd => VisitPropertyDeclaration(pd, p),
            AccessorDeclaration ad => VisitAccessorDeclaration(ad, p),
            AttributeList al => VisitAttributeList(al, p),
            NamedExpression ne => VisitNamedExpression(ne, p),
            RefExpression re => VisitRefExpression(re, p),
            DeclarationExpression de => VisitDeclarationExpression(de, p),
            CsLambda csl => VisitCsLambda(csl, p),
            RelationalPattern rp => VisitRelationalPattern(rp, p),
            PropertyPattern pp => VisitPropertyPattern(pp, p),
            TypeParameterBound tpb => VisitTypeParameterBound(tpb, p),
            InterpolatedString istr => VisitInterpolatedString(istr, p),
            Interpolation interp => VisitInterpolation(interp, p),
            AwaitExpression ae => VisitAwaitExpression(ae, p),
            YieldStatement ys => VisitYieldStatement(ys, p),
            NamespaceDeclaration ns => VisitNamespaceDeclaration(ns, p),
            TupleType tt => VisitTupleType(tt, p),
            TupleExpression te => VisitTupleExpression(te, p),
            ConditionalBlock cb => VisitConditionalBlock(cb, p),
            IfDirective ifd => VisitIfDirective(ifd, p),
            ElifDirective elif => VisitElifDirective(elif, p),
            ElseDirective elsed => VisitElseDirective(elsed, p),
            PragmaWarningDirective pwd => VisitPragmaWarningDirective(pwd, p),
            NullableDirective nd => VisitNullableDirective(nd, p),
            RegionDirective rd => VisitRegionDirective(rd, p),
            EndRegionDirective erd => VisitEndRegionDirective(erd, p),
            DefineDirective dd => VisitDefineDirective(dd, p),
            UndefDirective ud2 => VisitUndefDirective(ud2, p),
            ErrorDirective ed => VisitErrorDirective(ed, p),
            WarningDirective wd => VisitWarningDirective(wd, p),
            LineDirective ld => VisitLineDirective(ld, p),
            _ => throw new InvalidOperationException($"Unknown Cs tree type: {tree.GetType().Name}")
        };

        Cursor = Cursor.Parent!;

        return result;
    }

    public virtual J VisitCompilationUnit(CompilationUnit compilationUnit, P p)
    {
        var members = new List<Statement>();
        foreach (var member in compilationUnit.Members)
        {
            var visited = Visit(member, p);
            if (visited is Statement stmt)
            {
                members.Add(stmt);
            }
        }

        return compilationUnit with { Members = members };
    }

    public virtual J VisitUsingDirective(UsingDirective usingDirective, P p)
    {
        Visit(usingDirective.NamespaceOrType, p);
        return usingDirective;
    }

    public virtual J VisitPropertyDeclaration(PropertyDeclaration prop, P p)
    {
        Visit(prop.TypeExpression, p);
        if (prop.Accessors != null)
        {
            Visit(prop.Accessors, p);
        }
        if (prop.ExpressionBody != null)
        {
            Visit(prop.ExpressionBody.Element, p);
        }
        return prop;
    }

    public virtual J VisitAccessorDeclaration(AccessorDeclaration accessor, P p)
    {
        if (accessor.Body != null)
        {
            Visit(accessor.Body, p);
        }
        if (accessor.ExpressionBody != null)
        {
            Visit(accessor.ExpressionBody.Element, p);
        }
        return accessor;
    }

    public virtual J VisitAttributeList(AttributeList attrList, P p)
    {
        foreach (var paddedAttr in attrList.Attributes)
        {
            Visit(paddedAttr.Element, p);
        }
        return attrList;
    }

    public virtual J VisitNamedExpression(NamedExpression ne, P p)
    {
        var expr = Visit(ne.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ne.Expression))
        {
            return ne with { Expression = e };
        }
        return ne;
    }

    public virtual J VisitRefExpression(RefExpression re, P p)
    {
        var expr = Visit(re.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, re.Expression))
        {
            return re with { Expression = e };
        }
        return re;
    }

    public virtual J VisitDeclarationExpression(DeclarationExpression de, P p)
    {
        Visit(de.Type, p);
        return de;
    }

    public virtual J VisitCsLambda(CsLambda csLambda, P p)
    {
        TypeTree? returnType = csLambda.ReturnType;
        if (returnType != null)
        {
            var visited = Visit(returnType, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, returnType))
            {
                returnType = tt;
            }
        }

        var lambda = VisitLambda(csLambda.LambdaExpression, p);

        if (!ReferenceEquals(returnType, csLambda.ReturnType) ||
            (lambda is Lambda l && !ReferenceEquals(l, csLambda.LambdaExpression)))
        {
            return csLambda with
            {
                ReturnType = returnType,
                LambdaExpression = lambda is Lambda newLambda ? newLambda : csLambda.LambdaExpression
            };
        }

        return csLambda;
    }

    public virtual J VisitRelationalPattern(RelationalPattern rp, P p)
    {
        var value = Visit(rp.Value, p);
        if (value is Expression v && !ReferenceEquals(v, rp.Value))
        {
            return rp with { Value = v };
        }
        return rp;
    }

    public virtual J VisitPropertyPattern(PropertyPattern pp, P p)
    {
        TypeTree? typeQualifier = pp.TypeQualifier;
        if (typeQualifier != null)
        {
            var visited = Visit(typeQualifier, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, typeQualifier))
            {
                typeQualifier = tt;
            }
        }

        var newElements = new List<JRightPadded<NamedExpression>>();
        bool changed = false;

        foreach (var ne in pp.Subpatterns.Elements)
        {
            var visited = VisitNamedExpression(ne.Element, p);
            if (visited is NamedExpression sub && !ReferenceEquals(sub, ne.Element))
            {
                changed = true;
                newElements.Add(ne with { Element = sub });
            }
            else
            {
                newElements.Add(ne);
            }
        }

        var subpatterns = changed
            ? pp.Subpatterns with { Elements = newElements }
            : pp.Subpatterns;

        if (!ReferenceEquals(typeQualifier, pp.TypeQualifier) ||
            !ReferenceEquals(subpatterns, pp.Subpatterns))
        {
            return pp with
            {
                TypeQualifier = typeQualifier,
                Subpatterns = subpatterns
            };
        }

        return pp;
    }

    public virtual J VisitTypeParameterBound(TypeParameterBound tpb, P p)
    {
        TypeTree? name = tpb.Name;
        if (name != null)
        {
            var visitedName = Visit(name, p);
            if (visitedName is TypeTree n && !ReferenceEquals(n, name))
            {
                name = n;
            }
        }

        var bound = Visit(tpb.Bound, p);

        if (!ReferenceEquals(name, tpb.Name) ||
            (bound is TypeTree b && !ReferenceEquals(b, tpb.Bound)))
        {
            return tpb with
            {
                Name = name,
                Bound = bound is TypeTree newBound ? newBound : tpb.Bound
            };
        }

        return tpb;
    }

    public virtual J VisitInterpolatedString(InterpolatedString istr, P p)
    {
        var newParts = new List<J>();
        bool changed = false;

        foreach (var part in istr.Parts)
        {
            var visited = Visit(part, p);
            if (visited is J v)
            {
                if (!ReferenceEquals(v, part))
                {
                    changed = true;
                }
                newParts.Add(v);
            }
        }

        if (changed)
        {
            return istr with { Parts = newParts };
        }
        return istr;
    }

    public virtual J VisitInterpolation(Interpolation interp, P p)
    {
        var expr = Visit(interp.Expression, p);

        Expression? newAlignment = null;
        if (interp.Alignment != null)
        {
            var visited = Visit(interp.Alignment.Element, p);
            if (visited is Expression ae && !ReferenceEquals(ae, interp.Alignment.Element))
            {
                newAlignment = ae;
            }
        }

        Identifier? newFormat = null;
        if (interp.Format != null)
        {
            var visited = Visit(interp.Format.Element, p);
            if (visited is Identifier f && !ReferenceEquals(f, interp.Format.Element))
            {
                newFormat = f;
            }
        }

        if ((expr is Expression e && !ReferenceEquals(e, interp.Expression)) ||
            newAlignment != null || newFormat != null)
        {
            return interp with
            {
                Expression = expr is Expression newExpr ? newExpr : interp.Expression,
                Alignment = newAlignment != null && interp.Alignment != null
                    ? interp.Alignment with { Element = newAlignment }
                    : interp.Alignment,
                Format = newFormat != null && interp.Format != null
                    ? interp.Format with { Element = newFormat }
                    : interp.Format
            };
        }

        return interp;
    }

    public virtual J VisitAwaitExpression(AwaitExpression ae, P p)
    {
        var expr = Visit(ae.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ae.Expression))
        {
            return ae with { Expression = e };
        }
        return ae;
    }

    public virtual J VisitYieldStatement(YieldStatement yield, P p)
    {
        if (yield.Value != null)
        {
            var value = Visit(yield.Value, p);
            if (value is Expression v && !ReferenceEquals(v, yield.Value))
            {
                return yield with { Value = v };
            }
        }
        return yield;
    }

    public virtual J VisitNamespaceDeclaration(NamespaceDeclaration ns, P p)
    {
        var name = Visit(ns.Name.Element, p);

        var members = ns.Members;
        var newMembers = new List<JRightPadded<Statement>>();
        bool membersChanged = false;

        foreach (var member in members)
        {
            var visited = Visit(member.Element, p);
            if (visited is Statement stmt)
            {
                if (!ReferenceEquals(stmt, member.Element))
                {
                    membersChanged = true;
                }
                newMembers.Add(member with { Element = stmt });
            }
        }

        if ((name is Expression e && !ReferenceEquals(e, ns.Name.Element)) ||
            membersChanged)
        {
            return ns with
            {
                Name = name is Expression newName
                    ? ns.Name with { Element = newName }
                    : ns.Name,
                Members = membersChanged ? newMembers : ns.Members
            };
        }

        return ns;
    }

    public virtual J VisitTupleType(TupleType tupleType, P p)
    {
        var elements = tupleType.Elements.Elements;
        var newElements = new List<JRightPadded<VariableDeclarations>>();
        bool elementsChanged = false;

        foreach (var element in elements)
        {
            var visited = Visit(element.Element, p);
            if (visited is VariableDeclarations vd)
            {
                if (!ReferenceEquals(vd, element.Element))
                {
                    elementsChanged = true;
                }
                newElements.Add(element with { Element = vd });
            }
        }

        if (elementsChanged)
        {
            return tupleType with
            {
                Elements = new JContainer<VariableDeclarations>(tupleType.Elements.Before, newElements, tupleType.Elements.Markers)
            };
        }

        return tupleType;
    }

    public virtual J VisitTupleExpression(TupleExpression tupleExpr, P p)
    {
        var args = tupleExpr.Arguments.Elements;
        var newArgs = new List<JRightPadded<Expression>>();
        bool argsChanged = false;

        foreach (var arg in args)
        {
            var visited = Visit(arg.Element, p);
            if (visited is Expression expr)
            {
                if (!ReferenceEquals(expr, arg.Element))
                {
                    argsChanged = true;
                }
                newArgs.Add(arg with { Element = expr });
            }
        }

        if (argsChanged)
        {
            return tupleExpr with
            {
                Arguments = new JContainer<Expression>(tupleExpr.Arguments.Before, newArgs, tupleExpr.Arguments.Markers)
            };
        }

        return tupleExpr;
    }

    public virtual J VisitConditionalBlock(ConditionalBlock conditionalBlock, P p)
    {
        var ifBranch = (IfDirective?)Visit(conditionalBlock.IfBranch, p);
        var elifBranches = new List<ElifDirective>();
        foreach (var elif in conditionalBlock.ElifBranches)
        {
            var visited = Visit(elif, p);
            if (visited is ElifDirective ed) elifBranches.Add(ed);
        }
        var elseBranch = conditionalBlock.ElseBranch != null
            ? (ElseDirective?)Visit(conditionalBlock.ElseBranch, p)
            : null;
        return conditionalBlock with
        {
            IfBranch = ifBranch!,
            ElifBranches = elifBranches,
            ElseBranch = elseBranch
        };
    }

    public virtual J VisitIfDirective(IfDirective ifDirective, P p)
    {
        var condition = (Expression?)Visit(ifDirective.Condition, p);
        return ifDirective with { Condition = condition! };
    }

    public virtual J VisitElifDirective(ElifDirective elifDirective, P p)
    {
        var condition = (Expression?)Visit(elifDirective.Condition, p);
        return elifDirective with { Condition = condition! };
    }

    public virtual J VisitElseDirective(ElseDirective elseDirective, P p)
    {
        return elseDirective;
    }

    public virtual J VisitPragmaWarningDirective(PragmaWarningDirective pragmaWarningDirective, P p)
    {
        return pragmaWarningDirective;
    }

    public virtual J VisitNullableDirective(NullableDirective nullableDirective, P p)
    {
        return nullableDirective;
    }

    public virtual J VisitRegionDirective(RegionDirective regionDirective, P p)
    {
        return regionDirective;
    }

    public virtual J VisitEndRegionDirective(EndRegionDirective endRegionDirective, P p)
    {
        return endRegionDirective;
    }

    public virtual J VisitDefineDirective(DefineDirective defineDirective, P p)
    {
        var symbol = (Identifier?)Visit(defineDirective.Symbol, p);
        return defineDirective with { Symbol = symbol! };
    }

    public virtual J VisitUndefDirective(UndefDirective undefDirective, P p)
    {
        var symbol = (Identifier?)Visit(undefDirective.Symbol, p);
        return undefDirective with { Symbol = symbol! };
    }

    public virtual J VisitErrorDirective(ErrorDirective errorDirective, P p)
    {
        return errorDirective;
    }

    public virtual J VisitWarningDirective(WarningDirective warningDirective, P p)
    {
        return warningDirective;
    }

    public virtual J VisitLineDirective(LineDirective lineDirective, P p)
    {
        return lineDirective;
    }
}
