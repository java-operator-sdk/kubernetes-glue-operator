package io.javaoperatorsdk.operator.glue.conditions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class QuteCondition implements Condition<GenericKubernetesResource, Glue> {

  private static final Logger LOG = LoggerFactory.getLogger(QuteCondition.class);

  private final GenericTemplateHandler genericTemplateHandler;
  private final String template;

  public QuteCondition(GenericTemplateHandler genericTemplateHandler, String template) {
    this.genericTemplateHandler = genericTemplateHandler;
    this.template = template;
  }

  @Override
  public boolean isMet(DependentResource<GenericKubernetesResource, Glue> dependentResource,
      Glue primary, Context<Glue> context) {

    LOG.debug("Evaluating condition with template: {}", template);

    var data = GenericTemplateHandler.createDataWithResources(primary, context);
    data.put("target", GenericTemplateHandler
        .convertToValue(dependentResource.getSecondaryResource(primary, context)));

    var res = genericTemplateHandler.processTemplate(data, template, false);

    LOG.debug("Qute condition result: {}", res);

    return "true".equalsIgnoreCase(res.trim());
  }
}
