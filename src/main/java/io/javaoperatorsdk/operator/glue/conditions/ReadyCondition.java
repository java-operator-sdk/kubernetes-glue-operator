package io.javaoperatorsdk.operator.glue.conditions;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.glue.customresource.glue.Glue;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ReadyCondition<R extends HasMetadata> implements Condition<R, Glue> {

  private final Readiness readiness = Readiness.getInstance();

  private final boolean negated;

  public ReadyCondition(boolean negated) {
    this.negated = negated;
  }


  @Override
  public boolean isMet(DependentResource<R, Glue> dependentResource,
      Glue glue,
      Context<Glue> context) {
    var met = dependentResource.getSecondaryResource(glue, context).map(readiness::isReady)
        .orElse(false);
    return negated != met;
  }
}
