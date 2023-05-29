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
import org.apache.commons.codec.digest.DigestUtils;
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
  public static final String DISTRIBUTION_SHA_256_SUM = "distributionSha256Sum";

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
              File distribution = File.createTempFile("rewrite", "download");
              FileUtils.copyURLToFile(new URL(distributionUrl.getValue().getText()), distribution, 10_000, 10_000);
              distribution.deleteOnExit();

              String distributionSha256Hex = new DigestUtils("SHA-256").digestAsHex(distribution);

              Properties.Value propertyValue = new Properties.Value(Tree.randomId(), "", Markers.EMPTY, distributionSha256Hex);
              Properties.Entry entry = new Properties.Entry(
                  Tree.randomId(),
                  "\n",
                  Markers.EMPTY,
                  DISTRIBUTION_SHA_256_SUM,
                  "",
                  Properties.Entry.Delimiter.EQUALS,
                  propertyValue
              );
              List<Properties.Content> contentList = ListUtils.concat(((Properties.File) mavenWrapperProperties).getContent(), entry);
              mavenWrapperProperties = ((Properties.File) mavenWrapperProperties).withContent(contentList);
            } catch (IOException e) {
              //
            }
          }
        }

        return mavenWrapperProperties;
      }
    };
  }
}
