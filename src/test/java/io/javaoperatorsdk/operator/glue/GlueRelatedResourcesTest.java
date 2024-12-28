package io.javaoperatorsdk.operator.glue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.*;
import io.javaoperatorsdk.operator.glue.customresource.ClusterScopeTestCustomResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
public class GlueRelatedResourcesTest extends TestBase {

  private static final String BASE64_VALUE_1 =
      Base64.getEncoder().encodeToString("val1".getBytes(StandardCharsets.UTF_8));
  private static final String BASE64_VALUE_2 =
      Base64.getEncoder().encodeToString("val2".getBytes(StandardCharsets.UTF_8));
  public static final String CONFIG_MAP_VALUE_1 = "val1";
  public static final String CONFIG_MAP_VALUE_2 = "val2";

  @BeforeEach
  void applyCRD() {
    TestUtils.applyTestCrd(client, ClusterScopeTestCustomResource.class);
  }

  @Test
  void simpleRelatedResourceUsage() {
    create(secret());
    Glue glue =
        TestUtils.loadGlue("/glue/RelatedResourceSimpleWithCondition.yaml");

    create(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", BASE64_VALUE_1);

      var cm2 = get(ConfigMap.class, "cm2");
      assertThat(cm2).isNotNull();
    });

    delete(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      var cm2 = get(ConfigMap.class, "cm2");

      assertThat(cm1).isNull();
      assertThat(cm2).isNull();
    });
  }

  @Test
  void multipleResourceNamesInRelated() {
    create(secret("test-secret1", BASE64_VALUE_1));
    create(secret("test-secret2", BASE64_VALUE_2));

    Glue glue =
        create(TestUtils.loadGlue("/glue/MultiNameRelatedResource.yaml"));

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key1", BASE64_VALUE_1);
      assertThat(cm1.getData()).containsEntry("key2", BASE64_VALUE_2);
    });

    delete(glue);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1).isNull();
    });
  }

  @Test
  void managedAndRelatedResourceOfSameTypeAndTriggering() {
    var relatedConfigMap = create(configMap());
    Glue glue =
        create(TestUtils.loadGlue("/glue/RelatesResourceSameType.yaml"));

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1).isNotNull();
      assertThat(cm1.getData()).containsEntry("key", CONFIG_MAP_VALUE_1);
    });

    // changes on the managed config map reverted
    var cm = get(ConfigMap.class, "cm1");
    cm.getData().put("key", CONFIG_MAP_VALUE_2);
    update(cm);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1.getData()).containsEntry("key", CONFIG_MAP_VALUE_1);
    });

    relatedConfigMap.getData().put("key", CONFIG_MAP_VALUE_2);
    update(relatedConfigMap);

    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1.getData()).containsEntry("key", CONFIG_MAP_VALUE_2);
    });

    delete(glue);
    await().untilAsserted(() -> {
      var cm1 = get(ConfigMap.class, "cm1");
      assertThat(cm1).isNull();
    });
  }

  Secret secret() {
    return secret("test-secret1", BASE64_VALUE_1);
  }

  Secret secret(String name, String val) {
    return new SecretBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName(name)
            .build())
        .withData(Map.of("key", val))
        .build();
  }

  ConfigMap configMap() {
    return new ConfigMapBuilder()
        .withMetadata(new ObjectMetaBuilder()
            .withName("related-cm1")
            .build())
        .withData(Map.of("key", CONFIG_MAP_VALUE_1))
        .build();
  }

}
