package io.javaoperatorsdk.operator.glue.customresource;

import java.util.List;

public class TestCustomResourceSpec {

  private String value;

  private Integer replicas;

  private List<String> listValues;

  public String getValue() {
    return value;
  }

  public TestCustomResourceSpec setValue(String value) {
    this.value = value;
    return this;
  }

  public List<String> getListValues() {
    return listValues;
  }

  public TestCustomResourceSpec setListValues(List<String> listValues) {
    this.listValues = listValues;
    return this;
  }


  public Integer getReplicas() {
    return replicas;
  }

  public void setReplicas(Integer replicas) {
    this.replicas = replicas;
  }
}
