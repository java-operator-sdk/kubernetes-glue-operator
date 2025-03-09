package io.javaoperatorsdk.operator.glue.reconciler.glue;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.PatchContext;
import io.fabric8.kubernetes.client.dsl.base.PatchType;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.glue.Utils;
import io.javaoperatorsdk.operator.glue.conditions.JavaScripCondition;
import io.javaoperatorsdk.operator.glue.conditions.ReadyCondition;
import io.javaoperatorsdk.operator.glue.customresource.glue.DependentResourceSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.GlueStatus;
import io.javaoperatorsdk.operator.glue.customresource.glue.condition.ConditionSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.condition.JavaScriptConditionSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.condition.ReadyConditionSpec;
import io.javaoperatorsdk.operator.glue.dependent.GCGenericBulkDependentResource;
import io.javaoperatorsdk.operator.glue.dependent.GCGenericDependentResource;
import io.javaoperatorsdk.operator.glue.dependent.GenericDependentResource;
import io.javaoperatorsdk.operator.glue.reconciler.ValidationAndStatusHandler;
import io.javaoperatorsdk.operator.glue.reconciler.operator.GlueOperatorReconciler;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GroupVersionKindPlural;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.KubernetesResourceDeletedCondition;
import io.javaoperatorsdk.operator.processing.dependent.workflow.WorkflowBuilder;

import static io.javaoperatorsdk.operator.glue.Utils.getResourceForSSAFrom;
import static io.javaoperatorsdk.operator.glue.reconciler.operator.GlueOperatorReconciler.FOR_GLUE_OPERATOR_LABEL_VALUE;
import static io.javaoperatorsdk.operator.glue.reconciler.operator.GlueOperatorReconciler.PARENT_RELATED_RESOURCE_NAME;

@ControllerConfiguration(name = GlueReconciler.GLUE_RECONCILER_NAME)
public class GlueReconciler implements Reconciler<Glue>, Cleaner<Glue> {

  private static final Logger log = LoggerFactory.getLogger(GlueReconciler.class);
  public static final String DEPENDENT_NAME_ANNOTATION_KEY =
      "io.javaoperatorsdk.operator.glue/resource-name";
  public static final String PARENT_GLUE_FINALIZER_PREFIX =
      "io.javaoperatorsdk.operator.glue/";
  public static final String GLUE_RECONCILER_NAME = "glue";


  private final ValidationAndStatusHandler validationAndStatusHandler;
  private final InformerRegister informerRegister;

  private final KubernetesResourceDeletedCondition deletePostCondition =
      new KubernetesResourceDeletedCondition();

  private final GenericTemplateHandler genericTemplateHandler;

  public GlueReconciler(ValidationAndStatusHandler validationAndStatusHandler,
      InformerRegister informerRegister,
      GenericTemplateHandler genericTemplateHandler) {
    this.validationAndStatusHandler = validationAndStatusHandler;
    this.informerRegister = informerRegister;
    this.genericTemplateHandler = genericTemplateHandler;
  }

  /**
   * Handling finalizers for GlueOperator: Glue ids a finalizer to parent, that is necessary since
   * on clean up the resource name might be calculated based on the parents name, and it this way
   * makes sure that parent is not cleaned up until the Glue is cleaned up. The finalizer is removed
   * during cleanup. On Glue side however it is important to make sure that if the parent is deleted
   * glue gets deleted too, this is made sure in the reconcile method for glue explicitly deleting
   * itself.
   */

  @Override
  public UpdateControl<Glue> reconcile(Glue primary,
      Context<Glue> context) {

    log.debug("Reconciling glue. name: {} namespace: {}",
        primary.getMetadata().getName(), primary.getMetadata().getNamespace());

    validationAndStatusHandler.checkIfValidGlueSpec(primary.getSpec());

    registerRelatedResourceInformers(context, primary);
    if (deletedGlueIfParentMarkedForDeletion(context, primary)) {
      return UpdateControl.noUpdate();
    }
    addFinalizersToParentResource(primary, context);
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    var result = actualWorkflow.reconcile(primary, context);
    cleanupRemovedResourcesFromWorkflow(context, primary);
    informerRegister.deRegisterInformerOnResourceFlowChange(context, primary);
    result.throwAggregateExceptionIfErrorsPresent();
    patchRelatedResourcesStatus(context, primary);
    return validationAndStatusHandler.handleStatusUpdate(primary);
  }

  @Override
  public DeleteControl cleanup(Glue primary, Context<Glue> context) {

    log.debug("Cleanup for Glue. Name: {} namespace: {}", primary.getMetadata().getName(),
        primary.getMetadata().getNamespace());

    registerRelatedResourceInformers(context, primary);
    var actualWorkflow = buildWorkflowAndRegisterInformers(primary, context);
    var result = actualWorkflow.cleanup(primary, context);
    result.throwAggregateExceptionIfErrorsPresent();

    var deletableResourceCount = actualWorkflow.getDependentResourcesByName()
        .entrySet().stream().filter(e -> e.getValue().isDeletable()).count();

    if (!result.allPostConditionsMet() || result.getDeleteCalledOnDependents()
        .size() < deletableResourceCount) {
      return DeleteControl.noFinalizerRemoval();
    } else {
      removeFinalizerForParent(primary, context);
      actualWorkflow.getDependentResourcesWithoutActivationCondition().forEach(dr -> {
        var genericDependentResource = (GenericDependentResource) dr;
        informerRegister.deRegisterInformer(genericDependentResource.getGroupVersionKind(),
            primary, context);
      });
      informerRegister.deRegisterInformerForRelatedResources(primary, context);

      return DeleteControl.defaultDelete();
    }
  }

  @Override
  public ErrorStatusUpdateControl<Glue> updateErrorStatus(Glue resource, Context<Glue> context,
      Exception e) {
    if (resource.getStatus() == null) {
      resource.setStatus(new GlueStatus());
    }
    return validationAndStatusHandler.updateStatusErrorMessage(e, resource);
  }

  private boolean deletedGlueIfParentMarkedForDeletion(Context<Glue> context, Glue primary) {
    var parent = getParentRelatedResource(primary, context);
    if (parent.map(HasMetadata::isMarkedForDeletion).orElse(false)) {
      context.getClient().resource(primary).delete();
      return true;
    } else {
      return false;
    }
  }

  private void registerRelatedResourceInformers(Context<Glue> context,
      Glue glue) {
    glue.getSpec().getRelatedResources()
        .forEach(r -> informerRegister.registerInformerForRelatedResource(context, glue, r));
  }

  // todo test
  private void cleanupRemovedResourcesFromWorkflow(Context<Glue> context,
      Glue primary) {
    context.getSecondaryResources(GenericKubernetesResource.class).forEach(r -> {
      String dependentName = r.getMetadata().getAnnotations().get(DEPENDENT_NAME_ANNOTATION_KEY);
      // dependent name is null for related resources
      if (dependentName != null && primary.getSpec().getChildResources().stream()
          .filter(pr -> pr.getName().equals(dependentName)).findAny().isEmpty()) {
        try {
          log.debug("Deleting resource with name: {}", dependentName + "for resource flow: {} "
              + primary.getMetadata().getName());
          context.getClient().resource(r).delete();
        } catch (KubernetesClientException e) {
          // can happen that already deleted, just in cache.
          log.warn("Error during deleting resource on workflow change", e);
        }
      }
    });
  }

  private io.javaoperatorsdk.operator.processing.dependent.workflow.Workflow<Glue> buildWorkflowAndRegisterInformers(
      Glue primary, Context<Glue> context) {
    var builder = new WorkflowBuilder<Glue>();
    Set<String> leafDependentNames = Utils.leafResourceNames(primary);

    Map<String, GenericDependentResource> genericDependentResourceMap = new HashMap<>();
    primary.getSpec().getChildResources().forEach(spec -> createAndAddDependentToWorkflow(primary,
        context, spec, genericDependentResourceMap, builder,
        leafDependentNames.contains(spec.getName())));

    return builder.build();
  }

  private void createAndAddDependentToWorkflow(Glue primary, Context<Glue> context,
      DependentResourceSpec spec,
      Map<String, GenericDependentResource> genericDependentResourceMap,
      WorkflowBuilder<Glue> builder, boolean leafDependent) {

    // todo test processing ns not as template
    // todo test processing ns as template
    // name can reference related resources todo doc
    var targetNamespace = Utils.getNamespace(spec).map(ns -> genericTemplateHandler
        .processTemplate(ns, primary, false, context));
    var resourceInSameNamespaceAsPrimary =
        targetNamespace.map(n -> n.trim().equals(primary.getMetadata().getNamespace().trim()))
            .orElse(true);

    var name = genericTemplateHandler.processTemplate(Utils.getName(spec), primary, false, context);
    var dr = createDependentResource(name, spec, leafDependent, resourceInSameNamespaceAsPrimary,
        targetNamespace.orElse(null));
    GroupVersionKind gvk = dr.getGroupVersionKind();
    // remove when fixed in josdk
    if (gvk instanceof GroupVersionKindPlural gvkp) {
      gvk = new GroupVersionKind(gvkp.getGroup(), gvkp.getVersion(), gvkp.getKind());
    }

    var es = informerRegister.registerInformer(context, gvk, primary);
    dr.setEventSource(es);

    var nodeBuilder = builder.addDependentResourceAndConfigure(dr);
    spec.getDependsOn().forEach(s -> nodeBuilder.dependsOn(genericDependentResourceMap.get(s)));
    // if resources do not depend on another, there is no reason to add cleanup condition
    if (!spec.getDependsOn().isEmpty()) {
      nodeBuilder.withDeletePostcondition(deletePostCondition);
    }
    genericDependentResourceMap.put(spec.getName(), dr);

    Optional.ofNullable(spec.getReadyPostCondition())
        .ifPresent(c -> nodeBuilder.withReadyPostcondition(toCondition(c)));
    Optional.ofNullable(spec.getCondition())
        .ifPresent(c -> nodeBuilder.withReconcilePrecondition(toCondition(c)));

  }

  private GenericDependentResource createDependentResource(String resourceName,
      DependentResourceSpec spec,
      boolean leafDependent, Boolean resourceInSameNamespaceAsPrimary, String namespace) {

    if (leafDependent && resourceInSameNamespaceAsPrimary && !spec.isClusterScoped()) {
      return spec.getResourceTemplate() != null
          ? spec.getBulk()
              ? new GCGenericBulkDependentResource(genericTemplateHandler,
                  spec.getResourceTemplate(),
                  spec.getName(),
                  spec.isClusterScoped(), spec.getMatcher())
              : new GCGenericDependentResource(genericTemplateHandler, spec.getResourceTemplate(),
                  spec.getName(), resourceName, namespace,
                  spec.isClusterScoped(), spec.getMatcher())
          : new GCGenericDependentResource(genericTemplateHandler, spec.getResource(),
              spec.getName(), resourceName, namespace,
              spec.isClusterScoped(), spec.getMatcher());
    } else {
      return spec.getResourceTemplate() != null
          ? new GenericDependentResource(genericTemplateHandler,
              spec.getResourceTemplate(), spec.getName(), resourceName, namespace,
              spec.isClusterScoped(),
              spec.getMatcher())
          : new GenericDependentResource(genericTemplateHandler,
              spec.getResource(), spec.getName(), resourceName, namespace, spec.isClusterScoped(),
              spec.getMatcher());
    }
  }

  private void patchRelatedResourcesStatus(Context<Glue> context,
      Glue primary) {

    var targetRelatedResources = primary.getSpec().getRelatedResources().stream()
        .filter(r -> r.getStatusPatch() != null || r.getStatusPatchTemplate() != null)
        .toList();

    if (targetRelatedResources.isEmpty()) {
      return;
    }
    var actualData = genericTemplateHandler.createDataWithResources(primary, context);

    targetRelatedResources.forEach(r -> {
      var relatedResources = Utils.getRelatedResources(primary, r, context);

      var objectTemplate = r.getStatusPatch() != null;
      var template =
          objectTemplate ? Serialization.asYaml(r.getStatusPatch()) : r.getStatusPatchTemplate();
      var resultTemplate =
          genericTemplateHandler.processTemplate(actualData, template, objectTemplate);
      var statusObjectMap = GenericTemplateHandler.parseTemplateToMapObject(resultTemplate);
      relatedResources.forEach((n, kr) -> {
        if (kr != null) {
          kr.setAdditionalProperty("status", statusObjectMap);
          context.getClient().resource(kr).patchStatus();
        }
      });
    });

  }

  @SuppressWarnings({"rawtypes"})
  private Condition toCondition(ConditionSpec condition) {
    if (condition instanceof ReadyConditionSpec readyConditionSpec) {
      return new ReadyCondition(readyConditionSpec.isNegated());
    } else if (condition instanceof JavaScriptConditionSpec jsCondition) {
      return new JavaScripCondition(jsCondition.getScript());
    }
    throw new IllegalStateException("Unknown condition: " + condition);
  }

  private void addFinalizersToParentResource(Glue primary, Context<Glue> context) {
    if (!isGlueOfAGlueOperator(primary)) {
      return;
    }
    var parent = getParentRelatedResource(primary, context);

    parent.ifPresent(p -> {
      log.debug("Adding finalizer to parent. Glue name: {} namespace: {}",
          primary.getMetadata().getName(), primary.getMetadata().getNamespace());
      String finalizer = parentFinalizer(primary.getMetadata().getName());
      if (!p.getMetadata().getFinalizers().contains(finalizer)) {
        var res = getResourceForSSAFrom(p);
        res.getMetadata().getFinalizers().add(finalizer);
        patchResource(res, context);
      }
    });
  }

  private void removeFinalizerForParent(Glue primary, Context<Glue> context) {
    if (!isGlueOfAGlueOperator(primary)) {
      return;
    }
    var parent = getParentRelatedResource(primary, context);
    parent.ifPresentOrElse(p -> {
      log.debug("Removing finalizer from parent. Glue name: {} namespace: {}",
          primary.getMetadata().getName(), primary.getMetadata().getNamespace());
      String finalizer = parentFinalizer(primary.getMetadata().getName());
      if (p.getMetadata().getFinalizers().contains(finalizer)) {
        var res = getResourceForSSAFrom(p);
        patchResource(res, context);
      }
    }, () -> log.warn(
        "Parent resource expected to be present on cleanup. Glue name: {} namespace: {}",
        primary.getMetadata().getName(), primary.getMetadata().getNamespace()));
  }

  private GenericKubernetesResource patchResource(GenericKubernetesResource res,
      Context<Glue> context) {
    return context.getClient().resource(res)
        .patch(new PatchContext.Builder()
            .withFieldManager(context.getControllerConfiguration().fieldManager())
            .withForce(true)
            .withPatchType(PatchType.SERVER_SIDE_APPLY)
            .build());
  }

  private Optional<GenericKubernetesResource> getParentRelatedResource(Glue primary,
      Context<Glue> context) {
    var parentRelated = primary.getSpec().getRelatedResources().stream()
        .filter(r -> r.getName().equals(PARENT_RELATED_RESOURCE_NAME))
        .findAny();

    return parentRelated.flatMap(r -> {
      var relatedResources = Utils.getRelatedResources(primary, r, context);
      if (relatedResources.size() > 1) {
        throw new IllegalStateException(
            "parent related resource contains more resourceNames for glue name: "
                + primary.getMetadata().getName()
                + " namespace: " + primary.getMetadata().getNamespace());
      }
      if (relatedResources.isEmpty()) {
        return Optional.empty();
      } else {
        return Optional.of(relatedResources.entrySet().iterator().next().getValue());
      }
    });
  }

  private String parentFinalizer(String glueName) {
    return PARENT_GLUE_FINALIZER_PREFIX + glueName;
  }

  public static boolean isGlueOfAGlueOperator(Glue glue) {
    var labelValue =
        glue.getMetadata().getLabels().get(GlueOperatorReconciler.FOR_GLUE_OPERATOR_LABEL_KEY);
    return FOR_GLUE_OPERATOR_LABEL_VALUE.equals(labelValue);

  }

}
