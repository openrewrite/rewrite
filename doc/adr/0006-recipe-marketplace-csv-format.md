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
- **`estimatedEffortPerOccurrence`**: ISO-8601 duration format (e.g., PT5M, PT1H)
- **`category1, category2, ..., categoryN`**: Zero or more category columns, read **left to right** with **left representing the deepest level category**
- **`option1Name, option1DisplayName, option1Description`**: First recipe option
- **`option2Name, option2DisplayName, option2Description`**: Second recipe option
- **`optionNName, optionNDisplayName, optionNDescription`**: Additional options following the same pattern

### Optional Bundle Columns

- **`version`**: Package version (optional, allows version-independent recipe catalogs)
- **`team`**: Optional team identifier for marketplace partitioning

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
  - `RecipeBundle`: Simple data class containing ecosystem, packageName, version (optional), and team (optional)
  - `RecipeListing`: Represents a recipe with metadata (name, displayName, description, options) and an associated `RecipeBundle`
  - `RecipeMarketplace`: Hierarchical structure with nested `Category` instances and a list of `RecipeBundleResolver` instances

- **Reader**: `RecipeMarketplaceReader` (using univocity-parsers)
  - Parses CSV into `RecipeMarketplace` hierarchies
  - Creates `RecipeListing` instances with `RecipeBundle` objects from CSV data
  - Requires `ecosystem` and `packageName` columns; `version` and `team` are optional
  - Dynamically detects category and option columns

- **Writer**: `RecipeMarketplaceWriter` (using univocity-parsers)
  - Dynamically determines required category and option columns based on marketplace content
  - Always includes `ecosystem` and `packageName` columns
  - Only includes `version` column if at least one recipe has version information
  - Only includes `team` column if at least one recipe has team information

- **Bundle Resolution**: Two-phase resolution system
  - `RecipeBundleResolver`: Interface with `getEcosystem()` and `resolve(RecipeBundle)` methods
    - Ecosystem-specific resolvers are registered with the `RecipeMarketplace`
    - Examples: `MavenRecipeBundleResolver` in `rewrite-maven`, `NpmRecipeBundleResolver` in `rewrite-javascript`
  - `RecipeBundleReader`: Interface returned by resolvers with methods:
    - `getBundle()`: Returns the associated `RecipeBundle`
    - `read()`: Reads the bundle and returns a `RecipeMarketplace`
    - `describe(RecipeListing)`: Returns a `RecipeDescriptor` for a listing
    - `prepare(RecipeListing, Map<String, Object>)`: Creates a configured `Recipe` instance
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

1. **CSV limitations**: No native support for nested structures (mitigated by column naming conventions)
2. **Sparse data**: Recipes with few options result in many empty cells in CSVs with high option counts
3. **Always requires bundle metadata**: Unlike earlier designs, ecosystem and packageName are always required, even for basic recipe catalogs

### Trade-offs

- **Left-to-right category ordering** (left = deepest): This matches the `moderne-organizations-format` convention but may be counterintuitive to some users who expect left-to-right to represent root-to-leaf
- **Two-phase resolution**: Separating RecipeBundleResolver and RecipeBundleReader provides flexibility but adds complexity compared to a single interface
- **Dynamic columns**: Provides flexibility but means schema varies between files, making generic CSV processing tools less effective
- **Resolver configuration**: RecipeBundleResolvers must be registered with the RecipeMarketplace instance before calling RecipeListing.resolve(), describe(), or prepare()

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

### With Recipe Options

```csv
ecosystem,packageName,name,displayName,description,category1,option1Name,option1DisplayName,option1Description,option2Name,option2DisplayName,option2Description
maven,org.openrewrite:rewrite-maven,org.openrewrite.maven.UpgradeDependencyVersion,Upgrade Dependency,Upgrades a Maven dependency.,Maven,groupId,Group ID,The group ID.,artifactId,Artifact ID,The artifact ID.
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
