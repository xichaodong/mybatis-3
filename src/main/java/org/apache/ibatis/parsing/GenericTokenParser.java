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

/**
 * @author Clinton Begin
 * @comment mybatis中的占位符解析器，作用和spring中的PropertyPlaceholderHelper类似
 * 主要方法
 * @see #parse(String) 主要的解析方法
 */
public class GenericTokenParser {
  /**
   * 占位符的开始符号，比如'${'
   */
  private final String openToken;
  /**
   * 占位符的开始符号，比如'}'
   */
  private final String closeToken;
  /**
   * 具体的处理器，有很多实现类
   * @see PropertyParser
   */
  private final TokenHandler handler;

  public GenericTokenParser(String openToken, String closeToken, TokenHandler handler) {
    this.openToken = openToken;
    this.closeToken = closeToken;
    this.handler = handler;
  }

  //解析字符串
  public String parse(String text) {
    //如果要解析的字符串是空的话就直接返回
    if (text == null || text.isEmpty()) {
      return "";
    }
    //获取占位符的开始位置
    // search open token
    int start = text.indexOf(openToken);
    //如果没有占位符就直接返回
    if (start == -1) {
      return text;
    }
    //把字符串转成字符数组
    char[] src = text.toCharArray();
    int offset = 0;
    //保存解析后的结果
    final StringBuilder builder = new StringBuilder();
    //保存占位符内的字段
    StringBuilder expression = null;
    do {
      //如果占位符的开始字符前面是\\，说明被转义了，那这个开始字符不算
      if (start > 0 && src[start - 1] == '\\') {
        //把从上一个结束字符到\\前面的部分加入到结果字符串中
        // this open token is escaped. remove the backslash and continue.
        builder.append(src, offset, start - offset - 1).append(openToken);
        //更新offset到开始字符
        offset = start + openToken.length();
      } else {
        //如果开始字符没有被转义，则需要开始找结束字符，首先要刷新保存变量的字符串
        // found open token. let's search close token.
        if (expression == null) {
          expression = new StringBuilder();
        } else {
          expression.setLength(0);
        }
        //把从上个结束字符到开始字符之间饿部分加入到保存结果的字符串中
        builder.append(src, offset, start - offset);
        //更新offset
        offset = start + openToken.length();
        //从offset的位置开始找第一个结束字符串
        int end = text.indexOf(closeToken, offset);
        //如果能找到
        while (end > -1) {
          //如果是被转义了的结束字符
          if (end > offset && src[end - 1] == '\\') {
            //和被转义的开始字符的处理方法相同，把\\前面的部分扔到结果字符串里，然后更新offset
            // this close token is escaped. remove the backslash and continue.
            expression.append(src, offset, end - offset - 1).append(closeToken);
            offset = end + closeToken.length();
            //继续进行结束字符的查找
            end = text.indexOf(closeToken, offset);
          } else {
            //如果是没有被转义的结束字符的话，说明已经找到一对合法的占位符，则加到保存占位符变量的字符串中
            expression.append(src, offset, end - offset);
            break;
          }
        }
        //如果找不到的话，说明后面不需要再解析了，直接把剩余的部分全更新到结果里
        if (end == -1) {
          // close token was not found.
          builder.append(src, start, src.length - start);
          offset = src.length;
        } else {
          //如果不是-1，说明找到了一对合法的占位符，则扔到预先设置的handler中处理
          builder.append(handler.handleToken(expression.toString()));
          //更新offset到结束字符的位置
          offset = end + closeToken.length();
        }
      }
      //进行下一轮占位符的查找和替换
      start = text.indexOf(openToken, offset);
    } while (start > -1);
    //把最后一段也加入到结果字符串中
    if (offset < src.length) {
      builder.append(src, offset, src.length - offset);
    }
    //返回解析后的字符串
    return builder.toString();
  }
}
