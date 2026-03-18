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
            TypeWithArguments twa => VisitTypeWithArguments(twa, p),
            ExplicitInterfaceMember eim => VisitExplicitInterfaceMember(eim, p),
            ExceptionFilteredTry eft => VisitExceptionFilteredTry(eft, p),
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
            .WithMembers(ListUtils.Map(compilationUnit.Members, m => VisitRightPadded(m, p)))
            .WithEof(VisitSpace(compilationUnit.Eof, p));
    }

    public virtual J VisitUsingDirective(UsingDirective usingDirective, P p)
    {
        usingDirective = (UsingDirective)VisitStatement(usingDirective, p);
        return usingDirective
            .WithPrefix(VisitSpace(usingDirective.Prefix, p))
            .WithMarkers(VisitMarkers(usingDirective.Markers, p))
            .WithAlias(VisitRightPadded(usingDirective.Alias, p))
            .WithNamespaceOrType(Visit(usingDirective.NamespaceOrType, p) as TypeTree ?? usingDirective.NamespaceOrType);
    }

    public virtual J VisitPropertyDeclaration(PropertyDeclaration prop, P p)
    {
        prop = (PropertyDeclaration)VisitStatement(prop, p);
        return prop
            .WithPrefix(VisitSpace(prop.Prefix, p))
            .WithMarkers(VisitMarkers(prop.Markers, p))
            .WithAttributeLists(ListUtils.Map(prop.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithTypeExpression(Visit(prop.TypeExpression, p) as TypeTree ?? prop.TypeExpression)
            .WithInterfaceSpecifier(VisitRightPadded(prop.InterfaceSpecifier, p))
            .WithName(Visit(prop.Name, p) as Identifier ?? prop.Name)
            .WithAccessors(prop.Accessors != null ? Visit(prop.Accessors, p) as Block ?? prop.Accessors : null)
            .WithExpressionBody(VisitLeftPadded(prop.ExpressionBody, p))
            .WithInitializer(VisitLeftPadded(prop.Initializer, p));
    }

    public virtual J VisitAccessorDeclaration(AccessorDeclaration accessor, P p)
    {
        accessor = (AccessorDeclaration)VisitStatement(accessor, p);
        return accessor
            .WithPrefix(VisitSpace(accessor.Prefix, p))
            .WithMarkers(VisitMarkers(accessor.Markers, p))
            .WithAttributeLists(ListUtils.Map(accessor.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithBody(accessor.Body != null ? Visit(accessor.Body, p) as Block ?? accessor.Body : null)
            .WithExpressionBody(VisitLeftPadded(accessor.ExpressionBody, p));
    }

    public virtual J VisitAttributeList(AttributeList attrList, P p)
    {
        attrList = (AttributeList)VisitStatement(attrList, p);
        return attrList
            .WithPrefix(VisitSpace(attrList.Prefix, p))
            .WithMarkers(VisitMarkers(attrList.Markers, p))
            .WithTarget(VisitRightPadded(attrList.Target, p))
            .WithAttributes(ListUtils.Map(attrList.Attributes, a => VisitRightPadded(a, p)));
    }

    public virtual J VisitNamedExpression(NamedExpression ne, P p)
    {
        ne = (NamedExpression)VisitExpression(ne, p);
        return ne
            .WithPrefix(VisitSpace(ne.Prefix, p))
            .WithMarkers(VisitMarkers(ne.Markers, p))
            .WithName(VisitRightPadded(ne.Name, p) ?? ne.Name)
            .WithExpression(Visit(ne.Expression, p) as Expression ?? ne.Expression);
    }

    public virtual J VisitRefExpression(RefExpression re, P p)
    {
        re = (RefExpression)VisitExpression(re, p);
        return re
            .WithPrefix(VisitSpace(re.Prefix, p))
            .WithMarkers(VisitMarkers(re.Markers, p))
            .WithExpression(Visit(re.Expression, p) as Expression ?? re.Expression);
    }

    public virtual J VisitDeclarationExpression(DeclarationExpression de, P p)
    {
        de = (DeclarationExpression)VisitExpression(de, p);
        return de
            .WithPrefix(VisitSpace(de.Prefix, p))
            .WithMarkers(VisitMarkers(de.Markers, p))
            .WithTypeExpression(de.TypeExpression != null ? Visit(de.TypeExpression, p) as TypeTree ?? de.TypeExpression : null)
            .WithVariables(Visit(de.Variables, p) as Expression ?? de.Variables);
    }

    public virtual J VisitIsPattern(IsPattern isPattern, P p)
    {
        isPattern = (IsPattern)VisitExpression(isPattern, p);
        return isPattern
            .WithPrefix(VisitSpace(isPattern.Prefix, p))
            .WithMarkers(VisitMarkers(isPattern.Markers, p))
            .WithExpression(Visit(isPattern.Expression, p) as Expression ?? isPattern.Expression)
            .WithPattern(VisitLeftPadded(isPattern.Pattern, p) ?? isPattern.Pattern);
    }

    public virtual J VisitStatementExpression(StatementExpression se, P p)
    {
        // Pattern extends Expression, NOT Statement despite the name
        se = (StatementExpression)VisitExpression(se, p);
        return se
            .WithPrefix(VisitSpace(se.Prefix, p))
            .WithMarkers(VisitMarkers(se.Markers, p))
            .WithStatement(Visit(se.Statement, p) as Statement ?? se.Statement);
    }

    public virtual J VisitSizeOf(SizeOf sizeOf, P p)
    {
        sizeOf = (SizeOf)VisitExpression(sizeOf, p);
        return sizeOf
            .WithPrefix(VisitSpace(sizeOf.Prefix, p))
            .WithMarkers(VisitMarkers(sizeOf.Markers, p))
            .WithExpression(Visit(sizeOf.Expression, p) as Expression ?? sizeOf.Expression)
            .WithType((JavaType?)VisitType(sizeOf.Type, p));
    }

    public virtual J VisitUnsafeStatement(UnsafeStatement unsafeStatement, P p)
    {
        unsafeStatement = (UnsafeStatement)VisitStatement(unsafeStatement, p);
        return unsafeStatement
            .WithPrefix(VisitSpace(unsafeStatement.Prefix, p))
            .WithMarkers(VisitMarkers(unsafeStatement.Markers, p))
            .WithBlock(Visit(unsafeStatement.Block, p) as Block ?? unsafeStatement.Block);
    }

    public virtual J VisitPointerType(PointerType pointerType, P p)
    {
        pointerType = (PointerType)VisitExpression(pointerType, p);
        return pointerType
            .WithPrefix(VisitSpace(pointerType.Prefix, p))
            .WithMarkers(VisitMarkers(pointerType.Markers, p))
            .WithElementType(VisitRightPadded(pointerType.ElementType, p) ?? pointerType.ElementType);
    }

    public virtual J VisitFixedStatement(FixedStatement fixedStatement, P p)
    {
        fixedStatement = (FixedStatement)VisitStatement(fixedStatement, p);
        return fixedStatement
            .WithPrefix(VisitSpace(fixedStatement.Prefix, p))
            .WithMarkers(VisitMarkers(fixedStatement.Markers, p))
            .WithDeclarations(Visit(fixedStatement.Declarations, p) as ControlParentheses<VariableDeclarations> ?? fixedStatement.Declarations)
            .WithBlock(Visit(fixedStatement.Block, p) as Block ?? fixedStatement.Block);
    }

    public virtual J VisitExternAlias(ExternAlias externAlias, P p)
    {
        externAlias = (ExternAlias)VisitStatement(externAlias, p);
        return externAlias
            .WithPrefix(VisitSpace(externAlias.Prefix, p))
            .WithMarkers(VisitMarkers(externAlias.Markers, p))
            .WithIdentifier(VisitLeftPadded(externAlias.Identifier, p)!);
    }

    public virtual J VisitInitializerExpression(InitializerExpression initializerExpression, P p)
    {
        initializerExpression = (InitializerExpression)VisitExpression(initializerExpression, p);
        return initializerExpression
            .WithPrefix(VisitSpace(initializerExpression.Prefix, p))
            .WithMarkers(VisitMarkers(initializerExpression.Markers, p))
            .WithExpressions(VisitContainer(initializerExpression.Expressions, p) ?? initializerExpression.Expressions);
    }

    public virtual J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, P p)
    {
        nullSafeExpression = (NullSafeExpression)VisitExpression(nullSafeExpression, p);
        return nullSafeExpression
            .WithPrefix(VisitSpace(nullSafeExpression.Prefix, p))
            .WithMarkers(VisitMarkers(nullSafeExpression.Markers, p))
            .WithExpressionPadded(VisitRightPadded(nullSafeExpression.ExpressionPadded, p) ?? nullSafeExpression.ExpressionPadded);
    }

    public virtual J VisitDefaultExpression(DefaultExpression defaultExpression, P p)
    {
        defaultExpression = (DefaultExpression)VisitExpression(defaultExpression, p);
        return defaultExpression
            .WithPrefix(VisitSpace(defaultExpression.Prefix, p))
            .WithMarkers(VisitMarkers(defaultExpression.Markers, p))
            .WithTypeOperator(VisitContainer(defaultExpression.TypeOperator, p));
    }

    public virtual J VisitCsLambda(CsLambda csLambda, P p)
    {
        csLambda = (CsLambda)VisitStatement(csLambda, p);
        csLambda = (CsLambda)VisitExpression(csLambda, p);
        return csLambda
            .WithPrefix(VisitSpace(csLambda.Prefix, p))
            .WithMarkers(VisitMarkers(csLambda.Markers, p))
            .WithAttributeLists(ListUtils.Map(csLambda.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithReturnType(csLambda.ReturnType != null ? Visit(csLambda.ReturnType, p) as TypeTree ?? csLambda.ReturnType : null)
            .WithLambdaExpression(VisitLambda(csLambda.LambdaExpression, p) as Lambda ?? csLambda.LambdaExpression);
    }

    public virtual J VisitRelationalPattern(RelationalPattern rp, P p)
    {
        rp = (RelationalPattern)VisitExpression(rp, p);
        return rp
            .WithPrefix(VisitSpace(rp.Prefix, p))
            .WithMarkers(VisitMarkers(rp.Markers, p))
            .WithValue(Visit(rp.Value, p) as Expression ?? rp.Value);
    }

    public virtual J VisitPropertyPattern(PropertyPattern pp, P p)
    {
        pp = (PropertyPattern)VisitExpression(pp, p);
        return pp
            .WithPrefix(VisitSpace(pp.Prefix, p))
            .WithMarkers(VisitMarkers(pp.Markers, p))
            .WithTypeQualifier(pp.TypeQualifier != null ? Visit(pp.TypeQualifier, p) as TypeTree ?? pp.TypeQualifier : null)
            .WithSubpatterns(VisitContainer(pp.Subpatterns, p)!)
            .WithDesignation(pp.Designation != null ? Visit(pp.Designation, p) as Identifier ?? pp.Designation : null);
    }

    public virtual J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, P p)
    {
        return ctp
            .WithPrefix(VisitSpace(ctp.Prefix, p))
            .WithMarkers(VisitMarkers(ctp.Markers, p))
            .WithAttributeLists(ListUtils.Map(ctp.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithName(Visit(ctp.Name, p) as Identifier ?? ctp.Name)
            .WithWhereConstraint(VisitLeftPadded(ctp.WhereConstraint, p))
            .WithConstraints(VisitContainer(ctp.Constraints, p))
            .WithType((JavaType?)VisitType(ctp.Type, p));
    }

    public virtual J VisitInterpolatedString(InterpolatedString istr, P p)
    {
        istr = (InterpolatedString)VisitStatement(istr, p);
        istr = (InterpolatedString)VisitExpression(istr, p);
        return istr
            .WithPrefix(VisitSpace(istr.Prefix, p))
            .WithMarkers(VisitMarkers(istr.Markers, p))
            .WithParts(ListUtils.Map(istr.Parts, part => Visit(part, p)));
    }

    public virtual J VisitInterpolation(Interpolation interp, P p)
    {
        return interp
            .WithPrefix(VisitSpace(interp.Prefix, p))
            .WithMarkers(VisitMarkers(interp.Markers, p))
            .WithExpression(Visit(interp.Expression, p) as Expression ?? interp.Expression)
            .WithAlignment(VisitLeftPadded(interp.Alignment, p))
            .WithFormat(VisitLeftPadded(interp.Format, p))
            .WithAfter(VisitSpace(interp.After, p));
    }

    public virtual J VisitAwaitExpression(AwaitExpression ae, P p)
    {
        ae = (AwaitExpression)VisitStatement(ae, p);
        ae = (AwaitExpression)VisitExpression(ae, p);
        return ae
            .WithPrefix(VisitSpace(ae.Prefix, p))
            .WithMarkers(VisitMarkers(ae.Markers, p))
            .WithExpression(Visit(ae.Expression, p) as Expression ?? ae.Expression)
            .WithType((JavaType?)VisitType(ae.Type, p));
    }

    public virtual J VisitYield(Yield yield, P p)
    {
        yield = (Yield)VisitStatement(yield, p);
        return yield
            .WithPrefix(VisitSpace(yield.Prefix, p))
            .WithMarkers(VisitMarkers(yield.Markers, p))
            .WithReturnOrBreakKeyword(Visit(yield.ReturnOrBreakKeyword, p) as Keyword ?? yield.ReturnOrBreakKeyword)
            .WithExpression(yield.Expression != null ? Visit(yield.Expression, p) as Expression ?? yield.Expression : null);
    }

    public virtual J VisitNamespaceDeclaration(NamespaceDeclaration ns, P p)
    {
        ns = (NamespaceDeclaration)VisitStatement(ns, p);
        return ns
            .WithPrefix(VisitSpace(ns.Prefix, p))
            .WithMarkers(VisitMarkers(ns.Markers, p))
            .WithName(VisitRightPadded(ns.Name, p) ?? ns.Name)
            .WithMembers(ListUtils.Map(ns.Members, m => VisitRightPadded(m, p)))
            .WithEnd(VisitSpace(ns.End, p));
    }

    public virtual J VisitTupleType(TupleType tupleType, P p)
    {
        tupleType = (TupleType)VisitExpression(tupleType, p);
        return tupleType
            .WithPrefix(VisitSpace(tupleType.Prefix, p))
            .WithMarkers(VisitMarkers(tupleType.Markers, p))
            .WithElements(VisitContainer(tupleType.Elements, p) ?? tupleType.Elements)
            .WithType((JavaType?)VisitType(tupleType.Type, p));
    }

    public virtual J VisitTupleExpression(TupleExpression tupleExpr, P p)
    {
        tupleExpr = (TupleExpression)VisitExpression(tupleExpr, p);
        return tupleExpr
            .WithPrefix(VisitSpace(tupleExpr.Prefix, p))
            .WithMarkers(VisitMarkers(tupleExpr.Markers, p))
            .WithArguments(VisitContainer(tupleExpr.Arguments, p) ?? tupleExpr.Arguments);
    }

    public virtual J VisitConditionalDirective(ConditionalDirective conditionalDirective, P p)
    {
        conditionalDirective = (ConditionalDirective)VisitStatement(conditionalDirective, p);
        return conditionalDirective
            .WithPrefix(VisitSpace(conditionalDirective.Prefix, p))
            .WithMarkers(VisitMarkers(conditionalDirective.Markers, p))
            .WithBranches(ListUtils.Map(conditionalDirective.Branches, b => VisitRightPadded(b, p)));
    }

    public virtual J VisitPragmaWarningDirective(PragmaWarningDirective pragmaWarningDirective, P p)
    {
        pragmaWarningDirective = (PragmaWarningDirective)VisitStatement(pragmaWarningDirective, p);
        return pragmaWarningDirective
            .WithPrefix(VisitSpace(pragmaWarningDirective.Prefix, p))
            .WithMarkers(VisitMarkers(pragmaWarningDirective.Markers, p))
            .WithWarningCodes(ListUtils.Map(pragmaWarningDirective.WarningCodes, c => VisitRightPadded(c, p)));
    }

    public virtual J VisitPragmaChecksumDirective(PragmaChecksumDirective pragmaChecksumDirective, P p)
    {
        pragmaChecksumDirective = (PragmaChecksumDirective)VisitStatement(pragmaChecksumDirective, p);
        return pragmaChecksumDirective
            .WithPrefix(VisitSpace(pragmaChecksumDirective.Prefix, p))
            .WithMarkers(VisitMarkers(pragmaChecksumDirective.Markers, p));
    }

    public virtual J VisitNullableDirective(NullableDirective nullableDirective, P p)
    {
        nullableDirective = (NullableDirective)VisitStatement(nullableDirective, p);
        return nullableDirective
            .WithPrefix(VisitSpace(nullableDirective.Prefix, p))
            .WithMarkers(VisitMarkers(nullableDirective.Markers, p));
    }

    public virtual J VisitRegionDirective(RegionDirective regionDirective, P p)
    {
        regionDirective = (RegionDirective)VisitStatement(regionDirective, p);
        return regionDirective
            .WithPrefix(VisitSpace(regionDirective.Prefix, p))
            .WithMarkers(VisitMarkers(regionDirective.Markers, p));
    }

    public virtual J VisitEndRegionDirective(EndRegionDirective endRegionDirective, P p)
    {
        endRegionDirective = (EndRegionDirective)VisitStatement(endRegionDirective, p);
        return endRegionDirective
            .WithPrefix(VisitSpace(endRegionDirective.Prefix, p))
            .WithMarkers(VisitMarkers(endRegionDirective.Markers, p));
    }

    public virtual J VisitDefineDirective(DefineDirective defineDirective, P p)
    {
        defineDirective = (DefineDirective)VisitStatement(defineDirective, p);
        return defineDirective
            .WithPrefix(VisitSpace(defineDirective.Prefix, p))
            .WithMarkers(VisitMarkers(defineDirective.Markers, p))
            .WithSymbol(Visit(defineDirective.Symbol, p) as Identifier ?? defineDirective.Symbol);
    }

    public virtual J VisitUndefDirective(UndefDirective undefDirective, P p)
    {
        undefDirective = (UndefDirective)VisitStatement(undefDirective, p);
        return undefDirective
            .WithPrefix(VisitSpace(undefDirective.Prefix, p))
            .WithMarkers(VisitMarkers(undefDirective.Markers, p))
            .WithSymbol(Visit(undefDirective.Symbol, p) as Identifier ?? undefDirective.Symbol);
    }

    public virtual J VisitErrorDirective(ErrorDirective errorDirective, P p)
    {
        errorDirective = (ErrorDirective)VisitStatement(errorDirective, p);
        return errorDirective
            .WithPrefix(VisitSpace(errorDirective.Prefix, p))
            .WithMarkers(VisitMarkers(errorDirective.Markers, p));
    }

    public virtual J VisitWarningDirective(WarningDirective warningDirective, P p)
    {
        warningDirective = (WarningDirective)VisitStatement(warningDirective, p);
        return warningDirective
            .WithPrefix(VisitSpace(warningDirective.Prefix, p))
            .WithMarkers(VisitMarkers(warningDirective.Markers, p));
    }

    public virtual J VisitLineDirective(LineDirective lineDirective, P p)
    {
        lineDirective = (LineDirective)VisitStatement(lineDirective, p);
        return lineDirective
            .WithPrefix(VisitSpace(lineDirective.Prefix, p))
            .WithMarkers(VisitMarkers(lineDirective.Markers, p))
            .WithLine(lineDirective.Line != null ? Visit(lineDirective.Line, p) as Expression ?? lineDirective.Line : null)
            .WithFile(lineDirective.File != null ? Visit(lineDirective.File, p) as Expression ?? lineDirective.File : null);
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
            .WithName(VisitRightPadded(nameColon.Name, p) ?? nameColon.Name);
    }

    public virtual J VisitAnnotatedStatement(AnnotatedStatement annotatedStatement, P p)
    {
        annotatedStatement = (AnnotatedStatement)VisitStatement(annotatedStatement, p);
        return annotatedStatement
            .WithPrefix(VisitSpace(annotatedStatement.Prefix, p))
            .WithMarkers(VisitMarkers(annotatedStatement.Markers, p))
            .WithAttributeLists(ListUtils.Map(annotatedStatement.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithStatement(Visit(annotatedStatement.Statement, p) as Statement ?? annotatedStatement.Statement);
    }

    public virtual J VisitArrayRankSpecifier(ArrayRankSpecifier arrayRankSpecifier, P p)
    {
        arrayRankSpecifier = (ArrayRankSpecifier)VisitExpression(arrayRankSpecifier, p);
        return arrayRankSpecifier
            .WithPrefix(VisitSpace(arrayRankSpecifier.Prefix, p))
            .WithMarkers(VisitMarkers(arrayRankSpecifier.Markers, p))
            .WithSizes(VisitContainer(arrayRankSpecifier.Sizes, p) ?? arrayRankSpecifier.Sizes);
    }

    public virtual J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        assignmentOperation = (AssignmentOperation)VisitStatement(assignmentOperation, p);
        assignmentOperation = (AssignmentOperation)VisitExpression(assignmentOperation, p);
        return assignmentOperation
            .WithPrefix(VisitSpace(assignmentOperation.Prefix, p))
            .WithMarkers(VisitMarkers(assignmentOperation.Markers, p))
            .WithVariable(Visit(assignmentOperation.Variable, p) as Expression ?? assignmentOperation.Variable)
            .WithAssignmentValue(Visit(assignmentOperation.AssignmentValue, p) as Expression ?? assignmentOperation.AssignmentValue)
            .WithType((JavaType?)VisitType(assignmentOperation.Type, p));
    }

    public virtual J VisitStackAllocExpression(StackAllocExpression stackAllocExpression, P p)
    {
        stackAllocExpression = (StackAllocExpression)VisitExpression(stackAllocExpression, p);
        return stackAllocExpression
            .WithPrefix(VisitSpace(stackAllocExpression.Prefix, p))
            .WithMarkers(VisitMarkers(stackAllocExpression.Markers, p))
            .WithExpression(Visit(stackAllocExpression.Expression, p) as NewArray ?? stackAllocExpression.Expression);
    }

    public virtual J VisitGotoStatement(GotoStatement gotoStatement, P p)
    {
        gotoStatement = (GotoStatement)VisitStatement(gotoStatement, p);
        return gotoStatement
            .WithPrefix(VisitSpace(gotoStatement.Prefix, p))
            .WithMarkers(VisitMarkers(gotoStatement.Markers, p))
            .WithCaseOrDefaultKeyword(gotoStatement.CaseOrDefaultKeyword != null ? Visit(gotoStatement.CaseOrDefaultKeyword, p) as Keyword ?? gotoStatement.CaseOrDefaultKeyword : null)
            .WithTarget(gotoStatement.Target != null ? Visit(gotoStatement.Target, p) as Expression ?? gotoStatement.Target : null);
    }

    public virtual J VisitEventDeclaration(EventDeclaration eventDeclaration, P p)
    {
        eventDeclaration = (EventDeclaration)VisitStatement(eventDeclaration, p);
        return eventDeclaration
            .WithPrefix(VisitSpace(eventDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(eventDeclaration.Markers, p))
            .WithAttributeLists(ListUtils.Map(eventDeclaration.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithTypeExpressionPadded(VisitLeftPadded(eventDeclaration.TypeExpressionPadded, p)!)
            .WithName(Visit(eventDeclaration.Name, p) as Identifier ?? eventDeclaration.Name)
            .WithInterfaceSpecifier(VisitRightPadded(eventDeclaration.InterfaceSpecifier, p))
            .WithAccessors(VisitContainer(eventDeclaration.Accessors, p));
    }

    public virtual J VisitCsBinary(CsBinary csBinary, P p)
    {
        csBinary = (CsBinary)VisitExpression(csBinary, p);
        return csBinary
            .WithPrefix(VisitSpace(csBinary.Prefix, p))
            .WithMarkers(VisitMarkers(csBinary.Markers, p))
            .WithLeft(Visit(csBinary.Left, p) as Expression ?? csBinary.Left)
            .WithRight(Visit(csBinary.Right, p) as Expression ?? csBinary.Right)
            .WithType((JavaType?)VisitType(csBinary.Type, p));
    }

    public virtual J VisitCollectionExpression(CollectionExpression collectionExpression, P p)
    {
        collectionExpression = (CollectionExpression)VisitExpression(collectionExpression, p);
        return collectionExpression
            .WithPrefix(VisitSpace(collectionExpression.Prefix, p))
            .WithMarkers(VisitMarkers(collectionExpression.Markers, p))
            .WithElements(ListUtils.Map(collectionExpression.Elements, e => VisitRightPadded(e, p)))
            .WithType((JavaType?)VisitType(collectionExpression.Type, p));
    }

    public virtual J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, P p)
    {
        forEachVariableLoop = (ForEachVariableLoop)VisitStatement(forEachVariableLoop, p);
        return forEachVariableLoop
            .WithPrefix(VisitSpace(forEachVariableLoop.Prefix, p))
            .WithMarkers(VisitMarkers(forEachVariableLoop.Markers, p))
            .WithControlElement(Visit(forEachVariableLoop.ControlElement, p) as ForEachVariableLoopControl ?? forEachVariableLoop.ControlElement)
            .WithBody(VisitRightPadded(forEachVariableLoop.Body, p) ?? forEachVariableLoop.Body);
    }

    public virtual J VisitForEachVariableLoopControl(ForEachVariableLoopControl control, P p)
    {
        return control
            .WithPrefix(VisitSpace(control.Prefix, p))
            .WithMarkers(VisitMarkers(control.Markers, p))
            .WithVariable(VisitRightPadded(control.Variable, p) ?? control.Variable)
            .WithIterable(VisitRightPadded(control.Iterable, p) ?? control.Iterable);
    }

    public virtual J VisitUsingStatement(UsingStatement usingStatement, P p)
    {
        usingStatement = (UsingStatement)VisitStatement(usingStatement, p);
        return usingStatement
            .WithPrefix(VisitSpace(usingStatement.Prefix, p))
            .WithMarkers(VisitMarkers(usingStatement.Markers, p))
            .WithExpressionPadded(VisitLeftPadded(usingStatement.ExpressionPadded, p)!)
            .WithStatement(Visit(usingStatement.Statement, p) as Statement ?? usingStatement.Statement);
    }

    public virtual J VisitAllowsConstraintClause(AllowsConstraintClause clause, P p)
    {
        clause = (AllowsConstraintClause)VisitExpression(clause, p);
        return clause
            .WithPrefix(VisitSpace(clause.Prefix, p))
            .WithMarkers(VisitMarkers(clause.Markers, p))
            .WithExpressions(VisitContainer(clause.Expressions, p) ?? clause.Expressions);
    }

    public virtual J VisitRefStructConstraint(RefStructConstraint constraint, P p)
    {
        constraint = (RefStructConstraint)VisitExpression(constraint, p);
        return constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));
    }

    public virtual J VisitClassOrStructConstraint(ClassOrStructConstraint constraint, P p)
    {
        constraint = (ClassOrStructConstraint)VisitExpression(constraint, p);
        return constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));
    }

    public virtual J VisitConstructorConstraint(ConstructorConstraint constraint, P p)
    {
        constraint = (ConstructorConstraint)VisitExpression(constraint, p);
        return constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));
    }

    public virtual J VisitDefaultConstraint(DefaultConstraint constraint, P p)
    {
        constraint = (DefaultConstraint)VisitExpression(constraint, p);
        return constraint
            .WithPrefix(VisitSpace(constraint.Prefix, p))
            .WithMarkers(VisitMarkers(constraint.Markers, p));
    }

    public virtual J VisitSingleVariableDesignation(SingleVariableDesignation designation, P p)
    {
        designation = (SingleVariableDesignation)VisitExpression(designation, p);
        return designation
            .WithPrefix(VisitSpace(designation.Prefix, p))
            .WithMarkers(VisitMarkers(designation.Markers, p))
            .WithName(Visit(designation.Name, p) as Identifier ?? designation.Name);
    }

    public virtual J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation designation, P p)
    {
        designation = (ParenthesizedVariableDesignation)VisitExpression(designation, p);
        return designation
            .WithPrefix(VisitSpace(designation.Prefix, p))
            .WithMarkers(VisitMarkers(designation.Markers, p))
            .WithVariables(VisitContainer(designation.Variables, p) ?? designation.Variables)
            .WithType((JavaType?)VisitType(designation.Type, p));
    }

    public virtual J VisitDiscardVariableDesignation(DiscardVariableDesignation designation, P p)
    {
        designation = (DiscardVariableDesignation)VisitExpression(designation, p);
        return designation
            .WithPrefix(VisitSpace(designation.Prefix, p))
            .WithMarkers(VisitMarkers(designation.Markers, p))
            .WithDiscard(Visit(designation.Discard, p) as Identifier ?? designation.Discard);
    }

    public virtual J VisitCsUnary(CsUnary csUnary, P p)
    {
        csUnary = (CsUnary)VisitStatement(csUnary, p);
        csUnary = (CsUnary)VisitExpression(csUnary, p);
        return csUnary
            .WithPrefix(VisitSpace(csUnary.Prefix, p))
            .WithMarkers(VisitMarkers(csUnary.Markers, p))
            .WithExpression(Visit(csUnary.Expression, p) as Expression ?? csUnary.Expression)
            .WithType((JavaType?)VisitType(csUnary.Type, p));
    }

    public virtual J VisitTupleElement(TupleElement tupleElement, P p)
    {
        return tupleElement
            .WithPrefix(VisitSpace(tupleElement.Prefix, p))
            .WithMarkers(VisitMarkers(tupleElement.Markers, p))
            .WithElementType(Visit(tupleElement.ElementType, p) as TypeTree ?? tupleElement.ElementType)
            .WithName(tupleElement.Name != null ? Visit(tupleElement.Name, p) as Identifier ?? tupleElement.Name : null);
    }

    public virtual J VisitConstantPattern(ConstantPattern constantPattern, P p)
    {
        constantPattern = (ConstantPattern)VisitExpression(constantPattern, p);
        return constantPattern
            .WithPrefix(VisitSpace(constantPattern.Prefix, p))
            .WithMarkers(VisitMarkers(constantPattern.Markers, p))
            .WithValue(Visit(constantPattern.Value, p) as Expression ?? constantPattern.Value);
    }

    public virtual J VisitDiscardPattern(DiscardPattern discardPattern, P p)
    {
        discardPattern = (DiscardPattern)VisitExpression(discardPattern, p);
        return discardPattern
            .WithPrefix(VisitSpace(discardPattern.Prefix, p))
            .WithMarkers(VisitMarkers(discardPattern.Markers, p))
            .WithType((JavaType?)VisitType(discardPattern.Type, p));
    }

    public virtual J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, P p)
    {
        implicitElementAccess = (ImplicitElementAccess)VisitExpression(implicitElementAccess, p);
        return implicitElementAccess
            .WithPrefix(VisitSpace(implicitElementAccess.Prefix, p))
            .WithMarkers(VisitMarkers(implicitElementAccess.Markers, p))
            .WithArgumentList(VisitContainer(implicitElementAccess.ArgumentList, p) ?? implicitElementAccess.ArgumentList);
    }

    public virtual J VisitSlicePattern(SlicePattern slicePattern, P p)
    {
        slicePattern = (SlicePattern)VisitExpression(slicePattern, p);
        return slicePattern
            .WithPrefix(VisitSpace(slicePattern.Prefix, p))
            .WithMarkers(VisitMarkers(slicePattern.Markers, p));
    }

    public virtual J VisitListPattern(ListPattern listPattern, P p)
    {
        listPattern = (ListPattern)VisitExpression(listPattern, p);
        return listPattern
            .WithPrefix(VisitSpace(listPattern.Prefix, p))
            .WithMarkers(VisitMarkers(listPattern.Markers, p))
            .WithPatterns(VisitContainer(listPattern.Patterns, p) ?? listPattern.Patterns)
            .WithDesignation(listPattern.Designation != null ? Visit(listPattern.Designation, p) as VariableDesignation ?? listPattern.Designation : null);
    }

    public virtual J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        switchExpression = (SwitchExpression)VisitExpression(switchExpression, p);
        return switchExpression
            .WithPrefix(VisitSpace(switchExpression.Prefix, p))
            .WithMarkers(VisitMarkers(switchExpression.Markers, p))
            .WithExpressionPadded(VisitRightPadded(switchExpression.ExpressionPadded, p) ?? switchExpression.ExpressionPadded)
            .WithArms(VisitContainer(switchExpression.Arms, p) ?? switchExpression.Arms);
    }

    public virtual J VisitSwitchExpressionArm(SwitchExpressionArm arm, P p)
    {
        return arm
            .WithPrefix(VisitSpace(arm.Prefix, p))
            .WithMarkers(VisitMarkers(arm.Markers, p))
            .WithPattern(Visit(arm.Pattern, p) as J ?? arm.Pattern)
            .WithWhenExpression(VisitLeftPadded(arm.WhenExpression, p))
            .WithExpressionPadded(VisitLeftPadded(arm.ExpressionPadded, p)!);
    }

    public virtual J VisitCheckedExpression(CheckedExpression checkedExpression, P p)
    {
        checkedExpression = (CheckedExpression)VisitExpression(checkedExpression, p);
        return checkedExpression
            .WithPrefix(VisitSpace(checkedExpression.Prefix, p))
            .WithMarkers(VisitMarkers(checkedExpression.Markers, p))
            .WithCheckedOrUncheckedKeyword(Visit(checkedExpression.CheckedOrUncheckedKeyword, p) as Keyword ?? checkedExpression.CheckedOrUncheckedKeyword)
            .WithExpressionValue(Visit(checkedExpression.ExpressionValue, p) as ControlParentheses<Expression> ?? checkedExpression.ExpressionValue);
    }

    public virtual J VisitCheckedStatement(CheckedStatement checkedStatement, P p)
    {
        checkedStatement = (CheckedStatement)VisitStatement(checkedStatement, p);
        return checkedStatement
            .WithPrefix(VisitSpace(checkedStatement.Prefix, p))
            .WithMarkers(VisitMarkers(checkedStatement.Markers, p))
            .WithKeywordValue(Visit(checkedStatement.KeywordValue, p) as Keyword ?? checkedStatement.KeywordValue)
            .WithBlock(Visit(checkedStatement.Block, p) as Block ?? checkedStatement.Block);
    }

    public virtual J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        rangeExpression = (RangeExpression)VisitExpression(rangeExpression, p);
        return rangeExpression
            .WithPrefix(VisitSpace(rangeExpression.Prefix, p))
            .WithMarkers(VisitMarkers(rangeExpression.Markers, p))
            .WithStart(VisitRightPadded(rangeExpression.Start, p))
            .WithEnd(rangeExpression.End != null ? Visit(rangeExpression.End, p) as Expression ?? rangeExpression.End : null);
    }

    public virtual J VisitIndexerDeclaration(IndexerDeclaration indexerDeclaration, P p)
    {
        indexerDeclaration = (IndexerDeclaration)VisitStatement(indexerDeclaration, p);
        return indexerDeclaration
            .WithPrefix(VisitSpace(indexerDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(indexerDeclaration.Markers, p))
            .WithTypeExpression(Visit(indexerDeclaration.TypeExpression, p) as TypeTree ?? indexerDeclaration.TypeExpression)
            .WithExplicitInterfaceSpecifier(VisitRightPadded(indexerDeclaration.ExplicitInterfaceSpecifier, p))
            .WithIndexer(Visit(indexerDeclaration.Indexer, p) as Expression ?? indexerDeclaration.Indexer)
            .WithParameters(VisitContainer(indexerDeclaration.Parameters, p) ?? indexerDeclaration.Parameters)
            .WithExpressionBody(VisitLeftPadded(indexerDeclaration.ExpressionBody, p))
            .WithAccessors(indexerDeclaration.Accessors != null ? Visit(indexerDeclaration.Accessors, p) as Block ?? indexerDeclaration.Accessors : null);
    }

    public virtual J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, P p)
    {
        delegateDeclaration = (DelegateDeclaration)VisitStatement(delegateDeclaration, p);
        return delegateDeclaration
            .WithPrefix(VisitSpace(delegateDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(delegateDeclaration.Markers, p))
            .WithAttributes(ListUtils.Map(delegateDeclaration.Attributes, al => Visit(al, p) as AttributeList))
            .WithReturnType(VisitLeftPadded(delegateDeclaration.ReturnType, p)!)
            .WithIdentifierName(Visit(delegateDeclaration.IdentifierName, p) as Identifier ?? delegateDeclaration.IdentifierName)
            .WithTypeParameters(VisitContainer(delegateDeclaration.TypeParameters, p))
            .WithParameters(VisitContainer(delegateDeclaration.Parameters, p) ?? delegateDeclaration.Parameters);
    }

    public virtual J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
    {
        conversion = (ConversionOperatorDeclaration)VisitStatement(conversion, p);
        return conversion
            .WithPrefix(VisitSpace(conversion.Prefix, p))
            .WithMarkers(VisitMarkers(conversion.Markers, p))
            .WithInterfaceSpecifier(VisitRightPadded(conversion.InterfaceSpecifier, p))
            .WithReturnType(VisitLeftPadded(conversion.ReturnType, p)!)
            .WithParameters(VisitContainer(conversion.Parameters, p) ?? conversion.Parameters)
            .WithExpressionBody(VisitLeftPadded(conversion.ExpressionBody, p))
            .WithBody(conversion.Body != null ? Visit(conversion.Body, p) as Block ?? conversion.Body : null);
    }

    public virtual J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
    {
        operatorDeclaration = (OperatorDeclaration)VisitStatement(operatorDeclaration, p);
        return operatorDeclaration
            .WithPrefix(VisitSpace(operatorDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(operatorDeclaration.Markers, p))
            .WithAttributeLists(ListUtils.Map(operatorDeclaration.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithExplicitInterfaceSpecifier(VisitRightPadded(operatorDeclaration.ExplicitInterfaceSpecifier, p))
            .WithOperatorKeyword(Visit(operatorDeclaration.OperatorKeyword, p) as Keyword ?? operatorDeclaration.OperatorKeyword)
            .WithCheckedKeyword(operatorDeclaration.CheckedKeyword != null ? Visit(operatorDeclaration.CheckedKeyword, p) as Keyword ?? operatorDeclaration.CheckedKeyword : null)
            .WithReturnType(Visit(operatorDeclaration.ReturnType, p) as TypeTree ?? operatorDeclaration.ReturnType)
            .WithParameters(VisitContainer(operatorDeclaration.Parameters, p) ?? operatorDeclaration.Parameters)
            .WithBody(Visit(operatorDeclaration.Body, p) as Block ?? operatorDeclaration.Body)
            .WithMethodType((JavaType.Method?)VisitType(operatorDeclaration.MethodType, p));
    }

    public virtual J VisitEnumDeclaration(EnumDeclaration enumDeclaration, P p)
    {
        enumDeclaration = (EnumDeclaration)VisitStatement(enumDeclaration, p);
        return enumDeclaration
            .WithPrefix(VisitSpace(enumDeclaration.Prefix, p))
            .WithMarkers(VisitMarkers(enumDeclaration.Markers, p))
            .WithAttributeLists(enumDeclaration.AttributeLists != null
                ? ListUtils.Map(enumDeclaration.AttributeLists, al => Visit(al, p) as AttributeList)
                : null)
            .WithNamePadded(VisitLeftPadded(enumDeclaration.NamePadded, p)!)
            .WithBaseType(VisitLeftPadded(enumDeclaration.BaseType, p))
            .WithMembers(VisitContainer(enumDeclaration.Members, p));
    }

    public virtual J VisitEnumMemberDeclaration(EnumMemberDeclaration enumMember, P p)
    {
        enumMember = (EnumMemberDeclaration)VisitExpression(enumMember, p);
        return enumMember
            .WithPrefix(VisitSpace(enumMember.Prefix, p))
            .WithMarkers(VisitMarkers(enumMember.Markers, p))
            .WithAttributeLists(ListUtils.Map(enumMember.AttributeLists, al => Visit(al, p) as AttributeList))
            .WithName(Visit(enumMember.Name, p) as Identifier ?? enumMember.Name)
            .WithInitializer(VisitLeftPadded(enumMember.Initializer, p));
    }

    public virtual J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, P p)
    {
        aliasQualifiedName = (AliasQualifiedName)VisitExpression(aliasQualifiedName, p);
        return aliasQualifiedName
            .WithPrefix(VisitSpace(aliasQualifiedName.Prefix, p))
            .WithMarkers(VisitMarkers(aliasQualifiedName.Markers, p))
            .WithAlias(VisitRightPadded(aliasQualifiedName.Alias, p) ?? aliasQualifiedName.Alias)
            .WithName(Visit(aliasQualifiedName.Name, p) as Expression ?? aliasQualifiedName.Name);
    }

    public virtual J VisitPointerDereference(PointerDereference pointerDereference, P p)
    {
        pointerDereference = (PointerDereference)VisitExpression(pointerDereference, p);
        return pointerDereference
            .WithPrefix(VisitSpace(pointerDereference.Prefix, p))
            .WithMarkers(VisitMarkers(pointerDereference.Markers, p))
            .WithExpression(Visit(pointerDereference.Expression, p) as Expression ?? pointerDereference.Expression)
            .WithType((JavaType?)VisitType(pointerDereference.Type, p));
    }

    public virtual J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, P p)
    {
        pointerFieldAccess = (PointerFieldAccess)VisitStatement(pointerFieldAccess, p);
        pointerFieldAccess = (PointerFieldAccess)VisitExpression(pointerFieldAccess, p);
        return pointerFieldAccess
            .WithPrefix(VisitSpace(pointerFieldAccess.Prefix, p))
            .WithMarkers(VisitMarkers(pointerFieldAccess.Markers, p))
            .WithTarget(Visit(pointerFieldAccess.Target, p) as Expression ?? pointerFieldAccess.Target)
            .WithNamePadded(VisitLeftPadded(pointerFieldAccess.NamePadded, p)!)
            .WithType((JavaType?)VisitType(pointerFieldAccess.Type, p));
    }

    public virtual J VisitRefType(RefType refType, P p)
    {
        refType = (RefType)VisitExpression(refType, p);
        return refType
            .WithPrefix(VisitSpace(refType.Prefix, p))
            .WithMarkers(VisitMarkers(refType.Markers, p))
            .WithTypeIdentifier(Visit(refType.TypeIdentifier, p) as TypeTree ?? refType.TypeIdentifier)
            .WithType((JavaType?)VisitType(refType.Type, p));
    }

    // ---- LINQ ----

    public virtual J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        queryExpression = (QueryExpression)VisitExpression(queryExpression, p);
        return queryExpression
            .WithPrefix(VisitSpace(queryExpression.Prefix, p))
            .WithMarkers(VisitMarkers(queryExpression.Markers, p))
            .WithFromClause(Visit(queryExpression.FromClause, p) as FromClause ?? queryExpression.FromClause)
            .WithBody(Visit(queryExpression.Body, p) as QueryBody ?? queryExpression.Body);
    }

    public virtual J VisitQueryBody(QueryBody queryBody, P p)
    {
        return queryBody
            .WithPrefix(VisitSpace(queryBody.Prefix, p))
            .WithMarkers(VisitMarkers(queryBody.Markers, p))
            .WithClauses(ListUtils.Map(queryBody.Clauses, c => (QueryClause?)Visit(c, p)))
            .WithSelectOrGroup(queryBody.SelectOrGroup != null ? Visit(queryBody.SelectOrGroup, p) as SelectOrGroupClause ?? queryBody.SelectOrGroup : null)
            .WithContinuation(queryBody.Continuation != null ? (QueryContinuation)VisitQueryContinuation(queryBody.Continuation, p) : null);
    }

    public virtual J VisitFromClause(FromClause fromClause, P p)
    {
        fromClause = (FromClause)VisitExpression(fromClause, p);
        return fromClause
            .WithPrefix(VisitSpace(fromClause.Prefix, p))
            .WithMarkers(VisitMarkers(fromClause.Markers, p))
            .WithTypeIdentifier(fromClause.TypeIdentifier != null ? Visit(fromClause.TypeIdentifier, p) as TypeTree ?? fromClause.TypeIdentifier : null)
            .WithIdentifierPadded(VisitRightPadded(fromClause.IdentifierPadded, p) ?? fromClause.IdentifierPadded)
            .WithExpression(Visit(fromClause.Expression, p) as Expression ?? fromClause.Expression);
    }

    public virtual J VisitLetClause(LetClause letClause, P p)
    {
        return letClause
            .WithPrefix(VisitSpace(letClause.Prefix, p))
            .WithMarkers(VisitMarkers(letClause.Markers, p))
            .WithIdentifierPadded(VisitRightPadded(letClause.IdentifierPadded, p) ?? letClause.IdentifierPadded)
            .WithExpression(Visit(letClause.Expression, p) as Expression ?? letClause.Expression);
    }

    public virtual J VisitJoinClause(JoinClause joinClause, P p)
    {
        return joinClause
            .WithPrefix(VisitSpace(joinClause.Prefix, p))
            .WithMarkers(VisitMarkers(joinClause.Markers, p))
            .WithIdentifierPadded(VisitRightPadded(joinClause.IdentifierPadded, p) ?? joinClause.IdentifierPadded)
            .WithInExpression(VisitRightPadded(joinClause.InExpression, p) ?? joinClause.InExpression)
            .WithLeftExpression(VisitRightPadded(joinClause.LeftExpression, p) ?? joinClause.LeftExpression)
            .WithRightExpression(Visit(joinClause.RightExpression, p) as Expression ?? joinClause.RightExpression)
            .WithInto(VisitLeftPadded(joinClause.Into, p));
    }

    public virtual J VisitJoinIntoClause(JoinIntoClause joinIntoClause, P p)
    {
        return joinIntoClause
            .WithPrefix(VisitSpace(joinIntoClause.Prefix, p))
            .WithMarkers(VisitMarkers(joinIntoClause.Markers, p))
            .WithIdentifier(Visit(joinIntoClause.Identifier, p) as Identifier ?? joinIntoClause.Identifier);
    }

    public virtual J VisitWhereClause(WhereClause whereClause, P p)
    {
        return whereClause
            .WithPrefix(VisitSpace(whereClause.Prefix, p))
            .WithMarkers(VisitMarkers(whereClause.Markers, p))
            .WithCondition(Visit(whereClause.Condition, p) as Expression ?? whereClause.Condition);
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
            .WithExpressionPadded(VisitRightPadded(ordering.ExpressionPadded, p) ?? ordering.ExpressionPadded);
    }

    public virtual J VisitSelectClause(SelectClause selectClause, P p)
    {
        return selectClause
            .WithPrefix(VisitSpace(selectClause.Prefix, p))
            .WithMarkers(VisitMarkers(selectClause.Markers, p))
            .WithExpression(Visit(selectClause.Expression, p) as Expression ?? selectClause.Expression);
    }

    public virtual J VisitGroupClause(GroupClause groupClause, P p)
    {
        return groupClause
            .WithPrefix(VisitSpace(groupClause.Prefix, p))
            .WithMarkers(VisitMarkers(groupClause.Markers, p))
            .WithGroupExpression(VisitRightPadded(groupClause.GroupExpression, p) ?? groupClause.GroupExpression)
            .WithKey(Visit(groupClause.Key, p) as Expression ?? groupClause.Key);
    }

    public virtual J VisitQueryContinuation(QueryContinuation queryContinuation, P p)
    {
        return queryContinuation
            .WithPrefix(VisitSpace(queryContinuation.Prefix, p))
            .WithMarkers(VisitMarkers(queryContinuation.Markers, p))
            .WithIdentifier(Visit(queryContinuation.Identifier, p) as Identifier ?? queryContinuation.Identifier)
            .WithBody((QueryBody)VisitQueryBody(queryContinuation.Body, p));
    }

    public virtual J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression anonymousObject, P p)
    {
        anonymousObject = (AnonymousObjectCreationExpression)VisitExpression(anonymousObject, p);
        return anonymousObject
            .WithPrefix(VisitSpace(anonymousObject.Prefix, p))
            .WithMarkers(VisitMarkers(anonymousObject.Markers, p))
            .WithInitializers(VisitContainer(anonymousObject.Initializers, p) ?? anonymousObject.Initializers)
            .WithType((JavaType?)VisitType(anonymousObject.Type, p));
    }

    public virtual J VisitWithExpression(WithExpression withExpression, P p)
    {
        withExpression = (WithExpression)VisitExpression(withExpression, p);
        return withExpression
            .WithPrefix(VisitSpace(withExpression.Prefix, p))
            .WithMarkers(VisitMarkers(withExpression.Markers, p))
            .WithTarget(Visit(withExpression.Target, p) as Expression ?? withExpression.Target)
            .WithInitializerPadded(VisitLeftPadded(withExpression.InitializerPadded, p)!)
            .WithType((JavaType?)VisitType(withExpression.Type, p));
    }

    public virtual J VisitSpreadExpression(SpreadExpression spreadExpression, P p)
    {
        spreadExpression = (SpreadExpression)VisitExpression(spreadExpression, p);
        return spreadExpression
            .WithPrefix(VisitSpace(spreadExpression.Prefix, p))
            .WithMarkers(VisitMarkers(spreadExpression.Markers, p))
            .WithExpression(Visit(spreadExpression.Expression, p) as Expression ?? spreadExpression.Expression)
            .WithType((JavaType?)VisitType(spreadExpression.Type, p));
    }

    public virtual J VisitFunctionPointerType(FunctionPointerType functionPointerType, P p)
    {
        functionPointerType = (FunctionPointerType)VisitExpression(functionPointerType, p);
        return functionPointerType
            .WithPrefix(VisitSpace(functionPointerType.Prefix, p))
            .WithMarkers(VisitMarkers(functionPointerType.Markers, p))
            .WithUnmanagedCallingConventionTypes(VisitContainer(functionPointerType.UnmanagedCallingConventionTypes, p))
            .WithParameterTypes(VisitContainer(functionPointerType.ParameterTypes, p) ?? functionPointerType.ParameterTypes)
            .WithType((JavaType?)VisitType(functionPointerType.Type, p));
    }

    public virtual J VisitTypeWithArguments(TypeWithArguments twa, P p)
    {
        twa = (TypeWithArguments)VisitExpression(twa, p);
        return twa
            .WithPrefix(VisitSpace(twa.Prefix, p))
            .WithMarkers(VisitMarkers(twa.Markers, p))
            .WithTypeExpression(Visit(twa.TypeExpression, p) as TypeTree ?? twa.TypeExpression)
            .WithArguments(VisitContainer(twa.Arguments, p) ?? twa.Arguments);
    }

    public virtual J VisitExceptionFilteredTry(ExceptionFilteredTry eft, P p)
    {
        eft = (ExceptionFilteredTry)VisitStatement(eft, p);

        eft = eft
            .WithPrefix(VisitSpace(eft.Prefix, p))
            .WithMarkers(VisitMarkers(eft.Markers, p))
            .WithTry(Visit(eft.Try, p) as Try ?? eft.Try);

        // Keep manual loop for CatchFilters since the list element type is nullable (JLeftPadded<...>?)
        var newFilters = new List<JLeftPadded<ControlParentheses<Expression>>?>(eft.CatchFilters.Count);
        bool filtersChanged = false;
        foreach (var filter in eft.CatchFilters)
        {
            if (filter != null)
            {
                var visited = VisitLeftPadded(filter, p);
                if (!ReferenceEquals(visited, filter)) filtersChanged = true;
                newFilters.Add(visited);
            }
            else
            {
                newFilters.Add(null);
            }
        }

        return eft.WithCatchFilters(filtersChanged ? newFilters : eft.CatchFilters);
    }

    public virtual J VisitExplicitInterfaceMember(ExplicitInterfaceMember eim, P p)
    {
        eim = (ExplicitInterfaceMember)VisitStatement(eim, p);
        return eim
            .WithPrefix(VisitSpace(eim.Prefix, p))
            .WithMarkers(VisitMarkers(eim.Markers, p))
            .WithInterfaceSpecifier(VisitRightPadded(eim.InterfaceSpecifier, p) ?? eim.InterfaceSpecifier)
            .WithMethodDeclaration(Visit(eim.MethodDeclaration, p) as MethodDeclaration ?? eim.MethodDeclaration);
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
