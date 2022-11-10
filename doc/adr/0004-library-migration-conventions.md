# 2. Framework/Library Migration Recipe Naming Conventions

Date: 2022-11-09

## Status

Accepted

## Context

Provide standards and conventions for framework/library migration recipes such that they show up in user-facing
experiences in a consistent manner. OpenRewrite's `Recipe` class contains methods for providing a display name and
description text. The conventions outlined  within this document should be applied to all new and existing
"migration" recipes. All conventions outlined in the [Naming recipes ADR](0002-recipe-naming.md) shall also apply.  

## Decision

### Framework/Library Migration Conventions

Any recipe that involves the migration of a framework or library should use the following naming conventions for the display name and description.

#### Display Name

The general form for the display name will be:

`Migrate to <LIB_NAME> <NEW_VERSION> from <LIB_NAME> <OLD_VERSION>`

If a recipe allows for a range of versions to be upgraded, the range should be included in the display name:

`Migrate to <LIB_NAME> <NEW_VERSION> from <LIB_NAME> <LOWER_BOUND> through <UPPER_BOUND>`

* **DO** use inclusive versions for both `LOWER_BOUND` and `UPPER_BOUND` when defining the range.
* **DO** use semantic versioning `1.1.0` or `1.2.x` when a library is commonly referenced by this semantic version.
* **DO** use a version without minor/patch numbers if a library is commonly referenced without minor/patch version.
* **DO** use `x` wildcards to indicate a version includes all minor/patch versions.
* **DO NOT** use the word "upgrade" within the display name.

#### Description

The description for a framework/library migration should provide details about what is included as part of the migration.
Specifically, if a recipe includes other composite recipes, those should be noted in the description.

#### Example for "Migrate to Java 17"

**Display Name:**
```
Migrate to Java 17 from Java 8 through Java 16
```

**Description**
```
Migrate applications built with previous versions of Java to Java 17. Specifically, for those
applications that are built on Java 8, this recipe will update and add dependencies on J2EE libraries
that are no longer directly bundled with the JDK.

The use of deprecated or restricted APIs will also be migrated to code that is compliant with Java 17.
```

#### Example for "Migrate to Spring Boot 3.0":

**Display Name:**
```
Migrate to Spring Boot 3.0 from Spring Boot 2.0 through 2.7.x
```
**Description**
```
Migrate applications built with previous versions of Spring Boot to Spring Boot 3.0. This recipe will modify an
application's build files to update the dependencies, makechanges to deprecated APIs, and migrate configuration
setting that have changed across Spring Boot versions. This recipe will include the migration of both Java 17
and J2EE 9 as they are prerequisites for migration to Spring Boot 3.0. 
```

## Consequences

The use of a convention for migration recipes improves the discoverability and search experience when a user
is looking for a specific migration.
