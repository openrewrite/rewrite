import org.junit.jupiter.api.Test
import org.openrewrite.yaml.MergeYaml
import org.openrewrite.yaml.YamlRecipeTest

class MergeYamlTest : YamlRecipeTest {

    @Test
    fun mustMergeYamlWhenBlockExists() = assertChanged(
        recipe = MergeYaml(
            "/spec/lifecycleRule",
            """
            lifecycleRule:
                - condition:
                    age: 7
            """.trimIndent()
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 1
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 7
        """,
        cycles = 2
    )

    @Test
    fun mustMergeYamlWhenBlockDoesntExist() = assertChanged(
        recipe = MergeYaml(
            "/spec/lifecycleRule",
            """
              lifecycleRule:
                  - action:
                        type: Delete
                    condition:
                        age: 7
            """.trimIndent()
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
                lifecycleRule:
                    - action:
                          type: Delete
                      condition:
                          age: 7
        """,
        cycles = 2
    )

    @Test
    fun mustMergeYamlForScalar() = assertChanged(
        recipe = MergeYaml(
            "/spec/bucketPolicyOnly",
            """
              bucketPolicyOnly: true
            """.trimIndent()
        ),
        before = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: false
        """,
        after = """
            apiVersion: storage.cnrm.cloud.google.com/v1beta1
            kind: StorageBucket
            spec:
                bucketPolicyOnly: true
        """,
        cycles = 2
    )

}