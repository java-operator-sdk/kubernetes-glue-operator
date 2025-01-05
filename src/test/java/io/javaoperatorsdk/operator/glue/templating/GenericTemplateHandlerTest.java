package io.javaoperatorsdk.operator.glue.templating;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.fabric8.kubernetes.client.utils.Serialization;

import static org.junit.jupiter.api.Assertions.*;

class GenericTemplateHandlerTest {

  GenericTemplateHandler templateHandler = new GenericTemplateHandler();

  @Test
  void testDoubleCurlyBrackets() {
    var template = """
          intValue: "{{spec.intvalue}}"
          stringValue: "{spec.stringvalue}"
        """;

    Map<String, Map<?, ?>> data = new HashMap<>();

    Map values = new HashMap();
    values.put("intvalue", 1);
    values.put("stringvalue", "value1");
    data.put("spec", values);

    var result = templateHandler.processTemplate(data, template, true);

    Map mapResult = Serialization.unmarshal(result, Map.class);

    assertEquals(1, mapResult.get("intValue"));
    assertEquals("value1", mapResult.get("stringValue"));
  }

}
