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
using OpenRewrite.CSharp.Format;
using OpenRewrite.Java;

namespace OpenRewrite.CSharp;

/// <summary>
/// Visitor for C# LST elements. Extends JavaVisitor to handle J types,
/// adding dispatch for C#-specific (Cs) types.
/// </summary>
public class CSharpVisitor<P> : JavaVisitor<P>
{
    protected override J? Accept(J tree, P p)
    {
        // DeconstructionPattern is a J type that implements Cs.Pattern
        // but must be visited via JavaVisitor which has the actual visit method
        if (tree is DeconstructionPattern) return base.Accept(tree, p);

        if (tree is not Cs) return base.Accept(tree, p);

        return tree switch
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
            IsPattern ip => VisitIsPattern(ip, p),
            StatementExpression se => VisitStatementExpression(se, p),
            SizeOf sof => VisitSizeOf(sof, p),
            UnsafeStatement us => VisitUnsafeStatement(us, p),
            FixedStatement fs => VisitFixedStatement(fs, p),
            PointerType pt => VisitPointerType(pt, p),
            DefaultExpression de2 => VisitDefaultExpression(de2, p),
            ExternAlias ea => VisitExternAlias(ea, p),
            InitializerExpression ie => VisitInitializerExpression(ie, p),
            RelationalPattern rp => VisitRelationalPattern(rp, p),
            PropertyPattern pp => VisitPropertyPattern(pp, p),
            ConstrainedTypeParameter ctp => VisitConstrainedTypeParameter(ctp, p),
            InterpolatedString istr => VisitInterpolatedString(istr, p),
            Interpolation interp => VisitInterpolation(interp, p),
            AwaitExpression ae => VisitAwaitExpression(ae, p),
            Yield ys => VisitYield(ys, p),
            NamespaceDeclaration ns => VisitNamespaceDeclaration(ns, p),
            TupleType tt => VisitTupleType(tt, p),
            TupleExpression te => VisitTupleExpression(te, p),
            ConditionalDirective cd => VisitConditionalDirective(cd, p),
            PragmaWarningDirective pwd => VisitPragmaWarningDirective(pwd, p),
            PragmaChecksumDirective pcd => VisitPragmaChecksumDirective(pcd, p),
            NullableDirective nd => VisitNullableDirective(nd, p),
            RegionDirective rd => VisitRegionDirective(rd, p),
            EndRegionDirective erd => VisitEndRegionDirective(erd, p),
            DefineDirective dd => VisitDefineDirective(dd, p),
            UndefDirective ud2 => VisitUndefDirective(ud2, p),
            ErrorDirective ed => VisitErrorDirective(ed, p),
            WarningDirective wd => VisitWarningDirective(wd, p),
            LineDirective ld => VisitLineDirective(ld, p),
            NullSafeExpression nse => VisitNullSafeExpression(nse, p),
            // New types
            Keyword kw => VisitKeyword(kw, p),
            NameColon nc => VisitNameColon(nc, p),
            AnnotatedStatement ans => VisitAnnotatedStatement(ans, p),
            ArrayRankSpecifier ars => VisitArrayRankSpecifier(ars, p),
            AssignmentOperation ao => VisitAssignmentOperation(ao, p),
            StackAllocExpression sae => VisitStackAllocExpression(sae, p),
            GotoStatement gs => VisitGotoStatement(gs, p),
            EventDeclaration evd => VisitEventDeclaration(evd, p),
            CsBinary csb => VisitCsBinary(csb, p),
            CollectionExpression ce => VisitCollectionExpression(ce, p),
            ForEachVariableLoop fevl => VisitForEachVariableLoop(fevl, p),
            ForEachVariableLoopControl fevlc => VisitForEachVariableLoopControl(fevlc, p),
            UsingStatement ust => VisitUsingStatement(ust, p),
            AllowsConstraintClause acc => VisitAllowsConstraintClause(acc, p),
            RefStructConstraint rsc => VisitRefStructConstraint(rsc, p),
            ClassOrStructConstraint cosc => VisitClassOrStructConstraint(cosc, p),
            ConstructorConstraint cc => VisitConstructorConstraint(cc, p),
            DefaultConstraint dc => VisitDefaultConstraint(dc, p),
            SingleVariableDesignation svd => VisitSingleVariableDesignation(svd, p),
            ParenthesizedVariableDesignation pvd => VisitParenthesizedVariableDesignation(pvd, p),
            DiscardVariableDesignation dvd => VisitDiscardVariableDesignation(dvd, p),
            CsUnary csu => VisitCsUnary(csu, p),
            TupleElement tel => VisitTupleElement(tel, p),
            ImplicitElementAccess iea => VisitImplicitElementAccess(iea, p),
            ConstantPattern cp => VisitConstantPattern(cp, p),
            DiscardPattern dp => VisitDiscardPattern(dp, p),
            ListPattern lp => VisitListPattern(lp, p),
            SlicePattern sp => VisitSlicePattern(sp, p),
            SwitchExpression swe => VisitSwitchExpression(swe, p),
            SwitchExpressionArm swea => VisitSwitchExpressionArm(swea, p),
            CheckedExpression che => VisitCheckedExpression(che, p),
            CheckedStatement chs => VisitCheckedStatement(chs, p),
            RangeExpression rang => VisitRangeExpression(rang, p),
            IndexerDeclaration idxd => VisitIndexerDeclaration(idxd, p),
            DelegateDeclaration deld => VisitDelegateDeclaration(deld, p),
            ConversionOperatorDeclaration cod => VisitConversionOperatorDeclaration(cod, p),
            OperatorDeclaration opd => VisitOperatorDeclaration(opd, p),
            EnumDeclaration enumd => VisitEnumDeclaration(enumd, p),
            EnumMemberDeclaration enummd => VisitEnumMemberDeclaration(enummd, p),
            AliasQualifiedName aqn => VisitAliasQualifiedName(aqn, p),
            PointerDereference pd => VisitPointerDereference(pd, p),
            PointerFieldAccess pfa => VisitPointerFieldAccess(pfa, p),
            RefType rt => VisitRefType(rt, p),
            AnonymousObjectCreationExpression aoce => VisitAnonymousObjectCreationExpression(aoce, p),
            WithExpression we => VisitWithExpression(we, p),
            SpreadExpression se => VisitSpreadExpression(se, p),
            FunctionPointerType fpt => VisitFunctionPointerType(fpt, p),
            // LINQ
            QueryExpression qe => VisitQueryExpression(qe, p),
            QueryBody qb => VisitQueryBody(qb, p),
            FromClause fc => VisitFromClause(fc, p),
            LetClause lc => VisitLetClause(lc, p),
            JoinClause jc => VisitJoinClause(jc, p),
            JoinIntoClause jic => VisitJoinIntoClause(jic, p),
            WhereClause wc => VisitWhereClause(wc, p),
            OrderByClause obc => VisitOrderByClause(obc, p),
            Ordering ord => VisitOrdering(ord, p),
            SelectClause sc => VisitSelectClause(sc, p),
            GroupClause gc => VisitGroupClause(gc, p),
            QueryContinuation qcont => VisitQueryContinuation(qcont, p),
            _ => throw new InvalidOperationException($"Unknown Cs tree type: {tree.GetType().Name}")
        };
    }

    public virtual J VisitCompilationUnit(CompilationUnit compilationUnit, P p)
    {
        var members = new List<Statement>();
        bool changed = false;
        foreach (var member in compilationUnit.Members)
        {
            var visited = Visit(member, p);
            if (visited is Statement stmt)
            {
                if (!ReferenceEquals(stmt, member)) changed = true;
                members.Add(stmt);
            }
        }

        return changed ? compilationUnit.WithMembers(members) : compilationUnit;
    }

    public virtual J VisitUsingDirective(UsingDirective usingDirective, P p)
    {
        var visited = Visit(usingDirective.NamespaceOrType, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, usingDirective.NamespaceOrType))
        {
            return usingDirective.WithNamespaceOrType(tt);
        }
        return usingDirective;
    }

    public virtual J VisitPropertyDeclaration(PropertyDeclaration prop, P p)
    {
        var changed = false;

        TypeTree newTypeExpr = prop.TypeExpression;
        var visitedType = Visit(prop.TypeExpression, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, prop.TypeExpression))
        {
            newTypeExpr = tt;
            changed = true;
        }

        Block? newAccessors = prop.Accessors;
        if (prop.Accessors != null)
        {
            var visitedAccessors = Visit(prop.Accessors, p);
            if (visitedAccessors is Block b && !ReferenceEquals(b, prop.Accessors))
            {
                newAccessors = b;
                changed = true;
            }
        }

        JLeftPadded<Expression>? newExprBody = prop.ExpressionBody;
        if (prop.ExpressionBody != null)
        {
            var visitedExpr = Visit(prop.ExpressionBody.Element, p);
            if (visitedExpr is Expression e && !ReferenceEquals(e, prop.ExpressionBody.Element))
            {
                newExprBody = prop.ExpressionBody.WithElement(e);
                changed = true;
            }
        }

        return changed
            ? prop.WithTypeExpression(newTypeExpr).WithAccessors(newAccessors).WithExpressionBody(newExprBody)
            : prop;
    }

    public virtual J VisitAccessorDeclaration(AccessorDeclaration accessor, P p)
    {
        var changed = false;

        Block? newBody = accessor.Body;
        if (accessor.Body != null)
        {
            var visited = Visit(accessor.Body, p);
            if (visited is Block b && !ReferenceEquals(b, accessor.Body))
            {
                newBody = b;
                changed = true;
            }
        }

        JLeftPadded<Expression>? newExprBody = accessor.ExpressionBody;
        if (accessor.ExpressionBody != null)
        {
            var visited = Visit(accessor.ExpressionBody.Element, p);
            if (visited is Expression e && !ReferenceEquals(e, accessor.ExpressionBody.Element))
            {
                newExprBody = accessor.ExpressionBody.WithElement(e);
                changed = true;
            }
        }

        return changed
            ? accessor.WithBody(newBody).WithExpressionBody(newExprBody)
            : accessor;
    }

    public virtual J VisitAttributeList(AttributeList attrList, P p)
    {
        var newAttrs = new List<JRightPadded<Annotation>>();
        bool changed = false;
        foreach (var paddedAttr in attrList.Attributes)
        {
            var visited = Visit(paddedAttr.Element, p);
            if (visited is Annotation a)
            {
                if (!ReferenceEquals(a, paddedAttr.Element)) changed = true;
                newAttrs.Add(paddedAttr.WithElement(a));
            }
            else
            {
                newAttrs.Add(paddedAttr);
            }
        }
        return changed ? attrList.WithAttributes(newAttrs) : attrList;
    }

    public virtual J VisitNamedExpression(NamedExpression ne, P p)
    {
        var expr = Visit(ne.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ne.Expression))
        {
            return ne.WithExpression(e);
        }
        return ne;
    }

    public virtual J VisitRefExpression(RefExpression re, P p)
    {
        var expr = Visit(re.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, re.Expression))
        {
            return re.WithExpression(e);
        }
        return re;
    }

    public virtual J VisitDeclarationExpression(DeclarationExpression de, P p)
    {
        var changed = false;

        TypeTree? newTypeExpr = de.TypeExpression;
        if (de.TypeExpression != null)
        {
            var visited = Visit(de.TypeExpression, p);
            if (visited is TypeTree tt && !ReferenceEquals(tt, de.TypeExpression))
            {
                newTypeExpr = tt;
                changed = true;
            }
        }

        Expression newVariables = de.Variables;
        var visitedVars = Visit(de.Variables, p);
        if (visitedVars is Expression vv && !ReferenceEquals(vv, de.Variables))
        {
            newVariables = vv;
            changed = true;
        }

        return changed
            ? de.WithTypeExpression(newTypeExpr).WithVariables(newVariables)
            : de;
    }

    public virtual J VisitIsPattern(IsPattern isPattern, P p)
    {
        var expr = Visit(isPattern.Expression, p);
        var pattern = Visit(isPattern.Pattern.Element, p);

        if ((expr is Expression e && !ReferenceEquals(e, isPattern.Expression)) ||
            (pattern is Pattern pat && !ReferenceEquals(pat, isPattern.Pattern.Element)))
        {
            return isPattern.WithExpression(expr is Expression newExpr ? newExpr : isPattern.Expression).WithPattern(pattern is Pattern newPat ? isPattern.Pattern.WithElement(newPat) : isPattern.Pattern);
        }

        return isPattern;
    }

    public virtual J VisitStatementExpression(StatementExpression se, P p)
    {
        var stmt = Visit(se.Statement, p);
        if (stmt is Statement s && !ReferenceEquals(s, se.Statement))
        {
            return se.WithStatement(s);
        }
        return se;
    }

    public virtual J VisitSizeOf(SizeOf sizeOf, P p)
    {
        var visited = Visit(sizeOf.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, sizeOf.Expression))
        {
            return sizeOf.WithExpression(e);
        }
        return sizeOf;
    }

    public virtual J VisitUnsafeStatement(UnsafeStatement unsafeStatement, P p)
    {
        var visited = Visit(unsafeStatement.Block, p);
        if (visited is Block b && !ReferenceEquals(b, unsafeStatement.Block))
        {
            return unsafeStatement.WithBlock(b);
        }
        return unsafeStatement;
    }

    public virtual J VisitPointerType(PointerType pointerType, P p)
    {
        var visited = Visit(pointerType.ElementType.Element, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, pointerType.ElementType.Element))
        {
            return pointerType.WithElementType(pointerType.ElementType.WithElement(tt));
        }
        return pointerType;
    }

    public virtual J VisitFixedStatement(FixedStatement fixedStatement, P p)
    {
        var changed = false;

        ControlParentheses<VariableDeclarations> newDecl = fixedStatement.Declarations;
        var visitedDecl = Visit(fixedStatement.Declarations, p);
        if (visitedDecl is ControlParentheses<VariableDeclarations> vd && !ReferenceEquals(vd, fixedStatement.Declarations))
        {
            newDecl = vd;
            changed = true;
        }

        Block newBlock = fixedStatement.Block;
        var visitedBlock = Visit(fixedStatement.Block, p);
        if (visitedBlock is Block b && !ReferenceEquals(b, fixedStatement.Block))
        {
            newBlock = b;
            changed = true;
        }

        return changed
            ? fixedStatement.WithDeclarations(newDecl).WithBlock(newBlock)
            : fixedStatement;
    }

    public virtual J VisitExternAlias(ExternAlias externAlias, P p)
    {
        var visited = Visit(externAlias.Identifier.Element, p);
        if (visited is Identifier id && !ReferenceEquals(id, externAlias.Identifier.Element))
        {
            return externAlias.WithIdentifier(externAlias.Identifier.WithElement(id));
        }
        return externAlias;
    }

    public virtual J VisitInitializerExpression(InitializerExpression initializerExpression, P p)
    {
        var newExprs = new List<JRightPadded<Expression>>();
        bool changed = false;
        foreach (var expr in initializerExpression.Expressions.Elements)
        {
            var visited = Visit(expr.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, expr.Element)) changed = true;
                newExprs.Add(expr.WithElement(e));
            }
            else
            {
                newExprs.Add(expr);
            }
        }
        return changed
            ? initializerExpression.WithExpressions(initializerExpression.Expressions.WithElements(newExprs))
            : initializerExpression;
    }

    public virtual J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, P p)
    {
        var visited = Visit(nullSafeExpression.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, nullSafeExpression.Expression))
        {
            return nullSafeExpression.WithExpressionPadded(nullSafeExpression.ExpressionPadded.WithElement(e));
        }
        return nullSafeExpression;
    }

    public virtual J VisitDefaultExpression(DefaultExpression defaultExpression, P p)
    {
        if (defaultExpression.TypeOperator != null)
        {
            var newElements = new List<JRightPadded<TypeTree>>();
            bool changed = false;
            foreach (var paddedType in defaultExpression.TypeOperator.Elements)
            {
                var visited = Visit(paddedType.Element, p);
                if (visited is TypeTree tt)
                {
                    if (!ReferenceEquals(tt, paddedType.Element)) changed = true;
                    newElements.Add(paddedType.WithElement(tt));
                }
                else
                {
                    newElements.Add(paddedType);
                }
            }
            if (changed)
            {
                return defaultExpression.WithTypeOperator(defaultExpression.TypeOperator.WithElements(newElements));
            }
        }
        return defaultExpression;
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
            return csLambda.WithReturnType(returnType).WithLambdaExpression(lambda is Lambda newLambda ? newLambda : csLambda.LambdaExpression);
        }

        return csLambda;
    }

    public virtual J VisitRelationalPattern(RelationalPattern rp, P p)
    {
        var value = Visit(rp.Value, p);
        if (value is Expression v && !ReferenceEquals(v, rp.Value))
        {
            return rp.WithValue(v);
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

        var newElements = new List<JRightPadded<Expression>>();
        bool changed = false;

        foreach (var ne in pp.Subpatterns.Elements)
        {
            var visited = ne.Element is NamedExpression namedExpr
                ? VisitNamedExpression(namedExpr, p)
                : Visit(ne.Element, p);
            if (visited is Expression expr && !ReferenceEquals(expr, ne.Element))
            {
                changed = true;
                newElements.Add(ne.WithElement(expr));
            }
            else
            {
                newElements.Add(ne);
            }
        }

        var subpatterns = changed
            ? pp.Subpatterns.WithElements(newElements)
            : pp.Subpatterns;

        if (!ReferenceEquals(typeQualifier, pp.TypeQualifier) ||
            !ReferenceEquals(subpatterns, pp.Subpatterns))
        {
            return pp.WithTypeQualifier(typeQualifier).WithSubpatterns(subpatterns);
        }

        return pp;
    }

    public virtual J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, P p)
    {
        var visited = Visit(ctp.Name, p);
        if (visited is Identifier id && !ReferenceEquals(id, ctp.Name))
        {
            return ctp.WithName(id);
        }
        return ctp;
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
            return istr.WithParts(newParts);
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
            return interp.WithExpression(expr is Expression newExpr ? newExpr : interp.Expression).WithAlignment(newAlignment != null && interp.Alignment != null ? interp.Alignment.WithElement(newAlignment) : interp.Alignment).WithFormat(newFormat != null && interp.Format != null ? interp.Format.WithElement(newFormat) : interp.Format);
        }

        return interp;
    }

    public virtual J VisitAwaitExpression(AwaitExpression ae, P p)
    {
        var expr = Visit(ae.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ae.Expression))
        {
            return ae.WithExpression(e);
        }
        return ae;
    }

    public virtual J VisitYield(Yield yield, P p)
    {
        var changed = false;

        Keyword newKeyword = yield.ReturnOrBreakKeyword;
        var visitedKw = Visit(yield.ReturnOrBreakKeyword, p);
        if (visitedKw is Keyword kw && !ReferenceEquals(kw, yield.ReturnOrBreakKeyword))
        {
            newKeyword = kw;
            changed = true;
        }

        Expression? newExpr = yield.Expression;
        if (yield.Expression != null)
        {
            var visited = Visit(yield.Expression, p);
            if (visited is Expression e && !ReferenceEquals(e, yield.Expression))
            {
                newExpr = e;
                changed = true;
            }
        }

        return changed
            ? yield.WithReturnOrBreakKeyword(newKeyword).WithExpression(newExpr)
            : yield;
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
                newMembers.Add(member.WithElement(stmt));
            }
        }

        if ((name is Expression e && !ReferenceEquals(e, ns.Name.Element)) ||
            membersChanged)
        {
            return ns.WithName(name is Expression newName ? ns.Name.WithElement(newName) : ns.Name).WithMembers(membersChanged ? newMembers : ns.Members);
        }

        return ns;
    }

    public virtual J VisitTupleType(TupleType tupleType, P p)
    {
        var elements = tupleType.Elements.Elements;
        var newElements = new List<JRightPadded<TupleElement>>();
        bool elementsChanged = false;

        foreach (var element in elements)
        {
            var visited = Visit(element.Element, p);
            if (visited is TupleElement te)
            {
                if (!ReferenceEquals(te, element.Element))
                {
                    elementsChanged = true;
                }
                newElements.Add(element.WithElement(te));
            }
        }

        if (elementsChanged)
        {
            return tupleType.WithElements(new JContainer<TupleElement>(tupleType.Elements.Before, newElements, tupleType.Elements.Markers));
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
                newArgs.Add(arg.WithElement(expr));
            }
        }

        if (argsChanged)
        {
            return tupleExpr.WithArguments(new JContainer<Expression>(tupleExpr.Arguments.Before, newArgs, tupleExpr.Arguments.Markers));
        }

        return tupleExpr;
    }

    public virtual J VisitConditionalDirective(ConditionalDirective conditionalDirective, P p)
    {
        var branches = new List<JRightPadded<CompilationUnit>>();
        foreach (var branch in conditionalDirective.Branches)
        {
            var visited = (CompilationUnit?)Visit(branch.Element, p);
            if (visited != null)
                branches.Add(branch.WithElement(visited));
        }
        return conditionalDirective.WithBranches(branches);
    }

    public virtual J VisitPragmaWarningDirective(PragmaWarningDirective pragmaWarningDirective, P p)
    {
        return pragmaWarningDirective;
    }

    public virtual J VisitPragmaChecksumDirective(PragmaChecksumDirective pragmaChecksumDirective, P p)
    {
        return pragmaChecksumDirective;
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
        return defineDirective.WithSymbol(symbol!);
    }

    public virtual J VisitUndefDirective(UndefDirective undefDirective, P p)
    {
        var symbol = (Identifier?)Visit(undefDirective.Symbol, p);
        return undefDirective.WithSymbol(symbol!);
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

    // ---- New types ----

    public virtual J VisitKeyword(Keyword keyword, P p) => keyword;

    public virtual J VisitNameColon(NameColon nameColon, P p) => nameColon;

    public virtual J VisitAnnotatedStatement(AnnotatedStatement annotatedStatement, P p)
    {
        var visited = Visit(annotatedStatement.Statement, p);
        if (visited is Statement s && !ReferenceEquals(s, annotatedStatement.Statement))
        {
            return annotatedStatement.WithStatement(s);
        }
        return annotatedStatement;
    }

    public virtual J VisitArrayRankSpecifier(ArrayRankSpecifier arrayRankSpecifier, P p) => arrayRankSpecifier;

    public virtual J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        var visitedVar = Visit(assignmentOperation.Variable, p);
        var visitedValue = Visit(assignmentOperation.AssignmentValue, p);

        var varChanged = visitedVar is Expression v && !ReferenceEquals(v, assignmentOperation.Variable);
        var valueChanged = visitedValue is Expression a && !ReferenceEquals(a, assignmentOperation.AssignmentValue);

        if (varChanged || valueChanged)
        {
            var result = assignmentOperation;
            if (varChanged)
                result = result.WithVariable((Expression)visitedVar!);
            if (valueChanged)
                result = result.WithAssignmentValue((Expression)visitedValue!);
            return result;
        }

        return assignmentOperation;
    }

    public virtual J VisitStackAllocExpression(StackAllocExpression stackAllocExpression, P p)
    {
        var visited = Visit(stackAllocExpression.Expression, p);
        if (visited is NewArray na && !ReferenceEquals(na, stackAllocExpression.Expression))
        {
            return stackAllocExpression.WithExpression(na);
        }
        return stackAllocExpression;
    }

    public virtual J VisitGotoStatement(GotoStatement gotoStatement, P p)
    {
        if (gotoStatement.Target != null)
        {
            var visited = Visit(gotoStatement.Target, p);
            if (visited is Expression e && !ReferenceEquals(e, gotoStatement.Target))
            {
                return gotoStatement.WithTarget(e);
            }
        }
        return gotoStatement;
    }

    public virtual J VisitEventDeclaration(EventDeclaration eventDeclaration, P p)
    {
        var changed = false;

        JLeftPadded<TypeTree> newTypeExpr = eventDeclaration.TypeExpressionPadded;
        var visitedType = Visit(eventDeclaration.TypeExpressionPadded.Element, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, eventDeclaration.TypeExpressionPadded.Element))
        {
            newTypeExpr = eventDeclaration.TypeExpressionPadded.WithElement(tt);
            changed = true;
        }

        Identifier newName = eventDeclaration.Name;
        var visitedName = Visit(eventDeclaration.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, eventDeclaration.Name))
        {
            newName = id;
            changed = true;
        }

        return changed
            ? eventDeclaration.WithTypeExpressionPadded(newTypeExpr).WithName(newName)
            : eventDeclaration;
    }

    public virtual J VisitCsBinary(CsBinary csBinary, P p)
    {
        var left = Visit(csBinary.Left, p);
        var right = Visit(csBinary.Right, p);

        if (left is Expression l && right is Expression r &&
            (!ReferenceEquals(l, csBinary.Left) || !ReferenceEquals(r, csBinary.Right)))
        {
            return csBinary.WithLeft(l).WithRight(r);
        }
        return csBinary;
    }

    public virtual J VisitCollectionExpression(CollectionExpression collectionExpression, P p) => collectionExpression;

    public virtual J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, P p)
    {
        var changed = false;

        ForEachVariableLoopControl newControl = forEachVariableLoop.ControlElement;
        var visitedControl = Visit(forEachVariableLoop.ControlElement, p);
        if (visitedControl is ForEachVariableLoopControl fc && !ReferenceEquals(fc, forEachVariableLoop.ControlElement))
        {
            newControl = fc;
            changed = true;
        }

        JRightPadded<Statement> newBody = forEachVariableLoop.Body;
        var visitedBody = Visit(forEachVariableLoop.Body.Element, p);
        if (visitedBody is Statement bs && !ReferenceEquals(bs, forEachVariableLoop.Body.Element))
        {
            newBody = forEachVariableLoop.Body.WithElement(bs);
            changed = true;
        }

        return changed
            ? forEachVariableLoop.WithControlElement(newControl).WithBody(newBody)
            : forEachVariableLoop;
    }

    public virtual J VisitForEachVariableLoopControl(ForEachVariableLoopControl control, P p)
    {
        var changed = false;

        JRightPadded<Expression> newVariable = control.Variable;
        var visitedVar = Visit(control.Variable.Element, p);
        if (visitedVar is Expression ve && !ReferenceEquals(ve, control.Variable.Element))
        {
            newVariable = control.Variable.WithElement(ve);
            changed = true;
        }

        JRightPadded<Expression> newIterable = control.Iterable;
        var visitedIterable = Visit(control.Iterable.Element, p);
        if (visitedIterable is Expression ie && !ReferenceEquals(ie, control.Iterable.Element))
        {
            newIterable = control.Iterable.WithElement(ie);
            changed = true;
        }

        return changed
            ? control.WithVariable(newVariable).WithIterable(newIterable)
            : control;
    }

    public virtual J VisitUsingStatement(UsingStatement usingStatement, P p)
    {
        var changed = false;

        JLeftPadded<Expression> newExpr = usingStatement.ExpressionPadded;
        var visitedExpr = Visit(usingStatement.ExpressionPadded.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, usingStatement.ExpressionPadded.Element))
        {
            newExpr = usingStatement.ExpressionPadded.WithElement(e);
            changed = true;
        }

        Statement newStmt = usingStatement.Statement;
        var visitedStmt = Visit(usingStatement.Statement, p);
        if (visitedStmt is Statement s && !ReferenceEquals(s, usingStatement.Statement))
        {
            newStmt = s;
            changed = true;
        }

        return changed
            ? usingStatement.WithExpressionPadded(newExpr).WithStatement(newStmt)
            : usingStatement;
    }

    public virtual J VisitAllowsConstraintClause(AllowsConstraintClause clause, P p) => clause;
    public virtual J VisitRefStructConstraint(RefStructConstraint constraint, P p) => constraint;
    public virtual J VisitClassOrStructConstraint(ClassOrStructConstraint constraint, P p) => constraint;
    public virtual J VisitConstructorConstraint(ConstructorConstraint constraint, P p) => constraint;
    public virtual J VisitDefaultConstraint(DefaultConstraint constraint, P p) => constraint;

    public virtual J VisitSingleVariableDesignation(SingleVariableDesignation designation, P p)
    {
        var visited = Visit(designation.Name, p);
        if (visited is Identifier id && !ReferenceEquals(id, designation.Name))
        {
            return designation.WithName(id);
        }
        return designation;
    }

    public virtual J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation designation, P p) => designation;

    public virtual J VisitDiscardVariableDesignation(DiscardVariableDesignation designation, P p)
    {
        var visited = Visit(designation.Discard, p);
        if (visited is Identifier id && !ReferenceEquals(id, designation.Discard))
        {
            return designation.WithDiscard(id);
        }
        return designation;
    }

    public virtual J VisitCsUnary(CsUnary csUnary, P p)
    {
        var visited = Visit(csUnary.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, csUnary.Expression))
        {
            return csUnary.WithExpression(e);
        }
        return csUnary;
    }

    public virtual J VisitTupleElement(TupleElement tupleElement, P p)
    {
        var changed = false;

        TypeTree newType = tupleElement.ElementType;
        var visitedType = Visit(tupleElement.ElementType, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, tupleElement.ElementType))
        {
            newType = tt;
            changed = true;
        }

        Identifier? newName = tupleElement.Name;
        if (tupleElement.Name != null)
        {
            var visitedName = Visit(tupleElement.Name, p);
            if (visitedName is Identifier id && !ReferenceEquals(id, tupleElement.Name))
            {
                newName = id;
                changed = true;
            }
        }

        return changed
            ? tupleElement.WithElementType(newType).WithName(newName)
            : tupleElement;
    }

    public virtual J VisitConstantPattern(ConstantPattern constantPattern, P p)
    {
        var visited = Visit(constantPattern.Value, p);
        if (visited is Expression e && !ReferenceEquals(e, constantPattern.Value))
        {
            return constantPattern.WithValue(e);
        }
        return constantPattern;
    }

    public virtual J VisitDiscardPattern(DiscardPattern discardPattern, P p) => discardPattern;

    public virtual J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, P p)
    {
        var newArgs = new List<JRightPadded<Expression>>();
        bool changed = false;
        foreach (var arg in implicitElementAccess.ArgumentList.Elements)
        {
            var visited = Visit(arg.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, arg.Element)) changed = true;
                newArgs.Add(arg.WithElement(e));
            }
            else
            {
                newArgs.Add(arg);
            }
        }
        return changed
            ? implicitElementAccess.WithArgumentList(implicitElementAccess.ArgumentList.WithElements(newArgs))
            : implicitElementAccess;
    }

    public virtual J VisitSlicePattern(SlicePattern slicePattern, P p) => slicePattern;

    public virtual J VisitListPattern(ListPattern listPattern, P p)
    {
        if (listPattern.Designation != null)
        {
            var visited = Visit(listPattern.Designation, p);
            if (visited is VariableDesignation v && !ReferenceEquals(v, listPattern.Designation))
            {
                return listPattern.WithDesignation(v);
            }
        }
        return listPattern;
    }

    public virtual J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        var changed = false;

        JRightPadded<Expression> newExpr = switchExpression.ExpressionPadded;
        var visitedExpr = Visit(switchExpression.ExpressionPadded.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, switchExpression.ExpressionPadded.Element))
        {
            newExpr = switchExpression.ExpressionPadded.WithElement(e);
            changed = true;
        }

        JContainer<SwitchExpressionArm> newArms = switchExpression.Arms;
        var armElements = new List<JRightPadded<SwitchExpressionArm>>();
        bool armsChanged = false;
        foreach (var arm in switchExpression.Arms.Elements)
        {
            var visited = Visit(arm.Element, p);
            if (visited is SwitchExpressionArm a)
            {
                if (!ReferenceEquals(a, arm.Element)) armsChanged = true;
                armElements.Add(arm.WithElement(a));
            }
            else
            {
                armElements.Add(arm);
            }
        }
        if (armsChanged)
        {
            newArms = switchExpression.Arms.WithElements(armElements);
            changed = true;
        }

        return changed
            ? switchExpression.WithExpressionPadded(newExpr).WithArms(newArms)
            : switchExpression;
    }

    public virtual J VisitSwitchExpressionArm(SwitchExpressionArm arm, P p)
    {
        var changed = false;

        J newPattern = arm.Pattern;
        var visitedPattern = Visit(arm.Pattern, p);
        if (visitedPattern is J pat && !ReferenceEquals(pat, arm.Pattern))
        {
            newPattern = pat;
            changed = true;
        }

        JLeftPadded<Expression> newExpr = arm.ExpressionPadded;
        var visitedExpr = Visit(arm.ExpressionPadded.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, arm.ExpressionPadded.Element))
        {
            newExpr = arm.ExpressionPadded.WithElement(e);
            changed = true;
        }

        return changed
            ? arm.WithPattern(newPattern).WithExpressionPadded(newExpr)
            : arm;
    }

    public virtual J VisitCheckedExpression(CheckedExpression checkedExpression, P p)
    {
        var changed = false;

        Keyword newKw = checkedExpression.CheckedOrUncheckedKeyword;
        var visitedKw = Visit(checkedExpression.CheckedOrUncheckedKeyword, p);
        if (visitedKw is Keyword kw && !ReferenceEquals(kw, checkedExpression.CheckedOrUncheckedKeyword))
        {
            newKw = kw;
            changed = true;
        }

        ControlParentheses<Expression> newExpr = checkedExpression.ExpressionValue;
        var visitedExpr = Visit(checkedExpression.ExpressionValue, p);
        if (visitedExpr is ControlParentheses<Expression> e && !ReferenceEquals(e, checkedExpression.ExpressionValue))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? checkedExpression.WithCheckedOrUncheckedKeyword(newKw).WithExpressionValue(newExpr)
            : checkedExpression;
    }

    public virtual J VisitCheckedStatement(CheckedStatement checkedStatement, P p)
    {
        var changed = false;

        Keyword newKw = checkedStatement.KeywordValue;
        var visitedKw = Visit(checkedStatement.KeywordValue, p);
        if (visitedKw is Keyword kw && !ReferenceEquals(kw, checkedStatement.KeywordValue))
        {
            newKw = kw;
            changed = true;
        }

        Block newBlock = checkedStatement.Block;
        var visitedBlock = Visit(checkedStatement.Block, p);
        if (visitedBlock is Block b && !ReferenceEquals(b, checkedStatement.Block))
        {
            newBlock = b;
            changed = true;
        }

        return changed
            ? checkedStatement.WithKeywordValue(newKw).WithBlock(newBlock)
            : checkedStatement;
    }

    public virtual J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        var changed = false;

        JRightPadded<Expression>? newStart = rangeExpression.Start;
        if (rangeExpression.Start != null)
        {
            var visited = Visit(rangeExpression.Start.Element, p);
            if (visited is Expression e && !ReferenceEquals(e, rangeExpression.Start.Element))
            {
                newStart = rangeExpression.Start.WithElement(e);
                changed = true;
            }
        }

        Expression? newEnd = rangeExpression.End;
        if (rangeExpression.End != null)
        {
            var visited = Visit(rangeExpression.End, p);
            if (visited is Expression e && !ReferenceEquals(e, rangeExpression.End))
            {
                newEnd = e;
                changed = true;
            }
        }

        return changed
            ? rangeExpression.WithStart(newStart).WithEnd(newEnd)
            : rangeExpression;
    }

    public virtual J VisitIndexerDeclaration(IndexerDeclaration indexerDeclaration, P p)
    {
        var changed = false;

        TypeTree newTypeExpr = indexerDeclaration.TypeExpression;
        var visitedType = Visit(indexerDeclaration.TypeExpression, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, indexerDeclaration.TypeExpression))
        {
            newTypeExpr = tt;
            changed = true;
        }

        Expression newIndexer = indexerDeclaration.Indexer;
        var visitedIndexer = Visit(indexerDeclaration.Indexer, p);
        if (visitedIndexer is Expression idx && !ReferenceEquals(idx, indexerDeclaration.Indexer))
        {
            newIndexer = idx;
            changed = true;
        }

        return changed
            ? indexerDeclaration.WithTypeExpression(newTypeExpr).WithIndexer(newIndexer)
            : indexerDeclaration;
    }

    public virtual J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, P p)
    {
        var changed = false;

        JLeftPadded<TypeTree> newReturnType = delegateDeclaration.ReturnType;
        var visitedReturn = Visit(delegateDeclaration.ReturnType.Element, p);
        if (visitedReturn is TypeTree tt && !ReferenceEquals(tt, delegateDeclaration.ReturnType.Element))
        {
            newReturnType = delegateDeclaration.ReturnType.WithElement(tt);
            changed = true;
        }

        Identifier newName = delegateDeclaration.IdentifierName;
        var visitedName = Visit(delegateDeclaration.IdentifierName, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, delegateDeclaration.IdentifierName))
        {
            newName = id;
            changed = true;
        }

        return changed
            ? delegateDeclaration.WithReturnType(newReturnType).WithIdentifierName(newName)
            : delegateDeclaration;
    }

    public virtual J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
    {
        var changed = false;

        JLeftPadded<TypeTree> newReturnType = conversion.ReturnType;
        var visitedReturn = Visit(conversion.ReturnType.Element, p);
        if (visitedReturn is TypeTree tt && !ReferenceEquals(tt, conversion.ReturnType.Element))
        {
            newReturnType = conversion.ReturnType.WithElement(tt);
            changed = true;
        }

        Block? newBody = conversion.Body;
        if (conversion.Body != null)
        {
            var visitedBody = Visit(conversion.Body, p);
            if (visitedBody is Block b && !ReferenceEquals(b, conversion.Body))
            {
                newBody = b;
                changed = true;
            }
        }

        return changed
            ? conversion.WithReturnType(newReturnType).WithBody(newBody)
            : conversion;
    }

    public virtual J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
    {
        var changed = false;

        TypeTree newReturnType = operatorDeclaration.ReturnType;
        var visitedReturn = Visit(operatorDeclaration.ReturnType, p);
        if (visitedReturn is TypeTree tt && !ReferenceEquals(tt, operatorDeclaration.ReturnType))
        {
            newReturnType = tt;
            changed = true;
        }

        Block newBody = operatorDeclaration.Body;
        var visitedBody = Visit(operatorDeclaration.Body, p);
        if (visitedBody is Block b && !ReferenceEquals(b, operatorDeclaration.Body))
        {
            newBody = b;
            changed = true;
        }

        return changed
            ? operatorDeclaration.WithReturnType(newReturnType).WithBody(newBody)
            : operatorDeclaration;
    }

    public virtual J VisitEnumDeclaration(EnumDeclaration enumDeclaration, P p) => enumDeclaration;

    public virtual J VisitEnumMemberDeclaration(EnumMemberDeclaration enumMember, P p)
    {
        var visited = Visit(enumMember.Name, p);
        if (visited is Identifier id && !ReferenceEquals(id, enumMember.Name))
        {
            return enumMember.WithName(id);
        }
        return enumMember;
    }

    public virtual J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, P p)
    {
        var visited = Visit(aliasQualifiedName.Name, p);
        if (visited is Identifier id && !ReferenceEquals(id, aliasQualifiedName.Name))
        {
            return aliasQualifiedName.WithName(id);
        }
        return aliasQualifiedName;
    }

    public virtual J VisitPointerDereference(PointerDereference pointerDereference, P p)
    {
        var expr = Visit(pointerDereference.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, pointerDereference.Expression))
        {
            return pointerDereference.WithExpression(e);
        }
        return pointerDereference;
    }

    public virtual J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, P p)
    {
        var changed = false;

        Expression newTarget = pointerFieldAccess.Target;
        var visitedTarget = Visit(pointerFieldAccess.Target, p);
        if (visitedTarget is Expression t && !ReferenceEquals(t, pointerFieldAccess.Target))
        {
            newTarget = t;
            changed = true;
        }

        JLeftPadded<Identifier> newName = pointerFieldAccess.NamePadded;
        var visitedName = Visit(pointerFieldAccess.NamePadded.Element, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, pointerFieldAccess.NamePadded.Element))
        {
            newName = pointerFieldAccess.NamePadded.WithElement(id);
            changed = true;
        }

        return changed
            ? pointerFieldAccess.WithTarget(newTarget).WithNamePadded(newName)
            : pointerFieldAccess;
    }

    public virtual J VisitRefType(RefType refType, P p)
    {
        var visited = Visit(refType.TypeIdentifier, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, refType.TypeIdentifier))
        {
            return refType.WithTypeIdentifier(tt);
        }
        return refType;
    }

    // ---- LINQ ----

    public virtual J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        var fromClause = (FromClause)VisitFromClause(queryExpression.FromClause, p);
        var body = (QueryBody)VisitQueryBody(queryExpression.Body, p);
        return queryExpression.WithFromClause(fromClause).WithBody(body);
    }

    public virtual J VisitQueryBody(QueryBody queryBody, P p)
    {
        var clauses = new List<QueryClause>();
        bool changed = false;
        foreach (var clause in queryBody.Clauses)
        {
            var visited = (QueryClause)Visit(clause, p)!;
            if (!ReferenceEquals(visited, clause)) changed = true;
            clauses.Add(visited);
        }
        SelectOrGroupClause? selectOrGroup = queryBody.SelectOrGroup;
        if (selectOrGroup != null)
        {
            var visited = (SelectOrGroupClause)Visit(selectOrGroup, p)!;
            if (!ReferenceEquals(visited, selectOrGroup)) { changed = true; selectOrGroup = visited; }
        }
        QueryContinuation? continuation = queryBody.Continuation;
        if (continuation != null)
        {
            var visited = (QueryContinuation)VisitQueryContinuation(continuation, p);
            if (!ReferenceEquals(visited, continuation)) { changed = true; continuation = visited; }
        }
        return changed ? queryBody.WithClauses(clauses).WithSelectOrGroup(selectOrGroup).WithContinuation(continuation) : queryBody;
    }

    public virtual J VisitFromClause(FromClause fromClause, P p)
    {
        var expr = Visit(fromClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, fromClause.Expression))
            return fromClause.WithExpression(e);
        return fromClause;
    }

    public virtual J VisitLetClause(LetClause letClause, P p)
    {
        var expr = Visit(letClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, letClause.Expression))
            return letClause.WithExpression(e);
        return letClause;
    }

    public virtual J VisitJoinClause(JoinClause joinClause, P p)
    {
        var changed = false;

        JRightPadded<Expression> newIn = joinClause.InExpression;
        var visitedIn = Visit(joinClause.InExpression.Element, p);
        if (visitedIn is Expression ie && !ReferenceEquals(ie, joinClause.InExpression.Element))
        {
            newIn = joinClause.InExpression.WithElement(ie);
            changed = true;
        }

        JRightPadded<Expression> newLeft = joinClause.LeftExpression;
        var visitedLeft = Visit(joinClause.LeftExpression.Element, p);
        if (visitedLeft is Expression le && !ReferenceEquals(le, joinClause.LeftExpression.Element))
        {
            newLeft = joinClause.LeftExpression.WithElement(le);
            changed = true;
        }

        Expression newRight = joinClause.RightExpression;
        var visitedRight = Visit(joinClause.RightExpression, p);
        if (visitedRight is Expression re && !ReferenceEquals(re, joinClause.RightExpression))
        {
            newRight = re;
            changed = true;
        }

        return changed
            ? joinClause.WithInExpression(newIn).WithLeftExpression(newLeft).WithRightExpression(newRight)
            : joinClause;
    }

    public virtual J VisitJoinIntoClause(JoinIntoClause joinIntoClause, P p)
    {
        var visited = Visit(joinIntoClause.Identifier, p);
        if (visited is Identifier id && !ReferenceEquals(id, joinIntoClause.Identifier))
        {
            return joinIntoClause.WithIdentifier(id);
        }
        return joinIntoClause;
    }

    public virtual J VisitWhereClause(WhereClause whereClause, P p)
    {
        var condition = Visit(whereClause.Condition, p);
        if (condition is Expression e && !ReferenceEquals(e, whereClause.Condition))
            return whereClause.WithCondition(e);
        return whereClause;
    }

    public virtual J VisitOrderByClause(OrderByClause orderByClause, P p)
    {
        var orderings = new List<JRightPadded<Ordering>>();
        bool changed = false;
        foreach (var rp in orderByClause.Orderings)
        {
            var visited = (Ordering)VisitOrdering(rp.Element, p);
            if (!ReferenceEquals(visited, rp.Element)) changed = true;
            orderings.Add(rp.WithElement(visited));
        }
        return changed ? orderByClause.WithOrderings(orderings) : orderByClause;
    }

    public virtual J VisitOrdering(Ordering ordering, P p)
    {
        var expr = Visit(ordering.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ordering.Expression))
            return ordering.WithExpressionPadded(ordering.ExpressionPadded.WithElement(e));
        return ordering;
    }

    public virtual J VisitSelectClause(SelectClause selectClause, P p)
    {
        var expr = Visit(selectClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, selectClause.Expression))
            return selectClause.WithExpression(e);
        return selectClause;
    }

    public virtual J VisitGroupClause(GroupClause groupClause, P p)
    {
        var changed = false;

        JRightPadded<Expression> newGroupExpr = groupClause.GroupExpression;
        var visitedGroup = Visit(groupClause.GroupExpression.Element, p);
        if (visitedGroup is Expression ge && !ReferenceEquals(ge, groupClause.GroupExpression.Element))
        {
            newGroupExpr = groupClause.GroupExpression.WithElement(ge);
            changed = true;
        }

        Expression newKey = groupClause.Key;
        var visitedKey = Visit(groupClause.Key, p);
        if (visitedKey is Expression ke && !ReferenceEquals(ke, groupClause.Key))
        {
            newKey = ke;
            changed = true;
        }

        return changed
            ? groupClause.WithGroupExpression(newGroupExpr).WithKey(newKey)
            : groupClause;
    }

    public virtual J VisitQueryContinuation(QueryContinuation queryContinuation, P p)
    {
        var changed = false;

        Identifier newId = queryContinuation.Identifier;
        var visitedId = Visit(queryContinuation.Identifier, p);
        if (visitedId is Identifier id && !ReferenceEquals(id, queryContinuation.Identifier))
        {
            newId = id;
            changed = true;
        }

        QueryBody newBody = queryContinuation.Body;
        var visitedBody = (QueryBody)VisitQueryBody(queryContinuation.Body, p);
        if (!ReferenceEquals(visitedBody, queryContinuation.Body))
        {
            newBody = visitedBody;
            changed = true;
        }

        return changed
            ? queryContinuation.WithIdentifier(newId).WithBody(newBody)
            : queryContinuation;
    }

    public virtual J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression anonymousObject, P p)
    {
        return anonymousObject;
    }

    public virtual J VisitWithExpression(WithExpression withExpression, P p)
    {
        var changed = false;

        Expression newTarget = withExpression.Target;
        var visitedTarget = Visit(withExpression.Target, p);
        if (visitedTarget is Expression t && !ReferenceEquals(t, withExpression.Target))
        {
            newTarget = t;
            changed = true;
        }

        JLeftPadded<Expression> newInit = withExpression.InitializerPadded;
        var visitedInit = Visit(withExpression.InitializerPadded.Element, p);
        if (visitedInit is Expression ie && !ReferenceEquals(ie, withExpression.InitializerPadded.Element))
        {
            newInit = withExpression.InitializerPadded.WithElement(ie);
            changed = true;
        }

        return changed
            ? withExpression.WithTarget(newTarget).WithInitializerPadded(newInit)
            : withExpression;
    }

    public virtual J VisitSpreadExpression(SpreadExpression spreadExpression, P p)
    {
        var visited = Visit(spreadExpression.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, spreadExpression.Expression))
        {
            return spreadExpression.WithExpression(e);
        }
        return spreadExpression;
    }

    public virtual J VisitFunctionPointerType(FunctionPointerType functionPointerType, P p)
    {
        return functionPointerType;
    }

    /// <summary>
    /// Auto-formats the given tree node using Roslyn within the enclosing compilation unit.
    /// </summary>
    protected T AutoFormat<T>(T tree, P p, Cursor cursor) where T : class, J
    {
        return tree.AutoFormat(cursor);
    }

    /// <summary>
    /// Auto-formats the given tree node if it differs from the original (before).
    /// </summary>
    protected T MaybeAutoFormat<T>(T before, T after, P p, Cursor cursor) where T : class, J
    {
        return ReferenceEquals(before, after) ? after : AutoFormat(after, p, cursor);
    }

    /// <summary>
    /// Auto-formats the given tree node if it differs from the original (before),
    /// stopping after the specified node.
    /// </summary>
    protected T MaybeAutoFormat<T>(T before, T after, J? stopAfter, P p, Cursor cursor) where T : class, J
    {
        return ReferenceEquals(before, after) ? after : after.AutoFormat(cursor, stopAfter);
    }
}
