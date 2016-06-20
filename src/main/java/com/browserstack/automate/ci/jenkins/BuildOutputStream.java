package com.browserstack.automate.ci.jenkins;

import hudson.console.LineTransformationOutputStream;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.regex.Pattern;

class BuildOutputStream extends LineTransformationOutputStream {
    private static final char CHAR_MASK = '*';

    private final OutputStream outputStream;
    private final Pattern accessKeyPattern;
    private final String accessKeyMask;

    BuildOutputStream(OutputStream outputStream, String accessKey) {
        boolean hasAccesskey = StringUtils.isNotEmpty(accessKey);
        this.outputStream = outputStream;
        this.accessKeyPattern = hasAccesskey ? Pattern.compile(accessKey) : null;
        this.accessKeyMask = hasAccesskey ? loadAccessKeyMask(accessKey.length()) : null;
    }

    private static String loadAccessKeyMask(int len) {
        char[] maskChars = new char[len];
        Arrays.fill(maskChars, CHAR_MASK);
        return new String(maskChars);
    }

    @Override
    protected void eol(byte[] bytes, int len) throws IOException {
        String line = new String(bytes, 0, len);
        if (accessKeyPattern != null) {
            outputStream.write(accessKeyPattern.matcher(line).replaceAll(accessKeyMask).getBytes());
        } else {
            outputStream.write(bytes, 0, len);
        }
    }
}