package spike.support;

import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.FileModelSource;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.resolution.ModelResolver;

import java.io.File;
import java.util.Properties;

/** Shared POM-string builders and a standard {@link DefaultModelBuilder} driver. */
public final class Fixtures {
    private Fixtures() {
    }

    public static DefaultModelBuilder builder() {
        return new DefaultModelBuilderFactory().newInstance();
    }

    public static ModelBuildingResult build(File rootPom, ModelResolver resolver) throws Exception {
        return builder().build(request(rootPom, resolver));
    }

    /**
     * Build with a caller-supplied raw root model. Setting {@code rawModel} makes {@code DefaultModelBuilder} skip the
     * root {@code readModel(source)} and therefore its {@code validateRawModel} — the only lever that avoids the
     * unconditional self-parent FATAL.
     */
    public static ModelBuildingResult buildFromRawModel(File rootPom, Model rawModel, ModelResolver resolver)
            throws Exception {
        DefaultModelBuildingRequest req = request(rootPom, resolver);
        req.setRawModel(rawModel);
        return builder().build(req);
    }

    public static Model readRaw(File pom) throws Exception {
        return new DefaultModelReader().read(pom, null);
    }

    private static DefaultModelBuildingRequest request(File rootPom, ModelResolver resolver) {
        DefaultModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setModelSource(new FileModelSource(rootPom));
        req.setModelResolver(resolver);
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setLocationTracking(true);
        req.setTwoPhaseBuilding(false);
        req.setSystemProperties(new Properties());
        req.setUserProperties(new Properties());
        return req;
    }

    /** A project whose parent is resolved through the {@link ModelResolver} (empty relativePath). */
    public static String withRepoParent(String g, String a, String v, String packaging,
                                        String pg, String pa, String pv, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<parent>" +
                "<groupId>" + pg + "</groupId>" +
                "<artifactId>" + pa + "</artifactId>" +
                "<version>" + pv + "</version>" +
                "<relativePath/>" +
                "</parent>" +
                "<groupId>" + g + "</groupId>" +
                "<artifactId>" + a + "</artifactId>" +
                "<version>" + v + "</version>" +
                "<packaging>" + packaging + "</packaging>" +
                body +
                "</project>";
    }

    /** A project whose parent is resolved via a filesystem relativePath. */
    public static String withRelativeParent(String g, String a, String v, String packaging,
                                            String pg, String pa, String pv, String relativePath, String body) {
        return "<project xmlns='http://maven.apache.org/POM/4.0.0'>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<parent>" +
                "<groupId>" + pg + "</groupId>" +
                "<artifactId>" + pa + "</artifactId>" +
                "<version>" + pv + "</version>" +
                "<relativePath>" + relativePath + "</relativePath>" +
                "</parent>" +
                "<groupId>" + g + "</groupId>" +
                "<artifactId>" + a + "</artifactId>" +
                "<version>" + v + "</version>" +
                "<packaging>" + packaging + "</packaging>" +
                body +
                "</project>";
    }

    /** A single managed dependency block, to prove parent-contributed inheritance survives a cycle break. */
    public static String managed(String g, String a, String v) {
        return "<dependencyManagement><dependencies><dependency>" +
                "<groupId>" + g + "</groupId>" +
                "<artifactId>" + a + "</artifactId>" +
                "<version>" + v + "</version>" +
                "</dependency></dependencies></dependencyManagement>";
    }
}
