package io.csviri.operator.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.javaoperatorsdk.operator.Operator;

public class Runner {

  private static final Logger log = LoggerFactory.getLogger(Runner.class);

  public static void main(String[] args) {
    Operator operator = new Operator();
    operator.register(new WorkflowReconciler());
    operator.start();
    log.info("Workflow controller started.");
  }
}
