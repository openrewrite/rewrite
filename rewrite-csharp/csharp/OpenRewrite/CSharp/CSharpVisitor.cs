using Rewrite.Core;
using Rewrite.Java;

namespace Rewrite.CSharp;

/// <summary>
/// Visitor for C# LST elements. Extends JavaVisitor to handle J types,
/// adding dispatch for C#-specific (Cs) types.
/// </summary>
public class CSharpVisitor<P> : JavaVisitor<P>
{
    protected override J? Accept(J tree, P p)
    {
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
            NullSafeExpression nse => VisitNullSafeExpression(nse, p),
            // New types
            Keyword kw => VisitKeyword(kw, p),
            CsArgument csa => VisitCsArgument(csa, p),
            NameColon nc => VisitNameColon(nc, p),
            AnnotatedStatement ans => VisitAnnotatedStatement(ans, p),
            ArrayRankSpecifier ars => VisitArrayRankSpecifier(ars, p),
            AssignmentOperation ao => VisitAssignmentOperation(ao, p),
            StackAllocExpression sae => VisitStackAllocExpression(sae, p),
            GotoStatement gs => VisitGotoStatement(gs, p),
            EventDeclaration evd => VisitEventDeclaration(evd, p),
            CsBinary csb => VisitCsBinary(csb, p),
            CollectionExpression ce => VisitCollectionExpression(ce, p),
            CsExpressionStatement ces => VisitCsExpressionStatement(ces, p),
            ForEachVariableLoop fevl => VisitForEachVariableLoop(fevl, p),
            ForEachVariableLoopControl fevlc => VisitForEachVariableLoopControl(fevlc, p),
            CsMethodDeclaration cmd => VisitCsMethodDeclaration(cmd, p),
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
            CsNewClass csnc => VisitCsNewClass(csnc, p),
            ImplicitElementAccess iea => VisitImplicitElementAccess(iea, p),
            UnaryPattern up => VisitUnaryPattern(up, p),
            TypePattern tp => VisitTypePattern(tp, p),
            BinaryPattern bp => VisitBinaryPattern(bp, p),
            ConstantPattern cp => VisitConstantPattern(cp, p),
            DiscardPattern dp => VisitDiscardPattern(dp, p),
            ListPattern lp => VisitListPattern(lp, p),
            ParenthesizedPattern pap => VisitParenthesizedPattern(pap, p),
            RecursivePattern rcp => VisitRecursivePattern(rcp, p),
            VarPattern vp => VisitVarPattern(vp, p),
            PositionalPatternClause ppc => VisitPositionalPatternClause(ppc, p),
            SlicePattern sp => VisitSlicePattern(sp, p),
            PropertyPatternClause ppclause => VisitPropertyPatternClause(ppclause, p),
            Subpattern sub => VisitSubpattern(sub, p),
            SwitchExpression swe => VisitSwitchExpression(swe, p),
            SwitchExpressionArm swea => VisitSwitchExpressionArm(swea, p),
            SwitchSection ss => VisitSwitchSection(ss, p),
            DefaultSwitchLabel dsl => VisitDefaultSwitchLabel(dsl, p),
            CasePatternSwitchLabel cpsl => VisitCasePatternSwitchLabel(cpsl, p),
            SwitchStatement sws => VisitSwitchStatement(sws, p),
            LockStatement ls => VisitLockStatement(ls, p),
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
            CsArrayType csat => VisitCsArrayType(csat, p),
            CsTryCatch cstc => VisitCsTryCatch(cstc, p),
            CsTry cstry => VisitCsTry(cstry, p),
            PointerFieldAccess pfa => VisitPointerFieldAccess(pfa, p),
            RefType rt => VisitRefType(rt, p),
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

        return changed ? compilationUnit with { Members = members } : compilationUnit;
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

    public virtual J VisitIsPattern(IsPattern isPattern, P p)
    {
        var expr = Visit(isPattern.Expression, p);
        var pattern = Visit(isPattern.Pattern.Element, p);

        if ((expr is Expression e && !ReferenceEquals(e, isPattern.Expression)) ||
            (pattern is J pat && !ReferenceEquals(pat, isPattern.Pattern.Element)))
        {
            return isPattern with
            {
                Expression = expr is Expression newExpr ? newExpr : isPattern.Expression,
                Pattern = pattern is J newPat
                    ? isPattern.Pattern with { Element = newPat }
                    : isPattern.Pattern
            };
        }

        return isPattern;
    }

    public virtual J VisitStatementExpression(StatementExpression se, P p)
    {
        var stmt = Visit(se.Statement, p);
        if (stmt is Statement s && !ReferenceEquals(s, se.Statement))
        {
            return se with { Statement = s };
        }
        return se;
    }

    public virtual J VisitSizeOf(SizeOf sizeOf, P p)
    {
        Visit(sizeOf.Expression, p);
        return sizeOf;
    }

    public virtual J VisitUnsafeStatement(UnsafeStatement unsafeStatement, P p)
    {
        Visit(unsafeStatement.Block, p);
        return unsafeStatement;
    }

    public virtual J VisitPointerType(PointerType pointerType, P p)
    {
        Visit(pointerType.ElementType.Element, p);
        return pointerType;
    }

    public virtual J VisitFixedStatement(FixedStatement fixedStatement, P p)
    {
        Visit(fixedStatement.Declarations, p);
        Visit(fixedStatement.Block, p);
        return fixedStatement;
    }

    public virtual J VisitExternAlias(ExternAlias externAlias, P p)
    {
        Visit(externAlias.Identifier.Element, p);
        return externAlias;
    }

    public virtual J VisitInitializerExpression(InitializerExpression initializerExpression, P p)
    {
        foreach (var expr in initializerExpression.Expressions.Elements)
        {
            Visit(expr.Element, p);
        }
        return initializerExpression;
    }

    public virtual J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, P p)
    {
        Visit(nullSafeExpression.Expression, p);
        return nullSafeExpression;
    }

    public virtual J VisitDefaultExpression(DefaultExpression defaultExpression, P p)
    {
        if (defaultExpression.TypeOperator != null)
        {
            foreach (var paddedType in defaultExpression.TypeOperator.Elements)
            {
                Visit(paddedType.Element, p);
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

    public virtual J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, P p)
    {
        Visit(ctp.Name, p);
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

    public virtual J VisitYield(Yield yield, P p)
    {
        Visit(yield.ReturnOrBreakKeyword, p);
        if (yield.Expression != null) Visit(yield.Expression, p);
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

    // ---- New types ----

    public virtual J VisitKeyword(Keyword keyword, P p) => keyword;

    public virtual J VisitCsArgument(CsArgument csArgument, P p)
    {
        Visit(csArgument.Expression, p);
        return csArgument;
    }

    public virtual J VisitNameColon(NameColon nameColon, P p) => nameColon;

    public virtual J VisitAnnotatedStatement(AnnotatedStatement annotatedStatement, P p)
    {
        Visit(annotatedStatement.Statement, p);
        return annotatedStatement;
    }

    public virtual J VisitArrayRankSpecifier(ArrayRankSpecifier arrayRankSpecifier, P p) => arrayRankSpecifier;

    public virtual J VisitAssignmentOperation(AssignmentOperation assignmentOperation, P p)
    {
        Visit(assignmentOperation.Variable, p);
        Visit(assignmentOperation.AssignmentValue, p);
        return assignmentOperation;
    }

    public virtual J VisitStackAllocExpression(StackAllocExpression stackAllocExpression, P p)
    {
        Visit(stackAllocExpression.Expression, p);
        return stackAllocExpression;
    }

    public virtual J VisitGotoStatement(GotoStatement gotoStatement, P p)
    {
        if (gotoStatement.Target != null) Visit(gotoStatement.Target, p);
        return gotoStatement;
    }

    public virtual J VisitEventDeclaration(EventDeclaration eventDeclaration, P p)
    {
        Visit(eventDeclaration.TypeExpression, p);
        Visit(eventDeclaration.Name, p);
        return eventDeclaration;
    }

    public virtual J VisitCsBinary(CsBinary csBinary, P p)
    {
        Visit(csBinary.Left, p);
        Visit(csBinary.Right, p);
        return csBinary;
    }

    public virtual J VisitCollectionExpression(CollectionExpression collectionExpression, P p) => collectionExpression;

    public virtual J VisitCsExpressionStatement(CsExpressionStatement csExpressionStatement, P p)
    {
        Visit(csExpressionStatement.Expression, p);
        return csExpressionStatement;
    }

    public virtual J VisitForEachVariableLoop(ForEachVariableLoop forEachVariableLoop, P p)
    {
        Visit(forEachVariableLoop.ControlElement, p);
        Visit(forEachVariableLoop.Body.Element, p);
        return forEachVariableLoop;
    }

    public virtual J VisitForEachVariableLoopControl(ForEachVariableLoopControl control, P p)
    {
        Visit(control.Variable.Element, p);
        Visit(control.Iterable.Element, p);
        return control;
    }

    public virtual J VisitCsMethodDeclaration(CsMethodDeclaration methodDeclaration, P p)
    {
        Visit(methodDeclaration.ReturnTypeExpression, p);
        Visit(methodDeclaration.Name, p);
        if (methodDeclaration.Body != null) Visit(methodDeclaration.Body, p);
        return methodDeclaration;
    }

    public virtual J VisitUsingStatement(UsingStatement usingStatement, P p)
    {
        Visit(usingStatement.ExpressionPadded.Element, p);
        Visit(usingStatement.Statement, p);
        return usingStatement;
    }

    public virtual J VisitAllowsConstraintClause(AllowsConstraintClause clause, P p) => clause;
    public virtual J VisitRefStructConstraint(RefStructConstraint constraint, P p) => constraint;
    public virtual J VisitClassOrStructConstraint(ClassOrStructConstraint constraint, P p) => constraint;
    public virtual J VisitConstructorConstraint(ConstructorConstraint constraint, P p) => constraint;
    public virtual J VisitDefaultConstraint(DefaultConstraint constraint, P p) => constraint;

    public virtual J VisitSingleVariableDesignation(SingleVariableDesignation designation, P p)
    {
        Visit(designation.Name, p);
        return designation;
    }

    public virtual J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation designation, P p) => designation;

    public virtual J VisitDiscardVariableDesignation(DiscardVariableDesignation designation, P p)
    {
        Visit(designation.Discard, p);
        return designation;
    }

    public virtual J VisitCsUnary(CsUnary csUnary, P p)
    {
        Visit(csUnary.Expression, p);
        return csUnary;
    }

    public virtual J VisitTupleElement(TupleElement tupleElement, P p)
    {
        Visit(tupleElement.ElementType, p);
        if (tupleElement.Name != null) Visit(tupleElement.Name, p);
        return tupleElement;
    }

    public virtual J VisitCsNewClass(CsNewClass csNewClass, P p)
    {
        Visit(csNewClass.NewClassCore, p);
        if (csNewClass.Initializer != null) Visit(csNewClass.Initializer, p);
        return csNewClass;
    }

    public virtual J VisitImplicitElementAccess(ImplicitElementAccess implicitElementAccess, P p) => implicitElementAccess;

    public virtual J VisitUnaryPattern(UnaryPattern unaryPattern, P p)
    {
        Visit(unaryPattern.Operator, p);
        Visit(unaryPattern.PatternValue, p);
        return unaryPattern;
    }

    public virtual J VisitTypePattern(TypePattern typePattern, P p)
    {
        Visit(typePattern.TypeIdentifier, p);
        if (typePattern.Designation != null) Visit(typePattern.Designation, p);
        return typePattern;
    }

    public virtual J VisitBinaryPattern(BinaryPattern binaryPattern, P p)
    {
        Visit(binaryPattern.Left, p);
        Visit(binaryPattern.Right, p);
        return binaryPattern;
    }

    public virtual J VisitConstantPattern(ConstantPattern constantPattern, P p)
    {
        Visit(constantPattern.Value, p);
        return constantPattern;
    }

    public virtual J VisitDiscardPattern(DiscardPattern discardPattern, P p) => discardPattern;

    public virtual J VisitListPattern(ListPattern listPattern, P p)
    {
        if (listPattern.Designation != null) Visit(listPattern.Designation, p);
        return listPattern;
    }

    public virtual J VisitParenthesizedPattern(ParenthesizedPattern parenthesizedPattern, P p) => parenthesizedPattern;

    public virtual J VisitRecursivePattern(RecursivePattern recursivePattern, P p)
    {
        if (recursivePattern.TypeQualifier != null) Visit(recursivePattern.TypeQualifier, p);
        if (recursivePattern.PositionalPattern != null) Visit(recursivePattern.PositionalPattern, p);
        if (recursivePattern.PropertyPatternValue != null) Visit(recursivePattern.PropertyPatternValue, p);
        if (recursivePattern.Designation != null) Visit(recursivePattern.Designation, p);
        return recursivePattern;
    }

    public virtual J VisitVarPattern(VarPattern varPattern, P p)
    {
        Visit(varPattern.Designation, p);
        return varPattern;
    }

    public virtual J VisitPositionalPatternClause(PositionalPatternClause clause, P p) => clause;
    public virtual J VisitSlicePattern(SlicePattern slicePattern, P p) => slicePattern;
    public virtual J VisitPropertyPatternClause(PropertyPatternClause clause, P p) => clause;

    public virtual J VisitSubpattern(Subpattern subpattern, P p)
    {
        if (subpattern.Name != null) Visit(subpattern.Name, p);
        Visit(subpattern.PatternValue.Element, p);
        return subpattern;
    }

    public virtual J VisitSwitchExpression(SwitchExpression switchExpression, P p)
    {
        Visit(switchExpression.ExpressionPadded.Element, p);
        foreach (var arm in switchExpression.Arms.Elements)
            Visit(arm.Element, p);
        return switchExpression;
    }

    public virtual J VisitSwitchExpressionArm(SwitchExpressionArm arm, P p)
    {
        Visit(arm.Pattern, p);
        Visit(arm.ExpressionPadded.Element, p);
        return arm;
    }

    public virtual J VisitSwitchSection(SwitchSection switchSection, P p) => switchSection;
    public virtual J VisitDefaultSwitchLabel(DefaultSwitchLabel label, P p) => label;

    public virtual J VisitCasePatternSwitchLabel(CasePatternSwitchLabel label, P p)
    {
        Visit(label.Pattern, p);
        return label;
    }

    public virtual J VisitSwitchStatement(SwitchStatement switchStatement, P p) => switchStatement;

    public virtual J VisitLockStatement(LockStatement lockStatement, P p)
    {
        Visit(lockStatement.ExpressionValue, p);
        Visit(lockStatement.StatementPadded.Element, p);
        return lockStatement;
    }

    public virtual J VisitCheckedExpression(CheckedExpression checkedExpression, P p)
    {
        Visit(checkedExpression.CheckedOrUncheckedKeyword, p);
        Visit(checkedExpression.ExpressionValue, p);
        return checkedExpression;
    }

    public virtual J VisitCheckedStatement(CheckedStatement checkedStatement, P p)
    {
        Visit(checkedStatement.KeywordValue, p);
        Visit(checkedStatement.Block, p);
        return checkedStatement;
    }

    public virtual J VisitRangeExpression(RangeExpression rangeExpression, P p)
    {
        if (rangeExpression.Start != null) Visit(rangeExpression.Start.Element, p);
        if (rangeExpression.End != null) Visit(rangeExpression.End, p);
        return rangeExpression;
    }

    public virtual J VisitIndexerDeclaration(IndexerDeclaration indexerDeclaration, P p)
    {
        Visit(indexerDeclaration.TypeExpression, p);
        Visit(indexerDeclaration.Indexer, p);
        return indexerDeclaration;
    }

    public virtual J VisitDelegateDeclaration(DelegateDeclaration delegateDeclaration, P p)
    {
        Visit(delegateDeclaration.ReturnType.Element, p);
        Visit(delegateDeclaration.IdentifierName, p);
        return delegateDeclaration;
    }

    public virtual J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration conversion, P p)
    {
        Visit(conversion.ReturnType.Element, p);
        if (conversion.Body != null) Visit(conversion.Body, p);
        return conversion;
    }

    public virtual J VisitOperatorDeclaration(OperatorDeclaration operatorDeclaration, P p)
    {
        Visit(operatorDeclaration.ReturnType, p);
        Visit(operatorDeclaration.Body, p);
        return operatorDeclaration;
    }

    public virtual J VisitEnumDeclaration(EnumDeclaration enumDeclaration, P p) => enumDeclaration;

    public virtual J VisitEnumMemberDeclaration(EnumMemberDeclaration enumMember, P p)
    {
        Visit(enumMember.Name, p);
        return enumMember;
    }

    public virtual J VisitAliasQualifiedName(AliasQualifiedName aliasQualifiedName, P p)
    {
        Visit(aliasQualifiedName.Name, p);
        return aliasQualifiedName;
    }

    public virtual J VisitCsArrayType(CsArrayType csArrayType, P p)
    {
        if (csArrayType.TypeExpression != null) Visit(csArrayType.TypeExpression, p);
        return csArrayType;
    }

    public virtual J VisitCsTryCatch(CsTryCatch csTryCatch, P p)
    {
        Visit(csTryCatch.Parameter, p);
        Visit(csTryCatch.Body, p);
        return csTryCatch;
    }

    public virtual J VisitCsTry(CsTry csTry, P p)
    {
        Visit(csTry.Body, p);
        foreach (var c in csTry.Catches) Visit(c, p);
        return csTry;
    }

    public virtual J VisitPointerFieldAccess(PointerFieldAccess pointerFieldAccess, P p)
    {
        Visit(pointerFieldAccess.Target, p);
        Visit(pointerFieldAccess.NamePadded.Element, p);
        return pointerFieldAccess;
    }

    public virtual J VisitRefType(RefType refType, P p)
    {
        Visit(refType.TypeIdentifier, p);
        return refType;
    }

    // ---- LINQ ----

    public virtual J VisitQueryExpression(QueryExpression queryExpression, P p)
    {
        var fromClause = (FromClause)VisitFromClause(queryExpression.FromClause, p);
        var body = (QueryBody)VisitQueryBody(queryExpression.Body, p);
        return queryExpression with { FromClause = fromClause, Body = body };
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
        return changed ? queryBody with { Clauses = clauses, SelectOrGroup = selectOrGroup, Continuation = continuation } : queryBody;
    }

    public virtual J VisitFromClause(FromClause fromClause, P p)
    {
        var expr = Visit(fromClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, fromClause.Expression))
            return fromClause with { Expression = e };
        return fromClause;
    }

    public virtual J VisitLetClause(LetClause letClause, P p)
    {
        var expr = Visit(letClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, letClause.Expression))
            return letClause with { Expression = e };
        return letClause;
    }

    public virtual J VisitJoinClause(JoinClause joinClause, P p)
    {
        Visit(joinClause.InExpression.Element, p);
        Visit(joinClause.LeftExpression.Element, p);
        Visit(joinClause.RightExpression, p);
        return joinClause;
    }

    public virtual J VisitJoinIntoClause(JoinIntoClause joinIntoClause, P p)
    {
        Visit(joinIntoClause.Identifier, p);
        return joinIntoClause;
    }

    public virtual J VisitWhereClause(WhereClause whereClause, P p)
    {
        var condition = Visit(whereClause.Condition, p);
        if (condition is Expression e && !ReferenceEquals(e, whereClause.Condition))
            return whereClause with { Condition = e };
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
            orderings.Add(rp with { Element = visited });
        }
        return changed ? orderByClause with { Orderings = orderings } : orderByClause;
    }

    public virtual J VisitOrdering(Ordering ordering, P p)
    {
        var expr = Visit(ordering.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, ordering.Expression))
            return ordering with { ExpressionPadded = ordering.ExpressionPadded with { Element = e } };
        return ordering;
    }

    public virtual J VisitSelectClause(SelectClause selectClause, P p)
    {
        var expr = Visit(selectClause.Expression, p);
        if (expr is Expression e && !ReferenceEquals(e, selectClause.Expression))
            return selectClause with { Expression = e };
        return selectClause;
    }

    public virtual J VisitGroupClause(GroupClause groupClause, P p)
    {
        Visit(groupClause.GroupExpression.Element, p);
        Visit(groupClause.Key, p);
        return groupClause;
    }

    public virtual J VisitQueryContinuation(QueryContinuation queryContinuation, P p)
    {
        Visit(queryContinuation.Identifier, p);
        var body = (QueryBody)VisitQueryBody(queryContinuation.Body, p);
        if (!ReferenceEquals(body, queryContinuation.Body))
            return queryContinuation with { Body = body };
        return queryContinuation;
    }
}
