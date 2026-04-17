# 6. Recipe Marketplace CSV Format

Date: 2025-11-14

## Status

Accepted

## Context

We need a standardized way to represent and exchange recipe marketplace data, including recipes, their categorization, options, and bundle information. The format should:

1. Be human-readable and editable
2. Support hierarchical categorization of recipes
3. Accommodate recipes with varying numbers of options
4. Handle both minimal recipe catalogs (descriptions only) and enriched marketplaces (with bundle installation details)
5. Support round-trip serialization/deserialization without data loss

Similar to the approach used in `moderne-organizations-format` for repository organization, we chose CSV as the interchange format due to its widespread tool support, simplicity, and ease of integration with existing systems.

## Decision

We will use a CSV format for recipe marketplace data with the following structure:

### Required Columns

- **`ecosystem`**: The package ecosystem (e.g., `maven`, `npm`, `yaml`)
- **`packageName`**: The package identifier (e.g., `org.openrewrite:rewrite-java`)
- **`name`**: The fully qualified recipe name (e.g., `org.openrewrite.java.cleanup.UnnecessaryParentheses`)

### Optional Recipe Metadata Columns

- **`displayName`**: Human-readable recipe display name
- **`description`**: Recipe description
- **`recipeCount`**: Integer count of recipes (direct + transitive), defaults to 1
- **`estimatedEffortPerOccurrence`**: ISO-8601 duration format (e.g., PT5M, PT1H)
- **`category1, category2, ..., categoryN`**: Zero or more category columns, read **left to right** with **left representing the deepest level category**
- **`category1Description, category2Description, ..., categoryNDescription`**: Optional descriptions for each category level
- **`options`**: JSON array of `OptionDescriptor` objects (see JSON Format section below)
- **`dataTables`**: JSON array of `DataTableDescriptor` objects (see JSON Format section below)

### Optional Bundle Columns

- **`version`**: Resolved package version (optional, allows version-independent recipe catalogs)
- **`requestedVersion`**: Version constraint as requested (e.g., `LATEST`, `0.2.0-SNAPSHOT`)
- **`team`**: Optional team identifier for marketplace partitioning

### Metadata Columns

Any unrecognized columns are preserved as metadata in `RecipeListing.getMetadata()` as a `Map<String, Object>`, allowing forward compatibility with future extensions.

### JSON Format for Options

The `options` column contains a JSON array of option descriptors:

```json
[
  {
    "name": "groupId",
    "type": "String",
    "displayName": "Group ID",
    "description": "The group ID of the dependency.",
    "example": "org.openrewrite",
    "required": true
  },
  {
    "name": "artifactId",
    "type": "String",
    "displayName": "Artifact ID",
    "description": "The artifact ID of the dependency.",
    "required": false
  }
]
```

### JSON Format for DataTables

The `dataTables` column contains a JSON array of data table descriptors:

```json
[
  {
    "name": "org.openrewrite.java.dependencies.DependencyListTable",
    "displayName": "Dependencies",
    "description": "Lists all dependencies found in the project.",
    "columns": [
      {
        "name": "groupId",
        "type": "String",
        "displayName": "Group ID",
        "description": "The dependency group."
      }
    ]
  }
]
```

The following data tables are excluded during writing as they are internal infrastructure tables:
- `org.openrewrite.table.SearchResults`
- `org.openrewrite.table.SourcesFileResults`
- `org.openrewrite.table.SourcesFileErrors`
- `org.openrewrite.table.RecipeRunStats`
- `org.openrewrite.table.ParseFailures`

### Category Structure

The `RecipeMarketplace` is a recursive data structure where each category is itself a marketplace. Categories are determined by column headers starting with "category":

```csv
name,category1,category2,category3
org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices
```

This creates the hierarchy: `Best Practices > Java > Cleanup > UnnecessaryParentheses`

The displayName of a category corresponds to the value in its category column.

### Root Category

The `RecipeMarketplace` maintains an internal root category that contains all top-level categories. The root is not exposed in the CSV format but allows the marketplace to support multiple top-level category trees.

### Version-Independent Catalogs

Since `version` is optional, marketplaces can represent version-independent recipe catalogs that describe what recipes exist and their categorization without tying them to specific package versions. Version information can be added later when recipes are installed or resolved in a specific environment.

### Implementation

- **Data Model**:
  - `RecipeBundle`: Data class containing:
    - `packageEcosystem`: The package ecosystem (e.g., `maven`, `npm`, `yaml`)
    - `packageName`: The package identifier
    - `requestedVersion`: Version constraint as requested (optional)
    - `version`: Resolved version (optional)
    - `team`: Team identifier (optional)
  - `RecipeListing`: Represents a recipe with metadata:
    - `name`, `displayName`, `description`
    - `estimatedEffortPerOccurrence`: ISO-8601 duration
    - `options`: List of `OptionDescriptor` objects
    - `dataTables`: List of `DataTableDescriptor` objects
    - `recipeCount`: Total count of recipes (direct + transitive)
    - `metadata`: Map of custom key-value pairs from unknown columns
    - `bundle`: Associated `RecipeBundle`
  - `RecipeMarketplace`: Hierarchical structure with nested `Category` instances and a list of `RecipeBundleResolver` instances

- **Reader**: `RecipeMarketplaceReader` (using univocity-parsers)
  - Parses CSV into `RecipeMarketplace` hierarchies
  - Creates `RecipeListing` instances with `RecipeBundle` objects from CSV data
  - Uses Jackson `ObjectMapper` for JSON deserialization of options and dataTables
  - Requires `ecosystem` and `packageName` columns; other columns are optional
  - Dynamically detects category columns by header prefix
  - Preserves unknown columns as metadata

- **Writer**: `RecipeMarketplaceWriter` (using univocity-parsers)
  - Dynamically determines required columns based on marketplace content
  - Always includes `ecosystem`, `packageName`, and `name` columns
  - Only includes optional columns if at least one recipe has data for them
  - Uses Jackson for JSON serialization with `NON_DEFAULT` inclusion
  - Places `options` and `dataTables` columns last for human readability

- **Bundle Resolution**: Two-phase resolution system
  - `RecipeBundleResolver`: Interface with `getEcosystem()` and `resolve(RecipeBundle)` methods
    - Ecosystem-specific resolvers are registered with the `RecipeMarketplace`
    - Implementations:
      - `MavenRecipeBundleResolver` in `rewrite-maven`
      - `NpmRecipeBundleResolver` in `rewrite-javascript`
      - `YamlRecipeBundleResolver` in `rewrite-core`
  - `RecipeBundleReader`: Interface returned by resolvers with methods:
    - `getBundle()`: Returns the associated `RecipeBundle`
    - `read()`: Reads the bundle and returns a `RecipeMarketplace`
    - `describe(RecipeListing)`: Returns a `RecipeDescriptor` for a listing
    - `prepare(RecipeListing, Map<String, Object>)`: Creates a configured `Recipe` instance
  - Implementations:
    - `MavenRecipeBundleReader`: Downloads Maven artifacts, reads `META-INF/rewrite/recipes.csv` from JAR, falls back to classpath scanning
    - `NpmRecipeBundleReader`: Uses `JavaScriptRewriteRpc` for remote communication with npm packages
    - `YamlRecipeBundleReader`: Reads recipes from YAML files by path or URI
  - `RecipeListing.resolve()`: Convenience method that finds the appropriate resolver and returns a `RecipeBundleReader`

- **Validators**: Tools for ensuring marketplace quality and completeness
  - `RecipeMarketplaceContentValidator` in `rewrite-core`: Validates content formatting rules
    - Display names must start with uppercase and not end with a period
    - Descriptions must end with a period
    - Returns `Validated<RecipeMarketplace>` with all formatting errors found
    - Recursively validates all categories and recipes in the hierarchy
  - `RecipeMarketplaceCompletenessValidator` in `rewrite-core`: Validates CSV ↔ JAR synchronization
    - Ensures every recipe in CSV exists in the JAR's `Environment`
    - Ensures every recipe in the JAR has at least one entry in the CSV
    - Detects "phantom recipes" (in CSV but not in JAR) and "missing recipes" (in JAR but not in CSV)
    - Returns `Validated<RecipeMarketplace>` with all completeness errors found
    - Uses `RecipeMarketplace.getAllRecipes()` to handle recipes appearing in multiple categories
    - Intended to prevent CSV from becoming stale as recipes are added/removed from source code

## Consequences

### Positive

1. **Human-editable**: CSV files can be created and modified in spreadsheet tools or text editors
2. **Flexible schema**: Dynamic column detection accommodates varying numbers of categories and options
3. **Version-independent catalogs**: Optional version column allows describing recipes independently of specific package versions
4. **Composable**: Multiple marketplace CSVs can be combined by merging rows
5. **Round-trip compatible**: Reader and writer preserve all information
6. **Familiar pattern**: Mirrors `moderne-organizations-format` conventions, reducing learning curve
7. **Extensible bundle resolution**: Two-phase resolution (RecipeBundleResolver → RecipeBundleReader) allows new package ecosystems to be added without modifying core modules
8. **Quality assurance**: Validators ensure content quality (formatting) and completeness (CSV ↔ JAR synchronization)
9. **Lazy resolution**: RecipeListing stores bundle metadata but only resolves to actual Recipe instances when needed via resolve()

### Negative

1. **CSV limitations**: No native support for nested structures (mitigated by JSON columns and column naming conventions)
2. **JSON in CSV cells**: Options and dataTables are stored as JSON strings, which can be harder to edit manually in spreadsheet tools
3. **Always requires bundle metadata**: Unlike earlier designs, ecosystem and packageName are always required, even for basic recipe catalogs

### Trade-offs

- **Left-to-right category ordering** (left = deepest): This matches the `moderne-organizations-format` convention but may be counterintuitive to some users who expect left-to-right to represent root-to-leaf
- **Two-phase resolution**: Separating RecipeBundleResolver and RecipeBundleReader provides flexibility but adds complexity compared to a single interface
- **JSON for complex data**: Using JSON for options and dataTables provides a fixed column structure but requires JSON parsing/writing
- **Dynamic category columns**: The number of category columns varies between files based on hierarchy depth
- **Resolver configuration**: RecipeBundleResolvers must be registered with the RecipeMarketplace instance before calling RecipeListing.resolve(), describe(), or prepare()
- **Requested vs resolved version**: Supporting both `requestedVersion` (constraint) and `version` (resolved) adds flexibility but also complexity in version handling

## Examples

### Basic Recipe Catalog (No Version)

```csv
ecosystem,packageName,name,displayName,description,category1
maven,org.openrewrite:rewrite-java,org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses.,Java Cleanup
```

### With Version Information

```csv
ecosystem,packageName,version,name,displayName,description,category1
maven,org.openrewrite:rewrite-java,8.0.0,org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses.,Java Cleanup
```

### With Requested Version

```csv
ecosystem,packageName,requestedVersion,version,name,displayName,description,category1
maven,org.openrewrite:rewrite-java,LATEST,8.45.0,org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses.,Java Cleanup
```

### With Recipe Options (JSON Format)

```csv
ecosystem,packageName,name,displayName,description,category1,options
maven,org.openrewrite:rewrite-maven,org.openrewrite.maven.UpgradeDependencyVersion,Upgrade Dependency,Upgrades a Maven dependency.,Maven,"[{""name"":""groupId"",""displayName"":""Group ID"",""description"":""The group ID.""},{""name"":""artifactId"",""displayName"":""Artifact ID"",""description"":""The artifact ID.""}]"
```

### With Recipe Count

```csv
ecosystem,packageName,name,displayName,description,recipeCount,category1
maven,org.openrewrite:rewrite-java,org.openrewrite.java.format.Autoformat,Autoformat,Formats Java source code.,15,Java Formatting
```

### With Team Partitioning

```csv
ecosystem,packageName,version,name,displayName,description,category1,team
maven,org.openrewrite:rewrite-java,8.0.0,org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses.,Java Cleanup,java-team
```

### Multi-level Categories

```csv
ecosystem,packageName,name,category1,category2,category3
maven,org.openrewrite:rewrite-java,org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices
maven,org.openrewrite:rewrite-java,org.openrewrite.java.format.AutoFormat,Formatting,Java,Best Practices
```

Creates: `Best Practices > Java > Cleanup > UnnecessaryParentheses` and `Best Practices > Java > Formatting > AutoFormat`

### YAML Ecosystem Example

```csv
ecosystem,packageName,name,displayName,description
yaml,/path/to/recipes.yml,org.example.MyRecipe,My Custom Recipe,A custom recipe from a YAML file.
```
