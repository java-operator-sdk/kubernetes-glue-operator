package io.javaoperatorsdk.operator.glue.sample.webpage;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.Singular;
import io.fabric8.kubernetes.model.annotation.Version;

@Version(value = "v1")
@Group("glueoperator.sample")
@Singular("webpage")
@Plural("webpages")
public class WebPage extends CustomResource<WebPageSpec, WebPageStatus>
    implements Namespaced {
}
