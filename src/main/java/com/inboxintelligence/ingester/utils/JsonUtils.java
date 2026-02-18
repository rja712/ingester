package com.inboxintelligence.ingester.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JsonUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private static String toPrettyJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }


}
