/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.maven;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.MavenArtifactCache;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;
import org.openrewrite.maven.utilities.MavenArtifactDownloader;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;
import static org.openrewrite.maven.AddMavenWrapperChecksumValidation.MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES;

@Value
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddMavenWrapperChecksumValidation extends Recipe {

    public static final String MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES = ".mvn/wrapper/maven-wrapper.properties";

    @Override
    public String getDisplayName() {
        return "Add Maven Wrapper checksum properties";
    }

    @Override
    public String getDescription() {
        return "Add checksum properties to `.mvn/wrapper/maven-wrapper.properties` for use in validation.";
    }

    @Option(displayName = "Distribution version",
            description = "Distribution version.",
            example = "3.9.1")
    String distributionVersion;

    @Option(displayName = "Wrapper version",
            description = "Wrapper version.",
            example = "3.2.0")
    String wrapperVersion;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new HasSourcePath<>(MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES),
                new AddMavenWrapperChecksumVisitor(distributionVersion, wrapperVersion));
    }

}

class AddMavenWrapperChecksumVisitor extends PropertiesVisitor<ExecutionContext> {

    private static final String DISTRIBUTION_URL = "distributionUrl";
    private static final String WRAPPER_URL = "wrapperUrl";
    private static final String DISTRIBUTION_SHA_256_SUM = "distributionSha256Sum";
    private static final String WRAPPER_SHA_256_SUM = "wrapperSha256Sum";

    private final String distributionVersion;
    private final String wrapperVersion;

    public AddMavenWrapperChecksumVisitor(String distributionVersion, String wrapperVersion) {
        this.distributionVersion = distributionVersion;
        this.wrapperVersion = wrapperVersion;
    }

    @Override
    public Properties visitFile(Properties.File file, ExecutionContext context) {
        Properties mavenWrapperProperties = super.visitFile(file, context);

        if (equalIgnoringSeparators(file.getSourcePath(), Paths.get(MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES))) {
            mavenWrapperProperties = addShaForUrl(
                    mavenWrapperProperties, DISTRIBUTION_URL, DISTRIBUTION_SHA_256_SUM, this::downloadDistributionAndMarkToDelete);
            mavenWrapperProperties = addShaForUrl(
                    mavenWrapperProperties, WRAPPER_URL, WRAPPER_SHA_256_SUM, this::downloadWrapperAndMarkToDelete);
        }

        return mavenWrapperProperties;
    }

    @NotNull
    private Properties addShaForUrl(Properties properties, String urlKey, String shaKey, Callable<File> download) {
        Set<Properties.Entry> urls = FindProperties.find(properties, urlKey, false);
        Set<Properties.Entry> shas = FindProperties.find(properties, shaKey, false);
        if (urls.size() == 1 && shas.isEmpty()) {
            Properties.Entry distributionUrl = urls.stream().findFirst().get();
            try {
                File distribution = download.call();
                String distributionSha256Hex = sha256AsHex(distribution);
                return addPropertyToProperties(properties, shaKey, distributionSha256Hex);
            } catch (Exception e) {
                // NOP
            }
        }
        return properties;
    }

    private File downloadDistributionAndMarkToDelete() throws IOException {
        File distribution = downloadMavenArtifact(
                new ResolvedGroupArtifactVersion(
                        "https://repo1.maven.org/maven2",
                        "org.apache.maven",
                        "apache-maven",
                        distributionVersion,
                        distributionVersion + "-bin"
                ),
                "zip"
        );
        distribution.deleteOnExit();
        return distribution;
    }

    private File downloadWrapperAndMarkToDelete() throws IOException {
        File wrapper = downloadMavenArtifact(
                new ResolvedGroupArtifactVersion(
                        "https://repo1.maven.org/maven2",
                        "org.apache.maven.wrapper",
                        "maven-wrapper",
                        wrapperVersion,
                        null
                ),
                "jar"
        );
        wrapper.deleteOnExit();
        return wrapper;
    }

    private File downloadMavenArtifact(final ResolvedGroupArtifactVersion rgav, String type) throws IOException {
        final MavenArtifactCache cache = new LocalMavenArtifactCache(Files.createTempDirectory("repository"));
        //final MavenSettings settings = new MavenSettings();
        final MavenArtifactDownloader downloader = new MavenArtifactDownloader(cache, null, Throwable::printStackTrace);
        final ResolvedDependency rd = new ResolvedDependency(
                MavenRepository.MAVEN_CENTRAL,
                rgav,
                new Dependency(
                        null,
                        null,
                        type,
                        null,
                        Collections.emptyList(),
                        null
                ),
                Collections.emptyList(),
                Collections.emptyList(),
                null,
                null,
                false,
                1,
                Collections.emptyList()

        );

        return Optional
                .ofNullable(downloader.downloadArtifact(rd))
                .map(Path::toFile)
                .orElseThrow(() -> new RuntimeException("Could not download artifact " + rd.toString()));
    }

    private Properties addPropertyToProperties(Properties mavenWrapperProperties, String name, String value) {
        Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, value);
        Properties.Entry entry = new Properties.Entry(
                Tree.randomId(),
                "\n",
                Markers.EMPTY,
                name,
                "",
                Properties.Entry.Delimiter.EQUALS,
                propertyValue
        );
        List<Properties.Content> contentList = ListUtils.concat(((Properties.File) mavenWrapperProperties).getContent(), entry);
        return ((Properties.File) mavenWrapperProperties).withContent(contentList);
    }

    /**
     * Compute SHA256 of file.
     *
     * @param file file
     * @return SHA256 as hex
     * @see <a href="https://www.baeldung.com/sha-256-hashing-java">source</a>
     * @see {@link Checksum#sha256(SourceFile)}, but not a {@link SourceFile} which makes it cumbersome to refactor
     */
    private String sha256AsHex(File file) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(
                Files.readAllBytes(file.toPath()));

        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
