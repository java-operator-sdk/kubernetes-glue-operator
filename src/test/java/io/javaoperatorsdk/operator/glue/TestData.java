package io.javaoperatorsdk.operator.glue;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.glue.customresource.TestCustomResource;
import io.javaoperatorsdk.operator.glue.customresource.TestCustomResource2;
import io.javaoperatorsdk.operator.glue.customresource.TestCustomResourceSpec;

public class TestData {

  public static final String TEST_RESOURCE_VALUE = "val";
  public static final String TEST_RESOURCE_PREFIX = "testcr";
  public static final String TEST_RESOURCE2_PREFIX = "testcr2";

  public static TestCustomResource testCustomResource() {
    return testCustomResource(1);
  }

  public static TestCustomResource testCustomResource(int index) {
    var res = new TestCustomResource();
    res.setMetadata(new ObjectMetaBuilder()
        .withName(TEST_RESOURCE_PREFIX + index)
        .build());
    res.setSpec(new TestCustomResourceSpec());
    res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
    return res;
  }

  public static TestCustomResource2 testCustomResource2(int index) {
    var res = new TestCustomResource2();
    res.setMetadata(new ObjectMetaBuilder()
        .withName(TEST_RESOURCE2_PREFIX + index)
        .build());
    res.setSpec(new TestCustomResourceSpec());
    res.getSpec().setValue(TEST_RESOURCE_VALUE + index);
    return res;
  }

}
