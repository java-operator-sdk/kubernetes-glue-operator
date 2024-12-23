package io.javaoperatorsdk.operator.glue.customresource.glue.condition;

public class ReadyConditionSpec extends ConditionSpec {

  private final boolean negated;

  public ReadyConditionSpec(boolean negated) {
    this.negated = negated;
  }

  public ReadyConditionSpec() {
    this(false);
  }

  public boolean isNegated() {
    return negated;
  }
}
