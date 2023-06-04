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
import org.apache.commons.io.FileUtils;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.properties.PropertiesVisitor;
import org.openrewrite.properties.search.FindProperties;
import org.openrewrite.properties.tree.Properties;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Set;

import static org.openrewrite.PathUtils.equalIgnoringSeparators;

@Value
// @AllArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AddMavenWrapperChecksumValidation extends Recipe {

  public static final String MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES = ".mvn/wrapper/maven-wrapper.properties";
  public static final String DISTRIBUTION_URL = "distributionUrl";
  public static final String WRAPPER_URL = "wrapperUrl";
  public static final String DISTRIBUTION_SHA_256_SUM = "distributionSha256Sum";
  public static final String WRAPPER_SHA_256_SUM = "wrapperSha256Sum";

  @Override
  public String getDisplayName() {
    return "Add checksum validation to maven wrapper";
  }

  @Override
  public String getDescription() {
    return "Add checksum validation to maven wrapper if used.";
  }

  @Override
  protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
    return new HasSourcePath<>(MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES);
  }

  @Override
  public TreeVisitor<?, ExecutionContext> getVisitor() {
    return new PropertiesVisitor<ExecutionContext>() {
      @Override
      public Properties visitFile(Properties.File file, ExecutionContext context) {
        Properties mavenWrapperProperties = super.visitFile(file, context);

        if (equalIgnoringSeparators(file.getSourcePath(), Paths.get(MVN_WRAPPER_MAVEN_WRAPPER_PROPERTIES))) {
          Set<Properties.Entry> distributionUrls =
              FindProperties.find(mavenWrapperProperties, DISTRIBUTION_URL, false);
          Set<Properties.Entry> distributionSha256Hexs =
              FindProperties.find(mavenWrapperProperties, DISTRIBUTION_SHA_256_SUM, false);

          if (distributionUrls.size() == 1 && distributionSha256Hexs.isEmpty()) {
            Properties.Entry distributionUrl = distributionUrls.stream().findFirst().get();

            try {
              File distribution = downloadDistributionAndMarkToDelete(distributionUrl.getValue().getText());

              String distributionSha256Hex = sha256AsHex(distribution);

              mavenWrapperProperties = addPropertyToProperties(mavenWrapperProperties, DISTRIBUTION_SHA_256_SUM, distributionSha256Hex);
            } catch (IOException | NoSuchAlgorithmException e) {
              // NOP
            }
          }


          Set<Properties.Entry> wrapperUrls =
                  FindProperties.find(mavenWrapperProperties, WRAPPER_URL, false);
          Set<Properties.Entry> wrapperSha256Hexs =
                  FindProperties.find(mavenWrapperProperties, WRAPPER_SHA_256_SUM, false);

          if (wrapperUrls.size() == 1 && wrapperSha256Hexs.isEmpty()) {
            Properties.Entry wrapperUrl = wrapperUrls.stream().findFirst().get();

            try {
              File wrapper = downloadWrapperAndMarkToDelete(wrapperUrl.getValue().getText());

              String wrapperSha256Hex = sha256AsHex(wrapper);

              mavenWrapperProperties = addPropertyToProperties(mavenWrapperProperties, WRAPPER_SHA_256_SUM, wrapperSha256Hex);
            } catch (IOException | NoSuchAlgorithmException e) {
              // NOP
            }
          }
        }

        return mavenWrapperProperties;
      }
    };
  }

  private File downloadDistributionAndMarkToDelete(String distributionUrl) throws IOException {
    File distribution = File.createTempFile("rewrite", "download");
    FileUtils.copyURLToFile(new URL(distributionUrl), distribution, 10_000, 10_000);
    distribution.deleteOnExit();
    return distribution;
  }

  private File downloadWrapperAndMarkToDelete(String wrapperUrl) throws IOException {
    File distribution = File.createTempFile("rewrite", "download");
    FileUtils.copyURLToFile(new URL(wrapperUrl), distribution, 10_000, 10_000);
    distribution.deleteOnExit();
    return distribution;
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
    mavenWrapperProperties = ((Properties.File) mavenWrapperProperties).withContent(contentList);
    return mavenWrapperProperties;
  }

  /**
   * Compute SHA256 of file.
   * 
   * @param file file
   * @return SHA256 as hex
   * 
   * @see <a href="https://www.baeldung.com/sha-256-hashing-java">source</a>
   */
  private String sha256AsHex(File file) throws NoSuchAlgorithmException, IOException {
    
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hash = digest.digest(
            FileUtils.readFileToByteArray(file));

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
