## Rewrite Java Technology Compatibility Kit

This project contains the source code for the Rewrite Java Technology Compatibility Kit (TCK). The TCK is a set of tests that can be used to verify that a `JavaParser` implementation conforms to expectations.

### Dependency relationships with `rewrite-java-tck`

Dependency relationships between parser implementations and the TCK:

```mermaid
---
title: Dependency relationships
---
classDiagram
`rewrite-java-tck` ..> `rewrite-java`: implementation
`rewrite-java-tck` ..> `rewrite-java-test`: implementation
`rewrite-java-17` ..> `rewrite-java-tck` : `compatibilityTest classpath`
`rewrite-java-11` ..> `rewrite-java-tck` : `compatibilityTest classpath`
`rewrite-java-8` ..> `rewrite-java-tck` : `compatibilityTest classpath`
`rewrite-groovy` ..> `rewrite-java-test` : testImplementation
`rewrite-java` ..> `rewrite-java-test` : testImplementation
note for `rewrite-java` "For Java Reflection type mapping"
`rewrite-java-tck` ..> `rewrite-java-17`: testRuntimeOnly
note for `rewrite-java-tck` "Bound to the latest current language level"
```
