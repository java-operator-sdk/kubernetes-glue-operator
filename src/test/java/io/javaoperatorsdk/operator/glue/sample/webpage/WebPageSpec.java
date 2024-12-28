package io.javaoperatorsdk.operator.glue.sample.webpage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"exposed", "html"})
public class WebPageSpec {

  private Boolean exposed;

  private String html;

  public Boolean getExposed() {
    return exposed;
  }

  public void setExposed(Boolean exposed) {
    this.exposed = exposed;
  }

  public String getHtml() {
    return html;
  }

  public void setHtml(String html) {
    this.html = html;
  }
}
