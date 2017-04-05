package org.graylog2.plugins.slack;

import java.util.Map;

public final class StringReplacement {
  private static final String DEFAULT_VALUE_DELIMITER = ":-";
  /**
    * Replaces all the occurrences of variables in the given source object with
    * their matching values from the map.
    * <p>
    * Variable format is ${expr[:-default]}.
    *
    * @param source  the source text containing the variables to substitute, null returns null
    * @param valueMap  the map with the values, may be null
    * @return the result of the replace operation    
    */
  public static String replace(String source, Map<String, Object> valueMap) {
    return replaceWithPrefix(source, null, valueMap);
  }

  /**
    * Replaces all the occurrences of variables in the given source object with
    * their matching values from the map. And add {@code prefix} string to the value
    * <p>
    * Variable format is ${expr[:-default]}.
    *
    * @param source  the source text containing the variables to substitute, null returns null
    * @param prefix prefix string
    * @param valueMap  the map with the values, may be null
    * @return the result of the replace operation    
    */
  public static String replaceWithPrefix(String source, String prefix, Map<String, Object> valueMap) {
    if (source == null || valueMap == null)
      return source;
    StringBuilder sb = new StringBuilder();
    char[] strArray = source.toCharArray();
    int i = 0;
    while (i < strArray.length - 1) {
      if (strArray[i] == '$' && strArray[i + 1] == '{') {
        i = i + 2;
        int begin = i;
        while (strArray[i] != '}')
          ++i;
        String[] exprs = findDefaultValue(source.substring(begin, i++));
        Object value = valueMap.get(exprs[0]);
        if (value == null && exprs.length > 1)
          value = exprs[1];
        if (value == null || "".equals(value))
          sb.append("");
        else {
          if (prefix != null)
            sb.append(prefix);
          sb.append(value);
        }
      } else {
        sb.append(strArray[i]);
        ++i;
      }
    }
    if (i < strArray.length)
      sb.append(strArray[i]);
    return sb.toString();
  }

  private static String[] findDefaultValue(String exprString) {
    if (exprString.endsWith(DEFAULT_VALUE_DELIMITER))
      return new String[] { exprString.substring(0, exprString.length() - DEFAULT_VALUE_DELIMITER.length()), ""};
    return exprString.split(DEFAULT_VALUE_DELIMITER);
  }
}
