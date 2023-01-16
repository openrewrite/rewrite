# Building & Developing OpenRewrite

We use [Gradle](https://gradle.org/) to build this project.
The gradle wrapper checked into this project defines the gradle version to use.  
When building from the command line invoke the wrapper with `./gradlew build` on unix-style terminals and `gradlew build` on windows-style terminals.

NOTE: windows-style users should ensure that they configure `core.autocrlf = false` as Rewrite requires unix-style line endings. This can be done at clone time by using `git clone -c core.autocrlf=false https://github.com/openrewrite/rewrite.git`.

## CLI Environment Configuration:

* [JDK](https://adoptium.net/) version: 17
* [Gradle](https://gradle.org/) version: Defined in wrapper

## IDE Configuration

We use [IntelliJ IDEA](https://www.jetbrains.com/idea/) to develop this project.
Other IDEs or versions of this IDE can be made to work.
These are one set of versions we know works:

* IDEA version:  2022.2.3

You must set the `-parameters` compiler flag to run Rewrite tests.
If your system does not have UTF-8 as its default character encoding (e.g., Windows) you must also add `-encoding utf8`.
Add these to the "additional command line parameters" field in IntelliJ -> Preferences -> Build, Execution, Deployment -> Compiler -> Java Compiler.

<img width="258" alt="image" src="https://user-images.githubusercontent.com/1697736/212771129-c19167ad-17eb-4d75-8b9d-439a03280a88.png">

### Optimizing your IDE only for modules you want to work on

If you are only working on a subset of the modules in this project, you can optimize your 
IDE to only load those modules. Copy `IDE.properties.tmp` to `IDE.properties` and comment out
any lines that correspond to modules you do not want to work on. This will cause Gradle to
swap those project dependencies for binary dependencies resolved from either Maven local or 
the OSS snapshots repository, and speed up your IDE.
