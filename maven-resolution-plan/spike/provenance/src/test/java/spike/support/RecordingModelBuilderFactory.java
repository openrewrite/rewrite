package spike.support;

import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.composition.DependencyManagementImporter;

/**
 * Proves P2's install seam: {@link DefaultModelBuilderFactory} exposes {@code newDependencyManagementImporter()} as a
 * protected override point, so a custom recording importer can be injected without any DI container.
 */
public class RecordingModelBuilderFactory extends DefaultModelBuilderFactory {
    public final RecordingDependencyManagementImporter recorder = new RecordingDependencyManagementImporter();

    @Override
    protected DependencyManagementImporter newDependencyManagementImporter() {
        return recorder;
    }
}
