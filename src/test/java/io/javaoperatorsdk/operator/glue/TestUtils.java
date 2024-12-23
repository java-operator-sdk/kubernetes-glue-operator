package io.javaoperatorsdk.operator.glue;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.NonDeletingOperation;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.ReconcilerUtils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperator;

public class TestUtils {

  public static final Duration GC_WAIT_TIMEOUT = Duration.ofSeconds(120);
  public static final Duration INITIAL_RECONCILE_WAIT_TIMEOUT = Duration.ofMillis(150);

  public static final int CRD_READY_WAIT = 1000;


  private static final Logger log = LoggerFactory.getLogger(TestUtils.class);

  public static Glue loadGlue(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, Glue.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GlueOperator loadGlueOperator(String path) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, GlueOperator.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static <T extends HasMetadata> T load(String path, Class<T> clazz) {
    try (InputStream is = TestUtils.class.getResourceAsStream(path)) {
      return Serialization.unmarshal(is, clazz);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static GenericKubernetesResource load(String path) {
    return load(path, GenericKubernetesResource.class);
  }

  public static GenericKubernetesResource createOrUpdate(KubernetesClient client, String path) {
    return client.resource(load(path)).createOr(NonDeletingOperation::update);
  }

  public static void applyCrd(Class<? extends HasMetadata> resourceClass, KubernetesClient client,
      boolean test) {
    applyCrd(ReconcilerUtils.getResourceTypeName(resourceClass), client, test);
  }

  @SafeVarargs
  public static void applyTestCrd(KubernetesClient client,
      Class<? extends HasMetadata>... resourceClasses) {
    Arrays.stream(resourceClasses).forEach(c -> applyCrd(c, client, true));
  }

  @SafeVarargs
  public static void applyCrd(KubernetesClient client,
      Class<? extends HasMetadata>... resourceClasses) {
    Arrays.stream(resourceClasses).forEach(c -> applyCrd(c, client, false));
  }

  public static void applyCrd(String resourceTypeName, KubernetesClient client, boolean test) {

    try (InputStream is = createInputStream(resourceTypeName, test)) {

      var crdString = new String(is.readAllBytes(), StandardCharsets.UTF_8);
      log.debug("Applying CRD: {}", crdString);
      final var crd = client.load(new ByteArrayInputStream(crdString.getBytes()));
      crd.createOrReplace();
      Thread.sleep(CRD_READY_WAIT); // readiness is not applicable for CRD, just wait a little
      log.debug("Applied CRD for resource: {}", resourceTypeName);
    } catch (InterruptedException ex) {
      log.error("Interrupted.", ex);
      Thread.currentThread().interrupt();
    } catch (Exception ex) {
      throw new IllegalStateException("Cannot apply CRD for: " + resourceTypeName, ex);
    }
  }

  private static InputStream createInputStream(String resourceTypeName, boolean test)
      throws FileNotFoundException {
    if (test) {
      String path = "/META-INF/fabric8/" + resourceTypeName + "-v1.yml";
      return TestUtils.class.getResourceAsStream(path);
    } else {
      String path = "target/kubernetes/" + resourceTypeName + "-v1.yml";
      File file = new File(path);

      var res = new FileInputStream(path);
      if (!file.exists()) {
        throw new IllegalStateException("Cannot find CRD at " + file.getAbsolutePath());
      }
      return res;
    }
  }

  public static void applyAndWait(KubernetesClient client, String path) {
    applyAndWait(client, path, null);
  }

  public static void applyAndWait(KubernetesClient client, String path,
      UnaryOperator<HasMetadata> transform) {
    try (FileInputStream fileInputStream = new FileInputStream(path)) {
      applyAndWait(client, fileInputStream, transform);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static void applyAndWait(KubernetesClient client, InputStream is) {
    applyAndWait(client, is, null);
  }

  public static void applyAndWait(KubernetesClient client, List<HasMetadata> resources,
      UnaryOperator<HasMetadata> transformer) {
    if (transformer != null) {
      resources = resources.stream().map(transformer).collect(Collectors.toList());
    }
    client.resourceList(resources).createOrReplace();
    client.resourceList(resources).waitUntilReady(3, TimeUnit.MINUTES);
  }

  public static void applyAndWait(KubernetesClient client, InputStream is,
      UnaryOperator<HasMetadata> transformer) {
    var resources = client.load(is).items();
    applyAndWait(client, resources, transformer);
  }

  public static void applyAndWait(KubernetesClient client, URL url) {
    try (InputStream is = url.openStream()) {
      applyAndWait(client, is);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
