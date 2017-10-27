# Rewrite - Distributed Java Source Refactoring

[![Build Status](https://travis-ci.org/Netflix-Skunkworks/rewrite.svg?branch=master)](https://travis-ci.org/Netflix-Skunkworks/rewrite)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/Netflix-Skunkworks/rewrite?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/Netflix-Skunkworks/rewrite.svg)](http://www.apache.org/licenses/LICENSE-2.0)

- [Installing](#installing)
- [Features](#features)
- [Example usage](#example-usage)
  - [Generating a git-style patch](#generating-a-git-style-patch)
- [License](#license)

The Rewrite project is a refactoring tool for Java source code. It contains a custom Abstract Syntax Tree (AST) supporting
Java 8 language features that encodes the structure and formatting of your source code. The AST is printable to
reconstitute the source code, including its original formatting.

Rewrite provides high-level search functions and refactoring functions that can transform the AST.

The AST is imbued with information about types (and their type hierarchies)
of expressions and statements in your code.

Rewrite provides visitor support over its AST. Basic visitors for printing the AST, transforming it with refactoring
operations, etc. are provided out of the box.

Rewrite provides utilities for unit testing refactoring logic and custom visitors.

At Netflix, we operate on thousands of repositories with refactoring rules written in Rewrite, issuing pull requests
en masse from an Apache Spark cluster. It is possible to execute these operations as part of your build process,
to incorporate them into IDE plugins, etc. depending on the needs of your organization.

To maximize the freedom of our engineers to incorporate changes at their convenience, Rewrite's objective is to
facilitate the incorporation of refactoring changes by the affected team or engineer. In this way, we consider
these operations to be *distributed refactoring*.

**This is an incubating feature**.

## Installing

Install the dependency from Maven Central or JCenter with:

```xml
<dependency>
    <groupId>com.netflix.devinsight.rewrite</groupId>
    <artifactId>rewrite-core</artifactId>
    <version>1.2.0</version>
</dependency>
```

or

```groovy
compile 'com.netflix.devinsight.rewrite:rewrite-core:1.2.0'
```

Add the Maven or Gradle classifier `jdkbundle` to fetch a version of the package that package relocates and shades the relevant parts of the JDK needed for parsing into the distribution.

## Features

* Building blocks
  - [Abstract Syntax Tree (AST)](https://github.com/Netflix/rewrite/wiki/Abstract-Syntax-Tree-(AST))
  - [Visitors and cursors](https://github.com/Netflix/rewrite/wiki/Visitors-and-Cursors)
* Code Search
  - [Finding method invocations](https://github.com/Netflix/rewrite/wiki/Finding-Method-Invocations)
  - [Finding fields](https://github.com/Netflix/rewrite/wiki/Finding-Fields)
  - [Finding types](https://github.com/Netflix/rewrite/wiki/Finding-Types)
  - [Finding annotations](https://github.com/Netflix/rewrite/wiki/Finding-Annotations)
  - [Has type/has import](https://github.com/Netflix/rewrite/wiki/Has-Type-and-Has-Import)
* Refactoring
  - [Changing method invocations](https://github.com/Netflix/rewrite/wiki/Changing-Method-Invocations)
  - [Changing types](https://github.com/Netflix/rewrite/wiki/Changing-Types)
  - [Adding and deleting fields](https://github.com/Netflix/rewrite/wiki/Adding-and-Deleting-Fields)
  - [Adding and removing imports](https://github.com/Netflix/rewrite/wiki/Adding-and-Removing-Imports)

## Example usage

Below is a simple example of a refactoring operation that changes the name of a method.

```java
public class ChangeMethodNameTestJava {
    Parser parser = new OracleJdkParser(); // pass binary dependencies to this constructor on a real project

    @Test
    public void refactorMethodName() {
        String a = "class A {{ B.foo(0); }}";
        String b = "class B { static void foo(int n) {} }";

        Tr.CompilationUnit cu = parser.parse(a, /* which depends on */ b);

        Refactor refactor = cu.refactor()
          .changeMethodName(cu.findMethodCalls("B foo(int)"), "bar");

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
operation by calling `refactor` on our compilation unit. We search for method invocations matching a certain signature (using the AspectJ pointcut grammar),
and for each matching invocation, change the name to a method called `bar`.

Next, we call `fix` to return a copy of the original AST with the refactoring changes made.

Lastly, we call `print` on the AST to emit the source code for the resulting change. Notice how the original style of the class was preserved!

To cut down a bit on the ceremony, we can shorten the process of setting up and executing a refactor operation into one call chain:

```java
Tr.CompilationUnit fixed = cu.refactor().changeName(cu.findMethodCalls("B foo(int)"), "bar").fix();
```

### Generating a git-style patch

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
