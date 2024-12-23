package io.javaoperatorsdk.operator.glue.sample.webpage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({})
@JsonDeserialize(using = com.fasterxml.jackson.databind.JsonDeserializer.None.class)
public class WebPageStatus {
}
