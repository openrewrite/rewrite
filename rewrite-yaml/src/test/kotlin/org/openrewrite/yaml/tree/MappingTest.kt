/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml.tree

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.yaml.YamlParser
import org.openrewrite.yaml.tree.Yaml.Scalar

@Suppress("YAMLUnusedAnchor")
class MappingTest: YamlParserTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/423")
    @Test
    fun emptyObject() = assertRoundTrip("workflow_dispatch: {}")

    @Issue("https://github.com/openrewrite/rewrite/issues/423")
    @Test
    fun flowStyleMapping() = assertRoundTrip(
        source = """
          {
            "data": {
              "prometheus.yml": "global:\n  scrape_interval: 10s\n  scrape_timeout: 9s"
            }
          }
        """
    )

    @Test
    fun multipleEntries() = assertRoundTrip(
            source = """
                type : specs.openrewrite.org/v1beta/visitor # comment with colon :
                name : org.openrewrite.text.ChangeTextToJon
            """,
            afterConditions = { y ->
                Assertions.assertThat((y.documents[0].block as Yaml.Mapping).entries.map { (it.key as Scalar).value })
                        .containsExactly("type", "name")
            }
    )

    @Test
    fun deep() = assertRoundTrip(
            source = """
                type:
                    name: org.openrewrite.text.ChangeTextToJon
            """,
            afterConditions = { y ->
                val mapping = y.documents[0].block as Yaml.Mapping
                Assertions.assertThat(mapping.entries.map { (it.key as Scalar).value }).containsExactly("type")
                Assertions.assertThat(mapping.entries[0].value).isInstanceOf(Yaml.Mapping::class.java)
            }
    )

    @Test
    fun largeScalar() = assertRoundTrip(
            source = """
                spring:
                  cloud:
                    config:
                      server:
                        composite:
                          - type: git
                            uri: git@gitserver.com:team/repo1.git
                            ignoreLocalSshSettings: true
                            greater-than-block-text-password: >
                              {cipher}d1b2458ccede07c856ff952bd841638ff4dd12ed1d36812663c3c7262d57bf46
                            privateKey: "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEAoqyz6YaYMTr7L8GLPSQpAQXaM04gRx4CCsGK2kfLQdw4BlqI\nyyxp38YcuZG9cUDBAxby+K2TKmwHaC1R61QTwbPuCRdIPrDwRz+FLoegm3iDLCmn\nuP6rjZDneYsqfU1KSdrOwIbCnONfDdvYL/vnZC/o8DDMlk5Orw2SfHkT3pq0o8km\nayBwN4Sf3bpyWTY0oZcmNeSCCoIdE59k8Pa7/t9bwY9caLj05C3DEsjucc7Ei/Eq\nTOyGyobtXwaya5CqKLUHes74Poz1aEP/yVFdUud91uezd8ZK1P1t5/ZKA3R6aHir\n+diDJ2/GQ2tD511FW46yw+EtBUJTO6ADVv4UnQIDAQABAoIBAF+5qwEfX82QfKFk\njfADqFFexUDtl1biFKeJrpC2MKhn01wByH9uejrhFKQqW8UaKroLthyZ34DWIyGt\nlDnHGv0gSVF2LuAdNLdobJGt49e4+c9yD61vxzm97Eh8mRs08SM2q/VlF35E2fmI\nxdWusUImYzd8L9e+6tRd8zZl9UhG5vR5XIstKqxC6S0g79aAt0hasE4Gw1FKOf2V\n4mlL15atjQSKCPdOicuyc4zpjAtU1A9AfF51iG8oOUuJebPW8tCftfOQxaeGFgMG\n7M9aai1KzXR6M5IBAKEv31yBvz/SHTneP7oZXNLeC1GIR420PKybmeZdNK8BbEAu\n3reKgm0CgYEA03Sx8JoF5UBsIvFPpP1fjSlTgKryM5EJR6KQtj5e4YfyxccJepN8\nq4MrqDfNKleG/a1acEtDMhBNovU7Usp2QIP7zpAeioHBOhmE5WSieZGc3icOGWWq\nmRkdulSONruqWKv76ZoluxftekE03bDhZDNlcCgmrslEKB/ufHd2oc8CgYEAxPFa\nlKOdSeiYFV5CtvO8Ro8em6rGpSsVz4qkPxbeBqUDCb9KXHhq6YrhRxOIfQJKfT7M\nZFCn8ArJXKgOGu+KsvwIErFHF9g2jJMG4DOUTpkQgi2yveihFxcmz/AltyVXgrnv\nZWQbAerH77pdKKhNivLGgEv72GYawdYjYNjemdMCgYA2kEMmMahZyrDcp2YEzfit\nBT/t0K6kzcUWPgWXcSqsiZcEn+J7RbmCzFskkhmX1nQX23adyV3yejB+X0dKisHO\nzf/ZAmlPFkJVCqa3RquCMSfIT02dEhXeYZPBM/Zqeyxuqxpa4hLgX0FBLbhFiFHw\nuC5xrXql2XuD2xF//peXEwKBgQC+pa28Cg7vRxxCQzduB9CQtWc55j3aEjVQ7bNF\n54sS/5ZLT0Ra8677WZfuyDfuW9NkHvCZg4Ku2qJG8eCFrrGjxlrCTZ62tHVJ6+JS\nE1xUIdRbUIWhVZrr0VufG6hG/P0T7Y6Tpi6G0pKtvMkF3LcD9TS3adboix8H2ZXx\n4L7MRQKBgQC0OO3qqNXOjIVYWOoqXLybOY/Wqu9lxCAgGyCYaMcstnBI7W0MZTBr\n/syluvGsaFc1sE7MMGOOzKi1tF4YvDmSnzA/R1nmaPguuD9fOA+w7Pwkv5vLvuJq\n2U7EeNwxq1I1L3Ag6E7wH4BHLHd4TKaZR6agFkn8oomz71yZPGjuZQ==\n-----END RSA PRIVATE KEY-----"
                            repos:
                              repo1:
                                uri: git@gitserver.com:team/repo2.git
                                hostKey: someHostKey
                                hostKeyAlgorithm: ssh-rsa
                                privateKey: |
                                  -----BEGIN RSA PRIVATE KEY-----
                                  MIIEpgIBAAKCAQEAx4UbaDzY5xjW6hc9jwN0mX33XpTDVW9WqHp5AKaRbtAC3DqX
                                  IXFMPgw3K45jxRb93f8tv9vL3rD9CUG1Gv4FM+o7ds7FRES5RTjv2RT/JVNJCoqF
                                  ol8+ngLqRZCyBtQN7zYByWMRirPGoDUqdPYrj2yq+ObBBNhg5N+hOwKjjpzdj2Ud
                                  1l7R+wxIqmJo1IYyy16xS8WsjyQuyC0lL456qkd5BDZ0Ag8j2X9H9D5220Ln7s9i
                                  oezTipXipS7p7Jekf3Ywx6abJwOmB0rX79dV4qiNcGgzATnG1PkXxqt76VhcGa0W
                                  DDVHEEYGbSQ6hIGSh0I7BQun0aLRZojfE3gqHQIDAQABAoIBAQCZmGrk8BK6tXCd
                                  fY6yTiKxFzwb38IQP0ojIUWNrq0+9Xt+NsypviLHkXfXXCKKU4zUHeIGVRq5MN9b
                                  BO56/RrcQHHOoJdUWuOV2qMqJvPUtC0CpGkD+valhfD75MxoXU7s3FK7yjxy3rsG
                                  EmfA6tHV8/4a5umo5TqSd2YTm5B19AhRqiuUVI1wTB41DjULUGiMYrnYrhzQlVvj
                                  5MjnKTlYu3V8PoYDfv1GmxPPh6vlpafXEeEYN8VB97e5x3DGHjZ5UrurAmTLTdO8
                                  +AahyoKsIY612TkkQthJlt7FJAwnCGMgY6podzzvzICLFmmTXYiZ/28I4BX/mOSe
                                  pZVnfRixAoGBAO6Uiwt40/PKs53mCEWngslSCsh9oGAaLTf/XdvMns5VmuyyAyKG
                                  ti8Ol5wqBMi4GIUzjbgUvSUt+IowIrG3f5tN85wpjQ1UGVcpTnl5Qo9xaS1PFScQ
                                  xrtWZ9eNj2TsIAMp/svJsyGG3OibxfnuAIpSXNQiJPwRlW3irzpGgVx/AoGBANYW
                                  dnhshUcEHMJi3aXwR12OTDnaLoanVGLwLnkqLSYUZA7ZegpKq90UAuBdcEfgdpyi
                                  PhKpeaeIiAaNnFo8m9aoTKr+7I6/uMTlwrVnfrsVTZv3orxjwQV20YIBCVRKD1uX
                                  VhE0ozPZxwwKSPAFocpyWpGHGreGF1AIYBE9UBtjAoGBAI8bfPgJpyFyMiGBjO6z
                                  FwlJc/xlFqDusrcHL7abW5qq0L4v3R+FrJw3ZYufzLTVcKfdj6GelwJJO+8wBm+R
                                  gTKYJItEhT48duLIfTDyIpHGVm9+I1MGhh5zKuCqIhxIYr9jHloBB7kRm0rPvYY4
                                  VAykcNgyDvtAVODP+4m6JvhjAoGBALbtTqErKN47V0+JJpapLnF0KxGrqeGIjIRV
                                  cYA6V4WYGr7NeIfesecfOC356PyhgPfpcVyEztwlvwTKb3RzIT1TZN8fH4YBr6Ee
                                  KTbTjefRFhVUjQqnucAvfGi29f+9oE3Ei9f7wA+H35ocF6JvTYUsHNMIO/3gZ38N
                                  CPjyCMa9AoGBAMhsITNe3QcbsXAbdUR00dDsIFVROzyFJ2m40i4KCRM35bC/BIBs
                                  q0TY3we+ERB40U8Z2BvU61QuwaunJ2+uGadHo58VSVdggqAo0BSkH58innKKt96J
                                  69pcVH/4rmLbXdcmNYGm6iu+MlPQk4BUZknHSmVHIFdJ0EPupVaQ8RHT
                                  -----END RSA PRIVATE KEY-----
            """
    )

    @Test
    fun mappingContainingSequence() {
        val yText = """
            foo:
              - bar: qwer
                asdf: hjkl
        """.trimIndent()
        val y = YamlParser().parse(yText)[0]
        Assertions.assertThat(y.printAll()).isEqualTo(yText)
    }

    @Test
    fun commentWithColon() = assertRoundTrip(
            source = """
                for: bar
                # Comment with a colon:
                baz: foo
            """,
            afterConditions = { documents ->
                val doc = documents.documents.first()
                val mapping = doc.block as Yaml.Mapping
                Assertions.assertThat(mapping.entries.size).isEqualTo(2)
                val bazFooEntry = mapping.entries[1]
                Assertions.assertThat(bazFooEntry.prefix).isEqualTo("\n# Comment with a colon:\n")
            }
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1469")
    @Test
    fun emptyDocument() = assertRoundTrip(
        source = """""",
        afterConditions = { documents ->
            Assertions.assertThat(documents.documents.isEmpty())
        }
    )

    @Test
    fun multiDocOnlyComments() = assertRoundTrip(
        source = """
            # doc-1-pre
            ---
            # doc-1-end
            ...
            ---
            # doc-2-pre
        """,
        afterConditions = { docs ->
            Assertions.assertThat(docs.documents.size).isEqualTo(2)
            val doc = docs.documents.first()
            Assertions.assertThat(doc.prefix).isEqualTo("# doc-1-pre\n")
            Assertions.assertThat(doc.end.prefix).isEqualTo("\n# doc-1-end\n")
            val doc2 = docs.documents[1]
            Assertions.assertThat(doc2.end.prefix).isEqualTo("\n# doc-2-pre")
        }
    )

    @Test
    fun singleDocOnlyComments() = assertRoundTrip(
        source = """
            # doc-1-pre
        """,
        afterConditions = { docs ->
            Assertions.assertThat(docs.documents.size).isEqualTo(1)
            val doc = docs.documents.first()
            Assertions.assertThat(doc.prefix).isEqualTo("# doc-1-pre")
        }
    )

    @Issue("https://github.com/spring-projects/spring-boot/issues/8438")
    @Test
    fun valueStartsWithAt() = assertRoundTrip("""
          date: @build.timestamp@
          version: @project.version@
    """)


    @Test
    fun suffixBeforeColon() = assertRoundTrip("""
          data :
            test : 0
    """)

    @Test
    fun literals() = assertRoundTrip("""
          data:
            prometheus.yml: |-
              global:
                scrape_interval: 10s
                scrape_timeout: 9s
                evaluation_interval: 10s
    """)

    @Test
    fun scalarValue() = assertRoundTrip("""
        default: &default test
        stage: *default
    """)

    @Test
    fun scalarValueInBrackets() = assertRoundTrip("""
        defaults: [&first A, &stage test, &last Z]
        config: [first: *first, stage: *stage, last: *last]
    """)

    @Test
    fun scalarKeyAnchor() = assertRoundTrip("""
        foo:
          - start: start
          - &anchor buz: buz
          - *anchor: baz
          - end: end
    """)

    @Test
    fun scalarEntryValue() = assertRoundTrip("""
         foo:
          - start: start
          - buz: &anchor ooo
          - fuz: *anchor
          - end: end
    """)

    @Test
    fun aliasEntryKey() = assertRoundTrip("""
        bar:
          &abc yo: friend
        baz:
          *abc: friendly
    """)

    @Test
    fun scalarKeyAnchorInBrackets() = assertRoundTrip("""
        foo: [start: start, &anchor buz: buz, *anchor: baz, end: end]
    """)
    @Test
    fun scalarEntryValueAnchorInBrackets() = assertRoundTrip("""
        foo: [start: start, &anchor buz: buz, baz: *anchor, end: end]
    """)

    @Test
    fun sequenceAnchor() = assertRoundTrip("""
        defaults: &defaults
          - A: 1
          - B: 2
        key: *defaults
    """)

    @Test
    fun sequenceAnchorFlowStyle() = assertRoundTrip("""
        defaults: &defaults [A:1, B:2] # comment
        key: *defaults
    """)
    @Test
    fun sequenceAnchorWithComments() = assertRoundTrip("""
        defaults: &defaults # sequence start comment
          - A: 1 # A comment
          - B: 2 # B comment
        key: *defaults
    """)

    @Test
    fun sequenceAnchorInBrackets() = assertRoundTrip("""
      defaults: &defaults [A: 1, B: 2]
      mapping: *defaults
    """)

    @Test
    fun mappingAnchor() = assertRoundTrip("""
        defaults: &defaults
          A: 1
          B: 2
        mapping:
          << : *defaults
          A: 23
          C: 99
    """)

    @Disabled
    @Test
    fun mappingKey() = assertRoundTrip("""
      ? 
        - key-1
        - key-2
      : 
        - value
    """)
}
