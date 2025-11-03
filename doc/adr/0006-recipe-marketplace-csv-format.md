# 6. Recipe Marketplace CSV Format

Date: 2025-01-27

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

- **`name`**: The fully qualified recipe name (e.g., `org.openrewrite.java.cleanup.UnnecessaryParentheses`)
- **`category1, category2, ..., categoryN`**: Zero or more category columns, read **left to right** with **left representing the deepest level category**

### Optional Recipe Columns

- **`displayName`**: Human-readable recipe display name
- **`description`**: Recipe description
- **`option1Name, option1DisplayName, option1Description`**: First recipe option
- **`option2Name, option2DisplayName, option2Description`**: Second recipe option
- **`optionNName, optionNDisplayName, optionNDescription`**: Additional options following the same pattern

### Optional Bundle Columns

Bundle columns describe where a recipe can be installed from. When absent, the marketplace represents a minimal catalog:

- **`ecosystem`**: Package ecosystem (e.g., `Maven`, `npm`, `yaml`)
- **`packageName`**: Package identifier (e.g., `org.openrewrite:rewrite-java`, npm package name)
- **`version`**: Package version
- **`team`**: Optional team identifier for marketplace partitioning

### Category Structure

The `RecipeMarketplace` is a recursive data structure where each category is itself a marketplace. Categories are determined by column headers starting with "category":

```csv
name,category1,category2,category3
org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices
```

This creates the hierarchy: `Best Practices > Java > Cleanup > UnnecessaryParentheses`

The displayName of a category corresponds to the value in its category column.

### Epsilon Root

When a CSV contains multiple top-level categories, a synthetic "epsilon root" (`ε`) is created similar to `moderne-organizations-format`. This root:

- Uses the epsilon character (`\u03B5`) as its display name
- Is identified via `RecipeMarketplace.isRoot()`
- Is never written to CSV output
- Allows the reader to return a single root when multiple disparate category trees exist

### Minimal vs. Enriched Marketplaces

**Minimal Marketplace**: Contains only recipe metadata (name, categories, options) without bundle information. Useful for describing what recipes exist and their categorization.

**Enriched Marketplace**: Includes bundle columns (ecosystem, packageName, version, team). Created when recipes are "installed" into an environment, combining the minimal catalog with actual bundle provenance.

### Implementation

- **Reader**: `RecipeMarketplaceReader` (using univocity-parsers)
  - Parses CSV into `RecipeMarketplace` hierarchies
  - Accepts optional `RecipeBundleLoader` instances via constructor
  - Creates bundle instances via loaders when ecosystem, packageName, and version are present
  - Creates `RecipeOffering` instances with null bundles when bundle columns are absent or no loader is configured
  - Returns epsilon root when multiple top-level categories exist

- **Writer**: `RecipeMarketplaceWriter` (using univocity-parsers)
  - Dynamically determines required category and option columns
  - Filters epsilon root from output
  - Only includes bundle columns if at least one recipe has bundle information

- **Bundle Loaders**: Configurable implementations passed to the reader
  - `MavenRecipeBundleLoader` in `rewrite-maven`: Creates `MavenRecipeBundle` instances, requires `MavenExecutionContextView` and `MavenArtifactDownloader`
  - `NpmRecipeBundleLoader` in `rewrite-javascript`: Creates `NpmRecipeBundle` instances
  - `RecipeBundleLoader` interface allows additional ecosystems to be added without modifying core

- **Generator**: `MavenRecipeMarketplaceGenerator` in `rewrite-maven`
  - Generates `RecipeMarketplace` from recipe JARs by scanning classpath and extracting recipe metadata
  - Automatically determines categories from recipe package names and `CategoryDescriptor` annotations
  - Creates bundle information from GAV coordinates
  - Distinguishes between YAML-based declarative recipes and Java class-based recipes
  - Useful for generating initial CSV files from existing recipe JARs

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
3. **Separation of concerns**: Minimal marketplaces can describe recipes independently of their installation source
4. **Composable**: Multiple marketplace CSVs can be combined by merging rows
5. **Round-trip compatible**: Reader and writer preserve all information
6. **Familiar pattern**: Mirrors `moderne-organizations-format` conventions, reducing learning curve
7. **Extensible bundle loading**: RecipeBundleLoader interface allows new package ecosystems to be added without modifying core modules
8. **Automated generation**: `MavenRecipeMarketplaceGenerator` can automatically create CSV files from recipe JARs
9. **Quality assurance**: Validators ensure content quality (formatting) and completeness (CSV ↔ JAR synchronization)

### Negative

1. **CSV limitations**: No native support for nested structures (mitigated by column naming conventions)
2. **Sparse data**: Recipes with few options result in many empty cells in CSVs with high option counts
3. **Manual maintenance**: Keeping bundle information synchronized with actual artifact versions requires tooling (though `MavenRecipeMarketplaceGenerator` and validators help address this)

### Trade-offs

- **Left-to-right category ordering** (left = deepest): This matches the `moderne-organizations-format` convention but may be counterintuitive to some users who expect left-to-right to represent root-to-leaf
- **Null bundles for minimal marketplaces**: Simplifies the model but means `RecipeOffering.describe()` and `prepare()` throw exceptions until bundles are associated
- **Dynamic columns**: Provides flexibility but means schema varies between files, making generic CSV processing tools less effective
- **Bundle loader configuration**: Bundle loaders require runtime dependencies (e.g., MavenExecutionContextView) that must be provided when constructing the loader and passed to RecipeMarketplaceReader constructor

## Examples

### Minimal Marketplace

```csv
name,displayName,description,category
org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Removes unnecessary parentheses,Java Cleanup
```

### With Options

```csv
name,displayName,option1Name,option1DisplayName,option1Description,option2Name,option2DisplayName,option2Description,category
org.openrewrite.maven.UpgradeDependencyVersion,Upgrade Dependency,groupId,Group ID,The group ID,artifactId,Artifact ID,The artifact ID,Maven
```

### Enriched with Bundle Information

```csv
name,displayName,category,ecosystem,packageName,version,team
org.openrewrite.java.cleanup.UnnecessaryParentheses,Remove Unnecessary Parentheses,Java Cleanup,Maven,org.openrewrite:rewrite-java,8.0.0,java-team
```

### Multi-level Categories

```csv
name,category1,category2,category3
org.openrewrite.java.cleanup.UnnecessaryParentheses,Cleanup,Java,Best Practices
org.openrewrite.java.format.AutoFormat,Formatting,Java,Best Practices
```

Creates: `Best Practices > Java > Cleanup > UnnecessaryParentheses` and `Best Practices > Java > Formatting > AutoFormat`
