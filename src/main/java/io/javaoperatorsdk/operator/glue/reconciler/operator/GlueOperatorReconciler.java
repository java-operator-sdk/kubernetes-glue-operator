package io.javaoperatorsdk.operator.glue.reconciler.operator;

import java.util.*;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.utils.KubernetesResourceUtil;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.*;
import io.javaoperatorsdk.operator.glue.ControllerConfig;
import io.javaoperatorsdk.operator.glue.GlueException;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.customresource.glue.GlueSpec;
import io.javaoperatorsdk.operator.glue.customresource.glue.RelatedResourceSpec;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperator;
import io.javaoperatorsdk.operator.glue.customresource.operator.GlueOperatorSpec;
import io.javaoperatorsdk.operator.glue.customresource.operator.Parent;
import io.javaoperatorsdk.operator.glue.customresource.operator.ResourceFlowOperatorStatus;
import io.javaoperatorsdk.operator.glue.reconciler.ValidationAndErrorHandler;
import io.javaoperatorsdk.operator.glue.reconciler.glue.GlueReconciler;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import jakarta.annotation.PostConstruct;

@ControllerConfiguration(name = GlueOperatorReconciler.GLUE_OPERATOR_RECONCILER_NAME)
public class GlueOperatorReconciler
    implements Reconciler<GlueOperator>, EventSourceInitializer<GlueOperator>,
    Cleaner<GlueOperator>, ErrorStatusHandler<GlueOperator> {

  private static final Logger log = LoggerFactory.getLogger(GlueOperatorReconciler.class);

  public static final String FOR_GLUE_OPERATOR_LABEL_KEY = "for-glue-operator";
  public static final String FOR_GLUE_OPERATOR_LABEL_VALUE = "true";
  public static final String PARENT_RELATED_RESOURCE_NAME = "parent";
  public static final String GLUE_OPERATOR_RECONCILER_NAME = "glue-operator";

  @ConfigProperty(name = "quarkus.operator-sdk.controllers." + GlueReconciler.GLUE_RECONCILER_NAME
      + ".selector")
  Optional<String> glueLabelSelector;

  private final ControllerConfig controllerConfig;
  private final ValidationAndErrorHandler validationAndErrorHandler;
  private final GenericTemplateHandler genericTemplateHandler;

  private Map<String, String> defaultGlueLabels;

  private InformerEventSource<Glue, GlueOperator> glueEventSource;

  public GlueOperatorReconciler(ControllerConfig controllerConfig,
      ValidationAndErrorHandler validationAndErrorHandler,
      GenericTemplateHandler genericTemplateHandler) {
    this.controllerConfig = controllerConfig;
    this.validationAndErrorHandler = validationAndErrorHandler;
    this.genericTemplateHandler = genericTemplateHandler;
  }

  @PostConstruct
  void init() {
    defaultGlueLabels = initDefaultLabelsToAddToGlue();
  }

  @Override
  public UpdateControl<GlueOperator> reconcile(GlueOperator glueOperator,
      Context<GlueOperator> context) {

    log.info("Reconciling GlueOperator {} in namespace: {}", glueOperator.getMetadata().getName(),
        glueOperator.getMetadata().getNamespace());

    validationAndErrorHandler.checkIfValidGlueSpec(glueOperator.getSpec());

    var targetCREventSource = getOrRegisterCustomResourceEventSource(glueOperator, context);
    targetCREventSource.list().forEach(cr -> {
      var actualResourceFlow = glueEventSource
          .get(new ResourceID(glueName(cr.getMetadata().getName(), cr.getKind()),
              cr.getMetadata().getNamespace()));
      var desiredResourceFlow = createGlue(cr, glueOperator);
      if (actualResourceFlow.isEmpty()) {
        context.getClient().resource(desiredResourceFlow).serverSideApply();
      } else if (!actualResourceFlow.orElseThrow().getSpec()
          .equals(desiredResourceFlow.getSpec())) {
        log.debug("Updating resource from for operator name: {} cr: {} namespace: {}",
            glueOperator.getMetadata().getName(),
            cr.getMetadata().getName(),
            glueOperator.getMetadata().getNamespace());
        context.getClient().resource(desiredResourceFlow).serverSideApply();
      }
    });

    return UpdateControl.noUpdate();
  }

  private Glue createGlue(GenericKubernetesResource targetParentResource,
      GlueOperator glueOperator) {
    var glue = new Glue();

    ObjectMeta glueMetadata = glueMetadata(glueOperator, targetParentResource);

    glue.setMetadata(glueMetadata);
    glue.setSpec(toWorkflowSpec(glueOperator.getSpec()));

    if (!defaultGlueLabels.isEmpty()) {
      glue.getMetadata().getLabels().putAll(defaultGlueLabels);
    }

    var parent = glueOperator.getSpec().getParent();
    RelatedResourceSpec parentRelatedSpec =
        parentRelatedResourceSpec(targetParentResource, glueOperator, parent);

    glue.getSpec().getRelatedResources().add(parentRelatedSpec);
    glue.addOwnerReference(targetParentResource);
    return glue;
  }

  private static RelatedResourceSpec parentRelatedResourceSpec(
      GenericKubernetesResource targetParentResource, GlueOperator glueOperator, Parent parent) {
    RelatedResourceSpec parentRelatedSpec = new RelatedResourceSpec();
    parentRelatedSpec.setName(PARENT_RELATED_RESOURCE_NAME);
    parentRelatedSpec.setApiVersion(parent.getApiVersion());
    parentRelatedSpec.setKind(parent.getKind());
    parentRelatedSpec.setResourceNames(List.of(targetParentResource.getMetadata().getName()));
    parentRelatedSpec.setNamespace(targetParentResource.getMetadata().getNamespace());
    parentRelatedSpec.setClusterScoped(glueOperator.getSpec().getParent().isClusterScoped());
    parentRelatedSpec
        .setStatusPatchTemplate(glueOperator.getSpec().getParent().getStatusTemplate());
    parentRelatedSpec.setStatusPatch(glueOperator.getSpec().getParent().getStatus());
    return parentRelatedSpec;
  }

  private ObjectMeta glueMetadata(GlueOperator glueOperator,
      GenericKubernetesResource parent) {

    ObjectMetaBuilder objectMetaBuilder = new ObjectMetaBuilder();

    var glueMeta = glueOperator.getSpec().getGlueMetadata();
    if (glueMeta != null) {
      // optimize
      var data = Map.of(PARENT_RELATED_RESOURCE_NAME, parent);
      var glueName = genericTemplateHandler.processInputAndTemplate(data, glueMeta.getName());
      var glueNamespace =
          genericTemplateHandler.processInputAndTemplate(data, glueMeta.getNamespace());
      objectMetaBuilder.withName(glueName);
      objectMetaBuilder.withNamespace(glueNamespace);
    } else {
      objectMetaBuilder.withName(
          glueName(parent.getMetadata().getName(), parent.getKind()))
          .withNamespace(parent.getMetadata().getNamespace());
    }

    objectMetaBuilder
        .withLabels(Map.of(FOR_GLUE_OPERATOR_LABEL_KEY, FOR_GLUE_OPERATOR_LABEL_VALUE));
    return objectMetaBuilder.build();
  }

  private GlueSpec toWorkflowSpec(GlueOperatorSpec spec) {
    var res = new GlueSpec();
    res.setChildResources(new ArrayList<>(spec.getChildResources()));
    res.setRelatedResources(new ArrayList<>(spec.getRelatedResources()));
    return res;
  }

  private InformerEventSource<GenericKubernetesResource, GlueOperator> getOrRegisterCustomResourceEventSource(
      GlueOperator glueOperator, Context<GlueOperator> context) {
    var spec = glueOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    InformerEventSource<GenericKubernetesResource, GlueOperator> es;
    // note that this allows just one operator per gvk (what is limitation but ok for now)
    try {
      es = (InformerEventSource<GenericKubernetesResource, GlueOperator>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
      es.start();
    } catch (IllegalArgumentException e) {
      var configBuilder = InformerConfiguration.from(gvk,
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration())
          .withSecondaryToPrimaryMapper(
              resource -> Set.of(ResourceID.fromResource(glueOperator)));

      if (spec.getParent().getLabelSelector() != null) {
        configBuilder.withLabelSelector(spec.getParent().getLabelSelector());
      }

      es = new InformerEventSource<>(configBuilder.build(),
          context.eventSourceRetriever().eventSourceContextForDynamicRegistration());
      context.eventSourceRetriever().dynamicallyRegisterEventSource(gvk.toString(), es);
    }
    return es;
  }

  @Override
  public Map<String, EventSource> prepareEventSources(
      EventSourceContext<GlueOperator> eventSourceContext) {
    glueEventSource = new InformerEventSource<>(
        InformerConfiguration.from(Glue.class, eventSourceContext)
            .withLabelSelector(FOR_GLUE_OPERATOR_LABEL_KEY + "=" + FOR_GLUE_OPERATOR_LABEL_VALUE)
            .build(),
        eventSourceContext);
    return EventSourceInitializer.nameEventSources(glueEventSource);
  }

  @Override
  public ErrorStatusUpdateControl<GlueOperator> updateErrorStatus(GlueOperator resource,
      Context<GlueOperator> context, Exception e) {
    if (resource.getStatus() == null) {
      resource.setStatus(new ResourceFlowOperatorStatus());
    }
    return validationAndErrorHandler.updateStatusErrorMessage(e, resource);
  }

  @Override
  public DeleteControl cleanup(GlueOperator glueOperator,
      Context<GlueOperator> context) {
    var spec = glueOperator.getSpec();
    var gvk = new GroupVersionKind(spec.getParent().getApiVersion(), spec.getParent().getKind());
    context.eventSourceRetriever().dynamicallyDeRegisterEventSource(gvk.toString());
    return DeleteControl.defaultDelete();
  }

  public static String glueName(String name, String kind) {
    return KubernetesResourceUtil.sanitizeName(name + "-" + kind);
  }

  private Map<String, String> initDefaultLabelsToAddToGlue() {
    Map<String, String> res = new HashMap<>();
    if (!controllerConfig.glueOperatorManagedGlueLabel().isEmpty()) {
      res.putAll(controllerConfig.glueOperatorManagedGlueLabel());
    } else {
      glueLabelSelector.ifPresent(ls -> {
        if (ls.contains(",") || ls.contains("(")) {
          throw new GlueException(
              "Glue reconciler label selector contains non-simple label selector: " + ls +
                  ". Specify Glue label selector in simple form ('key=value' or 'key') " +
                  "or configure 'glue.operator.glue-operator-managed-glue-label'");
        }
        String[] labelSelectorParts = ls.split("=");
        if (labelSelectorParts.length > 2) {
          throw new GlueException("Invalid label selector: " + ls);
        }
        if (labelSelectorParts.length == 1) {
          res.put(labelSelectorParts[0], "");
        } else {
          res.put(labelSelectorParts[0], labelSelectorParts[1]);
        }
      });
    }
    return res;
  }

}
