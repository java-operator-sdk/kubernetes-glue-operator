package io.javaoperatorsdk.operator.glue;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.glue.customresource.glue.DependentResourceSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.RelatedResourceSpec;
import io.javaoperatorsdk.operator.glue.reconciler.glue.GlueReconciler;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class Utils {

  private static final Logger log = LoggerFactory.getLogger(Utils.class);

  public static final String RESOURCE_NAME_DELIMITER = "#";

  private Utils() {}

  public static Map<String, GenericKubernetesResource> getActualResourcesByNameInWorkflow(
      Context<Glue> context, Glue glue) {
    var secondaryResources = context.getSecondaryResources(GenericKubernetesResource.class);
    Map<String, GenericKubernetesResource> res = new HashMap<>();
    secondaryResources.forEach(sr -> {
      var dependentSpec = glue.getSpec().getChildResources().stream()
          .filter(r ->
      // comparing the name from annotation since the resource name might be templated in spec
      // therefore "Utils.getName(relatedResourceSpec).equals(sr.getMetadata().getName())" would not
      // work
      r.getName()
          .equals(
              sr.getMetadata().getAnnotations().get(GlueReconciler.DEPENDENT_NAME_ANNOTATION_KEY)))
          .findFirst();
      dependentSpec.ifPresent(spec -> res.put(spec.getName(), sr));
    });

    glue.getSpec().getRelatedResources().forEach(r -> {
      var relatedResources = getRelatedResources(glue, r, context);
      if (relatedResources.size() == 1) {
        var resourceEntry = relatedResources.entrySet().iterator().next();
        res.put(r.getName(), resourceEntry.getValue());

      } else {
        relatedResources.forEach((resourceName, resource) -> res
            .put(r.getName() + RESOURCE_NAME_DELIMITER + resourceName, resource));
      }
    });

    return res;
  }

  @SuppressWarnings("unchecked")
  public static Map<String, GenericKubernetesResource> getRelatedResources(Glue glue,
      RelatedResourceSpec relatedResourceSpec,
      Context<?> context) {
    var gvk =
        new GroupVersionKind(relatedResourceSpec.getApiVersion(), relatedResourceSpec.getKind());
    log.trace("Getting event source for gvk: {}", gvk);
    var es =
        (InformerEventSource<GenericKubernetesResource, Glue>) context
            .eventSourceRetriever()
            .getEventSourceFor(GenericKubernetesResource.class, gvk.toString());

    var namespace = relatedResourceSpec.isClusterScoped() ? null
        : relatedResourceSpec.getNamespace() == null ? glue.getMetadata().getNamespace()
            : relatedResourceSpec.getNamespace();

    var res = new HashMap<String, GenericKubernetesResource>();

    relatedResourceSpec.getResourceNames()
        .forEach(r -> res.put(r, es.get(new ResourceID(r, namespace)).orElse(null)));
    return res;
  }

  public static GenericKubernetesResource getResourceForSSAFrom(
      GenericKubernetesResource resourceFromServer) {
    var res = new GenericKubernetesResource();
    res.setKind(resourceFromServer.getKind());
    res.setApiVersion(resourceFromServer.getApiVersion());
    res.setMetadata(new ObjectMetaBuilder()
        .withName(resourceFromServer.getMetadata().getName())
        .withNamespace(resourceFromServer.getMetadata().getNamespace())
        .build());
    return res;
  }

  public static GroupVersionKind getGVK(DependentResourceSpec spec) {
    return new GroupVersionKind(getApiVersion(spec), getKind(spec));
  }

  public static String getName(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return spec.getResource().getMetadata().getName();
    } else {
      return getPropertyValueFromTemplate(spec.getResourceTemplate(), "name");
    }
  }

  public static String getApiVersion(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return spec.getResource().getApiVersion();
    } else {
      return getPropertyValueFromTemplate(spec.getResourceTemplate(), "apiVersion");
    }
  }

  public static String getKind(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return spec.getResource().getKind();
    } else {
      return getPropertyValueFromTemplate(spec.getResourceTemplate(), "kind");
    }
  }

  public static Optional<String> getNamespace(DependentResourceSpec spec) {
    if (spec.getResource() != null) {
      return Optional.ofNullable(spec.getResource().getMetadata().getNamespace());
    } else {
      return getOptionalPropertyValueFromTemplate(spec.getResourceTemplate(), "namespace");
    }
  }

  public static String getApiVersionFromTemplate(String resourceTemplate) {
    return getPropertyValueFromTemplate(resourceTemplate, "apiVersion");
  }

  public static String getKindFromTemplate(String resourceTemplate) {
    return getPropertyValueFromTemplate(resourceTemplate, "kind");
  }

  private static Optional<String> getOptionalPropertyValueFromTemplate(String resourceTemplate,
      String property) {
    var finalProp = property + ":";
    var targetLine = resourceTemplate.lines().filter(l -> l.contains(finalProp)).findFirst();
    return targetLine.map(l -> {
      int index = l.indexOf(finalProp);
      if (index > 0) {
        l = l.substring(index);
      }
      return l.replace(finalProp, "").trim();
    });
  }

  private static String getPropertyValueFromTemplate(String resourceTemplate, String property) {
    return getOptionalPropertyValueFromTemplate(resourceTemplate, property)
        .orElseThrow(() -> new IllegalArgumentException(
            "Template does not contain property. " + resourceTemplate));
  }

  public static GroupVersionKind getGVKFromTemplate(String resourceTemplate) {
    String apiVersion = getApiVersionFromTemplate(resourceTemplate);
    String kind = getKindFromTemplate(resourceTemplate);
    return new GroupVersionKind(apiVersion, kind);
  }

}
