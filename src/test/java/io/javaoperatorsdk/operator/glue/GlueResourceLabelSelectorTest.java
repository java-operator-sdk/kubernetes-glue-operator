package io.javaoperatorsdk.operator.glue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
@TestProfile(GlueResourceLabelSelectorTest.GlueResourceLabelSelectorTestProfile.class)
public class GlueResourceLabelSelectorTest extends TestBase {

  @Test
  void showCreatingResourceWithLabelSelectorAndLabel() {
    var glue = create(TestUtils.loadGlue("/glue/SimpleGlueWithLabeledResource.yaml"));

    // this serves more like a sample, hard to test here if the label selector is on informer
    await().untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap");
      assertThat(cm).isNotNull();
      assertThat(cm.getMetadata().getLabels())
          .containsEntry("test-glue", "true");
    });

    delete(glue);
    await().untilAsserted(() -> {
      var cm = get(ConfigMap.class, "configmap");
      assertThat(cm).isNull();
    });
  }

  public static class GlueResourceLabelSelectorTestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
      return Map.of("glue.operator.resource-label-selector.v1#ConfigMap", "test-glue=true");
    }
  }
}
