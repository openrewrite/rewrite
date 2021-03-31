package org.openrewrite.maven.utilities;

import org.openrewrite.ExecutionContext;
import org.openrewrite.maven.cache.InMemoryMavenPomCache;
import org.openrewrite.maven.cache.LocalMavenArtifactCache;
import org.openrewrite.maven.cache.ReadOnlyLocalMavenArtifactCache;
import org.openrewrite.maven.internal.MavenPomDownloader;
import org.openrewrite.maven.internal.RawMaven;
import org.openrewrite.maven.internal.RawMavenResolver;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.MavenRepository;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MavenArtifactHelper {

    private static final MavenRepository SUPER_POM_REPOSITORY = new MavenRepository("central",
            URI.create("https://repo.maven.apache.org/maven2"), true, false, null, null);

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx) {
        return downloadArtifactAndDependencies(groupId, artifactId, version, ctx, SUPER_POM_REPOSITORY, true);
    }

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenRepository repository) {
        return downloadArtifactAndDependencies(groupId, artifactId, version, ctx, repository, true);
    }

    public static List<Path> downloadArtifactAndDependencies(String groupId, String artifactId, String version, ExecutionContext ctx, MavenRepository repository, boolean normalizeRepositories) {
        MavenPomDownloader mavenPomDownloader = new MavenPomDownloader(new InMemoryMavenPomCache(),
                Collections.emptyMap(), ctx, normalizeRepositories);
        List<MavenRepository> repositories = new ArrayList<>();
        repositories.add(repository);
        RawMaven rawMaven = mavenPomDownloader.download(groupId, artifactId, version, null, null,
                repositories, ctx);
        if (rawMaven == null) {
            return Collections.emptyList();
        }
        Xml.Document xml = new RawMavenResolver(mavenPomDownloader, Collections.emptyList(), true, ctx, null).resolve(rawMaven, new HashMap<>());
        if (xml == null) {
            return Collections.emptyList();
        }
        Maven maven = new Maven(xml);
        MavenArtifactDownloader mavenArtifactDownloader = new MavenArtifactDownloader(ReadOnlyLocalMavenArtifactCache.MAVEN_LOCAL.orElse(
                new LocalMavenArtifactCache(Paths.get(System.getProperty("user.home"), ".rewrite-cache", "artifacts"))
        ), null, ctx.getOnError());
        List<Path> artifactPaths = new ArrayList<>();
        artifactPaths.add(mavenArtifactDownloader.downloadArtifact(new Pom.Dependency(repository, Scope.Compile, null, null, false, new Pom(
                groupId,
                artifactId,
                version,
                null,
                null,
                null,
                null,
                null,
                null,
                Collections.emptyList(),
                new Pom.DependencyManagement(Collections.emptyList()),
                Collections.emptyList(),
                repositories,
                Collections.emptyMap(),
                Collections.emptyMap()
        ), null, null, Collections.emptySet())));
        for (Pom.Dependency dependency : maven.getModel().getDependencies()) {
            artifactPaths.add(mavenArtifactDownloader.downloadArtifact(dependency));
        }
        return artifactPaths;
    }
}
