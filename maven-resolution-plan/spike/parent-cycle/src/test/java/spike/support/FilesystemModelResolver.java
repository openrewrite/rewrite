package spike.support;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Repository;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.model.resolution.UnresolvableModelException;

import java.io.File;
import java.util.Map;

/** Plain no-network {@link ModelResolver} serving POMs from an on-disk GAV -> File map. No cycle awareness. */
public class FilesystemModelResolver implements ModelResolver {
    private final Map<String, File> byGav;

    public FilesystemModelResolver(Map<String, File> byGav) {
        this.byGav = byGav;
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
    public ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
        return resolveModel(parent.getGroupId(), parent.getArtifactId(), parent.getVersion());
    }

    @Override
    public ModelSource resolveModel(Dependency dependency) throws UnresolvableModelException {
        return resolveModel(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    @Override
    public void addRepository(Repository repository) {
    }

    @Override
    public void addRepository(Repository repository, boolean replace) {
    }

    @Override
    public ModelResolver newCopy() {
        return this;
    }
}
