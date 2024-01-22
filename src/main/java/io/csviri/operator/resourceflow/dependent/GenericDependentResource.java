package io.csviri.operator.resourceflow.dependent;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.stream.Collectors;

import io.csviri.operator.resourceflow.Utils;
import io.csviri.operator.resourceflow.customresource.resourceflow.ResourceFlow;
import io.csviri.operator.resourceflow.reconciler.ResourceFlowReconciler;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.GroupVersionKind;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.GenericKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.MustacheFactory;

import static io.csviri.operator.resourceflow.reconciler.ResourceFlowOperatorReconciler.*;

public class GenericDependentResource
    extends GenericKubernetesDependentResource<ResourceFlow>
    implements GarbageCollected<ResourceFlow>,
    Updater<GenericKubernetesResource, ResourceFlow>,
    Creator<GenericKubernetesResource, ResourceFlow> {

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final MustacheFactory mustacheFactory = new DefaultMustacheFactory();
  private static final String KEY_TO_ACCESS_PARENT = "parent";
  public static final String WORKFLOW_METADATA_KEY = "workflowMetadata";

  private final GenericKubernetesResource desired;
  private final String desiredTemplate;
  private final String name;

  public GenericDependentResource(GenericKubernetesResource desired, String name) {
    super(new GroupVersionKind(desired.getApiVersion(), desired.getKind()));
    this.desired = desired;
    this.desiredTemplate = null;
    this.name = name;
  }

  public GenericDependentResource(String desiredTemplate, String name) {
    super(new GroupVersionKind(Utils.getApiVersionFromTemplate(desiredTemplate),
        Utils.getKindFromTemplate(desiredTemplate)));
    this.name = name;
    this.desiredTemplate = desiredTemplate;
    this.desired = null;
  }

  @Override
  protected GenericKubernetesResource desired(ResourceFlow primary,
      Context<ResourceFlow> context) {

    var template = desired == null ? desiredTemplate : Serialization.asYaml(desired);
    // this can be precompiled?
    var mustache = mustacheFactory.compile(new StringReader(template), "desired");
    // convert GKR to Map for better access ?
    var mustacheContext = createMustacheContextWithResources(primary, context);

    var res = mustache.execute(new StringWriter(), mustacheContext);

    var resultDesired = Serialization.unmarshal(res.toString(), GenericKubernetesResource.class);

    resultDesired.getMetadata().getAnnotations()
        .put(ResourceFlowReconciler.DEPENDENT_NAME_ANNOTATION_KEY, name);

    // set only for cluster scoped when detection is ready
    if (resultDesired.getMetadata().getNamespace() == null) {
      resultDesired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
    }
    return resultDesired;
  }

  private Map<String, Map> createMustacheContextWithResources(ResourceFlow primary,
      Context<ResourceFlow> context) {
    var actualResourcesByName = Utils.getActualResourcesByNameInWorkflow(context, primary);
    var mustacheContext = actualResourcesByName.entrySet().stream().collect(Collectors
        .toMap(Map.Entry::getKey, e -> objectMapper.convertValue(e.getValue(), Map.class)));
    mustacheContext.put(WORKFLOW_METADATA_KEY,
        objectMapper.convertValue(primary.getMetadata(), Map.class));

    // todo remove if related resources finished
    addPrimaryResourceOfOperatorIfAvailable(context, primary, mustacheContext);
    return mustacheContext;
  }

  private void addPrimaryResourceOfOperatorIfAvailable(Context<ResourceFlow> context,
      ResourceFlow primary,
      Map<String, Map> mustacheContext) {
    var annotations = primary.getMetadata().getAnnotations();
    if (!annotations.containsKey(WATCH_GROUP)) {
      return;
    }

    GroupVersionKind gvk =
        new GroupVersionKind(annotations.get(WATCH_GROUP),
            annotations.get(WATCH_VERSION), annotations.get(WATCH_KIND));

    InformerEventSource<GenericKubernetesResource, ResourceFlow> is = null;
    try {
      is = (InformerEventSource<GenericKubernetesResource, ResourceFlow>) context
          .eventSourceRetriever()
          .getResourceEventSourceFor(GenericKubernetesResource.class, gvk.toString());
    } catch (IllegalArgumentException e) {
      // was not able to find es
    }
    if (is != null) {
      var resource =
          is.get(new ResourceID(annotations.get(WATCH_NAME), primary.getMetadata().getNamespace()));
      resource
          .ifPresent(r -> mustacheContext.put(KEY_TO_ACCESS_PARENT,
              objectMapper.convertValue(r, Map.class)));
    }
  }

  @Override
  public Result<GenericKubernetesResource> match(GenericKubernetesResource actualResource,
      ResourceFlow primary, Context<ResourceFlow> context) {
    return Result.nonComputed(false);
  }
}
