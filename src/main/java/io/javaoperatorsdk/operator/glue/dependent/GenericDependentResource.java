package io.javaoperatorsdk.operator.glue.dependent;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.glue.Utils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.reconciler.glue.GlueReconciler;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesDependentResource;

public class GenericDependentResource
    extends GenericKubernetesDependentResource<Glue>
    implements Deleter<Glue>,
    Updater<GenericKubernetesResource, Glue>,
    Creator<GenericKubernetesResource, Glue> {

  private final GenericKubernetesResource desired;
  private final String desiredTemplate;
  private final String name;
  private final boolean clusterScoped;

  // optimize share between instances
  private final GenericTemplateHandler genericTemplateHandler;

  public GenericDependentResource(GenericTemplateHandler genericTemplateHandler,
      GenericKubernetesResource desired, String name,
      boolean clusterScoped) {
    super(new GroupVersionKind(desired.getApiVersion(), desired.getKind()));
    this.desired = desired;
    this.desiredTemplate = null;
    this.name = name;
    this.clusterScoped = clusterScoped;
    this.genericTemplateHandler = genericTemplateHandler;
  }

  public GenericDependentResource(GenericTemplateHandler genericTemplateHandler,
      String desiredTemplate, String name, boolean clusterScoped) {
    super(new GroupVersionKind(Utils.getApiVersionFromTemplate(desiredTemplate),
        Utils.getKindFromTemplate(desiredTemplate)));
    this.genericTemplateHandler = genericTemplateHandler;
    this.name = name;
    this.desiredTemplate = desiredTemplate;
    this.desired = null;
    this.clusterScoped = clusterScoped;
  }

  @Override
  protected GenericKubernetesResource desired(Glue primary,
      Context<Glue> context) {

    var template = desired == null ? desiredTemplate : Serialization.asYaml(desired);

    var res = genericTemplateHandler.processTemplate(template, primary, context);
    var resultDesired = Serialization.unmarshal(res, GenericKubernetesResource.class);

    resultDesired.getMetadata().getAnnotations()
        .put(GlueReconciler.DEPENDENT_NAME_ANNOTATION_KEY, name);

    if (resultDesired.getMetadata().getNamespace() == null && !clusterScoped) {
      resultDesired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
    }
    return resultDesired;
  }

  @Override
  public Result<GenericKubernetesResource> match(GenericKubernetesResource actualResource,
      Glue primary, Context<Glue> context) {
    // see details here: https://github.com/operator-framework/java-operator-sdk/issues/2249
    if (actualResource.getKind().equals("Deployment")
        && actualResource.getApiVersion().equals("apps/v1")) {
      return super.match(actualResource, primary, context);
    }
    return Result.nonComputed(false);
  }
}
