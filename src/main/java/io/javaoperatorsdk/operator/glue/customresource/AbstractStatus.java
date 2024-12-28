package io.javaoperatorsdk.operator.glue.customresource;

import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

public class AbstractStatus extends ObservedGenerationAwareStatus {

  private String errorMessage;

  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

}
