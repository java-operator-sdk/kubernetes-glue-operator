package io.javaoperatorsdk.operator.glue.conditions;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.glue.GlueException;
import io.javaoperatorsdk.operator.glue.Utils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class JavaScripCondition implements Condition<GenericKubernetesResource, Glue> {

  private static final Logger LOG = LoggerFactory.getLogger(JavaScripCondition.class);

  private static final String RESOURCE_AS_STRING_NAME_SUFFIX = "Str";

  private final String inputScript;

  public JavaScripCondition(String inputScript) {
    this.inputScript = inputScript;
  }

  @Override
  public boolean isMet(DependentResource<GenericKubernetesResource, Glue> dependentResource,
      Glue glue,
      Context<Glue> context) {
    try {
      var start = LocalDateTime.now();
      ScriptEngineManager manager = new ScriptEngineManager();
      ScriptEngine engine = manager.getEngineByName("js");

      StringBuilder finalScript = new StringBuilder();
      addTargetResourceToScript(dependentResource, glue, context, engine, finalScript);
      addSecondaryResourceToScript(glue, context, engine, finalScript);

      finalScript.append("\n").append(inputScript);

      LOG.debug("Final Condition JS:\n{}", finalScript);

      CompiledScript script = ((Compilable) engine).compile(finalScript.toString());
      var res = (boolean) script.eval();
      LOG.debug("JS Condition evaluated as: {} within {}ms", res,
          ChronoUnit.MILLIS.between(start, LocalDateTime.now()));
      return res;
    } catch (ScriptException e) {
      throw new GlueException(e);
    }
  }

  private static void addSecondaryResourceToScript(Glue glue,
      Context<Glue> context,
      ScriptEngine engine, StringBuilder finalScript) {
    Map<String, String> namedSecondaryResources =
        nameAndSerializeSecondaryResources(context, glue);
    namedSecondaryResources.forEach((k, v) -> {
      var stringKey = k + RESOURCE_AS_STRING_NAME_SUFFIX;
      engine.put(stringKey, v);
      finalScript.append("const ").append(k).append(" = JSON.parse(").append(stringKey)
          .append(");\n");
    });
  }

  private static void addTargetResourceToScript(
      DependentResource<GenericKubernetesResource, Glue> dependentResource,
      Glue glue,
      Context<Glue> context, ScriptEngine engine, StringBuilder finalScript) {
    var target = dependentResource.getSecondaryResource(glue, context);
    target.ifPresent(t -> {
      engine.put("targetStr", Serialization.asJson(t));
      finalScript.append("const target = JSON.parse(targetStr);\n");
    });
  }

  private static Map<String, String> nameAndSerializeSecondaryResources(
      Context<Glue> context, Glue glue) {
    return Utils.getActualResourcesByNameInWorkflow(context, glue).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Serialization.asJson(e.getValue())));
  }

}
