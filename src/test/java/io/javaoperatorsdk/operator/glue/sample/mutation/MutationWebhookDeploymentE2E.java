package io.javaoperatorsdk.operator.glue.sample.mutation;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.javaoperatorsdk.operator.glue.TestUtils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class MutationWebhookDeploymentE2E {

  public static final String TEST_POD_NAME = "testpod";
  private final KubernetesClient client = new KubernetesClientBuilder().build();

  @BeforeEach
  void applyCRDs() throws MalformedURLException, URISyntaxException {
    TestUtils.applyCrd(client, Glue.class, GlueOperator.class);
    TestUtils.applyAndWait(client,
        new URI(
            "https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml")
            .toURL());
    TestUtils.applyAndWait(client, "target/kubernetes/kubernetes.yml");
    TestUtils.applyAndWait(client, "src/test/resources/sample/mutation/supporting.resource.yaml");
  }

  @Test
  void testMutationHookDeployment() {
    var glue = client.resource(TestUtils.load("/sample/mutation/mutation.glue.yaml"))
        .createOr(NonDeletingOperation::update);

    await().atMost(Duration.ofMinutes(5)).untilAsserted(() -> {
      var conf = client.admissionRegistration().v1().mutatingWebhookConfigurations()
          .withName("pod-mutating-webhook").get();
      var deployment = client.apps().deployments().withName("pod-mutating-hook").get();
      assertThat(conf).isNotNull();
      assertThat(deployment.getStatus().getReadyReplicas()).isGreaterThan(0);
    });

    var pod = client.resource(testPod()).create();
    assertThat(pod.getMetadata().getAnnotations()).containsEntry("sample.annotation.present",
        "true");

    client.resource(glue).delete();

    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var deployment = client.apps().deployments().withName("pod-mutating-hook").get();
      assertThat(deployment).isNull();
    });
  }

  Pod testPod() {
    return new PodBuilder()
        .withNewMetadata()
        .withName(TEST_POD_NAME)
        .endMetadata()
        .withNewSpec()
        .withContainers(new ContainerBuilder()
            .withName("nginx")
            .withImage("nginx:1.14.2")
            .withPorts(new ContainerPortBuilder()
                .withContainerPort(80)
                .build())
            .build())
        .endSpec()
        .build();
  }

}
