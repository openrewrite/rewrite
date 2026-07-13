package spike.support;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.composition.DefaultDependencyManagementImporter;
import org.apache.maven.model.composition.DependencyManagementImporter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recording {@link DependencyManagementImporter} that intercepts every scope=import BOM merge and records
 * GA -> BOM modelId (derived from each imported managed dependency's {@link InputLocation}), then delegates to the
 * stock importer so the effective model is unchanged. This is the seam that would reconstruct rewrite's {@code bomGav}.
 *
 * NOTE: the {@code sources} argument is only a {@code List<DependencyManagement>} -- the importing BOM's GAV is NOT
 * passed. The modelId is therefore derived from each managed dependency's InputLocation, which points at the model
 * that DEFINED the entry (the directly-imported BOM for single-level imports, but a parent of the BOM for multi-level
 * chains). See RESULTS.md.
 */
public class RecordingDependencyManagementImporter implements DependencyManagementImporter {
    private final DefaultDependencyManagementImporter delegate = new DefaultDependencyManagementImporter();

    /** GA ("groupId:artifactId") -> defining/importing BOM modelId ("g:a:v"), first-import-wins to mirror the merge. */
    public final Map<String, String> gaToBomModelId = new LinkedHashMap<>();

    @Override
    public void importManagement(Model target, List<? extends DependencyManagement> sources,
                                 ModelBuildingRequest request, ModelProblemCollector problems) {
        if (sources != null) {
            for (DependencyManagement source : sources) {
                for (Dependency d : source.getDependencies()) {
                    String ga = d.getGroupId() + ":" + d.getArtifactId();
                    gaToBomModelId.putIfAbsent(ga, modelIdOf(d));
                }
            }
        }
        delegate.importManagement(target, sources, request, problems);
    }

    private static String modelIdOf(Dependency d) {
        InputLocation loc = d.getLocation("version");
        if (loc == null) {
            loc = d.getLocation("artifactId");
        }
        if (loc == null) {
            loc = d.getLocation("");
        }
        return (loc != null && loc.getSource() != null) ? loc.getSource().getModelId() : null;
    }
}
