using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using OpenRewrite.Java;
using OpenRewrite.Java.Rpc;

namespace OpenRewrite.CSharp.Rpc;

/// <summary>
/// Deserializes C# AST elements via the RPC protocol.
/// Uses double delegation with JavaReceiver for J types.
/// </summary>
public class CSharpReceiver : CSharpVisitor<RpcReceiveQueue>
{
    private readonly CSharpReceiverDelegate _delegate;

    public CSharpReceiver()
    {
        _delegate = new CSharpReceiverDelegate(this);
    }

    public override J? Visit(Tree? tree, RpcReceiveQueue q)
    {
        if (tree == null) return null;

        // ExpressionStatement (from Rewrite.Java) maps to Cs$ExpressionStatement in Java,
        // which wraps expression in JRightPadded. C#'s model has a bare Expression, so we
        // intercept here and receive in the format Java's CSharpSender sends.
        if (tree is ExpressionStatement es)
        {
            Cursor = new Cursor(Cursor, tree);
            _delegate.ConsumePreVisit(es, q);
            var pvId = _delegate.PvId;
            var rp = q.Receive(
                new JRightPadded<Expression>(es.Expression, Space.Empty, Markers.Empty),
                rp2 => _delegate.VisitRightPadded(rp2, q));
            _delegate.PopPreVisit();
            Cursor = Cursor.Parent!;
            return es.WithId(pvId).WithExpression(rp!.Element);
        }

        if (tree is not Cs)
        {
            return _delegate.Visit(tree, q);
        }

        Cursor = new Cursor(Cursor, tree);
        _delegate.ConsumePreVisit((J)tree, q);

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

        _delegate.PopPreVisit();
        Cursor = Cursor.Parent!;
        return result;
    }

    // PreVisit data is stored in _delegate fields and accessed via properties
    private Guid PvId => _delegate.PvId;
    private Space PvPrefix => _delegate.PvPrefix;
    private Markers PvMarkers => _delegate.PvMarkers;

    // ---- CompilationUnit ----
    public override J VisitCompilationUnit(CompilationUnit cu, RpcReceiveQueue q)
    {
        var sourcePath = q.ReceiveAndGet<string, string>(cu.SourcePath, s => s);
        // Consume charset, bom, checksum, fileAttributes
        q.Receive<string>("UTF-8");
        q.Receive<bool>(false);
        q.Receive<object?>(null);
        q.Receive<object?>(null);
        // Externs: empty list
        q.ReceiveList<JRightPadded<Statement>>([], rp => _delegate.VisitRightPadded(rp, q));
        // Usings
        var usings = q.ReceiveList(
            cu.Members.Where(m => m is UsingDirective)
                .Select(m => new JRightPadded<Statement>(m, Space.Empty, Markers.Empty))
                .ToList(),
            rp => _delegate.VisitRightPadded(rp, q));
        // AttributeLists
        var attrLists = q.ReceiveList(
            cu.Members.OfType<AttributeList>().ToList(),
            t => (AttributeList)VisitNonNull(t, q));
        // Members (non-using, non-attributelist)
        var members = q.ReceiveList(
            cu.Members.Where(m => m is not UsingDirective && m is not AttributeList)
                .Select(m => new JRightPadded<Statement>(m, Space.Empty, Markers.Empty))
                .ToList(),
            rp => _delegate.VisitRightPadded(rp, q));
        var eof = q.Receive(cu.Eof, space => VisitSpace(space, q));

        // Reconstruct members
        var allMembers = new List<Statement>();
        if (usings != null)
        {
            foreach (var rp in usings)
                allMembers.Add(rp.Element);
        }
        if (attrLists != null) allMembers.AddRange(attrLists);
        if (members != null) allMembers.AddRange(members.Select(rp => rp.Element));

        return cu.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithSourcePath(sourcePath!).WithMembers(allMembers).WithEof(eof!);
    }

    // ---- UsingDirective ----
    public override J VisitUsingDirective(UsingDirective ud, RpcReceiveQueue q)
    {
        var global = q.Receive(ud.Global, rp => _delegate.VisitRightPadded(rp, q));
        var @static = q.Receive(ud.Static, lp => _delegate.VisitLeftPadded(lp, q));
        // Unsafe: nagoya doesn't model this; consume and discard
        q.Receive(new JLeftPadded<bool>(Space.Empty, false), lp => _delegate.VisitLeftPadded(lp, q));
        var alias = q.Receive(ud.Alias, rp => _delegate.VisitRightPadded(rp!, q));
        var namespaceOrType = q.Receive((J)ud.NamespaceOrType, el => (J)VisitNonNull(el, q));
        return ud.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers)
            .WithGlobal(global!).WithStatic(@static!).WithAlias(alias).WithNamespaceOrType((TypeTree)namespaceOrType!);
    }

    // ---- PropertyDeclaration ----
    public override J VisitPropertyDeclaration(PropertyDeclaration pd, RpcReceiveQueue q)
    {
        q.ReceiveList<AttributeList>([], t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(pd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeExpression = q.Receive((J)pd.TypeExpression, el => (J)VisitNonNull(el, q));
        q.Receive<object?>(null); // InterfaceSpecifier
        var name = q.Receive((J)pd.Name, el => (J)VisitNonNull(el, q));
        var accessors = q.Receive((J?)pd.Accessors, el => (J)VisitNonNull(el!, q));
        var expressionBody = q.Receive(pd.ExpressionBody, lp => _delegate.VisitLeftPadded(lp!, q));
        q.Receive<object?>(null); // Initializer
        return pd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithModifiers(modifiers!).WithTypeExpression((TypeTree)typeExpression!).WithName((Identifier)name!).WithAccessors((Block?)accessors).WithExpressionBody(expressionBody);
    }

    // ---- AccessorDeclaration ----
    public override J VisitAccessorDeclaration(AccessorDeclaration ad, RpcReceiveQueue q)
    {
        q.ReceiveList<AttributeList>([], t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(ad.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var kind = q.Receive(ad.Kind, lp => _delegate.VisitLeftPadded(lp, q));
        var expressionBody = q.Receive(ad.ExpressionBody, lp => _delegate.VisitLeftPadded(lp!, q));
        var body = q.Receive((J?)ad.Body, el => (J)VisitNonNull(el!, q));
        return ad.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithModifiers(modifiers!).WithKind(kind!).WithExpressionBody(expressionBody).WithBody((Block?)body);
    }

    // ---- AttributeList ----
    public override J VisitAttributeList(AttributeList al, RpcReceiveQueue q)
    {
        var target = q.Receive(al.Target, rp => _delegate.VisitRightPadded(rp!, q));
        var attributes = q.ReceiveList(al.Attributes, rp => _delegate.VisitRightPadded(rp, q));
        return al.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTarget(target).WithAttributes(attributes!);
    }

    // ---- NamedExpression ----
    public override J VisitNamedExpression(NamedExpression ne, RpcReceiveQueue q)
    {
        var name = q.Receive(ne.Name, rp => _delegate.VisitRightPadded(rp, q));
        var expression = q.Receive((J)ne.Expression, el => (J)VisitNonNull(el, q));
        return ne.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithName(name!).WithExpression((Expression)expression!);
    }

    // ---- RefExpression ----
    public override J VisitRefExpression(RefExpression re, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)re.Expression, el => (J)VisitNonNull(el, q));
        return re.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!);
    }

    // ---- DeclarationExpression ----
    public override J VisitDeclarationExpression(DeclarationExpression de, RpcReceiveQueue q)
    {
        var typeExpression = q.Receive((J?)de.TypeExpression, el => (J)VisitNonNull(el!, q));
        var variables = q.Receive((J)de.Variables, el => (J)VisitNonNull(el, q));
        return de.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTypeExpression((TypeTree?)typeExpression).WithVariables((Expression)variables!);
    }

    // ---- CsLambda ----
    public override J VisitCsLambda(CsLambda csl, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(csl.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var lambdaExpression = q.Receive((J)csl.LambdaExpression, el => (J)VisitNonNull(el, q));
        var returnType = q.Receive((J?)csl.ReturnType, el => (J)VisitNonNull(el!, q));
        var modifiers = q.ReceiveList(csl.Modifiers, m => (Modifier)VisitNonNull(m, q));
        return csl.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists!).WithLambdaExpression((Lambda)lambdaExpression!).WithReturnType((TypeTree?)returnType).WithModifiers(modifiers!);
    }

    // ---- IsPattern ----
    public override J VisitIsPattern(IsPattern ip, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)ip.Expression, el => (J)VisitNonNull(el, q));
        var pattern = q.Receive(ip.Pattern, lp => _delegate.VisitLeftPadded(lp, q));
        return ip.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!).WithPattern(pattern!);
    }

    // ---- StatementExpression ----
    public override J VisitStatementExpression(StatementExpression se, RpcReceiveQueue q)
    {
        var statement = q.Receive((J)se.Statement, el => (J)VisitNonNull(el, q));
        return se.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithStatement((Statement)statement!);
    }

    // ---- SizeOf ----
    public override J VisitSizeOf(SizeOf sizeOf, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)sizeOf.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(sizeOf.Type, t => VisitType(t, q)!);
        return sizeOf.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!).WithType(type);
    }

    // ---- UnsafeStatement ----
    public override J VisitUnsafeStatement(UnsafeStatement unsafeStatement, RpcReceiveQueue q)
    {
        var block = q.Receive((J)unsafeStatement.Block, el => (J)VisitNonNull(el, q));
        return unsafeStatement.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithBlock((Block)block!);
    }

    // ---- PointerType ----
    public override J VisitPointerType(PointerType pointerType, RpcReceiveQueue q)
    {
        var elementType = q.Receive(pointerType.ElementType, rp => VisitRightPadded(rp, q));
        return pointerType.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithElementType(elementType!);
    }

    // ---- FixedStatement ----
    public override J VisitFixedStatement(FixedStatement fixedStatement, RpcReceiveQueue q)
    {
        var declarations = q.Receive((J)fixedStatement.Declarations, el => (J)VisitNonNull(el, q));
        var block = q.Receive((J)fixedStatement.Block, el => (J)VisitNonNull(el, q));
        return fixedStatement.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithDeclarations((ControlParentheses<VariableDeclarations>)declarations!).WithBlock((Block)block!);
    }

    // ---- DefaultExpression ----
    public override J VisitDefaultExpression(DefaultExpression defaultExpression, RpcReceiveQueue q)
    {
        var typeOperator = q.Receive(defaultExpression.TypeOperator, c => VisitContainer(c, q));
        return defaultExpression.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTypeOperator(typeOperator);
    }

    public override J VisitExternAlias(ExternAlias externAlias, RpcReceiveQueue q)
    {
        var identifier = q.Receive(externAlias.Identifier, lp => _delegate.VisitLeftPadded(lp, q));
        return externAlias.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithIdentifier(identifier!);
    }

    public override J VisitInitializerExpression(InitializerExpression initializerExpression, RpcReceiveQueue q)
    {
        var expressions = q.Receive(initializerExpression.Expressions, c => VisitContainer(c, q));
        return initializerExpression.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressions(expressions!);
    }

    public override J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, RpcReceiveQueue q)
    {
        var expression = q.Receive(nullSafeExpression.ExpressionPadded, el => VisitRightPadded(el, q));
        return nullSafeExpression.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressionPadded(expression!);
    }

    // ---- RelationalPattern ----
    public override J VisitRelationalPattern(RelationalPattern rp, RpcReceiveQueue q)
    {
        var op = q.Receive(rp.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var value = q.Receive((J)rp.Value, el => (J)VisitNonNull(el, q));
        return rp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithOperator(op!).WithValue((Expression)value!);
    }

    // ---- PropertyPattern ----
    public override J VisitPropertyPattern(PropertyPattern pp, RpcReceiveQueue q)
    {
        var typeQualifier = q.Receive((J?)pp.TypeQualifier, el => (J)VisitNonNull(el!, q));
        var subpatterns = q.Receive(pp.Subpatterns, c => _delegate.VisitContainer(c, q));
        var designation = q.Receive((J?)pp.Designation, el => (J)VisitNonNull(el!, q));
        return pp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTypeQualifier((TypeTree?)typeQualifier).WithSubpatterns(subpatterns!).WithDesignation((Identifier?)designation);
    }

    // ---- ConstrainedTypeParameter ----
    public override J VisitConstrainedTypeParameter(ConstrainedTypeParameter ctp, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(ctp.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var variance = q.Receive(ctp.Variance, lp => _delegate.VisitLeftPadded(lp!, q));
        var name = q.Receive((J)ctp.Name, el => (J)VisitNonNull(el, q));
        var whereConstraint = q.Receive(ctp.WhereConstraint, lp => _delegate.VisitLeftPadded(lp!, q));
        var constraints = q.Receive(ctp.Constraints, c => VisitContainer(c!, q));
        var type = q.Receive((JavaType?)ctp.Type, t => VisitType(t, q)!);
        return ctp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists!).WithVariance(variance).WithName((Identifier)name!).WithWhereConstraint(whereConstraint).WithConstraints(constraints).WithType(type);
    }

    // ---- InterpolatedString ----
    public override J VisitInterpolatedString(InterpolatedString istr, RpcReceiveQueue q)
    {
        var delimiter = q.Receive(istr.Delimiter);
        var parts = q.ReceiveList(
            istr.Parts
                .Select(p => new JRightPadded<J>(p, Space.Empty, Markers.Empty))
                .ToList(),
            rp => _delegate.VisitRightPadded(rp, q));
        var endDelimiter = q.Receive(istr.EndDelimiter);
        return istr.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithDelimiter(delimiter!).WithEndDelimiter(endDelimiter!).WithParts(parts?.Select(rp => rp.Element).ToList() ?? new List<J>());
    }

    // ---- Interpolation ----
    public override J VisitInterpolation(Interpolation interp, RpcReceiveQueue q)
    {
        var expressionRp = q.Receive(
            new JRightPadded<Expression>(interp.Expression, interp.After, Markers.Empty),
            rp => _delegate.VisitRightPadded(rp, q));
        var alignmentRp = q.Receive(
            interp.Alignment != null
                ? (JRightPadded<Expression>?)new JRightPadded<Expression>(
                    interp.Alignment.Element, Space.Empty, Markers.Empty)
                : null,
            rp => _delegate.VisitRightPadded(rp!, q));
        var formatRp = q.Receive(
            interp.Format != null
                ? (JRightPadded<Expression>?)new JRightPadded<Expression>(
                    interp.Format.Element, Space.Empty, Markers.Empty)
                : null,
            rp => _delegate.VisitRightPadded(rp!, q));
        return interp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression(expressionRp!.Element).WithAfter(expressionRp!.After).WithAlignment(alignmentRp != null ? new JLeftPadded<Expression>(Space.Empty, alignmentRp.Element) : null).WithFormat(formatRp != null ? new JLeftPadded<Identifier>(Space.Empty, (Identifier)formatRp.Element) : null);
    }

    // ---- AwaitExpression ----
    public override J VisitAwaitExpression(AwaitExpression ae, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)ae.Expression, el => (J)VisitNonNull(el, q));
        q.Receive<object?>(null); // type attribution
        return ae.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!);
    }

    // ---- Yield ----
    public override J VisitYield(Yield ys, RpcReceiveQueue q)
    {
        var returnOrBreakKeyword = q.Receive((J)ys.ReturnOrBreakKeyword, el => (J)VisitNonNull(el, q));
        var expression = q.Receive((J?)ys.Expression, el => (J)VisitNonNull(el!, q));
        return ys.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithReturnOrBreakKeyword((Keyword)returnOrBreakKeyword!).WithExpression((Expression?)expression);
    }

    // ---- NamespaceDeclaration ----
    public override J VisitNamespaceDeclaration(NamespaceDeclaration ns, RpcReceiveQueue q)
    {
        var name = q.Receive(ns.Name, rp => _delegate.VisitRightPadded(rp, q));
        // Externs
        q.ReceiveList<JRightPadded<Statement>>([], rp => _delegate.VisitRightPadded(rp, q));
        // Usings
        var usings = q.ReceiveList(
            ns.Members.Where(m => m.Element is UsingDirective).ToList(),
            rp => _delegate.VisitRightPadded(rp, q));
        // Members
        var members = q.ReceiveList(
            ns.Members.Where(m => m.Element is not UsingDirective).ToList(),
            rp => _delegate.VisitRightPadded(rp, q));
        var end = q.Receive(ns.End, space => VisitSpace(space, q));

        var allMembers = new List<JRightPadded<Statement>>();
        if (usings != null) allMembers.AddRange(usings);
        if (members != null) allMembers.AddRange(members);

        return ns.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithName(name!).WithMembers(allMembers).WithEnd(end!);
    }

    // ---- TupleType ----
    public override J VisitTupleType(TupleType tt, RpcReceiveQueue q)
    {
        var elements = q.Receive(tt.Elements, c => _delegate.VisitContainer(c, q));
        q.Receive<object?>(null); // type attribution
        return tt.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithElements(elements!);
    }

    // ---- TupleExpression ----
    public override J VisitTupleExpression(TupleExpression te, RpcReceiveQueue q)
    {
        var arguments = q.Receive(te.Arguments, c => _delegate.VisitContainer(c, q));
        return te.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithArguments(arguments!);
    }

    // ---- ConditionalDirective ----
    public override J VisitConditionalDirective(ConditionalDirective cd, RpcReceiveQueue q)
    {
        // Receive DirectiveLines
        var count = q.Receive<int>(cd.DirectiveLines.Count);
        var directiveLines = new List<DirectiveLine>();
        for (int i = 0; i < count; i++)
        {
            var existing = i < cd.DirectiveLines.Count ? cd.DirectiveLines[i] : null;
            var lineNumber = q.Receive<int>(existing?.LineNumber ?? 0);
            var text = q.Receive<string>(existing?.Text ?? "")!;
            var kind = (PreprocessorDirectiveKind)q.Receive<int>((int)(existing?.Kind ?? 0));
            var groupId = q.Receive<int>(existing?.GroupId ?? 0);
            var activeBranchIndex = q.Receive<int>(existing?.ActiveBranchIndex ?? -1);
            directiveLines.Add(new DirectiveLine(lineNumber, text, kind, groupId, activeBranchIndex));
        }
        // Receive Branches
        var branches = q.ReceiveList(cd.Branches, rp => _delegate.VisitRightPadded(rp, q));
        return cd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithDirectiveLines(directiveLines).WithBranches(branches!);
    }

    // ---- PragmaWarningDirective ----
    public override J VisitPragmaWarningDirective(PragmaWarningDirective pwd, RpcReceiveQueue q)
    {
        var action = q.Receive<object>(pwd.Action);
        var warningCodes = q.ReceiveList(pwd.WarningCodes, rp => _delegate.VisitRightPadded(rp, q));
        return pwd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAction((PragmaWarningAction)action!).WithWarningCodes(warningCodes!);
    }

    // ---- NullableDirective ----
    public override J VisitNullableDirective(NullableDirective nd, RpcReceiveQueue q)
    {
        var setting = q.Receive<object>(nd.Setting);
        var target = q.Receive<object?>(nd.Target);
        var hashSpacing = q.Receive(nd.HashSpacing);
        var trailingComment = q.Receive(nd.TrailingComment);
        return nd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithSetting((NullableSetting)setting!).WithTarget(target != null ? (NullableTarget)target : null);
    }

    // ---- RegionDirective ----
    public override J VisitRegionDirective(RegionDirective rd, RpcReceiveQueue q)
    {
        var name = q.Receive<string?>(rd.Name);
        q.Receive(rd.HashSpacing); // consume to keep queue in sync
        return rd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithName(name);
    }

    // ---- EndRegionDirective ----
    public override J VisitEndRegionDirective(EndRegionDirective erd, RpcReceiveQueue q)
    {
        q.Receive<string?>(erd.Name); // consume to keep queue in sync
        q.Receive(erd.HashSpacing); // consume to keep queue in sync
        return erd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers);
    }

    // ---- DefineDirective ----
    public override J VisitDefineDirective(DefineDirective dd, RpcReceiveQueue q)
    {
        var symbol = q.Receive((J)dd.Symbol, el => (J)VisitNonNull(el, q));
        return dd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithSymbol((Identifier)symbol!);
    }

    // ---- UndefDirective ----
    public override J VisitUndefDirective(UndefDirective ud, RpcReceiveQueue q)
    {
        var symbol = q.Receive((J)ud.Symbol, el => (J)VisitNonNull(el, q));
        return ud.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithSymbol((Identifier)symbol!);
    }

    // ---- ErrorDirective ----
    public override J VisitErrorDirective(ErrorDirective ed, RpcReceiveQueue q)
    {
        var message = q.Receive<string>(ed.Message);
        return ed.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithMessage(message!);
    }

    // ---- WarningDirective ----
    public override J VisitWarningDirective(WarningDirective wd, RpcReceiveQueue q)
    {
        var message = q.Receive<string>(wd.Message);
        return wd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithMessage(message!);
    }

    // ---- LineDirective ----
    public override J VisitLineDirective(LineDirective ld, RpcReceiveQueue q)
    {
        var kind = q.Receive<object>(ld.Kind);
        var line = q.Receive((J?)ld.Line, el => (J)VisitNonNull(el!, q));
        var file = q.Receive((J?)ld.File, el => (J)VisitNonNull(el!, q));
        return ld.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithKind((LineKind)kind!).WithLine((Expression?)line).WithFile((Expression?)file);
    }

    // ---- New types ----

    public override J VisitKeyword(Keyword kw, RpcReceiveQueue q)
    {
        var kind = q.Receive<object>(kw.Kind);
        return kw.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithKind((KeywordKind)kind!);
    }

    public override J VisitNameColon(NameColon nc, RpcReceiveQueue q)
    {
        var name = q.Receive(nc.Name, rp => _delegate.VisitRightPadded(rp, q));
        return nc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithName(name!);
    }

    public override J VisitAnnotatedStatement(AnnotatedStatement ans, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(ans.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var statement = q.Receive((J)ans.Statement, el => (J)VisitNonNull(el, q));
        return ans.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists!).WithStatement((Statement)statement!);
    }

    public override J VisitArrayRankSpecifier(ArrayRankSpecifier ars, RpcReceiveQueue q)
    {
        var sizes = q.Receive(ars.Sizes, c => VisitContainer(c, q));
        return ars.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithSizes(sizes!);
    }

    public override J VisitAssignmentOperation(AssignmentOperation ao, RpcReceiveQueue q)
    {
        var variable = q.Receive((J)ao.Variable, el => (J)VisitNonNull(el, q));
        var op = q.Receive(ao.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var assignmentValue = q.Receive((J)ao.AssignmentValue, el => (J)VisitNonNull(el, q));
        var type = q.Receive(ao.Type, t => VisitType(t, q)!);
        return ao.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithVariable((Expression)variable!).WithOperator(op!).WithAssignmentValue((Expression)assignmentValue!).WithType(type);
    }

    public override J VisitStackAllocExpression(StackAllocExpression sae, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)sae.Expression, el => (J)VisitNonNull(el, q));
        return sae.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((NewArray)expression!);
    }

    public override J VisitGotoStatement(GotoStatement gs, RpcReceiveQueue q)
    {
        var caseOrDefault = q.Receive((J?)gs.CaseOrDefaultKeyword, el => (J)VisitNonNull(el!, q));
        var target = q.Receive((J?)gs.Target, el => (J)VisitNonNull(el!, q));
        return gs.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithCaseOrDefaultKeyword((Keyword?)caseOrDefault).WithTarget((Expression?)target);
    }

    public override J VisitEventDeclaration(EventDeclaration evd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(evd.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(evd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeExpr = q.Receive(evd.TypeExpressionPadded, lp => _delegate.VisitLeftPadded(lp, q));
        var interfaceSpec = q.Receive(evd.InterfaceSpecifier, rp => _delegate.VisitRightPadded(rp!, q));
        var name = q.Receive((J)evd.Name, el => (J)VisitNonNull(el, q));
        var accessors = q.Receive(evd.Accessors, c => VisitContainer(c!, q));
        return evd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists!).WithModifiers(modifiers!).WithTypeExpressionPadded(typeExpr!).WithInterfaceSpecifier(interfaceSpec).WithName((Identifier)name!).WithAccessors(accessors);
    }

    public override J VisitCsBinary(CsBinary csb, RpcReceiveQueue q)
    {
        var left = q.Receive((J)csb.Left, el => (J)VisitNonNull(el, q));
        var op = q.Receive(csb.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var right = q.Receive((J)csb.Right, el => (J)VisitNonNull(el, q));
        var type = q.Receive(csb.Type, t => VisitType(t, q)!);
        return csb.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithLeft((Expression)left!).WithOperator(op!).WithRight((Expression)right!).WithType(type);
    }

    public override J VisitCollectionExpression(CollectionExpression ce, RpcReceiveQueue q)
    {
        var elements = q.ReceiveList(ce.Elements, rp => _delegate.VisitRightPadded(rp, q));
        var type = q.Receive(ce.Type, t => VisitType(t, q)!);
        return ce.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithElements(elements!).WithType(type);
    }

    public override J VisitCsExpressionStatement(CsExpressionStatement ces, RpcReceiveQueue q)
    {
        var exprPadded = q.Receive(ces.ExpressionPadded, rp => _delegate.VisitRightPadded(rp, q));
        return ces.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressionPadded(exprPadded!);
    }

    public override J VisitForEachVariableLoop(ForEachVariableLoop fevl, RpcReceiveQueue q)
    {
        var controlElement = q.Receive((J)fevl.ControlElement, el => (J)VisitNonNull(el, q));
        var body = q.Receive(fevl.Body, rp => _delegate.VisitRightPadded(rp, q));
        return fevl.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithControlElement((ForEachVariableLoopControl)controlElement!).WithBody(body!);
    }

    public override J VisitForEachVariableLoopControl(ForEachVariableLoopControl ctrl, RpcReceiveQueue q)
    {
        var variable = q.Receive(ctrl.Variable, rp => _delegate.VisitRightPadded(rp, q));
        var iterable = q.Receive(ctrl.Iterable, rp => _delegate.VisitRightPadded(rp, q));
        return ctrl.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithVariable(variable!).WithIterable(iterable!);
    }

    public override J VisitCsMethodDeclaration(CsMethodDeclaration cmd, RpcReceiveQueue q)
    {
        var attrs = q.ReceiveList(cmd.Attributes, t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(cmd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeParams = q.Receive(cmd.TypeParameters, c => VisitContainer(c!, q));
        var returnType = q.Receive((J)cmd.ReturnTypeExpression, el => (J)VisitNonNull(el, q));
        var explIntSpec = q.Receive(cmd.ExplicitInterfaceSpecifier, rp => _delegate.VisitRightPadded(rp!, q));
        var name = q.Receive((J)cmd.Name, el => (J)VisitNonNull(el, q));
        var parameters = q.Receive(cmd.Parameters, c => VisitContainer(c, q));
        var body = q.Receive((J?)cmd.Body, el => (J)VisitNonNull(el!, q));
        var methodType = q.Receive((JavaType?)cmd.MethodType, t => VisitType(t, q)!);
        return cmd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributes(attrs!).WithModifiers(modifiers!).WithTypeParameters(typeParams).WithReturnTypeExpression((TypeTree)returnType!).WithExplicitInterfaceSpecifier(explIntSpec).WithName((Identifier)name!).WithParameters(parameters!).WithBody((Statement?)body).WithMethodType((JavaType.Method?)methodType);
    }

    public override J VisitUsingStatement(UsingStatement ust, RpcReceiveQueue q)
    {
        var exprPadded = q.Receive(ust.ExpressionPadded, lp => _delegate.VisitLeftPadded(lp, q));
        var statement = q.Receive((J)ust.Statement, el => (J)VisitNonNull(el, q));
        return ust.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressionPadded(exprPadded!).WithStatement((Statement)statement!);
    }

    public override J VisitAllowsConstraintClause(AllowsConstraintClause acc, RpcReceiveQueue q)
    {
        var expressions = q.Receive(acc.Expressions, c => VisitContainer(c, q));
        return acc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressions(expressions!);
    }

    public override J VisitRefStructConstraint(RefStructConstraint rsc, RpcReceiveQueue q)
    {
        return rsc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers);
    }

    public override J VisitClassOrStructConstraint(ClassOrStructConstraint cosc, RpcReceiveQueue q)
    {
        var kind = q.Receive<object>(cosc.Kind);
        var nullable = q.Receive<bool>(cosc.Nullable);
        return cosc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithKind((ClassOrStructConstraint.TypeKind)kind!).WithNullable(nullable);
    }

    public override J VisitConstructorConstraint(ConstructorConstraint cc, RpcReceiveQueue q)
    {
        return cc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers);
    }

    public override J VisitDefaultConstraint(DefaultConstraint dc, RpcReceiveQueue q)
    {
        return dc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers);
    }

    public override J VisitSingleVariableDesignation(SingleVariableDesignation svd, RpcReceiveQueue q)
    {
        var name = q.Receive((J)svd.Name, el => (J)VisitNonNull(el, q));
        return svd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithName((Identifier)name!);
    }

    public override J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation pvd, RpcReceiveQueue q)
    {
        var variables = q.Receive(pvd.Variables, c => VisitContainer(c, q));
        var type = q.Receive(pvd.Type, t => VisitType(t, q)!);
        return pvd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithVariables(variables!).WithType(type);
    }

    public override J VisitDiscardVariableDesignation(DiscardVariableDesignation dvd, RpcReceiveQueue q)
    {
        var discard = q.Receive((J)dvd.Discard, el => (J)VisitNonNull(el, q));
        return dvd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithDiscard((Identifier)discard!);
    }

    public override J VisitCsUnary(CsUnary csu, RpcReceiveQueue q)
    {
        var op = q.Receive(csu.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var expression = q.Receive((J)csu.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(csu.Type, t => VisitType(t, q)!);
        return csu.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithOperator(op!).WithExpression((Expression)expression!).WithType(type);
    }

    public override J VisitTupleElement(TupleElement tel, RpcReceiveQueue q)
    {
        var elementType = q.Receive((J)tel.ElementType, el => (J)VisitNonNull(el, q));
        var name = q.Receive((J?)tel.Name, el => (J)VisitNonNull(el!, q));
        return tel.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithElementType((TypeTree)elementType!).WithName((Identifier?)name);
    }

    public override J VisitCsNewClass(CsNewClass csnc, RpcReceiveQueue q)
    {
        var newClassCore = q.Receive((J)csnc.NewClassCore, el => (J)VisitNonNull(el, q));
        var initializer = q.Receive((J?)csnc.Initializer, el => (J)VisitNonNull(el!, q));
        return csnc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithNewClassCore((NewClass)newClassCore!).WithInitializer((InitializerExpression?)initializer);
    }

    public override J VisitImplicitElementAccess(ImplicitElementAccess iea, RpcReceiveQueue q)
    {
        var argList = q.Receive(iea.ArgumentList, c => VisitContainer(c, q));
        return iea.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithArgumentList(argList!);
    }

    public override J VisitConstantPattern(ConstantPattern cp, RpcReceiveQueue q)
    {
        var value = q.Receive((J)cp.Value, el => (J)VisitNonNull(el, q));
        return cp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithValue((Expression)value!);
    }

    public override J VisitDiscardPattern(DiscardPattern dp, RpcReceiveQueue q)
    {
        var type = q.Receive(dp.Type, t => VisitType(t, q)!);
        return dp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithType(type);
    }

    public override J VisitListPattern(ListPattern lp, RpcReceiveQueue q)
    {
        var patterns = q.Receive(lp.Patterns, c => VisitContainer(c, q));
        var designation = q.Receive((J?)lp.Designation, el => (J)VisitNonNull(el!, q));
        return lp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithPatterns(patterns!).WithDesignation((VariableDesignation?)designation);
    }

    public override J VisitSlicePattern(SlicePattern sp, RpcReceiveQueue q)
    {
        return sp.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers);
    }

    public override J VisitSwitchExpression(SwitchExpression swe, RpcReceiveQueue q)
    {
        var expr = q.Receive(swe.ExpressionPadded, rp => _delegate.VisitRightPadded(rp, q));
        var arms = q.Receive(swe.Arms, c => VisitContainer(c, q));
        return swe.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressionPadded(expr!).WithArms(arms!);
    }

    public override J VisitSwitchExpressionArm(SwitchExpressionArm swea, RpcReceiveQueue q)
    {
        var pattern = q.Receive((J)swea.Pattern, el => (J)VisitNonNull(el, q));
        var whenExpr = q.Receive(swea.WhenExpression, lp => _delegate.VisitLeftPadded(lp!, q));
        var exprPadded = q.Receive(swea.ExpressionPadded, lp => _delegate.VisitLeftPadded(lp, q));
        return swea.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithPattern((J)pattern!).WithWhenExpression(whenExpr).WithExpressionPadded(exprPadded!);
    }

    public override J VisitCheckedExpression(CheckedExpression che, RpcReceiveQueue q)
    {
        var keyword = q.Receive((J)che.CheckedOrUncheckedKeyword, el => (J)VisitNonNull(el, q));
        var exprValue = q.Receive((J)che.ExpressionValue, el => (J)VisitNonNull(el, q));
        return che.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithCheckedOrUncheckedKeyword((Keyword)keyword!).WithExpressionValue((ControlParentheses<Expression>)exprValue!);
    }

    public override J VisitCheckedStatement(CheckedStatement chs, RpcReceiveQueue q)
    {
        var keyword = q.Receive((J)chs.KeywordValue, el => (J)VisitNonNull(el, q));
        var block = q.Receive((J)chs.Block, el => (J)VisitNonNull(el, q));
        return chs.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithKeywordValue((Keyword)keyword!).WithBlock((Block)block!);
    }

    public override J VisitRangeExpression(RangeExpression rang, RpcReceiveQueue q)
    {
        var start = q.Receive(rang.Start, rp => _delegate.VisitRightPadded(rp!, q));
        var end = q.Receive((J?)rang.End, el => (J)VisitNonNull(el!, q));
        return rang.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithStart(start).WithEnd((Expression?)end);
    }

    public override J VisitIndexerDeclaration(IndexerDeclaration idxd, RpcReceiveQueue q)
    {
        var modifiers = q.ReceiveList(idxd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeExpr = q.Receive((J)idxd.TypeExpression, el => (J)VisitNonNull(el, q));
        var explIntSpec = q.Receive(idxd.ExplicitInterfaceSpecifier, rp => _delegate.VisitRightPadded(rp!, q));
        var indexer = q.Receive((J)idxd.Indexer, el => (J)VisitNonNull(el, q));
        var parameters = q.Receive(idxd.Parameters, c => VisitContainer(c, q));
        var exprBody = q.Receive(idxd.ExpressionBody, lp => _delegate.VisitLeftPadded(lp!, q));
        var accessors = q.Receive((J?)idxd.Accessors, el => (J)VisitNonNull(el!, q));
        return idxd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithModifiers(modifiers!).WithTypeExpression((TypeTree)typeExpr!).WithExplicitInterfaceSpecifier(explIntSpec).WithIndexer((Expression)indexer!).WithParameters(parameters!).WithExpressionBody(exprBody).WithAccessors((Block?)accessors);
    }

    public override J VisitDelegateDeclaration(DelegateDeclaration deld, RpcReceiveQueue q)
    {
        var attrs = q.ReceiveList(deld.Attributes, t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(deld.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var returnType = q.Receive(deld.ReturnType, lp => _delegate.VisitLeftPadded(lp, q));
        var name = q.Receive((J)deld.IdentifierName, el => (J)VisitNonNull(el, q));
        var typeParams = q.Receive(deld.TypeParameters, c => VisitContainer(c!, q));
        var parameters = q.Receive(deld.Parameters, c => VisitContainer(c, q));
        return deld.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributes(attrs!).WithModifiers(modifiers!).WithReturnType(returnType!).WithIdentifierName((Identifier)name!).WithTypeParameters(typeParams).WithParameters(parameters!);
    }

    public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration cod, RpcReceiveQueue q)
    {
        var modifiers = q.ReceiveList(cod.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var kind = q.Receive(cod.Kind, lp => _delegate.VisitLeftPadded(lp, q));
        var returnType = q.Receive(cod.ReturnType, lp => _delegate.VisitLeftPadded(lp, q));
        var parameters = q.Receive(cod.Parameters, c => VisitContainer(c, q));
        var exprBody = q.Receive(cod.ExpressionBody, lp => _delegate.VisitLeftPadded(lp!, q));
        var body = q.Receive((J?)cod.Body, el => (J)VisitNonNull(el!, q));
        return cod.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithModifiers(modifiers!).WithKind(kind!).WithReturnType(returnType!).WithParameters(parameters!).WithExpressionBody(exprBody).WithBody((Block?)body);
    }

    public override J VisitOperatorDeclaration(OperatorDeclaration opd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(opd.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(opd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var explIntSpec = q.Receive(opd.ExplicitInterfaceSpecifier, rp => _delegate.VisitRightPadded(rp!, q));
        var operatorKw = q.Receive((J)opd.OperatorKeyword, el => (J)VisitNonNull(el, q));
        var checkedKw = q.Receive((J?)opd.CheckedKeyword, el => (J)VisitNonNull(el!, q));
        var operatorToken = q.Receive(opd.OperatorToken, lp => _delegate.VisitLeftPadded(lp, q));
        var returnType = q.Receive((J)opd.ReturnType, el => (J)VisitNonNull(el, q));
        var parameters = q.Receive(opd.Parameters, c => VisitContainer(c, q));
        var body = q.Receive((J)opd.Body, el => (J)VisitNonNull(el, q));
        var methodType = q.Receive((JavaType?)opd.MethodType, t => VisitType(t, q)!);
        return opd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists!).WithModifiers(modifiers!).WithExplicitInterfaceSpecifier(explIntSpec).WithOperatorKeyword((Keyword)operatorKw!).WithCheckedKeyword((Keyword?)checkedKw).WithOperatorToken(operatorToken!).WithReturnType((TypeTree)returnType!).WithParameters(parameters!).WithBody((Block)body!).WithMethodType((JavaType.Method?)methodType);
    }

    public override J VisitEnumDeclaration(EnumDeclaration enumd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(enumd.AttributeLists ?? new List<AttributeList>(),
            t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(enumd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var namePadded = q.Receive(enumd.NamePadded, lp => _delegate.VisitLeftPadded(lp, q));
        var baseType = q.Receive(enumd.BaseType, lp => _delegate.VisitLeftPadded(lp!, q));
        var members = q.Receive(enumd.Members, c => VisitContainer(c!, q));
        return enumd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists).WithModifiers(modifiers!).WithNamePadded(namePadded!).WithBaseType(baseType).WithMembers(members);
    }

    public override J VisitEnumMemberDeclaration(EnumMemberDeclaration enummd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(enummd.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var name = q.Receive((J)enummd.Name, el => (J)VisitNonNull(el, q));
        var initializer = q.Receive(enummd.Initializer, lp => _delegate.VisitLeftPadded(lp!, q));
        return enummd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAttributeLists(attrLists!).WithName((Identifier)name!).WithInitializer(initializer);
    }

    public override J VisitAliasQualifiedName(AliasQualifiedName aqn, RpcReceiveQueue q)
    {
        var alias = q.Receive(aqn.Alias, rp => _delegate.VisitRightPadded(rp, q));
        var name = q.Receive((J)aqn.Name, el => (J)VisitNonNull(el, q));
        return aqn.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithAlias(alias!).WithName((Expression)name!);
    }

    public override J VisitCsArrayType(CsArrayType csat, RpcReceiveQueue q)
    {
        var typeExpr = q.Receive((J?)csat.TypeExpression, el => (J)VisitNonNull(el!, q));
        var dimensions = q.ReceiveList(csat.Dimensions,
            d => (ArrayDimension)VisitNonNull(d, q));
        var type = q.Receive(csat.Type, t => VisitType(t, q)!);
        return csat.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTypeExpression((TypeTree?)typeExpr).WithDimensions(dimensions!).WithType(type);
    }

    public override J VisitCsTry(CsTry cstry, RpcReceiveQueue q)
    {
        var body = q.Receive((J)cstry.Body, el => (J)VisitNonNull(el, q));
        var catches = q.ReceiveList(cstry.Catches, c => (CsTryCatch)VisitNonNull(c, q));
        var finallyBlock = q.Receive(cstry.Finally, lp => _delegate.VisitLeftPadded(lp!, q));
        return cstry.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithBody((Block)body!).WithCatches(catches!).WithFinally(finallyBlock);
    }

    public override J VisitCsTryCatch(CsTryCatch cstc, RpcReceiveQueue q)
    {
        var parameter = q.Receive((J)cstc.Parameter, el => (J)VisitNonNull(el, q));
        var filterExpr = q.Receive(cstc.FilterExpression, lp => _delegate.VisitLeftPadded(lp!, q));
        var body = q.Receive((J)cstc.Body, el => (J)VisitNonNull(el, q));
        return cstc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithParameter((ControlParentheses<VariableDeclarations>)parameter!).WithFilterExpression(filterExpr).WithBody((Block)body!);
    }

    public override J VisitPointerDereference(PointerDereference pd, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)pd.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(pd.Type, t => VisitType(t, q)!);
        return pd.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!).WithType(type);
    }

    public override J VisitPointerFieldAccess(PointerFieldAccess pfa, RpcReceiveQueue q)
    {
        var target = q.Receive((J)pfa.Target, el => (J)VisitNonNull(el, q));
        var namePadded = q.Receive(pfa.NamePadded, lp => _delegate.VisitLeftPadded(lp, q));
        var type = q.Receive(pfa.Type, t => VisitType(t, q)!);
        return pfa.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTarget((Expression)target!).WithNamePadded(namePadded!).WithType(type);
    }

    public override J VisitRefType(RefType rt, RpcReceiveQueue q)
    {
        var readonlyKw = q.Receive((J?)rt.ReadonlyKeyword, el => (J)VisitNonNull(el!, q));
        var typeIdentifier = q.Receive((J)rt.TypeIdentifier, el => (J)VisitNonNull(el, q));
        var type = q.Receive(rt.Type, t => VisitType(t, q)!);
        return rt.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithReadonlyKeyword((Modifier?)readonlyKw).WithTypeIdentifier((TypeTree)typeIdentifier!).WithType(type);
    }

    public override J VisitAnonymousObjectCreationExpression(AnonymousObjectCreationExpression aoce, RpcReceiveQueue q)
    {
        var initializers = q.Receive(aoce.Initializers, el => _delegate.VisitContainer(el, q));
        var type = q.Receive(aoce.Type, t => VisitType(t, q)!);
        return aoce.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithInitializers(initializers!).WithType(type);
    }

    public override J VisitWithExpression(WithExpression we, RpcReceiveQueue q)
    {
        var target = q.Receive((J)we.Target, el => (J)VisitNonNull(el, q));
        var initializer = q.Receive(we.InitializerPadded, lp => _delegate.VisitLeftPadded(lp, q));
        var type = q.Receive(we.Type, t => VisitType(t, q)!);
        return we.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTarget((Expression)target!).WithInitializerPadded(initializer!).WithType(type);
    }

    public override J VisitSpreadExpression(SpreadExpression se, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)se.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(se.Type, t => VisitType(t, q)!);
        return se.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!).WithType(type);
    }

    public override J VisitFunctionPointerType(FunctionPointerType fpt, RpcReceiveQueue q)
    {
        var callingConvention = q.Receive(fpt.CallingConvention, lp => _delegate.VisitLeftPadded(lp!, q));
        var unmanagedConventionTypes = q.Receive(fpt.UnmanagedCallingConventionTypes, el => _delegate.VisitContainer(el!, q));
        var parameterTypes = q.Receive(fpt.ParameterTypes, el => _delegate.VisitContainer(el, q));
        var type = q.Receive(fpt.Type, t => VisitType(t, q)!);
        return fpt.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithCallingConvention(callingConvention).WithUnmanagedCallingConventionTypes(unmanagedConventionTypes).WithParameterTypes(parameterTypes!).WithType(type);
    }

    // ---- LINQ ----

    public override J VisitQueryExpression(QueryExpression qe, RpcReceiveQueue q)
    {
        var fromClause = q.Receive((J)qe.FromClause, el => (J)VisitNonNull(el, q));
        var body = q.Receive((J)qe.Body, el => (J)VisitNonNull(el, q));
        return qe.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithFromClause((FromClause)fromClause!).WithBody((QueryBody)body!);
    }

    public override J VisitQueryBody(QueryBody qb, RpcReceiveQueue q)
    {
        var clauses = q.ReceiveList(qb.Clauses.Cast<QueryClause>().ToList(),
            el => (QueryClause)VisitNonNull(el, q));
        var selectOrGroup = q.Receive((J?)qb.SelectOrGroup, el => (J)VisitNonNull(el!, q));
        var continuation = q.Receive((J?)qb.Continuation, el => (J)VisitNonNull(el!, q));
        return qb.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithClauses(clauses?.Cast<QueryClause>().ToList() ?? new List<QueryClause>()).WithSelectOrGroup((SelectOrGroupClause?)selectOrGroup).WithContinuation((QueryContinuation?)continuation);
    }

    public override J VisitFromClause(FromClause fc, RpcReceiveQueue q)
    {
        var typeIdentifier = q.Receive((J?)fc.TypeIdentifier, el => (J)VisitNonNull(el!, q));
        var identifier = q.Receive(fc.IdentifierPadded, rp => _delegate.VisitRightPadded(rp, q));
        var expression = q.Receive((J)fc.Expression, el => (J)VisitNonNull(el, q));
        return fc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithTypeIdentifier((TypeTree?)typeIdentifier).WithIdentifierPadded(identifier!).WithExpression((Expression)expression!);
    }

    public override J VisitLetClause(LetClause lc, RpcReceiveQueue q)
    {
        var identifier = q.Receive(lc.IdentifierPadded, rp => _delegate.VisitRightPadded(rp, q));
        var expression = q.Receive((J)lc.Expression, el => (J)VisitNonNull(el, q));
        return lc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithIdentifierPadded(identifier!).WithExpression((Expression)expression!);
    }

    public override J VisitJoinClause(JoinClause jc, RpcReceiveQueue q)
    {
        var identifier = q.Receive(jc.IdentifierPadded, rp => _delegate.VisitRightPadded(rp, q));
        var inExpression = q.Receive(jc.InExpression, rp => _delegate.VisitRightPadded(rp, q));
        var leftExpression = q.Receive(jc.LeftExpression, rp => _delegate.VisitRightPadded(rp, q));
        var rightExpression = q.Receive((J)jc.RightExpression, el => (J)VisitNonNull(el, q));
        var into = q.Receive(jc.Into, lp => _delegate.VisitLeftPadded(lp!, q));
        return jc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithIdentifierPadded(identifier!).WithInExpression(inExpression!).WithLeftExpression(leftExpression!).WithRightExpression((Expression)rightExpression!).WithInto(into);
    }

    public override J VisitJoinIntoClause(JoinIntoClause jic, RpcReceiveQueue q)
    {
        var identifier = q.Receive((J)jic.Identifier, el => (J)VisitNonNull(el, q));
        return jic.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithIdentifier((Identifier)identifier!);
    }

    public override J VisitWhereClause(WhereClause wc, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)wc.Condition, el => (J)VisitNonNull(el, q));
        return wc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithCondition((Expression)condition!);
    }

    public override J VisitOrderByClause(OrderByClause obc, RpcReceiveQueue q)
    {
        var orderings = q.ReceiveList(obc.Orderings, rp => _delegate.VisitRightPadded(rp, q));
        return obc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithOrderings(orderings!);
    }

    public override J VisitOrdering(Ordering ord, RpcReceiveQueue q)
    {
        var expression = q.Receive(ord.ExpressionPadded, rp => _delegate.VisitRightPadded(rp, q));
        var direction = q.Receive<object?>(ord.Direction);
        return ord.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpressionPadded(expression!).WithDirection(direction != null ? (DirectionKind)direction : null);
    }

    public override J VisitSelectClause(SelectClause sc, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)sc.Expression, el => (J)VisitNonNull(el, q));
        return sc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithExpression((Expression)expression!);
    }

    public override J VisitGroupClause(GroupClause gc, RpcReceiveQueue q)
    {
        var groupExpression = q.Receive(gc.GroupExpression, rp => _delegate.VisitRightPadded(rp, q));
        var key = q.Receive((J)gc.Key, el => (J)VisitNonNull(el, q));
        return gc.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithGroupExpression(groupExpression!).WithKey((Expression)key!);
    }

    public override J VisitQueryContinuation(QueryContinuation qcont, RpcReceiveQueue q)
    {
        var identifier = q.Receive((J)qcont.Identifier, el => (J)VisitNonNull(el, q));
        var body = q.Receive((J)qcont.Body, el => (J)VisitNonNull(el, q));
        return qcont.WithId(PvId).WithPrefix(PvPrefix).WithMarkers(PvMarkers).WithIdentifier((Identifier)identifier!).WithBody((QueryBody)body!);
    }

    // ---- Helper delegation to JavaReceiver ----

    public JLeftPadded<T> VisitLeftPadded<T>(JLeftPadded<T> left, RpcReceiveQueue q) =>
        _delegate.VisitLeftPadded(left, q);

    public JRightPadded<T> VisitRightPadded<T>(JRightPadded<T> right, RpcReceiveQueue q) =>
        _delegate.VisitRightPadded(right, q);

    public JContainer<TJ> VisitContainer<TJ>(JContainer<TJ> container, RpcReceiveQueue q) where TJ : J =>
        _delegate.VisitContainer(container, q);

    public Space VisitSpace(Space space, RpcReceiveQueue q) =>
        _delegate.VisitSpace(space, q);

    public JavaType? VisitType(JavaType? javaType, RpcReceiveQueue q) =>
        _delegate.VisitType(javaType, q);

    // ---- Private helpers ----

    private J VisitNonNull(J tree, RpcReceiveQueue q) => Visit(tree, q)!;

    private static string DeriveEndDelimiter(string startDelimiter)
    {
        if (startDelimiter.Contains("\"\"\""))
            return "\"\"\"";
        return "\"";
    }

    // ---- Inner delegate class ----

    private class CSharpReceiverDelegate : JavaReceiver
    {
        private readonly CSharpReceiver _outer;

        public CSharpReceiverDelegate(CSharpReceiver outer)
        {
            _outer = outer;
        }

        // Expose PreVisit data to CSharpReceiver
        public Guid PvId => _pvId;
        public Space PvPrefix => _pvPrefix;
        public Markers PvMarkers => _pvMarkers;

        // Make ConsumePreVisit/PopPreVisit accessible to CSharpReceiver
        public new void ConsumePreVisit(J j, RpcReceiveQueue q) => base.ConsumePreVisit(j, q);
        public new void PopPreVisit() => base.PopPreVisit();

        public override J? Visit(Tree? tree, RpcReceiveQueue q)
        {
            if (tree is Cs || tree is ExpressionStatement)
            {
                return _outer.Visit(tree, q);
            }
            return base.Visit(tree, q);
        }
    }
}
