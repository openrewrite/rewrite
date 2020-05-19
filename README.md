# Rewrite - Eliminate Tech-Debt. Automatically.

[![Build Status](https://circleci.com/gh/openrewrite/rewrite.svg?style=shield)](https://circleci.com/gh/openrewrite/rewrite)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite/rewrite-java.svg)](https://mvnrepository.com/artifact/org.openrewrite/rewrite-java)

## What is this?

The Rewrite project is a mass refactoring ecosystem for Java and other source code, designed to eliminate technical debt across an engineering organization. Rewrite is designed to be plugged into various workflows, including:

* Discover and fix code as a **build tool task** (e.g. Gradle and Maven).
* Subsecond organization-wide code search for a pattern of arbitrary complexity.
* Mass pull-request issuance to fix a security vulnerability, eliminate the use of a deprecated API, migrate from one technology to another (e.g. JUnit asserts to AssertJ), etc.
* Mass organization-wide Git commits to do the same.

It builds on a custom Abstract Syntax Tree (AST) that encodes the structure and formatting of your source code. The AST is printable to reconstitute the source code, including its original formatting.

Rewrite provides high-level search and refactoring functions that can transform the AST as well as utilities for unit testing refactoring logic.

## What is unique about the Rewrite AST?

Rewrite's AST has a unique set of characteristics that make it suitable for both single-repository and mass refactoring operations:

* **Type-attributed**. Each AST element is imbued with type information. For a field reference, for example, the source code may just refer to `myField`. The Rewrite AST element for `myField` would also contain information about what the type of `myField` is, even if it isn't defined in the same source file or even the same project.
* **Format-preserving**. Whitespace before and after AST elements is preserved in the tree so the tree can be printed out to reconstitute the original source code without clobbering formatting. Additionally, refactoring operations that insert code are sensitive to the local style of the code around them, and match the local style.
* **Acyclic and serializable**. Most AST's containing type information are potentially _cyclic_. Cycles usually come from generic type signatures like `class A<T extends A<T>>`. This kind of pattern is generally found in things like abstract builder types in Java. Rewrite cuts these cycles off and adds serialization annotations to its types so the AST can be serialized/deserialized with libraries like Jackson.

All of these properties are necessary for scalable organization-wide search and refactoring. 

Format-preservation is necessary in any organization that doesn't have absolutely consistent formatting across its whole codebase. Most organizations don't have this kind of consistency because their codebase.

Type attribution is necessary for accurate matching of patterns. For example, if we are looking for SLF4J log statements and we see a statement like the following, without type attribution how do we know if `logger` is an SLF4J or a Logback logger?

```java
logger.info("Hi");
```

The production of type-attributed ASTs for a whole organization is arbitrarily computationally complex, since it requires dependency resolution, parsing of the source code, and type attribution (basically Java compilation up to the point of bytecode generation). Since Rewrite ASTs are serializable, we can store them off centrally as a byproduct of compilation in continuous integration environments and then operate on them _en masse_ later.

Once we have a serialized AST for a particular source file, and since it also contains type information, it can be refactored/searched completely independently of other source files in the same source package or repository. This makes mass search and refactoring a truly linearly scalable operation.

## Rewrite components

Rewrite consists of a core module `rewrite-core` and a series of language bindings including `rewrite-java` and `rewrite-xml` currently.

The core module provides abstractions for building refactoring plans and executing them. It defines interfaces for outputting the transformed source, generating diffs, and mass committing changes to remote Git repositories.

Language bindings provide parsers that generate language-specific Rewrite ASTs that are type-attributed, style-preserving, and serializable.

## Creating a Rewrite AST from Java source code

To build a Rewrite AST for Java source code, construct a `JavaParser` either with or without the runtime classpath:

```java
// JavaParser constructor signatures
JavaParser();
JavaParser(List<Path> classpath);
```

Providing a classpath is optional, because type-attribution is a _best effort_ for each element. Examples of different-levels of type-attribution:

* **No types needed at all**. If you are applying a refactoring rule like autoremediation for Checkstyle's `WhitespaceBefore` rule, we're strictly looking at source formatting and it's OK if none of the AST elements have types on them, as it doesn't influence the outcome.
* **Partial types needed**. If searching for occurrences of deprecated Guava methods, it is OK to construct a `JavaParser` with a path to a Guava binary. It doesn't even have to be the Guava version that the project is using! The resulting ASTs will have limited type information, but just enough to search for what we want.
* **Full types needed**. When ASTs are emitted as a side-effect of compilation to a central data store for later arbitrary code search, they need to have full type information, because we can't be sure in advance what kinds of searches people will attempt.

`JavaParser` contains a convenience method for building a `JavaParser` from the runtime classpath of the Java process that is constructing the parser:

```java
new JavaParser(JavaParser.dependenciesFromClasspath("guava"));
```

This utility takes the "artifact name" of the dependency to look for. The artifact name is the artifact portion of `group:artifact:version` coordinates. For example, for Google's Guava (`com.google.guava:guava:VERSION`), the artifact name is `guava`.

Once you have a `JavaParser` instance, you can parse all the source files in a project with the `parse` method, which takes a `List<Path>`:

```java
JavaParser parser = ...;
List<J.CompilationUnit> cus = parser.parse(pathsToSourceFiles);
```

`J.CompilationUnit` is the top-level AST element for Java source files, which contains information about the package, imports, and any class/enum/interface definitions contained in the source file. `J.CompilationUnit` is the basic building block upon which we'll build refactoring and search operations for Java source code.