package io.javaoperatorsdk.operator.glue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.glue.customresource.TestCustomResource;
import io.javaoperatorsdk.operator.glue.customresource.TestCustomResource2;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.reconciler.glue.GlueReconciler;
import io.javaoperatorsdk.operator.glue.reconciler.operator.GlueOperatorReconciler;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static io.javaoperatorsdk.operator.glue.TestUtils.INITIAL_RECONCILE_WAIT_TIMEOUT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(GlueOperatorLabelSelectorTest.GlueOperatorLabelSelectorTestProfile.class)
public class GlueOperatorLabelSelectorTest extends TestBase {

  public static final String GLUE_OPERATOR_LABEL_KEY = "test-glue-operator";
  public static final String GLUE_LABEL_KEY = "test-glue";
  public static final String LABEL_VALUE = "true";

  @BeforeEach
  void applyCRD() {
    TestUtils.applyTestCrd(client, TestCustomResource.class, TestCustomResource2.class);
  }

  @Test
  void testGlueOperatorLabelSelector() {
    var go = create(TestUtils
        .loadGlueOperator("/glueoperator/SimpleGlueOperator.yaml"));

    var testCR = create(TestData.testCustomResource());

    await().pollDelay(INITIAL_RECONCILE_WAIT_TIMEOUT).untilAsserted(() -> {
      assertThat(get(ConfigMap.class, testCR.getMetadata().getName())).isNull();
    });

    go.getMetadata().getLabels().put(GLUE_OPERATOR_LABEL_KEY, LABEL_VALUE);
    update(go);

    await().untilAsserted(() -> {
      assertThat(get(ConfigMap.class, testCR.getMetadata().getName())).isNotNull();
      var glue = get(Glue.class, GlueOperatorReconciler.glueName(testCR.getMetadata().getName(),
          testCR.getKind()));
      assertThat(glue).isNotNull();
      assertThat(glue.getMetadata().getLabels()).containsEntry(GLUE_LABEL_KEY, LABEL_VALUE);
    });

    // explicit cleanup, so there are no exception in the logs because of namespace marked for
    // deletion
    delete(testCR);
    await().untilAsserted(() -> {
      var glue = get(Glue.class, GlueOperatorReconciler.glueName(testCR.getMetadata().getName(),
          testCR.getKind()));
      assertThat(glue).isNull();
    });
  }

  public static class GlueOperatorLabelSelectorTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of(
          "quarkus.operator-sdk.controllers." + GlueReconciler.GLUE_RECONCILER_NAME + ".selector",
          GLUE_LABEL_KEY + "=" + LABEL_VALUE,
          "quarkus.operator-sdk.controllers." + GlueOperatorReconciler.GLUE_OPERATOR_RECONCILER_NAME
              + ".selector",
          GLUE_OPERATOR_LABEL_KEY + "=" + LABEL_VALUE);
    }
  }

}
