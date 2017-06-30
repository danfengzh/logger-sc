package com.github.ranchub.logger.impl;

import com.github.ranchub.logger.Logger;
import com.github.ranchub.logger.Utils;
import com.github.ranchub.logger.inter.LogAdapter;
import com.github.ranchub.logger.inter.Printer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;


public class LoggerPrinter implements Printer {

  /**
   * It is used for json pretty print
   */
  private static final int JSON_INDENT = 2;

  private final List<LogAdapter> logAdapters = new ArrayList<>();


  @Override public void d(String message, Object... args) {
    log(Logger.DEBUG, null, message, args);
  }

  @Override public void d(Object object) {
    log(Logger.DEBUG, null, Utils.toString(object));
  }

  @Override public void e(String message, Object... args) {
    e(null, message, args);
  }

  @Override public void e(Throwable throwable, String message, Object... args) {
    log(Logger.ERROR, throwable, message, args);
  }

  @Override public void w(String message, Object... args) {
    log(Logger.WARN, null, message, args);
  }

  @Override public void i(String message, Object... args) {
    log(Logger.INFO, null, message, args);
  }

  @Override public void v(String message, Object... args) {
    log(Logger.VERBOSE, null, message, args);
  }

  @Override public void json(String json) {
    if (Utils.isEmpty(json)) {
      d("Empty/Null json content");
      return;
    }
    try {
      json = json.trim();
      if (json.startsWith("{")) {
        JSONObject jsonObject = new JSONObject(json);
        String message = jsonObject.toString(JSON_INDENT);
        d(message);
        return;
      }
      if (json.startsWith("[")) {
        JSONArray jsonArray = new JSONArray(json);
        String message = jsonArray.toString(JSON_INDENT);
        d(message);
        return;
      }
      e("Invalid Json");
    } catch (JSONException e) {
      e("Invalid Json");
    }
  }

  @Override public void xml(String xml) {
    if (Utils.isEmpty(xml)) {
      d("Empty/Null xml content");
      return;
    }
    try {
      Source xmlInput = new StreamSource(new StringReader(xml));
      StreamResult xmlOutput = new StreamResult(new StringWriter());
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      transformer.transform(xmlInput, xmlOutput);
      d(xmlOutput.getWriter().toString().replaceFirst(">", ">\n"));
    } catch (TransformerException e) {
      e("Invalid xml");
    }
  }

  @Override public synchronized void log(int priority, String message, Throwable throwable) {
    if (throwable != null && message != null) {
      message += " : " + Utils.getStackTraceString(throwable);
    }
    if (throwable != null && message == null) {
      message = Utils.getStackTraceString(throwable);
    }
    if (Utils.isEmpty(message)) {
      message = "Empty/NULL log message";
    }

    for (LogAdapter adapter : logAdapters) {
      if (adapter.isLoggable(priority)) {
        adapter.log(priority, message);
      }
    }
  }

  @Override public void clearLogAdapters() {
    logAdapters.clear();
  }

  @Override public void addAdapter(LogAdapter adapter) {
    logAdapters.add(adapter);
  }

  /**
   * This method is synchronized in order to avoid messy of logs' order.
   */
  private synchronized void log(int priority, Throwable throwable, String msg, Object... args) {
    String message = createMessage(msg, args);
    log(priority, message, throwable);
  }


  private String createMessage(String message, Object... args) {
    return args == null || args.length == 0 ? message : String.format(message, args);
  }
}