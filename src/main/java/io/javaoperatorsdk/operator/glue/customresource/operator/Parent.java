package io.javaoperatorsdk.operator.glue.customresource.operator;

import java.util.Objects;

import io.fabric8.crd.generator.annotation.PreserveUnknownFields;

public class Parent {

  private String apiVersion;
  private String kind;
  private boolean clusterScoped = false;
  private String labelSelector;

  @PreserveUnknownFields
  private Object status;
  private String statusTemplate;

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

  public Object getStatus() {
    return status;
  }

  public void setStatus(Object status) {
    this.status = status;
  }

  public String getStatusTemplate() {
    return statusTemplate;
  }

  public void setStatusTemplate(String statusTemplate) {
    this.statusTemplate = statusTemplate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    Parent parent = (Parent) o;
    return clusterScoped == parent.clusterScoped && Objects.equals(apiVersion, parent.apiVersion)
        && Objects.equals(kind, parent.kind) && Objects.equals(labelSelector, parent.labelSelector)
        && Objects.equals(status, parent.status)
        && Objects.equals(statusTemplate, parent.statusTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiVersion, kind, clusterScoped, labelSelector, status, statusTemplate);
  }
}
