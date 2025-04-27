package io.javaoperatorsdk.operator.glue.conditions;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.glue.templating.GenericTemplateHandler;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class QuteCondition implements Condition<GenericKubernetesResource, Glue> {

  private final GenericTemplateHandler genericTemplateHandler;
  private final String template;

  public QuteCondition(GenericTemplateHandler genericTemplateHandler, String template) {
    this.genericTemplateHandler = genericTemplateHandler;
    this.template = template;
  }

  @Override
  public boolean isMet(DependentResource<GenericKubernetesResource, Glue> dependentResource,
      Glue primary, Context<Glue> context) {
    // TODO
    return false;
  }
}
