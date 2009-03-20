package net.arctics.clonk.parser.actmap;

import org.eclipse.core.resources.IFile;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.inireader.IniReader;
import net.arctics.clonk.parser.inireader.IniData.IniConfiguration;

public class ActMapParser extends IniReader {

	public static final String ACTION_SECTION = "Action";
	
	private final IniConfiguration configuration = ClonkCore.getDefault().INI_CONFIGURATIONS.getConfigurationFor("ActMap.txt");
	
	@Override
	protected IniConfiguration getConfiguration() {
		return configuration;
	}
	
	public ActMapParser(IFile file) {
		super(file);
	}
	
//	public static void printActMapEntryList() throws ParserConfigurationException, MalformedURLException, SAXException, IOException {
//		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//		DocumentBuilder builder = factory.newDocumentBuilder();
//		//Document doc = builder.parse(new URL("http", "www.clonk.de", 80, "docs/de/sdk/definition/actmap.html").openStream());
//		Document doc = builder.parse(new FileInputStream("/Users/madeen/Desktop/actmap.html"));
//		NodeList tables = doc.getElementsByTagName("table");
//		Node table = tables.item(0);
//		OuterLoop: for (Node n = table.getFirstChild(); n != null; n = n.getNextSibling()) {
//			if (n.getNodeName().equals("tr")) {
//				Node value;
//				Node type;
//				Node description;
//				value = type = description = null;
//				for (Node i = n.getFirstChild(); i != null && description == null; i = i.getNextSibling()) {
//					if (i.getNodeName().equals("td")) {
//						if (value == null)
//							value = i;
//						else if (type == null)
//							type = i;
//						else if (description == null)
//							description = i;
//					}
//					else if (i.getNodeName().equals("th"))
//						continue OuterLoop;
//				}
//				String valueClass;
//				String typeString = type.getFirstChild().getTextContent();
//				if (typeString.contains("Zeichenfolge")) {
//					valueClass = String.class.getName();
//				}
//				else if (typeString.equals("Integer") || typeString.equals("Boolean")) {
//					valueClass = SignedInteger.class.getName();
//				}
//				else if (typeString.equals("6 Integer")) {
//					valueClass = IntegerArray.class.getName();
//				}
//				else
//					valueClass = "whatever";
//				System.out.println("\t\t\t<entry name=\""+value.getFirstChild().getTextContent()+"\" class=\""+valueClass+"\" description=\""+description.getFirstChild().getTextContent()+"\"/>");
//			}
//		}
//	}

}
