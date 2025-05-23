package io.javaoperatorsdk.operator.glue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.javaoperatorsdk.operator.glue.customresource.TestCustomResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.DependentResourceSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.reconciler.ValidationAndStatusHandler;
import io.quarkus.test.junit.QuarkusTest;

import static io.javaoperatorsdk.operator.glue.TestUtils.INITIAL_RECONCILE_WAIT_TIMEOUT;
import static io.javaoperatorsdk.operator.glue.TestUtils.loadGlue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
class GlueTest extends TestBase {

  public static final String CHANGED_VALUE = "changed_value";

  @SuppressWarnings("unchecked")
  @Test
  void simpleTemplating() {
    Glue glue =
        TestUtils.loadGlue("/glue/Templating.yaml");
    glue = create(glue);

    await().timeout(Duration.ofHours(1)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    ((Map<String, String>) glue.getSpec().getChildResources().get(0).getResource()
        .getAdditionalProperties().get("data"))
        .put("key", CHANGED_VALUE);

    update(glue);

    await().untilAsserted(() -> {
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo(CHANGED_VALUE);
    });

    delete(glue);

    await().timeout(GC_TIMEOUT).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }


  @Test
  void crossReferenceResource() {
    Glue glue =
        TestUtils.loadGlue("/glue/CrossReferenceResource.yaml");
    glue = create(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm-1");
      var cm2 = get(ConfigMap.class, "cm-2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData()).containsEntry("valueFromCM1", "value1");
    });

    var resourceTemplate =
        glue.getSpec().getChildResources().stream().filter(r -> r.getName().equals("configMap1"))
            .findAny().orElseThrow().getResource();
    // set new value
    ((Map<String, String>) resourceTemplate.getAdditionalProperties().get("data")).put("key",
        "value2");
    glue = update(glue);

    await().untilAsserted(() -> {
      var cm2 = get(ConfigMap.class, "cm-2");
      assertThat(cm2.getData()).containsEntry("valueFromCM1", "value2");
    });

    delete(glue);

    await().timeout(GC_TIMEOUT).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm-1");
      var cm2 = get(ConfigMap.class, "cm-2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  void javaScriptCondition() {
    Glue glue =
        TestUtils.loadGlue("/glue/TwoResourcesAndCondition.yaml");
    create(glue);

    await().pollDelay(Duration.ofMillis(150)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
    });

    Map<String, String> map = (Map<String, String>) glue.getSpec().getChildResources()
        .get(0).getResource().getAdditionalProperties().get("data");
    map.put("createOther", "true");

    update(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    delete(glue);

    await().timeout(GC_TIMEOUT).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void stringTemplate() {
    Glue glue = create(TestUtils.loadGlue("/glue/ResourceTemplate.yaml"));

    await().timeout(Duration.ofSeconds(120)).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();

      assertThat(cm2.getData().get("valueFromCM1")).isEqualTo("value1");
    });

    delete(glue);

    await().timeout(GC_TIMEOUT).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "templconfigmap1");
      var cm2 = get(ConfigMap.class, "templconfigmap2");
      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void simpleConcurrencyTest() {
    int num = 10;
    List<Glue> glueList = testWorkflowList(num);

    glueList.forEach(this::create);

    await().untilAsserted(() -> IntStream.range(0, num).forEach(index -> {

      var w = get(Glue.class, "concurrencysample" + index);
      assertThat(w).isNotNull();
      var cm1 = get(ConfigMap.class, "concurrencysample" + index + "-1");
      var cm2 = get(ConfigMap.class, "concurrencysample" + index + "-2");

      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
      assertThat(cm2.getData().get("valueFromCM1"))
          .isEqualTo("value1");
    }));

    glueList.forEach(this::delete);
    await().timeout(GC_TIMEOUT).untilAsserted(() -> IntStream.range(0, num).forEach(index -> {
      var w = get(Glue.class, "concurrencysample" + index);
      assertThat(w).isNull();
    }));
  }

  @Test
  void changingWorkflow() {
    Glue glue = create(TestUtils.loadGlue("/glue/ChanginResources.yaml"));

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNotNull();
    });

    glue.getSpec().getChildResources().remove(1);
    glue.getSpec().getChildResources().add(new DependentResourceSpec()
        .setName("secret")
        .setResource(TestUtils.load("/Secret.yaml")));

    update(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");
      var s = get(Secret.class, "secret1");
      assertThat(cm1).isNotNull();
      assertThat(cm2).isNull();
      assertThat(s).isNotNull();
    });

    glue.getMetadata().setResourceVersion(null);
    delete(glue);

    await().timeout(GC_TIMEOUT).untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "configmap1");
      var s = get(Secret.class, "secret1");
      assertThat(cm1).isNull();
      assertThat(s).isNull();
    });
  }

  @Test
  void nonUniqueNameResultsInErrorMessageOnStatus() {
    Glue glue = create(TestUtils.loadGlue("/glue/NonUniqueName.yaml"));

    await().untilAsserted(() -> {
      var actualGlue = get(Glue.class, glue.getMetadata().getName());

      assertThat(actualGlue.getStatus()).isNotNull();
      Assertions.assertThat(actualGlue.getStatus().getErrorMessage())
          .startsWith(ValidationAndStatusHandler.NON_UNIQUE_NAMES_FOUND_PREFIX);
    });
  }

  @Test
  void childInDifferentNamespace() {
    Glue glue = create(TestUtils.loadGlue("/glue/ResourceInDifferentNamespace.yaml"));

    await().untilAsserted(() -> {
      var cmDifferentNS = client.configMaps().inNamespace("default")
          .withName("configmap1");
      var cm2 = get(ConfigMap.class, "configmap2");

      assertThat(cmDifferentNS).isNotNull();
      assertThat(cm2).isNotNull();
    });

    delete(glue);
    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var cmDifferentNS = client.configMaps().inNamespace("default")
          .withName("configmap1").get();
      var cm2 = get(ConfigMap.class, "configmap2");

      assertThat(cmDifferentNS).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void clusterScopedChild() {
    var glue = create(TestUtils.loadGlue("/glue/ClusterScopedChild.yaml"));
    await().untilAsserted(() -> {
      var ns = client.namespaces()
          .withName("testnamespace");
      assertThat(ns).isNotNull();
    });

    delete(glue);
    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var ns = client.namespaces()
          .withName("testnamespace").get();
      assertThat(ns).isNull();
    });
  }

  @Test
  void relatedResourceFromDifferentNamespace() {
    client.resource(new ConfigMapBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("related-configmap")
            .withNamespace("default")
            .build())
        .withData(Map.of("key1", "value1"))
        .build()).createOr(NonDeletingOperation::update);

    var glue = create(TestUtils.loadGlue("/glue/RelatedResourceFromDifferentNamespace.yaml"));

    await().untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap1");
      assertThat(cm).isNotNull();
      assertThat(cm.getData()).containsEntry("copy-key", "value1");
    });

    delete(glue);
    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap1");
      assertThat(cm).isNull();
    });
  }

  @Test
  void clusterScopedRelatedResource() {
    var glue = create(TestUtils.loadGlue("/glue/ClusterScopedRelatedResource.yaml"));

    await().untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap1");
      assertThat(cm).isNotNull();
      assertThat(cm.getData()).containsEntry("phase", "Active");
    });

    delete(glue);
    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap1");
      assertThat(cm).isNull();
    });
  }

  @ParameterizedTest
  @ValueSource(strings = {"PatchRelatedStatus.yaml", "PatchRelatedStatusWithTemplate.yaml"})
  void pathRelatedResourceStatus(String glueFileName) {
    TestUtils.applyTestCrd(client, TestCustomResource.class);

    var customResource = create(TestData.testCustomResource());
    var glue = createGlue("/glue/" + glueFileName);

    await().untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap1");
      assertThat(cm).isNotNull();
      var cr = get(TestCustomResource.class, "testcr1");
      assertThat(cr.getStatus()).isNotNull();
      assertThat(cr.getStatus().getValue()).isEqualTo(cm.getMetadata().getResourceVersion());
    });
    delete(glue);
    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap1");
      assertThat(cm).isNull();
    });
    delete(customResource);
    await().untilAsserted(() -> {
      var cr = get(TestCustomResource.class, "testcr1");
      assertThat(cr).isNull();
    });
  }

  @Test
  void customizeMatcher() {
    var glue = createGlue("/glue/SimpleNotUseSSA.yaml");

    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      assertThat(get(ConfigMap.class, "simple-glue-no-ssa-configmap")).isNotNull();
    });
    delete(glue);
    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      assertThat(get(ConfigMap.class, "simple-glue-no-ssa-configmap")).isNull();
    });
  }

  @Test
  void simpleBulk() {
    var glue = createGlue("/glue/" + "SimpleBulk.yaml");

    await().untilAsserted(() -> {
      var configMaps = getRelatedList(ConfigMap.class, glue.getMetadata().getName());
      assertThat(configMaps).hasSize(3);
      assertThat(configMaps)
          .allMatch(cm -> cm.getMetadata().getName().startsWith("simple-glue-configmap-"));
    });

    delete(glue);
    await().untilAsserted(
        () -> assertThat(getRelatedList(ConfigMap.class, glue.getMetadata().getName()).isEmpty()));
  }

  @Test
  void invalidGlueMessageHandling() {
    var glueName = "invalid-glue";
    var glue = createGlue("/glue/Invalid.yaml");

    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      var g = get(Glue.class, glueName);
      assertThat(g.getStatus()).isNotNull();
      assertThat(g.getStatus().getErrorMessage()).isNotNull();
    });

    // fix error
    glue.getSpec().getChildResources().get(1).setName("configMap2");
    glue.getMetadata().setResourceVersion(null);
    update(glue);

    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      var g = get(Glue.class, glueName);
      assertThat(g.getStatus().getErrorMessage()).isNull();
    });

    delete(glue);
    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      var g = get(Glue.class, glueName);
      assertThat(g).isNull();
    });
  }

  @Test
  void secretToConfigMapCopy() {
    String nsa = "namespace-a";
    String nsb = "namespace-b";

    createNamespace(nsa);
    createNamespace(nsb);
    createSecretToCopy(nsb);

    var glue = client.resource(loadGlue("/glue/CopySecretToConfigMap.yaml"))
        .createOr(NonDeletingOperation::update);

    await().untilAsserted(() -> {
      var cm = client.configMaps().inNamespace(nsa).withName("my-secret-copy").get();
      assertThat(cm).isNotNull();
      assertThat(cm.getData())
          .containsExactlyInAnyOrderEntriesOf(Map.of("key1", "value1", "key2", "value2"));
    });

    deleteInOwnNamespace(glue);
    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      var g = get(Glue.class, "secret-to-configmap", nsa);
      assertThat(g).isNull();
    });
  }

  private Secret createSecretToCopy(String nsb) {
    var secret = new Secret();
    secret.setMetadata(new ObjectMetaBuilder()
        .withName("secret-to-copy")
        .withNamespace(nsb)
        .build());
    secret.setData(Map.of("key1",
        Base64.getEncoder().encodeToString("value1".getBytes(StandardCharsets.UTF_8)),
        "key2",
        Base64.getEncoder().encodeToString("value2".getBytes(StandardCharsets.UTF_8))));

    return client.resource(secret).createOr(NonDeletingOperation::update);
  }

  private List<Glue> testWorkflowList(int num) {
    List<Glue> res = new ArrayList<>();
    IntStream.range(0, num).forEach(index -> {
      Glue w =
          TestUtils.loadGlue("/glue/TemplateForConcurrency.yaml");
      w.getMetadata().setName(w.getMetadata().getName() + index);
      res.add(w);
    });
    return res;
  }
}
