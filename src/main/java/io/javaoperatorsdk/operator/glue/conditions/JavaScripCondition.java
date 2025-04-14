package io.javaoperatorsdk.operator.glue.conditions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.utils.Serialization;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.glue.GlueException;
import io.javaoperatorsdk.operator.glue.Utils;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.wasm.QuickJsModule;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import com.dylibso.chicory.runtime.ImportValues;
import com.dylibso.chicory.runtime.Instance;
import com.dylibso.chicory.wasi.WasiOptions;
import com.dylibso.chicory.wasi.WasiPreview1;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JavaScripCondition implements Condition<GenericKubernetesResource, Glue> {

  private static final Logger LOG = LoggerFactory.getLogger(JavaScripCondition.class);

  private final String inputScript;

  public JavaScripCondition(String inputScript) {
    this.inputScript = inputScript;
  }

  @Override
  public boolean isMet(DependentResource<GenericKubernetesResource, Glue> dependentResource,
      Glue glue,
      Context<Glue> context) {
    try (var jsStderr = new ByteArrayOutputStream();
        var wasi = WasiPreview1.builder()
            .withOptions(WasiOptions.builder().withStderr(jsStderr).build()).build()) {
      var start = LocalDateTime.now();

      var quickjs = Instance.builder(QuickJsModule.load())
          .withImportValues(ImportValues.builder().addFunction(wasi.toHostFunctions()).build())
          .withMachineFactory(QuickJsModule::create)
          .build();

      StringBuilder finalScript = new StringBuilder();
      addTargetResourceToScript(dependentResource, glue, context, finalScript);
      addSecondaryResourceToScript(glue, context, finalScript);

      finalScript.append("\n").append(inputScript);

      // Using stderr to return the result
      String finalJsCode = "console.error(eval(`" + finalScript + "`));";
      LOG.debug("Final Condition JS:\n{}", finalJsCode);
      byte[] jsCode = finalJsCode.getBytes(UTF_8);

      var ptr =
          quickjs.export("canonical_abi_realloc")
              .apply(
                  0, // original_ptr
                  0, // original_size
                  1, // alignment
                  jsCode.length // new size
              )[0];

      quickjs.memory().write((int) ptr, jsCode);
      var aggregatedCodePtr = quickjs.export("compile_src").apply(ptr, jsCode.length)[0];

      var codePtr = quickjs.memory().readI32((int) aggregatedCodePtr); // 32 bit
      var codeLength = quickjs.memory().readU32((int) aggregatedCodePtr + 4);

      quickjs.export("eval_bytecode").apply(codePtr, codeLength);

      var res = Boolean.valueOf(jsStderr.toString().trim());
      LOG.debug("JS Condition evaluated as: {} within {}ms", res,
          ChronoUnit.MILLIS.between(start, LocalDateTime.now()));
      return res;
    } catch (IOException e) {
      throw new GlueException(e);
    }
  }

  private static void addSecondaryResourceToScript(Glue glue,
      Context<Glue> context,
      StringBuilder finalScript) {
    Map<String, String> namedSecondaryResources =
        nameAndSerializeSecondaryResources(context, glue);
    namedSecondaryResources.forEach((k, v) -> {
      finalScript.append("const ").append(k).append(" = JSON.parse('").append(v)
          .append("');\n");
    });
  }

  private static void addTargetResourceToScript(
      DependentResource<GenericKubernetesResource, Glue> dependentResource,
      Glue glue,
      Context<Glue> context, StringBuilder finalScript) {
    var target = dependentResource.getSecondaryResource(glue, context);
    target.ifPresent(t -> {
      finalScript.append("const target = JSON.parse('" + Serialization.asJson(t) + "');\n");
    });
  }

  private static Map<String, String> nameAndSerializeSecondaryResources(
      Context<Glue> context, Glue glue) {
    return Utils.getActualResourcesByNameInWorkflow(context, glue).entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, e -> Serialization.asJson(e.getValue())));
  }

}
