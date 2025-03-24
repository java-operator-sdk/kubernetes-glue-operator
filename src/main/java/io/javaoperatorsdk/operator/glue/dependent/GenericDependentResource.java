package io.javaoperatorsdk.operator.glue.dependent;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Deleter;
import io.javaoperatorsdk.operator.glue.Utils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.Matcher;
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

  private static final Logger log = LoggerFactory.getLogger(GenericDependentResource.class);

  protected final GenericKubernetesResource desired;
  protected final String desiredTemplate;
  // resource name might be templated
  protected final String resourceName;
  protected final String namespace;
  protected final boolean clusterScoped;
  protected final Matcher matcher;

  protected final GenericTemplateHandler genericTemplateHandler;

  public GenericDependentResource(GenericTemplateHandler genericTemplateHandler,
      GenericKubernetesResource desired, String name, String resourceName, String namespace,
      boolean clusterScoped, Matcher matcher) {
    super(new GroupVersionKind(desired.getApiVersion(), desired.getKind()), name);
    this.desired = desired;
    this.namespace = namespace;
    this.matcher = matcher;
    this.desiredTemplate = null;
    this.resourceName = resourceName;
    this.clusterScoped = clusterScoped;
    this.genericTemplateHandler = genericTemplateHandler;
  }

  public GenericDependentResource(GenericTemplateHandler genericTemplateHandler,
      String desiredTemplate, String name, String resourceName, String namespace,
      boolean clusterScoped,
      Matcher matcher) {
    super(new GroupVersionKind(Utils.getApiVersionFromTemplate(desiredTemplate),
        Utils.getKindFromTemplate(desiredTemplate)), name);
    this.genericTemplateHandler = genericTemplateHandler;
    this.resourceName = resourceName;
    this.desiredTemplate = desiredTemplate;
    this.namespace = namespace;
    this.matcher = matcher;
    this.desired = null;
    this.clusterScoped = clusterScoped;
  }

  @Override
  protected GenericKubernetesResource desired(Glue primary,
      Context<Glue> context) {
    boolean objectTemplate = desired != null;
    var template = objectTemplate ? Serialization.asYaml(desired) : desiredTemplate;

    var res = genericTemplateHandler.processTemplate(template, primary, objectTemplate, context);
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
    if (Matcher.SSA.equals(matcher)) {
      return super.match(actualResource, primary, context);
    } else {
      return Result.nonComputed(false);
    }
  }

  @Override
  protected Optional<GenericKubernetesResource> selectTargetSecondaryResource(
      Set<GenericKubernetesResource> secondaryResources,
      Glue primary,
      Context<Glue> context) {

    var allSecondaryResources = context.getSecondaryResources(GenericKubernetesResource.class);
    if (log.isDebugEnabled()) {
      log.debug("All secondary resources for DR: {}, resources: {}", name,
          allSecondaryResources.stream()
              .map(r -> "{ Name: %s; Namespace: %s }".formatted(r.getMetadata().getName(),
                  r.getMetadata().getNamespace()))
              .toList());
    }
    var res = allSecondaryResources
        .stream()
        .filter(r -> r.getKind().equals(getGroupVersionKind().getKind()) &&
            r.getApiVersion().equals(getGroupVersionKind().apiVersion()) &&
            r.getMetadata().getName().equals(resourceName) &&
            (namespace == null || Objects.equals(namespace, r.getMetadata().getNamespace())))
        .toList();

    if (res.size() > 1) {
      throw new IllegalStateException("Multiple resources found for gvk: " + getGroupVersionKind()
          + " name:" + name
          + " namespace:" + namespace);
    } else if (res.size() == 1) {
      return Optional.of(res.get(0));
    } else {
      return Optional.empty();
    }
  }

}
