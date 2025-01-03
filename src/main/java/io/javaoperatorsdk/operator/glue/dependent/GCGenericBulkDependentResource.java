package io.javaoperatorsdk.operator.glue.dependent;

import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.Matcher;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;

public class GCGenericBulkDependentResource extends GenericBulkDependentResource
    implements GarbageCollected<Glue> {

  public GCGenericBulkDependentResource(GenericTemplateHandler genericTemplateHandler,
      String desiredTemplate, String name,
      boolean clusterScoped, Matcher matcher) {
    super(genericTemplateHandler, desiredTemplate, name, clusterScoped, matcher);
  }

}
