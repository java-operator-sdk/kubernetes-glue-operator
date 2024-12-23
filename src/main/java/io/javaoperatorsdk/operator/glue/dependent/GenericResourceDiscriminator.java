package io.javaoperatorsdk.operator.glue.dependent;

import java.util.Objects;
import java.util.Optional;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;

public class GenericResourceDiscriminator
    implements ResourceDiscriminator<GenericKubernetesResource, Glue> {

  private final GroupVersionKind groupVersionKind;
  private final String name;
  private final String namespace;

  public GenericResourceDiscriminator(GroupVersionKind groupVersionKind, String name,
      String namespace) {
    this.groupVersionKind = groupVersionKind;
    this.name = name;
    this.namespace = namespace;
  }

  @Override
  public Optional<GenericKubernetesResource> distinguish(Class<GenericKubernetesResource> resource,
      Glue primary,
      Context<Glue> context) {
    var res = context.getSecondaryResources(GenericKubernetesResource.class)
        .stream()
        .filter(r -> r.getKind().equals(groupVersionKind.getKind()) &&
            r.getApiVersion().equals(groupVersionKind.apiVersion()) &&
            r.getMetadata().getName().equals(name) &&
            (namespace == null || Objects.equals(namespace, r.getMetadata().getNamespace())))
        .toList();

    if (res.size() > 1) {
      throw new IllegalStateException("Multiple resources found for gvk: " + groupVersionKind
          + " name:" + name
          + " namespace:" + namespace);
    } else if (res.size() == 1) {
      return Optional.of(res.get(0));
    } else {
      return Optional.empty();
    }
  }
}
