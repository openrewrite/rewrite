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
        bool changed = false;

        var members = new List<JRightPadded<Statement>>();
        foreach (var memberPadded in compilationUnit.Members)
        {
            var visited = Visit(memberPadded.Element, p);
            if (visited is Statement stmt)
            {
                if (!ReferenceEquals(stmt, memberPadded.Element)) changed = true;
                members.Add(memberPadded.WithElement(stmt));
            }
        }

        if (changed)
        {
            compilationUnit = compilationUnit.WithMembers(members);
        }

        return compilationUnit;
    }

    public virtual J VisitUsingDirective(UsingDirective usingDirective, P p)
    {
        usingDirective = (UsingDirective)VisitStatement(usingDirective, p);
        var changed = false;

        JRightPadded<Identifier>? newAlias = usingDirective.Alias;
        if (usingDirective.Alias != null)
        {
            var visitedAlias = Visit(usingDirective.Alias.Element, p);
            if (visitedAlias is Identifier a && !ReferenceEquals(a, usingDirective.Alias.Element))
            {
                newAlias = usingDirective.Alias.WithElement(a);
                changed = true;
            }
        }

        TypeTree newNs = usingDirective.NamespaceOrType;
        var visitedNs = Visit(usingDirective.NamespaceOrType, p);
        if (visitedNs is TypeTree tt && !ReferenceEquals(tt, usingDirective.NamespaceOrType))
        {
            newNs = tt;
            changed = true;
        }

        return changed
            ? usingDirective.WithAlias(newAlias).WithNamespaceOrType(newNs)
            : usingDirective;
    }

    public virtual J VisitPropertyDeclaration(PropertyDeclaration prop, P p)
    {
        prop = (PropertyDeclaration)VisitStatement(prop, p);
        var changed = false;

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

        JRightPadded<TypeTree>? newInterfaceSpec = prop.InterfaceSpecifier;
        if (prop.InterfaceSpecifier != null)
        {
            var visitedIs = Visit(prop.InterfaceSpecifier.Element, p);
            if (visitedIs is TypeTree ist && !ReferenceEquals(ist, prop.InterfaceSpecifier.Element))
            {
                newInterfaceSpec = prop.InterfaceSpecifier.WithElement(ist);
                changed = true;
            }
        }

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

        JLeftPadded<Expression>? newInit = prop.Initializer;
        if (prop.Initializer != null)
        {
            var visitedInit = Visit(prop.Initializer.Element, p);
            if (visitedInit is Expression ie && !ReferenceEquals(ie, prop.Initializer.Element))
            {
                newInit = prop.Initializer.WithElement(ie);
                changed = true;
            }
        }

        return changed
            ? prop.WithAttributeLists(attrChanged ? newAttrLists : prop.AttributeLists)
                  .WithTypeExpression(newTypeExpr).WithInterfaceSpecifier(newInterfaceSpec)
                  .WithName(newName).WithAccessors(newAccessors)
                  .WithExpressionBody(newExprBody).WithInitializer(newInit)
            : prop;
    }

    public virtual J VisitAccessorDeclaration(AccessorDeclaration accessor, P p)
    {
        accessor = (AccessorDeclaration)VisitStatement(accessor, p);
        var changed = false;

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
            ? accessor.WithAttributeLists(attrChanged ? newAttrLists : accessor.AttributeLists)
                      .WithBody(newBody).WithExpressionBody(newExprBody)
            : accessor;
    }

    public virtual J VisitAttributeList(AttributeList attrList, P p)
    {
        attrList = (AttributeList)VisitStatement(attrList, p);
        bool changed = false;

        if (attrList.Target != null)
        {
            var visitedTarget = Visit(attrList.Target.Element, p);
            if (visitedTarget is Identifier tid && !ReferenceEquals(tid, attrList.Target.Element))
            {
                attrList = attrList.WithTarget(attrList.Target.WithElement(tid));
                changed = true;
            }
        }

        var newAttrs = new List<JRightPadded<Annotation>>();
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
        ne = (NamedExpression)VisitExpression(ne, p);
        var changed = false;

        JRightPadded<Identifier> newName = ne.Name;
        var visitedName = Visit(ne.Name.Element, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, ne.Name.Element))
        {
            newName = ne.Name.WithElement(id);
            changed = true;
        }

        Expression newExpr = ne.Expression;
        var visitedExpr = Visit(ne.Expression, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, ne.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? ne.WithName(newName).WithExpression(newExpr)
            : ne;
    }

    public virtual J VisitRefExpression(RefExpression re, P p)
    {
        re = (RefExpression)VisitExpression(re, p);
        var expr = Visit(re.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, re.Expression))
        {
            return re.WithExpression(e);
        }
        return re;
    }

    public virtual J VisitDeclarationExpression(DeclarationExpression de, P p)
    {
        de = (DeclarationExpression)VisitExpression(de, p);
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
        isPattern = (IsPattern)VisitExpression(isPattern, p);
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
        // Pattern extends Expression, NOT Statement despite the name
        se = (StatementExpression)VisitExpression(se, p);
        var stmt = Visit(se.Statement, p);
        if (stmt is Statement s && !ReferenceEquals(s, se.Statement))
        {
            return se.WithStatement(s);
        }
        return se;
    }

    public virtual J VisitSizeOf(SizeOf sizeOf, P p)
    {
        sizeOf = (SizeOf)VisitExpression(sizeOf, p);
        var changed = false;

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
            ? sizeOf.WithExpression(newExpr).WithType(newType)
            : sizeOf;
    }

    public virtual J VisitUnsafeStatement(UnsafeStatement unsafeStatement, P p)
    {
        unsafeStatement = (UnsafeStatement)VisitStatement(unsafeStatement, p);
        var visited = Visit(unsafeStatement.Block, p);
        if (visited is Block b && !ReferenceEquals(b, unsafeStatement.Block))
        {
            return unsafeStatement.WithBlock(b);
        }
        return unsafeStatement;
    }

    public virtual J VisitPointerType(PointerType pointerType, P p)
    {
        pointerType = (PointerType)VisitExpression(pointerType, p);
        var visited = Visit(pointerType.ElementType.Element, p);
        if (visited is TypeTree tt && !ReferenceEquals(tt, pointerType.ElementType.Element))
        {
            return pointerType.WithElementType(pointerType.ElementType.WithElement(tt));
        }
        return pointerType;
    }

    public virtual J VisitFixedStatement(FixedStatement fixedStatement, P p)
    {
        fixedStatement = (FixedStatement)VisitStatement(fixedStatement, p);
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
        externAlias = (ExternAlias)VisitStatement(externAlias, p);
        var visited = Visit(externAlias.Identifier.Element, p);
        if (visited is Identifier id && !ReferenceEquals(id, externAlias.Identifier.Element))
        {
            return externAlias.WithIdentifier(externAlias.Identifier.WithElement(id));
        }
        return externAlias;
    }

    public virtual J VisitInitializerExpression(InitializerExpression initializerExpression, P p)
    {
        initializerExpression = (InitializerExpression)VisitExpression(initializerExpression, p);
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
        nullSafeExpression = (NullSafeExpression)VisitExpression(nullSafeExpression, p);
        var visited = Visit(nullSafeExpression.Expression, p);
        if (visited is Expression e && !ReferenceEquals(e, nullSafeExpression.Expression))
        {
            return nullSafeExpression.WithExpressionPadded(nullSafeExpression.ExpressionPadded.WithElement(e));
        }
        return nullSafeExpression;
    }

    public virtual J VisitDefaultExpression(DefaultExpression defaultExpression, P p)
    {
        defaultExpression = (DefaultExpression)VisitExpression(defaultExpression, p);
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
        csLambda = (CsLambda)VisitStatement(csLambda, p);
        csLambda = (CsLambda)VisitExpression(csLambda, p);

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
            (lambda is Lambda l && !ReferenceEquals(l, csLambda.LambdaExpression)))
        {
            return csLambda.WithAttributeLists(attrChanged ? newAttrLists : csLambda.AttributeLists)
                           .WithReturnType(returnType).WithLambdaExpression(lambda is Lambda newLambda ? newLambda : csLambda.LambdaExpression);
        }

        return csLambda;
    }

    public virtual J VisitRelationalPattern(RelationalPattern rp, P p)
    {
        rp = (RelationalPattern)VisitExpression(rp, p);
        var value = Visit(rp.Value, p);
        if (value is Expression v && !ReferenceEquals(v, rp.Value))
        {
            return rp.WithValue(v);
        }
        return rp;
    }

    public virtual J VisitPropertyPattern(PropertyPattern pp, P p)
    {
        pp = (PropertyPattern)VisitExpression(pp, p);
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
            !ReferenceEquals(newDesignation, pp.Designation))
        {
            return pp.WithTypeQualifier(typeQualifier).WithSubpatterns(subpatterns).WithDesignation(newDesignation);
        }

        return pp;
    }

    public virtual J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, P p)
    {
        var changed = false;

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

        JLeftPadded<Identifier>? newWhereConstraint = ctp.WhereConstraint;
        if (ctp.WhereConstraint != null)
        {
            var visitedWc = Visit(ctp.WhereConstraint.Element, p);
            if (visitedWc is Identifier wid && !ReferenceEquals(wid, ctp.WhereConstraint.Element))
            {
                newWhereConstraint = ctp.WhereConstraint.WithElement(wid);
                changed = true;
            }
        }

        JContainer<Expression>? newConstraints = ctp.Constraints;
        if (ctp.Constraints != null)
        {
            var constraintElements = new List<JRightPadded<Expression>>();
            bool constraintsChanged = false;
            foreach (var ce in ctp.Constraints.Elements)
            {
                var visitedCe = Visit(ce.Element, p);
                if (visitedCe is Expression expr)
                {
                    if (!ReferenceEquals(expr, ce.Element)) constraintsChanged = true;
                    constraintElements.Add(ce.WithElement(expr));
                }
                else
                {
                    constraintElements.Add(ce);
                }
            }
            if (constraintsChanged)
            {
                newConstraints = ctp.Constraints.WithElements(constraintElements);
                changed = true;
            }
        }

        JavaType? newType = (JavaType?)VisitType(ctp.Type, p);
        if (!ReferenceEquals(newType, ctp.Type)) changed = true;

        return changed
            ? ctp.WithAttributeLists(attrChanged ? newAttrLists : ctp.AttributeLists)
                 .WithName(newName).WithWhereConstraint(newWhereConstraint)
                 .WithConstraints(newConstraints).WithType(newType)
            : ctp;
    }

    public virtual J VisitInterpolatedString(InterpolatedString istr, P p)
    {
        istr = (InterpolatedString)VisitStatement(istr, p);
        istr = (InterpolatedString)VisitExpression(istr, p);
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
        ae = (AwaitExpression)VisitStatement(ae, p);
        ae = (AwaitExpression)VisitExpression(ae, p);
        var changed = false;

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
            ? ae.WithExpression(newExpr).WithType(newType)
            : ae;
    }

    public virtual J VisitYield(Yield yield, P p)
    {
        yield = (Yield)VisitStatement(yield, p);
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
        ns = (NamespaceDeclaration)VisitStatement(ns, p);
        var name = Visit(ns.Name.Element, p);
        bool changed = false;

        var newMembers = new List<JRightPadded<Statement>>();
        foreach (var member in ns.Members)
        {
            var visited = Visit(member.Element, p);
            if (visited is Statement stmt)
            {
                if (!ReferenceEquals(stmt, member.Element)) changed = true;
                newMembers.Add(member.WithElement(stmt));
            }
        }

        if ((name is Expression e && !ReferenceEquals(e, ns.Name.Element)) || changed)
        {
            return ns
                .WithName(name is Expression newName ? ns.Name.WithElement(newName) : ns.Name)
                .WithMembers(changed ? newMembers : ns.Members);
        }

        return ns;
    }

    public virtual J VisitTupleType(TupleType tupleType, P p)
    {
        tupleType = (TupleType)VisitExpression(tupleType, p);
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

        JavaType? newType = (JavaType?)VisitType(tupleType.Type, p);
        if (!ReferenceEquals(newType, tupleType.Type)) elementsChanged = true;

        if (elementsChanged)
        {
            return tupleType.WithElements(new JContainer<TupleElement>(tupleType.Elements.Before, newElements, tupleType.Elements.Markers)).WithType(newType);
        }

        return tupleType;
    }

    public virtual J VisitTupleExpression(TupleExpression tupleExpr, P p)
    {
        tupleExpr = (TupleExpression)VisitExpression(tupleExpr, p);
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
        conditionalDirective = (ConditionalDirective)VisitStatement(conditionalDirective, p);
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
        pragmaWarningDirective = (PragmaWarningDirective)VisitStatement(pragmaWarningDirective, p);
        var newCodes = new List<JRightPadded<Expression>>();
        bool changed = false;
        foreach (var code in pragmaWarningDirective.WarningCodes)
        {
            var visited = Visit(code.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, code.Element)) changed = true;
                newCodes.Add(code.WithElement(e));
            }
            else
            {
                newCodes.Add(code);
            }
        }
        return changed
            ? pragmaWarningDirective.WithWarningCodes(newCodes)
            : pragmaWarningDirective;
    }

    public virtual J VisitPragmaChecksumDirective(PragmaChecksumDirective pragmaChecksumDirective, P p)
    {
        pragmaChecksumDirective = (PragmaChecksumDirective)VisitStatement(pragmaChecksumDirective, p);
        return pragmaChecksumDirective;
    }

    public virtual J VisitNullableDirective(NullableDirective nullableDirective, P p)
    {
        nullableDirective = (NullableDirective)VisitStatement(nullableDirective, p);
        return nullableDirective;
    }

    public virtual J VisitRegionDirective(RegionDirective regionDirective, P p)
    {
        regionDirective = (RegionDirective)VisitStatement(regionDirective, p);
        return regionDirective;
    }

    public virtual J VisitEndRegionDirective(EndRegionDirective endRegionDirective, P p)
    {
        endRegionDirective = (EndRegionDirective)VisitStatement(endRegionDirective, p);
        return endRegionDirective;
    }

    public virtual J VisitDefineDirective(DefineDirective defineDirective, P p)
    {
        defineDirective = (DefineDirective)VisitStatement(defineDirective, p);
        var symbol = (Identifier?)Visit(defineDirective.Symbol, p);
        return defineDirective.WithSymbol(symbol!);
    }

    public virtual J VisitUndefDirective(UndefDirective undefDirective, P p)
    {
        undefDirective = (UndefDirective)VisitStatement(undefDirective, p);
        var symbol = (Identifier?)Visit(undefDirective.Symbol, p);
        return undefDirective.WithSymbol(symbol!);
    }

    public virtual J VisitErrorDirective(ErrorDirective errorDirective, P p)
    {
        errorDirective = (ErrorDirective)VisitStatement(errorDirective, p);
        return errorDirective;
    }

    public virtual J VisitWarningDirective(WarningDirective warningDirective, P p)
    {
        warningDirective = (WarningDirective)VisitStatement(warningDirective, p);
        return warningDirective;
    }

    public virtual J VisitLineDirective(LineDirective lineDirective, P p)
    {
        lineDirective = (LineDirective)VisitStatement(lineDirective, p);
        var changed = false;

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
            ? lineDirective.WithLine(newLine).WithFile(newFile)
            : lineDirective;
    }

    // ---- New types ----

    public virtual J VisitKeyword(Keyword keyword, P p) => keyword;

    public virtual J VisitNameColon(NameColon nameColon, P p)
    {
        var visited = Visit(nameColon.Name.Element, p);
        if (visited is Identifier id && !ReferenceEquals(id, nameColon.Name.Element))
        {
            return nameColon.WithName(nameColon.Name.WithElement(id));
        }
        return nameColon;
    }

    public virtual J VisitAnnotatedStatement(AnnotatedStatement annotatedStatement, P p)
    {
        annotatedStatement = (AnnotatedStatement)VisitStatement(annotatedStatement, p);
        var changed = false;

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
            ? annotatedStatement.WithAttributeLists(attrChanged ? newAttrLists : annotatedStatement.AttributeLists)
                                .WithStatement(newStmt)
            : annotatedStatement;
    }

    public virtual J VisitArrayRankSpecifier(ArrayRankSpecifier arrayRankSpecifier, P p)
    {
        arrayRankSpecifier = (ArrayRankSpecifier)VisitExpression(arrayRankSpecifier, p);
        var newSizes = new List<JRightPadded<Expression>>();
        bool changed = false;
        foreach (var size in arrayRankSpecifier.Sizes.Elements)
        {
            var visited = Visit(size.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, size.Element)) changed = true;
                newSizes.Add(size.WithElement(e));
            }
            else
            {
                newSizes.Add(size);
            }
        }
        return changed
            ? arrayRankSpecifier.WithSizes(arrayRankSpecifier.Sizes.WithElements(newSizes))
            : arrayRankSpecifier;
    }

    public virtual J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        assignmentOperation = (AssignmentOperation)VisitStatement(assignmentOperation, p);
        assignmentOperation = (AssignmentOperation)VisitExpression(assignmentOperation, p);
        var changed = false;

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
            ? assignmentOperation.WithVariable(newVar).WithAssignmentValue(newValue).WithType(newType)
            : assignmentOperation;
    }

    public virtual J VisitStackAllocExpression(StackAllocExpression stackAllocExpression, P p)
    {
        stackAllocExpression = (StackAllocExpression)VisitExpression(stackAllocExpression, p);
        var visited = Visit(stackAllocExpression.Expression, p);
        if (visited is NewArray na && !ReferenceEquals(na, stackAllocExpression.Expression))
        {
            return stackAllocExpression.WithExpression(na);
        }
        return stackAllocExpression;
    }

    public virtual J VisitGotoStatement(GotoStatement gotoStatement, P p)
    {
        gotoStatement = (GotoStatement)VisitStatement(gotoStatement, p);
        var changed = false;

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
            ? gotoStatement.WithCaseOrDefaultKeyword(newKw).WithTarget(newTarget)
            : gotoStatement;
    }

    public virtual J VisitEventDeclaration(EventDeclaration eventDeclaration, P p)
    {
        eventDeclaration = (EventDeclaration)VisitStatement(eventDeclaration, p);
        var changed = false;

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

        JRightPadded<TypeTree>? newInterfaceSpec = eventDeclaration.InterfaceSpecifier;
        if (eventDeclaration.InterfaceSpecifier != null)
        {
            var visitedIs = Visit(eventDeclaration.InterfaceSpecifier.Element, p);
            if (visitedIs is TypeTree ist && !ReferenceEquals(ist, eventDeclaration.InterfaceSpecifier.Element))
            {
                newInterfaceSpec = eventDeclaration.InterfaceSpecifier.WithElement(ist);
                changed = true;
            }
        }

        JContainer<Statement>? newAccessors = eventDeclaration.Accessors;
        if (eventDeclaration.Accessors != null)
        {
            var accessorElements = new List<JRightPadded<Statement>>();
            bool accessorsChanged = false;
            foreach (var acc in eventDeclaration.Accessors.Elements)
            {
                var visitedAcc = Visit(acc.Element, p);
                if (visitedAcc is Statement s)
                {
                    if (!ReferenceEquals(s, acc.Element)) accessorsChanged = true;
                    accessorElements.Add(acc.WithElement(s));
                }
                else
                {
                    accessorElements.Add(acc);
                }
            }
            if (accessorsChanged)
            {
                newAccessors = eventDeclaration.Accessors.WithElements(accessorElements);
                changed = true;
            }
        }

        return changed
            ? eventDeclaration.WithAttributeLists(attrChanged ? newAttrLists : eventDeclaration.AttributeLists)
                              .WithTypeExpressionPadded(newTypeExpr).WithInterfaceSpecifier(newInterfaceSpec)
                              .WithName(newName).WithAccessors(newAccessors)
            : eventDeclaration;
    }

    public virtual J VisitCsBinary(CsBinary csBinary, P p)
    {
        csBinary = (CsBinary)VisitExpression(csBinary, p);
        var changed = false;

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
            ? csBinary.WithLeft(newLeft).WithRight(newRight).WithType(newType)
            : csBinary;
    }

    public virtual J VisitCollectionExpression(CollectionExpression collectionExpression, P p)
    {
        collectionExpression = (CollectionExpression)VisitExpression(collectionExpression, p);
        var newElements = new List<JRightPadded<Expression>>();
        bool changed = false;
        foreach (var elem in collectionExpression.Elements)
        {
            var visited = Visit(elem.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, elem.Element)) changed = true;
                newElements.Add(elem.WithElement(e));
            }
            else
            {
                newElements.Add(elem);
            }
        }

        JavaType? newType = (JavaType?)VisitType(collectionExpression.Type, p);
        if (!ReferenceEquals(newType, collectionExpression.Type)) changed = true;

        return changed
            ? collectionExpression.WithElements(newElements).WithType(newType)
            : collectionExpression;
    }

    public virtual J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, P p)
    {
        forEachVariableLoop = (ForEachVariableLoop)VisitStatement(forEachVariableLoop, p);
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
        usingStatement = (UsingStatement)VisitStatement(usingStatement, p);
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

    public virtual J VisitAllowsConstraintClause(AllowsConstraintClause clause, P p)
    {
        clause = (AllowsConstraintClause)VisitExpression(clause, p);
        var newExprs = new List<JRightPadded<Expression>>();
        bool changed = false;
        foreach (var expr in clause.Expressions.Elements)
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
            ? clause.WithExpressions(clause.Expressions.WithElements(newExprs))
            : clause;
    }

    public virtual J VisitRefStructConstraint(RefStructConstraint constraint, P p)
    {
        constraint = (RefStructConstraint)VisitExpression(constraint, p);
        return constraint;
    }

    public virtual J VisitClassOrStructConstraint(ClassOrStructConstraint constraint, P p)
    {
        constraint = (ClassOrStructConstraint)VisitExpression(constraint, p);
        return constraint;
    }

    public virtual J VisitConstructorConstraint(ConstructorConstraint constraint, P p)
    {
        constraint = (ConstructorConstraint)VisitExpression(constraint, p);
        return constraint;
    }

    public virtual J VisitDefaultConstraint(DefaultConstraint constraint, P p)
    {
        constraint = (DefaultConstraint)VisitExpression(constraint, p);
        return constraint;
    }

    public virtual J VisitSingleVariableDesignation(SingleVariableDesignation designation, P p)
    {
        designation = (SingleVariableDesignation)VisitExpression(designation, p);
        var visited = Visit(designation.Name, p);
        if (visited is Identifier id && !ReferenceEquals(id, designation.Name))
        {
            return designation.WithName(id);
        }
        return designation;
    }

    public virtual J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation designation, P p)
    {
        designation = (ParenthesizedVariableDesignation)VisitExpression(designation, p);
        var changed = false;

        var newVars = new List<JRightPadded<VariableDesignation>>();
        bool varsChanged = false;
        foreach (var v in designation.Variables.Elements)
        {
            var visited = Visit(v.Element, p);
            if (visited is VariableDesignation vd)
            {
                if (!ReferenceEquals(vd, v.Element)) varsChanged = true;
                newVars.Add(v.WithElement(vd));
            }
            else
            {
                newVars.Add(v);
            }
        }
        if (varsChanged) changed = true;

        JavaType? newType = (JavaType?)VisitType(designation.Type, p);
        if (!ReferenceEquals(newType, designation.Type)) changed = true;

        return changed
            ? designation.WithVariables(designation.Variables.WithElements(newVars)).WithType(newType)
            : designation;
    }

    public virtual J VisitDiscardVariableDesignation(DiscardVariableDesignation designation, P p)
    {
        designation = (DiscardVariableDesignation)VisitExpression(designation, p);
        var visited = Visit(designation.Discard, p);
        if (visited is Identifier id && !ReferenceEquals(id, designation.Discard))
        {
            return designation.WithDiscard(id);
        }
        return designation;
    }

    public virtual J VisitCsUnary(CsUnary csUnary, P p)
    {
        csUnary = (CsUnary)VisitStatement(csUnary, p);
        csUnary = (CsUnary)VisitExpression(csUnary, p);
        var changed = false;

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
            ? csUnary.WithExpression(newExpr).WithType(newType)
            : csUnary;
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
        constantPattern = (ConstantPattern)VisitExpression(constantPattern, p);
        var visited = Visit(constantPattern.Value, p);
        if (visited is Expression e && !ReferenceEquals(e, constantPattern.Value))
        {
            return constantPattern.WithValue(e);
        }
        return constantPattern;
    }

    public virtual J VisitDiscardPattern(DiscardPattern discardPattern, P p)
    {
        discardPattern = (DiscardPattern)VisitExpression(discardPattern, p);

        JavaType? newType = (JavaType?)VisitType(discardPattern.Type, p);
        if (!ReferenceEquals(newType, discardPattern.Type))
        {
            return discardPattern.WithType(newType);
        }
        return discardPattern;
    }

    public virtual J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, P p)
    {
        implicitElementAccess = (ImplicitElementAccess)VisitExpression(implicitElementAccess, p);
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

    public virtual J VisitSlicePattern(SlicePattern slicePattern, P p)
    {
        slicePattern = (SlicePattern)VisitExpression(slicePattern, p);
        return slicePattern;
    }

    public virtual J VisitListPattern(ListPattern listPattern, P p)
    {
        listPattern = (ListPattern)VisitExpression(listPattern, p);
        var changed = false;

        var newPatterns = new List<JRightPadded<Pattern>>();
        bool patternsChanged = false;
        foreach (var pat in listPattern.Patterns.Elements)
        {
            var visited = Visit(pat.Element, p);
            if (visited is Pattern v)
            {
                if (!ReferenceEquals(v, pat.Element)) patternsChanged = true;
                newPatterns.Add(pat.WithElement(v));
            }
            else
            {
                newPatterns.Add(pat);
            }
        }
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
            ? listPattern.WithPatterns(listPattern.Patterns.WithElements(newPatterns)).WithDesignation(newDesignation)
            : listPattern;
    }

    public virtual J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        switchExpression = (SwitchExpression)VisitExpression(switchExpression, p);
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

        JLeftPadded<Expression>? newWhen = arm.WhenExpression;
        if (arm.WhenExpression != null)
        {
            var visitedWhen = Visit(arm.WhenExpression.Element, p);
            if (visitedWhen is Expression we && !ReferenceEquals(we, arm.WhenExpression.Element))
            {
                newWhen = arm.WhenExpression.WithElement(we);
                changed = true;
            }
        }

        JLeftPadded<Expression> newExpr = arm.ExpressionPadded;
        var visitedExpr = Visit(arm.ExpressionPadded.Element, p);
        if (visitedExpr is Expression e && !ReferenceEquals(e, arm.ExpressionPadded.Element))
        {
            newExpr = arm.ExpressionPadded.WithElement(e);
            changed = true;
        }

        return changed
            ? arm.WithPattern(newPattern).WithWhenExpression(newWhen).WithExpressionPadded(newExpr)
            : arm;
    }

    public virtual J VisitCheckedExpression(CheckedExpression checkedExpression, P p)
    {
        checkedExpression = (CheckedExpression)VisitExpression(checkedExpression, p);
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
        checkedStatement = (CheckedStatement)VisitStatement(checkedStatement, p);
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
        rangeExpression = (RangeExpression)VisitExpression(rangeExpression, p);
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
        indexerDeclaration = (IndexerDeclaration)VisitStatement(indexerDeclaration, p);
        var changed = false;

        TypeTree newTypeExpr = indexerDeclaration.TypeExpression;
        var visitedType = Visit(indexerDeclaration.TypeExpression, p);
        if (visitedType is TypeTree tt && !ReferenceEquals(tt, indexerDeclaration.TypeExpression))
        {
            newTypeExpr = tt;
            changed = true;
        }

        JRightPadded<TypeTree>? newInterfaceSpec = indexerDeclaration.ExplicitInterfaceSpecifier;
        if (indexerDeclaration.ExplicitInterfaceSpecifier != null)
        {
            var visitedIs = Visit(indexerDeclaration.ExplicitInterfaceSpecifier.Element, p);
            if (visitedIs is TypeTree ist && !ReferenceEquals(ist, indexerDeclaration.ExplicitInterfaceSpecifier.Element))
            {
                newInterfaceSpec = indexerDeclaration.ExplicitInterfaceSpecifier.WithElement(ist);
                changed = true;
            }
        }

        Expression newIndexer = indexerDeclaration.Indexer;
        var visitedIndexer = Visit(indexerDeclaration.Indexer, p);
        if (visitedIndexer is Expression idx && !ReferenceEquals(idx, indexerDeclaration.Indexer))
        {
            newIndexer = idx;
            changed = true;
        }

        JContainer<Expression> newParams = indexerDeclaration.Parameters;
        var paramElements = new List<JRightPadded<Expression>>();
        bool paramsChanged = false;
        foreach (var param in indexerDeclaration.Parameters.Elements)
        {
            var visitedParam = Visit(param.Element, p);
            if (visitedParam is Expression pe)
            {
                if (!ReferenceEquals(pe, param.Element)) paramsChanged = true;
                paramElements.Add(param.WithElement(pe));
            }
            else
            {
                paramElements.Add(param);
            }
        }
        if (paramsChanged)
        {
            newParams = indexerDeclaration.Parameters.WithElements(paramElements);
            changed = true;
        }

        JLeftPadded<Expression>? newExprBody = indexerDeclaration.ExpressionBody;
        if (indexerDeclaration.ExpressionBody != null)
        {
            var visitedExpr = Visit(indexerDeclaration.ExpressionBody.Element, p);
            if (visitedExpr is Expression e && !ReferenceEquals(e, indexerDeclaration.ExpressionBody.Element))
            {
                newExprBody = indexerDeclaration.ExpressionBody.WithElement(e);
                changed = true;
            }
        }

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
            ? indexerDeclaration.WithTypeExpression(newTypeExpr).WithExplicitInterfaceSpecifier(newInterfaceSpec)
                               .WithIndexer(newIndexer).WithParameters(newParams)
                               .WithExpressionBody(newExprBody).WithAccessors(newAccessors)
            : indexerDeclaration;
    }

    public virtual J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, P p)
    {
        delegateDeclaration = (DelegateDeclaration)VisitStatement(delegateDeclaration, p);
        var changed = false;

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

        JContainer<TypeParameter>? newTypeParams = delegateDeclaration.TypeParameters;
        if (delegateDeclaration.TypeParameters != null)
        {
            var tpElements = new List<JRightPadded<TypeParameter>>();
            bool tpChanged = false;
            foreach (var tp in delegateDeclaration.TypeParameters.Elements)
            {
                var visitedTp = Visit(tp.Element, p);
                if (visitedTp is TypeParameter tpe)
                {
                    if (!ReferenceEquals(tpe, tp.Element)) tpChanged = true;
                    tpElements.Add(tp.WithElement(tpe));
                }
                else
                {
                    tpElements.Add(tp);
                }
            }
            if (tpChanged)
            {
                newTypeParams = delegateDeclaration.TypeParameters.WithElements(tpElements);
                changed = true;
            }
        }

        JContainer<Statement> newParams = delegateDeclaration.Parameters;
        var paramElements = new List<JRightPadded<Statement>>();
        bool paramsChanged = false;
        foreach (var param in delegateDeclaration.Parameters.Elements)
        {
            var visitedParam = Visit(param.Element, p);
            if (visitedParam is Statement s)
            {
                if (!ReferenceEquals(s, param.Element)) paramsChanged = true;
                paramElements.Add(param.WithElement(s));
            }
            else
            {
                paramElements.Add(param);
            }
        }
        if (paramsChanged)
        {
            newParams = delegateDeclaration.Parameters.WithElements(paramElements);
            changed = true;
        }

        return changed
            ? delegateDeclaration.WithAttributes(attrChanged ? newAttrLists : delegateDeclaration.Attributes)
                                 .WithReturnType(newReturnType).WithIdentifierName(newName)
                                 .WithTypeParameters(newTypeParams).WithParameters(newParams)
            : delegateDeclaration;
    }

    public virtual J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
    {
        conversion = (ConversionOperatorDeclaration)VisitStatement(conversion, p);
        var changed = false;

        JLeftPadded<TypeTree> newReturnType = conversion.ReturnType;
        var visitedReturn = Visit(conversion.ReturnType.Element, p);
        if (visitedReturn is TypeTree tt && !ReferenceEquals(tt, conversion.ReturnType.Element))
        {
            newReturnType = conversion.ReturnType.WithElement(tt);
            changed = true;
        }

        JContainer<Statement> newParams = conversion.Parameters;
        var paramElements = new List<JRightPadded<Statement>>();
        bool paramsChanged = false;
        foreach (var param in conversion.Parameters.Elements)
        {
            var visitedParam = Visit(param.Element, p);
            if (visitedParam is Statement s)
            {
                if (!ReferenceEquals(s, param.Element)) paramsChanged = true;
                paramElements.Add(param.WithElement(s));
            }
            else
            {
                paramElements.Add(param);
            }
        }
        if (paramsChanged)
        {
            newParams = conversion.Parameters.WithElements(paramElements);
            changed = true;
        }

        JLeftPadded<Expression>? newExprBody = conversion.ExpressionBody;
        if (conversion.ExpressionBody != null)
        {
            var visitedExpr = Visit(conversion.ExpressionBody.Element, p);
            if (visitedExpr is Expression e && !ReferenceEquals(e, conversion.ExpressionBody.Element))
            {
                newExprBody = conversion.ExpressionBody.WithElement(e);
                changed = true;
            }
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
            ? conversion.WithReturnType(newReturnType).WithParameters(newParams)
                        .WithExpressionBody(newExprBody).WithBody(newBody)
            : conversion;
    }

    public virtual J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
    {
        operatorDeclaration = (OperatorDeclaration)VisitStatement(operatorDeclaration, p);
        var changed = false;

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

        JRightPadded<TypeTree>? newInterfaceSpec = operatorDeclaration.ExplicitInterfaceSpecifier;
        if (operatorDeclaration.ExplicitInterfaceSpecifier != null)
        {
            var visitedIs = Visit(operatorDeclaration.ExplicitInterfaceSpecifier.Element, p);
            if (visitedIs is TypeTree ist && !ReferenceEquals(ist, operatorDeclaration.ExplicitInterfaceSpecifier.Element))
            {
                newInterfaceSpec = operatorDeclaration.ExplicitInterfaceSpecifier.WithElement(ist);
                changed = true;
            }
        }

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

        JContainer<Expression> newParams = operatorDeclaration.Parameters;
        var paramElements = new List<JRightPadded<Expression>>();
        bool paramsChanged = false;
        foreach (var param in operatorDeclaration.Parameters.Elements)
        {
            var visitedParam = Visit(param.Element, p);
            if (visitedParam is Expression pe)
            {
                if (!ReferenceEquals(pe, param.Element)) paramsChanged = true;
                paramElements.Add(param.WithElement(pe));
            }
            else
            {
                paramElements.Add(param);
            }
        }
        if (paramsChanged)
        {
            newParams = operatorDeclaration.Parameters.WithElements(paramElements);
            changed = true;
        }

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
            ? operatorDeclaration.WithAttributeLists(attrChanged ? newAttrLists : operatorDeclaration.AttributeLists)
                                 .WithExplicitInterfaceSpecifier(newInterfaceSpec)
                                 .WithOperatorKeyword(newOpKw).WithCheckedKeyword(newCheckedKw)
                                 .WithReturnType(newReturnType).WithParameters(newParams)
                                 .WithBody(newBody).WithMethodType(newMethodType)
            : operatorDeclaration;
    }

    public virtual J VisitEnumDeclaration(EnumDeclaration enumDeclaration, P p)
    {
        enumDeclaration = (EnumDeclaration)VisitStatement(enumDeclaration, p);
        var changed = false;

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

        JLeftPadded<Identifier> newName = enumDeclaration.NamePadded;
        var visitedName = Visit(enumDeclaration.NamePadded.Element, p);
        if (visitedName is Identifier id && !ReferenceEquals(id, enumDeclaration.NamePadded.Element))
        {
            newName = enumDeclaration.NamePadded.WithElement(id);
            changed = true;
        }

        JLeftPadded<TypeTree>? newBaseType = enumDeclaration.BaseType;
        if (enumDeclaration.BaseType != null)
        {
            var visitedBase = Visit(enumDeclaration.BaseType.Element, p);
            if (visitedBase is TypeTree bt && !ReferenceEquals(bt, enumDeclaration.BaseType.Element))
            {
                newBaseType = enumDeclaration.BaseType.WithElement(bt);
                changed = true;
            }
        }

        JContainer<Expression>? newMembers = enumDeclaration.Members;
        if (enumDeclaration.Members != null)
        {
            var memberElements = new List<JRightPadded<Expression>>();
            bool membersChanged = false;
            foreach (var m in enumDeclaration.Members.Elements)
            {
                var visitedM = Visit(m.Element, p);
                if (visitedM is Expression e)
                {
                    if (!ReferenceEquals(e, m.Element)) membersChanged = true;
                    memberElements.Add(m.WithElement(e));
                }
                else
                {
                    memberElements.Add(m);
                }
            }
            if (membersChanged)
            {
                newMembers = enumDeclaration.Members.WithElements(memberElements);
                changed = true;
            }
        }

        return changed
            ? enumDeclaration.WithNamePadded(newName).WithBaseType(newBaseType).WithMembers(newMembers)
            : enumDeclaration;
    }

    public virtual J VisitEnumMemberDeclaration(EnumMemberDeclaration enumMember, P p)
    {
        enumMember = (EnumMemberDeclaration)VisitExpression(enumMember, p);
        var changed = false;

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

        JLeftPadded<Expression>? newInit = enumMember.Initializer;
        if (enumMember.Initializer != null)
        {
            var visitedInit = Visit(enumMember.Initializer.Element, p);
            if (visitedInit is Expression ie && !ReferenceEquals(ie, enumMember.Initializer.Element))
            {
                newInit = enumMember.Initializer.WithElement(ie);
                changed = true;
            }
        }

        return changed
            ? enumMember.WithAttributeLists(attrChanged ? newAttrLists : enumMember.AttributeLists)
                        .WithName(newName).WithInitializer(newInit)
            : enumMember;
    }

    public virtual J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, P p)
    {
        aliasQualifiedName = (AliasQualifiedName)VisitExpression(aliasQualifiedName, p);
        var changed = false;

        JRightPadded<Identifier> newAlias = aliasQualifiedName.Alias;
        var visitedAlias = Visit(aliasQualifiedName.Alias.Element, p);
        if (visitedAlias is Identifier aid && !ReferenceEquals(aid, aliasQualifiedName.Alias.Element))
        {
            newAlias = aliasQualifiedName.Alias.WithElement(aid);
            changed = true;
        }

        Expression newName = aliasQualifiedName.Name;
        var visited = Visit(aliasQualifiedName.Name, p);
        if (visited is Expression e && !ReferenceEquals(e, aliasQualifiedName.Name))
        {
            newName = e;
            changed = true;
        }

        return changed
            ? aliasQualifiedName.WithAlias(newAlias).WithName(newName)
            : aliasQualifiedName;
    }

    public virtual J VisitPointerDereference(PointerDereference pointerDereference, P p)
    {
        pointerDereference = (PointerDereference)VisitExpression(pointerDereference, p);
        var changed = false;

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
            ? pointerDereference.WithExpression(newExpr).WithType(newType)
            : pointerDereference;
    }

    public virtual J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, P p)
    {
        pointerFieldAccess = (PointerFieldAccess)VisitStatement(pointerFieldAccess, p);
        pointerFieldAccess = (PointerFieldAccess)VisitExpression(pointerFieldAccess, p);
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

        JavaType? newType = (JavaType?)VisitType(pointerFieldAccess.Type, p);
        if (!ReferenceEquals(newType, pointerFieldAccess.Type)) changed = true;

        return changed
            ? pointerFieldAccess.WithTarget(newTarget).WithNamePadded(newName).WithType(newType)
            : pointerFieldAccess;
    }

    public virtual J VisitRefType(RefType refType, P p)
    {
        refType = (RefType)VisitExpression(refType, p);
        var changed = false;

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
            ? refType.WithTypeIdentifier(newTypeId).WithType(newType)
            : refType;
    }

    // ---- LINQ ----

    public virtual J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        queryExpression = (QueryExpression)VisitExpression(queryExpression, p);
        var changed = false;

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
            ? queryExpression.WithFromClause(newFrom).WithBody(newBody)
            : queryExpression;
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
        fromClause = (FromClause)VisitExpression(fromClause, p);
        var changed = false;

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

        JRightPadded<Identifier> newId = fromClause.IdentifierPadded;
        var visitedId = Visit(fromClause.IdentifierPadded.Element, p);
        if (visitedId is Identifier id && !ReferenceEquals(id, fromClause.IdentifierPadded.Element))
        {
            newId = fromClause.IdentifierPadded.WithElement(id);
            changed = true;
        }

        Expression newExpr = fromClause.Expression;
        var expr = Visit(fromClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, fromClause.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? fromClause.WithTypeIdentifier(newTypeId).WithIdentifierPadded(newId).WithExpression(newExpr)
            : fromClause;
    }

    public virtual J VisitLetClause(LetClause letClause, P p)
    {
        var changed = false;

        JRightPadded<Identifier> newId = letClause.IdentifierPadded;
        var visitedId = Visit(letClause.IdentifierPadded.Element, p);
        if (visitedId is Identifier id && !ReferenceEquals(id, letClause.IdentifierPadded.Element))
        {
            newId = letClause.IdentifierPadded.WithElement(id);
            changed = true;
        }

        Expression newExpr = letClause.Expression;
        var expr = Visit(letClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, letClause.Expression))
        {
            newExpr = e;
            changed = true;
        }

        return changed
            ? letClause.WithIdentifierPadded(newId).WithExpression(newExpr)
            : letClause;
    }

    public virtual J VisitJoinClause(JoinClause joinClause, P p)
    {
        var changed = false;

        JRightPadded<Identifier> newId = joinClause.IdentifierPadded;
        var visitedId = Visit(joinClause.IdentifierPadded.Element, p);
        if (visitedId is Identifier id && !ReferenceEquals(id, joinClause.IdentifierPadded.Element))
        {
            newId = joinClause.IdentifierPadded.WithElement(id);
            changed = true;
        }

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

        JLeftPadded<JoinIntoClause>? newInto = joinClause.Into;
        if (joinClause.Into != null)
        {
            var visitedInto = Visit(joinClause.Into.Element, p);
            if (visitedInto is JoinIntoClause jic && !ReferenceEquals(jic, joinClause.Into.Element))
            {
                newInto = joinClause.Into.WithElement(jic);
                changed = true;
            }
        }

        return changed
            ? joinClause.WithIdentifierPadded(newId).WithInExpression(newIn)
                        .WithLeftExpression(newLeft).WithRightExpression(newRight).WithInto(newInto)
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
        anonymousObject = (AnonymousObjectCreationExpression)VisitExpression(anonymousObject, p);
        var changed = false;

        var initElements = new List<JRightPadded<Expression>>();
        bool initsChanged = false;
        foreach (var init in anonymousObject.Initializers.Elements)
        {
            var visited = Visit(init.Element, p);
            if (visited is Expression e)
            {
                if (!ReferenceEquals(e, init.Element)) initsChanged = true;
                initElements.Add(init.WithElement(e));
            }
            else
            {
                initElements.Add(init);
            }
        }
        if (initsChanged) changed = true;

        JavaType? newType = (JavaType?)VisitType(anonymousObject.Type, p);
        if (!ReferenceEquals(newType, anonymousObject.Type)) changed = true;

        return changed
            ? anonymousObject.WithInitializers(initsChanged ? anonymousObject.Initializers.WithElements(initElements) : anonymousObject.Initializers)
                             .WithType(newType)
            : anonymousObject;
    }

    public virtual J VisitWithExpression(WithExpression withExpression, P p)
    {
        withExpression = (WithExpression)VisitExpression(withExpression, p);
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

        JavaType? newType = (JavaType?)VisitType(withExpression.Type, p);
        if (!ReferenceEquals(newType, withExpression.Type)) changed = true;

        return changed
            ? withExpression.WithTarget(newTarget).WithInitializerPadded(newInit).WithType(newType)
            : withExpression;
    }

    public virtual J VisitSpreadExpression(SpreadExpression spreadExpression, P p)
    {
        spreadExpression = (SpreadExpression)VisitExpression(spreadExpression, p);
        var changed = false;

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
            ? spreadExpression.WithExpression(newExpr).WithType(newType)
            : spreadExpression;
    }

    public virtual J VisitFunctionPointerType(FunctionPointerType functionPointerType, P p)
    {
        functionPointerType = (FunctionPointerType)VisitExpression(functionPointerType, p);
        var changed = false;

        if (functionPointerType.UnmanagedCallingConventionTypes != null)
        {
            var convElements = new List<JRightPadded<Identifier>>();
            bool convChanged = false;
            foreach (var ct in functionPointerType.UnmanagedCallingConventionTypes.Elements)
            {
                var visited = Visit(ct.Element, p);
                if (visited is Identifier id)
                {
                    if (!ReferenceEquals(id, ct.Element)) convChanged = true;
                    convElements.Add(ct.WithElement(id));
                }
                else
                {
                    convElements.Add(ct);
                }
            }
            if (convChanged)
            {
                functionPointerType = functionPointerType.WithUnmanagedCallingConventionTypes(
                    functionPointerType.UnmanagedCallingConventionTypes.WithElements(convElements));
                changed = true;
            }
        }

        var paramTypeElements = new List<JRightPadded<TypeTree>>();
        bool paramTypesChanged = false;
        foreach (var pt in functionPointerType.ParameterTypes.Elements)
        {
            var visited = Visit(pt.Element, p);
            if (visited is TypeTree tt)
            {
                if (!ReferenceEquals(tt, pt.Element)) paramTypesChanged = true;
                paramTypeElements.Add(pt.WithElement(tt));
            }
            else
            {
                paramTypeElements.Add(pt);
            }
        }
        if (paramTypesChanged) changed = true;

        JavaType? newType = (JavaType?)VisitType(functionPointerType.Type, p);
        if (!ReferenceEquals(newType, functionPointerType.Type)) changed = true;

        return changed
            ? functionPointerType.WithParameterTypes(paramTypesChanged ? functionPointerType.ParameterTypes.WithElements(paramTypeElements) : functionPointerType.ParameterTypes)
                                 .WithType(newType)
            : functionPointerType;
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
