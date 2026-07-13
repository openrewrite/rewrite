package spike;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spike.support.FilesystemModelResolver;
import spike.support.Poms;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1 -- LOCATION TRACKING: with {@code locationTracking=true}, every effective-model field carries an
 * {@link InputLocation} whose {@link org.apache.maven.model.InputSource#getModelId()} identifies the POM that
 * contributed it. A managed dependency version inherited from a parent is attributed to the PARENT.
 */
class ModelLocationTrackingTest {

    @Test
    void managedVersionInheritedFromParentIsAttributedToParent(@TempDir File tmp) throws Exception {
        Poms poms = new Poms(tmp);
        poms.write("com.example", "parent", "1",
                "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <groupId>com.example</groupId>" +
                "  <artifactId>parent</artifactId>" +
                "  <version>1</version>" +
                "  <packaging>pom</packaging>" +
                "  <dependencyManagement>" +
                "    <dependencies>" +
                "      <dependency>" +
                "        <groupId>com.example</groupId>" +
                "        <artifactId>lib</artifactId>" +
                "        <version>1.2.3</version>" +
                "      </dependency>" +
                "    </dependencies>" +
                "  </dependencyManagement>" +
                "</project>");
        File childPom = poms.write("com.example", "child", "1",
                "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <parent>" +
                "    <groupId>com.example</groupId>" +
                "    <artifactId>parent</artifactId>" +
                "    <version>1</version>" +
                "    <relativePath/>" +
                "  </parent>" +
                "  <artifactId>child</artifactId>" +
                "  <dependencies>" +
                "    <dependency>" +
                "      <groupId>com.example</groupId>" +
                "      <artifactId>lib</artifactId>" +
                "    </dependency>" +
                "  </dependencies>" +
                "</project>");

        ModelBuildingResult result = build(childPom, poms);
        Model effective = result.getEffectiveModel();

        // The managed dependency version is inherited and its InputLocation points at the PARENT model.
        Dependency managed = findManaged(effective, "com.example", "lib");
        assertEquals("1.2.3", managed.getVersion());
        InputLocation versionLoc = managed.getLocation("version");
        assertNotNull(versionLoc, "expected an InputLocation for the managed version");
        assertNotNull(versionLoc.getSource());
        assertEquals("com.example:parent:1", versionLoc.getSource().getModelId(),
                "managed version must be attributed to the parent that declared it");

        // The effective dependency got its version from the parent's dependencyManagement.
        Dependency dep = findDependency(effective, "com.example", "lib");
        assertEquals("1.2.3", dep.getVersion(), "version injected from parent-managed dependency");

        // Lineage ids expose the parent chain (rewrite EffectivePomMapper: ResolvedPom parent chain).
        assertTrue(result.getModelIds().contains("com.example:parent:1"),
                "model ids should include the parent: " + result.getModelIds());
        assertTrue(result.getModelIds().contains("com.example:child:1"));
    }

    static ModelBuildingResult build(File pom, Poms poms) throws Exception {
        DefaultModelBuilder builder = new DefaultModelBuilderFactory().newInstance();
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setModelSource(new FileModelSource(pom));
        req.setModelResolver(new FilesystemModelResolver(poms.byGav));
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setLocationTracking(true);
        req.setTwoPhaseBuilding(false);
        req.setSystemProperties(new Properties());
        req.setUserProperties(new Properties());
        return builder.build(req);
    }

    static Dependency findManaged(Model model, String groupId, String artifactId) {
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        throw new AssertionError("managed dependency not found: " + groupId + ":" + artifactId);
    }

    static Dependency findDependency(Model model, String groupId, String artifactId) {
        for (Dependency d : model.getDependencies()) {
            if (groupId.equals(d.getGroupId()) && artifactId.equals(d.getArtifactId())) {
                return d;
            }
        }
        throw new AssertionError("dependency not found: " + groupId + ":" + artifactId);
    }
}
