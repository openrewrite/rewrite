/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.groovy.Assertions.groovy;

/**
 * A test with groovy examples picked from OSS repositories.
 */
class RealWorldGroovyTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/4116")
    @Test
    void jenkinsFile() {
        rewriteRun(
          groovy(
            """
              #!groovy
              def getEndBuild() {
                  { steps , config , domain ->
                      new net.one.gtu.jenkins.helper.ConsoleLogger(steps)
                          .logInfo("Intentionally exiting the build early. Please disregard subsequent errors; they do not represent real concerns/issues");
                      throw new org.jenkinsci.plugins.workflow.steps.FlowInterruptedException(hudson.model.Result.SUCCESS);
                  }
              }
              def buildPipeline1() {
                  pipelineRunner {
                      yml = 'jenkins_old.yml'
                      endBuild = getEndBuild();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-boot/blob/v3.4.1/settings.gradle")
    @Test
    void springBootSettingsGradle() {
        rewriteRun(
          groovy(
            """
              pluginManagement {
                  evaluate(new File("${rootDir}/buildSrc/SpringRepositorySupport.groovy")).apply(this)
                  repositories {
                      mavenCentral()
                      gradlePluginPortal()
                      spring.mavenRepositories();
                  }
                  resolutionStrategy {
                      eachPlugin {
                          if (requested.id.id == "org.jetbrains.kotlin.jvm") {
                              useVersion "${kotlinVersion}"
                          }
                          if (requested.id.id == "org.jetbrains.kotlin.plugin.spring") {
                              useVersion "${kotlinVersion}"
                          }
                      }
                  }
              }

              plugins {
                  id "io.spring.develocity.conventions" version "0.0.22"
              }

              rootProject.name="spring-boot-build"

              enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

              settings.gradle.projectsLoaded {
                  develocity {
                      buildScan {
                          def toolchainVersion = settings.gradle.rootProject.findProperty('toolchainVersion')
                          if (toolchainVersion != null) {
                              value('Toolchain version', toolchainVersion)
                              tag("JDK-$toolchainVersion")
                          }
                      }
                  }
              }

              include "spring-boot-system-tests:spring-boot-image-tests"

              file("${rootDir}/spring-boot-project/spring-boot-starters").eachDirMatch(~/spring-boot-starter.*/) {
                  include "spring-boot-project:spring-boot-starters:${it.name}"
              }

              file("${rootDir}/spring-boot-tests/spring-boot-smoke-tests").eachDirMatch(~/spring-boot-smoke-test.*/) {
                  include "spring-boot-tests:spring-boot-smoke-tests:${it.name}"
              }
              """
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-boot/blob/v3.4.1/spring-boot-project/spring-boot-tools/spring-boot-cli/src/test/resources/classloader-test-app.groovy")
    @Test
    void springBootClassloaderTestApp() {
        rewriteRun(
          groovy(
            """
              import org.springframework.util.*

              @Component
              public class Test implements CommandLineRunner {

                  public void run(String... args) throws Exception {
                      println "HasClasses-" + ClassUtils.isPresent("missing", null) + "-" +
                              ClassUtils.isPresent("org.springframework.boot.SpringApplication", null) + "-" +
                              ClassUtils.isPresent(args[0], null)
                  }

              }
              """
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-ldap/blob/v3.4.1/buildSrc/src/main/groovy/io/spring/gradle/convention/JavadocOptionsPlugin.groovy")
    @Test
    void springLdapJavadocOptionsPlugin() {
        rewriteRun(
          groovy(
            """
              import org.gradle.api.Plugin
              import org.gradle.api.Project
              import org.gradle.api.tasks.javadoc.Javadoc

              public class JavadocOptionsPlugin implements Plugin<Project> {

              	@Override
              	public void apply(Project project) {
              		project.getTasks().withType(Javadoc).all { t->
              			t.options.addStringOption('Xdoclint:none', '-quiet')
              		}
              	}
              }
              """
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-ldap/blob/v3.4.1/buildSrc/src/test/resources/samples/integrationtest/withgroovy/src/integration-test/groovy/sample/TheTest.groovy")
    @Test
    void springLdapTheTest() {
        rewriteRun(
          groovy(
            """
              import org.springframework.core.Ordered
              import spock.lang.Specification

              class TheTest extends Specification {
                  def "has Ordered"() {
                      expect: 'Loads Ordered fine'
                      Ordered ordered = new Ordered() {
                          @Override
                          int getOrder() {
                              return 0
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-session-data-geode/blob/v3.4.1/buildSrc/src/main/groovy/io/spring/gradle/convention/SchemaZipPlugin.groovy")
    @Test
    @Disabled
    void springTestDataGeodeSchemaZipPlugin() {
        rewriteRun(
          groovy(
            """
              import org.gradle.api.Plugin
              import org.gradle.api.Project
              import org.gradle.api.file.DuplicatesStrategy
              import org.gradle.api.plugins.JavaPlugin
              import org.gradle.api.tasks.bundling.Zip

              /**
               * Zips all Spring XML schemas (XSD) files.
               *
               * @author Rob Winch
               * @author John Blum
               * @see org.gradle.api.Plugin
               * @see org.gradle.api.Project
               */
              class SchemaZipPlugin implements Plugin<Project> {

                  @Override
                  void apply(Project project) {

                      Zip schemaZip = project.tasks.create('schemaZip', Zip)

                      schemaZip.archiveBaseName = project.rootProject.name
                      schemaZip.archiveClassifier = 'schema'
                      schemaZip.description = "Builds -${schemaZip.archiveClassifier} archive containing all XSDs" +
                              " for deployment to static.springframework.org/schema."
                      schemaZip.group = 'Distribution'

                      project.rootProject.subprojects.each { module ->

                          module.getPlugins().withType(JavaPlugin.class).all {

                              Properties schemas = new Properties();

                              module.sourceSets.main.resources
                                      .find { it.path.endsWith('META-INF/spring.schemas') }
                                      ?.withInputStream { schemas.load(it) }

                              for (def key : schemas.keySet()) {

                                  def zipEntryName = key.replaceAll(/http.*schema.(.*).spring-.*/, '$1')

                                  assert zipEntryName != key

                                  File xsdFile = module.sourceSets.main.resources.find {
                                      it.path.endsWith(schemas.get(key))
                                  }

                                  assert xsdFile != null

                                  schemaZip.into(zipEntryName) {
                                      duplicatesStrategy DuplicatesStrategy.EXCLUDE
                                      from xsdFile.path
                                  }
                              }
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-webflow/blob/v3.4.1/gradle/docs.gradle")
    @Test
    @Disabled
    void springWebflowGradleDocs() {
        rewriteRun(
          groovy(
            """
              apply plugin: "maven-publish"

              configurations {
                  asciidoctorExtensions
              }

              dependencies {
                  asciidoctorExtensions "io.spring.asciidoctor.backends:spring-asciidoctor-backends:0.0.7"
              }

              asciidoctorPdf {
                  baseDirFollowsSourceFile()
                  asciidoctorj {
                      sources {
                          include 'index.adoc'
                      }
                      options doctype: 'book'
                      attributes 'icons': 'font',
                              'sectanchors': '',
                              'sectnums': '',
                              'toc': '',
                              'source-highlighter' : 'coderay',
                              revnumber: project.version,
                              'project-version': project.version
                  }
              }

              asciidoctor {
                  baseDirFollowsSourceFile()
                  configurations "asciidoctorExtensions"
                  outputOptions {
                      backends "spring-html"
                  }
                  sources {
                      include 'index.adoc'
                  }
                  options doctype: 'book'

                  attributes 'docinfo': 'shared',
                          stylesdir: 'css/',
                          stylesheet: 'spring.css',
                          'linkcss': true,
                          'icons': 'font',
                          'sectanchors': '',
                          'source-highlighter': 'highlight.js',
                          'highlightjsdir': 'js/highlight',
                          'highlightjs-theme': 'github',
                          'idprefix': '',
                          'idseparator': '-',
                          'spring-version': project.version,
                          'allow-uri-read': '',
                          'toc': 'left',
                          'toclevels': '4',
                          revnumber: project.version,
                          'project-version': project.version
              }

              task api(type: Javadoc) {
                  group = "Documentation"
                  description = "Generates aggregated Javadoc API documentation."
                  title = "${rootProject.description} ${version} API"
                  options.memberLevel = JavadocMemberLevel.PROTECTED
                  options.author = true
                  options.header = rootProject.description
                  options.overview = "src/api/overview.html"
                  source subprojects.collect { project ->
                      project.sourceSets.main.allJava
                  }
                  destinationDir = new File(buildDir, "api")
                  classpath = files(subprojects.collect { project ->
                      project.sourceSets.main.compileClasspath
                  })
                  maxMemory = "1024m"
              }

              task docsZip(type: Zip) {
                  group = "Distribution"
                  archiveBaseName = "spring-webflow"
                  archiveClassifier = "docs"
                  description = "Builds -${archiveClassifier.get()} archive containing api and reference " +
                          "for deployment at static.springframework.org/spring-webflow/docs."

                  from (api) {
                      into "api"
                  }
                  from (asciidoctor) {
                      into "reference"
                  }
                  from (asciidoctorPdf) {
                      into "reference"
                  }
              }

              task schemaZip(type: Zip) {
                  group = "Distribution"
                  archiveBaseName = "spring-webflow"
                  archiveClassifier = "schema"
                  description = "Builds -${archiveClassifier.get()} archive containing all " +
                          "XSDs for deployment at static.springframework.org/schema."

                  duplicatesStrategy = DuplicatesStrategy.EXCLUDE

                  subprojects.each { subproject ->
                      Properties schemas = new Properties()

                      subproject.sourceSets.main.resources.find {
                          it.path.endsWith("META-INF/spring.schemas")
                      }?.withInputStream { schemas.load(it) }

                      for (def key : schemas.keySet()) {
                          def shortName = key.replaceAll(/http.*schema.(.*).spring-.*/, '$1')
                          assert shortName != key
                          File xsdFile = subproject.sourceSets.main.allSource.find {
                              it.path.endsWith(schemas.get(key))
                          } as File
                          assert xsdFile != null
                          into (shortName) {
                              from xsdFile.path
                          }
                      }
                  }

                  project(":spring-webflow").sourceSets.main.resources.matching {
                      include '**/engine/model/builder/xml/*.xsd'
                  }.each { File file ->
                      into ('webflow') {
                          from file.path
                      }
                  }
              }

              task distZip(type: Zip, dependsOn: [docsZip, schemaZip]) {
                  group = "Distribution"
                  archiveBaseName = "spring-webflow"
                  archiveClassifier = "dist"
                  description = "Builds -${archiveClassifier.get()} archive, containing all jars and docs, " +
                          "suitable for community download page."

                  def baseDir = "${archiveBaseName.get()}-${project.version}"

                  from("src/dist") {
                      include "notice.txt"
                      into "${baseDir}"
                      expand(copyright: new Date().format("yyyy"), version: project.version)
                  }
                  from("src/dist") {
                      include "readme.txt"
                      include "license.txt"
                      into "${baseDir}"
                      expand(version: project.version)
                  }
                  from(zipTree(docsZip.archiveFile)) {
                      into "${baseDir}/docs"
                  }
                  from(zipTree(schemaZip.archiveFile)) {
                      into "${baseDir}/schema"
                  }

                  subprojects.each { subproject ->
                      into ("${baseDir}/libs") {
                          from subproject.jar
                          if (subproject.tasks.findByPath("sourcesJar")) {
                              from subproject.sourcesJar
                          }
                          if (subproject.tasks.findByPath("javadocJar")) {
                              from subproject.javadocJar
                          }
                      }
                  }
              }

              publishing {
                  publications {
                      mavenJava(MavenPublication) {
                          artifact docsZip
                          artifact schemaZip
                          artifact distZip
                      }
                  }
              }
              """
          )
        );
    }
}
