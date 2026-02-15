using Rewrite.Core;
using Rewrite.Core.Rpc;
using Rewrite.Java;
using Rewrite.Java.Rpc;

namespace Rewrite.CSharp.Rpc;

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
            return es with { Id = pvId, Expression = rp!.Element };
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
            ConditionalBlock cb => VisitConditionalBlock(cb, q),
            IfDirective ifd => VisitIfDirective(ifd, q),
            ElifDirective elif => VisitElifDirective(elif, q),
            ElseDirective elsed => VisitElseDirective(elsed, q),
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
            CsArgument csa => VisitCsArgument(csa, q),
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
            UnaryPattern up => VisitUnaryPattern(up, q),
            TypePattern tp => VisitTypePattern(tp, q),
            BinaryPattern bp => VisitBinaryPattern(bp, q),
            ConstantPattern cp => VisitConstantPattern(cp, q),
            DiscardPattern dp => VisitDiscardPattern(dp, q),
            ListPattern lp => VisitListPattern(lp, q),
            ParenthesizedPattern pap => VisitParenthesizedPattern(pap, q),
            RecursivePattern rcp => VisitRecursivePattern(rcp, q),
            VarPattern vp => VisitVarPattern(vp, q),
            PositionalPatternClause ppc => VisitPositionalPatternClause(ppc, q),
            SlicePattern sp => VisitSlicePattern(sp, q),
            PropertyPatternClause ppclause => VisitPropertyPatternClause(ppclause, q),
            Subpattern sub => VisitSubpattern(sub, q),
            SwitchExpression swe => VisitSwitchExpression(swe, q),
            SwitchExpressionArm swea => VisitSwitchExpressionArm(swea, q),
            SwitchSection ss => VisitSwitchSection(ss, q),
            DefaultSwitchLabel dsl => VisitDefaultSwitchLabel(dsl, q),
            CasePatternSwitchLabel cpsl => VisitCasePatternSwitchLabel(cpsl, q),
            SwitchStatement sws => VisitSwitchStatement(sws, q),
            LockStatement ls => VisitLockStatement(ls, q),
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
            PointerFieldAccess pfa => VisitPointerFieldAccess(pfa, q),
            RefType rt => VisitRefType(rt, q),
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
        if (usings != null) allMembers.AddRange(usings.Select(rp => rp.Element));
        if (attrLists != null) allMembers.AddRange(attrLists);
        if (members != null) allMembers.AddRange(members.Select(rp => rp.Element));

        return cu with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            SourcePath = sourcePath!,
            Members = allMembers,
            Eof = eof!
        };
    }

    // ---- UsingDirective ----
    public override J VisitUsingDirective(UsingDirective ud, RpcReceiveQueue q)
    {
        q.Receive(ud.IsGlobal ? (object?)ud.Global : null);
        q.Receive(ud.IsStatic ? (object?)ud.Static : null);
        // Unsafe: nagoya doesn't model this
        q.Receive<object?>(null);
        var alias = q.Receive(ud.Alias, rp => _delegate.VisitRightPadded(rp!, q));
        var namespaceOrType = q.Receive((J)ud.NamespaceOrType, el => (J)VisitNonNull(el, q));
        return ud with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Alias = alias,
            NamespaceOrType = (TypeTree)namespaceOrType!
        };
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
        return pd with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Modifiers = modifiers!,
            TypeExpression = (TypeTree)typeExpression!,
            Name = (Identifier)name!,
            Accessors = (Block?)accessors,
            ExpressionBody = expressionBody
        };
    }

    // ---- AccessorDeclaration ----
    public override J VisitAccessorDeclaration(AccessorDeclaration ad, RpcReceiveQueue q)
    {
        q.ReceiveList<AttributeList>([], t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(ad.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var kind = q.Receive(ad.Kind, lp => _delegate.VisitLeftPadded(lp, q));
        var expressionBody = q.Receive(ad.ExpressionBody, lp => _delegate.VisitLeftPadded(lp!, q));
        var body = q.Receive((J?)ad.Body, el => (J)VisitNonNull(el!, q));
        return ad with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Modifiers = modifiers!,
            Kind = kind,
            ExpressionBody = expressionBody,
            Body = (Block?)body
        };
    }

    // ---- AttributeList ----
    public override J VisitAttributeList(AttributeList al, RpcReceiveQueue q)
    {
        var target = q.Receive(al.Target, rp => _delegate.VisitRightPadded(rp!, q));
        var attributes = q.ReceiveList(al.Attributes, rp => _delegate.VisitRightPadded(rp, q));
        return al with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Target = target,
            Attributes = attributes!
        };
    }

    // ---- NamedExpression ----
    public override J VisitNamedExpression(NamedExpression ne, RpcReceiveQueue q)
    {
        var name = q.Receive(ne.Name, rp => _delegate.VisitRightPadded(rp, q));
        var expression = q.Receive((J)ne.Expression, el => (J)VisitNonNull(el, q));
        return ne with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = name,
            Expression = (Expression)expression!
        };
    }

    // ---- RefExpression ----
    public override J VisitRefExpression(RefExpression re, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)re.Expression, el => (J)VisitNonNull(el, q));
        return re with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = (Expression)expression! };
    }

    // ---- DeclarationExpression ----
    public override J VisitDeclarationExpression(DeclarationExpression de, RpcReceiveQueue q)
    {
        var type = q.Receive((J)de.Type, el => (J)VisitNonNull(el, q));
        var variable = q.Receive((J)de.Variable.Element, el => (J)VisitNonNull(el, q));
        return de with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Type = (TypeTree)type!,
            Variable = de.Variable with { Element = (Identifier)variable! }
        };
    }

    // ---- CsLambda ----
    public override J VisitCsLambda(CsLambda csl, RpcReceiveQueue q)
    {
        var lambdaExpression = q.Receive((J)csl.LambdaExpression, el => (J)VisitNonNull(el, q));
        var returnType = q.Receive((J?)csl.ReturnType, el => (J)VisitNonNull(el!, q));
        var modifiers = q.ReceiveList(csl.Modifiers, m => (Modifier)VisitNonNull(m, q));
        return csl with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            LambdaExpression = (Lambda)lambdaExpression!,
            ReturnType = (TypeTree?)returnType,
            Modifiers = modifiers!
        };
    }

    // ---- IsPattern ----
    public override J VisitIsPattern(IsPattern ip, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)ip.Expression, el => (J)VisitNonNull(el, q));
        var pattern = q.Receive(ip.Pattern, lp => _delegate.VisitLeftPadded(lp, q));
        return ip with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = (Expression)expression!,
            Pattern = pattern
        };
    }

    // ---- StatementExpression ----
    public override J VisitStatementExpression(StatementExpression se, RpcReceiveQueue q)
    {
        var statement = q.Receive((J)se.Statement, el => (J)VisitNonNull(el, q));
        return se with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Statement = (Statement)statement! };
    }

    // ---- SizeOf ----
    public override J VisitSizeOf(SizeOf sizeOf, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)sizeOf.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(sizeOf.Type, t => VisitType(t, q)!);
        return sizeOf with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = (Expression)expression!, Type = type };
    }

    // ---- UnsafeStatement ----
    public override J VisitUnsafeStatement(UnsafeStatement unsafeStatement, RpcReceiveQueue q)
    {
        var block = q.Receive((J)unsafeStatement.Block, el => (J)VisitNonNull(el, q));
        return unsafeStatement with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Block = (Block)block! };
    }

    // ---- PointerType ----
    public override J VisitPointerType(PointerType pointerType, RpcReceiveQueue q)
    {
        var elementType = q.Receive(pointerType.ElementType, rp => VisitRightPadded(rp, q));
        return pointerType with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ElementType = elementType! };
    }

    // ---- FixedStatement ----
    public override J VisitFixedStatement(FixedStatement fixedStatement, RpcReceiveQueue q)
    {
        var declarations = q.Receive((J)fixedStatement.Declarations, el => (J)VisitNonNull(el, q));
        var block = q.Receive((J)fixedStatement.Block, el => (J)VisitNonNull(el, q));
        return fixedStatement with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Declarations = (ControlParentheses<VariableDeclarations>)declarations!, Block = (Block)block! };
    }

    // ---- DefaultExpression ----
    public override J VisitDefaultExpression(DefaultExpression defaultExpression, RpcReceiveQueue q)
    {
        var typeOperator = q.Receive(defaultExpression.TypeOperator, c => VisitContainer(c, q));
        return defaultExpression with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            TypeOperator = typeOperator };
    }

    public override J VisitExternAlias(ExternAlias externAlias, RpcReceiveQueue q)
    {
        var identifier = q.Receive(externAlias.Identifier, lp => _delegate.VisitLeftPadded(lp, q));
        return externAlias with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Identifier = identifier! };
    }

    public override J VisitInitializerExpression(InitializerExpression initializerExpression, RpcReceiveQueue q)
    {
        var expressions = q.Receive(initializerExpression.Expressions, c => VisitContainer(c, q));
        return initializerExpression with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expressions = expressions! };
    }

    public override J VisitNullSafeExpression(NullSafeExpression nullSafeExpression, RpcReceiveQueue q)
    {
        var expression = q.Receive(nullSafeExpression.ExpressionPadded, el => VisitRightPadded(el, q));
        return nullSafeExpression with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ExpressionPadded = expression! };
    }

    // ---- RelationalPattern ----
    public override J VisitRelationalPattern(RelationalPattern rp, RpcReceiveQueue q)
    {
        var op = q.Receive(rp.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var value = q.Receive((J)rp.Value, el => (J)VisitNonNull(el, q));
        return rp with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Operator = op,
            Value = (Expression)value!
        };
    }

    // ---- PropertyPattern ----
    public override J VisitPropertyPattern(PropertyPattern pp, RpcReceiveQueue q)
    {
        var typeQualifier = q.Receive((J?)pp.TypeQualifier, el => (J)VisitNonNull(el!, q));
        var subpatterns = q.Receive(pp.Subpatterns, c => _delegate.VisitContainer(c, q));
        return pp with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            TypeQualifier = (TypeTree?)typeQualifier,
            Subpatterns = subpatterns
        };
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
        return ctp with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AttributeLists = attrLists!,
            Variance = variance,
            Name = (Identifier)name!,
            WhereConstraint = whereConstraint,
            Constraints = constraints,
            Type = type
        };
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
        q.Receive(DeriveEndDelimiter(istr.Delimiter));
        return istr with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Delimiter = delimiter!,
            Parts = parts?.Select(rp => rp.Element).ToList() ?? new List<J>()
        };
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
        return interp with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = expressionRp!.Element,
            After = expressionRp!.After,
            Alignment = alignmentRp != null
                ? new JLeftPadded<Expression>(Space.Empty, alignmentRp.Element)
                : null,
            Format = formatRp != null
                ? new JLeftPadded<Identifier>(Space.Empty, (Identifier)formatRp.Element)
                : null
        };
    }

    // ---- AwaitExpression ----
    public override J VisitAwaitExpression(AwaitExpression ae, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)ae.Expression, el => (J)VisitNonNull(el, q));
        q.Receive<object?>(null); // type attribution
        return ae with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = (Expression)expression! };
    }

    // ---- Yield ----
    public override J VisitYield(Yield ys, RpcReceiveQueue q)
    {
        var returnOrBreakKeyword = q.Receive((J)ys.ReturnOrBreakKeyword, el => (J)VisitNonNull(el, q));
        var expression = q.Receive((J?)ys.Expression, el => (J)VisitNonNull(el!, q));
        return ys with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ReturnOrBreakKeyword = (Keyword)returnOrBreakKeyword!,
            Expression = (Expression?)expression
        };
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

        return ns with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = name,
            Members = allMembers,
            End = end!
        };
    }

    // ---- TupleType ----
    public override J VisitTupleType(TupleType tt, RpcReceiveQueue q)
    {
        var elements = q.Receive(tt.Elements, c => _delegate.VisitContainer(c, q));
        q.Receive<object?>(null); // type attribution
        return tt with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Elements = elements };
    }

    // ---- TupleExpression ----
    public override J VisitTupleExpression(TupleExpression te, RpcReceiveQueue q)
    {
        var arguments = q.Receive(te.Arguments, c => _delegate.VisitContainer(c, q));
        return te with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Arguments = arguments };
    }

    // ---- ConditionalBlock ----
    public override J VisitConditionalBlock(ConditionalBlock cb, RpcReceiveQueue q)
    {
        var ifBranch = q.Receive((J)cb.IfBranch, el => (J)VisitNonNull(el, q));
        var elifBranches = q.ReceiveList(
            cb.ElifBranches.Cast<ElifDirective>().ToList(),
            e => (ElifDirective)VisitNonNull(e, q));
        var elseBranch = q.Receive((J?)cb.ElseBranch, el => (J)VisitNonNull(el!, q));
        var beforeEndif = q.Receive(cb.BeforeEndif, space => VisitSpace(space, q));
        return cb with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            IfBranch = (IfDirective)ifBranch!,
            ElifBranches = elifBranches?.Cast<ElifDirective>().ToList() ?? new List<ElifDirective>(),
            ElseBranch = (ElseDirective?)elseBranch,
            BeforeEndif = beforeEndif!
        };
    }

    // ---- IfDirective ----
    public override J VisitIfDirective(IfDirective ifd, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)ifd.Condition, el => (J)VisitNonNull(el, q));
        var branchTaken = q.Receive<bool>(ifd.BranchTaken);
        var body = q.ReceiveList(ifd.Body, rp => _delegate.VisitRightPadded(rp, q));
        return ifd with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Condition = (Expression)condition!,
            BranchTaken = branchTaken,
            Body = body!
        };
    }

    // ---- ElifDirective ----
    public override J VisitElifDirective(ElifDirective elif, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)elif.Condition, el => (J)VisitNonNull(el, q));
        var branchTaken = q.Receive<bool>(elif.BranchTaken);
        var body = q.ReceiveList(elif.Body, rp => _delegate.VisitRightPadded(rp, q));
        return elif with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Condition = (Expression)condition!,
            BranchTaken = branchTaken,
            Body = body!
        };
    }

    // ---- ElseDirective ----
    public override J VisitElseDirective(ElseDirective elsed, RpcReceiveQueue q)
    {
        var branchTaken = q.Receive<bool>(elsed.BranchTaken);
        var body = q.ReceiveList(elsed.Body, rp => _delegate.VisitRightPadded(rp, q));
        return elsed with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            BranchTaken = branchTaken,
            Body = body!
        };
    }

    // ---- PragmaWarningDirective ----
    public override J VisitPragmaWarningDirective(PragmaWarningDirective pwd, RpcReceiveQueue q)
    {
        var action = q.Receive<object>(pwd.Action);
        var warningCodes = q.ReceiveList(pwd.WarningCodes, rp => _delegate.VisitRightPadded(rp, q));
        return pwd with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Action = (PragmaWarningAction)action!,
            WarningCodes = warningCodes!
        };
    }

    // ---- NullableDirective ----
    public override J VisitNullableDirective(NullableDirective nd, RpcReceiveQueue q)
    {
        var setting = q.Receive<object>(nd.Setting);
        var target = q.Receive<object?>(nd.Target);
        return nd with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Setting = (NullableSetting)setting!,
            Target = target != null ? (NullableTarget)target : null
        };
    }

    // ---- RegionDirective ----
    public override J VisitRegionDirective(RegionDirective rd, RpcReceiveQueue q)
    {
        var name = q.Receive<string?>(rd.Name);
        return rd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = name };
    }

    // ---- EndRegionDirective ----
    public override J VisitEndRegionDirective(EndRegionDirective erd, RpcReceiveQueue q)
    {
        return erd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers };
    }

    // ---- DefineDirective ----
    public override J VisitDefineDirective(DefineDirective dd, RpcReceiveQueue q)
    {
        var symbol = q.Receive((J)dd.Symbol, el => (J)VisitNonNull(el, q));
        return dd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Symbol = (Identifier)symbol! };
    }

    // ---- UndefDirective ----
    public override J VisitUndefDirective(UndefDirective ud, RpcReceiveQueue q)
    {
        var symbol = q.Receive((J)ud.Symbol, el => (J)VisitNonNull(el, q));
        return ud with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Symbol = (Identifier)symbol! };
    }

    // ---- ErrorDirective ----
    public override J VisitErrorDirective(ErrorDirective ed, RpcReceiveQueue q)
    {
        var message = q.Receive<string>(ed.Message);
        return ed with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Message = message! };
    }

    // ---- WarningDirective ----
    public override J VisitWarningDirective(WarningDirective wd, RpcReceiveQueue q)
    {
        var message = q.Receive<string>(wd.Message);
        return wd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Message = message! };
    }

    // ---- LineDirective ----
    public override J VisitLineDirective(LineDirective ld, RpcReceiveQueue q)
    {
        var kind = q.Receive<object>(ld.Kind);
        var line = q.Receive((J?)ld.Line, el => (J)VisitNonNull(el!, q));
        var file = q.Receive((J?)ld.File, el => (J)VisitNonNull(el!, q));
        return ld with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Kind = (LineKind)kind!,
            Line = (Expression?)line,
            File = (Expression?)file
        };
    }

    // ---- New types ----

    public override J VisitKeyword(Keyword kw, RpcReceiveQueue q)
    {
        var kind = q.Receive<object>(kw.Kind);
        return kw with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Kind = (KeywordKind)kind! };
    }

    public override J VisitCsArgument(CsArgument arg, RpcReceiveQueue q)
    {
        var nameColumn = q.Receive(arg.NameColumn, rp => _delegate.VisitRightPadded(rp!, q));
        var refKindKeyword = q.Receive((J?)arg.RefKindKeyword, el => (J)VisitNonNull(el!, q));
        var expression = q.Receive((J)arg.Expression, el => (J)VisitNonNull(el, q));
        return arg with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            NameColumn = nameColumn, RefKindKeyword = (Keyword?)refKindKeyword,
            Expression = (Expression)expression! };
    }

    public override J VisitNameColon(NameColon nc, RpcReceiveQueue q)
    {
        var name = q.Receive(nc.Name, rp => _delegate.VisitRightPadded(rp, q));
        return nc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = name! };
    }

    public override J VisitAnnotatedStatement(AnnotatedStatement ans, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(ans.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var statement = q.Receive((J)ans.Statement, el => (J)VisitNonNull(el, q));
        return ans with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AttributeLists = attrLists!, Statement = (Statement)statement! };
    }

    public override J VisitArrayRankSpecifier(ArrayRankSpecifier ars, RpcReceiveQueue q)
    {
        var sizes = q.Receive(ars.Sizes, c => VisitContainer(c, q));
        return ars with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Sizes = sizes! };
    }

    public override J VisitAssignmentOperation(AssignmentOperation ao, RpcReceiveQueue q)
    {
        var variable = q.Receive((J)ao.Variable, el => (J)VisitNonNull(el, q));
        var op = q.Receive(ao.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var assignmentValue = q.Receive((J)ao.AssignmentValue, el => (J)VisitNonNull(el, q));
        var type = q.Receive(ao.Type, t => VisitType(t, q)!);
        return ao with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Variable = (Expression)variable!, Operator = op!,
            AssignmentValue = (Expression)assignmentValue!, Type = type };
    }

    public override J VisitStackAllocExpression(StackAllocExpression sae, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)sae.Expression, el => (J)VisitNonNull(el, q));
        return sae with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = (NewArray)expression! };
    }

    public override J VisitGotoStatement(GotoStatement gs, RpcReceiveQueue q)
    {
        var caseOrDefault = q.Receive((J?)gs.CaseOrDefaultKeyword, el => (J)VisitNonNull(el!, q));
        var target = q.Receive((J?)gs.Target, el => (J)VisitNonNull(el!, q));
        return gs with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            CaseOrDefaultKeyword = (Keyword?)caseOrDefault, Target = (Expression?)target };
    }

    public override J VisitEventDeclaration(EventDeclaration evd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(evd.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(evd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeExpr = q.Receive(evd.TypeExpressionPadded, lp => _delegate.VisitLeftPadded(lp, q));
        var interfaceSpec = q.Receive(evd.InterfaceSpecifier, rp => _delegate.VisitRightPadded(rp!, q));
        var name = q.Receive((J)evd.Name, el => (J)VisitNonNull(el, q));
        var accessors = q.Receive(evd.Accessors, c => VisitContainer(c!, q));
        return evd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AttributeLists = attrLists!, Modifiers = modifiers!,
            TypeExpressionPadded = typeExpr!, InterfaceSpecifier = interfaceSpec,
            Name = (Identifier)name!, Accessors = accessors };
    }

    public override J VisitCsBinary(CsBinary csb, RpcReceiveQueue q)
    {
        var left = q.Receive((J)csb.Left, el => (J)VisitNonNull(el, q));
        var op = q.Receive(csb.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var right = q.Receive((J)csb.Right, el => (J)VisitNonNull(el, q));
        var type = q.Receive(csb.Type, t => VisitType(t, q)!);
        return csb with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Left = (Expression)left!, Operator = op!, Right = (Expression)right!, Type = type };
    }

    public override J VisitCollectionExpression(CollectionExpression ce, RpcReceiveQueue q)
    {
        var elements = q.ReceiveList(ce.Elements, rp => _delegate.VisitRightPadded(rp, q));
        var type = q.Receive(ce.Type, t => VisitType(t, q)!);
        return ce with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Elements = elements!, Type = type };
    }

    public override J VisitCsExpressionStatement(CsExpressionStatement ces, RpcReceiveQueue q)
    {
        var exprPadded = q.Receive(ces.ExpressionPadded, rp => _delegate.VisitRightPadded(rp, q));
        return ces with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ExpressionPadded = exprPadded! };
    }

    public override J VisitForEachVariableLoop(ForEachVariableLoop fevl, RpcReceiveQueue q)
    {
        var controlElement = q.Receive((J)fevl.ControlElement, el => (J)VisitNonNull(el, q));
        var body = q.Receive(fevl.Body, rp => _delegate.VisitRightPadded(rp, q));
        return fevl with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ControlElement = (ForEachVariableLoopControl)controlElement!, Body = body! };
    }

    public override J VisitForEachVariableLoopControl(ForEachVariableLoopControl ctrl, RpcReceiveQueue q)
    {
        var variable = q.Receive(ctrl.Variable, rp => _delegate.VisitRightPadded(rp, q));
        var iterable = q.Receive(ctrl.Iterable, rp => _delegate.VisitRightPadded(rp, q));
        return ctrl with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Variable = variable!, Iterable = iterable! };
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
        return cmd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Attributes = attrs!, Modifiers = modifiers!,
            TypeParameters = typeParams, ReturnTypeExpression = (TypeTree)returnType!,
            ExplicitInterfaceSpecifier = explIntSpec, Name = (Identifier)name!,
            Parameters = parameters!, Body = (Statement?)body,
            MethodType = (JavaType.Method?)methodType };
    }

    public override J VisitUsingStatement(UsingStatement ust, RpcReceiveQueue q)
    {
        var awaitKw = q.Receive((J?)ust.AwaitKeyword, el => (J)VisitNonNull(el!, q));
        var exprPadded = q.Receive(ust.ExpressionPadded, lp => _delegate.VisitLeftPadded(lp, q));
        var statement = q.Receive((J)ust.Statement, el => (J)VisitNonNull(el, q));
        return ust with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AwaitKeyword = (Keyword?)awaitKw, ExpressionPadded = exprPadded!,
            Statement = (Statement)statement! };
    }

    public override J VisitAllowsConstraintClause(AllowsConstraintClause acc, RpcReceiveQueue q)
    {
        var expressions = q.Receive(acc.Expressions, c => VisitContainer(c, q));
        return acc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expressions = expressions! };
    }

    public override J VisitRefStructConstraint(RefStructConstraint rsc, RpcReceiveQueue q)
    {
        return rsc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers };
    }

    public override J VisitClassOrStructConstraint(ClassOrStructConstraint cosc, RpcReceiveQueue q)
    {
        var kind = q.Receive<object>(cosc.Kind);
        return cosc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Kind = (ClassOrStructConstraint.TypeKind)kind! };
    }

    public override J VisitConstructorConstraint(ConstructorConstraint cc, RpcReceiveQueue q)
    {
        return cc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers };
    }

    public override J VisitDefaultConstraint(DefaultConstraint dc, RpcReceiveQueue q)
    {
        return dc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers };
    }

    public override J VisitSingleVariableDesignation(SingleVariableDesignation svd, RpcReceiveQueue q)
    {
        var name = q.Receive((J)svd.Name, el => (J)VisitNonNull(el, q));
        return svd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = (Identifier)name! };
    }

    public override J VisitParenthesizedVariableDesignation(ParenthesizedVariableDesignation pvd, RpcReceiveQueue q)
    {
        var variables = q.Receive(pvd.Variables, c => VisitContainer(c, q));
        var type = q.Receive(pvd.Type, t => VisitType(t, q)!);
        return pvd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Variables = variables!, Type = type };
    }

    public override J VisitDiscardVariableDesignation(DiscardVariableDesignation dvd, RpcReceiveQueue q)
    {
        var discard = q.Receive((J)dvd.Discard, el => (J)VisitNonNull(el, q));
        return dvd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Discard = (Identifier)discard! };
    }

    public override J VisitCsUnary(CsUnary csu, RpcReceiveQueue q)
    {
        var op = q.Receive(csu.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var expression = q.Receive((J)csu.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(csu.Type, t => VisitType(t, q)!);
        return csu with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Operator = op!, Expression = (Expression)expression!, Type = type };
    }

    public override J VisitTupleElement(TupleElement tel, RpcReceiveQueue q)
    {
        var elementType = q.Receive((J)tel.ElementType, el => (J)VisitNonNull(el, q));
        var name = q.Receive((J?)tel.Name, el => (J)VisitNonNull(el!, q));
        return tel with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ElementType = (TypeTree)elementType!, Name = (Identifier?)name };
    }

    public override J VisitCsNewClass(CsNewClass csnc, RpcReceiveQueue q)
    {
        var newClassCore = q.Receive((J)csnc.NewClassCore, el => (J)VisitNonNull(el, q));
        var initializer = q.Receive((J?)csnc.Initializer, el => (J)VisitNonNull(el!, q));
        return csnc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            NewClassCore = (NewClass)newClassCore!, Initializer = (InitializerExpression?)initializer };
    }

    public override J VisitImplicitElementAccess(ImplicitElementAccess iea, RpcReceiveQueue q)
    {
        var argList = q.Receive(iea.ArgumentList, c => VisitContainer(c, q));
        return iea with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ArgumentList = argList! };
    }

    public override J VisitUnaryPattern(UnaryPattern up, RpcReceiveQueue q)
    {
        var op = q.Receive((J)up.Operator, el => (J)VisitNonNull(el, q));
        var pattern = q.Receive((J)up.PatternValue, el => (J)VisitNonNull(el, q));
        return up with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Operator = (Keyword)op!, PatternValue = (Pattern)pattern! };
    }

    public override J VisitTypePattern(TypePattern tp, RpcReceiveQueue q)
    {
        var typeId = q.Receive((J)tp.TypeIdentifier, el => (J)VisitNonNull(el, q));
        var designation = q.Receive((J?)tp.Designation, el => (J)VisitNonNull(el!, q));
        return tp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            TypeIdentifier = (TypeTree)typeId!, Designation = (VariableDesignation?)designation };
    }

    public override J VisitBinaryPattern(BinaryPattern bp, RpcReceiveQueue q)
    {
        var left = q.Receive((J)bp.Left, el => (J)VisitNonNull(el, q));
        var op = q.Receive(bp.Operator, lp => _delegate.VisitLeftPadded(lp, q));
        var right = q.Receive((J)bp.Right, el => (J)VisitNonNull(el, q));
        return bp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Left = (Pattern)left!, Operator = op!, Right = (Pattern)right! };
    }

    public override J VisitConstantPattern(ConstantPattern cp, RpcReceiveQueue q)
    {
        var value = q.Receive((J)cp.Value, el => (J)VisitNonNull(el, q));
        return cp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Value = (Expression)value! };
    }

    public override J VisitDiscardPattern(DiscardPattern dp, RpcReceiveQueue q)
    {
        var type = q.Receive(dp.Type, t => VisitType(t, q)!);
        return dp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Type = type };
    }

    public override J VisitListPattern(ListPattern lp, RpcReceiveQueue q)
    {
        var patterns = q.Receive(lp.Patterns, c => VisitContainer(c, q));
        var designation = q.Receive((J?)lp.Designation, el => (J)VisitNonNull(el!, q));
        return lp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Patterns = patterns!, Designation = (VariableDesignation?)designation };
    }

    public override J VisitParenthesizedPattern(ParenthesizedPattern pap, RpcReceiveQueue q)
    {
        var patternValue = q.Receive(pap.PatternValue, c => VisitContainer(c, q));
        return pap with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            PatternValue = patternValue! };
    }

    public override J VisitRecursivePattern(RecursivePattern rcp, RpcReceiveQueue q)
    {
        var typeQualifier = q.Receive((J?)rcp.TypeQualifier, el => (J)VisitNonNull(el!, q));
        var positionalPattern = q.Receive((J?)rcp.PositionalPattern, el => (J)VisitNonNull(el!, q));
        var propertyPattern = q.Receive((J?)rcp.PropertyPatternValue, el => (J)VisitNonNull(el!, q));
        var designation = q.Receive((J?)rcp.Designation, el => (J)VisitNonNull(el!, q));
        return rcp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            TypeQualifier = (TypeTree?)typeQualifier,
            PositionalPattern = (PositionalPatternClause?)positionalPattern,
            PropertyPatternValue = (PropertyPatternClause?)propertyPattern,
            Designation = (VariableDesignation?)designation };
    }

    public override J VisitVarPattern(VarPattern vp, RpcReceiveQueue q)
    {
        var designation = q.Receive((J)vp.Designation, el => (J)VisitNonNull(el, q));
        return vp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Designation = (VariableDesignation)designation! };
    }

    public override J VisitPositionalPatternClause(PositionalPatternClause ppc, RpcReceiveQueue q)
    {
        var subpatterns = q.Receive(ppc.Subpatterns, c => VisitContainer(c, q));
        return ppc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Subpatterns = subpatterns! };
    }

    public override J VisitSlicePattern(SlicePattern sp, RpcReceiveQueue q)
    {
        return sp with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers };
    }

    public override J VisitPropertyPatternClause(PropertyPatternClause ppclause, RpcReceiveQueue q)
    {
        var subpatterns = q.Receive(ppclause.Subpatterns, c => VisitContainer(c, q));
        return ppclause with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Subpatterns = subpatterns! };
    }

    public override J VisitSubpattern(Subpattern sub, RpcReceiveQueue q)
    {
        var name = q.Receive((J?)sub.Name, el => (J)VisitNonNull(el!, q));
        var patternValue = q.Receive(sub.PatternValue, lp => _delegate.VisitLeftPadded(lp, q));
        return sub with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = (Expression?)name, PatternValue = patternValue! };
    }

    public override J VisitSwitchExpression(SwitchExpression swe, RpcReceiveQueue q)
    {
        var expr = q.Receive(swe.ExpressionPadded, rp => _delegate.VisitRightPadded(rp, q));
        var arms = q.Receive(swe.Arms, c => VisitContainer(c, q));
        return swe with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ExpressionPadded = expr!, Arms = arms! };
    }

    public override J VisitSwitchExpressionArm(SwitchExpressionArm swea, RpcReceiveQueue q)
    {
        var pattern = q.Receive((J)swea.Pattern, el => (J)VisitNonNull(el, q));
        var whenExpr = q.Receive(swea.WhenExpression, lp => _delegate.VisitLeftPadded(lp!, q));
        var exprPadded = q.Receive(swea.ExpressionPadded, lp => _delegate.VisitLeftPadded(lp, q));
        return swea with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Pattern = (Pattern)pattern!, WhenExpression = whenExpr,
            ExpressionPadded = exprPadded! };
    }

    public override J VisitSwitchSection(SwitchSection ss, RpcReceiveQueue q)
    {
        var labels = q.ReceiveList(ss.Labels, l => (SwitchLabel)VisitNonNull(l, q));
        var statements = q.ReceiveList(ss.Statements, rp => _delegate.VisitRightPadded(rp, q));
        return ss with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Labels = labels!, Statements = statements! };
    }

    public override J VisitDefaultSwitchLabel(DefaultSwitchLabel dsl, RpcReceiveQueue q)
    {
        var colonToken = q.Receive(dsl.ColonToken, space => VisitSpace(space, q));
        return dsl with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ColonToken = colonToken! };
    }

    public override J VisitCasePatternSwitchLabel(CasePatternSwitchLabel cpsl, RpcReceiveQueue q)
    {
        var pattern = q.Receive((J)cpsl.Pattern, el => (J)VisitNonNull(el, q));
        var whenClause = q.Receive(cpsl.WhenClause, lp => _delegate.VisitLeftPadded(lp!, q));
        var colonToken = q.Receive(cpsl.ColonToken, space => VisitSpace(space, q));
        return cpsl with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Pattern = (Pattern)pattern!, WhenClause = whenClause, ColonToken = colonToken! };
    }

    public override J VisitSwitchStatement(SwitchStatement sws, RpcReceiveQueue q)
    {
        var exprPadded = q.Receive(sws.ExpressionPadded, c => VisitContainer(c, q));
        var sections = q.Receive(sws.Sections, c => VisitContainer(c, q));
        return sws with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ExpressionPadded = exprPadded!, Sections = sections! };
    }

    public override J VisitLockStatement(LockStatement ls, RpcReceiveQueue q)
    {
        var exprValue = q.Receive((J)ls.ExpressionValue, el => (J)VisitNonNull(el, q));
        var stmtPadded = q.Receive(ls.StatementPadded, rp => _delegate.VisitRightPadded(rp, q));
        return ls with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ExpressionValue = (ControlParentheses<Expression>)exprValue!,
            StatementPadded = stmtPadded! };
    }

    public override J VisitCheckedExpression(CheckedExpression che, RpcReceiveQueue q)
    {
        var keyword = q.Receive((J)che.CheckedOrUncheckedKeyword, el => (J)VisitNonNull(el, q));
        var exprValue = q.Receive((J)che.ExpressionValue, el => (J)VisitNonNull(el, q));
        return che with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            CheckedOrUncheckedKeyword = (Keyword)keyword!,
            ExpressionValue = (ControlParentheses<Expression>)exprValue! };
    }

    public override J VisitCheckedStatement(CheckedStatement chs, RpcReceiveQueue q)
    {
        var keyword = q.Receive((J)chs.KeywordValue, el => (J)VisitNonNull(el, q));
        var block = q.Receive((J)chs.Block, el => (J)VisitNonNull(el, q));
        return chs with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            KeywordValue = (Keyword)keyword!, Block = (Block)block! };
    }

    public override J VisitRangeExpression(RangeExpression rang, RpcReceiveQueue q)
    {
        var start = q.Receive(rang.Start, rp => _delegate.VisitRightPadded(rp!, q));
        var end = q.Receive((J?)rang.End, el => (J)VisitNonNull(el!, q));
        return rang with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Start = start, End = (Expression?)end };
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
        return idxd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Modifiers = modifiers!, TypeExpression = (TypeTree)typeExpr!,
            ExplicitInterfaceSpecifier = explIntSpec, Indexer = (Expression)indexer!,
            Parameters = parameters!, ExpressionBody = exprBody,
            Accessors = (Block?)accessors };
    }

    public override J VisitDelegateDeclaration(DelegateDeclaration deld, RpcReceiveQueue q)
    {
        var attrs = q.ReceiveList(deld.Attributes, t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(deld.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var returnType = q.Receive(deld.ReturnType, lp => _delegate.VisitLeftPadded(lp, q));
        var name = q.Receive((J)deld.IdentifierName, el => (J)VisitNonNull(el, q));
        var typeParams = q.Receive(deld.TypeParameters, c => VisitContainer(c!, q));
        var parameters = q.Receive(deld.Parameters, c => VisitContainer(c, q));
        return deld with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Attributes = attrs!, Modifiers = modifiers!, ReturnType = returnType!,
            IdentifierName = (Identifier)name!, TypeParameters = typeParams,
            Parameters = parameters! };
    }

    public override J VisitConversionOperatorDeclaration(ConversionOperatorDeclaration cod, RpcReceiveQueue q)
    {
        var modifiers = q.ReceiveList(cod.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var kind = q.Receive(cod.Kind, lp => _delegate.VisitLeftPadded(lp, q));
        var returnType = q.Receive(cod.ReturnType, lp => _delegate.VisitLeftPadded(lp, q));
        var parameters = q.Receive(cod.Parameters, c => VisitContainer(c, q));
        var exprBody = q.Receive(cod.ExpressionBody, lp => _delegate.VisitLeftPadded(lp!, q));
        var body = q.Receive((J?)cod.Body, el => (J)VisitNonNull(el!, q));
        return cod with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Modifiers = modifiers!, Kind = kind!, ReturnType = returnType!,
            Parameters = parameters!, ExpressionBody = exprBody, Body = (Block?)body };
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
        return opd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AttributeLists = attrLists!, Modifiers = modifiers!,
            ExplicitInterfaceSpecifier = explIntSpec,
            OperatorKeyword = (Keyword)operatorKw!, CheckedKeyword = (Keyword?)checkedKw,
            OperatorToken = operatorToken!, ReturnType = (TypeTree)returnType!,
            Parameters = parameters!, Body = (Block)body!,
            MethodType = (JavaType.Method?)methodType };
    }

    public override J VisitEnumDeclaration(EnumDeclaration enumd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(enumd.AttributeLists ?? new List<AttributeList>(),
            t => (AttributeList)VisitNonNull(t, q));
        var modifiers = q.ReceiveList(enumd.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var namePadded = q.Receive(enumd.NamePadded, lp => _delegate.VisitLeftPadded(lp, q));
        var baseType = q.Receive(enumd.BaseType, lp => _delegate.VisitLeftPadded(lp!, q));
        var members = q.Receive(enumd.Members, c => VisitContainer(c!, q));
        return enumd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AttributeLists = attrLists, Modifiers = modifiers!,
            NamePadded = namePadded!, BaseType = baseType, Members = members };
    }

    public override J VisitEnumMemberDeclaration(EnumMemberDeclaration enummd, RpcReceiveQueue q)
    {
        var attrLists = q.ReceiveList(enummd.AttributeLists, t => (AttributeList)VisitNonNull(t, q));
        var name = q.Receive((J)enummd.Name, el => (J)VisitNonNull(el, q));
        var initializer = q.Receive(enummd.Initializer, lp => _delegate.VisitLeftPadded(lp!, q));
        return enummd with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            AttributeLists = attrLists!, Name = (Identifier)name!,
            Initializer = initializer };
    }

    public override J VisitAliasQualifiedName(AliasQualifiedName aqn, RpcReceiveQueue q)
    {
        var alias = q.Receive(aqn.Alias, rp => _delegate.VisitRightPadded(rp, q));
        var name = q.Receive((J)aqn.Name, el => (J)VisitNonNull(el, q));
        return aqn with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Alias = alias!, Name = (Expression)name! };
    }

    public override J VisitCsArrayType(CsArrayType csat, RpcReceiveQueue q)
    {
        var typeExpr = q.Receive((J?)csat.TypeExpression, el => (J)VisitNonNull(el!, q));
        var dimensions = q.ReceiveList(csat.Dimensions,
            d => (ArrayDimension)VisitNonNull(d, q));
        var type = q.Receive(csat.Type, t => VisitType(t, q)!);
        return csat with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            TypeExpression = (TypeTree?)typeExpr, Dimensions = dimensions!,
            Type = type };
    }

    public override J VisitCsTry(CsTry cstry, RpcReceiveQueue q)
    {
        var body = q.Receive((J)cstry.Body, el => (J)VisitNonNull(el, q));
        var catches = q.ReceiveList(cstry.Catches, c => (CsTryCatch)VisitNonNull(c, q));
        var finallyBlock = q.Receive(cstry.Finally, lp => _delegate.VisitLeftPadded(lp!, q));
        return cstry with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Body = (Block)body!, Catches = catches!,
            Finally = finallyBlock };
    }

    public override J VisitCsTryCatch(CsTryCatch cstc, RpcReceiveQueue q)
    {
        var parameter = q.Receive((J)cstc.Parameter, el => (J)VisitNonNull(el, q));
        var filterExpr = q.Receive(cstc.FilterExpression, lp => _delegate.VisitLeftPadded(lp!, q));
        var body = q.Receive((J)cstc.Body, el => (J)VisitNonNull(el, q));
        return cstc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Parameter = (ControlParentheses<VariableDeclarations>)parameter!,
            FilterExpression = filterExpr, Body = (Block)body! };
    }

    public override J VisitPointerFieldAccess(PointerFieldAccess pfa, RpcReceiveQueue q)
    {
        var target = q.Receive((J)pfa.Target, el => (J)VisitNonNull(el, q));
        var namePadded = q.Receive(pfa.NamePadded, lp => _delegate.VisitLeftPadded(lp, q));
        var type = q.Receive(pfa.Type, t => VisitType(t, q)!);
        return pfa with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Target = (Expression)target!, NamePadded = namePadded!, Type = type };
    }

    public override J VisitRefType(RefType rt, RpcReceiveQueue q)
    {
        var readonlyKw = q.Receive((J?)rt.ReadonlyKeyword, el => (J)VisitNonNull(el!, q));
        var typeIdentifier = q.Receive((J)rt.TypeIdentifier, el => (J)VisitNonNull(el, q));
        var type = q.Receive(rt.Type, t => VisitType(t, q)!);
        return rt with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ReadonlyKeyword = (Modifier?)readonlyKw, TypeIdentifier = (TypeTree)typeIdentifier!,
            Type = type };
    }

    // ---- LINQ ----

    public override J VisitQueryExpression(QueryExpression qe, RpcReceiveQueue q)
    {
        var fromClause = q.Receive((J)qe.FromClause, el => (J)VisitNonNull(el, q));
        var body = q.Receive((J)qe.Body, el => (J)VisitNonNull(el, q));
        return qe with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            FromClause = (FromClause)fromClause!, Body = (QueryBody)body! };
    }

    public override J VisitQueryBody(QueryBody qb, RpcReceiveQueue q)
    {
        var clauses = q.ReceiveList(qb.Clauses.Cast<QueryClause>().ToList(),
            el => (QueryClause)VisitNonNull(el, q));
        var selectOrGroup = q.Receive((J?)qb.SelectOrGroup, el => (J)VisitNonNull(el!, q));
        var continuation = q.Receive((J?)qb.Continuation, el => (J)VisitNonNull(el!, q));
        return qb with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Clauses = clauses?.Cast<QueryClause>().ToList() ?? new List<QueryClause>(),
            SelectOrGroup = (SelectOrGroupClause?)selectOrGroup,
            Continuation = (QueryContinuation?)continuation };
    }

    public override J VisitFromClause(FromClause fc, RpcReceiveQueue q)
    {
        var typeIdentifier = q.Receive((J?)fc.TypeIdentifier, el => (J)VisitNonNull(el!, q));
        var identifier = q.Receive(fc.IdentifierPadded, rp => _delegate.VisitRightPadded(rp, q));
        var expression = q.Receive((J)fc.Expression, el => (J)VisitNonNull(el, q));
        return fc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            TypeIdentifier = (TypeTree?)typeIdentifier,
            IdentifierPadded = identifier!,
            Expression = (Expression)expression! };
    }

    public override J VisitLetClause(LetClause lc, RpcReceiveQueue q)
    {
        var identifier = q.Receive(lc.IdentifierPadded, rp => _delegate.VisitRightPadded(rp, q));
        var expression = q.Receive((J)lc.Expression, el => (J)VisitNonNull(el, q));
        return lc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            IdentifierPadded = identifier!,
            Expression = (Expression)expression! };
    }

    public override J VisitJoinClause(JoinClause jc, RpcReceiveQueue q)
    {
        var identifier = q.Receive(jc.IdentifierPadded, rp => _delegate.VisitRightPadded(rp, q));
        var inExpression = q.Receive(jc.InExpression, rp => _delegate.VisitRightPadded(rp, q));
        var leftExpression = q.Receive(jc.LeftExpression, rp => _delegate.VisitRightPadded(rp, q));
        var rightExpression = q.Receive((J)jc.RightExpression, el => (J)VisitNonNull(el, q));
        var into = q.Receive(jc.Into, lp => _delegate.VisitLeftPadded(lp!, q));
        return jc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            IdentifierPadded = identifier!,
            InExpression = inExpression!,
            LeftExpression = leftExpression!,
            RightExpression = (Expression)rightExpression!,
            Into = into };
    }

    public override J VisitJoinIntoClause(JoinIntoClause jic, RpcReceiveQueue q)
    {
        var identifier = q.Receive((J)jic.Identifier, el => (J)VisitNonNull(el, q));
        return jic with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Identifier = (Identifier)identifier! };
    }

    public override J VisitWhereClause(WhereClause wc, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)wc.Condition, el => (J)VisitNonNull(el, q));
        return wc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Condition = (Expression)condition! };
    }

    public override J VisitOrderByClause(OrderByClause obc, RpcReceiveQueue q)
    {
        var orderings = q.ReceiveList(obc.Orderings, rp => _delegate.VisitRightPadded(rp, q));
        return obc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Orderings = orderings! };
    }

    public override J VisitOrdering(Ordering ord, RpcReceiveQueue q)
    {
        var expression = q.Receive(ord.ExpressionPadded, rp => _delegate.VisitRightPadded(rp, q));
        var direction = q.Receive<object?>(ord.Direction);
        return ord with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ExpressionPadded = expression!,
            Direction = direction != null ? (DirectionKind)direction : null };
    }

    public override J VisitSelectClause(SelectClause sc, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)sc.Expression, el => (J)VisitNonNull(el, q));
        return sc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Expression = (Expression)expression! };
    }

    public override J VisitGroupClause(GroupClause gc, RpcReceiveQueue q)
    {
        var groupExpression = q.Receive(gc.GroupExpression, rp => _delegate.VisitRightPadded(rp, q));
        var key = q.Receive((J)gc.Key, el => (J)VisitNonNull(el, q));
        return gc with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            GroupExpression = groupExpression!,
            Key = (Expression)key! };
    }

    public override J VisitQueryContinuation(QueryContinuation qcont, RpcReceiveQueue q)
    {
        var identifier = q.Receive((J)qcont.Identifier, el => (J)VisitNonNull(el, q));
        var body = q.Receive((J)qcont.Body, el => (J)VisitNonNull(el, q));
        return qcont with { Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Identifier = (Identifier)identifier!,
            Body = (QueryBody)body! };
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
