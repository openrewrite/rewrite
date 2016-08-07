# Distributed Java Source Refactoring

![Support Status](https://img.shields.io/badge/nebula-incubating-yellow.svg)
[![Build Status](https://travis-ci.org/nebula-plugins/java-source-refactor.svg?branch=master)](https://travis-ci.org/nebula-plugins/java-source-refactor)
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/nebula-plugins/java-source-refactor?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Apache 2.0](https://img.shields.io/github/license/nebula-plugins/java-source-refactor.svg)](http://www.apache.org/licenses/LICENSE-2.0)

## Purpose

The Java Source Refactoring plugin is a pluggable and distributed refactoring tool for Java source code.  **This is an incubating feature**.

It allows for the creation of style-preserving, type-aware refactoring changes that can be described programatically and distributed to the organization through the build process.

Refactoring rules are packed into libraries (generally the library which is trying to deprecate some aspect of its API). When other projects pick up a new version of the library with a refactoring rule, it is applied to their source code. In this sense, the refactoring operation is distributed to dependent teams and source code across the organization adapts to the change in an "eventually consistent" manner.

## Usage

Clone and build with:

    ./gradlew publishToMavenLocal

To apply this plugin:

    buildscript {
        repositories { mavenLocal() }
        dependencies {
            classpath 'com.netflix.nebula:java-source-refactor:latest.release'
        }

        configurations.classpath.resolutionStrategy.cacheDynamicVersionsFor 0, 'minutes'
    }

    apply plugin: 'nebula.source-refactor'
    
To perform refactoring, run `./gradlew fixSourceLint`.
    
The plugin scans the classpath looking for methods annotated with `@Refactor` and applies the rule defined by each of
these methods to the project's source.
    
## Writing Refactor Rules

To create a new rule, provide a public static method annotated with `@Refactor` that takes a `Refactorer` argument.

```java
@Refactor(value = "foo-to-bar", description = "replace foo() with bar()")
public class FooToBar extends JavaSourceVisitor {
    public void visit(JavaSource source) {
        source.refactor()
            .findMethodCalls("B foo(int)")
                .changeName("bar")
                .done()
            .changeType(B.class, B2.class)
            .fix();
    }
}
```

In the example rule above, two refactoring operations are chained together into one operation: changing invocations of `B.foo` to
`B.bar()` and types of `B` to `B2`. Together, this changes method invocations of `B.foo` to `B2.bar`.

That's it! Any project that declares a dependency on the artifact that contains your new rule and applies `nebula.source-refactor` will
now be refactorable.

# License

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
