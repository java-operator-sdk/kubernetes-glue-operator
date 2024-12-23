package io.javaoperatorsdk.operator.glue.customresource.operator;

import java.util.Objects;

public class Parent {

  private String apiVersion;
  private String kind;
  private boolean clusterScoped = false;
  private String labelSelector;


  public Parent() {}

  public Parent(String apiVersion, String kind) {
    this.apiVersion = apiVersion;
    this.kind = kind;
  }

  public String getApiVersion() {
    return apiVersion;
  }

  public Parent setApiVersion(String apiVersion) {
    this.apiVersion = apiVersion;
    return this;
  }

  public String getKind() {
    return kind;
  }

  public Parent setKind(String kind) {
    this.kind = kind;
    return this;
  }

  public String getLabelSelector() {
    return labelSelector;
  }

  public void setLabelSelector(String labelSelector) {
    this.labelSelector = labelSelector;
  }

  public boolean isClusterScoped() {
    return clusterScoped;
  }

  public void setClusterScoped(boolean clusterScoped) {
    this.clusterScoped = clusterScoped;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Parent parent = (Parent) o;
    return clusterScoped == parent.clusterScoped && Objects.equals(apiVersion, parent.apiVersion)
        && Objects.equals(kind, parent.kind) && Objects.equals(labelSelector, parent.labelSelector);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, clusterScoped, labelSelector);
  }
}
