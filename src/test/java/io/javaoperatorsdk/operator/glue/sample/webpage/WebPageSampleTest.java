package io.javaoperatorsdk.operator.glue.sample.webpage;


import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.javaoperatorsdk.operator.glue.TestBase;
import io.javaoperatorsdk.operator.glue.TestUtils;
import io.quarkus.test.junit.QuarkusTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@QuarkusTest
public class WebPageSampleTest extends TestBase {

  @Test
  void webPageCRUD() {
    createOrUpdate(TestUtils.load("/sample/webpage/webpage.crd.yaml"));
    createOrUpdate(TestUtils.load("/sample/webpage/webpage.operator.yaml"));
    WebPage webPage =
        createOrUpdate(TestUtils.load("/sample/webpage/webpage.sample.yaml", WebPage.class));

    await().untilAsserted(() -> {
      var deployment = get(Deployment.class, webPage.getMetadata().getName());
      var configMap = get(ConfigMap.class, webPage.getMetadata().getName());
      var service = get(Service.class, webPage.getMetadata().getName());
      var ingress = get(Ingress.class, webPage.getMetadata().getName());

      assertThat(ingress).isNull();
      assertThat(deployment).isNotNull();
      assertThat(service).isNotNull();
      assertThat(configMap).isNotNull();
      assertThat(configMap.getData().get("index.html")).contains("Hello World!");
    });

    webPage.getSpec().setExposed(true);
    webPage.getSpec().setHtml("""
        <html>
        <head>
        <title>Hello Operator World</title>
        </head>
        <body>
        Hello World 2!
        </body>
        </html>
        """);
    update(webPage);

    await().untilAsserted(() -> {
      var ingress = get(Ingress.class, webPage.getMetadata().getName());
      var configMap = get(ConfigMap.class, webPage.getMetadata().getName());

      assertThat(configMap.getData().get("index.html")).contains("Hello World 2!");
      assertThat(ingress).isNotNull();
    });

    delete(webPage);

    await().timeout(TestUtils.GC_WAIT_TIMEOUT).untilAsserted(() -> {
      var deployment = get(Deployment.class, webPage.getMetadata().getName());
      assertThat(deployment).isNull();
    });
  }
}
