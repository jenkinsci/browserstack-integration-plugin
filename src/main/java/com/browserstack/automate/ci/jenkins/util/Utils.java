package com.browserstack.automate.ci.jenkins.util;

import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;

public class Utils {

    public static String generateHash(final String input) {
        // [JENKINS-18178] Older versions of Jenkins produce a package conflict for commons-codec
        // We try to generate a SHA1-HEX hash using java.security libs, fallback to commons-codec

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(input.getBytes("utf8"));
            return DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
        } catch (Exception e) {
            return DigestUtils.sha1Hex(input);
        }
    }
}
