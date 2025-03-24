package io.javaoperatorsdk.operator.glue.reconciler.glue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.Mappers;

public class RelatedAndOwnedResourceSecondaryToPrimaryMapper
    implements SecondaryToPrimaryMapper<GenericKubernetesResource> {

  private static final Logger log =
      LoggerFactory.getLogger(RelatedAndOwnedResourceSecondaryToPrimaryMapper.class);

  private final Map<ResourceID, Set<ResourceID>> secondaryToPrimaryMap = new ConcurrentHashMap<>();

  @Override
  public Set<ResourceID> toPrimaryResourceIDs(GenericKubernetesResource resource) {
    // based on if GC or non GC dependent it can have different mapping
    var res = Mappers.fromOwnerReferences(Glue.class, false).toPrimaryResourceIDs(resource);
    res.addAll(Mappers.fromDefaultAnnotations(Glue.class).toPrimaryResourceIDs(resource));

    // related resource mapping
    var idMapped = secondaryToPrimaryMap.get(
        new ResourceID(resource.getMetadata().getName(), resource.getMetadata().getNamespace()));
    if (idMapped != null) {
      res.addAll(idMapped);
    }
    log.debug("Resource name: {}, namespace: {}, kind: {}, resourceIds: {}",
        resource.getMetadata().getName(),
        resource.getMetadata().getNamespace(), resource.getKind(), res);
    return res;
  }

  public void addResourceIDMapping(Collection<ResourceID> resourceIDs, ResourceID glueID) {
    Set<ResourceID> glueIDSet = new HashSet<>();
    glueIDSet.add(glueID);
    resourceIDs
        .forEach(resourceID -> secondaryToPrimaryMap.merge(resourceID, glueIDSet, (s1, s2) -> {
          s1.addAll(s2);
          return s1;
        }));
  }

  public void removeMappingFor(ResourceID workflowID) {
    secondaryToPrimaryMap.entrySet().stream().forEach(e -> {
      e.getValue().remove(workflowID);
    });
    secondaryToPrimaryMap.entrySet().removeIf(e -> e.getValue().isEmpty());
  }
}
