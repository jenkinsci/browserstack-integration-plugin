package com.browserstack.automate.ci.common;

import org.apache.commons.lang.RandomStringUtils;

import hudson.FilePath;
import hudson.model.Run;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

public class Tools {

    public static final Pattern BUILD_URL_PATTERN = Pattern.compile("(https?:\\/\\/[\\w-.]+\\/builds\\/\\w+)\\/sessions\\/\\w+");
    public static final SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy, HH:mm");
    public static final DateFormat SESSION_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH);

    /**
     * Returns a string with only '*' of length equal to the length of the inputStr
     *
     * @param strToMask
     * @return masked string
     */
    public static String maskString(String strToMask) {
        char[] maskChars = new char[strToMask.length()];
        Arrays.fill(maskChars, '*');
        return new String(maskChars);
    }

    /**
     * Returns human readable form
     *
     * @param duration in seconds
     * @return
     */
    public static String durationToHumanReadable(long duration) {
        String result = "";
        int seconds = (int) duration % 60;
        duration /= 60;
        int minutes = (int) duration % 60;
        duration /= 60;
        int hours = (int) duration % 24;
        int days = (int) duration / 24;

        if (days == 0) {
            if (hours == 0) {
                if (minutes == 0) {
                    result = String.format("%02ds", seconds);
                } else {
                    result = String.format("%02dm %02ds", minutes, seconds);
                }
            } else {
                result = String.format("%02dh %02dm %02ds", hours, minutes, seconds);
            }
        } else {
            result = String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        }
        return result;
    }

    public static String getUniqueString(boolean letters, boolean numbers) {
        return RandomStringUtils.random(48, letters, numbers);
    }

    /** Gets the directory to store report files */
    public static FilePath getBrowserStackReportDir(Run<?, ?> build, String dirName) {
        return new FilePath(new File(build.getRootDir(), dirName));
    }

    public static String getStackTraceAsString(Throwable throwable) {
        try {
            StringWriter stringWriter = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stringWriter));
            return stringWriter.toString();
        } catch(Throwable e) {
            return throwable != null ? throwable.toString() : "";
        }
    }
}
