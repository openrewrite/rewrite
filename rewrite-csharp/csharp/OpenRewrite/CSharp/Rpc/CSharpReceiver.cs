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
            TypeParameterBound tpb => VisitTypeParameterBound(tpb, q),
            InterpolatedString istr => VisitInterpolatedString(istr, q),
            Interpolation interp => VisitInterpolation(interp, q),
            AwaitExpression ae => VisitAwaitExpression(ae, q),
            YieldStatement ys => VisitYieldStatement(ys, q),
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

    // ---- TypeParameterBound ----
    public override J VisitTypeParameterBound(TypeParameterBound tpb, RpcReceiveQueue q)
    {
        var name = q.Receive((J?)tpb.Name, el => (J)VisitNonNull(el!, q));
        var beforeColon = q.Receive(tpb.BeforeColon, space => VisitSpace(space!, q));
        var bound = q.Receive((J)tpb.Bound, el => (J)VisitNonNull(el, q));
        return tpb with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            Name = (TypeTree?)name,
            BeforeColon = beforeColon,
            Bound = (TypeTree)bound!
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

    // ---- YieldStatement ----
    public override J VisitYieldStatement(YieldStatement ys, RpcReceiveQueue q)
    {
        var returnOrBreakKeyword = q.Receive(ys.ReturnOrBreakKeyword, lp => _delegate.VisitLeftPadded(lp, q));
        var value = q.Receive((J?)ys.Value, el => (J)VisitNonNull(el!, q));
        return ys with
        {
            Id = PvId, Prefix = PvPrefix, Markers = PvMarkers,
            ReturnOrBreakKeyword = returnOrBreakKeyword,
            Value = (Expression?)value
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
