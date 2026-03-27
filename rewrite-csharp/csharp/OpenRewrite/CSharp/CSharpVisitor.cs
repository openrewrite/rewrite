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
            ExpressionStatement es => VisitExpressionStatement(es, p),
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
            TypeWithArguments twa => VisitTypeWithArguments(twa, p),
            ExplicitInterfaceMember eim => VisitExplicitInterfaceMember(eim, p),
            WhenClause wc => VisitWhenClause(wc, p),
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
        return compilationUnit
            .WithPrefix(VisitSpace(compilationUnit.Prefix, p))
            .WithMarkers(VisitMarkers(compilationUnit.Markers, p))
            .WithExterns(ListUtils.Map(compilationUnit.Externs, e => VisitRightPadded(e, p)))
            .WithUsings(ListUtils.Map(compilationUnit.Usings, u => VisitRightPadded(u, p)))
            .WithAttributeLists(ListUtils.Map(compilationUnit.AttributeLists, al => (AttributeList?)Visit(al, p)))
            .WithMembers(ListUtils.Map(compilationUnit.Members, m => VisitRightPadded(m, p)))
            .WithEof(VisitSpace(compilationUnit.Eof, p));
    }

    public virtual J VisitUsingDirective(UsingDirective usingDirective, P p)
    {
        usingDirective = usingDirective
            .WithPrefix(VisitSpace(usingDirective.Prefix, p))
            .WithMarkers(VisitMarkers(usingDirective.Markers, p));

        return usingDirective
            .WithAlias(VisitRightPadded(usingDirective.Alias, p))
            .WithNamespaceOrType((TypeTree)Visit(usingDirective.NamespaceOrType, p)!);
    }

    public virtual J VisitPropertyDeclaration(PropertyDeclaration prop, P p)
    {
        prop = prop
            .WithPrefix(VisitSpace(prop.Prefix, p))
            .WithMarkers(VisitMarkers(prop.Markers, p));

        var stmtResult = VisitStatement(prop, p);
        if (stmtResult is not PropertyDeclaration node) return stmtResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithTypeExpression((TypeTree)Visit(node.TypeExpression, p)!)
            .WithInterfaceSpecifier(VisitRightPadded(node.InterfaceSpecifier, p))
            .WithName((Identifier)Visit(node.Name, p)!)
            .WithAccessors((Block?)Visit(node.Accessors, p))
            .WithExpressionBody(VisitLeftPadded(node.ExpressionBody, p))
            .WithInitializer(VisitLeftPadded(node.Initializer, p));
    }

    public virtual J VisitAccessorDeclaration(AccessorDeclaration accessor, P p)
    {
        accessor = accessor
            .WithPrefix(VisitSpace(accessor.Prefix, p))
            .WithMarkers(VisitMarkers(accessor.Markers, p));

        var stmtResult = VisitStatement(accessor, p);
        if (stmtResult is not AccessorDeclaration node) return stmtResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithBody((Block?)Visit(node.Body, p))
            .WithExpressionBody(VisitLeftPadded(node.ExpressionBody, p));
    }

    public virtual J VisitAttributeList(AttributeList attrList, P p)
    {
        attrList = attrList
            .WithPrefix(VisitSpace(attrList.Prefix, p))
            .WithMarkers(VisitMarkers(attrList.Markers, p));

        return attrList
            .WithTarget(VisitRightPadded(attrList.Target, p))
            .WithAttributes(ListUtils.Map(attrList.Attributes, a => VisitRightPadded(a, p)));
    }

    public virtual J VisitNamedExpression(NamedExpression ne, P p)
    {
        ne = ne
            .WithPrefix(VisitSpace(ne.Prefix, p))
            .WithMarkers(VisitMarkers(ne.Markers, p));

        var exprResult = VisitExpression(ne, p);
        if (exprResult is not NamedExpression node) return exprResult;

        return node
            .WithName(VisitRightPadded(node.Name, p)!)
            .WithExpression((Expression)Visit(node.Expression, p)!);
    }

    public virtual J VisitRefExpression(RefExpression re, P p)
    {
        re = re
            .WithPrefix(VisitSpace(re.Prefix, p))
            .WithMarkers(VisitMarkers(re.Markers, p));

        var exprResult = VisitExpression(re, p);
        if (exprResult is not RefExpression node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!);
    }

    public virtual J VisitDeclarationExpression(DeclarationExpression de, P p)
    {
        de = de
            .WithPrefix(VisitSpace(de.Prefix, p))
            .WithMarkers(VisitMarkers(de.Markers, p));

        var exprResult = VisitExpression(de, p);
        if (exprResult is not DeclarationExpression node) return exprResult;

        return node
            .WithTypeExpression((TypeTree?)Visit(node.TypeExpression, p))
            .WithVariables((Expression)Visit(node.Variables, p)!);
    }

    public virtual J VisitIsPattern(IsPattern isPattern, P p)
    {
        isPattern = isPattern
            .WithPrefix(VisitSpace(isPattern.Prefix, p))
            .WithMarkers(VisitMarkers(isPattern.Markers, p));

        var exprResult = VisitExpression(isPattern, p);
        if (exprResult is not IsPattern node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithPattern(VisitLeftPadded(node.Pattern, p)!);
    }

    public virtual J VisitStatementExpression(StatementExpression se, P p)
    {
        // Pattern extends Expression, NOT Statement despite the name
        se = se
            .WithPrefix(VisitSpace(se.Prefix, p))
            .WithMarkers(VisitMarkers(se.Markers, p));

        var exprResult = VisitExpression(se, p);
        if (exprResult is not StatementExpression node) return exprResult;

        return node
            .WithStatement((Statement)Visit(node.Statement, p)!);
    }

    public virtual J VisitSizeOf(SizeOf sizeOf, P p)
    {
        sizeOf = sizeOf
            .WithPrefix(VisitSpace(sizeOf.Prefix, p))
            .WithMarkers(VisitMarkers(sizeOf.Markers, p));

        var exprResult = VisitExpression(sizeOf, p);
        if (exprResult is not SizeOf node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitUnsafeStatement(UnsafeStatement unsafeStatement, P p)
    {
        unsafeStatement = unsafeStatement
            .WithPrefix(VisitSpace(unsafeStatement.Prefix, p))
            .WithMarkers(VisitMarkers(unsafeStatement.Markers, p));

        var stmtResult = VisitStatement(unsafeStatement, p);
        if (stmtResult is not UnsafeStatement node) return stmtResult;

        return node
            .WithBlock((Block)Visit(node.Block, p)!);
    }

    public virtual J VisitPointerType(PointerType pointerType, P p)
    {
        pointerType = pointerType
            .WithPrefix(VisitSpace(pointerType.Prefix, p))
            .WithMarkers(VisitMarkers(pointerType.Markers, p));

        var exprResult = VisitExpression(pointerType, p);
        if (exprResult is not PointerType node) return exprResult;

        return node
            .WithElementType(VisitRightPadded(node.ElementType, p)!);
    }

    public virtual J VisitFixedStatement(FixedStatement fixedStatement, P p)
    {
        fixedStatement = fixedStatement
            .WithPrefix(VisitSpace(fixedStatement.Prefix, p))
            .WithMarkers(VisitMarkers(fixedStatement.Markers, p));

        var stmtResult = VisitStatement(fixedStatement, p);
        if (stmtResult is not FixedStatement node) return stmtResult;

        return node
            .WithDeclarations((ControlParentheses<VariableDeclarations>)Visit(node.Declarations, p)!)
            .WithBlock((Block)Visit(node.Block, p)!);
    }

    public virtual J VisitExternAlias(ExternAlias externAlias, P p)
    {
        externAlias = externAlias
            .WithPrefix(VisitSpace(externAlias.Prefix, p))
            .WithMarkers(VisitMarkers(externAlias.Markers, p));

        return externAlias
            .WithIdentifier(VisitLeftPadded(externAlias.Identifier, p)!);
    }

    public virtual J VisitInitializerExpression(InitializerExpression initializerExpression, P p)
    {
        initializerExpression = initializerExpression
            .WithPrefix(VisitSpace(initializerExpression.Prefix, p))
            .WithMarkers(VisitMarkers(initializerExpression.Markers, p));

        var exprResult = VisitExpression(initializerExpression, p);
        if (exprResult is not InitializerExpression node) return exprResult;

        return node
            .WithExpressions(VisitContainer(node.Expressions, p)!);
    }

    public virtual J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, P p)
    {
        nullSafeExpression = nullSafeExpression
            .WithPrefix(VisitSpace(nullSafeExpression.Prefix, p))
            .WithMarkers(VisitMarkers(nullSafeExpression.Markers, p));

        var exprResult = VisitExpression(nullSafeExpression, p);
        if (exprResult is not NullSafeExpression node) return exprResult;

        return node
            .WithExpressionPadded(VisitRightPadded(node.ExpressionPadded, p)!);
    }

    public virtual J VisitDefaultExpression(DefaultExpression defaultExpression, P p)
    {
        defaultExpression = defaultExpression
            .WithPrefix(VisitSpace(defaultExpression.Prefix, p))
            .WithMarkers(VisitMarkers(defaultExpression.Markers, p));

        var exprResult = VisitExpression(defaultExpression, p);
        if (exprResult is not DefaultExpression node) return exprResult;

        return node
            .WithTypeOperator(VisitContainer(node.TypeOperator, p));
    }

    public virtual J VisitCsLambda(CsLambda csLambda, P p)
    {
        csLambda = csLambda
            .WithPrefix(VisitSpace(csLambda.Prefix, p))
            .WithMarkers(VisitMarkers(csLambda.Markers, p));

        var stmtResult = VisitStatement(csLambda, p);
        if (stmtResult is not CsLambda s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not CsLambda node) return exprResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithReturnType((TypeTree?)Visit(node.ReturnType, p))
            .WithLambdaExpression((Lambda)VisitLambda(node.LambdaExpression, p)!);
    }

    public virtual J VisitRelationalPattern(RelationalPattern rp, P p)
    {
        rp = rp
            .WithPrefix(VisitSpace(rp.Prefix, p))
            .WithMarkers(VisitMarkers(rp.Markers, p));

        var exprResult = VisitExpression(rp, p);
        if (exprResult is not RelationalPattern node) return exprResult;

        return node
            .WithValue((Expression)Visit(node.Value, p)!);
    }

    public virtual J VisitPropertyPattern(PropertyPattern pp, P p)
    {
        pp = pp
            .WithPrefix(VisitSpace(pp.Prefix, p))
            .WithMarkers(VisitMarkers(pp.Markers, p));

        var exprResult = VisitExpression(pp, p);
        if (exprResult is not PropertyPattern node) return exprResult;

        return node
            .WithTypeQualifier((TypeTree?)Visit(node.TypeQualifier, p))
            .WithSubpatterns(VisitContainer(node.Subpatterns, p)!)
            .WithDesignation((Identifier?)Visit(node.Designation, p));
    }

    public virtual J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, P p)
    {
        return ctp
            .WithPrefix(VisitSpace(ctp.Prefix, p))
            .WithMarkers(VisitMarkers(ctp.Markers, p))
            .WithAttributeLists(ListUtils.Map(ctp.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithName((Identifier)Visit(ctp.Name, p)!)
            .WithWhereConstraint(VisitLeftPadded(ctp.WhereConstraint, p))
            .WithConstraints(VisitContainer(ctp.Constraints, p))
            .WithType((JavaType?)VisitType(ctp.Type, p));
    }

    public virtual J VisitInterpolatedString(InterpolatedString istr, P p)
    {
        istr = istr
            .WithPrefix(VisitSpace(istr.Prefix, p))
            .WithMarkers(VisitMarkers(istr.Markers, p));

        var stmtResult = VisitStatement(istr, p);
        if (stmtResult is not InterpolatedString s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not InterpolatedString node) return exprResult;

        return node
            .WithParts(ListUtils.Map(node.Parts, part => Visit(part, p)));
    }

    public virtual J VisitInterpolation(Interpolation interp, P p)
    {
        return interp
            .WithPrefix(VisitSpace(interp.Prefix, p))
            .WithMarkers(VisitMarkers(interp.Markers, p))
            .WithExpression((Expression)Visit(interp.Expression, p)!)
            .WithAlignment(VisitLeftPadded(interp.Alignment, p))
            .WithFormat(VisitLeftPadded(interp.Format, p))
            .WithAfter(VisitSpace(interp.After, p));
    }

    public virtual J VisitAwaitExpression(AwaitExpression ae, P p)
    {
        ae = ae
            .WithPrefix(VisitSpace(ae.Prefix, p))
            .WithMarkers(VisitMarkers(ae.Markers, p));

        var stmtResult = VisitStatement(ae, p);
        if (stmtResult is not AwaitExpression s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not AwaitExpression node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitYield(Yield yield, P p)
    {
        yield = yield
            .WithPrefix(VisitSpace(yield.Prefix, p))
            .WithMarkers(VisitMarkers(yield.Markers, p));

        var stmtResult = VisitStatement(yield, p);
        if (stmtResult is not Yield node) return stmtResult;

        return node
            .WithReturnOrBreakKeyword((Keyword)Visit(node.ReturnOrBreakKeyword, p)!)
            .WithExpression((Expression?)Visit(node.Expression, p));
    }

    public virtual J VisitNamespaceDeclaration(NamespaceDeclaration ns, P p)
    {
        ns = ns
            .WithPrefix(VisitSpace(ns.Prefix, p))
            .WithMarkers(VisitMarkers(ns.Markers, p));

        var stmtResult = VisitStatement(ns, p);
        if (stmtResult is not NamespaceDeclaration node) return stmtResult;

        return node
            .WithName(VisitRightPadded(node.Name, p)!)
            .WithExterns(ListUtils.Map(node.Externs, e => VisitRightPadded(e, p)))
            .WithUsings(ListUtils.Map(node.Usings, u => VisitRightPadded(u, p)))
            .WithMembers(ListUtils.Map(node.Members, m => VisitRightPadded(m, p)))
            .WithEnd(VisitSpace(node.End, p));
    }

    public virtual J VisitTupleType(TupleType tupleType, P p)
    {
        tupleType = tupleType
            .WithPrefix(VisitSpace(tupleType.Prefix, p))
            .WithMarkers(VisitMarkers(tupleType.Markers, p));

        var exprResult = VisitExpression(tupleType, p);
        if (exprResult is not TupleType node) return exprResult;

        return node
            .WithElements(VisitContainer(node.Elements, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitTupleExpression(TupleExpression tupleExpr, P p)
    {
        tupleExpr = tupleExpr
            .WithPrefix(VisitSpace(tupleExpr.Prefix, p))
            .WithMarkers(VisitMarkers(tupleExpr.Markers, p));

        var exprResult = VisitExpression(tupleExpr, p);
        if (exprResult is not TupleExpression node) return exprResult;

        return node
            .WithArguments(VisitContainer(node.Arguments, p)!);
    }

    public virtual J VisitConditionalDirective(ConditionalDirective conditionalDirective, P p)
    {
        conditionalDirective = conditionalDirective
            .WithPrefix(VisitSpace(conditionalDirective.Prefix, p))
            .WithMarkers(VisitMarkers(conditionalDirective.Markers, p));

        var stmtResult = VisitStatement(conditionalDirective, p);
        if (stmtResult is not ConditionalDirective node) return stmtResult;

        return node
            .WithBranches(ListUtils.Map(node.Branches, b => VisitRightPadded(b, p)));
    }

    public virtual J VisitPragmaWarningDirective(PragmaWarningDirective pragmaWarningDirective, P p)
    {
        pragmaWarningDirective = pragmaWarningDirective
            .WithPrefix(VisitSpace(pragmaWarningDirective.Prefix, p))
            .WithMarkers(VisitMarkers(pragmaWarningDirective.Markers, p));

        var stmtResult = VisitStatement(pragmaWarningDirective, p);
        if (stmtResult is not PragmaWarningDirective node) return stmtResult;

        return node
            .WithWarningCodes(ListUtils.Map(node.WarningCodes, c => VisitRightPadded(c, p)));
    }

    public virtual J VisitPragmaChecksumDirective(PragmaChecksumDirective pragmaChecksumDirective, P p)
    {
        pragmaChecksumDirective = pragmaChecksumDirective
            .WithPrefix(VisitSpace(pragmaChecksumDirective.Prefix, p))
            .WithMarkers(VisitMarkers(pragmaChecksumDirective.Markers, p));

        var stmtResult = VisitStatement(pragmaChecksumDirective, p);
        if (stmtResult is not PragmaChecksumDirective node) return stmtResult;

        return node;
    }

    public virtual J VisitNullableDirective(NullableDirective nullableDirective, P p)
    {
        nullableDirective = nullableDirective
            .WithPrefix(VisitSpace(nullableDirective.Prefix, p))
            .WithMarkers(VisitMarkers(nullableDirective.Markers, p));

        var stmtResult = VisitStatement(nullableDirective, p);
        if (stmtResult is not NullableDirective node) return stmtResult;

        return node;
    }

    public virtual J VisitRegionDirective(RegionDirective regionDirective, P p)
    {
        regionDirective = regionDirective
            .WithPrefix(VisitSpace(regionDirective.Prefix, p))
            .WithMarkers(VisitMarkers(regionDirective.Markers, p));

        var stmtResult = VisitStatement(regionDirective, p);
        if (stmtResult is not RegionDirective node) return stmtResult;

        return node;
    }

    public virtual J VisitEndRegionDirective(EndRegionDirective endRegionDirective, P p)
    {
        endRegionDirective = endRegionDirective
            .WithPrefix(VisitSpace(endRegionDirective.Prefix, p))
            .WithMarkers(VisitMarkers(endRegionDirective.Markers, p));

        var stmtResult = VisitStatement(endRegionDirective, p);
        if (stmtResult is not EndRegionDirective node) return stmtResult;

        return node;
    }

    public virtual J VisitDefineDirective(DefineDirective defineDirective, P p)
    {
        defineDirective = defineDirective
            .WithPrefix(VisitSpace(defineDirective.Prefix, p))
            .WithMarkers(VisitMarkers(defineDirective.Markers, p));

        var stmtResult = VisitStatement(defineDirective, p);
        if (stmtResult is not DefineDirective node) return stmtResult;

        return node
            .WithSymbol((Identifier)Visit(node.Symbol, p)!);
    }

    public virtual J VisitUndefDirective(UndefDirective undefDirective, P p)
    {
        undefDirective = undefDirective
            .WithPrefix(VisitSpace(undefDirective.Prefix, p))
            .WithMarkers(VisitMarkers(undefDirective.Markers, p));

        var stmtResult = VisitStatement(undefDirective, p);
        if (stmtResult is not UndefDirective node) return stmtResult;

        return node
            .WithSymbol((Identifier)Visit(node.Symbol, p)!);
    }

    public virtual J VisitErrorDirective(ErrorDirective errorDirective, P p)
    {
        errorDirective = errorDirective
            .WithPrefix(VisitSpace(errorDirective.Prefix, p))
            .WithMarkers(VisitMarkers(errorDirective.Markers, p));

        var stmtResult = VisitStatement(errorDirective, p);
        if (stmtResult is not ErrorDirective node) return stmtResult;

        return node;
    }

    public virtual J VisitWarningDirective(WarningDirective warningDirective, P p)
    {
        warningDirective = warningDirective
            .WithPrefix(VisitSpace(warningDirective.Prefix, p))
            .WithMarkers(VisitMarkers(warningDirective.Markers, p));

        var stmtResult = VisitStatement(warningDirective, p);
        if (stmtResult is not WarningDirective node) return stmtResult;

        return node;
    }

    public virtual J VisitLineDirective(LineDirective lineDirective, P p)
    {
        lineDirective = lineDirective
            .WithPrefix(VisitSpace(lineDirective.Prefix, p))
            .WithMarkers(VisitMarkers(lineDirective.Markers, p));

        var stmtResult = VisitStatement(lineDirective, p);
        if (stmtResult is not LineDirective node) return stmtResult;

        return node
            .WithLine((Expression?)Visit(node.Line, p))
            .WithFile((Expression?)Visit(node.File, p));
    }

    // ---- New types ----

    public virtual J VisitKeyword(Keyword keyword, P p)
    {
        return keyword
            .WithPrefix(VisitSpace(keyword.Prefix, p))
            .WithMarkers(VisitMarkers(keyword.Markers, p));
    }

    public virtual J VisitNameColon(NameColon nameColon, P p)
    {
        return nameColon
            .WithPrefix(VisitSpace(nameColon.Prefix, p))
            .WithMarkers(VisitMarkers(nameColon.Markers, p))
            .WithName(VisitRightPadded(nameColon.Name, p)!);
    }

    public virtual J VisitAnnotatedStatement(AnnotatedStatement annotatedStatement, P p)
    {
        annotatedStatement = annotatedStatement
            .WithPrefix(VisitSpace(annotatedStatement.Prefix, p))
            .WithMarkers(VisitMarkers(annotatedStatement.Markers, p));

        var stmtResult = VisitStatement(annotatedStatement, p);
        if (stmtResult is not AnnotatedStatement node) return stmtResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithStatement((Statement)Visit(node.Statement, p)!);
    }

    public virtual J VisitArrayRankSpecifier(ArrayRankSpecifier arrayRankSpecifier, P p)
    {
        arrayRankSpecifier = arrayRankSpecifier
            .WithPrefix(VisitSpace(arrayRankSpecifier.Prefix, p))
            .WithMarkers(VisitMarkers(arrayRankSpecifier.Markers, p));

        var exprResult = VisitExpression(arrayRankSpecifier, p);
        if (exprResult is not ArrayRankSpecifier node) return exprResult;

        return node
            .WithSizes(VisitContainer(node.Sizes, p)!);
    }

    public virtual J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        assignmentOperation = assignmentOperation
            .WithPrefix(VisitSpace(assignmentOperation.Prefix, p))
            .WithMarkers(VisitMarkers(assignmentOperation.Markers, p));

        var stmtResult = VisitStatement(assignmentOperation, p);
        if (stmtResult is not AssignmentOperation s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not AssignmentOperation node) return exprResult;

        return node
            .WithVariable((Expression)Visit(node.Variable, p)!)
            .WithAssignmentValue((Expression)Visit(node.AssignmentValue, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitStackAllocExpression(StackAllocExpression stackAllocExpression, P p)
    {
        stackAllocExpression = stackAllocExpression
            .WithPrefix(VisitSpace(stackAllocExpression.Prefix, p))
            .WithMarkers(VisitMarkers(stackAllocExpression.Markers, p));

        var exprResult = VisitExpression(stackAllocExpression, p);
        if (exprResult is not StackAllocExpression node) return exprResult;

        return node
            .WithExpression((NewArray)Visit(node.Expression, p)!);
    }

    public virtual J VisitGotoStatement(GotoStatement gotoStatement, P p)
    {
        gotoStatement = gotoStatement
            .WithPrefix(VisitSpace(gotoStatement.Prefix, p))
            .WithMarkers(VisitMarkers(gotoStatement.Markers, p));

        var stmtResult = VisitStatement(gotoStatement, p);
        if (stmtResult is not GotoStatement node) return stmtResult;

        return node
            .WithCaseOrDefaultKeyword((Keyword?)Visit(node.CaseOrDefaultKeyword, p))
            .WithTarget((Expression?)Visit(node.Target, p));
    }

    public virtual J VisitEventDeclaration(EventDeclaration eventDeclaration, P p)
    {
        eventDeclaration = eventDeclaration
            .WithPrefix(VisitSpace(eventDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(eventDeclaration.Markers, p));

        var stmtResult = VisitStatement(eventDeclaration, p);
        if (stmtResult is not EventDeclaration node) return stmtResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithTypeExpressionPadded(VisitLeftPadded(node.TypeExpressionPadded, p)!)
            .WithName((Identifier)Visit(node.Name, p)!)
            .WithInterfaceSpecifier(VisitRightPadded(node.InterfaceSpecifier, p))
            .WithAccessors(VisitContainer(node.Accessors, p));
    }

    public virtual J VisitCsBinary(CsBinary csBinary, P p)
    {
        csBinary = csBinary
            .WithPrefix(VisitSpace(csBinary.Prefix, p))
            .WithMarkers(VisitMarkers(csBinary.Markers, p));

        var exprResult = VisitExpression(csBinary, p);
        if (exprResult is not CsBinary node) return exprResult;

        return node
            .WithLeft((Expression)Visit(node.Left, p)!)
            .WithRight((Expression)Visit(node.Right, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitCollectionExpression(CollectionExpression collectionExpression, P p)
    {
        collectionExpression = collectionExpression
            .WithPrefix(VisitSpace(collectionExpression.Prefix, p))
            .WithMarkers(VisitMarkers(collectionExpression.Markers, p));

        var exprResult = VisitExpression(collectionExpression, p);
        if (exprResult is not CollectionExpression node) return exprResult;

        return node
            .WithElements(ListUtils.Map(node.Elements, e => VisitRightPadded(e, p)))
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, P p)
    {
        forEachVariableLoop = forEachVariableLoop
            .WithPrefix(VisitSpace(forEachVariableLoop.Prefix, p))
            .WithMarkers(VisitMarkers(forEachVariableLoop.Markers, p));

        var stmtResult = VisitStatement(forEachVariableLoop, p);
        if (stmtResult is not ForEachVariableLoop node) return stmtResult;

        return node
            .WithControlElement((ForEachVariableLoopControl)Visit(node.ControlElement, p)!)
            .WithBody(VisitRightPadded(node.Body, p)!);
    }

    public virtual J VisitForEachVariableLoopControl(ForEachVariableLoopControl control, P p)
    {
        return control
            .WithPrefix(VisitSpace(control.Prefix, p))
            .WithMarkers(VisitMarkers(control.Markers, p))
            .WithVariable(VisitRightPadded(control.Variable, p)!)
            .WithIterable(VisitRightPadded(control.Iterable, p)!);
    }

    public virtual J VisitUsingStatement(UsingStatement usingStatement, P p)
    {
        usingStatement = usingStatement
            .WithPrefix(VisitSpace(usingStatement.Prefix, p))
            .WithMarkers(VisitMarkers(usingStatement.Markers, p));

        var stmtResult = VisitStatement(usingStatement, p);
        if (stmtResult is not UsingStatement node) return stmtResult;

        return node
            .WithExpressionPadded(VisitLeftPadded(node.ExpressionPadded, p)!)
            .WithStatement((Statement)Visit(node.Statement, p)!);
    }

    public virtual J VisitAllowsConstraintClause(AllowsConstraintClause clause, P p)
    {
        clause = clause
            .WithPrefix(VisitSpace(clause.Prefix, p))
            .WithMarkers(VisitMarkers(clause.Markers, p));

        var exprResult = VisitExpression(clause, p);
        if (exprResult is not AllowsConstraintClause node) return exprResult;

        return node
            .WithExpressions(VisitContainer(node.Expressions, p)!);
    }

    public virtual J VisitRefStructConstraint(RefStructConstraint constraint, P p)
    {
        constraint = constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));

        var exprResult = VisitExpression(constraint, p);
        if (exprResult is not RefStructConstraint node) return exprResult;

        return node;
    }

    public virtual J VisitClassOrStructConstraint(ClassOrStructConstraint constraint, P p)
    {
        constraint = constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));

        var exprResult = VisitExpression(constraint, p);
        if (exprResult is not ClassOrStructConstraint node) return exprResult;

        return node;
    }

    public virtual J VisitConstructorConstraint(ConstructorConstraint constraint, P p)
    {
        constraint = constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));

        var exprResult = VisitExpression(constraint, p);
        if (exprResult is not ConstructorConstraint node) return exprResult;

        return node;
    }

    public virtual J VisitDefaultConstraint(DefaultConstraint constraint, P p)
    {
        constraint = constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));

        var exprResult = VisitExpression(constraint, p);
        if (exprResult is not DefaultConstraint node) return exprResult;

        return node;
    }

    public virtual J VisitSingleVariableDesignation(SingleVariableDesignation designation, P p)
    {
        designation = designation
            .WithPrefix(VisitSpace(designation.Prefix, p))
            .WithMarkers(VisitMarkers(designation.Markers, p));

        var exprResult = VisitExpression(designation, p);
        if (exprResult is not SingleVariableDesignation node) return exprResult;

        return node
            .WithName((Identifier)Visit(node.Name, p)!);
    }

    public virtual J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation designation, P p)
    {
        designation = designation
            .WithPrefix(VisitSpace(designation.Prefix, p))
            .WithMarkers(VisitMarkers(designation.Markers, p));

        var exprResult = VisitExpression(designation, p);
        if (exprResult is not ParenthesizedVariableDesignation node) return exprResult;

        return node
            .WithVariables(VisitContainer(node.Variables, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitDiscardVariableDesignation(DiscardVariableDesignation designation, P p)
    {
        designation = designation
            .WithPrefix(VisitSpace(designation.Prefix, p))
            .WithMarkers(VisitMarkers(designation.Markers, p));

        var exprResult = VisitExpression(designation, p);
        if (exprResult is not DiscardVariableDesignation node) return exprResult;

        return node
            .WithDiscard((Identifier)Visit(node.Discard, p)!);
    }

    public virtual J VisitCsUnary(CsUnary csUnary, P p)
    {
        csUnary = csUnary
            .WithPrefix(VisitSpace(csUnary.Prefix, p))
            .WithMarkers(VisitMarkers(csUnary.Markers, p));

        var stmtResult = VisitStatement(csUnary, p);
        if (stmtResult is not CsUnary s1) return stmtResult;

        var exprResult = VisitExpression(s1, p);
        if (exprResult is not CsUnary node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitTupleElement(TupleElement tupleElement, P p)
    {
        return tupleElement
            .WithPrefix(VisitSpace(tupleElement.Prefix, p))
            .WithMarkers(VisitMarkers(tupleElement.Markers, p))
            .WithElementType((TypeTree)Visit(tupleElement.ElementType, p)!)
            .WithName((Identifier?)Visit(tupleElement.Name, p));
    }

    public virtual J VisitConstantPattern(ConstantPattern constantPattern, P p)
    {
        constantPattern = constantPattern
            .WithPrefix(VisitSpace(constantPattern.Prefix, p))
            .WithMarkers(VisitMarkers(constantPattern.Markers, p));

        var exprResult = VisitExpression(constantPattern, p);
        if (exprResult is not ConstantPattern node) return exprResult;

        return node
            .WithValue((Expression)Visit(node.Value, p)!);
    }

    public virtual J VisitDiscardPattern(DiscardPattern discardPattern, P p)
    {
        discardPattern = discardPattern
            .WithPrefix(VisitSpace(discardPattern.Prefix, p))
            .WithMarkers(VisitMarkers(discardPattern.Markers, p));

        var exprResult = VisitExpression(discardPattern, p);
        if (exprResult is not DiscardPattern node) return exprResult;

        return node
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, P p)
    {
        implicitElementAccess = implicitElementAccess
            .WithPrefix(VisitSpace(implicitElementAccess.Prefix, p))
            .WithMarkers(VisitMarkers(implicitElementAccess.Markers, p));

        var exprResult = VisitExpression(implicitElementAccess, p);
        if (exprResult is not ImplicitElementAccess node) return exprResult;

        return node
            .WithArgumentList(VisitContainer(node.ArgumentList, p)!);
    }

    public virtual J VisitSlicePattern(SlicePattern slicePattern, P p)
    {
        slicePattern = slicePattern
            .WithPrefix(VisitSpace(slicePattern.Prefix, p))
            .WithMarkers(VisitMarkers(slicePattern.Markers, p));

        var exprResult = VisitExpression(slicePattern, p);
        if (exprResult is not SlicePattern node) return exprResult;

        return node;
    }

    public virtual J VisitListPattern(ListPattern listPattern, P p)
    {
        listPattern = listPattern
            .WithPrefix(VisitSpace(listPattern.Prefix, p))
            .WithMarkers(VisitMarkers(listPattern.Markers, p));

        var exprResult = VisitExpression(listPattern, p);
        if (exprResult is not ListPattern node) return exprResult;

        return node
            .WithPatterns(VisitContainer(node.Patterns, p)!)
            .WithDesignation((VariableDesignation?)Visit(node.Designation, p));
    }

    public virtual J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        switchExpression = switchExpression
            .WithPrefix(VisitSpace(switchExpression.Prefix, p))
            .WithMarkers(VisitMarkers(switchExpression.Markers, p));

        var exprResult = VisitExpression(switchExpression, p);
        if (exprResult is not SwitchExpression node) return exprResult;

        return node
            .WithExpressionPadded(VisitRightPadded(node.ExpressionPadded, p)!)
            .WithArms(VisitContainer(node.Arms, p)!);
    }

    public virtual J VisitSwitchExpressionArm(SwitchExpressionArm arm, P p)
    {
        return arm
            .WithPrefix(VisitSpace(arm.Prefix, p))
            .WithMarkers(VisitMarkers(arm.Markers, p))
            .WithPattern((J)Visit(arm.Pattern, p)!)
            .WithWhenExpression(VisitLeftPadded(arm.WhenExpression, p))
            .WithExpressionPadded(VisitLeftPadded(arm.ExpressionPadded, p)!);
    }

    public virtual J VisitCheckedExpression(CheckedExpression checkedExpression, P p)
    {
        checkedExpression = checkedExpression
            .WithPrefix(VisitSpace(checkedExpression.Prefix, p))
            .WithMarkers(VisitMarkers(checkedExpression.Markers, p));

        var exprResult = VisitExpression(checkedExpression, p);
        if (exprResult is not CheckedExpression node) return exprResult;

        return node
            .WithCheckedOrUncheckedKeyword((Keyword)Visit(node.CheckedOrUncheckedKeyword, p)!)
            .WithExpressionValue((ControlParentheses<Expression>)Visit(node.ExpressionValue, p)!);
    }

    public virtual J VisitCheckedStatement(CheckedStatement checkedStatement, P p)
    {
        checkedStatement = checkedStatement
            .WithPrefix(VisitSpace(checkedStatement.Prefix, p))
            .WithMarkers(VisitMarkers(checkedStatement.Markers, p));

        var stmtResult = VisitStatement(checkedStatement, p);
        if (stmtResult is not CheckedStatement node) return stmtResult;

        return node
            .WithKeywordValue((Keyword)Visit(node.KeywordValue, p)!)
            .WithBlock((Block)Visit(node.Block, p)!);
    }

    public virtual J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        rangeExpression = rangeExpression
            .WithPrefix(VisitSpace(rangeExpression.Prefix, p))
            .WithMarkers(VisitMarkers(rangeExpression.Markers, p));

        var exprResult = VisitExpression(rangeExpression, p);
        if (exprResult is not RangeExpression node) return exprResult;

        return node
            .WithStart(VisitRightPadded(node.Start, p))
            .WithEnd((Expression?)Visit(node.End, p));
    }

    public virtual J VisitIndexerDeclaration(IndexerDeclaration indexerDeclaration, P p)
    {
        indexerDeclaration = indexerDeclaration
            .WithPrefix(VisitSpace(indexerDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(indexerDeclaration.Markers, p));

        var stmtResult = VisitStatement(indexerDeclaration, p);
        if (stmtResult is not IndexerDeclaration node) return stmtResult;

        return node
            .WithTypeExpression((TypeTree)Visit(node.TypeExpression, p)!)
            .WithExplicitInterfaceSpecifier(VisitRightPadded(node.ExplicitInterfaceSpecifier, p))
            .WithIndexer((Expression)Visit(node.Indexer, p)!)
            .WithParameters(VisitContainer(node.Parameters, p)!)
            .WithExpressionBody(VisitLeftPadded(node.ExpressionBody, p))
            .WithAccessors((Block?)Visit(node.Accessors, p));
    }

    public virtual J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, P p)
    {
        delegateDeclaration = delegateDeclaration
            .WithPrefix(VisitSpace(delegateDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(delegateDeclaration.Markers, p));

        var stmtResult = VisitStatement(delegateDeclaration, p);
        if (stmtResult is not DelegateDeclaration node) return stmtResult;

        return node
            .WithAttributes(ListUtils.Map(node.Attributes, al => Visit(al, p) as AttributeList))
            .WithReturnType(VisitLeftPadded(node.ReturnType, p)!)
            .WithIdentifierName((Identifier)Visit(node.IdentifierName, p)!)
            .WithTypeParameters(VisitContainer(node.TypeParameters, p))
            .WithParameters(VisitContainer(node.Parameters, p)!);
    }

    public virtual J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
    {
        conversion = conversion
            .WithPrefix(VisitSpace(conversion.Prefix, p))
            .WithMarkers(VisitMarkers(conversion.Markers, p));

        var stmtResult = VisitStatement(conversion, p);
        if (stmtResult is not ConversionOperatorDeclaration node) return stmtResult;

        return node
            .WithInterfaceSpecifier(VisitRightPadded(node.InterfaceSpecifier, p))
            .WithReturnType(VisitLeftPadded(node.ReturnType, p)!)
            .WithParameters(VisitContainer(node.Parameters, p)!)
            .WithExpressionBody(VisitLeftPadded(node.ExpressionBody, p))
            .WithBody((Block?)Visit(node.Body, p));
    }

    public virtual J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
    {
        operatorDeclaration = operatorDeclaration
            .WithPrefix(VisitSpace(operatorDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(operatorDeclaration.Markers, p));

        var stmtResult = VisitStatement(operatorDeclaration, p);
        if (stmtResult is not OperatorDeclaration node) return stmtResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithExplicitInterfaceSpecifier(VisitRightPadded(node.ExplicitInterfaceSpecifier, p))
            .WithOperatorKeyword((Keyword)Visit(node.OperatorKeyword, p)!)
            .WithCheckedKeyword((Keyword?)Visit(node.CheckedKeyword, p))
            .WithReturnType((TypeTree)Visit(node.ReturnType, p)!)
            .WithParameters(VisitContainer(node.Parameters, p)!)
            .WithBody((Block)Visit(node.Body, p)!)
            .WithMethodType((JavaType.Method?)VisitType(node.MethodType, p));
    }

    public virtual J VisitEnumDeclaration(EnumDeclaration enumDeclaration, P p)
    {
        enumDeclaration = enumDeclaration
            .WithPrefix(VisitSpace(enumDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(enumDeclaration.Markers, p));

        var stmtResult = VisitStatement(enumDeclaration, p);
        if (stmtResult is not EnumDeclaration node) return stmtResult;

        return node
            .WithAttributeLists(node.AttributeLists != null
                ? ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList)
                : null)
            .WithNamePadded(VisitLeftPadded(node.NamePadded, p)!)
            .WithBaseType(VisitLeftPadded(node.BaseType, p))
            .WithMembers(VisitContainer(node.Members, p));
    }

    public virtual J VisitEnumMemberDeclaration(EnumMemberDeclaration enumMember, P p)
    {
        enumMember = enumMember
            .WithPrefix(VisitSpace(enumMember.Prefix, p))
            .WithMarkers(VisitMarkers(enumMember.Markers, p));

        var exprResult = VisitExpression(enumMember, p);
        if (exprResult is not EnumMemberDeclaration node) return exprResult;

        return node
            .WithAttributeLists(ListUtils.Map(node.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithName((Identifier)Visit(node.Name, p)!)
            .WithInitializer(VisitLeftPadded(node.Initializer, p));
    }

    public virtual J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, P p)
    {
        aliasQualifiedName = aliasQualifiedName
            .WithPrefix(VisitSpace(aliasQualifiedName.Prefix, p))
            .WithMarkers(VisitMarkers(aliasQualifiedName.Markers, p));

        var exprResult = VisitExpression(aliasQualifiedName, p);
        if (exprResult is not AliasQualifiedName node) return exprResult;

        return node
            .WithAlias(VisitRightPadded(node.Alias, p)!)
            .WithName((Expression)Visit(node.Name, p)!);
    }

    public virtual J VisitPointerDereference(PointerDereference pointerDereference, P p)
    {
        pointerDereference = pointerDereference
            .WithPrefix(VisitSpace(pointerDereference.Prefix, p))
            .WithMarkers(VisitMarkers(pointerDereference.Markers, p));

        var exprResult = VisitExpression(pointerDereference, p);
        if (exprResult is not PointerDereference node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, P p)
    {
        pointerFieldAccess = pointerFieldAccess
            .WithPrefix(VisitSpace(pointerFieldAccess.Prefix, p))
            .WithMarkers(VisitMarkers(pointerFieldAccess.Markers, p));

        var exprResult = VisitExpression(pointerFieldAccess, p);
        if (exprResult is not PointerFieldAccess e1) return exprResult;

        var stmtResult = VisitStatement(e1, p);
        if (stmtResult is not PointerFieldAccess node) return stmtResult;

        return node
            .WithTarget((Expression)Visit(node.Target, p)!)
            .WithNamePadded(VisitLeftPadded(node.NamePadded, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitRefType(RefType refType, P p)
    {
        refType = refType
            .WithPrefix(VisitSpace(refType.Prefix, p))
            .WithMarkers(VisitMarkers(refType.Markers, p));

        var exprResult = VisitExpression(refType, p);
        if (exprResult is not RefType node) return exprResult;

        return node
            .WithTypeIdentifier((TypeTree)Visit(node.TypeIdentifier, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    // ---- LINQ ----

    public virtual J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        queryExpression = queryExpression
            .WithPrefix(VisitSpace(queryExpression.Prefix, p))
            .WithMarkers(VisitMarkers(queryExpression.Markers, p));

        var exprResult = VisitExpression(queryExpression, p);
        if (exprResult is not QueryExpression node) return exprResult;

        return node
            .WithFromClause((FromClause)Visit(node.FromClause, p)!)
            .WithBody((QueryBody)Visit(node.Body, p)!);
    }

    public virtual J VisitQueryBody(QueryBody queryBody, P p)
    {
        return queryBody
            .WithPrefix(VisitSpace(queryBody.Prefix, p))
            .WithMarkers(VisitMarkers(queryBody.Markers, p))
            .WithClauses(ListUtils.Map(queryBody.Clauses, c => (QueryClause?)Visit(c, p)))
            .WithSelectOrGroup((SelectOrGroupClause?)Visit(queryBody.SelectOrGroup, p))
            .WithContinuation(queryBody.Continuation != null ? (QueryContinuation)VisitQueryContinuation(queryBody.Continuation, p) : null);
    }

    public virtual J VisitFromClause(FromClause fromClause, P p)
    {
        fromClause = fromClause
            .WithPrefix(VisitSpace(fromClause.Prefix, p))
            .WithMarkers(VisitMarkers(fromClause.Markers, p));

        var exprResult = VisitExpression(fromClause, p);
        if (exprResult is not FromClause node) return exprResult;

        return node
            .WithTypeIdentifier((TypeTree?)Visit(node.TypeIdentifier, p))
            .WithIdentifierPadded(VisitRightPadded(node.IdentifierPadded, p)!)
            .WithExpression((Expression)Visit(node.Expression, p)!);
    }

    public virtual J VisitLetClause(LetClause letClause, P p)
    {
        return letClause
            .WithPrefix(VisitSpace(letClause.Prefix, p))
            .WithMarkers(VisitMarkers(letClause.Markers, p))
            .WithIdentifierPadded(VisitRightPadded(letClause.IdentifierPadded, p)!)
            .WithExpression((Expression)Visit(letClause.Expression, p)!);
    }

    public virtual J VisitJoinClause(JoinClause joinClause, P p)
    {
        return joinClause
            .WithPrefix(VisitSpace(joinClause.Prefix, p))
            .WithMarkers(VisitMarkers(joinClause.Markers, p))
            .WithIdentifierPadded(VisitRightPadded(joinClause.IdentifierPadded, p)!)
            .WithInExpression(VisitRightPadded(joinClause.InExpression, p)!)
            .WithLeftExpression(VisitRightPadded(joinClause.LeftExpression, p)!)
            .WithRightExpression((Expression)Visit(joinClause.RightExpression, p)!)
            .WithInto(VisitLeftPadded(joinClause.Into, p));
    }

    public virtual J VisitJoinIntoClause(JoinIntoClause joinIntoClause, P p)
    {
        return joinIntoClause
            .WithPrefix(VisitSpace(joinIntoClause.Prefix, p))
            .WithMarkers(VisitMarkers(joinIntoClause.Markers, p))
            .WithIdentifier((Identifier)Visit(joinIntoClause.Identifier, p)!);
    }

    public virtual J VisitWhereClause(WhereClause whereClause, P p)
    {
        return whereClause
            .WithPrefix(VisitSpace(whereClause.Prefix, p))
            .WithMarkers(VisitMarkers(whereClause.Markers, p))
            .WithCondition((Expression)Visit(whereClause.Condition, p)!);
    }

    public virtual J VisitOrderByClause(OrderByClause orderByClause, P p)
    {
        return orderByClause
            .WithPrefix(VisitSpace(orderByClause.Prefix, p))
            .WithMarkers(VisitMarkers(orderByClause.Markers, p))
            .WithOrderings(ListUtils.Map(orderByClause.Orderings, o => VisitRightPadded(o, p)));
    }

    public virtual J VisitOrdering(Ordering ordering, P p)
    {
        return ordering
            .WithPrefix(VisitSpace(ordering.Prefix, p))
            .WithMarkers(VisitMarkers(ordering.Markers, p))
            .WithExpressionPadded(VisitRightPadded(ordering.ExpressionPadded, p)!);
    }

    public virtual J VisitSelectClause(SelectClause selectClause, P p)
    {
        return selectClause
            .WithPrefix(VisitSpace(selectClause.Prefix, p))
            .WithMarkers(VisitMarkers(selectClause.Markers, p))
            .WithExpression((Expression)Visit(selectClause.Expression, p)!);
    }

    public virtual J VisitGroupClause(GroupClause groupClause, P p)
    {
        return groupClause
            .WithPrefix(VisitSpace(groupClause.Prefix, p))
            .WithMarkers(VisitMarkers(groupClause.Markers, p))
            .WithGroupExpression(VisitRightPadded(groupClause.GroupExpression, p)!)
            .WithKey((Expression)Visit(groupClause.Key, p)!);
    }

    public virtual J VisitQueryContinuation(QueryContinuation queryContinuation, P p)
    {
        return queryContinuation
            .WithPrefix(VisitSpace(queryContinuation.Prefix, p))
            .WithMarkers(VisitMarkers(queryContinuation.Markers, p))
            .WithIdentifier((Identifier)Visit(queryContinuation.Identifier, p)!)
            .WithBody((QueryBody)VisitQueryBody(queryContinuation.Body, p));
    }

    public virtual J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression anonymousObject, P p)
    {
        anonymousObject = anonymousObject
            .WithPrefix(VisitSpace(anonymousObject.Prefix, p))
            .WithMarkers(VisitMarkers(anonymousObject.Markers, p));

        var exprResult = VisitExpression(anonymousObject, p);
        if (exprResult is not AnonymousObjectCreationExpression node) return exprResult;

        return node
            .WithInitializers(VisitContainer(node.Initializers, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitWithExpression(WithExpression withExpression, P p)
    {
        withExpression = withExpression
            .WithPrefix(VisitSpace(withExpression.Prefix, p))
            .WithMarkers(VisitMarkers(withExpression.Markers, p));

        var exprResult = VisitExpression(withExpression, p);
        if (exprResult is not WithExpression node) return exprResult;

        return node
            .WithTarget((Expression)Visit(node.Target, p)!)
            .WithInitializerPadded(VisitLeftPadded(node.InitializerPadded, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitSpreadExpression(SpreadExpression spreadExpression, P p)
    {
        spreadExpression = spreadExpression
            .WithPrefix(VisitSpace(spreadExpression.Prefix, p))
            .WithMarkers(VisitMarkers(spreadExpression.Markers, p));

        var exprResult = VisitExpression(spreadExpression, p);
        if (exprResult is not SpreadExpression node) return exprResult;

        return node
            .WithExpression((Expression)Visit(node.Expression, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitFunctionPointerType(FunctionPointerType functionPointerType, P p)
    {
        functionPointerType = functionPointerType
            .WithPrefix(VisitSpace(functionPointerType.Prefix, p))
            .WithMarkers(VisitMarkers(functionPointerType.Markers, p));

        var exprResult = VisitExpression(functionPointerType, p);
        if (exprResult is not FunctionPointerType node) return exprResult;

        return node
            .WithUnmanagedCallingConventionTypes(VisitContainer(node.UnmanagedCallingConventionTypes, p))
            .WithParameterTypes(VisitContainer(node.ParameterTypes, p)!)
            .WithType((JavaType?)VisitType(node.Type, p));
    }

    public virtual J VisitTypeWithArguments(TypeWithArguments twa, P p)
    {
        twa = twa
            .WithPrefix(VisitSpace(twa.Prefix, p))
            .WithMarkers(VisitMarkers(twa.Markers, p));

        var exprResult = VisitExpression(twa, p);
        if (exprResult is not TypeWithArguments node) return exprResult;

        return node
            .WithTypeExpression((TypeTree)Visit(node.TypeExpression, p)!)
            .WithArguments(VisitContainer(node.Arguments, p)!);
    }

    public virtual J VisitWhenClause(WhenClause whenClause, P p)
    {
        whenClause = whenClause
            .WithPrefix(VisitSpace(whenClause.Prefix, p))
            .WithMarkers(VisitMarkers(whenClause.Markers, p));

        var exprResult = VisitExpression(whenClause, p);
        if (exprResult is not WhenClause node) return exprResult;

        node = node
            .WithCondition((ControlParentheses<Expression>)Visit(node.Condition, p)!);

        return node;
    }

    public virtual J VisitExplicitInterfaceMember(ExplicitInterfaceMember eim, P p)
    {
        eim = eim
            .WithPrefix(VisitSpace(eim.Prefix, p))
            .WithMarkers(VisitMarkers(eim.Markers, p));

        var stmtResult = VisitStatement(eim, p);
        if (stmtResult is not ExplicitInterfaceMember node) return stmtResult;

        return node
            .WithInterfaceSpecifier(VisitRightPadded(node.InterfaceSpecifier, p)!)
            .WithMethodDeclaration((MethodDeclaration)Visit(node.MethodDeclaration, p)!);
    }

    /// <summary>
    /// Auto-formats the given tree node using Roslyn within the enclosing compilation unit.
    /// For subtrees, formatting is deferred to a single batch pass after the visitor completes,
    /// avoiding O(N × file_size) cost when many nodes are formatted in the same file.
    /// For the CompilationUnit itself, formats immediately (no benefit to deferring).
    /// </summary>
    protected T AutoFormat<T>(T tree, P p, Cursor cursor) where T : class, J
    {
        // Full CU formatting — do it immediately, no benefit to deferring
        if (tree is CompilationUnit cu)
            return (T)(J)RoslynFormatter.Format(cu);

        _deferredFormat ??= new RoslynFormatter.DeferredFormatVisitor<P>();
        _deferredFormat.Add(tree);
        MaybeDoAfterVisit(_deferredFormat);
        return tree;
    }

    /// <summary>
    /// Auto-formats the given tree node if it differs from the original (before).
    /// Formatting is deferred to a single batch pass after the visitor completes.
    /// </summary>
    protected T MaybeAutoFormat<T>(T before, T after, P p, Cursor cursor) where T : class, J
    {
        return ReferenceEquals(before, after) ? after : AutoFormat(after, p, cursor);
    }

    /// <summary>
    /// Auto-formats the given tree node if it differs from the original (before),
    /// stopping after the specified node. This overload formats immediately (not deferred)
    /// because stopAfter scoping is not supported in batch mode.
    /// </summary>
    protected T MaybeAutoFormat<T>(T before, T after, J? stopAfter, P p, Cursor cursor) where T : class, J
    {
        if (ReferenceEquals(before, after)) return after;
        var visitor = new AutoFormatVisitor<int>(stopAfter);
        return visitor.Format(after, cursor) as T ?? after;
    }

    private RoslynFormatter.DeferredFormatVisitor<P>? _deferredFormat;
}
