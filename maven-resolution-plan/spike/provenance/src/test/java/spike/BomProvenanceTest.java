package spike;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spike.support.FilesystemModelResolver;
import spike.support.Poms;
import spike.support.RecordingModelBuilderFactory;

import java.io.File;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P2 -- BOM PROVENANCE ON MAVEN 3.9. Maven 3.9 has no {@code importedFrom} (that is Maven 4). We prove:
 * (a) a recording {@link org.apache.maven.model.composition.DependencyManagementImporter} installs cleanly via
 *     {@link org.apache.maven.model.building.DefaultModelBuilderFactory}'s protected override point;
 * (b) for a directly-declared (single-level) BOM, both the recorded map AND the default build's InputLocation
 *     attribute the managed entry to the importing BOM's GAV -- reconstructing rewrite's {@code bomGav};
 * (c) for a MULTI-LEVEL BOM (BOM inherits its dependencyManagement from a parent), the InputLocation (and thus the
 *     importer, which reads it) points at the DEFINING pom (the BOM's parent), NOT the directly-imported BOM.
 *     This is exactly the gap Maven 4's {@code importedFrom} closes. Documented as a caveat.
 */
class BomProvenanceTest {

    @Test
    void singleLevelBomEntryIsAttributedToTheImportingBom(@TempDir File tmp) throws Exception {
        Poms poms = new Poms(tmp);
        poms.write("com.example", "bom", "1",
                project("com.example", "bom", "1", "pom",
                        "  <dependencyManagement>" +
                        "    <dependencies>" +
                        "      <dependency>" +
                        "        <groupId>com.example</groupId>" +
                        "        <artifactId>lib</artifactId>" +
                        "        <version>2.0.0</version>" +
                        "      </dependency>" +
                        "    </dependencies>" +
                        "  </dependencyManagement>"));
        File appPom = poms.write("com.example", "app", "1",
                project("com.example", "app", "1", "jar",
                        importBom("com.example", "bom", "1") +
                        "  <dependencies>" +
                        "    <dependency>" +
                        "      <groupId>com.example</groupId>" +
                        "      <artifactId>lib</artifactId>" +
                        "    </dependency>" +
                        "  </dependencies>"));

        RecordingModelBuilderFactory factory = new RecordingModelBuilderFactory();
        ModelBuildingResult result = build(factory, appPom, poms);
        Model effective = result.getEffectiveModel();

        // The BOM supplied the version.
        assertEquals("2.0.0", findDependency(effective, "com.example", "lib").getVersion());

        // (b1) Custom importer reconstructed GA -> importing BOM GAV == rewrite bomGav.
        assertEquals("com.example:bom:1", factory.recorder.gaToBomModelId.get("com.example:lib"),
                "recorded importing-BOM GAV should match rewrite's bomGav");

        // (b2) Default-build InputLocation of the imported managed entry ALSO points at the BOM.
        Dependency managed = findManaged(effective, "com.example", "lib");
        InputLocation loc = managed.getLocation("version");
        assertNotNull(loc);
        assertEquals("com.example:bom:1", loc.getSource().getModelId(),
                "InputLocation of a directly-imported managed entry points at the importing BOM");
    }

    @Test
    void multiLevelBomEntryIsAttributedToDefiningParentNotImportedBom(@TempDir File tmp) throws Exception {
        Poms poms = new Poms(tmp);
        poms.write("com.example", "parentbom", "1",
                project("com.example", "parentbom", "1", "pom",
                        "  <dependencyManagement>" +
                        "    <dependencies>" +
                        "      <dependency>" +
                        "        <groupId>com.example</groupId>" +
                        "        <artifactId>lib</artifactId>" +
                        "        <version>2.0.0</version>" +
                        "      </dependency>" +
                        "    </dependencies>" +
                        "  </dependencyManagement>"));
        // bom2 inherits its entire dependencyManagement from parentbom and declares none of its own.
        poms.write("com.example", "bom2", "1",
                "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <parent>" +
                "    <groupId>com.example</groupId>" +
                "    <artifactId>parentbom</artifactId>" +
                "    <version>1</version>" +
                "    <relativePath/>" +
                "  </parent>" +
                "  <artifactId>bom2</artifactId>" +
                "  <packaging>pom</packaging>" +
                "</project>");
        File appPom = poms.write("com.example", "app2", "1",
                project("com.example", "app2", "1", "jar",
                        importBom("com.example", "bom2", "1") +
                        "  <dependencies>" +
                        "    <dependency>" +
                        "      <groupId>com.example</groupId>" +
                        "      <artifactId>lib</artifactId>" +
                        "    </dependency>" +
                        "  </dependencies>"));

        RecordingModelBuilderFactory factory = new RecordingModelBuilderFactory();
        ModelBuildingResult result = build(factory, appPom, poms);
        Model effective = result.getEffectiveModel();

        assertEquals("2.0.0", findDependency(effective, "com.example", "lib").getVersion());

        // CAVEAT: both InputLocation and the importer attribute the entry to the DEFINING pom (parentbom),
        // NOT the directly-imported bom2. rewrite's bomGav for this entry would be com.example:bom2:1.
        Dependency managed = findManaged(effective, "com.example", "lib");
        assertEquals("com.example:parentbom:1", managed.getLocation("version").getSource().getModelId(),
                "3.9 InputLocation resolves to the defining pom, not the directly-imported BOM");
        assertEquals("com.example:parentbom:1", factory.recorder.gaToBomModelId.get("com.example:lib"),
                "importer derives BOM identity from the same InputLocation, so it shares the limitation");
    }

    // ---- helpers ----

    static ModelBuildingResult build(RecordingModelBuilderFactory factory, File pom, Poms poms) throws Exception {
        DefaultModelBuilder builder = factory.newInstance();
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

    static String project(String g, String a, String v, String packaging, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "  <modelVersion>4.0.0</modelVersion>" +
                "  <groupId>" + g + "</groupId>" +
                "  <artifactId>" + a + "</artifactId>" +
                "  <version>" + v + "</version>" +
                "  <packaging>" + packaging + "</packaging>" +
                body +
                "</project>";
    }

    static String importBom(String g, String a, String v) {
        return "  <dependencyManagement>" +
                "    <dependencies>" +
                "      <dependency>" +
                "        <groupId>" + g + "</groupId>" +
                "        <artifactId>" + a + "</artifactId>" +
                "        <version>" + v + "</version>" +
                "        <type>pom</type>" +
                "        <scope>import</scope>" +
                "      </dependency>" +
                "    </dependencies>" +
                "  </dependencyManagement>";
    }

    static Dependency findManaged(Model model, String g, String a) {
        for (Dependency d : model.getDependencyManagement().getDependencies()) {
            if (g.equals(d.getGroupId()) && a.equals(d.getArtifactId())) {
                return d;
            }
        }
        throw new AssertionError("managed dependency not found: " + g + ":" + a);
    }

    static Dependency findDependency(Model model, String g, String a) {
        for (Dependency d : model.getDependencies()) {
            if (g.equals(d.getGroupId()) && a.equals(d.getArtifactId())) {
                return d;
            }
        }
        throw new AssertionError("dependency not found: " + g + ":" + a);
    }
}
