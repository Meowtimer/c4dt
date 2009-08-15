package net.arctics.clonk.parser.c4script;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.arctics.clonk.parser.C4Declaration;

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
	
	public XMLDocImporter() {
		super();
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

	public C4Declaration importFromXML(InputStream stream) throws SAXException, IOException, XPathExpressionException {
		
		Document doc = builder.parse(stream);
		
		doc.getFirstChild();
		Node titleNode = (Node) xPath.evaluate("./funcs/func/title[1]", doc.getFirstChild(), XPathConstants.NODE);
		Node rTypeNode = (Node) xPath.evaluate("./funcs/func/syntax/rtype[1]", doc.getFirstChild(), XPathConstants.NODE);
		NodeList parmNodes = (NodeList) xPath.evaluate("./funcs/func/syntax/params/param", doc.getFirstChild(), XPathConstants.NODESET);
		Node descNode = (Node) xPath.evaluate("./funcs/func/desc[1]", doc.getFirstChild(), XPathConstants.NODE);
		
		if (titleNode != null && rTypeNode != null) {
			C4Declaration result = (parmNodes != null) ? new C4Function() : new C4Variable();
			result.setName(titleNode.getTextContent());
			((ITypedDeclaration)result).forceType(C4Type.makeType(rTypeNode.getTextContent()));
			if (parmNodes != null) {
				C4Function function = (C4Function) result;
				for (int i = 0; i < parmNodes.getLength(); i++) {
					Node n = parmNodes.item(i);
					Node nameNode  = (Node) xPath.evaluate("./name", n, XPathConstants.NODE);
					Node typeNode  = (Node) xPath.evaluate("./type", n, XPathConstants.NODE);
					Node descNode_ = (Node) xPath.evaluate("./desc", n, XPathConstants.NODE);
					if (nameNode != null && typeNode != null) {
						C4Variable parm = new C4Variable(nameNode.getTextContent(), C4Type.makeType(typeNode.getTextContent()));
						if (descNode_ != null)
							parm.setUserDescription(descNode_.getTextContent());
						function.getParameters().add(parm);
					}
				}
			}
			if (descNode != null)
				((IHasUserDescription)result).setUserDescription(descNode.getTextContent());
			return result;
		}
		return null;
			
	}
	
}
