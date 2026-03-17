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
        bool changed = false;

        var newPrefix = VisitSpace(compilationUnit.Prefix, p);
        if (!ReferenceEquals(newPrefix, compilationUnit.Prefix)) changed = true;

        var newMarkers = VisitMarkers(compilationUnit.Markers, p);
        if (!ReferenceEquals(newMarkers, compilationUnit.Markers)) changed = true;

        var members = new List<JRightPadded<Statement>>();
        bool membersChanged = false;
        foreach (var memberPadded in compilationUnit.Members)
        {
            var visited = VisitRightPadded(memberPadded, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, memberPadded)) membersChanged = true;
                members.Add(visited);
            }
            else membersChanged = true;
        }
        if (membersChanged) changed = true;

        if (changed)
        {
            compilationUnit = compilationUnit.WithPrefix(newPrefix).WithMarkers(newMarkers)
                .WithMembers(membersChanged ? members : compilationUnit.Members);
        }

        return compilationUnit;
    }

    public virtual J VisitUsingDirective(UsingDirective usingDirective, P p)
    {
        usingDirective = (UsingDirective)VisitStatement(usingDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(usingDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, usingDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(usingDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, usingDirective.Markers)) changed = true;

        var newAlias = VisitRightPadded(usingDirective.Alias, p);
        if (!ReferenceEquals(newAlias, usingDirective.Alias)) changed = true;

        TypeTree newNs = usingDirective.NamespaceOrType;
        var visitedNs = Visit(usingDirective.NamespaceOrType, p);
        if (visitedNs is TypeTree tt && !ReferenceEquals(tt, usingDirective.NamespaceOrType))
        {
            newNs = tt;
            changed = true;
        }

        return changed
            ? usingDirective.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAlias(newAlias).WithNamespaceOrType(newNs)
            : usingDirective;
    }

    public virtual J VisitPropertyDeclaration(PropertyDeclaration prop, P p)
    {
        prop = (PropertyDeclaration)VisitStatement(prop, p);
        var changed = false;

        var newPrefix = VisitSpace(prop.Prefix, p);
        if (!ReferenceEquals(newPrefix, prop.Prefix)) changed = true;

        var newMarkers = VisitMarkers(prop.Markers, p);
        if (!ReferenceEquals(newMarkers, prop.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in prop.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        TypeTree newTypeExpr = prop.TypeExpression;
        var visitedType = Visit(prop.TypeExpression, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, prop.TypeExpression))
        {
            newTypeExpr = tt;
            changed = true;
        }

        var newInterfaceSpec = VisitRightPadded(prop.InterfaceSpecifier, p);
        if (!ReferenceEquals(newInterfaceSpec, prop.InterfaceSpecifier)) changed = true;

        Identifier newName = prop.Name;
        var visitedName = Visit(prop.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, prop.Name))
        {
            newName = id;
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

        var newExprBody = VisitLeftPadded(prop.ExpressionBody, p);
        if (!ReferenceEquals(newExprBody, prop.ExpressionBody)) changed = true;

        var newInit = VisitLeftPadded(prop.Initializer, p);
        if (!ReferenceEquals(newInit, prop.Initializer)) changed = true;

        return changed
            ? prop.WithPrefix(newPrefix).WithMarkers(newMarkers)
                  .WithAttributeLists(attrChanged ? newAttrLists : prop.AttributeLists)
                  .WithTypeExpression(newTypeExpr).WithInterfaceSpecifier(newInterfaceSpec)
                  .WithName(newName).WithAccessors(newAccessors)
                  .WithExpressionBody(newExprBody).WithInitializer(newInit)
            : prop;
    }

    public virtual J VisitAccessorDeclaration(AccessorDeclaration accessor, P p)
    {
        accessor = (AccessorDeclaration)VisitStatement(accessor, p);
        var changed = false;

        var newPrefix = VisitSpace(accessor.Prefix, p);
        if (!ReferenceEquals(newPrefix, accessor.Prefix)) changed = true;

        var newMarkers = VisitMarkers(accessor.Markers, p);
        if (!ReferenceEquals(newMarkers, accessor.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in accessor.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

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

        var newExprBody = VisitLeftPadded(accessor.ExpressionBody, p);
        if (!ReferenceEquals(newExprBody, accessor.ExpressionBody)) changed = true;

        return changed
            ? accessor.WithPrefix(newPrefix).WithMarkers(newMarkers)
                      .WithAttributeLists(attrChanged ? newAttrLists : accessor.AttributeLists)
                      .WithBody(newBody).WithExpressionBody(newExprBody)
            : accessor;
    }

    public virtual J VisitAttributeList(AttributeList attrList, P p)
    {
        attrList = (AttributeList)VisitStatement(attrList, p);
        bool changed = false;

        var newPrefix = VisitSpace(attrList.Prefix, p);
        if (!ReferenceEquals(newPrefix, attrList.Prefix)) changed = true;

        var newMarkers = VisitMarkers(attrList.Markers, p);
        if (!ReferenceEquals(newMarkers, attrList.Markers)) changed = true;

        var newTarget = VisitRightPadded(attrList.Target, p);
        if (!ReferenceEquals(newTarget, attrList.Target))
        {
            attrList = attrList.WithTarget(newTarget);
            changed = true;
        }

        var newAttrs = new List<JRightPadded<Annotation>>();
        bool attrsChanged = false;
        foreach (var paddedAttr in attrList.Attributes)
        {
            var visited = VisitRightPadded(paddedAttr, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, paddedAttr)) attrsChanged = true;
                newAttrs.Add(visited);
            }
            else attrsChanged = true;
        }
        if (attrsChanged) changed = true;
        return changed ? attrList.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAttributes(attrsChanged ? newAttrs : attrList.Attributes) : attrList;
    }

    public virtual J VisitNamedExpression(NamedExpression ne, P p)
    {
        ne = (NamedExpression)VisitExpression(ne, p);
        var changed = false;

        var newPrefix = VisitSpace(ne.Prefix, p);
        if (!ReferenceEquals(newPrefix, ne.Prefix)) changed = true;

        var newMarkers = VisitMarkers(ne.Markers, p);
        if (!ReferenceEquals(newMarkers, ne.Markers)) changed = true;

        var newName = VisitRightPadded(ne.Name, p)!;
        if (!ReferenceEquals(newName, ne.Name)) changed = true;

        Expression newExpr = ne.Expression;
        var visitedExpr = Visit(ne.Expression, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, ne.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? ne.WithPrefix(newPrefix).WithMarkers(newMarkers).WithName(newName).WithExpression(newExpr)
            : ne;
    }

    public virtual J VisitRefExpression(RefExpression re, P p)
    {
        re = (RefExpression)VisitExpression(re, p);
        var changed = false;

        var newPrefix = VisitSpace(re.Prefix, p);
        if (!ReferenceEquals(newPrefix, re.Prefix)) changed = true;

        var newMarkers = VisitMarkers(re.Markers, p);
        if (!ReferenceEquals(newMarkers, re.Markers)) changed = true;

        Expression newExpr = re.Expression;
        var expr = Visit(re.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, re.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? re.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr)
            : re;
    }

    public virtual J VisitDeclarationExpression(DeclarationExpression de, P p)
    {
        de = (DeclarationExpression)VisitExpression(de, p);
        var changed = false;

        var newPrefix = VisitSpace(de.Prefix, p);
        if (!ReferenceEquals(newPrefix, de.Prefix)) changed = true;

        var newMarkers = VisitMarkers(de.Markers, p);
        if (!ReferenceEquals(newMarkers, de.Markers)) changed = true;

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
            ? de.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTypeExpression(newTypeExpr).WithVariables(newVariables)
            : de;
    }

    public virtual J VisitIsPattern(IsPattern isPattern, P p)
    {
        isPattern = (IsPattern)VisitExpression(isPattern, p);
        var changed = false;

        var newPrefix = VisitSpace(isPattern.Prefix, p);
        if (!ReferenceEquals(newPrefix, isPattern.Prefix)) changed = true;

        var newMarkers = VisitMarkers(isPattern.Markers, p);
        if (!ReferenceEquals(newMarkers, isPattern.Markers)) changed = true;

        var expr = Visit(isPattern.Expression, p);
        if (expr is Expression newExpr && !ReferenceEquals(newExpr, isPattern.Expression)) changed = true;

        var newPattern = VisitLeftPadded(isPattern.Pattern, p);
        if (!ReferenceEquals(newPattern, isPattern.Pattern)) changed = true;

        return changed
            ? isPattern.WithPrefix(newPrefix).WithMarkers(newMarkers)
                       .WithExpression(expr is Expression e ? e : isPattern.Expression)
                       .WithPattern(newPattern ?? isPattern.Pattern)
            : isPattern;
    }

    public virtual J VisitStatementExpression(StatementExpression se, P p)
    {
        // Pattern extends Expression, NOT Statement despite the name
        se = (StatementExpression)VisitExpression(se, p);
        var changed = false;

        var newPrefix = VisitSpace(se.Prefix, p);
        if (!ReferenceEquals(newPrefix, se.Prefix)) changed = true;

        var newMarkers = VisitMarkers(se.Markers, p);
        if (!ReferenceEquals(newMarkers, se.Markers)) changed = true;

        Statement newStmt = se.Statement;
        var stmt = Visit(se.Statement, p);
        if (stmt is Statement s && !ReferenceEquals(s, se.Statement))
        {
            newStmt = s;
            changed = true;
        }

        return changed
            ? se.WithPrefix(newPrefix).WithMarkers(newMarkers).WithStatement(newStmt)
            : se;
    }

    public virtual J VisitSizeOf(SizeOf sizeOf, P p)
    {
        sizeOf = (SizeOf)VisitExpression(sizeOf, p);
        var changed = false;

        var newPrefix = VisitSpace(sizeOf.Prefix, p);
        if (!ReferenceEquals(newPrefix, sizeOf.Prefix)) changed = true;

        var newMarkers = VisitMarkers(sizeOf.Markers, p);
        if (!ReferenceEquals(newMarkers, sizeOf.Markers)) changed = true;

        Expression newExpr = sizeOf.Expression;
        var visited = Visit(sizeOf.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, sizeOf.Expression))
        {
            newExpr = e;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(sizeOf.Type, p);
        if (!ReferenceEquals(newType, sizeOf.Type)) changed = true;

        return changed
            ? sizeOf.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr).WithType(newType)
            : sizeOf;
    }

    public virtual J VisitUnsafeStatement(UnsafeStatement unsafeStatement, P p)
    {
        unsafeStatement = (UnsafeStatement)VisitStatement(unsafeStatement, p);
        var changed = false;

        var newPrefix = VisitSpace(unsafeStatement.Prefix, p);
        if (!ReferenceEquals(newPrefix, unsafeStatement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(unsafeStatement.Markers, p);
        if (!ReferenceEquals(newMarkers, unsafeStatement.Markers)) changed = true;

        Block newBlock = unsafeStatement.Block;
        var visited = Visit(unsafeStatement.Block, p);
        if (visited is Block b && !ReferenceEquals(b, unsafeStatement.Block))
        {
            newBlock = b;
            changed = true;
        }

        return changed
            ? unsafeStatement.WithPrefix(newPrefix).WithMarkers(newMarkers).WithBlock(newBlock)
            : unsafeStatement;
    }

    public virtual J VisitPointerType(PointerType pointerType, P p)
    {
        pointerType = (PointerType)VisitExpression(pointerType, p);
        var changed = false;

        var newPrefix = VisitSpace(pointerType.Prefix, p);
        if (!ReferenceEquals(newPrefix, pointerType.Prefix)) changed = true;

        var newMarkers = VisitMarkers(pointerType.Markers, p);
        if (!ReferenceEquals(newMarkers, pointerType.Markers)) changed = true;

        var newElementType = VisitRightPadded(pointerType.ElementType, p)!;
        if (!ReferenceEquals(newElementType, pointerType.ElementType)) changed = true;

        return changed
            ? pointerType.WithPrefix(newPrefix).WithMarkers(newMarkers).WithElementType(newElementType)
            : pointerType;
    }

    public virtual J VisitFixedStatement(FixedStatement fixedStatement, P p)
    {
        fixedStatement = (FixedStatement)VisitStatement(fixedStatement, p);
        var changed = false;

        var newPrefix = VisitSpace(fixedStatement.Prefix, p);
        if (!ReferenceEquals(newPrefix, fixedStatement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(fixedStatement.Markers, p);
        if (!ReferenceEquals(newMarkers, fixedStatement.Markers)) changed = true;

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
            ? fixedStatement.WithPrefix(newPrefix).WithMarkers(newMarkers).WithDeclarations(newDecl).WithBlock(newBlock)
            : fixedStatement;
    }

    public virtual J VisitExternAlias(ExternAlias externAlias, P p)
    {
        externAlias = (ExternAlias)VisitStatement(externAlias, p);
        var changed = false;

        var newPrefix = VisitSpace(externAlias.Prefix, p);
        if (!ReferenceEquals(newPrefix, externAlias.Prefix)) changed = true;

        var newMarkers = VisitMarkers(externAlias.Markers, p);
        if (!ReferenceEquals(newMarkers, externAlias.Markers)) changed = true;

        var newIdentifier = VisitLeftPadded(externAlias.Identifier, p)!;
        if (!ReferenceEquals(newIdentifier, externAlias.Identifier)) changed = true;

        return changed
            ? externAlias.WithPrefix(newPrefix).WithMarkers(newMarkers).WithIdentifier(newIdentifier)
            : externAlias;
    }

    public virtual J VisitInitializerExpression(InitializerExpression initializerExpression, P p)
    {
        initializerExpression = (InitializerExpression)VisitExpression(initializerExpression, p);
        bool changed = false;

        var newPrefix = VisitSpace(initializerExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, initializerExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(initializerExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, initializerExpression.Markers)) changed = true;

        var newExprs = VisitContainer(initializerExpression.Expressions, p);
        if (!ReferenceEquals(newExprs, initializerExpression.Expressions)) changed = true;

        return changed
            ? initializerExpression.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                   .WithExpressions(newExprs ?? initializerExpression.Expressions)
            : initializerExpression;
    }

    public virtual J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, P p)
    {
        nullSafeExpression = (NullSafeExpression)VisitExpression(nullSafeExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(nullSafeExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, nullSafeExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(nullSafeExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, nullSafeExpression.Markers)) changed = true;

        var newExprPadded = VisitRightPadded(nullSafeExpression.ExpressionPadded, p)!;
        if (!ReferenceEquals(newExprPadded, nullSafeExpression.ExpressionPadded)) changed = true;

        return changed
            ? nullSafeExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpressionPadded(newExprPadded)
            : nullSafeExpression;
    }

    public virtual J VisitDefaultExpression(DefaultExpression defaultExpression, P p)
    {
        defaultExpression = (DefaultExpression)VisitExpression(defaultExpression, p);
        var outerChanged = false;

        var newPrefix = VisitSpace(defaultExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, defaultExpression.Prefix)) outerChanged = true;

        var newMarkers = VisitMarkers(defaultExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, defaultExpression.Markers)) outerChanged = true;

        if (outerChanged)
        {
            defaultExpression = defaultExpression.WithPrefix(newPrefix).WithMarkers(newMarkers);
        }

        var newTypeOperator = VisitContainer(defaultExpression.TypeOperator, p);
        if (!ReferenceEquals(newTypeOperator, defaultExpression.TypeOperator))
        {
            return defaultExpression.WithTypeOperator(newTypeOperator);
        }
        return defaultExpression;
    }

    public virtual J VisitCsLambda(CsLambda csLambda, P p)
    {
        csLambda = (CsLambda)VisitStatement(csLambda, p);
        csLambda = (CsLambda)VisitExpression(csLambda, p);

        var newPrefix = VisitSpace(csLambda.Prefix, p);
        var newMarkers = VisitMarkers(csLambda.Markers, p);

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in csLambda.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }

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

        if (attrChanged || !ReferenceEquals(returnType, csLambda.ReturnType) ||
            (lambda is Lambda l && !ReferenceEquals(l, csLambda.LambdaExpression)) ||
            !ReferenceEquals(newPrefix, csLambda.Prefix) || !ReferenceEquals(newMarkers, csLambda.Markers))
        {
            return csLambda.WithPrefix(newPrefix).WithMarkers(newMarkers)
                           .WithAttributeLists(attrChanged ? newAttrLists : csLambda.AttributeLists)
                           .WithReturnType(returnType).WithLambdaExpression(lambda is Lambda newLambda ? newLambda : csLambda.LambdaExpression);
        }

        return csLambda;
    }

    public virtual J VisitRelationalPattern(RelationalPattern rp, P p)
    {
        rp = (RelationalPattern)VisitExpression(rp, p);
        var changed = false;

        var newPrefix = VisitSpace(rp.Prefix, p);
        if (!ReferenceEquals(newPrefix, rp.Prefix)) changed = true;

        var newMarkers = VisitMarkers(rp.Markers, p);
        if (!ReferenceEquals(newMarkers, rp.Markers)) changed = true;

        Expression newValue = rp.Value;
        var value = Visit(rp.Value, p);
        if (value is Expression v && !ReferenceEquals(v, rp.Value))
        {
            newValue = v;
            changed = true;
        }

        return changed
            ? rp.WithPrefix(newPrefix).WithMarkers(newMarkers).WithValue(newValue)
            : rp;
    }

    public virtual J VisitPropertyPattern(PropertyPattern pp, P p)
    {
        pp = (PropertyPattern)VisitExpression(pp, p);

        var newPrefix = VisitSpace(pp.Prefix, p);
        var newMarkers = VisitMarkers(pp.Markers, p);

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

        Identifier? newDesignation = pp.Designation;
        if (pp.Designation != null)
        {
            var visitedDesignation = Visit(pp.Designation, p);
            if (visitedDesignation is Identifier did && !ReferenceEquals(did, pp.Designation))
            {
                newDesignation = did;
            }
        }

        if (!ReferenceEquals(typeQualifier, pp.TypeQualifier) ||
            !ReferenceEquals(subpatterns, pp.Subpatterns) ||
            !ReferenceEquals(newDesignation, pp.Designation) ||
            !ReferenceEquals(newPrefix, pp.Prefix) || !ReferenceEquals(newMarkers, pp.Markers))
        {
            return pp.WithPrefix(newPrefix).WithMarkers(newMarkers)
                     .WithTypeQualifier(typeQualifier).WithSubpatterns(subpatterns).WithDesignation(newDesignation);
        }

        return pp;
    }

    public virtual J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(ctp.Prefix, p);
        if (!ReferenceEquals(newPrefix, ctp.Prefix)) changed = true;

        var newMarkers = VisitMarkers(ctp.Markers, p);
        if (!ReferenceEquals(newMarkers, ctp.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in ctp.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        Identifier newName = ctp.Name;
        var visitedName = Visit(ctp.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, ctp.Name))
        {
            newName = id;
            changed = true;
        }

        var newWhereConstraint = VisitLeftPadded(ctp.WhereConstraint, p);
        if (!ReferenceEquals(newWhereConstraint, ctp.WhereConstraint)) changed = true;

        var newConstraints = VisitContainer(ctp.Constraints, p);
        if (!ReferenceEquals(newConstraints, ctp.Constraints)) changed = true;

        JavaType? newType = (JavaType?)VisitType(ctp.Type, p);
        if (!ReferenceEquals(newType, ctp.Type)) changed = true;

        return changed
            ? ctp.WithPrefix(newPrefix).WithMarkers(newMarkers)
                 .WithAttributeLists(attrChanged ? newAttrLists : ctp.AttributeLists)
                 .WithName(newName).WithWhereConstraint(newWhereConstraint)
                 .WithConstraints(newConstraints).WithType(newType)
            : ctp;
    }

    public virtual J VisitInterpolatedString(InterpolatedString istr, P p)
    {
        istr = (InterpolatedString)VisitStatement(istr, p);
        istr = (InterpolatedString)VisitExpression(istr, p);
        bool changed = false;

        var newPrefix = VisitSpace(istr.Prefix, p);
        if (!ReferenceEquals(newPrefix, istr.Prefix)) changed = true;

        var newMarkers = VisitMarkers(istr.Markers, p);
        if (!ReferenceEquals(newMarkers, istr.Markers)) changed = true;

        var newParts = new List<J>();

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
            return istr.WithPrefix(newPrefix).WithMarkers(newMarkers).WithParts(newParts);
        }
        return istr;
    }

    public virtual J VisitInterpolation(Interpolation interp, P p)
    {
        var newPrefix = VisitSpace(interp.Prefix, p);
        var newMarkers = VisitMarkers(interp.Markers, p);

        bool changed = false;

        var expr = Visit(interp.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, interp.Expression)) changed = true;

        var newAlignment = VisitLeftPadded(interp.Alignment, p);
        if (!ReferenceEquals(newAlignment, interp.Alignment)) changed = true;

        var newFormat = VisitLeftPadded(interp.Format, p);
        if (!ReferenceEquals(newFormat, interp.Format)) changed = true;

        if (changed || !ReferenceEquals(newPrefix, interp.Prefix) || !ReferenceEquals(newMarkers, interp.Markers))
        {
            return interp.WithPrefix(newPrefix).WithMarkers(newMarkers)
                         .WithExpression(expr is Expression newExpr ? newExpr : interp.Expression)
                         .WithAlignment(newAlignment)
                         .WithFormat(newFormat);
        }

        return interp;
    }

    public virtual J VisitAwaitExpression(AwaitExpression ae, P p)
    {
        ae = (AwaitExpression)VisitStatement(ae, p);
        ae = (AwaitExpression)VisitExpression(ae, p);
        var changed = false;

        var newPrefix = VisitSpace(ae.Prefix, p);
        if (!ReferenceEquals(newPrefix, ae.Prefix)) changed = true;

        var newMarkers = VisitMarkers(ae.Markers, p);
        if (!ReferenceEquals(newMarkers, ae.Markers)) changed = true;

        Expression newExpr = ae.Expression;
        var expr = Visit(ae.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ae.Expression))
        {
            newExpr = e;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(ae.Type, p);
        if (!ReferenceEquals(newType, ae.Type)) changed = true;

        return changed
            ? ae.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr).WithType(newType)
            : ae;
    }

    public virtual J VisitYield(Yield yield, P p)
    {
        yield = (Yield)VisitStatement(yield, p);
        var changed = false;

        var newPrefix = VisitSpace(yield.Prefix, p);
        if (!ReferenceEquals(newPrefix, yield.Prefix)) changed = true;

        var newMarkers = VisitMarkers(yield.Markers, p);
        if (!ReferenceEquals(newMarkers, yield.Markers)) changed = true;

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
            ? yield.WithPrefix(newPrefix).WithMarkers(newMarkers).WithReturnOrBreakKeyword(newKeyword).WithExpression(newExpr)
            : yield;
    }

    public virtual J VisitNamespaceDeclaration(NamespaceDeclaration ns, P p)
    {
        ns = (NamespaceDeclaration)VisitStatement(ns, p);
        bool changed = false;

        var newPrefix = VisitSpace(ns.Prefix, p);
        if (!ReferenceEquals(newPrefix, ns.Prefix)) changed = true;

        var newMarkers = VisitMarkers(ns.Markers, p);
        if (!ReferenceEquals(newMarkers, ns.Markers)) changed = true;

        var newNamePadded = VisitRightPadded(ns.Name, p)!;
        if (!ReferenceEquals(newNamePadded, ns.Name)) changed = true;

        var newMembers = new List<JRightPadded<Statement>>();
        bool membersChanged = false;
        foreach (var member in ns.Members)
        {
            var visited = VisitRightPadded(member, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, member)) membersChanged = true;
                newMembers.Add(visited);
            }
            else membersChanged = true;
        }
        if (membersChanged) changed = true;

        return changed
            ? ns.WithPrefix(newPrefix).WithMarkers(newMarkers)
                .WithName(newNamePadded)
                .WithMembers(membersChanged ? newMembers : ns.Members)
            : ns;
    }

    public virtual J VisitTupleType(TupleType tupleType, P p)
    {
        tupleType = (TupleType)VisitExpression(tupleType, p);
        bool prefixMarkersChanged = false;

        var newPrefix = VisitSpace(tupleType.Prefix, p);
        if (!ReferenceEquals(newPrefix, tupleType.Prefix)) prefixMarkersChanged = true;

        var newMarkers = VisitMarkers(tupleType.Markers, p);
        if (!ReferenceEquals(newMarkers, tupleType.Markers)) prefixMarkersChanged = true;

        var newElements = VisitContainer(tupleType.Elements, p);
        bool elementsChanged = !ReferenceEquals(newElements, tupleType.Elements);

        JavaType? newType = (JavaType?)VisitType(tupleType.Type, p);
        if (!ReferenceEquals(newType, tupleType.Type)) elementsChanged = true;

        if (elementsChanged || prefixMarkersChanged)
        {
            return tupleType.WithPrefix(newPrefix).WithMarkers(newMarkers)
                            .WithElements(newElements ?? tupleType.Elements).WithType(newType);
        }

        return tupleType;
    }

    public virtual J VisitTupleExpression(TupleExpression tupleExpr, P p)
    {
        tupleExpr = (TupleExpression)VisitExpression(tupleExpr, p);
        bool prefixMarkersChanged = false;

        var newPrefix = VisitSpace(tupleExpr.Prefix, p);
        if (!ReferenceEquals(newPrefix, tupleExpr.Prefix)) prefixMarkersChanged = true;

        var newMarkers = VisitMarkers(tupleExpr.Markers, p);
        if (!ReferenceEquals(newMarkers, tupleExpr.Markers)) prefixMarkersChanged = true;

        var newArgs = VisitContainer(tupleExpr.Arguments, p);
        bool argsChanged = !ReferenceEquals(newArgs, tupleExpr.Arguments);

        if (argsChanged || prefixMarkersChanged)
        {
            return tupleExpr.WithPrefix(newPrefix).WithMarkers(newMarkers)
                            .WithArguments(newArgs ?? tupleExpr.Arguments);
        }

        return tupleExpr;
    }

    public virtual J VisitConditionalDirective(ConditionalDirective conditionalDirective, P p)
    {
        conditionalDirective = (ConditionalDirective)VisitStatement(conditionalDirective, p);

        var newPrefix = VisitSpace(conditionalDirective.Prefix, p);
        var newMarkers = VisitMarkers(conditionalDirective.Markers, p);

        var branches = new List<JRightPadded<CompilationUnit>>();
        foreach (var branch in conditionalDirective.Branches)
        {
            var visited = (CompilationUnit?)Visit(branch.Element, p);
            if (visited != null)
                branches.Add(branch.WithElement(visited));
        }
        return conditionalDirective.WithPrefix(newPrefix).WithMarkers(newMarkers).WithBranches(branches);
    }

    public virtual J VisitPragmaWarningDirective(PragmaWarningDirective pragmaWarningDirective, P p)
    {
        pragmaWarningDirective = (PragmaWarningDirective)VisitStatement(pragmaWarningDirective, p);
        bool changed = false;

        var newPrefix = VisitSpace(pragmaWarningDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, pragmaWarningDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(pragmaWarningDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, pragmaWarningDirective.Markers)) changed = true;

        var newCodes = new List<JRightPadded<Expression>>();
        bool codesChanged = false;
        foreach (var code in pragmaWarningDirective.WarningCodes)
        {
            var visited = VisitRightPadded(code, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, code)) codesChanged = true;
                newCodes.Add(visited);
            }
            else codesChanged = true;
        }
        if (codesChanged) changed = true;
        return changed
            ? pragmaWarningDirective.WithPrefix(newPrefix).WithMarkers(newMarkers).WithWarningCodes(codesChanged ? newCodes : pragmaWarningDirective.WarningCodes)
            : pragmaWarningDirective;
    }

    public virtual J VisitPragmaChecksumDirective(PragmaChecksumDirective pragmaChecksumDirective, P p)
    {
        pragmaChecksumDirective = (PragmaChecksumDirective)VisitStatement(pragmaChecksumDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(pragmaChecksumDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, pragmaChecksumDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(pragmaChecksumDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, pragmaChecksumDirective.Markers)) changed = true;

        return changed
            ? pragmaChecksumDirective.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : pragmaChecksumDirective;
    }

    public virtual J VisitNullableDirective(NullableDirective nullableDirective, P p)
    {
        nullableDirective = (NullableDirective)VisitStatement(nullableDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(nullableDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, nullableDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(nullableDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, nullableDirective.Markers)) changed = true;

        return changed
            ? nullableDirective.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : nullableDirective;
    }

    public virtual J VisitRegionDirective(RegionDirective regionDirective, P p)
    {
        regionDirective = (RegionDirective)VisitStatement(regionDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(regionDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, regionDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(regionDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, regionDirective.Markers)) changed = true;

        return changed
            ? regionDirective.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : regionDirective;
    }

    public virtual J VisitEndRegionDirective(EndRegionDirective endRegionDirective, P p)
    {
        endRegionDirective = (EndRegionDirective)VisitStatement(endRegionDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(endRegionDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, endRegionDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(endRegionDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, endRegionDirective.Markers)) changed = true;

        return changed
            ? endRegionDirective.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : endRegionDirective;
    }

    public virtual J VisitDefineDirective(DefineDirective defineDirective, P p)
    {
        defineDirective = (DefineDirective)VisitStatement(defineDirective, p);

        var newPrefix = VisitSpace(defineDirective.Prefix, p);
        var newMarkers = VisitMarkers(defineDirective.Markers, p);

        var symbol = (Identifier?)Visit(defineDirective.Symbol, p);
        return defineDirective.WithPrefix(newPrefix).WithMarkers(newMarkers).WithSymbol(symbol!);
    }

    public virtual J VisitUndefDirective(UndefDirective undefDirective, P p)
    {
        undefDirective = (UndefDirective)VisitStatement(undefDirective, p);

        var newPrefix = VisitSpace(undefDirective.Prefix, p);
        var newMarkers = VisitMarkers(undefDirective.Markers, p);

        var symbol = (Identifier?)Visit(undefDirective.Symbol, p);
        return undefDirective.WithPrefix(newPrefix).WithMarkers(newMarkers).WithSymbol(symbol!);
    }

    public virtual J VisitErrorDirective(ErrorDirective errorDirective, P p)
    {
        errorDirective = (ErrorDirective)VisitStatement(errorDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(errorDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, errorDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(errorDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, errorDirective.Markers)) changed = true;

        return changed
            ? errorDirective.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : errorDirective;
    }

    public virtual J VisitWarningDirective(WarningDirective warningDirective, P p)
    {
        warningDirective = (WarningDirective)VisitStatement(warningDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(warningDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, warningDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(warningDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, warningDirective.Markers)) changed = true;

        return changed
            ? warningDirective.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : warningDirective;
    }

    public virtual J VisitLineDirective(LineDirective lineDirective, P p)
    {
        lineDirective = (LineDirective)VisitStatement(lineDirective, p);
        var changed = false;

        var newPrefix = VisitSpace(lineDirective.Prefix, p);
        if (!ReferenceEquals(newPrefix, lineDirective.Prefix)) changed = true;

        var newMarkers = VisitMarkers(lineDirective.Markers, p);
        if (!ReferenceEquals(newMarkers, lineDirective.Markers)) changed = true;

        Expression? newLine = lineDirective.Line;
        if (lineDirective.Line != null)
        {
            var visited = Visit(lineDirective.Line, p);
            if (visited is Expression e && !ReferenceEquals(e, lineDirective.Line))
            {
                newLine = e;
                changed = true;
            }
        }

        Expression? newFile = lineDirective.File;
        if (lineDirective.File != null)
        {
            var visited = Visit(lineDirective.File, p);
            if (visited is Expression e && !ReferenceEquals(e, lineDirective.File))
            {
                newFile = e;
                changed = true;
            }
        }

        return changed
            ? lineDirective.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLine(newLine).WithFile(newFile)
            : lineDirective;
    }

    // ---- New types ----

    public virtual J VisitKeyword(Keyword keyword, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(keyword.Prefix, p);
        if (!ReferenceEquals(newPrefix, keyword.Prefix)) changed = true;

        var newMarkers = VisitMarkers(keyword.Markers, p);
        if (!ReferenceEquals(newMarkers, keyword.Markers)) changed = true;

        return changed
            ? keyword.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : keyword;
    }

    public virtual J VisitNameColon(NameColon nameColon, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(nameColon.Prefix, p);
        if (!ReferenceEquals(newPrefix, nameColon.Prefix)) changed = true;

        var newMarkers = VisitMarkers(nameColon.Markers, p);
        if (!ReferenceEquals(newMarkers, nameColon.Markers)) changed = true;

        var newName = VisitRightPadded(nameColon.Name, p)!;
        if (!ReferenceEquals(newName, nameColon.Name)) changed = true;

        return changed
            ? nameColon.WithPrefix(newPrefix).WithMarkers(newMarkers).WithName(newName)
            : nameColon;
    }

    public virtual J VisitAnnotatedStatement(AnnotatedStatement annotatedStatement, P p)
    {
        annotatedStatement = (AnnotatedStatement)VisitStatement(annotatedStatement, p);
        var changed = false;

        var newPrefix = VisitSpace(annotatedStatement.Prefix, p);
        if (!ReferenceEquals(newPrefix, annotatedStatement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(annotatedStatement.Markers, p);
        if (!ReferenceEquals(newMarkers, annotatedStatement.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in annotatedStatement.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        Statement newStmt = annotatedStatement.Statement;
        var visitedStmt = Visit(annotatedStatement.Statement, p);
        if (visitedStmt is Statement s && !ReferenceEquals(s, annotatedStatement.Statement))
        {
            newStmt = s;
            changed = true;
        }

        return changed
            ? annotatedStatement.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                .WithAttributeLists(attrChanged ? newAttrLists : annotatedStatement.AttributeLists)
                                .WithStatement(newStmt)
            : annotatedStatement;
    }

    public virtual J VisitArrayRankSpecifier(ArrayRankSpecifier arrayRankSpecifier, P p)
    {
        arrayRankSpecifier = (ArrayRankSpecifier)VisitExpression(arrayRankSpecifier, p);
        bool changed = false;

        var newPrefix = VisitSpace(arrayRankSpecifier.Prefix, p);
        if (!ReferenceEquals(newPrefix, arrayRankSpecifier.Prefix)) changed = true;

        var newMarkers = VisitMarkers(arrayRankSpecifier.Markers, p);
        if (!ReferenceEquals(newMarkers, arrayRankSpecifier.Markers)) changed = true;

        var newSizes = VisitContainer(arrayRankSpecifier.Sizes, p);
        if (!ReferenceEquals(newSizes, arrayRankSpecifier.Sizes)) changed = true;

        return changed
            ? arrayRankSpecifier.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                .WithSizes(newSizes ?? arrayRankSpecifier.Sizes)
            : arrayRankSpecifier;
    }

    public virtual J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        assignmentOperation = (AssignmentOperation)VisitStatement(assignmentOperation, p);
        assignmentOperation = (AssignmentOperation)VisitExpression(assignmentOperation, p);
        var changed = false;

        var newPrefix = VisitSpace(assignmentOperation.Prefix, p);
        if (!ReferenceEquals(newPrefix, assignmentOperation.Prefix)) changed = true;

        var newMarkers = VisitMarkers(assignmentOperation.Markers, p);
        if (!ReferenceEquals(newMarkers, assignmentOperation.Markers)) changed = true;

        Expression newVar = assignmentOperation.Variable;
        var visitedVar = Visit(assignmentOperation.Variable, p);
        if (visitedVar is Expression v && !ReferenceEquals(v, assignmentOperation.Variable))
        {
            newVar = v;
            changed = true;
        }

        Expression newValue = assignmentOperation.AssignmentValue;
        var visitedValue = Visit(assignmentOperation.AssignmentValue, p);
        if (visitedValue is Expression a && !ReferenceEquals(a, assignmentOperation.AssignmentValue))
        {
            newValue = a;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(assignmentOperation.Type, p);
        if (!ReferenceEquals(newType, assignmentOperation.Type)) changed = true;

        return changed
            ? assignmentOperation.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                 .WithVariable(newVar).WithAssignmentValue(newValue).WithType(newType)
            : assignmentOperation;
    }

    public virtual J VisitStackAllocExpression(StackAllocExpression stackAllocExpression, P p)
    {
        stackAllocExpression = (StackAllocExpression)VisitExpression(stackAllocExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(stackAllocExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, stackAllocExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(stackAllocExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, stackAllocExpression.Markers)) changed = true;

        NewArray newExpr = stackAllocExpression.Expression;
        var visited = Visit(stackAllocExpression.Expression, p);
        if (visited is NewArray na && !ReferenceEquals(na, stackAllocExpression.Expression))
        {
            newExpr = na;
            changed = true;
        }

        return changed
            ? stackAllocExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr)
            : stackAllocExpression;
    }

    public virtual J VisitGotoStatement(GotoStatement gotoStatement, P p)
    {
        gotoStatement = (GotoStatement)VisitStatement(gotoStatement, p);
        var changed = false;

        var newPrefix = VisitSpace(gotoStatement.Prefix, p);
        if (!ReferenceEquals(newPrefix, gotoStatement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(gotoStatement.Markers, p);
        if (!ReferenceEquals(newMarkers, gotoStatement.Markers)) changed = true;

        Keyword? newKw = gotoStatement.CaseOrDefaultKeyword;
        if (gotoStatement.CaseOrDefaultKeyword != null)
        {
            var visitedKw = Visit(gotoStatement.CaseOrDefaultKeyword, p);
            if (visitedKw is Keyword kw && !ReferenceEquals(kw, gotoStatement.CaseOrDefaultKeyword))
            {
                newKw = kw;
                changed = true;
            }
        }

        Expression? newTarget = gotoStatement.Target;
        if (gotoStatement.Target != null)
        {
            var visited = Visit(gotoStatement.Target, p);
            if (visited is Expression e && !ReferenceEquals(e, gotoStatement.Target))
            {
                newTarget = e;
                changed = true;
            }
        }

        return changed
            ? gotoStatement.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCaseOrDefaultKeyword(newKw).WithTarget(newTarget)
            : gotoStatement;
    }

    public virtual J VisitEventDeclaration(EventDeclaration eventDeclaration, P p)
    {
        eventDeclaration = (EventDeclaration)VisitStatement(eventDeclaration, p);
        var changed = false;

        var newPrefix = VisitSpace(eventDeclaration.Prefix, p);
        if (!ReferenceEquals(newPrefix, eventDeclaration.Prefix)) changed = true;

        var newMarkers = VisitMarkers(eventDeclaration.Markers, p);
        if (!ReferenceEquals(newMarkers, eventDeclaration.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in eventDeclaration.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        var newTypeExpr = VisitLeftPadded(eventDeclaration.TypeExpressionPadded, p)!;
        if (!ReferenceEquals(newTypeExpr, eventDeclaration.TypeExpressionPadded)) changed = true;

        Identifier newName = eventDeclaration.Name;
        var visitedName = Visit(eventDeclaration.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, eventDeclaration.Name))
        {
            newName = id;
            changed = true;
        }

        var newInterfaceSpec = VisitRightPadded(eventDeclaration.InterfaceSpecifier, p);
        if (!ReferenceEquals(newInterfaceSpec, eventDeclaration.InterfaceSpecifier)) changed = true;

        var newAccessors = VisitContainer(eventDeclaration.Accessors, p);
        if (!ReferenceEquals(newAccessors, eventDeclaration.Accessors)) changed = true;

        return changed
            ? eventDeclaration.WithPrefix(newPrefix).WithMarkers(newMarkers)
                              .WithAttributeLists(attrChanged ? newAttrLists : eventDeclaration.AttributeLists)
                              .WithTypeExpressionPadded(newTypeExpr).WithInterfaceSpecifier(newInterfaceSpec)
                              .WithName(newName).WithAccessors(newAccessors)
            : eventDeclaration;
    }

    public virtual J VisitCsBinary(CsBinary csBinary, P p)
    {
        csBinary = (CsBinary)VisitExpression(csBinary, p);
        var changed = false;

        var newPrefix = VisitSpace(csBinary.Prefix, p);
        if (!ReferenceEquals(newPrefix, csBinary.Prefix)) changed = true;

        var newMarkers = VisitMarkers(csBinary.Markers, p);
        if (!ReferenceEquals(newMarkers, csBinary.Markers)) changed = true;

        Expression newLeft = csBinary.Left;
        var left = Visit(csBinary.Left, p);
        if (left is Expression l && !ReferenceEquals(l, csBinary.Left))
        {
            newLeft = l;
            changed = true;
        }

        Expression newRight = csBinary.Right;
        var right = Visit(csBinary.Right, p);
        if (right is Expression r && !ReferenceEquals(r, csBinary.Right))
        {
            newRight = r;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(csBinary.Type, p);
        if (!ReferenceEquals(newType, csBinary.Type)) changed = true;

        return changed
            ? csBinary.WithPrefix(newPrefix).WithMarkers(newMarkers).WithLeft(newLeft).WithRight(newRight).WithType(newType)
            : csBinary;
    }

    public virtual J VisitCollectionExpression(CollectionExpression collectionExpression, P p)
    {
        collectionExpression = (CollectionExpression)VisitExpression(collectionExpression, p);
        bool changed = false;

        var newPrefix = VisitSpace(collectionExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, collectionExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(collectionExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, collectionExpression.Markers)) changed = true;

        var newElements = new List<JRightPadded<Expression>>();
        bool elementsChanged = false;
        foreach (var elem in collectionExpression.Elements)
        {
            var visited = VisitRightPadded(elem, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, elem)) elementsChanged = true;
                newElements.Add(visited);
            }
            else elementsChanged = true;
        }
        if (elementsChanged) changed = true;

        JavaType? newType = (JavaType?)VisitType(collectionExpression.Type, p);
        if (!ReferenceEquals(newType, collectionExpression.Type)) changed = true;

        return changed
            ? collectionExpression.WithPrefix(newPrefix).WithMarkers(newMarkers)
                .WithElements(elementsChanged ? newElements : collectionExpression.Elements).WithType(newType)
            : collectionExpression;
    }

    public virtual J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, P p)
    {
        forEachVariableLoop = (ForEachVariableLoop)VisitStatement(forEachVariableLoop, p);
        var changed = false;

        var newPrefix = VisitSpace(forEachVariableLoop.Prefix, p);
        if (!ReferenceEquals(newPrefix, forEachVariableLoop.Prefix)) changed = true;

        var newMarkers = VisitMarkers(forEachVariableLoop.Markers, p);
        if (!ReferenceEquals(newMarkers, forEachVariableLoop.Markers)) changed = true;

        ForEachVariableLoopControl newControl = forEachVariableLoop.ControlElement;
        var visitedControl = Visit(forEachVariableLoop.ControlElement, p);
        if (visitedControl is ForEachVariableLoopControl fc && !ReferenceEquals(fc, forEachVariableLoop.ControlElement))
        {
            newControl = fc;
            changed = true;
        }

        var newBody = VisitRightPadded(forEachVariableLoop.Body, p)!;
        if (!ReferenceEquals(newBody, forEachVariableLoop.Body)) changed = true;

        return changed
            ? forEachVariableLoop.WithPrefix(newPrefix).WithMarkers(newMarkers).WithControlElement(newControl).WithBody(newBody)
            : forEachVariableLoop;
    }

    public virtual J VisitForEachVariableLoopControl(ForEachVariableLoopControl control, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(control.Prefix, p);
        if (!ReferenceEquals(newPrefix, control.Prefix)) changed = true;

        var newMarkers = VisitMarkers(control.Markers, p);
        if (!ReferenceEquals(newMarkers, control.Markers)) changed = true;

        var newVariable = VisitRightPadded(control.Variable, p)!;
        if (!ReferenceEquals(newVariable, control.Variable)) changed = true;

        var newIterable = VisitRightPadded(control.Iterable, p)!;
        if (!ReferenceEquals(newIterable, control.Iterable)) changed = true;

        return changed
            ? control.WithPrefix(newPrefix).WithMarkers(newMarkers).WithVariable(newVariable).WithIterable(newIterable)
            : control;
    }

    public virtual J VisitUsingStatement(UsingStatement usingStatement, P p)
    {
        usingStatement = (UsingStatement)VisitStatement(usingStatement, p);
        var changed = false;

        var newPrefix = VisitSpace(usingStatement.Prefix, p);
        if (!ReferenceEquals(newPrefix, usingStatement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(usingStatement.Markers, p);
        if (!ReferenceEquals(newMarkers, usingStatement.Markers)) changed = true;

        var newExpr = VisitLeftPadded(usingStatement.ExpressionPadded, p)!;
        if (!ReferenceEquals(newExpr, usingStatement.ExpressionPadded)) changed = true;

        Statement newStmt = usingStatement.Statement;
        var visitedStmt = Visit(usingStatement.Statement, p);
        if (visitedStmt is Statement s && !ReferenceEquals(s, usingStatement.Statement))
        {
            newStmt = s;
            changed = true;
        }

        return changed
            ? usingStatement.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpressionPadded(newExpr).WithStatement(newStmt)
            : usingStatement;
    }

    public virtual J VisitAllowsConstraintClause(AllowsConstraintClause clause, P p)
    {
        clause = (AllowsConstraintClause)VisitExpression(clause, p);
        bool changed = false;

        var newPrefix = VisitSpace(clause.Prefix, p);
        if (!ReferenceEquals(newPrefix, clause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(clause.Markers, p);
        if (!ReferenceEquals(newMarkers, clause.Markers)) changed = true;

        var newExprs = VisitContainer(clause.Expressions, p);
        if (!ReferenceEquals(newExprs, clause.Expressions)) changed = true;

        return changed
            ? clause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpressions(newExprs ?? clause.Expressions)
            : clause;
    }

    public virtual J VisitRefStructConstraint(RefStructConstraint constraint, P p)
    {
        constraint = (RefStructConstraint)VisitExpression(constraint, p);
        var changed = false;

        var newPrefix = VisitSpace(constraint.Prefix, p);
        if (!ReferenceEquals(newPrefix, constraint.Prefix)) changed = true;

        var newMarkers = VisitMarkers(constraint.Markers, p);
        if (!ReferenceEquals(newMarkers, constraint.Markers)) changed = true;

        return changed
            ? constraint.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : constraint;
    }

    public virtual J VisitClassOrStructConstraint(ClassOrStructConstraint constraint, P p)
    {
        constraint = (ClassOrStructConstraint)VisitExpression(constraint, p);
        var changed = false;

        var newPrefix = VisitSpace(constraint.Prefix, p);
        if (!ReferenceEquals(newPrefix, constraint.Prefix)) changed = true;

        var newMarkers = VisitMarkers(constraint.Markers, p);
        if (!ReferenceEquals(newMarkers, constraint.Markers)) changed = true;

        return changed
            ? constraint.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : constraint;
    }

    public virtual J VisitConstructorConstraint(ConstructorConstraint constraint, P p)
    {
        constraint = (ConstructorConstraint)VisitExpression(constraint, p);
        var changed = false;

        var newPrefix = VisitSpace(constraint.Prefix, p);
        if (!ReferenceEquals(newPrefix, constraint.Prefix)) changed = true;

        var newMarkers = VisitMarkers(constraint.Markers, p);
        if (!ReferenceEquals(newMarkers, constraint.Markers)) changed = true;

        return changed
            ? constraint.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : constraint;
    }

    public virtual J VisitDefaultConstraint(DefaultConstraint constraint, P p)
    {
        constraint = (DefaultConstraint)VisitExpression(constraint, p);
        var changed = false;

        var newPrefix = VisitSpace(constraint.Prefix, p);
        if (!ReferenceEquals(newPrefix, constraint.Prefix)) changed = true;

        var newMarkers = VisitMarkers(constraint.Markers, p);
        if (!ReferenceEquals(newMarkers, constraint.Markers)) changed = true;

        return changed
            ? constraint.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : constraint;
    }

    public virtual J VisitSingleVariableDesignation(SingleVariableDesignation designation, P p)
    {
        designation = (SingleVariableDesignation)VisitExpression(designation, p);
        var changed = false;

        var newPrefix = VisitSpace(designation.Prefix, p);
        if (!ReferenceEquals(newPrefix, designation.Prefix)) changed = true;

        var newMarkers = VisitMarkers(designation.Markers, p);
        if (!ReferenceEquals(newMarkers, designation.Markers)) changed = true;

        Identifier newName = designation.Name;
        var visited = Visit(designation.Name, p);
        if (visited is Identifier id && !ReferenceEquals(id, designation.Name))
        {
            newName = id;
            changed = true;
        }

        return changed
            ? designation.WithPrefix(newPrefix).WithMarkers(newMarkers).WithName(newName)
            : designation;
    }

    public virtual J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation designation, P p)
    {
        designation = (ParenthesizedVariableDesignation)VisitExpression(designation, p);
        var changed = false;

        var newPrefix = VisitSpace(designation.Prefix, p);
        if (!ReferenceEquals(newPrefix, designation.Prefix)) changed = true;

        var newMarkers = VisitMarkers(designation.Markers, p);
        if (!ReferenceEquals(newMarkers, designation.Markers)) changed = true;

        var newVars = VisitContainer(designation.Variables, p);
        if (!ReferenceEquals(newVars, designation.Variables)) changed = true;

        JavaType? newType = (JavaType?)VisitType(designation.Type, p);
        if (!ReferenceEquals(newType, designation.Type)) changed = true;

        return changed
            ? designation.WithPrefix(newPrefix).WithMarkers(newMarkers)
                         .WithVariables(newVars ?? designation.Variables).WithType(newType)
            : designation;
    }

    public virtual J VisitDiscardVariableDesignation(DiscardVariableDesignation designation, P p)
    {
        designation = (DiscardVariableDesignation)VisitExpression(designation, p);
        var changed = false;

        var newPrefix = VisitSpace(designation.Prefix, p);
        if (!ReferenceEquals(newPrefix, designation.Prefix)) changed = true;

        var newMarkers = VisitMarkers(designation.Markers, p);
        if (!ReferenceEquals(newMarkers, designation.Markers)) changed = true;

        Identifier newDiscard = designation.Discard;
        var visited = Visit(designation.Discard, p);
        if (visited is Identifier id && !ReferenceEquals(id, designation.Discard))
        {
            newDiscard = id;
            changed = true;
        }

        return changed
            ? designation.WithPrefix(newPrefix).WithMarkers(newMarkers).WithDiscard(newDiscard)
            : designation;
    }

    public virtual J VisitCsUnary(CsUnary csUnary, P p)
    {
        csUnary = (CsUnary)VisitStatement(csUnary, p);
        csUnary = (CsUnary)VisitExpression(csUnary, p);
        var changed = false;

        var newPrefix = VisitSpace(csUnary.Prefix, p);
        if (!ReferenceEquals(newPrefix, csUnary.Prefix)) changed = true;

        var newMarkers = VisitMarkers(csUnary.Markers, p);
        if (!ReferenceEquals(newMarkers, csUnary.Markers)) changed = true;

        Expression newExpr = csUnary.Expression;
        var visited = Visit(csUnary.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, csUnary.Expression))
        {
            newExpr = e;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(csUnary.Type, p);
        if (!ReferenceEquals(newType, csUnary.Type)) changed = true;

        return changed
            ? csUnary.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr).WithType(newType)
            : csUnary;
    }

    public virtual J VisitTupleElement(TupleElement tupleElement, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(tupleElement.Prefix, p);
        if (!ReferenceEquals(newPrefix, tupleElement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(tupleElement.Markers, p);
        if (!ReferenceEquals(newMarkers, tupleElement.Markers)) changed = true;

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
            ? tupleElement.WithPrefix(newPrefix).WithMarkers(newMarkers).WithElementType(newType).WithName(newName)
            : tupleElement;
    }

    public virtual J VisitConstantPattern(ConstantPattern constantPattern, P p)
    {
        constantPattern = (ConstantPattern)VisitExpression(constantPattern, p);
        var changed = false;

        var newPrefix = VisitSpace(constantPattern.Prefix, p);
        if (!ReferenceEquals(newPrefix, constantPattern.Prefix)) changed = true;

        var newMarkers = VisitMarkers(constantPattern.Markers, p);
        if (!ReferenceEquals(newMarkers, constantPattern.Markers)) changed = true;

        Expression newValue = constantPattern.Value;
        var visited = Visit(constantPattern.Value, p);
        if (visited is Expression e && !ReferenceEquals(e, constantPattern.Value))
        {
            newValue = e;
            changed = true;
        }

        return changed
            ? constantPattern.WithPrefix(newPrefix).WithMarkers(newMarkers).WithValue(newValue)
            : constantPattern;
    }

    public virtual J VisitDiscardPattern(DiscardPattern discardPattern, P p)
    {
        discardPattern = (DiscardPattern)VisitExpression(discardPattern, p);
        var changed = false;

        var newPrefix = VisitSpace(discardPattern.Prefix, p);
        if (!ReferenceEquals(newPrefix, discardPattern.Prefix)) changed = true;

        var newMarkers = VisitMarkers(discardPattern.Markers, p);
        if (!ReferenceEquals(newMarkers, discardPattern.Markers)) changed = true;

        JavaType? newType = (JavaType?)VisitType(discardPattern.Type, p);
        if (!ReferenceEquals(newType, discardPattern.Type)) changed = true;

        return changed
            ? discardPattern.WithPrefix(newPrefix).WithMarkers(newMarkers).WithType(newType)
            : discardPattern;
    }

    public virtual J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, P p)
    {
        implicitElementAccess = (ImplicitElementAccess)VisitExpression(implicitElementAccess, p);
        bool changed = false;

        var newPrefix = VisitSpace(implicitElementAccess.Prefix, p);
        if (!ReferenceEquals(newPrefix, implicitElementAccess.Prefix)) changed = true;

        var newMarkers = VisitMarkers(implicitElementAccess.Markers, p);
        if (!ReferenceEquals(newMarkers, implicitElementAccess.Markers)) changed = true;

        var newArgs = VisitContainer(implicitElementAccess.ArgumentList, p);
        if (!ReferenceEquals(newArgs, implicitElementAccess.ArgumentList)) changed = true;

        return changed
            ? implicitElementAccess.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                   .WithArgumentList(newArgs ?? implicitElementAccess.ArgumentList)
            : implicitElementAccess;
    }

    public virtual J VisitSlicePattern(SlicePattern slicePattern, P p)
    {
        slicePattern = (SlicePattern)VisitExpression(slicePattern, p);
        var changed = false;

        var newPrefix = VisitSpace(slicePattern.Prefix, p);
        if (!ReferenceEquals(newPrefix, slicePattern.Prefix)) changed = true;

        var newMarkers = VisitMarkers(slicePattern.Markers, p);
        if (!ReferenceEquals(newMarkers, slicePattern.Markers)) changed = true;

        return changed
            ? slicePattern.WithPrefix(newPrefix).WithMarkers(newMarkers)
            : slicePattern;
    }

    public virtual J VisitListPattern(ListPattern listPattern, P p)
    {
        listPattern = (ListPattern)VisitExpression(listPattern, p);
        var changed = false;

        var newPrefix = VisitSpace(listPattern.Prefix, p);
        if (!ReferenceEquals(newPrefix, listPattern.Prefix)) changed = true;

        var newMarkers = VisitMarkers(listPattern.Markers, p);
        if (!ReferenceEquals(newMarkers, listPattern.Markers)) changed = true;

        var newPatterns = VisitContainer(listPattern.Patterns, p);
        bool patternsChanged = !ReferenceEquals(newPatterns, listPattern.Patterns);
        if (patternsChanged) changed = true;

        VariableDesignation? newDesignation = listPattern.Designation;
        if (listPattern.Designation != null)
        {
            var visited = Visit(listPattern.Designation, p);
            if (visited is VariableDesignation v && !ReferenceEquals(v, listPattern.Designation))
            {
                newDesignation = v;
                changed = true;
            }
        }

        return changed
            ? listPattern.WithPrefix(newPrefix).WithMarkers(newMarkers)
                         .WithPatterns(newPatterns ?? listPattern.Patterns).WithDesignation(newDesignation)
            : listPattern;
    }

    public virtual J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        switchExpression = (SwitchExpression)VisitExpression(switchExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(switchExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, switchExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(switchExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, switchExpression.Markers)) changed = true;

        var newExpr = VisitRightPadded(switchExpression.ExpressionPadded, p)!;
        if (!ReferenceEquals(newExpr, switchExpression.ExpressionPadded)) changed = true;

        var newArms = VisitContainer(switchExpression.Arms, p);
        if (!ReferenceEquals(newArms, switchExpression.Arms)) changed = true;

        return changed
            ? switchExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpressionPadded(newExpr).WithArms(newArms ?? switchExpression.Arms)
            : switchExpression;
    }

    public virtual J VisitSwitchExpressionArm(SwitchExpressionArm arm, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(arm.Prefix, p);
        if (!ReferenceEquals(newPrefix, arm.Prefix)) changed = true;

        var newMarkers = VisitMarkers(arm.Markers, p);
        if (!ReferenceEquals(newMarkers, arm.Markers)) changed = true;

        J newPattern = arm.Pattern;
        var visitedPattern = Visit(arm.Pattern, p);
        if (visitedPattern is J pat && !ReferenceEquals(pat, arm.Pattern))
        {
            newPattern = pat;
            changed = true;
        }

        var newWhen = VisitLeftPadded(arm.WhenExpression, p);
        if (!ReferenceEquals(newWhen, arm.WhenExpression)) changed = true;

        var newExpr = VisitLeftPadded(arm.ExpressionPadded, p)!;
        if (!ReferenceEquals(newExpr, arm.ExpressionPadded)) changed = true;

        return changed
            ? arm.WithPrefix(newPrefix).WithMarkers(newMarkers).WithPattern(newPattern).WithWhenExpression(newWhen).WithExpressionPadded(newExpr)
            : arm;
    }

    public virtual J VisitCheckedExpression(CheckedExpression checkedExpression, P p)
    {
        checkedExpression = (CheckedExpression)VisitExpression(checkedExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(checkedExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, checkedExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(checkedExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, checkedExpression.Markers)) changed = true;

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
            ? checkedExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCheckedOrUncheckedKeyword(newKw).WithExpressionValue(newExpr)
            : checkedExpression;
    }

    public virtual J VisitCheckedStatement(CheckedStatement checkedStatement, P p)
    {
        checkedStatement = (CheckedStatement)VisitStatement(checkedStatement, p);
        var changed = false;

        var newPrefix = VisitSpace(checkedStatement.Prefix, p);
        if (!ReferenceEquals(newPrefix, checkedStatement.Prefix)) changed = true;

        var newMarkers = VisitMarkers(checkedStatement.Markers, p);
        if (!ReferenceEquals(newMarkers, checkedStatement.Markers)) changed = true;

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
            ? checkedStatement.WithPrefix(newPrefix).WithMarkers(newMarkers).WithKeywordValue(newKw).WithBlock(newBlock)
            : checkedStatement;
    }

    public virtual J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        rangeExpression = (RangeExpression)VisitExpression(rangeExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(rangeExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, rangeExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(rangeExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, rangeExpression.Markers)) changed = true;

        var newStart = VisitRightPadded(rangeExpression.Start, p);
        if (!ReferenceEquals(newStart, rangeExpression.Start)) changed = true;

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
            ? rangeExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithStart(newStart).WithEnd(newEnd)
            : rangeExpression;
    }

    public virtual J VisitIndexerDeclaration(IndexerDeclaration indexerDeclaration, P p)
    {
        indexerDeclaration = (IndexerDeclaration)VisitStatement(indexerDeclaration, p);
        var changed = false;

        var newPrefix = VisitSpace(indexerDeclaration.Prefix, p);
        if (!ReferenceEquals(newPrefix, indexerDeclaration.Prefix)) changed = true;

        var newMarkers = VisitMarkers(indexerDeclaration.Markers, p);
        if (!ReferenceEquals(newMarkers, indexerDeclaration.Markers)) changed = true;

        TypeTree newTypeExpr = indexerDeclaration.TypeExpression;
        var visitedType = Visit(indexerDeclaration.TypeExpression, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, indexerDeclaration.TypeExpression))
        {
            newTypeExpr = tt;
            changed = true;
        }

        var newInterfaceSpec = VisitRightPadded(indexerDeclaration.ExplicitInterfaceSpecifier, p);
        if (!ReferenceEquals(newInterfaceSpec, indexerDeclaration.ExplicitInterfaceSpecifier)) changed = true;

        Expression newIndexer = indexerDeclaration.Indexer;
        var visitedIndexer = Visit(indexerDeclaration.Indexer, p);
        if (visitedIndexer is Expression idx && !ReferenceEquals(idx, indexerDeclaration.Indexer))
        {
            newIndexer = idx;
            changed = true;
        }

        var newParams = VisitContainer(indexerDeclaration.Parameters, p);
        if (!ReferenceEquals(newParams, indexerDeclaration.Parameters)) changed = true;

        var newExprBody = VisitLeftPadded(indexerDeclaration.ExpressionBody, p);
        if (!ReferenceEquals(newExprBody, indexerDeclaration.ExpressionBody)) changed = true;

        Block? newAccessors = indexerDeclaration.Accessors;
        if (indexerDeclaration.Accessors != null)
        {
            var visitedAccessors = Visit(indexerDeclaration.Accessors, p);
            if (visitedAccessors is Block b && !ReferenceEquals(b, indexerDeclaration.Accessors))
            {
                newAccessors = b;
                changed = true;
            }
        }

        return changed
            ? indexerDeclaration.WithPrefix(newPrefix).WithMarkers(newMarkers)
                               .WithTypeExpression(newTypeExpr).WithExplicitInterfaceSpecifier(newInterfaceSpec)
                               .WithIndexer(newIndexer).WithParameters(newParams ?? indexerDeclaration.Parameters)
                               .WithExpressionBody(newExprBody).WithAccessors(newAccessors)
            : indexerDeclaration;
    }

    public virtual J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, P p)
    {
        delegateDeclaration = (DelegateDeclaration)VisitStatement(delegateDeclaration, p);
        var changed = false;

        var newPrefix = VisitSpace(delegateDeclaration.Prefix, p);
        if (!ReferenceEquals(newPrefix, delegateDeclaration.Prefix)) changed = true;

        var newMarkers = VisitMarkers(delegateDeclaration.Markers, p);
        if (!ReferenceEquals(newMarkers, delegateDeclaration.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in delegateDeclaration.Attributes)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        var newReturnType = VisitLeftPadded(delegateDeclaration.ReturnType, p)!;
        if (!ReferenceEquals(newReturnType, delegateDeclaration.ReturnType)) changed = true;

        Identifier newName = delegateDeclaration.IdentifierName;
        var visitedName = Visit(delegateDeclaration.IdentifierName, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, delegateDeclaration.IdentifierName))
        {
            newName = id;
            changed = true;
        }

        var newTypeParams = VisitContainer(delegateDeclaration.TypeParameters, p);
        if (!ReferenceEquals(newTypeParams, delegateDeclaration.TypeParameters)) changed = true;

        var newParams = VisitContainer(delegateDeclaration.Parameters, p);
        if (!ReferenceEquals(newParams, delegateDeclaration.Parameters)) changed = true;

        return changed
            ? delegateDeclaration.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                 .WithAttributes(attrChanged ? newAttrLists : delegateDeclaration.Attributes)
                                 .WithReturnType(newReturnType).WithIdentifierName(newName)
                                 .WithTypeParameters(newTypeParams).WithParameters(newParams ?? delegateDeclaration.Parameters)
            : delegateDeclaration;
    }

    public virtual J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
    {
        conversion = (ConversionOperatorDeclaration)VisitStatement(conversion, p);
        var changed = false;

        var newPrefix = VisitSpace(conversion.Prefix, p);
        if (!ReferenceEquals(newPrefix, conversion.Prefix)) changed = true;

        var newMarkers = VisitMarkers(conversion.Markers, p);
        if (!ReferenceEquals(newMarkers, conversion.Markers)) changed = true;

        var newInterfaceSpec = VisitRightPadded(conversion.InterfaceSpecifier, p);
        if (!ReferenceEquals(newInterfaceSpec, conversion.InterfaceSpecifier)) changed = true;

        var newReturnType = VisitLeftPadded(conversion.ReturnType, p)!;
        if (!ReferenceEquals(newReturnType, conversion.ReturnType)) changed = true;

        var newParams = VisitContainer(conversion.Parameters, p);
        if (!ReferenceEquals(newParams, conversion.Parameters)) changed = true;

        var newExprBody = VisitLeftPadded(conversion.ExpressionBody, p);
        if (!ReferenceEquals(newExprBody, conversion.ExpressionBody)) changed = true;

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
            ? conversion.WithPrefix(newPrefix).WithMarkers(newMarkers)
                        .WithInterfaceSpecifier(newInterfaceSpec)
                        .WithReturnType(newReturnType).WithParameters(newParams ?? conversion.Parameters)
                        .WithExpressionBody(newExprBody).WithBody(newBody)
            : conversion;
    }

    public virtual J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
    {
        operatorDeclaration = (OperatorDeclaration)VisitStatement(operatorDeclaration, p);
        var changed = false;

        var newPrefix = VisitSpace(operatorDeclaration.Prefix, p);
        if (!ReferenceEquals(newPrefix, operatorDeclaration.Prefix)) changed = true;

        var newMarkers = VisitMarkers(operatorDeclaration.Markers, p);
        if (!ReferenceEquals(newMarkers, operatorDeclaration.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in operatorDeclaration.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        var newInterfaceSpec = VisitRightPadded(operatorDeclaration.ExplicitInterfaceSpecifier, p);
        if (!ReferenceEquals(newInterfaceSpec, operatorDeclaration.ExplicitInterfaceSpecifier)) changed = true;

        Keyword newOpKw = operatorDeclaration.OperatorKeyword;
        var visitedOpKw = Visit(operatorDeclaration.OperatorKeyword, p);
        if (visitedOpKw is Keyword okw && !ReferenceEquals(okw, operatorDeclaration.OperatorKeyword))
        {
            newOpKw = okw;
            changed = true;
        }

        Keyword? newCheckedKw = operatorDeclaration.CheckedKeyword;
        if (operatorDeclaration.CheckedKeyword != null)
        {
            var visitedCkw = Visit(operatorDeclaration.CheckedKeyword, p);
            if (visitedCkw is Keyword ckw && !ReferenceEquals(ckw, operatorDeclaration.CheckedKeyword))
            {
                newCheckedKw = ckw;
                changed = true;
            }
        }

        TypeTree newReturnType = operatorDeclaration.ReturnType;
        var visitedReturn = Visit(operatorDeclaration.ReturnType, p);
        if (visitedReturn is TypeTree tt && !ReferenceEquals(tt, operatorDeclaration.ReturnType))
        {
            newReturnType = tt;
            changed = true;
        }

        var newParams = VisitContainer(operatorDeclaration.Parameters, p);
        if (!ReferenceEquals(newParams, operatorDeclaration.Parameters)) changed = true;

        Block newBody = operatorDeclaration.Body;
        var visitedBody = Visit(operatorDeclaration.Body, p);
        if (visitedBody is Block b && !ReferenceEquals(b, operatorDeclaration.Body))
        {
            newBody = b;
            changed = true;
        }

        JavaType.Method? newMethodType = (JavaType.Method?)VisitType(operatorDeclaration.MethodType, p);
        if (!ReferenceEquals(newMethodType, operatorDeclaration.MethodType)) changed = true;

        return changed
            ? operatorDeclaration.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                 .WithAttributeLists(attrChanged ? newAttrLists : operatorDeclaration.AttributeLists)
                                 .WithExplicitInterfaceSpecifier(newInterfaceSpec)
                                 .WithOperatorKeyword(newOpKw).WithCheckedKeyword(newCheckedKw)
                                 .WithReturnType(newReturnType).WithParameters(newParams ?? operatorDeclaration.Parameters)
                                 .WithBody(newBody).WithMethodType(newMethodType)
            : operatorDeclaration;
    }

    public virtual J VisitEnumDeclaration(EnumDeclaration enumDeclaration, P p)
    {
        enumDeclaration = (EnumDeclaration)VisitStatement(enumDeclaration, p);
        var changed = false;

        var newPrefix = VisitSpace(enumDeclaration.Prefix, p);
        if (!ReferenceEquals(newPrefix, enumDeclaration.Prefix)) changed = true;

        var newMarkers = VisitMarkers(enumDeclaration.Markers, p);
        if (!ReferenceEquals(newMarkers, enumDeclaration.Markers)) changed = true;

        if (enumDeclaration.AttributeLists != null)
        {
            var newAttrLists = new List<AttributeList>();
            bool attrChanged = false;
            foreach (var al in enumDeclaration.AttributeLists)
            {
                var visited = Visit(al, p);
                if (visited is AttributeList a)
                {
                    if (!ReferenceEquals(a, al)) attrChanged = true;
                    newAttrLists.Add(a);
                }
            }
            if (attrChanged)
            {
                enumDeclaration = enumDeclaration.WithAttributeLists(newAttrLists);
                changed = true;
            }
        }

        var newName = VisitLeftPadded(enumDeclaration.NamePadded, p)!;
        if (!ReferenceEquals(newName, enumDeclaration.NamePadded)) changed = true;

        var newBaseType = VisitLeftPadded(enumDeclaration.BaseType, p);
        if (!ReferenceEquals(newBaseType, enumDeclaration.BaseType)) changed = true;

        var newMembers = VisitContainer(enumDeclaration.Members, p);
        if (!ReferenceEquals(newMembers, enumDeclaration.Members)) changed = true;

        return changed
            ? enumDeclaration.WithPrefix(newPrefix).WithMarkers(newMarkers)
                             .WithNamePadded(newName).WithBaseType(newBaseType).WithMembers(newMembers)
            : enumDeclaration;
    }

    public virtual J VisitEnumMemberDeclaration(EnumMemberDeclaration enumMember, P p)
    {
        enumMember = (EnumMemberDeclaration)VisitExpression(enumMember, p);
        var changed = false;

        var newPrefix = VisitSpace(enumMember.Prefix, p);
        if (!ReferenceEquals(newPrefix, enumMember.Prefix)) changed = true;

        var newMarkers = VisitMarkers(enumMember.Markers, p);
        if (!ReferenceEquals(newMarkers, enumMember.Markers)) changed = true;

        var newAttrLists = new List<AttributeList>();
        bool attrChanged = false;
        foreach (var al in enumMember.AttributeLists)
        {
            var visited = Visit(al, p);
            if (visited is AttributeList a)
            {
                if (!ReferenceEquals(a, al)) attrChanged = true;
                newAttrLists.Add(a);
            }
        }
        if (attrChanged) changed = true;

        Identifier newName = enumMember.Name;
        var visitedName = Visit(enumMember.Name, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, enumMember.Name))
        {
            newName = id;
            changed = true;
        }

        var newInit = VisitLeftPadded(enumMember.Initializer, p);
        if (!ReferenceEquals(newInit, enumMember.Initializer)) changed = true;

        return changed
            ? enumMember.WithPrefix(newPrefix).WithMarkers(newMarkers)
                        .WithAttributeLists(attrChanged ? newAttrLists : enumMember.AttributeLists)
                        .WithName(newName).WithInitializer(newInit)
            : enumMember;
    }

    public virtual J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, P p)
    {
        aliasQualifiedName = (AliasQualifiedName)VisitExpression(aliasQualifiedName, p);
        var changed = false;

        var newPrefix = VisitSpace(aliasQualifiedName.Prefix, p);
        if (!ReferenceEquals(newPrefix, aliasQualifiedName.Prefix)) changed = true;

        var newMarkers = VisitMarkers(aliasQualifiedName.Markers, p);
        if (!ReferenceEquals(newMarkers, aliasQualifiedName.Markers)) changed = true;

        var newAlias = VisitRightPadded(aliasQualifiedName.Alias, p)!;
        if (!ReferenceEquals(newAlias, aliasQualifiedName.Alias)) changed = true;

        Expression newName = aliasQualifiedName.Name;
        var visited = Visit(aliasQualifiedName.Name, p);
        if (visited is Expression e && !ReferenceEquals(e, aliasQualifiedName.Name))
        {
            newName = e;
            changed = true;
        }

        return changed
            ? aliasQualifiedName.WithPrefix(newPrefix).WithMarkers(newMarkers).WithAlias(newAlias).WithName(newName)
            : aliasQualifiedName;
    }

    public virtual J VisitPointerDereference(PointerDereference pointerDereference, P p)
    {
        pointerDereference = (PointerDereference)VisitExpression(pointerDereference, p);
        var changed = false;

        var newPrefix = VisitSpace(pointerDereference.Prefix, p);
        if (!ReferenceEquals(newPrefix, pointerDereference.Prefix)) changed = true;

        var newMarkers = VisitMarkers(pointerDereference.Markers, p);
        if (!ReferenceEquals(newMarkers, pointerDereference.Markers)) changed = true;

        Expression newExpr = pointerDereference.Expression;
        var expr = Visit(pointerDereference.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, pointerDereference.Expression))
        {
            newExpr = e;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(pointerDereference.Type, p);
        if (!ReferenceEquals(newType, pointerDereference.Type)) changed = true;

        return changed
            ? pointerDereference.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr).WithType(newType)
            : pointerDereference;
    }

    public virtual J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, P p)
    {
        pointerFieldAccess = (PointerFieldAccess)VisitStatement(pointerFieldAccess, p);
        pointerFieldAccess = (PointerFieldAccess)VisitExpression(pointerFieldAccess, p);
        var changed = false;

        var newPrefix = VisitSpace(pointerFieldAccess.Prefix, p);
        if (!ReferenceEquals(newPrefix, pointerFieldAccess.Prefix)) changed = true;

        var newMarkers = VisitMarkers(pointerFieldAccess.Markers, p);
        if (!ReferenceEquals(newMarkers, pointerFieldAccess.Markers)) changed = true;

        Expression newTarget = pointerFieldAccess.Target;
        var visitedTarget = Visit(pointerFieldAccess.Target, p);
        if (visitedTarget is Expression t && !ReferenceEquals(t, pointerFieldAccess.Target))
        {
            newTarget = t;
            changed = true;
        }

        var newName = VisitLeftPadded(pointerFieldAccess.NamePadded, p)!;
        if (!ReferenceEquals(newName, pointerFieldAccess.NamePadded)) changed = true;

        JavaType? newType = (JavaType?)VisitType(pointerFieldAccess.Type, p);
        if (!ReferenceEquals(newType, pointerFieldAccess.Type)) changed = true;

        return changed
            ? pointerFieldAccess.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                .WithTarget(newTarget).WithNamePadded(newName).WithType(newType)
            : pointerFieldAccess;
    }

    public virtual J VisitRefType(RefType refType, P p)
    {
        refType = (RefType)VisitExpression(refType, p);
        var changed = false;

        var newPrefix = VisitSpace(refType.Prefix, p);
        if (!ReferenceEquals(newPrefix, refType.Prefix)) changed = true;

        var newMarkers = VisitMarkers(refType.Markers, p);
        if (!ReferenceEquals(newMarkers, refType.Markers)) changed = true;

        TypeTree newTypeId = refType.TypeIdentifier;
        var visited = Visit(refType.TypeIdentifier, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, refType.TypeIdentifier))
        {
            newTypeId = tt;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(refType.Type, p);
        if (!ReferenceEquals(newType, refType.Type)) changed = true;

        return changed
            ? refType.WithPrefix(newPrefix).WithMarkers(newMarkers).WithTypeIdentifier(newTypeId).WithType(newType)
            : refType;
    }

    // ---- LINQ ----

    public virtual J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        queryExpression = (QueryExpression)VisitExpression(queryExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(queryExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, queryExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(queryExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, queryExpression.Markers)) changed = true;

        FromClause newFrom = queryExpression.FromClause;
        var visitedFrom = Visit(queryExpression.FromClause, p);
        if (visitedFrom is FromClause fc && !ReferenceEquals(fc, queryExpression.FromClause))
        {
            newFrom = fc;
            changed = true;
        }

        QueryBody newBody = queryExpression.Body;
        var visitedBody = Visit(queryExpression.Body, p);
        if (visitedBody is QueryBody qb && !ReferenceEquals(qb, queryExpression.Body))
        {
            newBody = qb;
            changed = true;
        }

        return changed
            ? queryExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithFromClause(newFrom).WithBody(newBody)
            : queryExpression;
    }

    public virtual J VisitQueryBody(QueryBody queryBody, P p)
    {
        bool changed = false;

        var newPrefix = VisitSpace(queryBody.Prefix, p);
        if (!ReferenceEquals(newPrefix, queryBody.Prefix)) changed = true;

        var newMarkers = VisitMarkers(queryBody.Markers, p);
        if (!ReferenceEquals(newMarkers, queryBody.Markers)) changed = true;

        var clauses = new List<QueryClause>();
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
        return changed ? queryBody.WithPrefix(newPrefix).WithMarkers(newMarkers).WithClauses(clauses).WithSelectOrGroup(selectOrGroup).WithContinuation(continuation) : queryBody;
    }

    public virtual J VisitFromClause(FromClause fromClause, P p)
    {
        fromClause = (FromClause)VisitExpression(fromClause, p);
        var changed = false;

        var newPrefix = VisitSpace(fromClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, fromClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(fromClause.Markers, p);
        if (!ReferenceEquals(newMarkers, fromClause.Markers)) changed = true;

        TypeTree? newTypeId = fromClause.TypeIdentifier;
        if (fromClause.TypeIdentifier != null)
        {
            var visitedType = Visit(fromClause.TypeIdentifier, p);
            if (visitedType is TypeTree tt && !ReferenceEquals(tt, fromClause.TypeIdentifier))
            {
                newTypeId = tt;
                changed = true;
            }
        }

        var newId = VisitRightPadded(fromClause.IdentifierPadded, p)!;
        if (!ReferenceEquals(newId, fromClause.IdentifierPadded)) changed = true;

        Expression newExpr = fromClause.Expression;
        var expr = Visit(fromClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, fromClause.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? fromClause.WithPrefix(newPrefix).WithMarkers(newMarkers)
                        .WithTypeIdentifier(newTypeId).WithIdentifierPadded(newId).WithExpression(newExpr)
            : fromClause;
    }

    public virtual J VisitLetClause(LetClause letClause, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(letClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, letClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(letClause.Markers, p);
        if (!ReferenceEquals(newMarkers, letClause.Markers)) changed = true;

        var newId = VisitRightPadded(letClause.IdentifierPadded, p)!;
        if (!ReferenceEquals(newId, letClause.IdentifierPadded)) changed = true;

        Expression newExpr = letClause.Expression;
        var expr = Visit(letClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, letClause.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? letClause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithIdentifierPadded(newId).WithExpression(newExpr)
            : letClause;
    }

    public virtual J VisitJoinClause(JoinClause joinClause, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(joinClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, joinClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(joinClause.Markers, p);
        if (!ReferenceEquals(newMarkers, joinClause.Markers)) changed = true;

        var newId = VisitRightPadded(joinClause.IdentifierPadded, p)!;
        if (!ReferenceEquals(newId, joinClause.IdentifierPadded)) changed = true;

        var newIn = VisitRightPadded(joinClause.InExpression, p)!;
        if (!ReferenceEquals(newIn, joinClause.InExpression)) changed = true;

        var newLeft = VisitRightPadded(joinClause.LeftExpression, p)!;
        if (!ReferenceEquals(newLeft, joinClause.LeftExpression)) changed = true;

        Expression newRight = joinClause.RightExpression;
        var visitedRight = Visit(joinClause.RightExpression, p);
        if (visitedRight is Expression re && !ReferenceEquals(re, joinClause.RightExpression))
        {
            newRight = re;
            changed = true;
        }

        var newInto = VisitLeftPadded(joinClause.Into, p);
        if (!ReferenceEquals(newInto, joinClause.Into)) changed = true;

        return changed
            ? joinClause.WithPrefix(newPrefix).WithMarkers(newMarkers)
                        .WithIdentifierPadded(newId).WithInExpression(newIn)
                        .WithLeftExpression(newLeft).WithRightExpression(newRight).WithInto(newInto)
            : joinClause;
    }

    public virtual J VisitJoinIntoClause(JoinIntoClause joinIntoClause, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(joinIntoClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, joinIntoClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(joinIntoClause.Markers, p);
        if (!ReferenceEquals(newMarkers, joinIntoClause.Markers)) changed = true;

        Identifier newIdentifier = joinIntoClause.Identifier;
        var visited = Visit(joinIntoClause.Identifier, p);
        if (visited is Identifier id && !ReferenceEquals(id, joinIntoClause.Identifier))
        {
            newIdentifier = id;
            changed = true;
        }

        return changed
            ? joinIntoClause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithIdentifier(newIdentifier)
            : joinIntoClause;
    }

    public virtual J VisitWhereClause(WhereClause whereClause, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(whereClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, whereClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(whereClause.Markers, p);
        if (!ReferenceEquals(newMarkers, whereClause.Markers)) changed = true;

        Expression newCondition = whereClause.Condition;
        var condition = Visit(whereClause.Condition, p);
        if (condition is Expression e && !ReferenceEquals(e, whereClause.Condition))
        {
            newCondition = e;
            changed = true;
        }

        return changed
            ? whereClause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithCondition(newCondition)
            : whereClause;
    }

    public virtual J VisitOrderByClause(OrderByClause orderByClause, P p)
    {
        bool changed = false;

        var newPrefix = VisitSpace(orderByClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, orderByClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(orderByClause.Markers, p);
        if (!ReferenceEquals(newMarkers, orderByClause.Markers)) changed = true;

        var orderings = new List<JRightPadded<Ordering>>();
        bool orderingsChanged = false;
        foreach (var rp in orderByClause.Orderings)
        {
            var visited = VisitRightPadded(rp, p);
            if (visited != null)
            {
                if (!ReferenceEquals(visited, rp)) orderingsChanged = true;
                orderings.Add(visited);
            }
            else orderingsChanged = true;
        }
        if (orderingsChanged) changed = true;
        return changed ? orderByClause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithOrderings(orderingsChanged ? orderings : orderByClause.Orderings) : orderByClause;
    }

    public virtual J VisitOrdering(Ordering ordering, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(ordering.Prefix, p);
        if (!ReferenceEquals(newPrefix, ordering.Prefix)) changed = true;

        var newMarkers = VisitMarkers(ordering.Markers, p);
        if (!ReferenceEquals(newMarkers, ordering.Markers)) changed = true;

        var newExprPadded = VisitRightPadded(ordering.ExpressionPadded, p)!;
        if (!ReferenceEquals(newExprPadded, ordering.ExpressionPadded)) changed = true;

        return changed
            ? ordering.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpressionPadded(newExprPadded)
            : ordering;
    }

    public virtual J VisitSelectClause(SelectClause selectClause, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(selectClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, selectClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(selectClause.Markers, p);
        if (!ReferenceEquals(newMarkers, selectClause.Markers)) changed = true;

        Expression newExpr = selectClause.Expression;
        var expr = Visit(selectClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, selectClause.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? selectClause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr)
            : selectClause;
    }

    public virtual J VisitGroupClause(GroupClause groupClause, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(groupClause.Prefix, p);
        if (!ReferenceEquals(newPrefix, groupClause.Prefix)) changed = true;

        var newMarkers = VisitMarkers(groupClause.Markers, p);
        if (!ReferenceEquals(newMarkers, groupClause.Markers)) changed = true;

        var newGroupExpr = VisitRightPadded(groupClause.GroupExpression, p)!;
        if (!ReferenceEquals(newGroupExpr, groupClause.GroupExpression)) changed = true;

        Expression newKey = groupClause.Key;
        var visitedKey = Visit(groupClause.Key, p);
        if (visitedKey is Expression ke && !ReferenceEquals(ke, groupClause.Key))
        {
            newKey = ke;
            changed = true;
        }

        return changed
            ? groupClause.WithPrefix(newPrefix).WithMarkers(newMarkers).WithGroupExpression(newGroupExpr).WithKey(newKey)
            : groupClause;
    }

    public virtual J VisitQueryContinuation(QueryContinuation queryContinuation, P p)
    {
        var changed = false;

        var newPrefix = VisitSpace(queryContinuation.Prefix, p);
        if (!ReferenceEquals(newPrefix, queryContinuation.Prefix)) changed = true;

        var newMarkers = VisitMarkers(queryContinuation.Markers, p);
        if (!ReferenceEquals(newMarkers, queryContinuation.Markers)) changed = true;

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
            ? queryContinuation.WithPrefix(newPrefix).WithMarkers(newMarkers).WithIdentifier(newId).WithBody(newBody)
            : queryContinuation;
    }

    public virtual J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression anonymousObject, P p)
    {
        anonymousObject = (AnonymousObjectCreationExpression)VisitExpression(anonymousObject, p);
        var changed = false;

        var newPrefix = VisitSpace(anonymousObject.Prefix, p);
        if (!ReferenceEquals(newPrefix, anonymousObject.Prefix)) changed = true;

        var newMarkers = VisitMarkers(anonymousObject.Markers, p);
        if (!ReferenceEquals(newMarkers, anonymousObject.Markers)) changed = true;

        var newInits = VisitContainer(anonymousObject.Initializers, p);
        if (!ReferenceEquals(newInits, anonymousObject.Initializers)) changed = true;

        JavaType? newType = (JavaType?)VisitType(anonymousObject.Type, p);
        if (!ReferenceEquals(newType, anonymousObject.Type)) changed = true;

        return changed
            ? anonymousObject.WithPrefix(newPrefix).WithMarkers(newMarkers)
                             .WithInitializers(newInits ?? anonymousObject.Initializers)
                             .WithType(newType)
            : anonymousObject;
    }

    public virtual J VisitWithExpression(WithExpression withExpression, P p)
    {
        withExpression = (WithExpression)VisitExpression(withExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(withExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, withExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(withExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, withExpression.Markers)) changed = true;

        Expression newTarget = withExpression.Target;
        var visitedTarget = Visit(withExpression.Target, p);
        if (visitedTarget is Expression t && !ReferenceEquals(t, withExpression.Target))
        {
            newTarget = t;
            changed = true;
        }

        var newInit = VisitLeftPadded(withExpression.InitializerPadded, p)!;
        if (!ReferenceEquals(newInit, withExpression.InitializerPadded)) changed = true;

        JavaType? newType = (JavaType?)VisitType(withExpression.Type, p);
        if (!ReferenceEquals(newType, withExpression.Type)) changed = true;

        return changed
            ? withExpression.WithPrefix(newPrefix).WithMarkers(newMarkers)
                            .WithTarget(newTarget).WithInitializerPadded(newInit).WithType(newType)
            : withExpression;
    }

    public virtual J VisitSpreadExpression(SpreadExpression spreadExpression, P p)
    {
        spreadExpression = (SpreadExpression)VisitExpression(spreadExpression, p);
        var changed = false;

        var newPrefix = VisitSpace(spreadExpression.Prefix, p);
        if (!ReferenceEquals(newPrefix, spreadExpression.Prefix)) changed = true;

        var newMarkers = VisitMarkers(spreadExpression.Markers, p);
        if (!ReferenceEquals(newMarkers, spreadExpression.Markers)) changed = true;

        Expression newExpr = spreadExpression.Expression;
        var visited = Visit(spreadExpression.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, spreadExpression.Expression))
        {
            newExpr = e;
            changed = true;
        }

        JavaType? newType = (JavaType?)VisitType(spreadExpression.Type, p);
        if (!ReferenceEquals(newType, spreadExpression.Type)) changed = true;

        return changed
            ? spreadExpression.WithPrefix(newPrefix).WithMarkers(newMarkers).WithExpression(newExpr).WithType(newType)
            : spreadExpression;
    }

    public virtual J VisitFunctionPointerType(FunctionPointerType functionPointerType, P p)
    {
        functionPointerType = (FunctionPointerType)VisitExpression(functionPointerType, p);
        var changed = false;

        var newPrefix = VisitSpace(functionPointerType.Prefix, p);
        if (!ReferenceEquals(newPrefix, functionPointerType.Prefix)) changed = true;

        var newMarkers = VisitMarkers(functionPointerType.Markers, p);
        if (!ReferenceEquals(newMarkers, functionPointerType.Markers)) changed = true;

        var newConvTypes = VisitContainer(functionPointerType.UnmanagedCallingConventionTypes, p);
        if (!ReferenceEquals(newConvTypes, functionPointerType.UnmanagedCallingConventionTypes))
        {
            functionPointerType = functionPointerType.WithUnmanagedCallingConventionTypes(newConvTypes);
            changed = true;
        }

        var newParamTypes = VisitContainer(functionPointerType.ParameterTypes, p);
        if (!ReferenceEquals(newParamTypes, functionPointerType.ParameterTypes)) changed = true;

        JavaType? newType = (JavaType?)VisitType(functionPointerType.Type, p);
        if (!ReferenceEquals(newType, functionPointerType.Type)) changed = true;

        return changed
            ? functionPointerType.WithPrefix(newPrefix).WithMarkers(newMarkers)
                                 .WithParameterTypes(newParamTypes ?? functionPointerType.ParameterTypes)
                                 .WithType(newType)
            : functionPointerType;
    }

    public virtual J VisitTypeWithArguments(TypeWithArguments twa, P p)
    {
        twa = (TypeWithArguments)VisitExpression(twa, p);
        var changed = false;

        var newPrefix = VisitSpace(twa.Prefix, p);
        if (!ReferenceEquals(newPrefix, twa.Prefix)) changed = true;

        var newMarkers = VisitMarkers(twa.Markers, p);
        if (!ReferenceEquals(newMarkers, twa.Markers)) changed = true;

        var newType = (TypeTree?)Visit(twa.Type, p);
        if (newType != null && !ReferenceEquals(newType, twa.Type)) changed = true;

        var newArgs = VisitContainer(twa.Arguments, p);
        if (!ReferenceEquals(newArgs, twa.Arguments)) changed = true;

        return changed
            ? twa.WithPrefix(newPrefix).WithMarkers(newMarkers)
                 .WithType(newType ?? twa.Type)
                 .WithArguments(newArgs ?? twa.Arguments)
            : twa;
    }

    public virtual J VisitExplicitInterfaceMember(ExplicitInterfaceMember eim, P p)
    {
        eim = (ExplicitInterfaceMember)VisitStatement(eim, p);
        var changed = false;

        var newPrefix = VisitSpace(eim.Prefix, p);
        if (!ReferenceEquals(newPrefix, eim.Prefix)) changed = true;

        var newMarkers = VisitMarkers(eim.Markers, p);
        if (!ReferenceEquals(newMarkers, eim.Markers)) changed = true;

        var newInterfaceSpecifier = VisitRightPadded(eim.InterfaceSpecifier, p)!;
        if (!ReferenceEquals(newInterfaceSpecifier, eim.InterfaceSpecifier)) changed = true;

        var newMethodDecl = (MethodDeclaration?)Visit(eim.MethodDeclaration, p);
        if (newMethodDecl != null && !ReferenceEquals(newMethodDecl, eim.MethodDeclaration)) changed = true;

        return changed
            ? eim.WithPrefix(newPrefix).WithMarkers(newMarkers)
                 .WithInterfaceSpecifier(newInterfaceSpecifier)
                 .WithMethodDeclaration(newMethodDecl ?? eim.MethodDeclaration)
            : eim;
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
