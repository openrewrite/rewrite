using Rewrite.Core;

namespace Rewrite.Java;

/// <summary>
/// The base interface for all Java-family LST elements.
/// C# LST elements extend J where the syntax is isomorphic.
/// </summary>
public interface J : Tree
{
    Space Prefix { get; }
    Markers Markers { get; }
}

/// <summary>
/// Marker interface for expressions that produce a value.
/// </summary>
public interface Expression : J
{
}

/// <summary>
/// Marker interface for statements.
/// </summary>
public interface Statement : J
{
}

/// <summary>
/// Marker indicating null-safe navigation (?. operator).
/// Used by languages with safe navigation: C#, Kotlin, Groovy.
/// When present on a MethodInvocation.Name or FieldAccess.Name, prints ?. instead of .
/// </summary>
public sealed record NullSafe(Guid Id) : Marker
{
    public static NullSafe Instance { get; } = new(Guid.Empty);
}

/// <summary>
/// Marker indicating parentheses should be omitted in NewClass.
/// Used for object initializers without constructor arguments: new Foo { X = 1 }
/// </summary>
public sealed record OmitParentheses(Guid Id) : Marker
{
    public static OmitParentheses Instance { get; } = new(Guid.Empty);
}

/// <summary>
/// Marker indicating a semicolon-terminated element.
/// Used for positional records (record Person(string Name);) and other contexts
/// where a semicolon replaces the expected body/braces.
/// </summary>
public sealed record Semicolon(Guid Id) : Marker;

/// <summary>
/// A package/namespace declaration.
/// Java: package com.example;
/// C#: namespace MyApp.Services; (file-scoped)
/// </summary>
public sealed record Package(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Expression> Expression,  // The package/namespace name with trailing space before ;
    IList<Annotation> Annotations         // Annotations before the package keyword
) : J, Statement
{
    public Package WithId(Guid id) => this with { Id = id };
    public Package WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Package WithMarkers(Markers markers) => this with { Markers = markers };
    public Package WithExpression(JRightPadded<Expression> expression) => this with { Expression = expression };
    public Package WithAnnotations(IList<Annotation> annotations) => this with { Annotations = annotations };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A literal value (string, number, boolean, char, null).
/// </summary>
public sealed record Literal(
    Guid Id,
    Space Prefix,
    Markers Markers,
    object? Value,
    string? ValueSource,
    IList<Literal.UnicodeEscape>? UnicodeEscapes,
    JavaType.Primitive? Type
) : J, Expression
{
    public record UnicodeEscape(int ValueSourceIndex, string CodePoint);

    public Literal WithId(Guid id) => this with { Id = id };
    public Literal WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Literal WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An expression used as a statement.
/// </summary>
public sealed record ExpressionStatement(
    Guid Id,
    Expression Expression
) : J, Statement
{
    // ExpressionStatement delegates prefix/markers to its expression
    public Space Prefix => Expression.Prefix;
    public Markers Markers => Expression.Markers;

    public ExpressionStatement WithId(Guid id) => this with { Id = id };
    public ExpressionStatement WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A block of statements enclosed in braces { }.
/// </summary>
public sealed record Block(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<bool> Static,
    IList<JRightPadded<Statement>> Statements,
    Space End
) : J, Statement
{
    public Block WithId(Guid id) => this with { Id = id };
    public Block WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Block WithMarkers(Markers markers) => this with { Markers = markers };
    public Block WithStatements(IList<JRightPadded<Statement>> statements) => this with { Statements = statements };
    public Block WithEnd(Space end) => this with { End = end };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A return statement (e.g., return 1;).
/// </summary>
public sealed record Return(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JLeftPadded<Expression>? Expression
) : J, Statement
{
    public Return WithId(Guid id) => this with { Id = id };
    public Return WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Return WithMarkers(Markers markers) => this with { Markers = markers };
    public Return WithExpression(JLeftPadded<Expression>? expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An if statement (e.g., if (condition) { } else { }).
/// </summary>
public sealed record If(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ControlParentheses<Expression> Condition,
    JRightPadded<Statement> ThenPart,
    If.Else? ElsePart
) : J, Statement
{
    public sealed record Else(
        Guid Id,
        Space Prefix,
        Markers Markers,
        JRightPadded<Statement> Body
    ) : J
    {
        public Else WithId(Guid id) => this with { Id = id };
        public Else WithPrefix(Space prefix) => this with { Prefix = prefix };
        public Else WithMarkers(Markers markers) => this with { Markers = markers };
        public Else WithBody(JRightPadded<Statement> body) => this with { Body = body };

        Tree Tree.WithId(Guid id) => WithId(id);
    }

    public If WithId(Guid id) => this with { Id = id };
    public If WithPrefix(Space prefix) => this with { Prefix = prefix };
    public If WithMarkers(Markers markers) => this with { Markers = markers };
    public If WithCondition(ControlParentheses<Expression> condition) => this with { Condition = condition };
    public If WithThenPart(JRightPadded<Statement> thenPart) => this with { ThenPart = thenPart };
    public If WithElsePart(Else? elsePart) => this with { ElsePart = elsePart };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// Parentheses used in control structures (if, while, etc.).
/// </summary>
public sealed record ControlParentheses<T>(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<T> Tree
) : J, Expression where T : J
{
    public ControlParentheses<T> WithId(Guid id) => this with { Id = id };
    public ControlParentheses<T> WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ControlParentheses<T> WithMarkers(Markers markers) => this with { Markers = markers };
    public ControlParentheses<T> WithTree(JRightPadded<T> tree) => this with { Tree = tree };

    Tree Core.Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A for loop (e.g., for (int i = 0; i &lt; 10; i++) { }).
/// </summary>
public sealed record ForLoop(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ForLoop.Control LoopControl,
    JRightPadded<Statement> Body
) : J, Statement
{
    public sealed record Control(
        Guid Id,
        Space Prefix,
        Markers Markers,
        IList<JRightPadded<Statement>> Init,
        JRightPadded<Expression> Condition,
        IList<JRightPadded<Statement>> Update
    ) : J
    {
        public Control WithId(Guid id) => this with { Id = id };
        public Control WithPrefix(Space prefix) => this with { Prefix = prefix };
        public Control WithMarkers(Markers markers) => this with { Markers = markers };

        Tree Tree.WithId(Guid id) => WithId(id);
    }

    public ForLoop WithId(Guid id) => this with { Id = id };
    public ForLoop WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ForLoop WithMarkers(Markers markers) => this with { Markers = markers };
    public ForLoop WithBody(JRightPadded<Statement> body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A foreach loop (e.g., foreach (var x in items) { }).
/// </summary>
public sealed record ForEachLoop(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ForEachLoop.Control LoopControl,
    JRightPadded<Statement> Body
) : J, Statement
{
    public sealed record Control(
        Guid Id,
        Space Prefix,
        Markers Markers,
        JRightPadded<VariableDeclarations> Variable,
        JRightPadded<Expression> Iterable
    ) : J
    {
        public Control WithId(Guid id) => this with { Id = id };
        public Control WithPrefix(Space prefix) => this with { Prefix = prefix };
        public Control WithMarkers(Markers markers) => this with { Markers = markers };

        Tree Tree.WithId(Guid id) => WithId(id);
    }

    public ForEachLoop WithId(Guid id) => this with { Id = id };
    public ForEachLoop WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ForEachLoop WithMarkers(Markers markers) => this with { Markers = markers };
    public ForEachLoop WithBody(JRightPadded<Statement> body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A while loop (e.g., while (condition) { }).
/// </summary>
public sealed record WhileLoop(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ControlParentheses<Expression> Condition,
    JRightPadded<Statement> Body
) : J, Statement
{
    public WhileLoop WithId(Guid id) => this with { Id = id };
    public WhileLoop WithPrefix(Space prefix) => this with { Prefix = prefix };
    public WhileLoop WithMarkers(Markers markers) => this with { Markers = markers };
    public WhileLoop WithBody(JRightPadded<Statement> body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A do-while loop (e.g., do { } while (condition);).
/// </summary>
public sealed record DoWhileLoop(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Statement> Body,
    JLeftPadded<ControlParentheses<Expression>> Condition
) : J, Statement
{
    public DoWhileLoop WithId(Guid id) => this with { Id = id };
    public DoWhileLoop WithPrefix(Space prefix) => this with { Prefix = prefix };
    public DoWhileLoop WithMarkers(Markers markers) => this with { Markers = markers };
    public DoWhileLoop WithBody(JRightPadded<Statement> body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A synchronized/lock block statement.
/// In Java: synchronized (lock) { body }
/// In C#: lock (lock) { body }
/// </summary>
public sealed record Synchronized(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ControlParentheses<Expression> Lock,
    Block Body
) : J, Statement
{
    public Synchronized WithId(Guid id) => this with { Id = id };
    public Synchronized WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Synchronized WithMarkers(Markers markers) => this with { Markers = markers };
    public Synchronized WithLock(ControlParentheses<Expression> @lock) => this with { Lock = @lock };
    public Synchronized WithBody(Block body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A try statement (e.g., try { } catch { } finally { }).
/// </summary>
public sealed record Try(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JContainer<NameTree>? Resources,
    Block Body,
    IList<Try.Catch> Catches,
    JLeftPadded<Block>? Finally
) : J, Statement
{
    public sealed record Catch(
        Guid Id,
        Space Prefix,
        Markers Markers,
        ControlParentheses<VariableDeclarations> Parameter,
        Block Body
    ) : J
    {
        public Catch WithId(Guid id) => this with { Id = id };
        public Catch WithPrefix(Space prefix) => this with { Prefix = prefix };
        public Catch WithMarkers(Markers markers) => this with { Markers = markers };

        Tree Tree.WithId(Guid id) => WithId(id);
    }

    public Try WithId(Guid id) => this with { Id = id };
    public Try WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Try WithMarkers(Markers markers) => this with { Markers = markers };
    public Try WithBody(Block body) => this with { Body = body };
    public Try WithCatches(IList<Catch> catches) => this with { Catches = catches };
    public Try WithFinally(JLeftPadded<Block>? finallyBlock) => this with { Finally = finallyBlock };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A throw statement (e.g., throw new Exception();).
/// </summary>
public sealed record Throw(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Exception
) : J, Statement
{
    public Throw WithId(Guid id) => this with { Id = id };
    public Throw WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Throw WithMarkers(Markers markers) => this with { Markers = markers };
    public Throw WithException(Expression exception) => this with { Exception = exception };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A break statement.
/// </summary>
public sealed record Break(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Identifier? Label
) : J, Statement
{
    public Break WithId(Guid id) => this with { Id = id };
    public Break WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Break WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A continue statement.
/// </summary>
public sealed record Continue(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Identifier? Label
) : J, Statement
{
    public Continue WithId(Guid id) => this with { Id = id };
    public Continue WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Continue WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An empty statement (just a semicolon).
/// </summary>
public sealed record Empty(
    Guid Id,
    Space Prefix,
    Markers Markers
) : J, Statement, Expression
{
    public Empty WithId(Guid id) => this with { Id = id };
    public Empty WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Empty WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A class, struct, interface, or record declaration.
/// </summary>
public sealed record ClassDeclaration(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Annotation> LeadingAnnotations,
    IList<Modifier> Modifiers,
    ClassDeclaration.Kind ClassKind,
    Identifier Name,
    JContainer<TypeParameter>? TypeParameters,
    JContainer<TypeTree>? PrimaryConstructor,
    JLeftPadded<TypeTree>? Extends,
    JContainer<TypeTree>? Implements,
    JContainer<TypeTree>? Permits,
    Block Body,
    JavaType.FullyQualified? Type
) : J, Statement
{
    public enum KindType { Class, Enum, Interface, Annotation, Record, Value }

    public sealed record Kind(
        Guid Id,
        Space Prefix,
        Markers Markers,
        IList<Annotation> Annotations,
        KindType Type
    ) : J
    {
        public Kind WithId(Guid id) => this with { Id = id };
        public Kind WithPrefix(Space prefix) => this with { Prefix = prefix };
        public Kind WithMarkers(Markers markers) => this with { Markers = markers };

        Tree Tree.WithId(Guid id) => WithId(id);
    }

    public ClassDeclaration WithId(Guid id) => this with { Id = id };
    public ClassDeclaration WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ClassDeclaration WithMarkers(Markers markers) => this with { Markers = markers };
    public ClassDeclaration WithName(Identifier name) => this with { Name = name };
    public ClassDeclaration WithBody(Block body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A single enum constant/value.
/// Examples:
///   Red
///   Green = 1
///   Blue = 2
/// </summary>
public sealed record EnumValue(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Annotation> Annotations,
    Identifier Name,
    JLeftPadded<Expression>? Initializer  // For = value
) : J
{
    public EnumValue WithId(Guid id) => this with { Id = id };
    public EnumValue WithPrefix(Space prefix) => this with { Prefix = prefix };
    public EnumValue WithMarkers(Markers markers) => this with { Markers = markers };
    public EnumValue WithAnnotations(IList<Annotation> annotations) => this with { Annotations = annotations };
    public EnumValue WithName(Identifier name) => this with { Name = name };
    public EnumValue WithInitializer(JLeftPadded<Expression>? initializer) => this with { Initializer = initializer };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// The set of enum values in an enum declaration.
/// Example:
///   Red, Green = 1, Blue
/// </summary>
public sealed record EnumValueSet(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<JRightPadded<EnumValue>> Enums,
    bool TerminatedWithSemicolon
) : J, Statement
{
    public EnumValueSet WithId(Guid id) => this with { Id = id };
    public EnumValueSet WithPrefix(Space prefix) => this with { Prefix = prefix };
    public EnumValueSet WithMarkers(Markers markers) => this with { Markers = markers };
    public EnumValueSet WithEnums(IList<JRightPadded<EnumValue>> enums) => this with { Enums = enums };
    public EnumValueSet WithTerminatedWithSemicolon(bool terminated) => this with { TerminatedWithSemicolon = terminated };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A method declaration (e.g., void Bar() { }).
/// </summary>
public sealed record MethodDeclaration(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Annotation> LeadingAnnotations,
    IList<Modifier> Modifiers,
    JContainer<TypeParameter>? TypeParameters,
    TypeTree? ReturnTypeExpression,
    Identifier Name,
    JContainer<Statement> Parameters,
    JContainer<NameTree>? Throws,
    Block? Body,
    JLeftPadded<Expression>? DefaultValue,
    JavaType.Method? MethodType
) : J, Statement
{
    public MethodDeclaration WithId(Guid id) => this with { Id = id };
    public MethodDeclaration WithPrefix(Space prefix) => this with { Prefix = prefix };
    public MethodDeclaration WithMarkers(Markers markers) => this with { Markers = markers };
    public MethodDeclaration WithName(Identifier name) => this with { Name = name };
    public MethodDeclaration WithBody(Block? body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A type parameter in a generic declaration (e.g., T in class Foo&lt;T&gt;).
/// </summary>
public sealed record TypeParameter(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Annotation> Annotations,
    IList<Modifier> Modifiers,
    Expression Name,
    JContainer<TypeTree>? Bounds
) : J
{
    public TypeParameter WithId(Guid id) => this with { Id = id };
    public TypeParameter WithPrefix(Space prefix) => this with { Prefix = prefix };
    public TypeParameter WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An identifier (variable name, type name, etc.).
/// </summary>
public sealed record Identifier(
    Guid Id,
    Space Prefix,
    Markers Markers,
    string SimpleName,
    JavaType? Type
) : J, Expression, NameTree
{
    public Identifier WithId(Guid id) => this with { Id = id };
    public Identifier WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Identifier WithMarkers(Markers markers) => this with { Markers = markers };
    public Identifier WithSimpleName(string simpleName) => this with { SimpleName = simpleName };
    public Identifier WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A field access (qualified name), e.g., System.Console or obj.field.
/// </summary>
public sealed record FieldAccess(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Target,
    JLeftPadded<Identifier> Name,
    JavaType? Type
) : J, TypeTree, Expression, Statement
{
    public Identifier GetName() => Name.Element;

    public FieldAccess WithId(Guid id) => this with { Id = id };
    public FieldAccess WithPrefix(Space prefix) => this with { Prefix = prefix };
    public FieldAccess WithMarkers(Markers markers) => this with { Markers = markers };
    public FieldAccess WithTarget(Expression target) => this with { Target = target };
    public FieldAccess WithName(Identifier name) => this with { Name = Name with { Element = name } };
    public FieldAccess WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An assignment expression (e.g., x = 1, x += 2).
/// </summary>
public sealed record Assignment(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Variable,
    JLeftPadded<Expression> AssignmentValue,
    JavaType? Type
) : J, Statement, Expression
{
    public Assignment WithId(Guid id) => this with { Id = id };
    public Assignment WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Assignment WithMarkers(Markers markers) => this with { Markers = markers };
    public Assignment WithVariable(Expression variable) => this with { Variable = variable };
    public Assignment WithAssignmentValue(JLeftPadded<Expression> value) => this with { AssignmentValue = value };
    public Assignment WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A compound assignment expression with operator (e.g., x += 1, x -= 2).
/// </summary>
public sealed record AssignmentOperation(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Variable,
    JLeftPadded<AssignmentOperation.OperatorType> Operator,
    Expression AssignmentValue,
    JavaType? Type
) : J, Statement, Expression
{
    public enum OperatorType
    {
        Addition,
        Subtraction,
        Multiplication,
        Division,
        Modulo,
        BitAnd,
        BitOr,
        BitXor,
        LeftShift,
        RightShift,
        UnsignedRightShift,
        Coalesce  // ??= in C#
    }

    public AssignmentOperation WithId(Guid id) => this with { Id = id };
    public AssignmentOperation WithPrefix(Space prefix) => this with { Prefix = prefix };
    public AssignmentOperation WithMarkers(Markers markers) => this with { Markers = markers };
    public AssignmentOperation WithVariable(Expression variable) => this with { Variable = variable };
    public AssignmentOperation WithOperator(JLeftPadded<OperatorType> op) => this with { Operator = op };
    public AssignmentOperation WithAssignmentValue(Expression value) => this with { AssignmentValue = value };
    public AssignmentOperation WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A method invocation (e.g., foo.Bar(), Console.WriteLine("hello"), Bar&lt;T&gt;(x)).
/// </summary>
public sealed record MethodInvocation(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<Expression>? Select,        // target with After = space before dot (null if unqualified)
    Identifier Name,                          // method name with Prefix = space after dot
    JContainer<Expression>? TypeParameters,   // <T> generic type arguments (C# puts these after name)
    JContainer<Expression> Arguments,         // (args) with Before = space before (
    JavaType.Method? MethodType
) : J, Statement, Expression
{
    public MethodInvocation WithId(Guid id) => this with { Id = id };
    public MethodInvocation WithPrefix(Space prefix) => this with { Prefix = prefix };
    public MethodInvocation WithMarkers(Markers markers) => this with { Markers = markers };
    public MethodInvocation WithSelect(JRightPadded<Expression>? select) => this with { Select = select };
    public MethodInvocation WithName(Identifier name) => this with { Name = name };
    public MethodInvocation WithTypeParameters(JContainer<Expression>? typeParameters) => this with { TypeParameters = typeParameters };
    public MethodInvocation WithArguments(JContainer<Expression> arguments) => this with { Arguments = arguments };
    public MethodInvocation WithMethodType(JavaType.Method? methodType) => this with { MethodType = methodType };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A binary expression (e.g., a + b, x == y).
/// </summary>
public sealed record Binary(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Left,
    JLeftPadded<Binary.OperatorType> Operator,
    Expression Right,
    JavaType? Type
) : J, Expression
{
    public enum OperatorType
    {
        Addition,
        Subtraction,
        Multiplication,
        Division,
        Modulo,
        LessThan,
        GreaterThan,
        LessThanOrEqual,
        GreaterThanOrEqual,
        Equal,
        NotEqual,
        BitAnd,
        BitOr,
        BitXor,
        LeftShift,
        RightShift,
        UnsignedRightShift,
        Or,
        And,
        PatternOr,    // C# pattern 'or' keyword (distinct from expression '||')
        PatternAnd    // C# pattern 'and' keyword (distinct from expression '&&')
    }

    public Binary WithId(Guid id) => this with { Id = id };
    public Binary WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Binary WithMarkers(Markers markers) => this with { Markers = markers };
    public Binary WithLeft(Expression left) => this with { Left = left };
    public Binary WithOperator(JLeftPadded<OperatorType> op) => this with { Operator = op };
    public Binary WithRight(Expression right) => this with { Right = right };
    public Binary WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A unary expression (e.g., !x, -y, ++i, i--).
/// </summary>
public sealed record Unary(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JLeftPadded<Unary.OperatorType> Operator,
    Expression Expression,
    JavaType? Type
) : J, Statement, Expression
{
    public enum OperatorType
    {
        PreIncrement,
        PreDecrement,
        PostIncrement,
        PostDecrement,
        Positive,
        Negative,
        Complement,
        Not,
        PatternNot    // C# pattern 'not' keyword (distinct from expression '!')
    }

    public Unary WithId(Guid id) => this with { Id = id };
    public Unary WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Unary WithMarkers(Markers markers) => this with { Markers = markers };
    public Unary WithOperator(JLeftPadded<OperatorType> op) => this with { Operator = op };
    public Unary WithExpression(Expression expression) => this with { Expression = expression };
    public Unary WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A type cast expression (e.g., (int)x, (string)obj).
/// </summary>
public sealed record TypeCast(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ControlParentheses<TypeTree> Clazz,  // The (type) part
    Expression Expression                 // The expression being cast
) : J, Expression
{
    public TypeCast WithId(Guid id) => this with { Id = id };
    public TypeCast WithPrefix(Space prefix) => this with { Prefix = prefix };
    public TypeCast WithMarkers(Markers markers) => this with { Markers = markers };
    public TypeCast WithClazz(ControlParentheses<TypeTree> clazz) => this with { Clazz = clazz };
    public TypeCast WithExpression(Expression expression) => this with { Expression = expression };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A parenthesized expression.
/// </summary>
public sealed record Parentheses<T>(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JRightPadded<T> Tree
) : J, Expression where T : J
{
    public Parentheses<T> WithId(Guid id) => this with { Id = id };
    public Parentheses<T> WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Parentheses<T> WithMarkers(Markers markers) => this with { Markers = markers };
    public Parentheses<T> WithTree(JRightPadded<T> tree) => this with { Tree = tree };

    Tree Core.Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A variable declaration statement (e.g., var x = 1; or int x = 1, y = 2;).
/// </summary>
public sealed record VariableDeclarations(
    Guid Id,
    Space Prefix,
    Markers Markers,
    IList<Modifier> LeadingAnnotations,
    IList<Modifier> Modifiers,
    TypeTree? TypeExpression,
    Space? Varargs,
    IList<JLeftPadded<Space>> DimensionsBeforeName,
    IList<JRightPadded<NamedVariable>> Variables
) : J, Statement
{
    public VariableDeclarations WithId(Guid id) => this with { Id = id };
    public VariableDeclarations WithPrefix(Space prefix) => this with { Prefix = prefix };
    public VariableDeclarations WithMarkers(Markers markers) => this with { Markers = markers };
    public VariableDeclarations WithVariables(IList<JRightPadded<NamedVariable>> variables) => this with { Variables = variables };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A single variable in a variable declaration (e.g., x = 1).
/// </summary>
public sealed record NamedVariable(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Identifier Name,
    IList<JLeftPadded<Space>> DimensionsAfterName,
    JLeftPadded<Expression>? Initializer,
    JavaType? Type
) : J
{
    public NamedVariable WithId(Guid id) => this with { Id = id };
    public NamedVariable WithPrefix(Space prefix) => this with { Prefix = prefix };
    public NamedVariable WithMarkers(Markers markers) => this with { Markers = markers };
    public NamedVariable WithName(Identifier name) => this with { Name = name };
    public NamedVariable WithInitializer(JLeftPadded<Expression>? initializer) => this with { Initializer = initializer };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A type reference in source code.
/// </summary>
public interface TypeTree : J
{
}

/// <summary>
/// A primitive type (int, long, etc.) or the var keyword.
/// </summary>
public sealed record Primitive(
    Guid Id,
    Space Prefix,
    Markers Markers,
    JavaType.PrimitiveKind Kind
) : J, TypeTree, Expression
{
    public Primitive WithId(Guid id) => this with { Id = id };
    public Primitive WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Primitive WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A modifier (public, static, etc.).
/// </summary>
public sealed record Modifier(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Modifier.ModifierType Type,
    IList<Annotation> Annotations
) : J
{
    public enum ModifierType
    {
        Default,
        Public,
        Private,
        Protected,
        Internal,
        Static,
        Abstract,
        Final,
        Sealed,
        Native,
        Strictfp,
        Synchronized,
        Transient,
        Volatile,
        Async,
        Override,
        Virtual,
        Readonly,
        Const,
        New,
        Extern,
        Unsafe,
        Partial,
        Ref,
        Out,
        In,
        LanguageExtension
    }

    public Modifier WithId(Guid id) => this with { Id = id };
    public Modifier WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Modifier WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An annotation/attribute.
/// </summary>
public sealed record Annotation(
    Guid Id,
    Space Prefix,
    Markers Markers,
    NameTree AnnotationType,
    JContainer<Expression>? Arguments
) : J, Expression
{
    public Annotation WithId(Guid id) => this with { Id = id };
    public Annotation WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Annotation WithMarkers(Markers markers) => this with { Markers = markers };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A name reference in source code.
/// </summary>
public interface NameTree : TypeTree
{
}

/// <summary>
/// Array/indexer access expression (e.g., arr[0], dict["key"]).
/// </summary>
public sealed record ArrayAccess(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Indexed,      // The expression being indexed (e.g., "arr" in arr[0])
    ArrayDimension Dimension, // The [index] part
    JavaType? Type
) : J, Expression
{
    public ArrayAccess WithId(Guid id) => this with { Id = id };
    public ArrayAccess WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ArrayAccess WithMarkers(Markers markers) => this with { Markers = markers };
    public ArrayAccess WithIndexed(Expression indexed) => this with { Indexed = indexed };
    public ArrayAccess WithDimension(ArrayDimension dimension) => this with { Dimension = dimension };
    public ArrayAccess WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A single dimension in an array access (the [index] part).
/// </summary>
public sealed record ArrayDimension(
    Guid Id,
    Space Prefix,           // Space before '['
    Markers Markers,
    JRightPadded<Expression> Index  // The index expression, After = space before ']'
) : J
{
    public ArrayDimension WithId(Guid id) => this with { Id = id };
    public ArrayDimension WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ArrayDimension WithMarkers(Markers markers) => this with { Markers = markers };
    public ArrayDimension WithIndex(JRightPadded<Expression> index) => this with { Index = index };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An array type expression (e.g., int[], string[,]).
/// For multi-dimensional arrays like int[,], each dimension is nested.
/// </summary>
public sealed record ArrayType(
    Guid Id,
    Space Prefix,
    Markers Markers,
    TypeTree ElementType,                  // The element type (int, string, etc.)
    IList<Annotation>? Annotations,        // Type annotations (rare in C#)
    JLeftPadded<Space>? Dimension,         // The [] part: Before = space before [, Element = space before ]
    JavaType? Type
) : J, TypeTree, Expression
{
    public ArrayType WithId(Guid id) => this with { Id = id };
    public ArrayType WithPrefix(Space prefix) => this with { Prefix = prefix };
    public ArrayType WithMarkers(Markers markers) => this with { Markers = markers };
    public ArrayType WithElementType(TypeTree elementType) => this with { ElementType = elementType };
    public ArrayType WithAnnotations(IList<Annotation>? annotations) => this with { Annotations = annotations };
    public ArrayType WithDimension(JLeftPadded<Space>? dimension) => this with { Dimension = dimension };
    public ArrayType WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// An array creation expression.
/// Examples:
///   new int[10]
///   new int[] { 1, 2, 3 }
///   new[] { 1, 2, 3 }
///   new int[2, 3]
/// </summary>
public sealed record NewArray(
    Guid Id,
    Space Prefix,
    Markers Markers,
    TypeTree? TypeExpression,              // The element type (null for implicitly typed new[])
    IList<ArrayDimension> Dimensions,      // The [size] parts
    JContainer<Expression>? Initializer,   // The { elements } part
    JavaType? Type
) : J, Expression
{
    public NewArray WithId(Guid id) => this with { Id = id };
    public NewArray WithPrefix(Space prefix) => this with { Prefix = prefix };
    public NewArray WithMarkers(Markers markers) => this with { Markers = markers };
    public NewArray WithTypeExpression(TypeTree? typeExpression) => this with { TypeExpression = typeExpression };
    public NewArray WithDimensions(IList<ArrayDimension> dimensions) => this with { Dimensions = dimensions };
    public NewArray WithInitializer(JContainer<Expression>? initializer) => this with { Initializer = initializer };
    public NewArray WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A constructor invocation (new expression).
/// Examples:
///   new Foo()
///   new Foo(arg1, arg2)
///   new List&lt;int&gt;()
/// </summary>
public sealed record NewClass(
    Guid Id,
    Space Prefix,                          // Space before entire expression
    Markers Markers,
    JRightPadded<Expression>? Enclosing,   // For outer.new Inner() style (rare in C#)
    Space New,                             // Space after 'new' keyword
    TypeTree? Clazz,                       // The type being constructed
    JContainer<Expression> Arguments,      // Constructor arguments (includes parens)
    Block? Body,                           // For anonymous classes/initializers
    JavaType.Method? ConstructorType       // Type attribution
) : J, Expression, Statement
{
    public NewClass WithId(Guid id) => this with { Id = id };
    public NewClass WithPrefix(Space prefix) => this with { Prefix = prefix };
    public NewClass WithMarkers(Markers markers) => this with { Markers = markers };
    public NewClass WithEnclosing(JRightPadded<Expression>? enclosing) => this with { Enclosing = enclosing };
    public NewClass WithNew(Space @new) => this with { New = @new };
    public NewClass WithClazz(TypeTree? clazz) => this with { Clazz = clazz };
    public NewClass WithArguments(JContainer<Expression> arguments) => this with { Arguments = arguments };
    public NewClass WithBody(Block? body) => this with { Body = body };
    public NewClass WithConstructorType(JavaType.Method? constructorType) => this with { ConstructorType = constructorType };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A ternary conditional expression (e.g., condition ? trueExpr : falseExpr).
/// Also used for null-coalescing (a ?? b) with NullCoalescing marker.
///
/// For regular ternary: condition ? truePart : falsePart
/// For null-coalescing: condition ?? falsePart (truePart is empty, has NullCoalescing marker)
/// </summary>
public sealed record Ternary(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Condition,           // The condition expression
    JLeftPadded<Expression> TruePart,  // Space before ? and true expression (empty for ??)
    JLeftPadded<Expression> FalsePart, // Space before : (or ??) and false expression
    JavaType? Type
) : J, Expression, Statement
{
    public Ternary WithId(Guid id) => this with { Id = id };
    public Ternary WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Ternary WithMarkers(Markers markers) => this with { Markers = markers };
    public Ternary WithCondition(Expression condition) => this with { Condition = condition };
    public Ternary WithTruePart(JLeftPadded<Expression> truePart) => this with { TruePart = truePart };
    public Ternary WithFalsePart(JLeftPadded<Expression> falsePart) => this with { FalsePart = falsePart };
    public Ternary WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A switch statement.
/// Example:
///   switch (x) {
///       case 1:
///           DoSomething();
///           break;
///       default:
///           DoDefault();
///           break;
///   }
/// </summary>
public sealed record Switch(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ControlParentheses<Expression> Selector,  // The (expression) being switched
    Block Cases                                // Block containing Case elements
) : J, Statement
{
    public Switch WithId(Guid id) => this with { Id = id };
    public Switch WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Switch WithMarkers(Markers markers) => this with { Markers = markers };
    public Switch WithSelector(ControlParentheses<Expression> selector) => this with { Selector = selector };
    public Switch WithCases(Block cases) => this with { Cases = cases };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A switch expression (Java 14+ / C# 8+).
/// Java: switch (x) { case 1 -> "one"; default -> "other"; }
/// C#: x switch { 1 => "one", _ => "other" }
/// For C#, the Selector stores the expression and space before 'switch' keyword
/// (Selector.Prefix = space before selector for C#, Selector.Tree.After = space before 'switch').
/// </summary>
public sealed record SwitchExpression(
    Guid Id,
    Space Prefix,
    Markers Markers,
    ControlParentheses<Expression> Selector,  // For C#: no parens, just selector + space before 'switch'
    Block Cases,                               // Block containing Case elements
    JavaType? Type
) : J, Expression
{
    public SwitchExpression WithId(Guid id) => this with { Id = id };
    public SwitchExpression WithPrefix(Space prefix) => this with { Prefix = prefix };
    public SwitchExpression WithMarkers(Markers markers) => this with { Markers = markers };
    public SwitchExpression WithSelector(ControlParentheses<Expression> selector) => this with { Selector = selector };
    public SwitchExpression WithCases(Block cases) => this with { Cases = cases };
    public SwitchExpression WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// The kind of case statement.
/// </summary>
public enum CaseType
{
    /// <summary>
    /// Traditional case with colon: case 1: statements...
    /// </summary>
    Statement,
    /// <summary>
    /// Arrow case (switch expression arm): case 1 => expression
    /// </summary>
    Rule
}

/// <summary>
/// A case within a switch statement or expression.
/// Examples:
///   case 1:                    (Statement type)
///   case 1, 2:                 (multiple labels)
///   default:                   (default case)
///   case 1 => expr             (Rule type, switch expression)
///   case > 5 when x < 10:      (pattern with guard)
/// </summary>
public sealed record Case(
    Guid Id,
    Space Prefix,
    Markers Markers,
    CaseType CaseKind,                         // Statement (colon) or Rule (arrow)
    JContainer<J> CaseLabels,                  // The case labels/patterns (includes 'case' keyword space)
    Expression? Guard,                         // Optional 'when' guard expression
    JContainer<Statement> Statements,          // For Statement type: statements after colon
    JRightPadded<J>? Body                      // For Rule type: expression after arrow
) : J, Statement
{
    public Case WithId(Guid id) => this with { Id = id };
    public Case WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Case WithMarkers(Markers markers) => this with { Markers = markers };
    public Case WithCaseKind(CaseType caseKind) => this with { CaseKind = caseKind };
    public Case WithCaseLabels(JContainer<J> caseLabels) => this with { CaseLabels = caseLabels };
    public Case WithGuard(Expression? guard) => this with { Guard = guard };
    public Case WithStatements(JContainer<Statement> statements) => this with { Statements = statements };
    public Case WithBody(JRightPadded<J>? body) => this with { Body = body };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A lambda expression: (params) => body
/// Examples:
///   x => x * 2
///   (x, y) => x + y
///   (int x) => { return x; }
///   () => Console.WriteLine()
/// </summary>
public sealed record Lambda(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Lambda.Parameters Params,      // Lambda parameters
    Space Arrow,                    // Space before =>
    J Body,                         // Expression or Block
    JavaType? Type
) : J, Expression, Statement
{
    public Lambda WithId(Guid id) => this with { Id = id };
    public Lambda WithPrefix(Space prefix) => this with { Prefix = prefix };
    public Lambda WithMarkers(Markers markers) => this with { Markers = markers };
    public Lambda WithParams(Parameters @params) => this with { Params = @params };
    public Lambda WithArrow(Space arrow) => this with { Arrow = arrow };
    public Lambda WithBody(J body) => this with { Body = body };
    public Lambda WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);

    /// <summary>
    /// Lambda parameters, optionally wrapped in parentheses.
    /// For x => ..., Parenthesized is false.
    /// For (x) => ... or (x, y) => ..., Parenthesized is true.
    /// </summary>
    public sealed record Parameters(
        Guid Id,
        Space Prefix,              // Space before ( if parenthesized, otherwise empty
        Markers Markers,
        bool Parenthesized,        // Whether wrapped in parentheses
        IList<JRightPadded<J>> Elements  // The parameters (usually VariableDeclarations or Identifiers)
    ) : J
    {
        public Parameters WithId(Guid id) => this with { Id = id };
        public Parameters WithPrefix(Space prefix) => this with { Prefix = prefix };
        public Parameters WithMarkers(Markers markers) => this with { Markers = markers };
        public Parameters WithParenthesized(bool parenthesized) => this with { Parenthesized = parenthesized };
        public Parameters WithElements(IList<JRightPadded<J>> elements) => this with { Elements = elements };

        Tree Tree.WithId(Guid id) => WithId(id);
    }
}

/// <summary>
/// A deconstruction pattern for positional pattern matching.
/// Used for record patterns in Java and positional/tuple patterns in C#.
/// Examples:
///   case Point(int x, int y):     // Named type deconstruction
///   case (int x, int y):          // C# tuple deconstruction (Deconstructor = Empty)
///   case (> 0, > 0):              // Nested patterns
/// </summary>
public sealed record DeconstructionPattern(
    Guid Id,
    Space Prefix,
    Markers Markers,
    Expression Deconstructor,     // The type being deconstructed; J.Empty for tuple patterns without type
    JContainer<J> Nested,         // The ( pattern, pattern, ... )
    JavaType? Type
) : J, Expression
{
    public DeconstructionPattern WithId(Guid id) => this with { Id = id };
    public DeconstructionPattern WithPrefix(Space prefix) => this with { Prefix = prefix };
    public DeconstructionPattern WithMarkers(Markers markers) => this with { Markers = markers };
    public DeconstructionPattern WithDeconstructor(Expression deconstructor) => this with { Deconstructor = deconstructor };
    public DeconstructionPattern WithNested(JContainer<J> nested) => this with { Nested = nested };
    public DeconstructionPattern WithType(JavaType? type) => this with { Type = type };

    Tree Tree.WithId(Guid id) => WithId(id);
}

/// <summary>
/// A container for a list of elements with shared prefix.
/// </summary>
public sealed record JContainer<T>(Space Before, IList<JRightPadded<T>> Elements, Markers Markers)
{
    public static JContainer<T> Empty() => new(Space.Empty, [], Core.Markers.Empty);

    public JContainer<T> WithBefore(Space before) => this with { Before = before };
    public JContainer<T> WithElements(IList<JRightPadded<T>> elements) => this with { Elements = elements };
    public JContainer<T> WithMarkers(Markers markers) => this with { Markers = markers };
}

/// <summary>
/// Left-padded element with space before it.
/// </summary>
public sealed record JLeftPadded<T>(Space Before, T Element)
{
    public JLeftPadded<T> WithBefore(Space before) => this with { Before = before };
    public JLeftPadded<T> WithElement(T element) => this with { Element = element };
}

/// <summary>
/// Right-padded element with space after it.
/// </summary>
public sealed record JRightPadded<T>(T Element, Space After, Markers Markers)
{
    public static JRightPadded<T> Build(T element) => new(element, Space.Empty, Core.Markers.Empty);

    public JRightPadded<T> WithElement(T element) => this with { Element = element };
    public JRightPadded<T> WithAfter(Space after) => this with { After = after };
    public JRightPadded<T> WithMarkers(Markers markers) => this with { Markers = markers };
}
