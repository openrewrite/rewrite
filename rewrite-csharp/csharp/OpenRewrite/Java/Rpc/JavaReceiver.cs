using Rewrite.Core;
using Rewrite.Core.Rpc;

namespace Rewrite.Java.Rpc;

/// <summary>
/// Deserializes Java AST elements via the RPC protocol.
/// Mirrors JavaSender field-by-field in reverse.
/// </summary>
public class JavaReceiver : JavaVisitor<RpcReceiveQueue>
{
    // PreVisit data stored on a stack so nested Visit calls don't overwrite parent data.
    // In Java, preVisit() modifies and returns the tree before visitXxx is called,
    // so method-chaining captures the values early. In C#, record `with` expressions
    // evaluate all properties at the end, after child visits would overwrite shared fields.
    private readonly Stack<(Guid Id, Space Prefix, Markers Markers)> _pvStack = new();
    protected Guid _pvId => _pvStack.Peek().Id;
    protected Space _pvPrefix => _pvStack.Peek().Prefix;
    protected Markers _pvMarkers => _pvStack.Peek().Markers;

    public override J? Visit(Tree? tree, RpcReceiveQueue q)
    {
        if (tree == null) return null;

        Cursor = new Cursor(Cursor, tree);

        if (tree is J j)
        {
            ConsumePreVisit(j, q);
        }

        var result = tree switch
        {
            Annotation ann => VisitAnnotation(ann, q),
            Block blk => VisitBlock(blk, q),
            ClassDeclaration cd => VisitClassDeclaration(cd, q),
            EnumValueSet evs => VisitEnumValueSet(evs, q),
            EnumValue ev => VisitEnumValue(ev, q),
            MethodDeclaration md => VisitMethodDeclaration(md, q),
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
            _ => throw new InvalidOperationException($"Unknown J tree type: {tree.GetType().Name}")
        };

        PopPreVisit();
        Cursor = Cursor.Parent!;
        return result;
    }

    /// <summary>
    /// Consumes Id, Prefix, and Markers from the queue and pushes them
    /// onto the stack for use by the dispatched VisitXxx method.
    /// </summary>
    protected void ConsumePreVisit(J j, RpcReceiveQueue q)
    {
        var id = q.ReceiveAndGet<Guid, string>(j.Id, Guid.Parse);
        var prefix = q.Receive(j.Prefix, space => VisitSpace(space, q))!;
        var markers = q.Receive(j.Markers)!;
        _pvStack.Push((id, prefix, markers));
    }

    protected void PopPreVisit() => _pvStack.Pop();

    public override J VisitAnnotation(Annotation annotation, RpcReceiveQueue q)
    {
        var annotationType = q.Receive((J)annotation.AnnotationType, el => (J)VisitNonNull(el, q));
        var arguments = q.Receive(annotation.Arguments, c => VisitContainer(c, q));
        return annotation with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            AnnotationType = (NameTree)annotationType!, Arguments = arguments };
    }

    public override J VisitArrayAccess(ArrayAccess arrayAccess, RpcReceiveQueue q)
    {
        var indexed = q.Receive((J)arrayAccess.Indexed, el => (J)VisitNonNull(el, q));
        var dimension = q.Receive((J)arrayAccess.Dimension, el => (J)VisitNonNull(el, q));
        return arrayAccess with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Indexed = (Expression)indexed!, Dimension = (ArrayDimension)dimension! };
    }

    public override J VisitArrayDimension(ArrayDimension arrayDimension, RpcReceiveQueue q)
    {
        var index = q.Receive(arrayDimension.Index, rp => VisitRightPadded(rp, q));
        return arrayDimension with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Index = index! };
    }

    public override J VisitLabel(Label label, RpcReceiveQueue q)
    {
        var labelName = q.Receive(label.LabelName, rp => VisitRightPadded(rp, q));
        var statement = q.Receive(label.Statement, s => (Statement)VisitNonNull(s, q));
        return label with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            LabelName = labelName!, Statement = statement! };
    }

    public override J VisitInstanceOf(InstanceOf instanceOf, RpcReceiveQueue q)
    {
        var expression = q.Receive(instanceOf.Expression, rp => VisitRightPadded(rp, q));
        var clazz = q.Receive(instanceOf.Clazz, el => (J)VisitNonNull(el, q));
        var pattern = q.Receive(instanceOf.Pattern, el => (J)VisitNonNull(el, q));
        var type = q.Receive(instanceOf.Type, t => VisitType(t, q)!);
        var modifier = q.Receive(instanceOf.InstanceOfModifier, m => (Modifier)VisitNonNull(m, q));
        return instanceOf with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Expression = expression!, Clazz = clazz!, Pattern = pattern, Type = type, InstanceOfModifier = modifier };
    }

    public override J VisitNullableType(NullableType nullableType, RpcReceiveQueue q)
    {
        var annotations = q.ReceiveList(nullableType.Annotations, a => (Annotation)VisitNonNull(a, q));
        var typeTreePadded = q.Receive(nullableType.TypeTreePadded, t => VisitRightPadded(t, q));
        return nullableType with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Annotations = annotations!, TypeTreePadded = typeTreePadded! };
    }

    public override J VisitParameterizedType(ParameterizedType type, RpcReceiveQueue q)
    {
        var clazz = q.Receive((J)type.Clazz, el => (J)VisitNonNull(el, q));
        var typeParameters = q.Receive(type.TypeParameters, c => VisitContainer(c, q));
        var javaType = q.Receive(type.Type, t => VisitType(t, q)!);
        return type with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Clazz = (NameTree)clazz!, TypeParameters = typeParameters, Type = javaType };
    }

    public override J VisitArrayType(ArrayType arrayType, RpcReceiveQueue q)
    {
        var elementType = q.Receive((J)arrayType.ElementType, el => (J)VisitNonNull(el, q));
        var annotations = q.ReceiveList(arrayType.Annotations, a => (Annotation)VisitNonNull(a, q));
        var dimension = q.Receive(arrayType.Dimension, lp => VisitLeftPadded(lp, q));
        var type = q.Receive(arrayType.Type, t => VisitType(t, q)!);
        return arrayType with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            ElementType = (TypeTree)elementType!, Annotations = annotations!, Dimension = dimension, Type = type };
    }

    public override J VisitAssignment(Assignment assignment, RpcReceiveQueue q)
    {
        var variable = q.Receive((J)assignment.Variable, el => (J)VisitNonNull(el, q));
        var assignmentValue = q.Receive(assignment.AssignmentValue, lp => VisitLeftPadded(lp, q));
        var type = q.Receive(assignment.Type, t => VisitType(t, q)!);
        return assignment with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Variable = (Expression)variable!, AssignmentValue = assignmentValue!, Type = type };
    }

    public override J VisitAssignmentOperation(AssignmentOperation assignmentOperation, RpcReceiveQueue q)
    {
        var variable = q.Receive((J)assignmentOperation.Variable, el => (J)VisitNonNull(el, q));
        var @operator = q.Receive(assignmentOperation.Operator, lp => VisitLeftPadded(lp, q));
        var assignmentValue = q.Receive((J)assignmentOperation.AssignmentValue, el => (J)VisitNonNull(el, q));
        var type = q.Receive(assignmentOperation.Type, t => VisitType(t, q)!);
        return assignmentOperation with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Variable = (Expression)variable!, Operator = @operator!, AssignmentValue = (Expression)assignmentValue!, Type = type };
    }

    public override J VisitBinary(Binary binary, RpcReceiveQueue q)
    {
        var left = q.Receive((J)binary.Left, el => (J)VisitNonNull(el, q));
        var @operator = q.Receive(binary.Operator, lp => VisitLeftPadded(lp, q));
        var right = q.Receive((J)binary.Right, el => (J)VisitNonNull(el, q));
        var type = q.Receive(binary.Type, t => VisitType(t, q)!);
        return binary with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Left = (Expression)left!, Operator = @operator!, Right = (Expression)right!, Type = type };
    }

    public override J VisitBlock(Block block, RpcReceiveQueue q)
    {
        var @static = q.Receive(block.Static, rp => VisitRightPadded(rp, q));
        var statements = q.ReceiveList(block.Statements, rp => VisitRightPadded(rp, q));
        var end = q.Receive(block.End, space => VisitSpace(space, q));
        return block with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Static = @static!, Statements = statements!, End = end! };
    }

    public override J VisitBreak(Break breakStmt, RpcReceiveQueue q)
    {
        var label = q.Receive((J?)breakStmt.Label, el => (J)VisitNonNull(el!, q));
        return breakStmt with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Label = (Identifier?)label };
    }

    public override J VisitCase(Case caseStmt, RpcReceiveQueue q)
    {
        var caseKind = q.Receive(caseStmt.CaseKind);
        var caseLabels = q.Receive(caseStmt.CaseLabels, c => VisitContainer(c, q));
        var statements = q.Receive(caseStmt.Statements, c => VisitContainer(c, q));
        var body = q.Receive(caseStmt.Body, rp => VisitRightPadded(rp, q));
        var guard = q.Receive((J?)caseStmt.Guard, el => (J)VisitNonNull(el!, q));
        return caseStmt with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            CaseKind = caseKind, CaseLabels = caseLabels!, Statements = statements!, Body = body, Guard = (Expression?)guard };
    }

    public override J VisitClassDeclaration(ClassDeclaration classDecl, RpcReceiveQueue q)
    {
        var leadingAnnotations = q.ReceiveList(classDecl.LeadingAnnotations, a => (Annotation)VisitNonNull(a, q));
        var modifiers = q.ReceiveList(classDecl.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var kind = q.Receive((J)classDecl.ClassKind, el => (J)VisitClassDeclarationKind((ClassDeclaration.Kind)el, q));
        var name = q.Receive((J)classDecl.Name, el => (J)VisitNonNull(el, q));
        var typeParameters = q.Receive(classDecl.TypeParameters, c => VisitContainer(c, q));
        var primaryConstructor = q.Receive(classDecl.PrimaryConstructor, c => VisitContainer(c, q));
        var extends_ = q.Receive(classDecl.Extends, lp => VisitLeftPadded(lp, q));
        var implements_ = q.Receive(classDecl.Implements, c => VisitContainer(c, q));
        var permits = q.Receive(classDecl.Permits, c => VisitContainer(c, q));
        var body = q.Receive((J)classDecl.Body, el => (J)VisitNonNull(el, q));
        return classDecl with
        {
            Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            LeadingAnnotations = leadingAnnotations!,
            Modifiers = modifiers!,
            ClassKind = (ClassDeclaration.Kind)kind!,
            Name = (Identifier)name!,
            TypeParameters = typeParameters,
            PrimaryConstructor = primaryConstructor,
            Extends = extends_,
            Implements = implements_,
            Permits = permits,
            Body = (Block)body!
        };
    }

    private J VisitClassDeclarationKind(ClassDeclaration.Kind kind, RpcReceiveQueue q)
    {
        ConsumePreVisit(kind, q);
        var annotations = q.ReceiveList(kind.Annotations, a => (Annotation)VisitNonNull(a, q));
        var type = q.Receive(kind.Type);
        var result = kind with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Annotations = annotations!, Type = type };
        PopPreVisit();
        return result;
    }

    public override J VisitContinue(Continue continueStmt, RpcReceiveQueue q)
    {
        var label = q.Receive((J?)continueStmt.Label, el => (J)VisitNonNull(el!, q));
        return continueStmt with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Label = (Identifier?)label };
    }

    public J VisitControlParentheses<T>(ControlParentheses<T> controlParens, RpcReceiveQueue q) where T : J
    {
        var tree = q.Receive(controlParens.Tree, rp => VisitRightPadded(rp, q));
        return controlParens with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Tree = tree! };
    }

    public override J VisitDeconstructionPattern(DeconstructionPattern deconstructionPattern, RpcReceiveQueue q)
    {
        var deconstructor = q.Receive((J)deconstructionPattern.Deconstructor, el => (J)VisitNonNull(el, q));
        var nested = q.Receive(deconstructionPattern.Nested, c => VisitContainer(c, q));
        var type = q.Receive(deconstructionPattern.Type, t => VisitType(t, q)!);
        return deconstructionPattern with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Deconstructor = (Expression)deconstructor!, Nested = nested!, Type = type };
    }

    public override J VisitDoWhileLoop(DoWhileLoop doWhileLoop, RpcReceiveQueue q)
    {
        var body = q.Receive(doWhileLoop.Body, rp => VisitRightPadded(rp, q));
        var condition = q.Receive(doWhileLoop.Condition, lp => VisitLeftPadded(lp, q));
        return doWhileLoop with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Body = body!, Condition = condition! };
    }

    public J VisitElse(If.Else anElse, RpcReceiveQueue q)
    {
        var body = q.Receive(anElse.Body, rp => VisitRightPadded(rp, q));
        return anElse with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Body = body! };
    }

    public override J VisitEmpty(Empty empty, RpcReceiveQueue q)
    {
        return empty with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers };
    }

    public override J VisitEnumValue(EnumValue enumValue, RpcReceiveQueue q)
    {
        var annotations = q.ReceiveList(enumValue.Annotations, a => (Annotation)VisitNonNull(a, q));
        var name = q.Receive((J)enumValue.Name, el => (J)VisitNonNull(el, q));
        var initializer = q.Receive(enumValue.Initializer, lp => VisitLeftPadded(lp, q));
        return enumValue with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Annotations = annotations!, Name = (Identifier)name!, Initializer = initializer };
    }

    public override J VisitEnumValueSet(EnumValueSet enumValueSet, RpcReceiveQueue q)
    {
        var enums = q.ReceiveList(enumValueSet.Enums, rp => VisitRightPadded(rp, q));
        var terminatedWithSemicolon = q.Receive(enumValueSet.TerminatedWithSemicolon);
        return enumValueSet with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Enums = enums!, TerminatedWithSemicolon = terminatedWithSemicolon };
    }

    public override J VisitExpressionStatement(ExpressionStatement expressionStatement, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)expressionStatement.Expression, el => (J)VisitNonNull(el, q));
        // ExpressionStatement delegates Prefix/Markers to Expression — only set Id
        return expressionStatement with { Id = _pvId, Expression = (Expression)expression! };
    }

    public override J VisitFieldAccess(FieldAccess fieldAccess, RpcReceiveQueue q)
    {
        var target = q.Receive((J)fieldAccess.Target, el => (J)VisitNonNull(el, q));
        var name = q.Receive(fieldAccess.Name, lp => VisitLeftPadded(lp, q));
        var type = q.Receive(fieldAccess.Type, t => VisitType(t, q)!);
        return fieldAccess with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Target = (Expression)target!, Name = name!, Type = type };
    }

    public J VisitForEachControl(ForEachLoop.Control control, RpcReceiveQueue q)
    {
        var variable = q.Receive(control.Variable, rp => VisitRightPadded(rp, q));
        var iterable = q.Receive(control.Iterable, rp => VisitRightPadded(rp, q));
        return control with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Variable = variable!, Iterable = iterable! };
    }

    public override J VisitForEachLoop(ForEachLoop forEachLoop, RpcReceiveQueue q)
    {
        var loopControl = q.Receive((J)forEachLoop.LoopControl, el => (J)VisitNonNull(el, q));
        var body = q.Receive(forEachLoop.Body, rp => VisitRightPadded(rp, q));
        return forEachLoop with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            LoopControl = (ForEachLoop.Control)loopControl!, Body = body! };
    }

    public J VisitForControl(ForLoop.Control control, RpcReceiveQueue q)
    {
        var init = q.ReceiveList(control.Init, rp => VisitRightPadded(rp, q));
        var condition = q.Receive(control.Condition, rp => VisitRightPadded(rp, q));
        var update = q.ReceiveList(control.Update, rp => VisitRightPadded(rp, q));
        return control with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Init = init!, Condition = condition!, Update = update! };
    }

    public override J VisitForLoop(ForLoop forLoop, RpcReceiveQueue q)
    {
        var loopControl = q.Receive((J)forLoop.LoopControl, el => (J)VisitNonNull(el, q));
        var body = q.Receive(forLoop.Body, rp => VisitRightPadded(rp, q));
        return forLoop with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            LoopControl = (ForLoop.Control)loopControl!, Body = body! };
    }

    public override J VisitIdentifier(Identifier identifier, RpcReceiveQueue q)
    {
        // C# model does not have Annotations on Identifier; consume and discard
        q.ReceiveList<Annotation>([], el => (Annotation)VisitNonNull(el, q));
        var simpleName = q.Receive(identifier.SimpleName);
        var type = q.Receive(identifier.Type, t => VisitType(t, q)!);
        // C# model does not have FieldType; consume and discard
        q.Receive<object?>(null);
        return identifier with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            SimpleName = simpleName!, Type = type };
    }

    public override J VisitIf(If iff, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)iff.Condition, el => (J)VisitNonNull(el, q));
        var thenPart = q.Receive(iff.ThenPart, rp => VisitRightPadded(rp, q));
        var elsePart = q.Receive((J?)iff.ElsePart, el => (J)VisitNonNull(el!, q));
        return iff with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Condition = (ControlParentheses<Expression>)condition!, ThenPart = thenPart!, ElsePart = (If.Else?)elsePart };
    }

    public override J VisitLambda(Lambda lambda, RpcReceiveQueue q)
    {
        var @params = q.Receive((J)lambda.Params, el => (J)VisitNonNull(el, q));
        var arrow = q.Receive(lambda.Arrow, space => VisitSpace(space, q));
        var body = q.Receive(lambda.Body, el => (J)VisitNonNull(el, q));
        var type = q.Receive(lambda.Type, t => VisitType(t, q)!);
        return lambda with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Params = (Lambda.Parameters)@params!, Arrow = arrow!, Body = body!, Type = type };
    }

    public J VisitLambdaParameters(Lambda.Parameters parameters, RpcReceiveQueue q)
    {
        var parenthesized = q.Receive(parameters.Parenthesized);
        var elements = q.ReceiveList(parameters.Elements, rp => VisitRightPadded(rp, q));
        return parameters with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Parenthesized = parenthesized, Elements = elements! };
    }

    public override J VisitLiteral(Literal literal, RpcReceiveQueue q)
    {
        var value = q.Receive(literal.Value);
        var valueSource = q.Receive(literal.ValueSource);
        var unicodeEscapes = q.ReceiveList(literal.UnicodeEscapes, _ => _);
        var type = q.Receive(literal.Type, t => (JavaType.Primitive)VisitType(t, q)!);
        return literal with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Value = value, ValueSource = valueSource, UnicodeEscapes = unicodeEscapes, Type = type };
    }

    public override J VisitMethodDeclaration(MethodDeclaration method, RpcReceiveQueue q)
    {
        var leadingAnnotations = q.ReceiveList(method.LeadingAnnotations, a => (Annotation)VisitNonNull(a, q));
        var modifiers = q.ReceiveList(method.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeParameters = q.Receive(method.TypeParameters, c => VisitContainer(c, q));
        var returnTypeExpression = q.Receive((J?)method.ReturnTypeExpression, el => (J)VisitNonNull(el!, q));
        // C# model does not have name annotations; consume and discard
        q.ReceiveList<Annotation>([], el => (Annotation)VisitNonNull(el, q));
        var name = q.Receive((J)method.Name, el => (J)VisitNonNull(el, q));
        var parameters = q.Receive(method.Parameters, c => VisitContainer(c, q));
        var throws_ = q.Receive(method.Throws, c => VisitContainer(c, q));
        var body = q.Receive((J?)method.Body, el => (J)VisitNonNull(el!, q));
        var defaultValue = q.Receive(method.DefaultValue, lp => VisitLeftPadded(lp, q));
        var methodType = q.Receive(method.MethodType, t => (JavaType.Method)VisitType(t, q)!);
        return method with
        {
            Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            LeadingAnnotations = leadingAnnotations!,
            Modifiers = modifiers!,
            TypeParameters = typeParameters,
            ReturnTypeExpression = (TypeTree?)returnTypeExpression,
            Name = (Identifier)name!,
            Parameters = parameters!,
            Throws = throws_,
            Body = (Block?)body,
            DefaultValue = defaultValue,
            MethodType = methodType
        };
    }

    public override J VisitMethodInvocation(MethodInvocation methodInvocation, RpcReceiveQueue q)
    {
        var select = q.Receive(methodInvocation.Select, rp => VisitRightPadded(rp, q));
        var typeParameters = q.Receive(methodInvocation.TypeParameters, c => VisitContainer(c, q));
        var name = q.Receive((J)methodInvocation.Name, el => (J)VisitNonNull(el, q));
        var arguments = q.Receive(methodInvocation.Arguments, c => VisitContainer(c, q));
        var methodType = q.Receive(methodInvocation.MethodType, t => (JavaType.Method)VisitType(t, q)!);
        return methodInvocation with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Select = select, TypeParameters = typeParameters, Name = (Identifier)name!, Arguments = arguments!, MethodType = methodType };
    }

    public J VisitModifier(Modifier modifier, RpcReceiveQueue q)
    {
        // Java model has a keyword string field; consume for protocol compatibility
        var keyword = q.Receive(modifier.Keyword ?? GetModifierKeyword(modifier.Type));
        var modifierType = q.Receive(modifier.Type);
        // Map LanguageExtension back to the correct C# modifier type using the keyword
        string? storedKeyword = null;
        if (modifierType == Modifier.ModifierType.LanguageExtension && keyword != null)
        {
            var mapped = MapKeywordToModifierType(keyword);
            if (mapped != null)
            {
                modifierType = mapped.Value;
            }
            else
            {
                // Truly a language extension (e.g. "event") — store keyword for printing
                storedKeyword = keyword;
            }
        }
        var annotations = q.ReceiveList(modifier.Annotations, a => (Annotation)VisitNonNull(a, q));
        return modifier with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Type = modifierType, Annotations = annotations!, Keyword = storedKeyword };
    }

    public override J VisitNewArray(NewArray newArray, RpcReceiveQueue q)
    {
        var typeExpression = q.Receive((J?)newArray.TypeExpression, el => (J)VisitNonNull(el!, q));
        var dimensions = q.ReceiveList(newArray.Dimensions, d => (ArrayDimension)VisitNonNull(d, q));
        var initializer = q.Receive(newArray.Initializer, c => VisitContainer(c, q));
        var type = q.Receive(newArray.Type, t => VisitType(t, q)!);
        return newArray with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            TypeExpression = (TypeTree?)typeExpression, Dimensions = dimensions!, Initializer = initializer, Type = type };
    }

    public override J VisitNewClass(NewClass newClass, RpcReceiveQueue q)
    {
        var enclosing = q.Receive(newClass.Enclosing, rp => VisitRightPadded(rp, q));
        var @new = q.Receive(newClass.New, space => VisitSpace(space, q));
        var clazz = q.Receive((J?)newClass.Clazz, el => (J)VisitNonNull(el!, q));
        var arguments = q.Receive(newClass.Arguments, c => VisitContainer(c, q));
        var body = q.Receive((J?)newClass.Body, el => (J)VisitNonNull(el!, q));
        var constructorType = q.Receive(newClass.ConstructorType, t => (JavaType.Method)VisitType(t, q)!);
        return newClass with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Enclosing = enclosing, New = @new!, Clazz = (TypeTree?)clazz, Arguments = arguments!, Body = (Block?)body, ConstructorType = constructorType };
    }

    public override J VisitPackage(Package pkg, RpcReceiveQueue q)
    {
        var expression = q.Receive((J)pkg.Expression.Element, el => (J)VisitNonNull(el, q));
        var annotations = q.ReceiveList(pkg.Annotations, a => (Annotation)VisitNonNull(a, q));
        return pkg with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Expression = pkg.Expression with { Element = (Expression)expression! }, Annotations = annotations! };
    }

    public J VisitParentheses<T>(Parentheses<T> parentheses, RpcReceiveQueue q) where T : J
    {
        var tree = q.Receive(parentheses.Tree, rp => VisitRightPadded(rp, q));
        return parentheses with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Tree = tree! };
    }

    public override J VisitPrimitive(Primitive primitive, RpcReceiveQueue q)
    {
        var type = q.Receive(
            primitive.Kind != 0 ? (JavaType?)new JavaType.Primitive(primitive.Kind) : null,
            t => VisitType(t, q)!);
        var kind = type is JavaType.Primitive p ? p.Kind : primitive.Kind;
        return primitive with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Kind = kind };
    }

    public override J VisitReturn(Return aReturn, RpcReceiveQueue q)
    {
        var expression = q.Receive((J?)aReturn.Expression, el => (J)VisitNonNull(el!, q));
        return aReturn with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Expression = (Expression?)expression };
    }

    public override J VisitSwitch(Switch switchStmt, RpcReceiveQueue q)
    {
        var selector = q.Receive((J)switchStmt.Selector, el => (J)VisitNonNull(el, q));
        var cases = q.Receive((J)switchStmt.Cases, el => (J)VisitNonNull(el, q));
        return switchStmt with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Selector = (ControlParentheses<Expression>)selector!, Cases = (Block)cases! };
    }

    public override J VisitSwitchExpression(SwitchExpression switchExpression, RpcReceiveQueue q)
    {
        var selector = q.Receive((J)switchExpression.Selector, el => (J)VisitNonNull(el, q));
        var cases = q.Receive((J)switchExpression.Cases, el => (J)VisitNonNull(el, q));
        var type = q.Receive(switchExpression.Type, t => VisitType(t, q)!);
        return switchExpression with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Selector = (ControlParentheses<Expression>)selector!, Cases = (Block)cases!, Type = type };
    }

    public override J VisitSynchronized(Synchronized synch, RpcReceiveQueue q)
    {
        var @lock = q.Receive((J)synch.Lock, el => (J)VisitNonNull(el, q));
        var body = q.Receive((J)synch.Body, el => (J)VisitNonNull(el, q));
        return synch with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Lock = (ControlParentheses<Expression>)@lock!, Body = (Block)body! };
    }

    public override J VisitTernary(Ternary ternary, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)ternary.Condition, el => (J)VisitNonNull(el, q));
        var truePart = q.Receive(ternary.TruePart, lp => VisitLeftPadded(lp, q));
        var falsePart = q.Receive(ternary.FalsePart, lp => VisitLeftPadded(lp, q));
        var type = q.Receive(ternary.Type, t => VisitType(t, q)!);
        return ternary with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Condition = (Expression)condition!, TruePart = truePart!, FalsePart = falsePart!, Type = type };
    }

    public override J VisitThrow(Throw throwStmt, RpcReceiveQueue q)
    {
        var exception = q.Receive((J)throwStmt.Exception, el => (J)VisitNonNull(el, q));
        return throwStmt with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Exception = (Expression)exception! };
    }

    public override J VisitTry(Try tryStmt, RpcReceiveQueue q)
    {
        var resources = q.Receive(tryStmt.Resources, c => VisitContainer(c, q));
        var body = q.Receive((J)tryStmt.Body, el => (J)VisitNonNull(el, q));
        var catches = q.ReceiveList(tryStmt.Catches, c => (Try.Catch)VisitNonNull(c, q));
        var @finally = q.Receive(tryStmt.Finally, lp => VisitLeftPadded(lp, q));
        return tryStmt with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Resources = resources, Body = (Block)body!, Catches = catches!, Finally = @finally };
    }

    public override J VisitTypeCast(TypeCast typeCast, RpcReceiveQueue q)
    {
        var clazz = q.Receive((J)typeCast.Clazz, el => (J)VisitNonNull(el, q));
        var expression = q.Receive((J)typeCast.Expression, el => (J)VisitNonNull(el, q));
        return typeCast with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Clazz = (ControlParentheses<TypeTree>)clazz!, Expression = (Expression)expression! };
    }

    public J VisitTypeParameter(TypeParameter typeParam, RpcReceiveQueue q)
    {
        var annotations = q.ReceiveList(typeParam.Annotations, a => (Annotation)VisitNonNull(a, q));
        var modifiers = q.ReceiveList(typeParam.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var name = q.Receive((J)typeParam.Name, el => (J)VisitNonNull(el, q));
        var bounds = q.Receive(typeParam.Bounds, c => VisitContainer(c, q));
        return typeParam with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Annotations = annotations!, Modifiers = modifiers!, Name = (Expression)name!, Bounds = bounds };
    }

    public override J VisitUnary(Unary unary, RpcReceiveQueue q)
    {
        var @operator = q.Receive(unary.Operator, lp => VisitLeftPadded(lp, q));
        var expression = q.Receive((J)unary.Expression, el => (J)VisitNonNull(el, q));
        var type = q.Receive(unary.Type, t => VisitType(t, q)!);
        return unary with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Operator = @operator!, Expression = (Expression)expression!, Type = type };
    }

    public J VisitVariable(NamedVariable variable, RpcReceiveQueue q)
    {
        var name = q.Receive((J)variable.Name, el => (J)VisitNonNull(el, q));
        var dimensionsAfterName = q.ReceiveList(variable.DimensionsAfterName, lp => VisitLeftPadded(lp, q));
        var initializer = q.Receive(variable.Initializer, lp => VisitLeftPadded(lp, q));
        var type = q.Receive(variable.Type, t => VisitType(t, q)!);
        return variable with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Name = (Identifier)name!, DimensionsAfterName = dimensionsAfterName!, Initializer = initializer, Type = type };
    }

    public override J VisitVariableDeclarations(VariableDeclarations variableDeclarations, RpcReceiveQueue q)
    {
        var leadingAnnotations = q.ReceiveList(variableDeclarations.LeadingAnnotations, m => (Modifier)VisitNonNull(m, q));
        var modifiers = q.ReceiveList(variableDeclarations.Modifiers, m => (Modifier)VisitNonNull(m, q));
        var typeExpression = q.Receive((J?)variableDeclarations.TypeExpression, el => (J)VisitNonNull(el!, q));
        var varargs = q.Receive(variableDeclarations.Varargs, space => VisitSpace(space, q));
        var variables = q.ReceiveList(variableDeclarations.Variables, rp => VisitRightPadded(rp, q));
        return variableDeclarations with
        {
            Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            LeadingAnnotations = leadingAnnotations!,
            Modifiers = modifiers!,
            TypeExpression = (TypeTree?)typeExpression,
            Varargs = varargs,
            Variables = variables!
        };
    }

    public override J VisitWhileLoop(WhileLoop whileLoop, RpcReceiveQueue q)
    {
        var condition = q.Receive((J)whileLoop.Condition, el => (J)VisitNonNull(el, q));
        var body = q.Receive(whileLoop.Body, rp => VisitRightPadded(rp, q));
        return whileLoop with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Condition = (ControlParentheses<Expression>)condition!, Body = body! };
    }

    public J VisitCatch(Try.Catch tryCatch, RpcReceiveQueue q)
    {
        var parameter = q.Receive((J)tryCatch.Parameter, el => (J)VisitNonNull(el, q));
        var body = q.Receive((J)tryCatch.Body, el => (J)VisitNonNull(el, q));
        return tryCatch with { Id = _pvId, Prefix = _pvPrefix, Markers = _pvMarkers,
            Parameter = (ControlParentheses<VariableDeclarations>)parameter!, Body = (Block)body! };
    }

    // Helper methods

    public virtual JLeftPadded<T> VisitLeftPadded<T>(JLeftPadded<T> left, RpcReceiveQueue q)
    {
        var before = q.Receive(left.Before, space => VisitSpace(space, q));
        T element;
        if (left.Element is J)
            element = (T)(object)q.Receive((J)(object)left.Element!, el => (J)VisitNonNull(el, q))!;
        else if (left.Element is Space)
            element = (T)(object)q.Receive((Space)(object)left.Element!, space => VisitSpace(space, q))!;
        else
            element = q.Receive(left.Element)!;
        // C# JLeftPadded doesn't have Markers; consume and discard
        q.Receive<Markers>(Markers.Empty);
        return left with { Before = before!, Element = element };
    }

    public virtual JRightPadded<T> VisitRightPadded<T>(JRightPadded<T> right, RpcReceiveQueue q)
    {
        T element;
        if (right.Element is J)
            element = (T)(object)q.Receive((J)(object)right.Element!, el => (J)VisitNonNull(el, q))!;
        else if (right.Element is Space)
            element = (T)(object)q.Receive((Space)(object)right.Element!, space => VisitSpace(space, q))!;
        else
            element = q.Receive(right.Element)!;
        var after = q.Receive(right.After, space => VisitSpace(space, q));
        var markers = q.Receive(right.Markers);
        return right with { Element = element, After = after!, Markers = markers! };
    }

    public virtual JContainer<TJ> VisitContainer<TJ>(JContainer<TJ> container, RpcReceiveQueue q) where TJ : J
    {
        var before = q.Receive(container.Before, space => VisitSpace(space, q));
        var elements = q.ReceiveList(container.Elements, rp => VisitRightPadded(rp, q));
        var markers = q.Receive(container.Markers);
        return container with { Before = before!, Elements = elements!, Markers = markers! };
    }

    public virtual Space VisitSpace(Space space, RpcReceiveQueue q)
    {
        var comments = q.ReceiveList(space.Comments, c =>
        {
            if (c is TextComment tc)
            {
                var multiline = q.Receive(tc.Multiline);
                var text = q.Receive(tc.Text);
                var suffix = q.Receive(tc.Suffix);
                // C# Comment doesn't have Markers; consume and discard
                q.Receive<Markers>(Markers.Empty);
                return (Comment)(tc with { Multiline = multiline, Text = text!, Suffix = suffix! });
            }
            throw new ArgumentException($"Unexpected comment type {c.GetType().Name}");
        });
        var whitespace = q.Receive(space.Whitespace);
        return space with { Comments = comments!, Whitespace = whitespace! };
    }

    public virtual JavaType? VisitType(JavaType? javaType, RpcReceiveQueue q)
    {
        if (javaType == null) return null;

        switch (javaType)
        {
            case JavaType.Annotation annotation:
                annotation.AnnotationType = (JavaType.FullyQualified?)q.Receive(
                    (JavaType?)annotation.AnnotationType, t => VisitType(t, q)!);
                break;

            case JavaType.MultiCatch multiCatch:
                multiCatch.ThrowableTypes = q.ReceiveList(multiCatch.ThrowableTypes,
                    t => VisitType(t, q)!);
                break;

            case JavaType.Intersection intersection:
                intersection.Bounds = q.ReceiveList(intersection.Bounds,
                    t => VisitType(t, q)!);
                break;

            case JavaType.Class cls:
                cls.FlagsBitMap = q.Receive(cls.FlagsBitMap);
                cls.ClassKind = q.ReceiveAndGet(cls.ClassKind, RpcReceiveQueue.ToEnum<JavaType.FullyQualified.FullyQualifiedKind>());
                cls.FullyQualifiedName = q.Receive(cls.FullyQualifiedName)!;
                cls.TypeParameters = q.ReceiveList(cls.TypeParameters,
                    t => VisitType(t, q)!);
                cls.Supertype = (JavaType.FullyQualified?)q.Receive(
                    (JavaType?)cls.Supertype, t => VisitType(t, q)!);
                cls.OwningClass = (JavaType.FullyQualified?)q.Receive(
                    (JavaType?)cls.OwningClass, t => VisitType(t, q)!);
                cls.Annotations = (IList<JavaType.FullyQualified>?)q.ReceiveList(cls.Annotations,
                    t => (JavaType.FullyQualified)VisitType(t, q)!);
                cls.Interfaces = (IList<JavaType.FullyQualified>?)q.ReceiveList(cls.Interfaces,
                    t => (JavaType.FullyQualified)VisitType(t, q)!);
                cls.Members = (IList<JavaType.Variable>?)q.ReceiveList(cls.Members,
                    t => (JavaType.Variable)VisitType(t, q)!);
                cls.Methods = (IList<JavaType.Method>?)q.ReceiveList(cls.Methods,
                    t => (JavaType.Method)VisitType(t, q)!);
                break;

            case JavaType.Parameterized parameterized:
                parameterized.Type = (JavaType.FullyQualified?)q.Receive(
                    (JavaType?)parameterized.Type, t => VisitType(t, q)!);
                parameterized.TypeParameters = q.ReceiveList(parameterized.TypeParameters,
                    t => VisitType(t, q)!);
                break;

            case JavaType.GenericTypeVariable generic:
                generic.Name = q.Receive(generic.Name)!;
                generic.Variance = q.Receive(generic.Variance);
                generic.Bounds = q.ReceiveList(generic.Bounds,
                    t => VisitType(t, q)!);
                break;

            case JavaType.Array array:
                array.ElemType = q.Receive(array.ElemType, t => VisitType(t, q)!)!;
                array.Annotations = (IList<JavaType.FullyQualified>?)q.ReceiveList(array.Annotations,
                    t => (JavaType.FullyQualified)VisitType(t, q)!);
                break;

            case JavaType.Primitive prim:
                q.Receive(GetPrimitiveKeyword(prim.Kind));
                break;

            case JavaType.Method method:
                method.DeclaringType = (JavaType.FullyQualified?)q.Receive(
                    (JavaType?)method.DeclaringType, t => VisitType(t, q)!);
                method.Name = q.Receive(method.Name)!;
                method.FlagsBitMap = q.Receive(method.FlagsBitMap);
                method.ReturnType = q.Receive(method.ReturnType, t => VisitType(t, q)!)!;
                method.ParameterNames = q.ReceiveList(method.ParameterNames,
                    s => s);
                method.ParameterTypes = q.ReceiveList(method.ParameterTypes,
                    t => VisitType(t, q)!);
                method.ThrownExceptions = q.ReceiveList(method.ThrownExceptions,
                    t => VisitType(t, q)!);
                method.Annotations = (IList<JavaType.FullyQualified>?)q.ReceiveList(method.Annotations,
                    t => (JavaType.FullyQualified)VisitType(t, q)!);
                method.DefaultValue = q.ReceiveList(method.DefaultValue,
                    s => s);
                method.DeclaredFormalTypeNames = q.ReceiveList(method.DeclaredFormalTypeNames,
                    s => s);
                break;

            case JavaType.Variable variable:
                variable.Name = q.Receive(variable.Name)!;
                variable.Owner = q.Receive(variable.Owner, t => VisitType(t, q)!);
                variable.Type = q.Receive(variable.Type, t => VisitType(t, q)!);
                variable.Annotations = (IList<JavaType.FullyQualified>?)q.ReceiveList(variable.Annotations,
                    t => (JavaType.FullyQualified)VisitType(t, q)!);
                break;
        }

        return javaType;
    }

    // Utility methods

    protected J VisitNonNull(J tree, RpcReceiveQueue q) => Visit(tree, q)!;

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
    /// Maps a keyword string back to the correct C# modifier type.
    /// Used when Java sends LanguageExtension for C#-specific modifiers.
    /// </summary>
    private static Modifier.ModifierType? MapKeywordToModifierType(string keyword) => keyword switch
    {
        "internal" => Modifier.ModifierType.Internal,
        "override" => Modifier.ModifierType.Override,
        "virtual" => Modifier.ModifierType.Virtual,
        "readonly" => Modifier.ModifierType.Readonly,
        "const" => Modifier.ModifierType.Const,
        "new" => Modifier.ModifierType.New,
        "extern" => Modifier.ModifierType.Extern,
        "unsafe" => Modifier.ModifierType.Unsafe,
        "partial" => Modifier.ModifierType.Partial,
        "ref" => Modifier.ModifierType.Ref,
        "out" => Modifier.ModifierType.Out,
        "in" => Modifier.ModifierType.In,
        _ => null
    };
}
