package io.javaoperatorsdk.operator.glue.sample.webpage;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.javaoperatorsdk.operator.glue.TestUtils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class WebPageE2E {

  private KubernetesClient client = new KubernetesClientBuilder().build();

  @BeforeEach
  void applyCRDs() {
    TestUtils.applyCrd(client, Glue.class, GlueOperator.class);
    TestUtils.applyTestCrd(client, WebPage.class);
    TestUtils.applyAndWait(client, "target/kubernetes/kubernetes.yml");
  }

  @Test
  void testWebPageCRUDOperations() {
    client.resource(TestUtils.load("/sample/webpage/webpage.operator.yaml"))
        .createOr(NonDeletingOperation::update);
    var webPage = TestUtils.load("/sample/webpage/webpage.sample.yaml", WebPage.class);
    var createdWebPage = client.resource(webPage).createOr(NonDeletingOperation::update);

    await().untilAsserted(() -> {
      var deployment =
          client.resources(Deployment.class).withName(webPage.getMetadata().getName()).get();
      var configMap =
          client.resources(ConfigMap.class).withName(webPage.getMetadata().getName()).get();
      var service = client.resources(Service.class).withName(webPage.getMetadata().getName()).get();
      var ingress = client.resources(Ingress.class).withName(webPage.getMetadata().getName()).get();

      assertThat(deployment).isNotNull();
      assertThat(configMap).isNotNull();
      assertThat(service).isNotNull();
      assertThat(ingress).isNull();
    });

    createdWebPage.getMetadata().setResourceVersion(null);
    createdWebPage.getSpec().setExposed(true);
    createdWebPage = client.resource(createdWebPage).update();

    await().untilAsserted(() -> {
      var ingress = client.resources(Ingress.class).withName(webPage.getMetadata().getName()).get();
      assertThat(ingress).isNotNull();
    });

    client.resource(createdWebPage).delete();

    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var deployment =
          client.resources(Deployment.class).withName(webPage.getMetadata().getName()).get();
      var configMap =
          client.resources(ConfigMap.class).withName(webPage.getMetadata().getName()).get();
      var service = client.resources(Service.class).withName(webPage.getMetadata().getName()).get();
      var ingress = client.resources(Ingress.class).withName(webPage.getMetadata().getName()).get();

      assertThat(deployment).isNull();
      assertThat(configMap).isNull();
      assertThat(service).isNull();
      assertThat(ingress).isNull();
    });
  }

}
