using OpenRewrite.Core;
using OpenRewrite.Core.Rpc;
using static Rewrite.Core.Rpc.Reference;

namespace OpenRewrite.Java.Rpc;

public class JavaSender : JavaVisitor<RpcSendQueue>
{
    private static readonly IList<Annotation> EmptyAnnotationList = Array.Empty<Annotation>();

    public override J? Visit(Tree? tree, RpcSendQueue q)
    {
        if (tree == null) return null;

        Cursor = new Cursor(Cursor, tree);

        if (tree is J j)
        {
            PreVisit(j, q);
        }

        var result = tree switch
        {
            Annotation ann => VisitAnnotation(ann, q),
            Block blk => VisitBlock(blk, q),
            ClassDeclaration cd => VisitClassDeclaration(cd, q),
            EnumValueSet evs => VisitEnumValueSet(evs, q),
            EnumValue ev => VisitEnumValue(ev, q),
            MethodDeclaration md => VisitMethodDeclaration(md, q),
            TypeParameters tps => VisitTypeParameters(tps, q),
            TypeParameter tp => VisitTypeParameter(tp, q),
            Modifier mod => VisitModifier(mod, q),
            Return ret => VisitReturn(ret, q),
            If.Else els => VisitElse(els, q),
            If iff => VisitIf(iff, q),
            WhileLoop whl => VisitWhileLoop(whl, q),
            DoWhileLoop dwl => VisitDoWhileLoop(dwl, q),
            ForLoop.Control fc => VisitForControl(fc, q),
            ForLoop fl => VisitForLoop(fl, q),
            ForEachLoop.Control fec => VisitForEachControl(fec, q),
            ForEachLoop fel => VisitForEachLoop(fel, q),
            Try.Catch tryCatch => VisitCatch(tryCatch, q),
            Try tr => VisitTry(tr, q),
            Throw thr => VisitThrow(thr, q),
            Break brk => VisitBreak(brk, q),
            Continue cont => VisitContinue(cont, q),
            Empty emp => VisitEmpty(emp, q),
            Literal lit => VisitLiteral(lit, q),
            Identifier id => VisitIdentifier(id, q),
            FieldAccess fa => VisitFieldAccess(fa, q),
            MemberReference mr => VisitMemberReference(mr, q),
            Binary bin => VisitBinary(bin, q),
            Ternary ter => VisitTernary(ter, q),
            Assignment asn => VisitAssignment(asn, q),
            AssignmentOperation asnOp => VisitAssignmentOperation(asnOp, q),
            Unary un => VisitUnary(un, q),
            Parentheses<Expression> paren => VisitParentheses(paren, q),
            ControlParentheses<Expression> cp => VisitControlParentheses(cp, q),
            ControlParentheses<TypeTree> cptt => VisitControlParentheses(cptt, q),
            ControlParentheses<VariableDeclarations> cpvd => VisitControlParentheses(cpvd, q),
            ExpressionStatement es => VisitExpressionStatement(es, q),
            VariableDeclarations vd => VisitVariableDeclarations(vd, q),
            NamedVariable nv => VisitVariable(nv, q),
            Primitive prim => VisitPrimitive(prim, q),
            MethodInvocation mi => VisitMethodInvocation(mi, q),
            NewClass nc => VisitNewClass(nc, q),
            NewArray na => VisitNewArray(na, q),
            InstanceOf io => VisitInstanceOf(io, q),
            NullableType nt => VisitNullableType(nt, q),
            ParameterizedType pt => VisitParameterizedType(pt, q),
            ArrayType at => VisitArrayType(at, q),
            ArrayAccess aa => VisitArrayAccess(aa, q),
            ArrayDimension ad => VisitArrayDimension(ad, q),
            Lambda.Parameters lp => VisitLambdaParameters(lp, q),
            Lambda lam => VisitLambda(lam, q),
            Switch sw => VisitSwitch(sw, q),
            SwitchExpression se => VisitSwitchExpression(se, q),
            Case cs => VisitCase(cs, q),
            DeconstructionPattern dp => VisitDeconstructionPattern(dp, q),
            Label lbl => VisitLabel(lbl, q),
            Synchronized sync => VisitSynchronized(sync, q),
            TypeCast tc => VisitTypeCast(tc, q),
            Package pkg => VisitPackage(pkg, q),
            _ => throw new InvalidOperationException($"Unknown J tree type: {tree.GetType()}")
        };

        Cursor = Cursor.Parent!;
        return result;
    }

    public new virtual J PreVisit(J j, RpcSendQueue q)
    {
        q.GetAndSend(j, (J t) => t.Id);
        q.GetAndSend(j, (J t) => t.Prefix, space => VisitSpace(GetValueNonNull<Space>(space), q));
        q.GetAndSend(j, (J t) => t.Markers);
        return j;
    }

    public override J VisitAnnotation(Annotation annotation, RpcSendQueue q)
    {
        q.GetAndSend(annotation, a => a.AnnotationType, type => Visit(type, q));
        q.GetAndSend(annotation, a => a.Arguments, args => VisitContainer(args, q));
        return annotation;
    }

    public override J VisitArrayAccess(ArrayAccess arrayAccess, RpcSendQueue q)
    {
        q.GetAndSend(arrayAccess, a => a.Indexed, indexed => Visit(indexed, q));
        q.GetAndSend(arrayAccess, a => a.Dimension, dim => Visit(dim, q));
        return arrayAccess;
    }

    public override J VisitArrayDimension(ArrayDimension arrayDimension, RpcSendQueue q)
    {
        q.GetAndSend(arrayDimension, a => a.Index, idx => VisitRightPadded(idx, q));
        return arrayDimension;
    }

    public override J VisitLabel(Label label, RpcSendQueue q)
    {
        q.GetAndSend(label, l => l.LabelName, lbl => VisitRightPadded(lbl, q));
        q.GetAndSend(label, l => l.Statement, stmt => Visit(stmt, q));
        return label;
    }

    public override J VisitInstanceOf(InstanceOf instanceOf, RpcSendQueue q)
    {
        q.GetAndSend(instanceOf, i => i.Expression, expr => VisitRightPadded(expr, q));
        q.GetAndSend(instanceOf, i => i.Clazz, clazz => Visit(clazz, q));
        q.GetAndSend(instanceOf, i => i.Pattern, pattern => Visit(pattern, q));
        q.GetAndSend(instanceOf, i => AsRef(i.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        q.GetAndSend(instanceOf, i => i.InstanceOfModifier, m => Visit(m, q));
        return instanceOf;
    }

    public override J VisitNullableType(NullableType nullableType, RpcSendQueue q)
    {
        q.GetAndSendList(nullableType, n => n.Annotations, a => a.Id, a => Visit(a, q));
        q.GetAndSend(nullableType, n => n.TypeTreePadded, t => VisitRightPadded(t, q));
        return nullableType;
    }

    public override J VisitParameterizedType(ParameterizedType type, RpcSendQueue q)
    {
        q.GetAndSend(type, t => (J)t.Clazz, clazz => Visit(clazz, q));
        q.GetAndSend(type, t => t.TypeParameters, p => VisitContainer(p, q));
        q.GetAndSend(type, t => AsRef(t.Type), t => VisitType(GetValueNonNull<JavaType>(t), q));
        return type;
    }

    public override J VisitArrayType(ArrayType arrayType, RpcSendQueue q)
    {
        q.GetAndSend(arrayType, a => (J)a.ElementType, type => Visit(type, q));
        q.GetAndSendList(arrayType, a => a.Annotations, a => a.Id, a => Visit(a, q));
        q.GetAndSend(arrayType, a => a.Dimension, d => VisitLeftPadded(d, q));
        q.GetAndSend(arrayType, a => AsRef(a.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return arrayType;
    }

    public override J VisitAssignment(Assignment assignment, RpcSendQueue q)
    {
        q.GetAndSend(assignment, a => (J)a.Variable, variable => Visit(variable, q));
        q.GetAndSend(assignment, a => a.AssignmentValue, assign => VisitLeftPadded(assign, q));
        q.GetAndSend(assignment, a => AsRef(a.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return assignment;
    }

    public override J VisitAssignmentOperation(AssignmentOperation assignmentOperation, RpcSendQueue q)
    {
        q.GetAndSend(assignmentOperation, a => (J)a.Variable, variable => Visit(variable, q));
        q.GetAndSend(assignmentOperation, a => a.Operator, op => VisitLeftPadded(op, q));
        q.GetAndSend(assignmentOperation, a => (J)a.AssignmentValue, assign => Visit(assign, q));
        q.GetAndSend(assignmentOperation, a => AsRef(a.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return assignmentOperation;
    }

    public override J VisitBinary(Binary binary, RpcSendQueue q)
    {
        q.GetAndSend(binary, b => (J)b.Left, left => Visit(left, q));
        q.GetAndSend(binary, b => b.Operator, op => VisitLeftPadded(op, q));
        q.GetAndSend(binary, b => (J)b.Right, right => Visit(right, q));
        q.GetAndSend(binary, b => AsRef(b.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return binary;
    }

    public override J VisitBlock(Block block, RpcSendQueue q)
    {
        q.GetAndSend(block, b => b.Static, s => VisitRightPadded(s, q));
        q.GetAndSendList(block, b => b.Statements, r => r.Element.Id, stmts => VisitRightPadded(stmts, q));
        q.GetAndSend(block, b => b.End, space => VisitSpace(GetValueNonNull<Space>(space), q));
        return block;
    }

    public override J VisitBreak(Break breakStmt, RpcSendQueue q)
    {
        q.GetAndSend(breakStmt, b => b.Label, label => Visit(label, q));
        return breakStmt;
    }

    public override J VisitCase(Case caseStmt, RpcSendQueue q)
    {
        q.GetAndSend(caseStmt, c => c.CaseKind);
        q.GetAndSend(caseStmt, c => c.CaseLabels, labels => VisitContainer(labels, q));
        q.GetAndSend(caseStmt, c => c.Statements, stmts => VisitContainer(stmts, q));
        q.GetAndSend(caseStmt, c => c.Body, body => VisitRightPadded(body, q));
        q.GetAndSend(caseStmt, c => c.Guard, guard => Visit(guard, q));
        return caseStmt;
    }

    public override J VisitClassDeclaration(ClassDeclaration classDecl, RpcSendQueue q)
    {
        q.GetAndSendList(classDecl, c => c.LeadingAnnotations, a => a.Id, j => Visit(j, q));
        q.GetAndSendList(classDecl, c => c.Modifiers, m => m.Id, j => Visit(j, q));
        q.GetAndSend(classDecl, c => c.ClassKind, k => VisitClassDeclarationKind(k, q));
        q.GetAndSend(classDecl, c => (J)c.Name, j => Visit(j, q));
        q.GetAndSend(classDecl, c => c.TypeParameters, tp => VisitContainer(tp, q));
        q.GetAndSend(classDecl, c => c.PrimaryConstructor, ctor => VisitContainer(ctor, q));
        q.GetAndSend(classDecl, c => c.Extends, ext => VisitLeftPadded(ext, q));
        q.GetAndSend(classDecl, c => c.Implements, impl => VisitContainer(impl, q));
        q.GetAndSend(classDecl, c => c.Permits, perm => VisitContainer(perm, q));
        q.GetAndSend(classDecl, c => (J)c.Body, j => Visit(j, q));
        return classDecl;
    }

    private void VisitClassDeclarationKind(ClassDeclaration.Kind kind, RpcSendQueue q)
    {
        // preVisit() is not automatically called in this case
        PreVisit(kind, q);
        q.GetAndSendList(kind, k => k.Annotations, a => a.Id, j => Visit(j, q));
        q.GetAndSend(kind, k => k.Type);
    }

    public override J VisitContinue(Continue continueStmt, RpcSendQueue q)
    {
        q.GetAndSend(continueStmt, c => c.Label, label => Visit(label, q));
        return continueStmt;
    }

    public J VisitControlParentheses<T>(ControlParentheses<T> controlParens, RpcSendQueue q) where T : J
    {
        q.GetAndSend(controlParens, c => c.Tree, tree => VisitRightPadded(tree, q));
        return controlParens;
    }

    public override J VisitDeconstructionPattern(DeconstructionPattern deconstructionPattern, RpcSendQueue q)
    {
        q.GetAndSend(deconstructionPattern, dp => (J)dp.Deconstructor, dec => Visit(dec, q));
        q.GetAndSend(deconstructionPattern, dp => dp.Nested, nested => VisitContainer(nested, q));
        q.GetAndSend(deconstructionPattern, dp => AsRef(dp.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return deconstructionPattern;
    }

    public override J VisitDoWhileLoop(DoWhileLoop doWhileLoop, RpcSendQueue q)
    {
        q.GetAndSend(doWhileLoop, d => d.Body, body => VisitRightPadded(body, q));
        q.GetAndSend(doWhileLoop, d => d.Condition, cond => VisitLeftPadded(cond, q));
        return doWhileLoop;
    }

    public J VisitElse(If.Else anElse, RpcSendQueue q)
    {
        q.GetAndSend(anElse, e => e.Body, body => VisitRightPadded(body, q));
        return anElse;
    }

    public override J VisitEmpty(Empty empty, RpcSendQueue q)
    {
        return empty;
    }

    public override J VisitEnumValue(EnumValue enumValue, RpcSendQueue q)
    {
        q.GetAndSendList(enumValue, e => e.Annotations, a => a.Id, a => Visit(a, q));
        q.GetAndSend(enumValue, e => (J)e.Name, name => Visit(name, q));
        q.GetAndSend(enumValue, e => e.Initializer, init => VisitLeftPadded(init, q));
        return enumValue;
    }

    public override J VisitEnumValueSet(EnumValueSet enumValueSet, RpcSendQueue q)
    {
        q.GetAndSendList(enumValueSet, e => e.Enums, r => r.Element.Id, e => VisitRightPadded(e, q));
        q.GetAndSend(enumValueSet, e => e.TerminatedWithSemicolon);
        return enumValueSet;
    }

    public override J VisitExpressionStatement(ExpressionStatement expressionStatement, RpcSendQueue q)
    {
        q.GetAndSend(expressionStatement, es => (J)es.Expression, expr => Visit(expr, q));
        return expressionStatement;
    }

    public override J VisitFieldAccess(FieldAccess fieldAccess, RpcSendQueue q)
    {
        q.GetAndSend(fieldAccess, f => (J)f.Target, t => Visit(t, q));
        q.GetAndSend(fieldAccess, f => f.Name, name => VisitLeftPadded(name, q));
        q.GetAndSend(fieldAccess, f => AsRef(f.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return fieldAccess;
    }

    public override J VisitMemberReference(MemberReference memberRef, RpcSendQueue q)
    {
        q.GetAndSend(memberRef, m => m.Containing, containing => VisitRightPadded(containing, q));
        q.GetAndSend(memberRef, m => m.TypeParameters, typeParams => VisitContainer(typeParams!, q));
        q.GetAndSend(memberRef, m => m.Reference, reference => VisitLeftPadded(reference, q));
        q.GetAndSend(memberRef, m => AsRef(m.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        q.GetAndSend(memberRef, m => AsRef(m.MethodType), type => VisitType(GetValueNonNull<JavaType>(type), q));
        q.GetAndSend(memberRef, m => AsRef(m.VariableType), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return memberRef;
    }

    public J VisitForEachControl(ForEachLoop.Control control, RpcSendQueue q)
    {
        q.GetAndSend(control, c => c.Variable, variable => VisitRightPadded(variable, q));
        q.GetAndSend(control, c => c.Iterable, iterable => VisitRightPadded(iterable, q));
        return control;
    }

    public override J VisitForEachLoop(ForEachLoop forEachLoop, RpcSendQueue q)
    {
        q.GetAndSend(forEachLoop, f => (J)f.LoopControl, control => Visit(control, q));
        q.GetAndSend(forEachLoop, f => f.Body, body => VisitRightPadded(body, q));
        return forEachLoop;
    }

    public J VisitForControl(ForLoop.Control control, RpcSendQueue q)
    {
        q.GetAndSendList(control, c => c.Init, r => r.Element.Id, init => VisitRightPadded(init, q));
        q.GetAndSend(control, c => c.Condition, cond => VisitRightPadded(cond, q));
        q.GetAndSendList(control, c => c.Update, r => r.Element.Id, update => VisitRightPadded(update, q));
        return control;
    }

    public override J VisitForLoop(ForLoop forLoop, RpcSendQueue q)
    {
        q.GetAndSend(forLoop, f => (J)f.LoopControl, control => Visit(control, q));
        q.GetAndSend(forLoop, f => f.Body, body => VisitRightPadded(body, q));
        return forLoop;
    }

    public override J VisitIdentifier(Identifier identifier, RpcSendQueue q)
    {
        // C# model does not have Annotations on Identifier; send empty list for protocol compatibility
        q.GetAndSendList(identifier, _ => EmptyAnnotationList, a => a.Id, a => Visit(a, q));
        q.GetAndSend(identifier, i => i.SimpleName);
        q.GetAndSend(identifier, i => AsRef(i.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        // C# model does not have FieldType; send null for protocol compatibility
        q.GetAndSend(identifier, _ => (object?)null);
        return identifier;
    }

    public override J VisitIf(If iff, RpcSendQueue q)
    {
        q.GetAndSend(iff, i => (J)i.Condition, cond => Visit(cond, q));
        q.GetAndSend(iff, i => i.ThenPart, thenPart => VisitRightPadded(thenPart, q));
        q.GetAndSend(iff, i => i.ElsePart, elsePart => Visit(elsePart, q));
        return iff;
    }

    public override J VisitLambda(Lambda lambda, RpcSendQueue q)
    {
        q.GetAndSend(lambda, l => (J)l.Params, @params => Visit(@params, q));
        q.GetAndSend(lambda, l => l.Arrow, arrow => VisitSpace(arrow, q));
        q.GetAndSend(lambda, l => l.Body, body => Visit(body, q));
        q.GetAndSend(lambda, l => AsRef(l.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return lambda;
    }

    public J VisitLambdaParameters(Lambda.Parameters parameters, RpcSendQueue q)
    {
        q.GetAndSend(parameters, p => p.Parenthesized);
        q.GetAndSendList(parameters, p => p.Elements, r => r.Element.Id, param => VisitRightPadded(param, q));
        return parameters;
    }

    public override J VisitLiteral(Literal literal, RpcSendQueue q)
    {
        q.GetAndSend(literal, l => l.Value);
        q.GetAndSend(literal, l => l.ValueSource);
        q.GetAndSendList(literal, l => l.UnicodeEscapes,
            s => s.ValueSourceIndex + s.CodePoint,
            _ => { });
        q.GetAndSend(literal, l => AsRef(l.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return literal;
    }

    public override J VisitMethodDeclaration(MethodDeclaration method, RpcSendQueue q)
    {
        q.GetAndSendList(method, m => m.LeadingAnnotations, a => a.Id, a => Visit(a, q));
        q.GetAndSendList(method, m => m.Modifiers, m2 => m2.Id, m2 => Visit(m2, q));
        // Java stores type parameters as J.TypeParameters (a tree node), not JContainer directly.
        // Wrap the JContainer in TypeParameters for protocol compatibility.
        q.GetAndSend(method,
            m => m.TypeParameters != null ? TypeParameters.FromContainer(m.TypeParameters) : null,
            tp => Visit(tp, q));
        q.GetAndSend(method, m => m.ReturnTypeExpression, type => Visit(type, q));
        // C# model does not have name annotations; send empty list for protocol compatibility
        q.GetAndSendList(method, _ => EmptyAnnotationList, a => a.Id, a => Visit(a, q));
        q.GetAndSend(method, m => (J)m.Name, name => Visit(name, q));
        q.GetAndSend(method, m => m.Parameters, @params => VisitContainer(@params, q));
        q.GetAndSend(method, m => m.Throws, thr => VisitContainer(thr, q));
        q.GetAndSend(method, m => m.Body, body => Visit(body, q));
        q.GetAndSend(method, m => m.DefaultValue, def => VisitLeftPadded(def, q));
        q.GetAndSend(method, m => AsRef(m.MethodType), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return method;
    }

    public override J VisitMethodInvocation(MethodInvocation methodInvocation, RpcSendQueue q)
    {
        q.GetAndSend(methodInvocation, m => m.Select, select => VisitRightPadded(select, q));
        q.GetAndSend(methodInvocation, m => m.TypeParameters, typeParams => VisitContainer(typeParams, q));
        q.GetAndSend(methodInvocation, m => (J)m.Name, name => Visit(name, q));
        q.GetAndSend(methodInvocation, m => m.Arguments, args => VisitContainer(args, q));
        q.GetAndSend(methodInvocation, m => AsRef(m.MethodType), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return methodInvocation;
    }

    public J VisitModifier(Modifier modifier, RpcSendQueue q)
    {
        // Java model has a keyword string field; use stored keyword for LanguageExtension, derive for others
        q.GetAndSend(modifier, m => m.Keyword ?? GetModifierKeyword(m.Type));
        // C#-specific modifier types must be mapped to LanguageExtension for Java interop
        q.GetAndSend(modifier, m => MapModifierTypeForJava(m.Type));
        q.GetAndSendList(modifier, m => m.Annotations, a => a.Id, annot => Visit(annot, q));
        return modifier;
    }

    public override J VisitNewArray(NewArray newArray, RpcSendQueue q)
    {
        q.GetAndSend(newArray, n => n.TypeExpression, type => Visit(type, q));
        q.GetAndSendList(newArray, n => n.Dimensions, d => d.Id, dim => Visit(dim, q));
        q.GetAndSend(newArray, n => n.Initializer, init => VisitContainer(init, q));
        q.GetAndSend(newArray, n => AsRef(n.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return newArray;
    }

    public override J VisitNewClass(NewClass newClass, RpcSendQueue q)
    {
        q.GetAndSend(newClass, n => n.Enclosing, enclosing => VisitRightPadded(enclosing, q));
        q.GetAndSend(newClass, n => n.New, n2 => VisitSpace(n2, q));
        q.GetAndSend(newClass, n => n.Clazz, clazz => Visit(clazz, q));
        q.GetAndSend(newClass, n => n.Arguments, args => VisitContainer(args, q));
        q.GetAndSend(newClass, n => n.Body, body => Visit(body, q));
        q.GetAndSend(newClass, n => AsRef(n.ConstructorType), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return newClass;
    }

    public override J VisitPackage(Package pkg, RpcSendQueue q)
    {
        q.GetAndSend(pkg, p => (J)p.Expression.Element, expr => Visit(expr, q));
        q.GetAndSendList(pkg, p => p.Annotations, a => a.Id, a => Visit(a, q));
        return pkg;
    }

    public J VisitParentheses<T>(Parentheses<T> parentheses, RpcSendQueue q) where T : J
    {
        q.GetAndSend(parentheses, p => p.Tree, tree => VisitRightPadded(tree, q));
        return parentheses;
    }

    public override J VisitPrimitive(Primitive primitive, RpcSendQueue q)
    {
        q.GetAndSend(primitive, p => AsRef(new JavaType.Primitive(p.Kind)), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return primitive;
    }

    public override J VisitReturn(Return aReturn, RpcSendQueue q)
    {
        q.GetAndSend(aReturn, r => (J?)r.Expression, expr => Visit(expr!, q));
        return aReturn;
    }

    public override J VisitSwitch(Switch switchStmt, RpcSendQueue q)
    {
        q.GetAndSend(switchStmt, s => (J)s.Selector, selector => Visit(selector, q));
        q.GetAndSend(switchStmt, s => (J)s.Cases, cases => Visit(cases, q));
        return switchStmt;
    }

    public override J VisitSwitchExpression(SwitchExpression switchExpression, RpcSendQueue q)
    {
        q.GetAndSend(switchExpression, s => (J)s.Selector, selector => Visit(selector, q));
        q.GetAndSend(switchExpression, s => (J)s.Cases, cases => Visit(cases, q));
        q.GetAndSend(switchExpression, s => AsRef(s.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return switchExpression;
    }

    public override J VisitSynchronized(Synchronized synch, RpcSendQueue q)
    {
        q.GetAndSend(synch, s => (J)s.Lock, @lock => Visit(@lock, q));
        q.GetAndSend(synch, s => (J)s.Body, body => Visit(body, q));
        return synch;
    }

    public override J VisitTernary(Ternary ternary, RpcSendQueue q)
    {
        q.GetAndSend(ternary, t => (J)t.Condition, cond => Visit(cond, q));
        q.GetAndSend(ternary, t => t.TruePart, truePart => VisitLeftPadded(truePart, q));
        q.GetAndSend(ternary, t => t.FalsePart, falsePart => VisitLeftPadded(falsePart, q));
        q.GetAndSend(ternary, t => AsRef(t.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return ternary;
    }

    public override J VisitThrow(Throw throwStmt, RpcSendQueue q)
    {
        q.GetAndSend(throwStmt, t => (J)t.Exception, ex => Visit(ex, q));
        return throwStmt;
    }

    public override J VisitTry(Try tryStmt, RpcSendQueue q)
    {
        q.GetAndSend(tryStmt, t => t.Resources, resources => VisitContainer(resources, q));
        q.GetAndSend(tryStmt, t => (J)t.Body, body => Visit(body, q));
        q.GetAndSendList(tryStmt, t => t.Catches, c => c.Id, c => Visit(c, q));
        q.GetAndSend(tryStmt, t => t.Finally, fin => VisitLeftPadded(fin, q));
        return tryStmt;
    }

    public override J VisitTypeCast(TypeCast typeCast, RpcSendQueue q)
    {
        q.GetAndSend(typeCast, t => (J)t.Clazz, clazz => Visit(clazz, q));
        q.GetAndSend(typeCast, t => (J)t.Expression, expr => Visit(expr, q));
        return typeCast;
    }

    public virtual J VisitTypeParameters(TypeParameters typeParameters, RpcSendQueue q)
    {
        q.GetAndSendList(typeParameters, t => t.Annotations, a => a.Id, annot => Visit(annot, q));
        q.GetAndSendList(typeParameters, t => t.Params, r => (object)r.Element.Id,
            mod => VisitRightPadded(mod, q));
        return typeParameters;
    }

    public override J VisitTypeParameter(TypeParameter typeParam, RpcSendQueue q)
    {
        q.GetAndSendList(typeParam, t => t.Annotations, a => a.Id, annot => Visit(annot, q));
        q.GetAndSendList(typeParam, t => t.Modifiers, m => m.Id, mod => Visit(mod, q));
        q.GetAndSend(typeParam, t => (J)t.Name, name => Visit(name, q));
        q.GetAndSend(typeParam, t => t.Bounds, bounds => VisitContainer(bounds, q));
        return typeParam;
    }

    public override J VisitUnary(Unary unary, RpcSendQueue q)
    {
        q.GetAndSend(unary, u => u.Operator, op => VisitLeftPadded(op, q));
        q.GetAndSend(unary, u => (J)u.Expression, expr => Visit(expr, q));
        q.GetAndSend(unary, u => AsRef(u.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return unary;
    }

    public J VisitVariable(NamedVariable variable, RpcSendQueue q)
    {
        q.GetAndSend(variable, v => (J)v.Name, decl => Visit(decl, q));
        q.GetAndSendList(variable, v => v.DimensionsAfterName,
            l => l.Element.ToString()!, dim => VisitLeftPadded(dim, q));
        q.GetAndSend(variable, v => v.Initializer, init => VisitLeftPadded(init, q));
        q.GetAndSend(variable, v => AsRef(v.Type), type => VisitType(GetValueNonNull<JavaType>(type), q));
        return variable;
    }

    public override J VisitVariableDeclarations(VariableDeclarations variableDeclarations, RpcSendQueue q)
    {
        q.GetAndSendList(variableDeclarations, v => v.LeadingAnnotations, m => m.Id, a => Visit(a, q));
        q.GetAndSendList(variableDeclarations, v => v.Modifiers, m => m.Id, m => Visit(m, q));
        q.GetAndSend(variableDeclarations, v => v.TypeExpression, type => Visit(type, q));
        q.GetAndSend(variableDeclarations, v => v.Varargs, space => VisitSpace(GetValueNonNull<Space>(space), q));
        q.GetAndSendList(variableDeclarations, v => v.Variables, r => r.Element.Id, v => VisitRightPadded(v, q));
        return variableDeclarations;
    }

    public override J VisitWhileLoop(WhileLoop whileLoop, RpcSendQueue q)
    {
        q.GetAndSend(whileLoop, w => (J)w.Condition, cond => Visit(cond, q));
        q.GetAndSend(whileLoop, w => w.Body, body => VisitRightPadded(body, q));
        return whileLoop;
    }

    public J VisitCatch(Try.Catch tryCatch, RpcSendQueue q)
    {
        q.GetAndSend(tryCatch, c => (J)c.Parameter, param => Visit(param, q));
        q.GetAndSend(tryCatch, c => (J)c.Body, body => Visit(body, q));
        return tryCatch;
    }

    // Helper methods

    public virtual void VisitLeftPadded<T>(JLeftPadded<T> left, RpcSendQueue q)
    {
        q.GetAndSend(left, l => l.Before, space => VisitSpace(GetValueNonNull<Space>(space), q));
        var element = left.Element;
        if (element is J j)
        {
            q.GetAndSend(left, l => l.Element, elem => Visit((J)(object)elem!, q));
        }
        else if (element is Space)
        {
            q.GetAndSend(left, l => l.Element, space => VisitSpace(GetValueNonNull<Space>(space), q));
        }
        else
        {
            q.GetAndSend(left, l => l.Element);
        }
        // C# JLeftPadded does not have Markers; send empty for protocol compatibility
        q.GetAndSend(left, _ => Markers.Empty);
    }

    public virtual void VisitRightPadded<T>(JRightPadded<T> right, RpcSendQueue q)
    {
        var element = right.Element;
        if (element is J j)
        {
            q.GetAndSend(right, r => r.Element, elem => Visit((J)(object)elem!, q));
        }
        else if (element is Space)
        {
            q.GetAndSend(right, r => r.Element, space => VisitSpace(GetValueNonNull<Space>(space), q));
        }
        else
        {
            q.GetAndSend(right, r => r.Element);
        }
        q.GetAndSend(right, r => r.After, space => VisitSpace(GetValueNonNull<Space>(space), q));
        q.GetAndSend(right, r => r.Markers);
    }

    public virtual void VisitContainer<TJ>(JContainer<TJ> container, RpcSendQueue q) where TJ : J
    {
        q.GetAndSend(container, c => c.Before, space => VisitSpace(GetValueNonNull<Space>(space), q));
        q.GetAndSendList(container, c => c.Elements, e =>
        {
            if (e.Element == null)
                throw new NullReferenceException(
                    $"Null element in JContainer<{typeof(TJ).Name}>, cursor path: {BuildCursorPath()}");
            return e.Element.Id;
        }, e => VisitRightPadded(e, q));
        q.GetAndSend(container, c => c.Markers);
    }

    private string BuildCursorPath()
    {
        var parts = new List<string>();
        var c = Cursor;
        while (c != null && !c.IsRoot)
        {
            parts.Add(c.Value?.GetType().Name ?? "null");
            c = c.Parent;
        }
        parts.Reverse();
        return string.Join(" > ", parts);
    }

    public virtual void VisitSpace(Space space, RpcSendQueue q)
    {
        q.GetAndSendList(space, s => s.Comments,
            c =>
            {
                if (c is TextComment tc)
                {
                    return tc.Text + c.Suffix;
                }
                throw new ArgumentException($"Unexpected comment type {c.GetType().Name}");
            },
            c =>
            {
                if (c is TextComment tc)
                {
                    q.GetAndSend(tc, t => t.Multiline);
                    q.GetAndSend(tc, t => t.Text);
                }
                else
                {
                    throw new ArgumentException($"Unexpected comment type {c.GetType().Name}");
                }
                q.GetAndSend(c, cm => cm.Suffix);
                // C# Comment does not have Markers; send empty Markers for protocol compatibility
                q.GetAndSend(c, _ => Markers.Empty);
            });
        q.GetAndSend(space, s => s.Whitespace);
    }

    public virtual JavaType? VisitType(JavaType? javaType, RpcSendQueue q)
    {
        if (javaType == null) return null;

        switch (javaType)
        {
            case JavaType.Annotation annotation:
                q.GetAndSend(annotation, a => AsRef(a.AnnotationType),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                break;

            case JavaType.MultiCatch multiCatch:
                q.GetAndSendListAsRef(multiCatch, m => m.ThrowableTypes,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;

            case JavaType.Intersection intersection:
                q.GetAndSendListAsRef(intersection, i => i.Bounds,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;

            case JavaType.Class cls:
                q.GetAndSend(cls, c => c.FlagsBitMap);
                q.GetAndSend(cls, c => c.ClassKind);
                q.GetAndSend(cls, c => c.FullyQualifiedName);
                q.GetAndSendListAsRef(cls, c => c.TypeParameters,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSend(cls, c => AsRef(c.Supertype),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSend(cls, c => AsRef(c.OwningClass),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSendListAsRef(cls, c => c.Annotations,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSendListAsRef(cls, c => c.Interfaces,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSendListAsRef(cls, c => c.Members,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSendListAsRef(cls, c => c.Methods,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;

            case JavaType.Parameterized parameterized:
                q.GetAndSend(parameterized, p => AsRef(p.Type),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSendListAsRef(parameterized, p => p.TypeParameters,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;

            case JavaType.GenericTypeVariable generic:
                q.GetAndSend(generic, g => g.Name);
                q.GetAndSend(generic, g => g.Variance.ToString().ToUpper());
                q.GetAndSendListAsRef(generic, g => g.Bounds,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;

            case JavaType.Array array:
                q.GetAndSend(array, a => AsRef(a.ElemType),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSendListAsRef(array, a => a.Annotations,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;

            case JavaType.Primitive primitive:
                q.GetAndSend(primitive, p => GetPrimitiveKeyword(p.Kind));
                break;

            case JavaType.Method method:
                q.GetAndSend(method, m => AsRef(m.DeclaringType),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSend(method, m => m.Name);
                q.GetAndSend(method, m => m.FlagsBitMap);
                q.GetAndSend(method, m => AsRef(m.ReturnType),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSendList(method, m => m.ParameterNames,
                    s => s, null);
                q.GetAndSendListAsRef(method, m => m.ParameterTypes,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSendListAsRef(method, m => m.ThrownExceptions,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSendListAsRef(method, m => m.Annotations,
                    t => TypeSignature(t), t => VisitType(t, q));
                q.GetAndSendList(method, m => m.DefaultValue,
                    s => s, null);
                q.GetAndSendList(method, m => m.DeclaredFormalTypeNames,
                    s => s, null);
                break;

            case JavaType.Variable variable:
                q.GetAndSend(variable, v => v.Name);
                q.GetAndSend(variable, v => AsRef(v.Owner),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSend(variable, v => AsRef(v.Type),
                    t => VisitType(GetValueNonNull<JavaType>(t), q));
                q.GetAndSendListAsRef(variable, v => v.Annotations,
                    t => TypeSignature(t), t => VisitType(t, q));
                break;
        }

        return javaType;
    }

    /// <summary>
    /// Produces a signature string for a JavaType, used as the identity function
    /// for GetAndSendListAsRef. Matches DefaultJavaTypeSignatureBuilder.java.
    /// </summary>
    [ThreadStatic] private static HashSet<JavaType>? _typeSignatureStack;

    private static object TypeSignature(JavaType type)
    {
        _typeSignatureStack ??= new HashSet<JavaType>(ReferenceEqualityComparer.Instance);
        if (!_typeSignatureStack.Add(type))
            return "{circular}";
        try
        {
            return type switch
            {
                JavaType.Primitive p => p.Keyword,
                JavaType.Class c => c.FullyQualifiedName,
                JavaType.Parameterized p => TypeSignature(p.Type!) + "<" +
                    string.Join(", ", (p.TypeParameters ?? []).Select(TypeSignature)) + ">",
                JavaType.GenericTypeVariable g => "Generic{" + g.Name + "}",
                JavaType.Array a => TypeSignature(a.ElemType!) + "[]",
                JavaType.Method m => TypeSignature(m.DeclaringType!) +
                    "{name=" + m.Name + ",return=" + TypeSignature(m.ReturnType!) +
                    ",parameters=[" + string.Join(",", (m.ParameterTypes ?? []).Select(TypeSignature)) + "]}",
                JavaType.Variable v => TypeSignature(v.Owner!) +
                    "{name=" + v.Name + ",type=" + TypeSignature(v.Type!) + "}",
                JavaType.MultiCatch mc => string.Join("|",
                    (mc.ThrowableTypes ?? []).Select(TypeSignature)),
                JavaType.Intersection i => string.Join(" & ",
                    (i.Bounds ?? []).Select(TypeSignature)),
                JavaType.Annotation a => "@" + (a.AnnotationType != null ? TypeSignature(a.AnnotationType) : "{undefined}"),
                JavaType.Unknown => "{undefined}",
                _ => "{undefined}"
            };
        }
        finally
        {
            _typeSignatureStack.Remove(type);
        }
    }

    private static string GetPrimitiveKeyword(JavaType.PrimitiveKind kind) => kind switch
    {
        JavaType.PrimitiveKind.Boolean => "boolean",
        JavaType.PrimitiveKind.Byte => "byte",
        JavaType.PrimitiveKind.Char => "char",
        JavaType.PrimitiveKind.Double => "double",
        JavaType.PrimitiveKind.Float => "float",
        JavaType.PrimitiveKind.Int => "int",
        JavaType.PrimitiveKind.Long => "long",
        JavaType.PrimitiveKind.Short => "short",
        JavaType.PrimitiveKind.Void => "void",
        JavaType.PrimitiveKind.String => "String",
        JavaType.PrimitiveKind.Null => "null",
        JavaType.PrimitiveKind.None => "none",
        _ => throw new ArgumentException($"Unknown primitive kind: {kind}")
    };

    // Utility methods

    private static string GetModifierKeyword(Modifier.ModifierType type) => type switch
    {
        Modifier.ModifierType.Default => "default",
        Modifier.ModifierType.Public => "public",
        Modifier.ModifierType.Private => "private",
        Modifier.ModifierType.Protected => "protected",
        Modifier.ModifierType.Internal => "internal",
        Modifier.ModifierType.Static => "static",
        Modifier.ModifierType.Abstract => "abstract",
        Modifier.ModifierType.Final => "final",
        Modifier.ModifierType.Sealed => "sealed",
        Modifier.ModifierType.Native => "native",
        Modifier.ModifierType.Strictfp => "strictfp",
        Modifier.ModifierType.Synchronized => "synchronized",
        Modifier.ModifierType.Transient => "transient",
        Modifier.ModifierType.Volatile => "volatile",
        Modifier.ModifierType.Async => "async",
        Modifier.ModifierType.Override => "override",
        Modifier.ModifierType.Virtual => "virtual",
        Modifier.ModifierType.Readonly => "readonly",
        Modifier.ModifierType.Const => "const",
        Modifier.ModifierType.New => "new",
        Modifier.ModifierType.Extern => "extern",
        Modifier.ModifierType.Unsafe => "unsafe",
        Modifier.ModifierType.Partial => "partial",
        Modifier.ModifierType.Ref => "ref",
        Modifier.ModifierType.Out => "out",
        Modifier.ModifierType.In => "in",
        Modifier.ModifierType.LanguageExtension => "",
        _ => type.ToString().ToLowerInvariant()
    };

    /// <summary>
    /// Maps C#-specific modifier types to LanguageExtension for Java interop.
    /// Java's J.Modifier.Type enum only contains modifiers shared across languages.
    /// C#-only modifiers (partial, internal, virtual, override, etc.) are sent as
    /// LanguageExtension with the actual keyword preserved in the keyword field.
    /// </summary>
    private static Modifier.ModifierType MapModifierTypeForJava(Modifier.ModifierType type) => type switch
    {
        // These exist in Java's J.Modifier.Type enum
        Modifier.ModifierType.Default => type,
        Modifier.ModifierType.Public => type,
        Modifier.ModifierType.Protected => type,
        Modifier.ModifierType.Private => type,
        Modifier.ModifierType.Abstract => type,
        Modifier.ModifierType.Static => type,
        Modifier.ModifierType.Final => type,
        Modifier.ModifierType.Sealed => type,
        Modifier.ModifierType.Transient => type,
        Modifier.ModifierType.Volatile => type,
        Modifier.ModifierType.Synchronized => type,
        Modifier.ModifierType.Native => type,
        Modifier.ModifierType.Strictfp => type,
        Modifier.ModifierType.Async => type,
        Modifier.ModifierType.LanguageExtension => type,
        // C#-specific modifiers map to LanguageExtension
        _ => Modifier.ModifierType.LanguageExtension
    };
}
