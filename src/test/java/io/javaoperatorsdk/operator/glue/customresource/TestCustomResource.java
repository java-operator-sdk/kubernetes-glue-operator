package io.javaoperatorsdk.operator.glue.customresource;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group(TestCustomResource.CR_GROUP)
@Version("v1")
@ShortNames("tcr")
public class TestCustomResource extends CustomResource<TestCustomResourceSpec, Void>
    implements Namespaced {

  public static final String CR_GROUP = "io.javaoperatorsdk.operator.glue";


}
