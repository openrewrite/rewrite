/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.gradle.marker;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.RecipeSerializer;
import org.openrewrite.gradle.attributes.Category;
import org.openrewrite.gradle.attributes.ProjectAttribute;
import org.openrewrite.gradle.toolingapi.OpenRewriteModel;
import org.openrewrite.gradle.toolingapi.OpenRewriteModelBuilder;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.tree.Scm;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * When running these in an IDE you should enable delegation to Gradle for best results.
 * Also uncomment embedded(true) in OpenRewriteModelBuilder and you should be able to hit breakpoints.
 */
class GradleProjectTest {
    public static GradleVersion gradleVersion = System.getProperty("org.openrewrite.test.gradleVersion") == null ?
      GradleVersion.current() :
      GradleVersion.version(System.getProperty("org.openrewrite.test.gradleVersion"));

    public static boolean gradleOlderThan8() {
        return gradleVersion.compareTo(GradleVersion.version("8.0")) < 0;
    }

    static byte[] readAllBytes(InputStream is) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        int nRead;
        byte[] data = new byte[1024];
        while ((nRead = is.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    @Nested
    class gradle4Compatibility {
        @TempDir
        static Path dir;

        static GradleProject gradleProject;

        //language=groovy
        static String buildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation 'org.openrewrite:rewrite-java:8.56.0'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          """;


        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream(buildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = model.getGradleProject();
        }

        @Test
        void requestedCorrespondsDirectlyToResolved() {
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getRequested())
              .hasSize(1);
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getDirectResolved())
              .hasSize(1);
        }

        @Test
        void serializable() throws Exception {
            ObjectMapper m = new RecipeSerializer().getMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.writeValue(baos, gradleProject);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            GradleProject roundTripped = m.readValue(bais, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }

    }


    @Nested
    @DisabledIf("org.openrewrite.gradle.marker.GradleProjectTest#gradleOlderThan8")
    class singleProject {
        @TempDir
        static Path dir;

        static GradleProject gradleProject;

        //language=groovy
        static String buildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          configurations.all {
              resolutionStrategy.eachDependency { details ->
                  if (details.requested.group == 'com.fasterxml.jackson.core' && details.requested.name == 'jackson-databind') {
                      details.useVersion('2.18.3')
                      details.because('CVE-2025-BAD')
                  }
              }
          }

          dependencies{
              constraints {
                  implementation('com.fasterxml.jackson.core:jackson-core:2.18.4') {
                      because 'CVE-2024-BAD'
                  }
              }

              implementation platform('org.openrewrite:rewrite-bom:8.56.0')
              implementation 'org.openrewrite:rewrite-java'
              implementation 'org.openrewrite.recipe:rewrite-java-dependencies:1.35.+'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream(buildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = model.getGradleProject();
        }

        @Test
        void recordsBuildFilePathRelativeToProjectDirectory() {
            assertThat(gradleProject.getBuildFilePath()).isEqualTo("build.gradle");
        }

        @Test
        void resolutionStrategy() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getAllConstraints())
              .anyMatch(it -> "jackson-databind".equals(it.getArtifactId()) && "2.18.3".equals(it.getStrictVersion()));
            assertThat(runtimeClasspath.getResolved())
              .anyMatch(it -> "jackson-databind".equals(it.getArtifactId()) && "2.18.3".equals(it.getVersion()));
        }

        @Test
        void sameConfigurationIsReferentiallySame() {
            GradleDependencyConfiguration implementation = requireNonNull(gradleProject.getConfiguration("implementation"));
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getExtendsFrom()).anyMatch(it -> it == implementation);
        }

        @Test
        void plusDependency() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getRequested())
              .anyMatch(it -> "rewrite-java-dependencies".equals(it.getArtifactId()) && "1.35.+".equals(it.getVersion()));
            assertThat(runtimeClasspath.getDirectResolved())
              .anyMatch(it -> "rewrite-java-dependencies".equals(it.getArtifactId()) && "1.35.2".equals(it.getVersion()));
        }

        @Test
        void transitiveDependencies() {
            Map<Integer, Integer> dependenciesByDepth = new HashMap<>();
            for (GradleDependencyConfiguration configuration : gradleProject.getConfigurations()) {
                for (ResolvedDependency resolvedDependency : configuration.getResolved()) {
                    dependenciesByDepth.merge(resolvedDependency.getDepth(), 1, Integer::sum);
                }
            }
            assertThat(dependenciesByDepth).containsKeys(0, 1);
        }

        @Test
        void requestedCorrespondsDirectlyToResolved() {
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getRequested())
              .hasSize(3);
            assertThat(requireNonNull(gradleProject.getConfiguration("compileClasspath")).getDirectResolved())
              .hasSize(3);
        }

        @Test
        void serializable() throws Exception {
            ObjectMapper m = new RecipeSerializer().getMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.writeValue(baos, gradleProject);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            GradleProject roundTripped = m.readValue(bais, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }

        /**
         * {@link GroupArtifactVersion} and {@link org.openrewrite.maven.tree.ResolvedGroupArtifactVersion} carry
         * {@code @JsonIdentityInfo(generator = IntSequenceGenerator.class, property = "@ref")} so that an instance
         * shared by reference across this {@code singleProject} fixture's {@code extendsFrom} chain is written once and
         * referenced by a {@code @ref} integer thereafter — a real size win on large reactors. We keep that
         * optimization, but a newer consumer must still be able to read representations produced by an
         * <em>older</em> writer that predates the annotation, i.e. payloads with no {@code @ref} property and full
         * objects at every position.
         * <p>
         * The older writer is simulated here with a mix-in that turns object-id handling off, producing the
         * pre-annotation "full objects everywhere" wire format. The newer (annotated) consumer must deserialize it
         * cleanly: when no {@code @ref} property is present Jackson simply reads each object normally and registers no
         * id, and there are no back-references to resolve. This locks in that upgrading a consumer does not break
         * reading already-persisted, pre-annotation artifacts.
         */
        @Test
        void newerConsumerReadsOlderRepresentation() throws Exception {
            // Older writer: predates @JsonIdentityInfo, so it emits full objects with no "@ref" property.
            ObjectMapper olderWriter = new RecipeSerializer().getMapper();
            olderWriter.addMixIn(GroupArtifactVersion.class, NoObjectIdMixin.class);
            olderWriter.addMixIn(ResolvedGroupArtifactVersion.class, NoObjectIdMixin.class);

            // Newer reader: the current build, with the annotation present on the real classes.
            ObjectMapper newerReader = new RecipeSerializer().getMapper();

            byte[] olderRepresentation = olderWriter.writeValueAsBytes(gradleProject);
            GradleProject roundTripped = newerReader.readValue(olderRepresentation, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }

        /**
         * Mix-in modeling a class from before {@code @JsonIdentityInfo} was added to it. Overriding the generator with
         * {@link ObjectIdGenerators.None} turns object-id handling off, producing the older "full objects" wire format.
         */
        @JsonIdentityInfo(generator = ObjectIdGenerators.None.class)
        private interface NoObjectIdMixin {
        }

        @Test
        void transitiveDependencyConstraint() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));

            assertThat(runtimeClasspath)
              .extracting(GradleDependencyConfiguration::getAllConstraints)
              .asInstanceOf(InstanceOfAssertFactories.list(GradleDependencyConstraint.class))
              .as("runtime classpath should have inherited the implementation constraint on jackson-core:2.18.4")
              .anyMatch(constraint -> "com.fasterxml.jackson.core".equals(constraint.getGroupId()) && "jackson-core".equals(constraint.getArtifactId()) && "2.18.4".equals(constraint.getRequiredVersion()));

            assertThat(runtimeClasspath)
              .extracting(GradleDependencyConfiguration::getResolved)
              .asInstanceOf(InstanceOfAssertFactories.list(ResolvedDependency.class))
              .as("Constraint should have set the version of transitive jackson-core to 2.18.4")
              .anyMatch(dep -> "com.fasterxml.jackson.core".equals(dep.getGroupId()) && "jackson-core".equals(dep.getArtifactId()) && "2.18.4".equals(dep.getVersion()));
        }

        @Test
        void bom() {
            GradleDependencyConfiguration runtimeClasspath = requireNonNull(gradleProject.getConfiguration("runtimeClasspath"));
            assertThat(runtimeClasspath.getRequested())
              .as("rewrite-bom should be marked as a platform() dependency")
              .anyMatch(it -> it.findAttribute(Category.class)
                                .filter(Category.REGULAR_PLATFORM::equals)
                                .isPresent() &&
                              "rewrite-bom".equals(it.getArtifactId()) &&
                              "pom".equals(it.getType()));
            assertThat(runtimeClasspath.getDirectResolved())
              .filteredOn(it -> "rewrite-bom".equals(it.getArtifactId()))
              .singleElement()
              .extracting(ResolvedDependency::getType)
              .as("platform() dependencies should have type \"pom\" to help indicate that they aren't jars going onto a classpath")
              .isEqualTo("pom");
        }
    }

    /**
     * Runs on every Gradle version this module is tested with, the oldest included.
     */
    @Nested
    class sourceRepositories {
        @TempDir
        static Path dir;

        static GradleProject gradleProject;

        //language=groovy
        static String buildGradle = """
          buildscript {
              repositories {
                  mavenCentral()
              }
              dependencies {
                  classpath 'junit:junit:4.13.2'
              }
          }

          apply plugin: 'java'

          repositories {
              mavenCentral()
              flatDir {
                  dirs 'libs'
              }
              ivy {
                  url 'ivy-repo'
              }
              maven {
                  url 'maven-repo'
              }
          }

          dependencies {
              implementation 'junit:junit:4.13.2'
              implementation 'com.google.guava:guava:29.0-jre'
              implementation 'aopalliance:aopalliance:1.0'
              implementation ':no-pom:1.0'
              implementation 'org.example:ivy-lib:1.0'
              implementation 'org.example:own-url:1.0'
              implementation 'org.example:own-address:1.0'
              implementation project(':lib')
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          include("lib")
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            Files.write(dir.resolve("build.gradle"), buildGradle.getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("settings.gradle"), settingsGradle.getBytes(StandardCharsets.UTF_8));
            Files.createDirectory(dir.resolve("lib"));
            Files.write(dir.resolve("lib/build.gradle"), "apply plugin: 'java'\n".getBytes(StandardCharsets.UTF_8));
            Files.createDirectory(dir.resolve("libs"));
            emptyJar(dir.resolve("libs/no-pom-1.0.jar"));
            Path ivyLib = Files.createDirectories(dir.resolve("ivy-repo/org.example/ivy-lib/1.0"));
            emptyJar(ivyLib.resolve("ivy-lib-1.0.jar"));
            Files.write(ivyLib.resolve("ivy-1.0.xml"), """
              <ivy-module version="2.0">
                <info organisation="org.example" module="ivy-lib" revision="1.0"/>
                <publications>
                  <artifact name="ivy-lib" type="jar" ext="jar"/>
                </publications>
              </ivy-module>
              """.getBytes(StandardCharsets.UTF_8));

            mavenModule("scm-parent", "pom", """
              <scm>
                <connection>scm:git:https://git.example.org/parent.git</connection>
                <tag>HEAD</tag>
              </scm>
              """);
            mavenModule("own-url", "jar", """
              <parent>
                <groupId>org.example</groupId>
                <artifactId>scm-parent</artifactId>
                <version>1.0</version>
              </parent>
              <scm>
                <url>https://git.example.org/own-url</url>
              </scm>
              """);
            mavenModule("own-address", "jar", """
              <parent>
                <groupId>org.example</groupId>
                <artifactId>scm-parent</artifactId>
                <version>1.0</version>
              </parent>
              <scm>
                <connection>scm:git:https://git.example.org/own-address.git</connection>
                <url>https://git.example.org/own-address</url>
              </scm>
              """);

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = model.getGradleProject();
        }

        private static void mavenModule(String artifactId, String packaging, String body) throws IOException {
            Path module = Files.createDirectories(dir.resolve("maven-repo/org/example/" + artifactId + "/1.0"));
            Files.write(module.resolve(artifactId + "-1.0.pom"), ("""
              <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>org.example</groupId>
                <artifactId>%s</artifactId>
                <version>1.0</version>
                <packaging>%s</packaging>
                %s
              </project>
              """.formatted(artifactId, packaging, body)).getBytes(StandardCharsets.UTF_8));
            if ("jar".equals(packaging)) {
                emptyJar(module.resolve(artifactId + "-1.0.jar"));
            }
        }

        @Test
        void declaredScmIsInheritedFromNoPom() {
            assertThat(compileClasspath("junit").getScm())
              .extracting(Scm::getInheritedFrom)
              .isNull();
        }

        @Test
        void inheritedScmNamesTheNearestParentThatContributed() {
            assertThat(compileClasspath("guava").getScm())
              .extracting(Scm::getInheritedFrom)
              .isEqualTo(new GroupArtifactVersion("com.google.guava", "guava-parent", "29.0-jre"));

            ResolvedDependency j2objc = compileClasspath("guava").findDependency("com.google.j2objc", "j2objc-annotations");
            assertThat(requireNonNull(j2objc).getScm())
              .as("its own POM declares no <scm>, so this is the repository of a parent of another organization")
              .isEqualTo(new Scm(
                "scm:svn:http://svn.sonatype.org/spice/tags/oss-parent-7",
                "scm:svn:https://svn.sonatype.org/spice/tags/oss-parent-7",
                "http://svn.sonatype.org/spice/tags/oss-parent-7",
                null,
                new GroupArtifactVersion("org.sonatype.oss", "oss-parent", "7")));
        }

        @Test
        void ownUrlWithAnInheritedConnectionIsInherited() {
            assertThat(compileClasspath("own-url").getScm()).isEqualTo(new Scm(
              "scm:git:https://git.example.org/parent.git",
              null,
              "https://git.example.org/own-url",
              "HEAD",
              new GroupArtifactVersion("org.example", "scm-parent", "1.0")));
        }

        @Test
        void inheritingOnlyTheTagIsNotInheritingTheRepository() {
            Scm scm = compileClasspath("own-address").getScm();

            assertThat(scm).isEqualTo(new Scm(
              "scm:git:https://git.example.org/own-address.git",
              null,
              "https://git.example.org/own-address",
              "HEAD"));
            assertThat(scm.getInheritedFrom()).isNull();
        }

        @Test
        void scmDeclaredByTheDependencyItself() {
            assertThat(compileClasspath("junit").getScm()).isEqualTo(new Scm(
              "scm:git:git://github.com/junit-team/junit4.git",
              "scm:git:git@github.com:junit-team/junit4.git",
              "https://github.com/junit-team/junit4",
              "r4.13.2"));
        }

        @Test
        void scmInheritedFromAParentWithoutTheArtifactIdAppended() {
            assertThat(compileClasspath("guava").getScm()).isEqualTo(new Scm(
              "scm:git:https://github.com/google/guava.git",
              "scm:git:git@github.com:google/guava.git",
              "https://github.com/google/guava",
              null,
              new GroupArtifactVersion("com.google.guava", "guava-parent", "29.0-jre")));
        }

        @Test
        void transitiveDependencyHasScm() {
            ResolvedDependency failureaccess = compileClasspath("guava").findDependency("com.google.guava", "failureaccess");

            assertThat(requireNonNull(failureaccess).getScm())
              .extracting(Scm::getUrl)
              .isEqualTo("https://github.com/google/guava");
        }

        @Test
        void noScmWhereThePomDeclaresNone() {
            assertThat(compileClasspath("aopalliance").getScm()).isNull();
        }

        @Test
        void noScmWithoutAPom() {
            GradleDependencyConfiguration compileClasspath = requireNonNull(gradleProject.getConfiguration("compileClasspath"));

            assertThat(compileClasspath.getExceptionType())
              .as("%s", compileClasspath.getExceptionMessage())
              .isNull();
            assertThat(compileClasspath("no-pom").getScm())
              .as("from a flat directory repository")
              .isNull();
            assertThat(compileClasspath("ivy-lib").getScm())
              .as("from an Ivy repository")
              .isNull();
            assertThat(compileClasspath("lib").getScm())
              .as("a project of this build")
              .isNull();
        }

        @Test
        void buildscriptDependencyHasScm() {
            assertThat(requireNonNull(gradleProject.getBuildscript().getConfiguration("classpath")).getDirectResolved())
              .filteredOn(dependency -> "junit".equals(dependency.getArtifactId()))
              .singleElement()
              .extracting(ResolvedDependency::getScm)
              .extracting(Scm::getUrl)
              .isEqualTo("https://github.com/junit-team/junit4");
        }

        @Test
        void serializable() throws Exception {
            ObjectMapper m = new RecipeSerializer().getMapper();

            GradleProject roundTripped = m.readValue(m.writeValueAsBytes(gradleProject), GradleProject.class);

            assertThat(roundTripped).isEqualTo(gradleProject);
            assertThat(requireNonNull(roundTripped.getConfiguration("compileClasspath")).getDirectResolved())
              .filteredOn(dependency -> "guava".equals(dependency.getArtifactId()))
              .singleElement()
              .extracting(ResolvedDependency::getScm)
              .isEqualTo(compileClasspath("guava").getScm())
              .extracting(Scm::getInheritedFrom)
              .isEqualTo(new GroupArtifactVersion("com.google.guava", "guava-parent", "29.0-jre"));
        }

        private static void emptyJar(Path jar) throws IOException {
            try (ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(jar))) {
                out.putNextEntry(new ZipEntry("META-INF/"));
                out.closeEntry();
            }
        }

        private ResolvedDependency compileClasspath(String artifactId) {
            return requireNonNull(gradleProject.getConfiguration("compileClasspath")).getDirectResolved().stream()
              .filter(dependency -> artifactId.equals(dependency.getArtifactId()))
              .findFirst()
              .orElseThrow(() -> new AssertionError("No " + artifactId + " on the compile classpath"));
        }
    }

    @Nested
    class multiProject {
        @TempDir
        static Path dir;

        static GradleProject rootGradleProject;
        static GradleProject aGradleProject;
        static GradleProject bGradleProject;

        //language=groovy
        static String aBuildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation 'org.openrewrite:rewrite-java:8.57.0'
              testImplementation("org.projectlombok:lombok:latest.release")
          }
          """;

        //language=groovy
        static String bBuildGradle = """
          plugins{
              id 'java'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation project(":a")
              implementation 'org.openrewrite:rewrite-java:8.56.0'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          include("a")
          include("b")
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream("\n".getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            Path aDir = dir.resolve("a");
            Files.createDirectory(aDir);
            try (InputStream is = new ByteArrayInputStream(aBuildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(aDir.resolve("build.gradle"), readAllBytes(is));
            }

            Path bDir = dir.resolve("b");
            Files.createDirectory(bDir);
            try (InputStream is = new ByteArrayInputStream(bBuildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(bDir.resolve("build.gradle"), readAllBytes(is));
            }


            OpenRewriteModel rootModel = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            rootGradleProject = rootModel.getGradleProject();
            OpenRewriteModel aModel = OpenRewriteModelBuilder.forProjectDirectory(aDir.toFile(), aDir.resolve("build.gradle").toFile());
            aGradleProject = aModel.getGradleProject();
            OpenRewriteModel bModel = OpenRewriteModelBuilder.forProjectDirectory(bDir.toFile(), bDir.resolve("build.gradle").toFile());
            bGradleProject = bModel.getGradleProject();
        }

        @Test
        void projectDependencyHasAttribute() {
            assertThat(requireNonNull(bGradleProject.getConfiguration("compileClasspath")).getRequested())
              .anyMatch(dep -> dep.findAttribute(ProjectAttribute.class).isPresent() && "a".equals(dep.getGav().getArtifactId()));
        }
    }

    /**
     * Configurations are read in name order, so c's annotationProcessor reaches a through b, with no attribute
     * to say a is a project, before c's compileClasspath declares a.
     */
    @Nested
    class projectReachedTransitivelyFirst {
        @TempDir
        static Path dir;

        static GradleProject cGradleProject;

        //language=groovy
        static String bBuildGradle = """
          plugins{
              id 'java'
          }

          dependencies{
              implementation project(":a")
          }
          """;

        //language=groovy
        static String cBuildGradle = """
          plugins{
              id 'java'
          }

          dependencies{
              annotationProcessor project(":b")
              implementation project(":a")
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "reached-transitively-first"
          include("a")
          include("b")
          include("c")
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            Files.write(dir.resolve("build.gradle"), "\n".getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("settings.gradle"), settingsGradle.getBytes(StandardCharsets.UTF_8));
            Files.createDirectory(dir.resolve("a"));
            Files.write(dir.resolve("a/build.gradle"), "plugins { id 'java' }\n".getBytes(StandardCharsets.UTF_8));
            Files.createDirectory(dir.resolve("b"));
            Files.write(dir.resolve("b/build.gradle"), bBuildGradle.getBytes(StandardCharsets.UTF_8));
            Path cDir = dir.resolve("c");
            Files.createDirectory(cDir);
            Files.write(cDir.resolve("build.gradle"), cBuildGradle.getBytes(StandardCharsets.UTF_8));

            cGradleProject = OpenRewriteModelBuilder.forProjectDirectory(cDir.toFile(), cDir.resolve("build.gradle").toFile())
              .getGradleProject();
        }

        @Test
        void projectDependencyKeepsItsAttribute() {
            assertThat(requireNonNull(cGradleProject.getConfiguration("compileClasspath")).getRequested())
              .anyMatch(dep -> dep.findAttribute(ProjectAttribute.class).isPresent() && "a".equals(dep.getGav().getArtifactId()));
        }
    }

    @Nested
    @DisabledIf("org.openrewrite.gradle.marker.GradleProjectTest#gradleOlderThan8")
    class SpringDependencyManagement {
        @TempDir
        static Path dir;

        static GradleProject gradleProject;

        //language=groovy
        static String buildGradle = """
          plugins{
              id 'java'
              id 'org.springframework.boot' version '3.3.5'
              id 'io.spring.dependency-management' version '1.1.7'
          }

          repositories{
              mavenCentral()
          }

          dependencies{
              implementation 'org.yaml:snakeyaml'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            try (InputStream is = new ByteArrayInputStream(buildGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("build.gradle"), readAllBytes(is));
            }

            try (InputStream is = new ByteArrayInputStream(settingsGradle.getBytes(StandardCharsets.UTF_8))) {
                Files.write(dir.resolve("settings.gradle"), readAllBytes(is));
            }

            OpenRewriteModel model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), dir.resolve("build.gradle").toFile());
            gradleProject = model.getGradleProject();
        }

        @Test
        void capturesBomImportedByBootPlugin() {
            SpringDependencyManagementPlugin dependencyManagement = requireNonNull(gradleProject.getSpringDependencyManagementPlugin());
            assertThat(dependencyManagement.getImportedBoms())
              .anyMatch(bom -> "org.springframework.boot".equals(bom.getGroupId()) &&
                               "spring-boot-dependencies".equals(bom.getArtifactId()) &&
                               "3.3.5".equals(bom.getVersion()));
            assertThat(dependencyManagement.getImportedProperties())
              .containsKey("snakeyaml.version");
            assertThat(dependencyManagement.getManagedVersions())
              .containsEntry("org.yaml:snakeyaml", dependencyManagement.getImportedProperties().get("snakeyaml.version"));
        }

        @Test
        void serializable() throws Exception {
            ObjectMapper m = new RecipeSerializer().getMapper();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            m.writeValue(baos, gradleProject);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            GradleProject roundTripped = m.readValue(bais, GradleProject.class);
            assertThat(roundTripped).isEqualTo(gradleProject);
        }
    }

    @Nested
    class RenamedBuildFile {
        @TempDir
        static Path dir;

        static OpenRewriteModel model;

        //language=groovy
        static String buildGradle = """
          plugins{
              id 'java'
          }
          """;

        //language=groovy
        static String settingsGradle = """
          rootProject.name = "sample"
          rootProject.buildFileName = 'root.gradle'
          include("api")
          project(':api').buildFileName = 'api.gradle'
          """;

        @BeforeAll
        static void gradleProject() throws IOException {
            Files.write(dir.resolve("root.gradle"), buildGradle.getBytes(StandardCharsets.UTF_8));
            Files.write(dir.resolve("settings.gradle"), settingsGradle.getBytes(StandardCharsets.UTF_8));
            Path apiDir = Files.createDirectory(dir.resolve("api"));
            Files.write(apiDir.resolve("api.gradle"), buildGradle.getBytes(StandardCharsets.UTF_8));

            model = OpenRewriteModelBuilder.forProjectDirectory(dir.toFile(), null);
        }

        @Test
        void recordsRenamedRootBuildFile() {
            assertThat(model.getGradleProject().getBuildFilePath()).isEqualTo("root.gradle");
        }

        @Test
        void recordsRenamedSubprojectBuildFileRelativeToItsOwnDirectory() {
            assertThat(requireNonNull(model.getGradleProjectsByPath().get(":api")).getBuildFilePath())
              .isEqualTo("api.gradle");
        }
    }
}
