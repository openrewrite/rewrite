package spike.support;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A cycle-aware {@link ModelResolver} that detects an about-to-repeat parent GAV and serves a synthesized parentless
 * stub instead of recursing. Models the plan's proposed {@code LenientModelResolver} cycle break.
 *
 * <p>Two strategies, matching E3(a)/E3(b):
 * <ul>
 *   <li>{@link Strategy#KEEP_GAV} — stub keeps the revisited GAV; the passed {@link Parent} is left untouched.</li>
 *   <li>{@link Strategy#MUTATE_ID} — the passed {@link Parent}'s artifactId is mutated to a synthesized non-colliding
 *       value before the stub is returned. Because {@code DefaultModelBuilder} derives the cycle-check id from the
 *       (mutated) {@link Parent} element, this is the only resolver-level lever that can change that id.</li>
 * </ul>
 *
 * <p>The resolver must be seeded with the root GAV(s) being built, because the fatal {@code parentIds} check fires the
 * instant the root GAV is re-added — one step before the resolver would otherwise notice a repeated parent request.
 */
public class CycleAwareModelResolver implements ModelResolver {
    public enum Strategy { KEEP_GAV, MUTATE_ID }

    private final Map<String, File> byGav;
    private final File stubDir;
    private final Strategy strategy;
    private final Set<String> seen;
    public final List<String> log = new ArrayList<>();

    public CycleAwareModelResolver(Map<String, File> byGav, File stubDir, Strategy strategy, String... seedRootGavs) {
        this.byGav = byGav;
        this.stubDir = stubDir;
        this.strategy = strategy;
        this.seen = new LinkedHashSet<>();
        for (String g : seedRootGavs) {
            seen.add(g);
        }
    }

    @Override
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        String gav = parent.getGroupId() + ":" + parent.getArtifactId() + ":" + parent.getVersion();
        if (seen.contains(gav)) {
            if (strategy == Strategy.MUTATE_ID) {
                String mutatedArtifactId = parent.getArtifactId() + "--cyclebreak";
                parent.setArtifactId(mutatedArtifactId);
                String mutatedGav = parent.getGroupId() + ":" + mutatedArtifactId + ":" + parent.getVersion();
                log.add("STUB(MUTATE_ID) revisit=" + gav + " served-as=" + mutatedGav);
                return stub(parent.getGroupId(), mutatedArtifactId, parent.getVersion());
            }
            log.add("STUB(KEEP_GAV) revisit=" + gav);
            return stub(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
        }
        seen.add(gav);
        log.add("SERVE " + gav);
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {
        File f = byGav.get(groupId + ":" + artifactId + ":" + version);
        if (f == null || !f.isFile()) {
            throw new UnresolvableModelException("No fixture POM for " + groupId + ":" + artifactId + ":" + version,
                    groupId, artifactId, version);
        }
        return new FileModelSource(f);
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    /** A minimal, parentless {@code pom}-packaged stub written to disk so the returned source is a real file. */
    private ModelSource stub(String groupId, String artifactId, String version) {
        String xml = "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>" + groupId + "</groupId>" +
                "<artifactId>" + artifactId + "</artifactId>" +
                "<version>" + version + "</version>" +
                "<packaging>pom</packaging>" +
                "</project>";
        try {
            File f = new File(stubDir, "stub-" + artifactId + "-" + version + ".pom");
            Files.createDirectories(stubDir.toPath());
            Files.write(f.toPath(), xml.getBytes(StandardCharsets.UTF_8));
            return new FileModelSource(f);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addRepository(Repository repository) {
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
    }

    @Override
    public ModelResolver newCopy() {
        return this; // share the seen-set across the whole build
    }
}
