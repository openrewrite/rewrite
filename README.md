# Rewrite - Eliminate Tech-Debt. Automatically.

[![Build Status](https://circleci.com/gh/openrewrite/rewrite-checkstyle.svg?style=shield)](https://circleci.com/gh/openrewrite/rewrite)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite/rewrite-java.svg)](https://mvnrepository.com/artifact/org.openrewrite/rewrite-java)

The Rewrite project is a refactoring tool for Java and other source code. It contains a custom Abstract Syntax Tree (AST)  that encodes the structure and formatting of your source code. The AST is printable to
reconstitute the source code, including its original formatting.

Rewrite provides high-level search functions and refactoring functions that can transform the AST.

The AST is imbued with information about types (and their type hierarchies)
of expressions and statements in your code.

Rewrite provides visitor support over its AST. Basic visitors for printing the AST, transforming it with refactoring
operations, etc. are provided out of the box.

Rewrite provides utilities for unit testing refactoring logic and custom visitors.
