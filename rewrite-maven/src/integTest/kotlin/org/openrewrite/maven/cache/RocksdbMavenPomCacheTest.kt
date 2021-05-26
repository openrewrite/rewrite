/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.cache

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.maven.MavenParser
import java.io.File
import java.nio.file.Path

class RocksdbMavenPomCacheTest {

    companion object {
        private val executionContext: ExecutionContext
            get() = InMemoryExecutionContext { t ->
                    t.printStackTrace()
                }
    }

    @Test
    fun rocksCache(@TempDir tempDir: Path) {


        val pom = """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.2.11.RELEASE</version>
                        <relativePath/> 
                    </parent>
                    <groupId>com.foo</groupId>
                    <artifactId>test</artifactId>
                    <version>${"$"}{revision}</version>
                    <name>test</name>
                
                    <properties>
                        <java.version>1.8</java.version>
                        <spring-cloud-service.version>2.2.6.RELEASE</spring-cloud-service.version>
                        <spring-cloud.version>Hoxton.SR9</spring-cloud.version>
                        <jackson-bom.version>2.12.1</jackson-bom.version>
                        <guava-bom.version>30.1-jre</guava-bom.version>
                        <revision>1.0.0</revision>
                        <owaspDb>${"$"}{maven.repo.local}/org/owasp/dependency-check-data/3.0</owaspDb>
                        <argLine></argLine>
                        <org.mapstruct.version>1.4.2.Final</org.mapstruct.version>
                        <tomcat.version>9.0.43</tomcat.version>
                    </properties>
                
                    <dependencies>
                        <dependency>
                            <groupId>org.yaml</groupId>
                            <artifactId>snakeyaml</artifactId>
                            <version>1.27</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-actuator</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-starter-sleuth</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-data-jpa</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-devtools</artifactId>
                            <scope>runtime</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-test</artifactId>
                            <scope>test</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct</artifactId>
                            <version>${"$"}{org.mapstruct.version}</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>com.h2database</groupId>
                            <artifactId>h2</artifactId>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.flywaydb</groupId>
                            <artifactId>flyway-core</artifactId>
                            <version>6.0.8</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.postgresql</groupId>
                            <artifactId>postgresql</artifactId>
                            <scope>provided</scope>
                            <version>42.2.18.jre7</version>
                        </dependency>
                        <dependency>
                            <groupId>org.bouncycastle</groupId>
                            <artifactId>bcprov-jdk15on</artifactId>
                            <version>1.68</version>
                        </dependency>
                        <dependency>
                            <groupId>org.bouncycastle</groupId>
                            <artifactId>bcpkix-jdk15on</artifactId>
                            <version>1.68</version>
                        </dependency>
                        <dependency>
                            <groupId>org.hibernate.validator</groupId>
                            <artifactId>hibernate-validator</artifactId>
                            <version>7.0.1.Final</version>
                        </dependency>
                        <dependency>
                            <groupId>org.hibernate</groupId> 
                            <artifactId>hibernate-core</artifactId>
                            <version>5.4.28.Final</version>
                        </dependency>
                    </dependencies>
                
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>io.pivotal.spring.cloud</groupId>
                                <artifactId>spring-cloud-services-dependencies</artifactId>
                                <version>${"$"}{spring-cloud-service.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                            <dependency>
                                <groupId>org.springframework.cloud</groupId>
                                <artifactId>spring-cloud-dependencies</artifactId>
                                <version>${"$"}{spring-cloud.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                            <dependency>
                                <groupId>com.fasterxml.jackson</groupId>
                                <artifactId>jackson-bom</artifactId>
                                <version>${"$"}{jackson-bom.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                            <dependency>
                                <groupId>com.google.guava</groupId>
                                <artifactId>guava-bom</artifactId>
                                <version>${"$"}{guava-bom.version}</version>
                                <type>pom</type>
                                <scope>import</scope>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """
        //This is not much of a test, it just ensures the maven parser works correctly with the rocksdb caching
        //implementation. This does work as a good place to experiment with the various configuration settings.
        val workspace = Path.of(System.getProperty("user.home") + "/.rewrite/cache/")

        val mavenCache = RocksdbMavenPomCache(workspace)
        val pomAst = MavenParser.builder()
            .cache(mavenCache)
            .build()
            .parse(executionContext, pom)
            .first()

        Assertions.assertThat(pomAst.model).isNotNull
    }
}