# rewrite-scala

OpenRewrite support for Scala source code. Parses Scala 3 source files into a Lossless Semantic Tree (LST) that reuses the Java `J` model wherever possible, adding Scala-specific `S` types and markers only when necessary.

## Syntax Element Mapping

The table below catalogs every Scala syntax element, the LST element it maps to, and example syntax. The **Why S/marker?** column explains why a Scala-specific type or marker was needed instead of a plain `J` mapping.

### Scala-specific LST types (`S.*`)

| Scala syntax | LST element | Example(s) | Why S type? |
|---|---|---|---|
| Compilation unit | `S.CompilationUnit` | `package foo; class Bar { }` | Scala files have top-level statements (vals, defs, expressions) outside classes. `J.CompilationUnit` only holds `List<ClassDeclaration>`, not arbitrary statements. |
| Tuple pattern / destructuring | `S.TuplePattern` | `val (a, b) = (1, 2)` | Java has no destructuring bind. A tuple pattern contains an ordered list of names that bind simultaneously, which has no `J` equivalent. |
| Underscore placeholder | `S.Wildcard` | `list.map(_ * 2)`, `add(5, _)` | Represents `_` as an *expression* (partially applied argument or placeholder lambda body). `J.Wildcard` is a *type* wildcard (`? extends T`), not an expression. |
| Block expression | `S.BlockExpression` | `val x = { val t = 1; t + 1 }` | In Scala, `{ ... }` is an expression that returns its last value. `J.Block` is a statement, not an expression, so it cannot appear on the right side of an assignment. |

### Scala-specific markers

| Scala syntax | LST element + marker | Example(s) | Why marker? |
|---|---|---|---|
| Object (singleton) | `J.ClassDeclaration` + `SObject` | `object App { }`, `case object Empty` | Structurally identical to a class declaration but printed with `object` keyword. The marker distinguishes the keyword without a new tree node. |
| Trait | `J.ClassDeclaration` (`Kind.Interface`) | `trait Animal { }` | Printed as `trait` instead of `interface`. Uses the existing `Kind.Type.Interface` enum; no marker needed. |
| `val` / `var` | `J.VariableDeclarations` (+ `Final` modifier for `val`) | `val x = 1`, `var y = 2` | `val` maps to a `final` variable, `var` to a non-final. The printer outputs `val`/`var` based on the `Final` modifier. |
| `lazy val` | `J.VariableDeclarations` + `LanguageExtension` modifier (`"lazy"`) | `lazy val x = expensive()` | Java has no `lazy` keyword. Uses the standard extended modifier pattern (`Type.LanguageExtension` + `keyword = "lazy"`) rather than a custom marker. |
| Lambda parameter | `J.VariableDeclarations` + `LambdaParameter` | `(x: Int) => x + 1` | Lambda params look like variable declarations but must not print `val`/`var`. The marker suppresses the keyword. |
| Underscore placeholder lambda | `J.Lambda` + `UnderscorePlaceholderLambda` | `_ + 1`, `_.toString` | The lambda has no explicit parameter list. The marker tells the printer to skip parameters and print `_` placeholders in the body via `S.Wildcard`. |
| Function application | `J.MethodInvocation` + `FunctionApplication` | `println("hi")`, `arr(0)` | Syntactic sugar for `.apply()`. Structurally a method invocation, but printed without a dot or method name — just `target(args)`. |
| Infix notation | `J.MethodInvocation` + `InfixNotation` | `list map f`, `a + b` (custom ops) | A method call written as `a op b` instead of `a.op(b)`. The marker tells the printer to omit the dot and parentheses. |
| Implicit return | `J.Return` + `ImplicitReturn` | `def add(a: Int, b: Int) = a + b` | Scala returns the last expression without `return`. The marker tells the printer to emit only the expression, not the `return` keyword. |
| Omitted braces | `J.Block` + `OmitBraces` | `object Empty` (no body) | The class has no body block in source. The marker tells the printer to skip emitting `{ }`. |
| Implicit modifier | `J.Modifier` + `Implicit` | `object Foo` (implicitly `final`) | Objects are implicitly final in Scala. The modifier exists in the LST for semantic correctness but the marker prevents it from being printed. |
| Scala for loop | `J.ForLoop` + `ScalaForLoop` | `for (i <- 0 until 10) { ... }` | Range-based `for` with generators (`<-`) has no Java equivalent syntax. The marker stores the original source to round-trip print correctly. |

### Standard `J` mappings (no Scala-specific type needed)

| Scala syntax | LST element | Example(s) |
|---|---|---|
| Package declaration | `J.Package` | `package com.example` |
| Import | `J.Import` | `import java.util.List`, `import scala.collection._` |
| Class declaration | `J.ClassDeclaration` | `class Person(name: String)`, `case class Point(x: Int, y: Int)` |
| Method definition | `J.MethodDeclaration` | `def greet(name: String): String = "Hello " + name` |
| Constructor parameters | `J.ClassDeclaration` primary constructor | `class Foo(val x: Int, var y: Int)` |
| Type parameters | `J.TypeParameter` | `class Box[T]`, `def id[A](a: A): A = a` |
| Literal | `J.Literal` | `42`, `"hello"`, `true`, `3.14` |
| Identifier | `J.Identifier` | `x`, `myVar`, `println` |
| Field access | `J.FieldAccess` | `obj.field`, `pkg.ClassName` |
| Method invocation | `J.MethodInvocation` | `obj.method(arg)`, `list.map(f)` |
| Binary expression | `J.Binary` | `a + b`, `x == y`, `a && b` |
| Unary expression | `J.Unary` | `!flag`, `-x`, `~bits` |
| Assignment | `J.Assignment` | `x = 10` |
| Compound assignment | `J.AssignmentOperation` | `x += 1`, `y *= 2` |
| Parenthesized expression | `J.Parentheses` | `(a + b)` |
| If expression | `J.If` | `if (x > 0) "pos" else "neg"` |
| While loop | `J.WhileLoop` | `while (running) { step() }` |
| For-each loop | `J.ForEachLoop` | `for (x <- list) { println(x) }` |
| Return | `J.Return` | `return 42` |
| Throw | `J.Throw` | `throw new Exception("err")` |
| Try / catch / finally | `J.Try` | `try { risky() } catch { case e: Exception => handle(e) }` |
| New instance | `J.NewClass` | `new Person("Alice")`, `new Trait { }` |
| Type cast | `J.TypeCast` | `x.asInstanceOf[Int]` |
| Instance check | `J.InstanceOf` | `x.isInstanceOf[String]` |
| Parameterized type | `J.ParameterizedType` | `List[Int]`, `Map[String, Any]` |
| Array creation | `J.NewArray` | `Array(1, 2, 3)`, `Array[Int](1, 2)` |
| Lambda | `J.Lambda` | `(x: Int) => x + 1`, `(a, b) => a + b` |
| Member reference | `J.MemberReference` | `greet _` |
| Block | `J.Block` | `{ val x = 1; x + 1 }` |
| Annotation | `J.Annotation` | `@deprecated`, `@throws(classOf[Exception])` |
| Synchronized | `J.Synchronized` | `synchronized { counter += 1 }` |
| Break / Continue | `J.Break` / `J.Continue` | `break`, `continue` (via `scala.util.control.Breaks`) |

### Printing differences

Even when Scala maps to the same `J` type as Java, the `ScalaPrinter` adjusts the output syntax:

| Feature | Java syntax | Scala syntax |
|---|---|---|
| Type parameters | `<T>` | `[T]` |
| Type cast | `(Type) expr` | `expr.asInstanceOf[Type]` |
| Instance check | `expr instanceof Type` | `expr.isInstanceOf[Type]` |
| Array access | `arr[i]` | `arr(i)` |
| Array creation | `new int[]{1,2}` | `Array(1, 2)` |
| Wildcard import | `import java.util.*` | `import java.util._` |
| Interface | `interface Foo` | `trait Foo` |
| Statement terminator | `;` required | `;` omitted |
| Variable declaration | `int x = 1` | `val x: Int = 1` (type after name) |
| Lambda arrow | `->` | `=>` |
| Type bounds | `<T extends U>` | `[T <: U]` |
| Trait mixing | `implements A, B` | `extends A with B` |
