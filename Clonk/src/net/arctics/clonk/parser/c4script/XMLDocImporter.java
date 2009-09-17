package net.arctics.clonk.parser.c4script;

import java.io.ByteArrayInputStream;


import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.arctics.clonk.parser.C4Declaration;
import net.arctics.clonk.util.Utilities;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLDocImporter {
	private static XMLDocImporter instance;
	public static XMLDocImporter instance() {
		if (instance == null)
			instance = new XMLDocImporter();
		return instance;
	}
	
	private XPath xPath = XPathFactory.newInstance().newXPath();
	private DocumentBuilder builder;
	private String repositoryPath;
	private InputSource clonkDTD;
	
	private XPathExpression parmNameExpr;
	private XPathExpression parmTypeExpr;
	private XPathExpression parmDescExpr;
	private XPathExpression titleExpr;
	private XPathExpression rtypeExpr;
	private XPathExpression parmsExpr;
	private XPathExpression descExpr;
	
	public XMLDocImporter() {
		super();
		try {
			parmNameExpr = xPath.compile("./name");
			parmTypeExpr = xPath.compile("./type");
			parmDescExpr = xPath.compile("./desc");
			titleExpr = xPath.compile("./func/title[1]");
			rtypeExpr = xPath.compile("./func/syntax/rtype[1]");
			parmsExpr = xPath.compile("./func/syntax/params/param");
			descExpr = xPath.compile("./func/desc[1]");
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {	
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (systemId.endsWith("clonk.dtd"))
						return clonkDTD;
					return null;
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getRepositoryPath() {
		return repositoryPath;
	}

	public void setRepositoryPath(String repositoryPath) {
		this.repositoryPath = repositoryPath;
		try {
			clonkDTD = new InputSource(new FileReader(repositoryPath + "/docs/clonk.dtd"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public C4Declaration importFromXML(InputStream stream) throws IOException, XPathExpressionException {
		
		String text = Utilities.stringFromInputStream(stream);
		// get rid of pesky meta information
		text = text.replaceAll("\\<\\?.*\\?\\>", "").replaceAll("\\<\\!.*\\>", "");
		Document doc;
		try {
			doc = builder.parse(new ByteArrayInputStream(text.getBytes("UTF-8")));
		} catch (SAXException e) {
			Matcher m = Pattern.compile("\\<title\\>(.*)\\<\\/title\\>").matcher(text);
			if (m.find()) {
				System.out.println(m.group(1));
			}
			return null;
		}
		
		doc.getFirstChild();
		Node titleNode = (Node) titleExpr.evaluate(doc.getFirstChild(), XPathConstants.NODE);
		Node rTypeNode = (Node) rtypeExpr.evaluate(doc.getFirstChild(), XPathConstants.NODE);
		NodeList parmNodes = (NodeList) parmsExpr.evaluate(doc.getFirstChild(), XPathConstants.NODESET);
		Node descNode = (Node) descExpr.evaluate(doc.getFirstChild(), XPathConstants.NODE);
		
		if (titleNode != null && rTypeNode != null) {
			C4Declaration result;
			String name = titleNode.getTextContent();
			if (parmNodes != null && (parmNodes.getLength() > 0 || !C4Declaration.looksLikeConstName(name))) {
				result = new C4Function();
				C4Function function;
				result = function = new C4Function();
				for (int i = 0; i < parmNodes.getLength(); i++) {
					Node n = parmNodes.item(i);
					Node nameNode  = (Node) parmNameExpr.evaluate(n, XPathConstants.NODE);
					Node typeNode  = (Node) parmTypeExpr.evaluate(n, XPathConstants.NODE);
					Node descNode_ = (Node) parmDescExpr.evaluate(n, XPathConstants.NODE);
					if (nameNode != null && typeNode != null) {
						C4Variable parm = new C4Variable(nameNode.getTextContent(), C4Type.makeType(typeNode.getTextContent()));
						if (descNode_ != null)
							parm.setUserDescription(descNode_.getTextContent());
						function.getParameters().add(parm);
					}
				}
			} else {
				result = new C4Variable();
			}
			result.setName(name);
			((ITypedDeclaration)result).forceType(C4Type.makeType(rTypeNode.getTextContent()));
			if (descNode != null)
				((IHasUserDescription)result).setUserDescription(descNode.getTextContent());
			return result;
		}
		return null;
			
	}
	
}
