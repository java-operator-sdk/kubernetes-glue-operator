package io.javaoperatorsdk.operator.glue.customresource.glue;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Group("io.javaoperatorsdk.operator.glue")
@Version("v1beta1")
@ShortNames("g")
public class Glue extends CustomResource<GlueSpec, GlueStatus>
    implements Namespaced {
}
