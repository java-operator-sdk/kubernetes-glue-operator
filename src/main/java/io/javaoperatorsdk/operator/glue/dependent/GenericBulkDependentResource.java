package io.javaoperatorsdk.operator.glue.dependent;

import java.util.Map;
import java.util.stream.Collectors;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.GenericKubernetesResourceList;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.Matcher;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;
import io.javaoperatorsdk.operator.processing.dependent.BulkDependentResource;

import static io.javaoperatorsdk.operator.glue.reconciler.glue.GlueReconciler.DEPENDENT_NAME_ANNOTATION_KEY;

public class GenericBulkDependentResource extends
    GenericDependentResource implements
    BulkDependentResource<GenericKubernetesResource, Glue> {

  public GenericBulkDependentResource(GenericTemplateHandler genericTemplateHandler,
      String desiredTemplate, String name,
      boolean clusterScoped,
      Matcher matcher) {
    super(genericTemplateHandler, desiredTemplate, name, clusterScoped, matcher);
  }

  @Override
  public Map<String, GenericKubernetesResource> desiredResources(Glue primary,
      Context<Glue> context) {

    var res = genericTemplateHandler.processTemplate(desiredTemplate, primary, false, context);
    var desiredList = Serialization.unmarshal(res, GenericKubernetesResourceList.class).getItems();
    desiredList.forEach(r -> {
      r.getMetadata().getAnnotations()
          .put(DEPENDENT_NAME_ANNOTATION_KEY, name);
      if (r.getMetadata().getNamespace() == null && !clusterScoped) {
        r.getMetadata().setNamespace(primary.getMetadata().getNamespace());
      }
    });
    return desiredList.stream().collect(Collectors.toMap(r -> r.getMetadata().getName(), r -> r));
  }

  @Override
  public Map<String, GenericKubernetesResource> getSecondaryResources(Glue glue,
      Context<Glue> context) {
    return context.getSecondaryResources(GenericKubernetesResource.class).stream()
        .filter(
            r -> name.equals(r.getMetadata().getAnnotations().get(DEPENDENT_NAME_ANNOTATION_KEY)))
        .collect(Collectors.toMap(r -> r.getMetadata().getName(), r -> r));
  }
}
