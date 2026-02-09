using System.Linq;
using Rewrite.Core;
using Rewrite.Core.Rpc;
using Rewrite.Java;
using Rewrite.Java.Rpc;
using static Rewrite.Core.Rpc.Reference;

namespace Rewrite.CSharp.Rpc;

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
        // Send null for Global/Static when absent; nagoya's bool representation doesn't
        // match Java's Keyword tree, but null/NO_CHANGE is protocol-compatible for absent cases
        q.GetAndSend(ud, u => u.IsGlobal ? (object?)u.Global : null);
        q.GetAndSend(ud, u => u.IsStatic ? (object?)u.Static : null);
        // Unsafe: nagoya doesn't model this
        q.GetAndSend(ud, _ => (object?)null);
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
        q.GetAndSend(de, d => (J)d.Type, el => Visit(el, q));
        q.GetAndSend(de, d => (J)d.Variable.Element, el => Visit(el, q));
        return de;
    }

    // ---- CsLambda ----
    public override J VisitCsLambda(CsLambda csl, RpcSendQueue q)
    {
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
        return pp;
    }

    // ---- TypeParameterBound ----
    public override J VisitTypeParameterBound(TypeParameterBound tpb, RpcSendQueue q)
    {
        q.GetAndSend(tpb, t => (J?)t.Name, el => Visit(el!, q));
        q.GetAndSend(tpb, t => t.BeforeColon, space => VisitSpace(space!, q));
        q.GetAndSend(tpb, t => (J)t.Bound, el => Visit(el, q));
        return tpb;
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
        q.GetAndSend(istr, i => DeriveEndDelimiter(i.Delimiter));
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

    // ---- YieldStatement ----
    public override J VisitYieldStatement(YieldStatement ys, RpcSendQueue q)
    {
        q.GetAndSend(ys, y => y.ReturnOrBreakKeyword, lp => VisitLeftPadded(lp, q));
        q.GetAndSend(ys, y => (J?)y.Value, el => Visit(el!, q));
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

    // ---- ConditionalBlock ----
    public override J VisitConditionalBlock(ConditionalBlock cb, RpcSendQueue q)
    {
        q.GetAndSend(cb, c => (J)c.IfBranch, el => Visit(el, q));
        q.GetAndSendList(cb, c => (IList<ElifDirective>)c.ElifBranches,
            e => (object)e.Id, e => Visit(e, q));
        q.GetAndSend(cb, c => (J?)c.ElseBranch, el => Visit(el!, q));
        q.GetAndSend(cb, c => c.BeforeEndif, space => VisitSpace(space, q));
        return cb;
    }

    // ---- IfDirective ----
    public override J VisitIfDirective(IfDirective ifd, RpcSendQueue q)
    {
        q.GetAndSend(ifd, i => (J)i.Condition, el => Visit(el, q));
        q.GetAndSend(ifd, i => (object)i.BranchTaken);
        q.GetAndSendList(ifd, i => i.Body,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        return ifd;
    }

    // ---- ElifDirective ----
    public override J VisitElifDirective(ElifDirective elif, RpcSendQueue q)
    {
        q.GetAndSend(elif, e => (J)e.Condition, el => Visit(el, q));
        q.GetAndSend(elif, e => (object)e.BranchTaken);
        q.GetAndSendList(elif, e => e.Body,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        return elif;
    }

    // ---- ElseDirective ----
    public override J VisitElseDirective(ElseDirective elsed, RpcSendQueue q)
    {
        q.GetAndSend(elsed, e => (object)e.BranchTaken);
        q.GetAndSendList(elsed, e => e.Body,
            r => (object)r.Element.Id, r => VisitRightPadded(r, q));
        return elsed;
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
        return nd;
    }

    // ---- RegionDirective ----
    public override J VisitRegionDirective(RegionDirective rd, RpcSendQueue q)
    {
        q.GetAndSend(rd, r => (object?)r.Name);
        return rd;
    }

    // ---- EndRegionDirective ----
    public override J VisitEndRegionDirective(EndRegionDirective erd, RpcSendQueue q)
    {
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
