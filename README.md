![Logo](https://github.com/openrewrite/rewrite/raw/main/doc/logo-oss.png)
# Semantic code search and transformation

[![ci](https://github.com/openrewrite/rewrite/actions/workflows/ci.yml/badge.svg)](https://github.com/openrewrite/rewrite/actions/workflows/ci.yml)
[![Apache 2.0](https://img.shields.io/github/license/openrewrite/rewrite.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.openrewrite/rewrite-java.svg)](https://mvnrepository.com/artifact/org.openrewrite/rewrite-java)
[![Revved up by Gradle Enterprise](https://img.shields.io/badge/Revved%20up%20by-Gradle%20Enterprise-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.openrewrite.org/scans)

OpenRewrite project is a mass refactoring ecosystem for Java and other source code, designed to eliminate technical debt across an engineering organization.
This project delivers scalable automated code maintenance, best practices, vulnerability patching, API migrations, dependency management, and more.

Start with our [quickstart guide](https://docs.openrewrite.org/getting-started/getting-started) and let OpenRewrite start handling the boring parts of software development for you. Full documentation available at [docs.openrewrite.org](https://docs.openrewrite.org/).

Feel free to join us on [Slack](https://join.slack.com/t/rewriteoss/shared_invite/zt-nj42n3ea-b~62rIHzb3Vo0E1APKCXEA) or [Discord](https://discord.gg/xk3ZKrhWAb)! We're happy to answer your questions directly.

Follow us on [Twitter](https://twitter.com/moderneinc) and [LinkedIn](https://www.linkedin.com/company/moderneinc).

## Building this project

OpenRewrite is built with [Gradle](https://gradle.org/). It is not typically necessary to manually install gradle, as invoking the `./gradlew` (Linux and Mac) or `gradlew.bat` (Windows) shell scripts will download the appropriate version of Gradle to your user directory.

OpenRewrite requires several JDK versions to be installed on your system. If you are able to access [Adoptium](https://adoptium.net/) then Gradle will automatically download and install any needed JDKs which you may be missing. If your network configuration or security policies do not permit this, then you must manually install JDK versions 8, 11, and 17.

To compile and run tests invoke `./gradlew build`. To publish a snapshot build to your maven local repository, run `./gradlew publishToMavenLocal`. 

### Building within Secure/Isolated environments

OpenRewrite typically accesses the Maven Central artifact repository to download necessary dependencies.
If organizational security policy or network configuration forbids this, then you can use a Gradle [init script](https://docs.gradle.org/current/userguide/init_scripts.html) to forcibly reconfigure the OpenRewrite build to use a different repository.

Copy this script to a file named `init.gradle.kts` into the <user home>/.gradle directory.
Modify the `enterpriseRepository` value as appropriate for your situation.
```kotlin
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository

// Replace with your organization's artifact repository which mirrors the contents of Maven Central
val mavenCentralMirror = "https://repo.maven.apache.org/maven2/"
// Replace with your organization's artifact repository which mirrors the contents of the Gradle Plugin portal
val gradlePluginPortalMirror = "https://plugins.gradle.org/m2"
// Replace with your organization's artifact repository which mirrors the contents of Gradle's 
// This one is required only for building the rewrite-gradle project
val gradleLibsRelease = "https://repo.gradle.org/gradle/libs-releases-local/"

val allowedRepos = listOf(mavenCentralMirror, gradlePluginPortalMirror, gradleLibsRelease)

// Fill out as appropriate if your repository requires authentication
// Consider using system properties to fill these in for better security
val user: String? = null; 
val pass: String? = null;

fun repoIsAcceptable(repo: ArtifactRepository): Boolean = 
    repo is DefaultMavenLocalArtifactRepository || 
    (repo is MavenArtifactRepository && allowedRepos.find { it == (repo as MavenArtifactRepository).getUrl().toString() } != null)

beforeSettings {
    pluginManagement.repositories {
        all { 
            if (!repoIsAcceptable(this)) {
                remove(this)
            }
        }
        mavenLocal()
        allowedRepos.forEach { enterpriseRepository ->
            maven { 
                url = uri(enterpriseRepository)
                if(user != null && pass != null)  {
                    authentication {
                        create<BasicAuthentication>("basic")
                    }
                    
                    credentials {
                        username = user
                        password = pass
                    }
                }
            }
        }
    }
}
allprojects {
    repositories {
        all { 
            if (!repoIsAcceptable(this)) {
                remove(this)
            }
        }
        mavenLocal()
        allowedRepos.forEach { enterpriseRepository ->
            maven { 
                url = uri(enterpriseRepository)
                if(user != null && pass != null)  {
                    authentication {
                        create<BasicAuthentication>("basic")
                    }
                    
                    credentials {
                        username = user
                        password = pass
                    }
                }
            }
        }
    }
}

```

With this file placed, all of your gradle builds will prefer to use your corporate repository instead of whatever repositories they would normally be configured with.
  
## Refactoring at Scale

[![Moderne](./doc/video_preview.png)](https://www.youtube.com/watch?v=ndU2GKXQAH0)

Try it yourself at https://app.moderne.io, now in open beta.

This project is maintained by [Moderne](https://moderne.io/).
Visit us at https://moderne.io to learn how to remove the tedium, technical debt, and inefficiency from your organization's software development.
