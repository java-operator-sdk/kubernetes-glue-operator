package io.javaoperatorsdk.operator.glue;

import java.util.Map;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "glue.operator")
public interface ControllerConfig {

  Map<String, String> glueOperatorManagedGlueLabel();

  Map<String, String> resourceLabelSelector();

}
