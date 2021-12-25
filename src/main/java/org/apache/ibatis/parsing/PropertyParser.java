/*
 *    Copyright 2009-2021 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

import java.util.Properties;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 * @comment chaodong.xi
 * 根据传入的properties替换字符串中的${}占位符
 * 主要方法
 * @see #parse(String, Properties) 通过properties替换字符串中的${}占位符，允许指定默认值如¥{env:dev}这种，如果在proerties中找不到env对应的值则默认赋值为dev
 */
public class PropertyParser {
  //配置属性的前缀
  private static final String KEY_PREFIX = "org.apache.ibatis.parsing.PropertyParser.";
  /**
   * The special property key that indicate whether enable a default value on placeholder.
   * <p>
   * The default value is {@code false} (indicate disable a default value on placeholder)
   * If you specify the {@code true}, you can specify key and default value on placeholder (e.g. {@code ${db.username:postgres}}).
   * </p>
   *
   * @since 3.4.2
   * 是否使用默认值的配置项的名称
   */
  public static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

  /**
   * The special property key that specify a separator for key and default value on placeholder.
   * <p>
   * The default separator is {@code ":"}.
   * </p>
   *
   * @since 3.4.2
   * 默认变量的分隔符的配置项的名称
   */
  public static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";
  //是否使用默认值，默认是false，可通过配置文件设置
  private static final String ENABLE_DEFAULT_VALUE = "false";
  //默认变量的分隔符，默认是':'，可通过配置文件设置
  private static final String DEFAULT_VALUE_SEPARATOR = ":";

  private PropertyParser() {
    // Prevent Instantiation
  }

  public static String parse(String string, Properties variables) {
    VariableTokenHandler handler = new VariableTokenHandler(variables);
    //占位符写死是${}
    GenericTokenParser parser = new GenericTokenParser("${", "}", handler);
    return parser.parse(string);
  }

  private static class VariableTokenHandler implements TokenHandler {
    private final Properties variables;
    private final boolean enableDefaultValue;
    private final String defaultValueSeparator;

    private VariableTokenHandler(Properties variables) {
      this.variables = variables;
      this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE, ENABLE_DEFAULT_VALUE));
      this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
    }

    private String getPropertyValue(String key, String defaultValue) {
      return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
    }

    @Override
    public String handleToken(String content) {
      if (variables != null) {
        String key = content;
        //如果允许使用默认值
        if (enableDefaultValue) {
          //寻找占位符字符串中的分隔符
          final int separatorIndex = content.indexOf(defaultValueSeparator);
          String defaultValue = null;
          //如果能找到
          if (separatorIndex >= 0) {
            //按分隔符切分，前面的为key,后面为默认值
            key = content.substring(0, separatorIndex);
            defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
          }
          //如果有默认值的话，就传个默认值进去
          if (defaultValue != null) {
            return variables.getProperty(key, defaultValue);
          }
        }
        //走到这里说明不允许用默认值，则不传默认值进去
        if (variables.containsKey(key)) {
          return variables.getProperty(key);
        }
      }
      //如果variables没传的话，直接返回不解析的字符串
      return "${" + content + "}";
    }
  }

}
