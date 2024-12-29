package io.javaoperatorsdk.operator.glue.templating;

import java.util.HashMap;
import java.util.Map;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.glue.Utils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.quarkus.qute.Engine;
import io.quarkus.qute.Template;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.inject.Singleton;

@Singleton
public class GenericTemplateHandler {

  public static final String WORKFLOW_METADATA_KEY = "glueMetadata";

  private static final ObjectMapper objectMapper = new ObjectMapper();
  private static final Engine engine = Engine.builder().addDefaults().build();

  public String processTemplate(Map<String, Map<?, ?>> data, String template) {
    Template parsedTemplate = engine.parse(template);
    return parsedTemplate.data(data).render();
  }

  public String processInputAndTemplate(Map<String, GenericKubernetesResource> data,
      String template) {
    Map<String, Map<?, ?>> res =
        genericKubernetesResourceDataToGenericData(data);
    return processTemplate(res, template);
  }

  public String processTemplate(String template, Glue primary,
      Context<Glue> context) {
    var data = createDataWithResources(primary, context);
    return processTemplate(data, template);
  }

  private static Map<String, Map<?, ?>> genericKubernetesResourceDataToGenericData(
      Map<String, GenericKubernetesResource> data) {
    Map<String, Map<?, ?>> res = new HashMap<>();
    data.forEach((key, value) -> res.put(key,
        value == null ? null : objectMapper.convertValue(value, Map.class)));
    return res;
  }

  public static Map<String, Map<?, ?>> createDataWithResources(Glue primary,
      Context<Glue> context) {
    Map<String, Map<?, ?>> res = new HashMap<>();
    var actualResourcesByName = Utils.getActualResourcesByNameInWorkflow(context, primary);

    actualResourcesByName.forEach((key, value) -> res.put(key,
        value == null ? null : objectMapper.convertValue(value, Map.class)));

    res.put(WORKFLOW_METADATA_KEY,
        objectMapper.convertValue(primary.getMetadata(), Map.class));

    return res;
  }

  // todo unit test
  @SuppressWarnings("unchecked")
  public static Map<String, ?> parseTemplateToMapObject(String template) {
    return objectMapper.convertValue(template, Map.class);
  }

}
