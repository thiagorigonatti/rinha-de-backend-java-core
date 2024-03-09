package me.thiagorigonatti.rinhadebackendjavacore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;

public class Rinha {


    private static final Logger LOG = LoggerFactory.getLogger(Rinha.class);

    public static void main(String[] args) throws IOException {

        try (InputStream is = Rinha.class.getClassLoader().getResourceAsStream("logo");
             InputStream is2 = Rinha.class.getClassLoader().getResourceAsStream("version")) {
            if (InetAddress.getLocalHost().getHostName().equals("api01")) {
                LOG.info(new String(is.readAllBytes()).replace("%ver%", new String(is2.readAllBytes())));
                LOG.info("");
            }
        }

        new RinhaServer().start();


    }
}
