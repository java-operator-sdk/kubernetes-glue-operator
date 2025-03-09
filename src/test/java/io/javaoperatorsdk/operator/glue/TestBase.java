package io.javaoperatorsdk.operator.glue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;

import jakarta.inject.Inject;

import static io.javaoperatorsdk.operator.glue.TestUtils.loadGlue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class TestBase {

  public static final int GC_TIMEOUT_SEC = 120;
  public static final Duration GC_TIMEOUT = Duration.ofSeconds(GC_TIMEOUT_SEC);

  @Inject
  protected KubernetesClient client;

  protected String testNamespace;

  @BeforeEach
  public void prepareNamespace(TestInfo testInfo) {
    testInfo.getTestMethod()
        .ifPresent(method -> testNamespace = KubernetesResourceUtil.sanitizeName(method.getName()));

    client.namespaces().resource(testNamespace(testNamespace)).create();
  }

  @AfterEach
  void cleanupNamespace() {
    client.namespaces().withName(testNamespace).delete();
    await().timeout(Duration.ofSeconds(GC_TIMEOUT_SEC)).untilAsserted(() -> {
      var ns = client.namespaces().withName(testNamespace).get();
      assertThat(ns).isNull();
    });
  }

  protected Namespace testNamespace(String name) {
    return new NamespaceBuilder().withMetadata(new ObjectMetaBuilder()
        .withName(name)
        .build()).build();
  }

  protected Glue createGlue(String path) {
    return create(loadGlue(path));
  }

  protected <T extends HasMetadata> T create(T resource) {
    return client.resource(resource).inNamespace(testNamespace).create();
  }

  protected <T extends HasMetadata> T createOrUpdate(T resource) {
    return client.resource(resource).inNamespace(testNamespace)
        .createOr(NonDeletingOperation::update);
  }

  protected <T extends HasMetadata> T get(Class<T> clazz, String name) {
    return client.resources(clazz).inNamespace(testNamespace).withName(name).get();
  }

  protected <T extends HasMetadata> List<T> list(Class<T> clazz) {
    return client.resources(clazz).inNamespace(testNamespace).list().getItems();
  }

  protected <T extends HasMetadata> List<T> getRelatedList(Class<T> clazz, String ownerName) {
    return list(clazz).stream()
        .filter(cm -> !cm.getMetadata().getOwnerReferences().isEmpty()
            && cm.getMetadata().getOwnerReferences()
                .get(0).getName().equals(ownerName))
        .toList();
  }

  protected <T extends HasMetadata> T update(T resource) {
    resource.getMetadata().setResourceVersion(null);
    return client.resource(resource).inNamespace(testNamespace).update();
  }

  protected void delete(HasMetadata resource) {
    client.resource(resource).inNamespace(testNamespace).delete();
  }



}
