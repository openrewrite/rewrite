# Dependencies

## Project dependencies including transitive dependencies

Complete dependency tree including transitive dependencies. Use this to understand what libraries the project uses and avoid suggesting dependencies that conflict with existing ones.

## Data Tables

### Dependency report

**File:** [`dependency-list-report.csv`](dependency-list-report.csv)

Lists all Gradle and Maven dependencies

| Column | Description |
|--------|-------------|
| Build tool | The build tool used to manage dependencies (Gradle or Maven). |
| Group id | The Group ID of the Gradle project or Maven module requesting the dependency. |
| Artifact id | The Artifact ID of the Gradle project or Maven module requesting the dependency. |
| Version | The version of Gradle project or Maven module requesting the dependency. |
| Dependency group id | The Group ID of the dependency. |
| Dependency artifact id | The Artifact ID of the dependency. |
| Dependency version | The version of the dependency. |
| Direct Dependency | When `true` the project directly depends on the dependency. When `false` the project depends on the dependency transitively through at least one direct dependency. |
| Resolution failure | The reason why the dependency could not be resolved. Blank when resolution was not attempted. |

