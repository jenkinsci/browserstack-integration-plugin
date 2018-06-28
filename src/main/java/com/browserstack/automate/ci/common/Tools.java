package com.browserstack.automate.ci.common;

import java.util.Arrays;

public class Tools {

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
}
