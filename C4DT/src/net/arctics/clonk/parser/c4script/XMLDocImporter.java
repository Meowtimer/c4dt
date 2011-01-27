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

import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Variable.C4VariableScope;
import net.arctics.clonk.util.StreamUtil;
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

	private static Pattern TITLE_PATTERN = Pattern.compile("\\<title\\>(.*)\\<\\/title\\>"); //$NON-NLS-1$

	public XMLDocImporter() {
		super();
		try {
			parmNameExpr = xPath.compile("./name"); //$NON-NLS-1$
			parmTypeExpr = xPath.compile("./type"); //$NON-NLS-1$
			parmDescExpr = xPath.compile("./desc"); //$NON-NLS-1$
			titleExpr = xPath.compile("./func/title[1]"); //$NON-NLS-1$
			rtypeExpr = xPath.compile("./func/syntax/rtype[1]"); //$NON-NLS-1$
			parmsExpr = xPath.compile("./func/syntax/params/param"); //$NON-NLS-1$
			descExpr = xPath.compile("./func/desc[1]"); //$NON-NLS-1$
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}
		try {
			builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			builder.setEntityResolver(new EntityResolver() {	
				public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
					if (systemId.endsWith("clonk.dtd")) //$NON-NLS-1$
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
			clonkDTD = new InputSource(new FileReader(repositoryPath + "/docs/clonk.dtd")); //$NON-NLS-1$
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public Declaration importFromXML(InputStream stream) throws IOException, XPathExpressionException {

		String text = StreamUtil.stringFromInputStream(stream, "ISO-8859-1"); //$NON-NLS-1$
		// get rid of pesky meta information
		text = text.replaceAll("\\<\\?.*\\?\\>", "").replaceAll("\\<\\!.*\\>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		Document doc;
		try {
			doc = builder.parse(new ByteArrayInputStream(text.getBytes("UTF-8"))); //$NON-NLS-1$
		} catch (SAXException e) {
			Matcher m = TITLE_PATTERN.matcher(text);
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
			Declaration result;
			String name = titleNode.getTextContent();
			if (parmNodes != null && (parmNodes.getLength() > 0 || !Declaration.looksLikeConstName(name))) {
				Function function;
				result = function = new Function();
				for (int i = 0; i < parmNodes.getLength(); i++) {
					Node n = parmNodes.item(i);
					Node nameNode  = (Node) parmNameExpr.evaluate(n, XPathConstants.NODE);
					Node typeNode  = (Node) parmTypeExpr.evaluate(n, XPathConstants.NODE);
					Node descNode_ = (Node) parmDescExpr.evaluate(n, XPathConstants.NODE);
					String typeStr = typeNode != null ? typeNode.getTextContent() : PrimitiveType.ANY.toString();
					if (nameNode != null) {
						Variable parm = new Variable(nameNode.getTextContent(), PrimitiveType.makeType(typeStr));
						if (descNode_ != null)
							parm.setUserDescription(descNode_.getTextContent());
						function.getParameters().add(parm);
					}
				}
			} else {
				result = new Variable(name, PrimitiveType.INT, null, C4VariableScope.CONST);
			}
			result.setName(name);
			((ITypedDeclaration)result).forceType(PrimitiveType.makeType(rTypeNode.getTextContent()));
			if (descNode != null)
				((IHasUserDescription)result).setUserDescription(descNode.getTextContent());
			return result;
		}
		return null;

	}

}
