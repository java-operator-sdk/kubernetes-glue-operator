package io.javaoperatorsdk.operator.glue;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UtilsTest {


  @Test
  void getAPIVersionFromListResource() {
    String apiVersion = Utils.getApiVersionFromTemplate("""
        - apiVersion: v1
          kind: ConfigMap
          metadata:
             name: simple-glue-configmap-{i}
          data:
             key: "value1"
        """);

    assertThat(apiVersion).isEqualTo("v1");
  }

  @Test
  void getAPIVersionPFromResource() {
    String apiVersion = Utils.getApiVersionFromTemplate("""
        apiVersion: v1
        kind: ConfigMap
        metadata:
           name: simple-glue-configmap-{i}
        data:
           key: "value1"
        """);

    assertThat(apiVersion).isEqualTo("v1");
  }

}
