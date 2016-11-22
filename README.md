# Rewrite - Distributed Java Source Refactoring

[![Build Status](https://travis-ci.org/Netflix/rewrite.svg?branch=master)](https://travis-ci.org/Netflix/rewrite)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Netflix/rewrite?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/Netflix/rewrite.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Purpose

The Rewrite project is a pluggable and distributed refactoring tool for Java source code.  **This is an incubating feature**.

It consists of an interface that allows you to perform type-aware searches for code patterns and make style-preserving refactoring changes.

## Installing

Install the dependency from Maven Central or JCenter with:

```xml
<dependency>
    <groupId>com.netflix.devinsight</groupId>
    <artifactId>rewrite</artifactId>
    <version>0.7.0</version>
</dependency>
```

or

```groovy
compile 'com.netflix.devinsight:rewrite:0.7.0'
```

## Example usage

Below is a simple example of a refactoring operation that changes the name of a method.

```java
public class ChangeMethodNameTestJava {
    Parser parser = new OracleJdkParser(); // pass binary dependencies to this constructor on a real project

    @Test
    public void refactorMethodName() {
        String a = "class A {{ B.foo(0); }}";
        String b = "class B { static void foo(int n) {} }";

        final Tr.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        Refactor refactor = cu.refactor();

        for (Tr.MethodInvocation inv : cu.findMethodCalls("B foo(int)")) {
             refactor.changeName(inv, "bar");
        }

        Tr.CompilationUnit fixed = refactor.fix();

        assertEquals(fixed.print(), "class A {{ B.bar(0); }}");
    }
}
```

First, we construct a `Parser` instance, in this case an `OracleJdkParser` which will tightly control the parsing and type attribution phases of the standard
Oracle JDK to produce an abstract syntax tree (AST) that we can work with. If we were working with a real project, we would pass a `List<Path>` of the binary
dependencies of the project to the `OracleJdkParser` constructor.

Next, we use the parser to parse some source code. Typically, you would pass a `List<Path>` of all the source files in the project, and `parse` would return
a `List<Tr.CompilationUnit>` representing the ASTs of each source file in order. Here we are using a convenience utility that is especially handy while writing
tests that allows us to pass any number of strings each representing a different Java source file, and we will receive back a single `Tr.CompilationUnit` for the first
source string in the list. Note that a compilation unit is a combination of package declaration, imports, and all of the types (classes, interfaces, enums, etc.) defined
in the file. Remember that, while usually there is one type per file whose name matches the file name, Java does allow additional non-public types to be defined inside
a single Java source file.

At this point, we can use the `Tr.CompilationUnit` to either do a type-aware deep dive on the code or perform a refactoring operation. Here, we begin a refactoring
operation by calling `refactor` on our compilation unit. The refactoring operation searches for method invocations matching a certain signature (using the AspectJ pointcut grammar),
and for each matching invocation, changes the name to a method called `bar`.

Next, we call `fix` to return a copy of the original AST with the refactoring changes made.

Lastly, we call `print` on the AST to emit the source code for the resulting change. Notice how the original style of the class was preserved!

To cut down a bit on the ceremony, we can shorten the process of setting up and executing a refactor operation into one call:

```java
Tr.CompilationUnit fixed = cu.refactor(refactor -> {
    for (Tr.MethodInvocation inv : cu.findMethodCalls("B foo(int)")) {
         refactor.changeName(inv, "bar");
    }
}).fix();
```

### Generating a git-style patch for a refactor

Rather than calling `fix` on a `Refactor` instance as described above, you may also call `diff`() which generates a git-style patch that can be used to generate
a pull request or submit a patch for review and integration later. We also use `diff` when performing a refactoring operation across thousands of projects to wrap
our heads around what kinds of changes are going to be made to the source code and sanity-check that our refactoring logic makes sense.

Let's adjust our definition of the A class in our above example to incorporate some newlines, so that it looks like this:

```java
class A {
   {
      B.foo(0);
   }
}
```

Performing the same refactoring operation, but calling `diff` instead would yield:

```text
diff --git a//home/A.java b//home/A.java
index 9b034e8..0234fb8 100644
--- a//home/A.java
+++ b//home/A.java
@@ -1,5 +1,5 @@
 class A {
    {
-      B.foo(0);
+      B.bar(0);
    }
 }
\ No newline at end of file
```

### Abstract Syntax Tree (AST) Type Reference

Below is a visual reference of the AST elements that are available:

* Tr.Annotation - `@SuppressWarnings("ALL")`
* Tr.ArrayAccess - The index portion of `myArray[0]`
* Tr.ArrayType - The type portion of `String[][] foo`
* Tr.AssignOp - `n += 1`
* Tr.Assign - `n = 0`
* Tr.Binary - `n + 1`
* Tr.Block - The class body and method body portions of:
```java
public class A {
	public void foo() {  }
}
```
* Tr.Break - `break label` (optional label)
* Tr.Case - Case statements inside switches `case 0:` including `default`.
* Tr.Catch - `catch (Throwable t) { }`
* Tr.ClassDecl - Any type declaration:
	- `class A {}`
	- `interface A {}`
	- `@interface A {}`
	- `enum A {}`
* Tr.Comment - Raw comments, either block or single line.
* Tr.CompilationUnit - Package declaration (optional), imports (optional), and any type declarations contained in a single Java source file.
* Tr.Continue - `continue label` (optional label).
* Tr.DoWhile - `do { } while(cond)`
* Tr.Empty - Empty statements can appear in as statements in blocks (`{ ; }`) and other constructs like for loops (e.g. `for(;;) {}`)
* Tr.FieldAccess - `new A().myField`
* Tr.ForEachLoop - `for(A a: listOfAs) { }`
* Tr.ForLoop - `for(int i = 0; i < 10; i++) { }`
* Tr.Ident - Either a qualified (`com.foo.A`) or unqualified (`A`, `int`, `myField`) name. Refers to either a package portion, a type, or variable name, depending on the context.
* Tr.If - `if(cond) { } else if(cond2) { } else { }`
* Tr.Import - A regular or static import, including wildcard imports (e.g. `import com.foo.A`)
* Tr.InstanceOf - `a instanceof A`
* Tr.Label - The label and target statement of `labeled: while(cond) { }`
* Tr.Lambda - `(String s) -> s.substring(1)`
* Tr.Literal - `0`, `'a'`, `"a"`. Also preserves base markers `010`, `0xA0`, `0b01` and type both standard and non-standard markers `0.0f`, `1.0d`, `1.0D`, `1L`.
* Tr.MethodDecl - `public <P, R> R foo(P p, String s, String... args) { }`
* Tr.MethodInvocation - `a.foo(0, 1, 2)`
* Tr.NewArray - `new int[0]`
* Tr.NewClass - `new A.B() {}`
* Tr.Parentheses - `( 0 )`
* Tr.Primitive - Where type declarations are possible: `int`, `double`, etc.
* Tr.Return - `return value` (optional value)
* Tr.Switch - `switch(n) { case 0: break; }`
* Tr.Synchronized - `synchronized(mutex) { }`
* Tr.Ternary - `n % 2 == 0 ? "even" : "odd"`
* Tr.Throw - `throw new UnsupportedOperationException()`
* Tr.TryCatch - `try { } catch(Throwable t) { } finally { }`
* Tr.TypeCast -  `(Class<A>)`
* Tr.TypeParameter - `<P>` and `<P extends A>`
* Tr.Unary - `i++`, `!foo`, etc.
* Tr.VariableDecls - Single or multi-variable declaration, e.g. `Integer n = 0` and `Integer n = 0, m = 0`
* Tr.WhileLoop - `while(cond) { }`
* Tr.Wildcard - `<?>`, `<? extends A`>, and `<? super A>`

## License

Copyright 2015-2016 Netflix, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<http://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
