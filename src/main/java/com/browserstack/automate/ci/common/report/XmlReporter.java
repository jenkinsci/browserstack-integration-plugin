package com.browserstack.automate.ci.common.report;

import com.browserstack.automate.ci.common.model.BrowserStackSession;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Shirish Kamath
 * @author Anirudha Khanna
 */
public class XmlReporter {

  public static Map<String, String> parse(File f) throws IOException {
    Map<String, String> testSessionMap = new HashMap<String, String>();
    Document doc;

    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      doc = dBuilder.parse(f);
    } catch (Exception e) {
      throw new IOException(e.getMessage());
    }

    Element documentElement = doc.getDocumentElement();
    NodeList testCaseNodes = documentElement.getElementsByTagName("testcase");

    for (int i = 0; i < testCaseNodes.getLength(); i++) {
      Node n = testCaseNodes.item(i);

      if (n.getNodeType() == Node.ELEMENT_NODE) {
        Element el = (Element) n;
        if (el.hasAttribute("id") && el.hasChildNodes()) {
          String testId = el.getAttribute("id");
          NodeList sessionNode = el.getElementsByTagName("session");
          if (sessionNode.getLength() > 0
              && sessionNode.item(0).getNodeType() == Node.ELEMENT_NODE) {
            NodeList projectTypeNode = el.getElementsByTagName("projectType");
            String projectType =
                projectTypeNode.getLength() > 0 ? projectTypeNode.item(0).getTextContent() : "";
            Gson gson = new GsonBuilder().create();

            BrowserStackSession session =
                new BrowserStackSession(sessionNode.item(0).getTextContent(), projectType);
            testSessionMap.put(testId, gson.toJson(session));
          }
        }
      }
    }

    return testSessionMap;
  }
}
