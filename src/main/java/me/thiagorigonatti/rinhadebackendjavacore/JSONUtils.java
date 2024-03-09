package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JSONUtils {

    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    public static boolean isNotValid(final byte[] bytes) {

        if (bytes.length == 0) return true;

        try {
            OBJECT_MAPPER.readTree(bytes);
            return false;
        } catch (IOException e) {
            return true;
        }
    }
}
