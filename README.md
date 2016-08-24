# Distributed Java Source Refactoring

![Support Status](https://img.shields.io/badge/nebula-incubating-yellow.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/java-source-refactor.svg?branch=master)](https://travis-ci.org/nebula-plugins/java-source-refactor)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/java-source-refactor?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/java-source-refactor.svg)](http://www.apache.org/licenses/LICENSE-2.0)

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
  - [Purpose](#purpose)
  - [Constructing and using a `SourceSet`](#constructing-and-using-a-sourceset)
  - [Searching for code with `JavaSourceScanner`](#searching-for-code-with-javasourcescanner)
  - [Refactoring code with `JavaSourceScanner`](#refactoring-code-with-javasourcescanner)
  - [Using the Gradle plugin](#using-the-gradle-plugin)
  - [Writing @AutoRefactor rules for the Gradle plugin](#writing-@autorefactor-rules-for-the-gradle-plugin)
  - [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

## Purpose

The Java Source Refactoring project is a pluggable and distributed refactoring tool for Java source code.  **This is an incubating feature**.

It consists of an interface that allows you to perform type-aware searches for code patterns and make style-preserving refactoring changes.
It also packs with a Gradle plugin that performs refactoring operations on a project-wide basis.

## Constructing and using a `SourceSet`

A `SourceSet` consists of Java source files and their required compile-time binary dependencies. Code search and refactoring operations are performed
against groups of Java files represented by a `SourceSet`.

Once we have a `SourceSet` instance, we can run operations against it via the `scan` method.

## Searching for code with `JavaSourceScanner`

`JavaSourceScanner` contains a single method `scan` that takes a single `JavaSource` parameter and returns any value you choose.

In this simple example, we are simply returning all the call sites for `org.slf4j.Logger.info(...)` where the call takes any number of arguments. Since a single java source file may
contain several matching calls, we are returning a `List<Method>` with the code used at each of those call sites. The results are then flattened into a single list.

```java
sourceSet
  .scan(java -> java.findMethodCalls("org.slf4j.Logger info(..)"))
  .stream()
  .flatMap(Collection::stream)
  .collect(Collectors.toList());
```

If we were just interested in the files that contained a reference, we could modify it like so:

```java
sourceSet
  .scan(java -> java.findMethodCalls("org.slf4j.Logger info(..)").isEmpty() ? null : java.file())
  .stream()
  .filter(file -> file != null)
  .collect(Collectors.toList());
```

You can find the following language constructs at this point:

| method                                      | description                                                                           |
| ------------------------------------------- | ------------------------------------------------------------------------------------- |
| findMethodCalls(String)                     | The method matching argument supports much of the AspectJ syntax for method matching  |
| findFields(Class/String)                    | Find fields matching this fully qualified name or class reference                     |
| findFieldsIncludingInherited(Class/String)  | Find fields including those declared on super types                                   |
| hasType(Class/String)                       | Simple boolean check for the existence of a reference to a types                      |
| hasImport(Class/String)                     | Simple boolean check for the existence of an import (also matches on star imports)    |

## Refactoring code with `JavaSourceScanner`

To initiate a refactoring operation, call `refactor()` on `JavaSource`. You can then chain together any number of refactoring operations and
complete the refactoring transaction with a call to `fix()`.

Here is a simple example where we just want to change all type references from `Foo` to `Bar`:

```java
sourceSet
  .scan(java -> {
    java.refactor().changeType(Foo.class, Bar.class).fix();
    return null; // we don't care to return anything in this case
  })
```

Here we change both a type and a method reference together:

```java
sourceSet
  .scan(java -> {
    java.refactor()
        .changeType(Foo.class, Bar.class)
        .findMethodCalls("com.netflix.Foo foo(String)")
          .changeName("bar")
          .changeArguments()
            .arg(0).changeLiterals(s -> s.toString().replaceAll("A", "B")).done()
            .done() // done changing arguments
          .done() // done changing the method call
        .fix();
    return null; // we don't care to return anything in this case
  })
```

Some refactoring operations, such as those directed at method calls can involve one or more individual changes and require a call to `done()` to mark the end of that change and the beginning (or continuation) of another.

## Using the Gradle plugin

Refer to the [Gradle Plugin Portal](https://plugins.gradle.org/plugin/nebula.source-refactor) for how to apply the Gradle plugin.

To perform refactoring, run `./gradlew fixSourceLint`.

The plugin scans the classpath looking for methods annotated with `@AutoRefactor` and applies the rule defined by each of
these methods to the project's source.

## Writing @AutoRefactor rules for the Gradle plugin

You may pack refactoring rules into libraries (generally the library which is trying to deprecate some aspect of its API). When other projects pick up a new version of the library with a refactoring rule, it can be applied to their source code via `./gradlew fixSourceLint`. In this way the refactoring operation is distributed to dependent teams and source code across the organization adapts to the change in an "eventually consistent" manner.

To create a new rule, provide a public static method annotated with `@AutoRefactor` and implements `JavaSourceScanner`. `JavaSourceScanner` is a generic type, and need not return anything
when used for this purpose.

```java
@AutoRefactor(value = "b-to-b2", description = "replace all references to B with B2")
public class FooToBar extends JavaSourceScanner<Void> {
    public void visit(JavaSource source) {
        source.refactor().changeType(B.class, B2.class).fix();
        return null;
    }
}
```

That's it! Any project that declares a dependency on the artifact that contains your new rule and applies `nebula.source-refactor` will
now be refactorable.

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
