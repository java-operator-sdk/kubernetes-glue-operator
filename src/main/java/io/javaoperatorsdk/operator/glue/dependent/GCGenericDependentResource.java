package io.javaoperatorsdk.operator.glue.dependent;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;

public class GCGenericDependentResource extends GenericDependentResource
    implements GarbageCollected<Glue> {

  public GCGenericDependentResource(GenericTemplateHandler genericTemplateHandler,
      GenericKubernetesResource desired, String name,
      boolean clusterScoped) {
    super(genericTemplateHandler, desired, name, clusterScoped);
  }

  public GCGenericDependentResource(GenericTemplateHandler genericTemplateHandler,
      String desiredTemplate, String name, boolean clusterScoped) {
    super(genericTemplateHandler, desiredTemplate, name, clusterScoped);
  }
}