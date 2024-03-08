package me.thiagorigonatti.rinhadebackendjavacore;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

public class JSONUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();


    public static boolean isValid(final byte[] bytes) {

        if (bytes.length == 0) return false;

        try {
            OBJECT_MAPPER.readTree(bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
