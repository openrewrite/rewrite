using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;
using static Rewrite.Core.Rpc.Reference;

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// Serializes C# AST elements via the RPC protocol.
/// Uses double delegation with JavaSender for J types.
/// </summary>
public class CSharpSender : CSharpVisitor<RpcSendQueue>
{
    private readonly CSharpSenderDelegate _delegate;

    private static readonly IList<JRightPadded<Statement>> EmptyPaddedStatements =
        Array.Empty<JRightPadded<Statement>>();

    private static readonly IList<AttributeList> EmptyAttributeLists =
        Array.Empty<AttributeList>();

    public CSharpSender()
    {
        _delegate = new CSharpSenderDelegate(this);
    }

    public override J? Visit(Tree? tree, RpcSendQueue q)
    {
        if (tree == null) return null;

        // ExpressionStatement (from Rewrite.Java) maps to Cs$ExpressionStatement in Java,
        // which wraps expression in JRightPadded. C#'s model has a bare Expression, so we
        // intercept here and send in the format Java's CSharpReceiver expects.
        if (tree is Java.ExpressionStatement es)
        {
            Cursor = new Cursor(Cursor, tree);
            PreVisit(es, q);
            q.GetAndSend(es,
                e => new JRightPadded<Expression>(e.Expression, Space.Empty, Markers.Empty),
                rp => VisitRightPadded(rp, q));
            Cursor = Cursor.Parent!;
            return es;
        }

        if (tree is not Cs)
        {
            return _delegate.Visit(tree, q);
        }

        Cursor = new Cursor(Cursor, tree);
        PreVisit((J)tree, q);

        var result = tree switch
        {
            CompilationUnit cu => VisitCompilationUnit(cu, q),
            UsingDirective ud => VisitUsingDirective(ud, q),
            PropertyDeclaration pd => VisitPropertyDeclaration(pd, q),
            AccessorDeclaration ad => VisitAccessorDeclaration(ad, q),
            AttributeList al => VisitAttributeList(al, q),
            NamedExpression ne => VisitNamedExpression(ne, q),
            RefExpression re => VisitRefExpression(re, q),
            DeclarationExpression de => VisitDeclarationExpression(de, q),
            CsLambda csl => VisitCsLambda(csl, q),
            IsPattern ip => VisitIsPattern(ip, q),
            StatementExpression se => VisitStatementExpression(se, q),
            SizeOf sof => VisitSizeOf(sof, q),
            UnsafeStatement us => VisitUnsafeStatement(us, q),
            FixedStatement fs => VisitFixedStatement(fs, q),
            PointerType pt => VisitPointerType(pt, q),
            DefaultExpression de2 => VisitDefaultExpression(de2, q),
            ExternAlias ea => VisitExternAlias(ea, q),
            InitializerExpression ie => VisitInitializerExpression(ie, q),
            RelationalPattern rp => VisitRelationalPattern(rp, q),
            PropertyPattern pp => VisitPropertyPattern(pp, q),
            ConstrainedTypeParameter ctp => VisitConstrainedTypeParameter(ctp, q),
            InterpolatedString istr => VisitInterpolatedString(istr, q),
            Interpolation interp => VisitInterpolation(interp, q),
            AwaitExpression ae => VisitAwaitExpression(ae, q),
            Yield ys => VisitYield(ys, q),
            NamespaceDeclaration ns => VisitNamespaceDeclaration(ns, q),
            TupleType tt => VisitTupleType(tt, q),
            TupleExpression te => VisitTupleExpression(te, q),
            ConditionalDirective cd => VisitConditionalDirective(cd, q),
            PragmaWarningDirective pwd => VisitPragmaWarningDirective(pwd, q),
            NullableDirective nd => VisitNullableDirective(nd, q),
            RegionDirective rd => VisitRegionDirective(rd, q),
            EndRegionDirective erd => VisitEndRegionDirective(erd, q),
            DefineDirective dd => VisitDefineDirective(dd, q),
            UndefDirective ud2 => VisitUndefDirective(ud2, q),
            ErrorDirective ed => VisitErrorDirective(ed, q),
            WarningDirective wd => VisitWarningDirective(wd, q),
            LineDirective ld => VisitLineDirective(ld, q),
            NullSafeExpression nse => VisitNullSafeExpression(nse, q),
            // New types
            Keyword kw => VisitKeyword(kw, q),
            NameColon nc => VisitNameColon(nc, q),
            AnnotatedStatement ans => VisitAnnotatedStatement(ans, q),
            ArrayRankSpecifier ars => VisitArrayRankSpecifier(ars, q),
            AssignmentOperation ao => VisitAssignmentOperation(ao, q),
            StackAllocExpression sae => VisitStackAllocExpression(sae, q),
            GotoStatement gs => VisitGotoStatement(gs, q),
            EventDeclaration evd => VisitEventDeclaration(evd, q),
            CsBinary csb => VisitCsBinary(csb, q),
            CollectionExpression ce => VisitCollectionExpression(ce, q),
            CsExpressionStatement ces => VisitCsExpressionStatement(ces, q),
            ForEachVariableLoop fevl => VisitForEachVariableLoop(fevl, q),
            ForEachVariableLoopControl fevlc => VisitForEachVariableLoopControl(fevlc, q),
            CsMethodDeclaration cmd => VisitCsMethodDeclaration(cmd, q),
            UsingStatement ust => VisitUsingStatement(ust, q),
            AllowsConstraintClause acc => VisitAllowsConstraintClause(acc, q),
            RefStructConstraint rsc => VisitRefStructConstraint(rsc, q),
            ClassOrStructConstraint cosc => VisitClassOrStructConstraint(cosc, q),
            ConstructorConstraint cc => VisitConstructorConstraint(cc, q),
            DefaultConstraint dc => VisitDefaultConstraint(dc, q),
            SingleVariableDesignation svd => VisitSingleVariableDesignation(svd, q),
            ParenthesizedVariableDesignation pvd => VisitParenthesizedVariableDesignation(pvd, q),
            DiscardVariableDesignation dvd => VisitDiscardVariableDesignation(dvd, q),
            CsUnary csu => VisitCsUnary(csu, q),
            TupleElement tel => VisitTupleElement(tel, q),
            CsNewClass csnc => VisitCsNewClass(csnc, q),
            ImplicitElementAccess iea => VisitImplicitElementAccess(iea, q),
            ConstantPattern cp => VisitConstantPattern(cp, q),
            DiscardPattern dp => VisitDiscardPattern(dp, q),
            ListPattern lp => VisitListPattern(lp, q),
            SlicePattern sp => VisitSlicePattern(sp, q),
            SwitchExpression swe => VisitSwitchExpression(swe, q),
            SwitchExpressionArm swea => VisitSwitchExpressionArm(swea, q),
            CheckedExpression che => VisitCheckedExpression(che, q),
            CheckedStatement chs => VisitCheckedStatement(chs, q),
            RangeExpression rang => VisitRangeExpression(rang, q),
            IndexerDeclaration idxd => VisitIndexerDeclaration(idxd, q),
            DelegateDeclaration deld => VisitDelegateDeclaration(deld, q),
            ConversionOperatorDeclaration cod => VisitConversionOperatorDeclaration(cod, q),
            OperatorDeclaration opd => VisitOperatorDeclaration(opd, q),
            EnumDeclaration enumd => VisitEnumDeclaration(enumd, q),
            EnumMemberDeclaration enummd => VisitEnumMemberDeclaration(enummd, q),
            AliasQualifiedName aqn => VisitAliasQualifiedName(aqn, q),
            CsArrayType csat => VisitCsArrayType(csat, q),
            CsTryCatch cstc => VisitCsTryCatch(cstc, q),
            CsTry cstry => VisitCsTry(cstry, q),
            PointerDereference pd => VisitPointerDereference(pd, q),
            PointerFieldAccess pfa => VisitPointerFieldAccess(pfa, q),
            RefType rt => VisitRefType(rt, q),
            AnonymousObjectCreationExpression aoce => VisitAnonymousObjectCreationExpression(aoce, q),
            WithExpression we => VisitWithExpression(we, q),
            SpreadExpression se => VisitSpreadExpression(se, q),
            FunctionPointerType fpt => VisitFunctionPointerType(fpt, q),
            // LINQ
            QueryExpression qe => VisitQueryExpression(qe, q),
            QueryBody qb => VisitQueryBody(qb, q),
            FromClause fc => VisitFromClause(fc, q),
            LetClause lc => VisitLetClause(lc, q),
            JoinClause jc => VisitJoinClause(jc, q),
            JoinIntoClause jic => VisitJoinIntoClause(jic, q),
            WhereClause wc => VisitWhereClause(wc, q),
            OrderByClause obc => VisitOrderByClause(obc, q),
            Ordering ord => VisitOrdering(ord, q),
            SelectClause sc => VisitSelectClause(sc, q),
            GroupClause gc => VisitGroupClause(gc, q),
            QueryContinuation qcont => VisitQueryContinuation(qcont, q),
            _ => throw new InvalidOperationException($"Unknown Cs tree type: {tree.GetType().FullName}")
        };

        Cursor = Cursor.Parent!;
        return result;
    }

    public new J PreVisit(J j, RpcSendQueue q)
    {
        q.GetAndSend(j, (J t) => t.Id);
        q.GetAndSend(j, (J t) => t.Prefix, space => VisitSpace(GetValueNonNull<Space>(space), q));
        q.GetAndSend(j, (J t) => t.Markers);
        return j;
    }

    // ---- CompilationUnit ----
    // Java protocol: SourcePath, Charset, CharsetBomMarked, Checksum, FileAttributes,
    //   Externs (RP list), Usings (RP list), AttributeLists (list), Members (RP list), Eof
    // Nagoya model: SourcePath, Members (flat IList<Statement>), Eof
    public override J VisitCompilationUnit(CompilationUnit cu, RpcSendQueue q)
    {
        q.GetAndSend(cu, c => c.SourcePath);
        // Nagoya doesn't model charset/bom/checksum/fileAttributes; send defaults
        q.GetAndSend(cu, _ => "UTF-8");
        q.GetAndSend(cu, _ => false);
        q.GetAndSend(cu, _ => (object?)null);
        q.GetAndSend(cu, _ => (object?)null);
        // Externs: nagoya doesn't model externs
        q.GetAndSendList(cu, _ => EmptyPaddedStatements,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        // Usings: extract from Members
        q.GetAndSendList(cu,
            c => (IList<JRightPadded<Statement>>)c.Members
                .Where(m => m is UsingDirective)
                .Select(m => new JRightPadded<Statement>(m, Space.Empty, Markers.Empty))
                .ToList(),
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        // AttributeLists: extract from Members
        q.GetAndSendList(cu,
            c => (IList<AttributeList>)c.Members.OfType<AttributeList>().ToList(),
            t => (object)t.Id, t => Visit(t, q));
        // Members: remaining (non-using, non-attributelist)
        q.GetAndSendList(cu,
            c => (IList<JRightPadded<Statement>>)c.Members
                .Where(m => m is not UsingDirective && m is not AttributeList)
                .Select(m => new JRightPadded<Statement>(m, Space.Empty, Markers.Empty))
                .ToList(),
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        q.GetAndSend(cu, c => c.Eof, space => VisitSpace(space, q));
        return cu;
    }

    // ---- UsingDirective ----
    // Java protocol: Global (RP<Keyword>?), Static (LP<Keyword>?), Unsafe (LP<Keyword>?),
    //   Alias (RP<Identifier>?), NamespaceOrType
    // Nagoya model: Global (RP<bool>), Static (LP<bool>), Alias (RP<Identifier>?), NamespaceOrType
    public override J VisitUsingDirective(UsingDirective ud, RpcSendQueue q)
    {
        q.GetAndSend(ud, u => u.Global, rp => VisitRightPadded(rp, q));
        q.GetAndSend(ud, u => u.Static, lp => VisitLeftPadded(lp, q));
        // Unsafe: nagoya doesn't model this, send a default JLeftPadded<bool>(false)
        q.GetAndSend(ud, _ => new JLeftPadded<bool>(Space.Empty, false), lp => VisitLeftPadded(lp, q));
        q.GetAndSend(ud, u => u.Alias, rp => VisitRightPadded(rp!, q));
        q.GetAndSend(ud, u => (J)u.NamespaceOrType, el => Visit(el, q));
        return ud;
    }

    // ---- PropertyDeclaration ----
    public override J VisitPropertyDeclaration(PropertyDeclaration pd, RpcSendQueue q)
    {
        // Java model has AttributeLists; nagoya does not
        q.GetAndSendList(pd, _ => EmptyAttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(pd, p => p.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(pd, p => (J)p.TypeExpression, el => Visit(el, q));
        // InterfaceSpecifier: nagoya doesn't model this
        q.GetAndSend(pd, _ => (object?)null);
        q.GetAndSend(pd, p => (J)p.Name, el => Visit(el, q));
        q.GetAndSend(pd, p => (J?)p.Accessors, el => Visit(el!, q));
        q.GetAndSend(pd, p => p.ExpressionBody, lp => VisitLeftPadded(lp!, q));
        // Initializer: nagoya doesn't model this
        q.GetAndSend(pd, _ => (object?)null);
        return pd;
    }

    // ---- AccessorDeclaration ----
    public override J VisitAccessorDeclaration(AccessorDeclaration ad, RpcSendQueue q)
    {
        // Java model has Attributes (list); nagoya does not
        q.GetAndSendList(ad, _ => EmptyAttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(ad, a => a.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(ad, a => a.Kind, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(ad, a => a.ExpressionBody, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(ad, a => (J?)a.Body, el => Visit(el!, q));
        return ad;
    }

    // ---- AttributeList ----
    public override J VisitAttributeList(AttributeList al, RpcSendQueue q)
    {
        q.GetAndSend(al, a => a.Target, rp => VisitRightPadded(rp!, q));
        q.GetAndSendList(al, a => a.Attributes,
            rp => (object)rp.Element.Id, rp => VisitRightPadded(rp, q));
        return al;
    }

    // ---- NamedExpression ----
    public override J VisitNamedExpression(NamedExpression ne, RpcSendQueue q)
    {
        q.GetAndSend(ne, n => n.Name, rp => VisitRightPadded(rp, q));
        q.GetAndSend(ne, n => (J)n.Expression, el => Visit(el, q));
        return ne;
    }

    // ---- RefExpression ----
    public override J VisitRefExpression(RefExpression re, RpcSendQueue q)
    {
        q.GetAndSend(re, r => (J)r.Expression, el => Visit(el, q));
        return re;
    }

    // ---- DeclarationExpression ----
    public override J VisitDeclarationExpression(DeclarationExpression de, RpcSendQueue q)
    {
        q.GetAndSend(de, d => (J?)d.TypeExpression, el => Visit(el!, q));
        q.GetAndSend(de, d => (J)d.Variables, el => Visit(el, q));
        return de;
    }

    // ---- CsLambda ----
    public override J VisitCsLambda(CsLambda csl, RpcSendQueue q)
    {
        q.GetAndSendList(csl, c => (IList<AttributeList>)c.AttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSend(csl, c => (J)c.LambdaExpression, el => Visit(el, q));
        q.GetAndSend(csl, c => (J?)c.ReturnType, el => Visit(el!, q));
        q.GetAndSendList(csl, c => c.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        return csl;
    }

    // ---- RelationalPattern ----
    public override J VisitIsPattern(IsPattern ip, RpcSendQueue q)
    {
        q.GetAndSend(ip, i => (J)i.Expression, el => Visit(el, q));
        q.GetAndSend(ip, i => i.Pattern, lp => VisitLeftPadded(lp, q));
        return ip;
    }

    public override J VisitStatementExpression(StatementExpression se, RpcSendQueue q)
    {
        q.GetAndSend(se, s => (J)s.Statement, el => Visit(el, q));
        return se;
    }

    public override J VisitSizeOf(SizeOf sizeOf, RpcSendQueue q)
    {
        q.GetAndSend(sizeOf, s => (J)s.Expression, el => Visit(el, q));
        q.GetAndSend(sizeOf, s => AsRef(s.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return sizeOf;
    }

    public override J VisitUnsafeStatement(UnsafeStatement unsafeStatement, RpcSendQueue q)
    {
        q.GetAndSend(unsafeStatement, u => (J)u.Block, el => Visit(el, q));
        return unsafeStatement;
    }

    public override J VisitPointerType(PointerType pointerType, RpcSendQueue q)
    {
        q.GetAndSend(pointerType, p => p.ElementType, el => VisitRightPadded(el, q));
        return pointerType;
    }

    public override J VisitFixedStatement(FixedStatement fixedStatement, RpcSendQueue q)
    {
        q.GetAndSend(fixedStatement, f => (J)f.Declarations, el => Visit(el, q));
        q.GetAndSend(fixedStatement, f => (J)f.Block, el => Visit(el, q));
        return fixedStatement;
    }

    public override J VisitDefaultExpression(DefaultExpression defaultExpression, RpcSendQueue q)
    {
        q.GetAndSend(defaultExpression, d => d.TypeOperator, el => VisitContainer(el, q));
        return defaultExpression;
    }

    public override J VisitExternAlias(ExternAlias externAlias, RpcSendQueue q)
    {
        q.GetAndSend(externAlias, e => e.Identifier, el => VisitLeftPadded(el, q));
        return externAlias;
    }

    public override J VisitInitializerExpression(InitializerExpression initializerExpression, RpcSendQueue q)
    {
        q.GetAndSend(initializerExpression, i => i.Expressions, el => VisitContainer(el, q));
        return initializerExpression;
    }

    public override J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, RpcSendQueue q)
    {
        q.GetAndSend(nullSafeExpression, n => n.ExpressionPadded, el => VisitRightPadded(el, q));
        return nullSafeExpression;
    }

    public override J VisitRelationalPattern(RelationalPattern rp, RpcSendQueue q)
    {
        q.GetAndSend(rp, r => r.Operator, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(rp, r => (J)r.Value, el => Visit(el, q));
        return rp;
    }

    // ---- PropertyPattern ----
    public override J VisitPropertyPattern(PropertyPattern pp, RpcSendQueue q)
    {
        q.GetAndSend(pp, p => (J?)p.TypeQualifier, el => Visit(el!, q));
        q.GetAndSend(pp, p => p.Subpatterns, c => VisitContainer(c, q));
        q.GetAndSend(pp, p => (J?)p.Designation, el => Visit(el!, q));
        return pp;
    }

    // ---- ConstrainedTypeParameter ----
    public override J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, RpcSendQueue q)
    {
        q.GetAndSendList(ctp, c => (IList<AttributeList>)c.AttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSend(ctp, c => c.Variance, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(ctp, c => (J)c.Name, el => Visit(el, q));
        q.GetAndSend(ctp, c => c.WhereConstraint, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(ctp, c => c.Constraints, c2 => VisitContainer(c2!, q));
        q.GetAndSend(ctp, c => AsRef(c.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return ctp;
    }

    // ---- InterpolatedString ----
    public override J VisitInterpolatedString(InterpolatedString istr, RpcSendQueue q)
    {
        // Java sends: Start (string), Parts (RP list), End (string)
        // Nagoya has: Delimiter (string), Parts (IList<J>)
        q.GetAndSend(istr, i => i.Delimiter);
        q.GetAndSendList(istr,
            i => (IList<JRightPadded<J>>)i.Parts
                .Select(p => new JRightPadded<J>(p, Space.Empty, Markers.Empty))
                .ToList(),
            rp => (object)rp.Element.Id, rp => VisitRightPadded(rp, q));
        q.GetAndSend(istr, i => i.EndDelimiter);
        return istr;
    }

    // ---- Interpolation ----
    public override J VisitInterpolation(Interpolation interp, RpcSendQueue q)
    {
        // Java sends Expression (RP), Alignment (RP), Format (RP)
        // Nagoya has Expression, Alignment (LP?), Format (LP?), After (Space)
        q.GetAndSend(interp,
            i => new JRightPadded<Expression>(i.Expression, i.After, Markers.Empty),
            rp => VisitRightPadded(rp, q));
        q.GetAndSend(interp,
            i => i.Alignment != null
                ? (JRightPadded<Expression>?)new JRightPadded<Expression>(
                    i.Alignment.Element, Space.Empty, Markers.Empty)
                : null,
            rp => VisitRightPadded(rp!, q));
        q.GetAndSend(interp,
            i => i.Format != null
                ? (JRightPadded<Expression>?)new JRightPadded<Expression>(
                    i.Format.Element, Space.Empty, Markers.Empty)
                : null,
            rp => VisitRightPadded(rp!, q));
        return interp;
    }

    // ---- AwaitExpression ----
    public override J VisitAwaitExpression(AwaitExpression ae, RpcSendQueue q)
    {
        q.GetAndSend(ae, a => (J)a.Expression, el => Visit(el, q));
        // Java has type attribution; nagoya doesn't
        q.GetAndSend(ae, _ => (object?)null);
        return ae;
    }

    // ---- Yield ----
    public override J VisitYield(Yield ys, RpcSendQueue q)
    {
        q.GetAndSend(ys, y => (J)y.ReturnOrBreakKeyword, el => Visit(el, q));
        q.GetAndSend(ys, y => (J?)y.Expression, el => Visit(el!, q));
        return ys;
    }

    // ---- NamespaceDeclaration ----
    // Java has BlockScopeNamespaceDeclaration / FileScopeNamespaceDeclaration;
    // nagoya has unified NamespaceDeclaration
    public override J VisitNamespaceDeclaration(NamespaceDeclaration ns, RpcSendQueue q)
    {
        q.GetAndSend(ns, n => n.Name, rp => VisitRightPadded(rp, q));
        // Externs: nagoya doesn't model
        q.GetAndSendList(ns, _ => EmptyPaddedStatements,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        // Usings: extract from Members
        q.GetAndSendList(ns,
            n => (IList<JRightPadded<Statement>>)n.Members
                .Where(m => m.Element is UsingDirective).ToList(),
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        // Members: non-using
        q.GetAndSendList(ns,
            n => (IList<JRightPadded<Statement>>)n.Members
                .Where(m => m.Element is not UsingDirective).ToList(),
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        q.GetAndSend(ns, n => n.End, space => VisitSpace(space, q));
        return ns;
    }

    // ---- TupleType ----
    public override J VisitTupleType(TupleType tt, RpcSendQueue q)
    {
        q.GetAndSend(tt, t => t.Elements, c => VisitContainer(c, q));
        // Java has type attribution; nagoya doesn't
        q.GetAndSend(tt, _ => (object?)null);
        return tt;
    }

    // ---- TupleExpression ----
    public override J VisitTupleExpression(TupleExpression te, RpcSendQueue q)
    {
        q.GetAndSend(te, t => t.Arguments, c => VisitContainer(c, q));
        return te;
    }

    // ---- ConditionalDirective ----
    public override J VisitConditionalDirective(ConditionalDirective cd, RpcSendQueue q)
    {
        // Send DirectiveLines as inline list
        q.GetAndSend(cd, c => (object)c.DirectiveLines.Count);
        foreach (var dl in cd.DirectiveLines)
        {
            q.GetAndSend(cd, _ => (object)dl.LineNumber);
            q.GetAndSend(cd, _ => (object)dl.Text);
            q.GetAndSend(cd, _ => (object)(int)dl.Kind);
            q.GetAndSend(cd, _ => (object)dl.GroupId);
            q.GetAndSend(cd, _ => (object)dl.ActiveBranchIndex);
        }
        // Send Branches
        q.GetAndSendList(cd, c => c.Branches,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        return cd;
    }

    // ---- PragmaWarningDirective ----
    public override J VisitPragmaWarningDirective(PragmaWarningDirective pwd, RpcSendQueue q)
    {
        q.GetAndSend(pwd, p => (object)p.Action);
        q.GetAndSendList(pwd, p => p.WarningCodes,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        return pwd;
    }

    // ---- NullableDirective ----
    public override J VisitNullableDirective(NullableDirective nd, RpcSendQueue q)
    {
        q.GetAndSend(nd, n => (object)n.Setting);
        q.GetAndSend(nd, n => (object?)n.Target);
        q.GetAndSend(nd, n => (object)n.HashSpacing);
        q.GetAndSend(nd, n => (object)n.TrailingComment);
        return nd;
    }

    // ---- RegionDirective ----
    public override J VisitRegionDirective(RegionDirective rd, RpcSendQueue q)
    {
        q.GetAndSend(rd, r => (object?)r.Name);
        q.GetAndSend(rd, r => (object)r.HashSpacing);
        return rd;
    }

    // ---- EndRegionDirective ----
    public override J VisitEndRegionDirective(EndRegionDirective erd, RpcSendQueue q)
    {
        q.GetAndSend(erd, e => (object?)e.Name);
        q.GetAndSend(erd, e => (object)e.HashSpacing);
        return erd;
    }

    // ---- DefineDirective ----
    public override J VisitDefineDirective(DefineDirective dd, RpcSendQueue q)
    {
        q.GetAndSend(dd, d => (J)d.Symbol, el => Visit(el, q));
        return dd;
    }

    // ---- UndefDirective ----
    public override J VisitUndefDirective(UndefDirective ud, RpcSendQueue q)
    {
        q.GetAndSend(ud, u => (J)u.Symbol, el => Visit(el, q));
        return ud;
    }

    // ---- ErrorDirective ----
    public override J VisitErrorDirective(ErrorDirective ed, RpcSendQueue q)
    {
        q.GetAndSend(ed, e => (object)e.Message);
        return ed;
    }

    // ---- WarningDirective ----
    public override J VisitWarningDirective(WarningDirective wd, RpcSendQueue q)
    {
        q.GetAndSend(wd, w => (object)w.Message);
        return wd;
    }

    // ---- LineDirective ----
    public override J VisitLineDirective(LineDirective ld, RpcSendQueue q)
    {
        q.GetAndSend(ld, l => (object)l.Kind);
        q.GetAndSend(ld, l => (J?)l.Line, el => Visit(el!, q));
        q.GetAndSend(ld, l => (J?)l.File, el => Visit(el!, q));
        return ld;
    }

    // ---- LINQ ----

    public override J VisitQueryExpression(QueryExpression qe, RpcSendQueue q)
    {
        q.GetAndSend(qe, x => (J)x.FromClause, el => Visit(el, q));
        q.GetAndSend(qe, x => (J)x.Body, el => Visit(el, q));
        return qe;
    }

    public override J VisitQueryBody(QueryBody qb, RpcSendQueue q)
    {
        q.GetAndSendList(qb, x => (IList<QueryClause>)x.Clauses,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSend(qb, x => (J?)x.SelectOrGroup, el => Visit(el!, q));
        q.GetAndSend(qb, x => (J?)x.Continuation, el => Visit(el!, q));
        return qb;
    }

    public override J VisitFromClause(FromClause fc, RpcSendQueue q)
    {
        q.GetAndSend(fc, x => (J?)x.TypeIdentifier, el => Visit(el!, q));
        q.GetAndSend(fc, x => x.IdentifierPadded, rp => VisitRightPadded(rp, q));
        q.GetAndSend(fc, x => (J)x.Expression, el => Visit(el, q));
        return fc;
    }

    public override J VisitLetClause(LetClause lc, RpcSendQueue q)
    {
        q.GetAndSend(lc, x => x.IdentifierPadded, rp => VisitRightPadded(rp, q));
        q.GetAndSend(lc, x => (J)x.Expression, el => Visit(el, q));
        return lc;
    }

    public override J VisitJoinClause(JoinClause jc, RpcSendQueue q)
    {
        q.GetAndSend(jc, x => x.IdentifierPadded, rp => VisitRightPadded(rp, q));
        q.GetAndSend(jc, x => x.InExpression, rp => VisitRightPadded(rp, q));
        q.GetAndSend(jc, x => x.LeftExpression, rp => VisitRightPadded(rp, q));
        q.GetAndSend(jc, x => (J)x.RightExpression, el => Visit(el, q));
        q.GetAndSend(jc, x => x.Into, lp => VisitLeftPadded(lp!, q));
        return jc;
    }

    public override J VisitJoinIntoClause(JoinIntoClause jic, RpcSendQueue q)
    {
        q.GetAndSend(jic, x => (J)x.Identifier, el => Visit(el, q));
        return jic;
    }

    public override J VisitWhereClause(WhereClause wc, RpcSendQueue q)
    {
        q.GetAndSend(wc, x => (J)x.Condition, el => Visit(el, q));
        return wc;
    }

    public override J VisitOrderByClause(OrderByClause obc, RpcSendQueue q)
    {
        q.GetAndSendList(obc, x => x.Orderings,
            rp => (object)rp.Element.Id, rp => VisitRightPadded(rp, q));
        return obc;
    }

    public override J VisitOrdering(Ordering ord, RpcSendQueue q)
    {
        q.GetAndSend(ord, x => x.ExpressionPadded, rp => VisitRightPadded(rp, q));
        q.GetAndSend(ord, x => (object?)x.Direction);
        return ord;
    }

    public override J VisitSelectClause(SelectClause sc, RpcSendQueue q)
    {
        q.GetAndSend(sc, x => (J)x.Expression, el => Visit(el, q));
        return sc;
    }

    public override J VisitGroupClause(GroupClause gc, RpcSendQueue q)
    {
        q.GetAndSend(gc, x => x.GroupExpression, rp => VisitRightPadded(rp, q));
        q.GetAndSend(gc, x => (J)x.Key, el => Visit(el, q));
        return gc;
    }

    public override J VisitQueryContinuation(QueryContinuation qcont, RpcSendQueue q)
    {
        q.GetAndSend(qcont, x => (J)x.Identifier, el => Visit(el, q));
        q.GetAndSend(qcont, x => (J)x.Body, el => Visit(el, q));
        return qcont;
    }

    // ---- New types ----

    public override J VisitKeyword(Keyword kw, RpcSendQueue q)
    {
        q.GetAndSend(kw, k => (object)k.Kind);
        return kw;
    }

    public override J VisitNameColon(NameColon nc, RpcSendQueue q)
    {
        q.GetAndSend(nc, n => n.Name, rp => VisitRightPadded(rp, q));
        return nc;
    }

    public override J VisitAnnotatedStatement(AnnotatedStatement ans, RpcSendQueue q)
    {
        q.GetAndSendList(ans, a => (IList<AttributeList>)a.AttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSend(ans, a => (J)a.Statement, el => Visit(el, q));
        return ans;
    }

    public override J VisitArrayRankSpecifier(ArrayRankSpecifier ars, RpcSendQueue q)
    {
        q.GetAndSend(ars, a => a.Sizes, el => VisitContainer(el, q));
        return ars;
    }

    public override J VisitAssignmentOperation(AssignmentOperation ao, RpcSendQueue q)
    {
        q.GetAndSend(ao, a => (J)a.Variable, el => Visit(el, q));
        q.GetAndSend(ao, a => a.Operator, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(ao, a => (J)a.AssignmentValue, el => Visit(el, q));
        q.GetAndSend(ao, a => AsRef(a.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return ao;
    }

    public override J VisitStackAllocExpression(StackAllocExpression sae, RpcSendQueue q)
    {
        q.GetAndSend(sae, s => (J)s.Expression, el => Visit(el, q));
        return sae;
    }

    public override J VisitGotoStatement(GotoStatement gs, RpcSendQueue q)
    {
        q.GetAndSend(gs, g => (J?)g.CaseOrDefaultKeyword, el => Visit(el!, q));
        q.GetAndSend(gs, g => (J?)g.Target, el => Visit(el!, q));
        return gs;
    }

    public override J VisitEventDeclaration(EventDeclaration evd, RpcSendQueue q)
    {
        q.GetAndSendList(evd, e => (IList<AttributeList>)e.AttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(evd, e => e.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(evd, e => e.TypeExpressionPadded, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(evd, e => e.InterfaceSpecifier, rp => VisitRightPadded(rp!, q));
        q.GetAndSend(evd, e => (J)e.Name, el => Visit(el, q));
        q.GetAndSend(evd, e => e.Accessors, c => VisitContainer(c!, q));
        return evd;
    }

    public override J VisitCsBinary(CsBinary csb, RpcSendQueue q)
    {
        q.GetAndSend(csb, b => (J)b.Left, el => Visit(el, q));
        q.GetAndSend(csb, b => b.Operator, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(csb, b => (J)b.Right, el => Visit(el, q));
        q.GetAndSend(csb, b => AsRef(b.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return csb;
    }

    public override J VisitCollectionExpression(CollectionExpression ce, RpcSendQueue q)
    {
        q.GetAndSendList(ce, c => c.Elements,
            rp => (object)rp.Element.Id, rp => VisitRightPadded(rp, q));
        q.GetAndSend(ce, c => AsRef(c.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return ce;
    }

    public override J VisitCsExpressionStatement(CsExpressionStatement ces, RpcSendQueue q)
    {
        q.GetAndSend(ces, e => e.ExpressionPadded, rp => VisitRightPadded(rp, q));
        return ces;
    }

    public override J VisitForEachVariableLoop(ForEachVariableLoop fevl, RpcSendQueue q)
    {
        q.GetAndSend(fevl, f => (J)f.ControlElement, el => Visit(el, q));
        q.GetAndSend(fevl, f => f.Body, rp => VisitRightPadded(rp, q));
        return fevl;
    }

    public override J VisitForEachVariableLoopControl(ForEachVariableLoopControl ctrl, RpcSendQueue q)
    {
        q.GetAndSend(ctrl, c => c.Variable, rp => VisitRightPadded(rp, q));
        q.GetAndSend(ctrl, c => c.Iterable, rp => VisitRightPadded(rp, q));
        return ctrl;
    }

    public override J VisitCsMethodDeclaration(CsMethodDeclaration cmd, RpcSendQueue q)
    {
        q.GetAndSendList(cmd, m => (IList<AttributeList>)m.Attributes,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(cmd, m => m.Modifiers,
            m2 => (object)m2.Id, m2 => Visit(m2, q));
        q.GetAndSend(cmd, m => m.TypeParameters, c => VisitContainer(c!, q));
        q.GetAndSend(cmd, m => (J)m.ReturnTypeExpression, el => Visit(el, q));
        q.GetAndSend(cmd, m => m.ExplicitInterfaceSpecifier, rp => VisitRightPadded(rp!, q));
        q.GetAndSend(cmd, m => (J)m.Name, el => Visit(el, q));
        q.GetAndSend(cmd, m => m.Parameters, c => VisitContainer(c, q));
        q.GetAndSend(cmd, m => (J?)m.Body, el => Visit(el!, q));
        q.GetAndSend(cmd, m => AsRef(m.MethodType), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return cmd;
    }

    public override J VisitUsingStatement(UsingStatement ust, RpcSendQueue q)
    {
        q.GetAndSend(ust, u => u.ExpressionPadded, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(ust, u => (J)u.Statement, el => Visit(el, q));
        return ust;
    }

    public override J VisitAllowsConstraintClause(AllowsConstraintClause acc, RpcSendQueue q)
    {
        q.GetAndSend(acc, a => a.Expressions, c => VisitContainer(c, q));
        return acc;
    }

    public override J VisitRefStructConstraint(RefStructConstraint rsc, RpcSendQueue q) => rsc;

    public override J VisitClassOrStructConstraint(ClassOrStructConstraint cosc, RpcSendQueue q)
    {
        q.GetAndSend(cosc, c => (object)c.Kind);
        q.GetAndSend(cosc, c => c.Nullable);
        return cosc;
    }

    public override J VisitConstructorConstraint(ConstructorConstraint cc, RpcSendQueue q) => cc;
    public override J VisitDefaultConstraint(DefaultConstraint dc, RpcSendQueue q) => dc;

    public override J VisitSingleVariableDesignation(SingleVariableDesignation svd, RpcSendQueue q)
    {
        q.GetAndSend(svd, s => (J)s.Name, el => Visit(el, q));
        return svd;
    }

    public override J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation pvd, RpcSendQueue q)
    {
        q.GetAndSend(pvd, p => p.Variables, c => VisitContainer(c, q));
        q.GetAndSend(pvd, p => AsRef(p.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return pvd;
    }

    public override J VisitDiscardVariableDesignation(DiscardVariableDesignation dvd, RpcSendQueue q)
    {
        q.GetAndSend(dvd, d => (J)d.Discard, el => Visit(el, q));
        return dvd;
    }

    public override J VisitCsUnary(CsUnary csu, RpcSendQueue q)
    {
        q.GetAndSend(csu, u => u.Operator, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(csu, u => (J)u.Expression, el => Visit(el, q));
        q.GetAndSend(csu, u => AsRef(u.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return csu;
    }

    public override J VisitTupleElement(TupleElement tel, RpcSendQueue q)
    {
        q.GetAndSend(tel, t => (J)t.ElementType, el => Visit(el, q));
        q.GetAndSend(tel, t => (J?)t.Name, el => Visit(el!, q));
        return tel;
    }

    public override J VisitCsNewClass(CsNewClass csnc, RpcSendQueue q)
    {
        q.GetAndSend(csnc, n => (J)n.NewClassCore, el => Visit(el, q));
        q.GetAndSend(csnc, n => (J?)n.Initializer, el => Visit(el!, q));
        return csnc;
    }

    public override J VisitImplicitElementAccess(ImplicitElementAccess iea, RpcSendQueue q)
    {
        q.GetAndSend(iea, i => i.ArgumentList, c => VisitContainer(c, q));
        return iea;
    }

    public override J VisitConstantPattern(ConstantPattern cp, RpcSendQueue q)
    {
        q.GetAndSend(cp, c => (J)c.Value, el => Visit(el, q));
        return cp;
    }

    public override J VisitDiscardPattern(DiscardPattern dp, RpcSendQueue q)
    {
        q.GetAndSend(dp, d => AsRef(d.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return dp;
    }

    public override J VisitListPattern(ListPattern lp, RpcSendQueue q)
    {
        q.GetAndSend(lp, l => l.Patterns, c => VisitContainer(c, q));
        q.GetAndSend(lp, l => (J?)l.Designation, el => Visit(el!, q));
        return lp;
    }

    public override J VisitSwitchExpression(SwitchExpression swe, RpcSendQueue q)
    {
        q.GetAndSend(swe, s => s.ExpressionPadded, rp => VisitRightPadded(rp, q));
        q.GetAndSend(swe, s => s.Arms, c => VisitContainer(c, q));
        return swe;
    }

    public override J VisitSwitchExpressionArm(SwitchExpressionArm swea, RpcSendQueue q)
    {
        q.GetAndSend(swea, s => (J)s.Pattern, el => Visit(el, q));
        q.GetAndSend(swea, s => s.WhenExpression, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(swea, s => s.ExpressionPadded, lp => VisitLeftPadded(lp, q));
        return swea;
    }

    public override J VisitCheckedExpression(CheckedExpression che, RpcSendQueue q)
    {
        q.GetAndSend(che, c => (J)c.CheckedOrUncheckedKeyword, el => Visit(el, q));
        q.GetAndSend(che, c => (J)c.ExpressionValue, el => Visit(el, q));
        return che;
    }

    public override J VisitCheckedStatement(CheckedStatement chs, RpcSendQueue q)
    {
        q.GetAndSend(chs, c => (J)c.KeywordValue, el => Visit(el, q));
        q.GetAndSend(chs, c => (J)c.Block, el => Visit(el, q));
        return chs;
    }

    public override J VisitRangeExpression(RangeExpression rang, RpcSendQueue q)
    {
        q.GetAndSend(rang, r => r.Start, rp => VisitRightPadded(rp!, q));
        q.GetAndSend(rang, r => (J?)r.End, el => Visit(el!, q));
        return rang;
    }

    public override J VisitIndexerDeclaration(IndexerDeclaration idxd, RpcSendQueue q)
    {
        q.GetAndSendList(idxd, i => i.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(idxd, i => (J)i.TypeExpression, el => Visit(el, q));
        q.GetAndSend(idxd, i => i.ExplicitInterfaceSpecifier, rp => VisitRightPadded(rp!, q));
        q.GetAndSend(idxd, i => (J)i.Indexer, el => Visit(el, q));
        q.GetAndSend(idxd, i => i.Parameters, c => VisitContainer(c, q));
        q.GetAndSend(idxd, i => i.ExpressionBody, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(idxd, i => (J?)i.Accessors, el => Visit(el!, q));
        return idxd;
    }

    public override J VisitDelegateDeclaration(DelegateDeclaration deld, RpcSendQueue q)
    {
        q.GetAndSendList(deld, d => (IList<AttributeList>)d.Attributes,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(deld, d => d.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(deld, d => d.ReturnType, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(deld, d => (J)d.IdentifierName, el => Visit(el, q));
        q.GetAndSend(deld, d => d.TypeParameters, c => VisitContainer(c!, q));
        q.GetAndSend(deld, d => d.Parameters, c => VisitContainer(c, q));
        return deld;
    }

    public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration cod, RpcSendQueue q)
    {
        q.GetAndSendList(cod, c => c.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(cod, c => c.Kind, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(cod, c => c.ReturnType, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(cod, c => c.Parameters, c2 => VisitContainer(c2, q));
        q.GetAndSend(cod, c => c.ExpressionBody, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(cod, c => (J?)c.Body, el => Visit(el!, q));
        return cod;
    }

    public override J VisitOperatorDeclaration(OperatorDeclaration opd, RpcSendQueue q)
    {
        q.GetAndSendList(opd, o => (IList<AttributeList>)o.AttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(opd, o => o.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(opd, o => o.ExplicitInterfaceSpecifier, rp => VisitRightPadded(rp!, q));
        q.GetAndSend(opd, o => (J)o.OperatorKeyword, el => Visit(el, q));
        q.GetAndSend(opd, o => (J?)o.CheckedKeyword, el => Visit(el!, q));
        q.GetAndSend(opd, o => o.OperatorToken, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(opd, o => (J)o.ReturnType, el => Visit(el, q));
        q.GetAndSend(opd, o => o.Parameters, c => VisitContainer(c, q));
        q.GetAndSend(opd, o => (J)o.Body, el => Visit(el, q));
        q.GetAndSend(opd, o => AsRef(o.MethodType), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return opd;
    }

    public override J VisitEnumDeclaration(EnumDeclaration enumd, RpcSendQueue q)
    {
        q.GetAndSendList(enumd, e => (IList<AttributeList>)(e.AttributeLists ?? new List<AttributeList>()),
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSendList(enumd, e => e.Modifiers,
            m => (object)m.Id, m => Visit(m, q));
        q.GetAndSend(enumd, e => e.NamePadded, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(enumd, e => e.BaseType, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(enumd, e => e.Members, c => VisitContainer(c!, q));
        return enumd;
    }

    public override J VisitEnumMemberDeclaration(EnumMemberDeclaration enummd, RpcSendQueue q)
    {
        q.GetAndSendList(enummd, e => (IList<AttributeList>)e.AttributeLists,
            t => (object)t.Id, t => Visit(t, q));
        q.GetAndSend(enummd, e => (J)e.Name, el => Visit(el, q));
        q.GetAndSend(enummd, e => e.Initializer, lp => VisitLeftPadded(lp!, q));
        return enummd;
    }

    public override J VisitAliasQualifiedName(AliasQualifiedName aqn, RpcSendQueue q)
    {
        q.GetAndSend(aqn, a => a.Alias, rp => VisitRightPadded(rp, q));
        q.GetAndSend(aqn, a => (J)a.Name, el => Visit(el, q));
        return aqn;
    }

    public override J VisitCsArrayType(CsArrayType csat, RpcSendQueue q)
    {
        q.GetAndSend(csat, a => (J?)a.TypeExpression, el => Visit(el!, q));
        q.GetAndSendList(csat, a => a.Dimensions,
            d => (object)d.Id, d => Visit(d, q));
        q.GetAndSend(csat, a => AsRef(a.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return csat;
    }

    public override J VisitCsTry(CsTry cstry, RpcSendQueue q)
    {
        q.GetAndSend(cstry, t => (J)t.Body, el => Visit(el, q));
        q.GetAndSendList(cstry, t => (IList<CsTryCatch>)t.Catches,
            c => (object)c.Id, c => Visit(c, q));
        q.GetAndSend(cstry, t => t.Finally, lp => VisitLeftPadded(lp!, q));
        return cstry;
    }

    public override J VisitCsTryCatch(CsTryCatch cstc, RpcSendQueue q)
    {
        q.GetAndSend(cstc, c => (J)c.Parameter, el => Visit(el, q));
        q.GetAndSend(cstc, c => c.FilterExpression, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(cstc, c => (J)c.Body, el => Visit(el, q));
        return cstc;
    }

    public override J VisitPointerDereference(PointerDereference pd, RpcSendQueue q)
    {
        q.GetAndSend(pd, p => (J)p.Expression, el => Visit(el, q));
        q.GetAndSend(pd, p => AsRef(p.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return pd;
    }

    public override J VisitPointerFieldAccess(PointerFieldAccess pfa, RpcSendQueue q)
    {
        q.GetAndSend(pfa, p => (J)p.Target, el => Visit(el, q));
        q.GetAndSend(pfa, p => p.NamePadded, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(pfa, p => AsRef(p.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return pfa;
    }

    public override J VisitRefType(RefType rt, RpcSendQueue q)
    {
        q.GetAndSend(rt, r => (J?)r.ReadonlyKeyword, el => Visit(el!, q));
        q.GetAndSend(rt, r => (J)r.TypeIdentifier, el => Visit(el, q));
        q.GetAndSend(rt, r => AsRef(r.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return rt;
    }

    public override J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression aoce, RpcSendQueue q)
    {
        q.GetAndSend(aoce, a => a.Initializers, el => VisitContainer(el, q));
        q.GetAndSend(aoce, a => AsRef(a.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return aoce;
    }

    public override J VisitWithExpression(WithExpression we, RpcSendQueue q)
    {
        q.GetAndSend(we, w => (J)w.Target, el => Visit(el, q));
        q.GetAndSend(we, w => w.InitializerPadded, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(we, w => AsRef(w.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return we;
    }

    public override J VisitSpreadExpression(SpreadExpression se, RpcSendQueue q)
    {
        q.GetAndSend(se, s => (J)s.Expression, el => Visit(el, q));
        q.GetAndSend(se, s => AsRef(s.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return se;
    }

    public override J VisitFunctionPointerType(FunctionPointerType fpt, RpcSendQueue q)
    {
        q.GetAndSend(fpt, f => f.CallingConvention, lp => VisitLeftPadded(lp!, q));
        q.GetAndSend(fpt, f => f.UnmanagedCallingConventionTypes, el => VisitContainer(el!, q));
        q.GetAndSend(fpt, f => f.ParameterTypes, el => VisitContainer(el, q));
        q.GetAndSend(fpt, f => AsRef(f.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return fpt;
    }

    // ---- Helper delegation to JavaSender ----

    public void VisitLeftPadded<T>(JLeftPadded<T> left, RpcSendQueue q) =>
        _delegate.VisitLeftPadded(left, q);

    public void VisitRightPadded<T>(JRightPadded<T> right, RpcSendQueue q) =>
        _delegate.VisitRightPadded(right, q);

    public void VisitContainer<TJ>(JContainer<TJ> container, RpcSendQueue q) where TJ : J =>
        _delegate.VisitContainer(container, q);

    public void VisitSpace(Space space, RpcSendQueue q) =>
        _delegate.VisitSpace(space, q);

    public JavaType? VisitType(JavaType? javaType, RpcSendQueue q) =>
        _delegate.VisitType(javaType, q);

    // ---- Private helpers ----

    private static string DeriveEndDelimiter(string startDelimiter)
    {
        if (startDelimiter.Contains("\"\"\""))
            return "\"\"\"";
        return "\"";
    }

    // ---- Inner delegate class ----

    private class CSharpSenderDelegate : JavaSender
    {
        private readonly CSharpSender _outer;

        public CSharpSenderDelegate(CSharpSender outer)
        {
            _outer = outer;
        }

        public override J? Visit(Tree? tree, RpcSendQueue q)
        {
            if (tree is Cs || tree is ExpressionStatement)
            {
                return _outer.Visit(tree, q);
            }
            return base.Visit(tree, q);
        }
    }
}
