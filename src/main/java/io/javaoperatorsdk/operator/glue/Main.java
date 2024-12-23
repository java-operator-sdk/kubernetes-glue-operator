package io.javaoperatorsdk.operator.glue;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

// this is just to easier to run the controller because of the bug:
// https://github.com/quarkusio/quarkus/issues/39833
@QuarkusMain
public class Main {
  public static void main(String... args) {
    Quarkus.run(args);
  }
}
