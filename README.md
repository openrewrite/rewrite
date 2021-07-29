![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)
# Semantic code search and transformation

[![ci](https://github.com/openrewrite/rewrite/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite/rewrite-java.svg)](https://mvnrepository.com/artifact/org.openrewrite/rewrite-java)

The Rewrite project is a mass refactoring ecosystem for Java and other source code, designed to eliminate technical debt across an engineering organization. It consists of a platform of prepackaged refactoring recipes for common framework migration and stylistic consistency tasks in Java, ready for you to apply in your build via Maven or Gradle plugins.

Read the full documentation at [docs.openrewrite.org](https://docs.openrewrite.org/).

Feel free to join us on [Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA)!

## Building & Developing OpenRewrite

We use [Gradle](https://gradle.org/) to build this project.
The gradle wrapper checked into this project defines the gradle version to use.  
When building from the command line invoke the wrapper with `./gradlew build` on unix-style terminals and `gradlew build` on windows-style terminals.
 
### CLI Environment Configuration:

* [JDK](https://adoptopenjdk.net/) version: 11
  * JDK language & bytecode level: 1.8
* [Gradle](https://gradle.org/) version: Defined in wrapper
* [Kotlin](https://kotlinlang.org/) version: 1.5
  * Kotlin language level: 1.5
  * Kotlin JVM bytecode level: 1.8 

### IDE Configuration

We use [IntelliJ IDEA](https://www.jetbrains.com/idea/) to develop this project. 
Other IDEs or versions of this IDE can be made to work. 
These are one set of versions we know works:

* IDEA version:  2021.1.3

You must set the `-parameters` compiler flag to run Rewrite tests. 
If your system does not have UTF-8 as its default character encoding (e.g., Windows) you must also add `-encoding utf8`.
Add these to the "additional command line parameters" field in IntelliJ -> Preferences -> Build, Execution, Deployment -> Compiler -> Java Compiler.
