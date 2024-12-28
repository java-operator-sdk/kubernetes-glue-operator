package io.javaoperatorsdk.operator.glue;

public class GlueException extends RuntimeException {

  public GlueException() {}

  public GlueException(String message) {
    super(message);
  }

  public GlueException(String message, Throwable cause) {
    super(message, cause);
  }

  public GlueException(Throwable cause) {
    super(cause);
  }

  public GlueException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
