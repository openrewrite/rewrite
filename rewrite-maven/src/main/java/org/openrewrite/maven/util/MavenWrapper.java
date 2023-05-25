package org.openrewrite.maven.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.HttpSenderExecutionContextView;
import org.openrewrite.Validated;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Value
public class MavenWrapper
{
   public static final Path WRAPPER_JAR_LOCATION = Paths.get("maven/wrapper/maven-wrapper.jar");
   public static final Path WRAPPER_PROPERTIES_LOCATION = Paths.get("maven/wrapper/maven-wrapper.properties");
   public static final Path WRAPPER_SCRIPT_LOCATION = Paths.get("mvnw");
   public static final Path WRAPPER_BATCH_LOCATION = Paths.get("mvnw.bat");
   public static final String MAVEN_VERSIONS_URL =
      "https://search.maven.org/solrsearch/select?q=g:\"org.apache.maven\"+AND+a:\"apache-maven\"&core=gav&wt=json&rows=%d";
   public static final String MAVEN_ARTIFACT_BASE_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%s/apache-maven-%s%s";
   public static final String MAVEN_WRAPPER_VERSIONS_URL =
      "https://search.maven.org/solrsearch/select?q=g:\"org.apache.maven.wrapper\"+AND+a:\"maven-wrapper\"&core=gav&wt=json&rows=%d";
   public static final String MAVEN_WRAPPER_ARTIFACT_BASE_URL = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/%s/maven-wrapper-%s%s";

   public static Validated validate(
      ExecutionContext ctx,
      String version,
      @Nullable String distribution,
      @Nullable Validated cachedValidation,
      @Nullable String repositoryUrl) {
      if (cachedValidation != null) {
         return cachedValidation;
      }
      HttpSender httpSender = HttpSenderExecutionContextView.view(ctx).getHttpSender();

      //noinspection unchecked
      return new Validated.Both(
         Semver.validate(version, null),
         Semver.validate(version, null)
      ) {
         MavenWrapper wrapper;

         @Override
         public boolean isValid() {
            if (!super.isValid()) {
               return false;
            }
            try {
               buildWrapper();
               return true;
            } catch (Throwable t) {
               return false;
            }
         }

         @Override
         public MavenWrapper getValue() {
            return buildWrapper();
         }

         private MavenWrapper buildWrapper() {
            if (wrapper != null) {
               return wrapper;
            }

            VersionComparator versionComparator = requireNonNull(Semver.validate(version, null).getValue());

            final String allVersionsUrl = getAllVersionsUrl(httpSender, MAVEN_VERSIONS_URL.formatted( 1 ) );

            String mavenVersionsUrl = (repositoryUrl == null) ?  allVersionsUrl : repositoryUrl;
            try (HttpSender.Response resp = httpSender.send(httpSender.get(mavenVersionsUrl).build())) {
               if (resp.isSuccessful()) {
                  final var objectMapper = new ObjectMapper()
                     .registerModule( new ParameterNamesModule() )
                     .configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
                  final var jsonNode = objectMapper
                     .readTree( resp.getBody() )
                     .get( "response" )
                     .get( "docs" );

                  final var allVersions =
                     objectMapper.readValue( jsonNode.toString(), new TypeReference< List< MavenVersion > >()
                     {
                     } );
                  MavenVersion mavenVersion = allVersions.stream()
                     .filter(v -> versionComparator.isValid(null, v.version))
                     .max((v1, v2) -> versionComparator.compare(null, v1.version, v2.version))
                     .orElseThrow(() -> new IllegalStateException("Expected to find at least one Maven wrapper version to select from."));

                  DistributionInfos infos = DistributionInfos.fetch(httpSender, distributionType, mavenVersion);
                  wrapper = new MavenWrapper(mavenVersion.version, infos);
                  return wrapper;
               }
               throw new IOException("Could not get Maven versions at: " + mavenVersionsUrl);
            } catch (IOException e) {
               throw new UncheckedIOException(e);
            }
         }

      };
   }

   private static String getAllVersionsUrl( final HttpSender httpSender, final String queryUrl )
   {
      try(HttpSender.Response resp = httpSender.send(httpSender.get(queryUrl).build())) {
         if(!resp.isSuccessful()) {
            throw new RuntimeException("");
         }
         try {
            final var jsonNode = new ObjectMapper()
               .registerModule( new ParameterNamesModule() )
               .readTree( resp.getBody() );
            final var numberOfVersions = jsonNode.get( "response" ).get( "numFound" ).intValue();
            return MAVEN_VERSIONS_URL.formatted( numberOfVersions );
         }
         catch( IOException e ) {
            throw new RuntimeException( e );
         }
      }
   }

   @Value
   static class MavenVersion {
      @JsonProperty("v")
      String version;
   }
}
