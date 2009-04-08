package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IniData {
	
	private final static Map<Class<?>, IEntryFactory> cachedFactories = new HashMap<Class<?>, IEntryFactory>(3);
	
	
	public static class IniConfiguration {
		private String filename;
		private Map<String, IniDataSection> sections = new HashMap<String, IniDataSection>();
		private IEntryFactory factory = null;
		
		protected IniConfiguration() {
		}
		
		public static IniConfiguration createByXML(Node fileNode) throws InvalidIniConfigurationException {
			IniConfiguration conf = new IniConfiguration();
			if (fileNode.getAttributes() == null || 
					fileNode.getAttributes().getLength() < 2 || 
					fileNode.getAttributes().getNamedItem("name") == null ||
					fileNode.getAttributes().getNamedItem("factoryclass") == null) {
				throw new InvalidIniConfigurationException("A <file> tag must have a name=\"\" and a factoryclass=\"\" attribute");
			}
			conf.filename = fileNode.getAttributes().getNamedItem("name").getNodeValue();
			try {
				Class<?> configClass = Class.forName(fileNode.getAttributes().getNamedItem("factoryclass").getNodeValue());
				if (!cachedFactories.containsKey(configClass)) {
					if (IEntryFactory.class.isAssignableFrom(configClass))
						cachedFactories.put(configClass, (IEntryFactory) configClass.newInstance());
					else
						throw new InvalidIniConfigurationException("Value of 'factorymethod' in file declaration '" + conf.filename + "' is not a subtype of IEntryFactory");;
				}
				conf.factory = cachedFactories.get(configClass);
			}  catch (ClassNotFoundException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}

			NodeList sectionNodes = fileNode.getChildNodes();
			for(int i = 0;i < sectionNodes.getLength();i++) {
				if (sectionNodes.item(i).getNodeName() == "section") {
					IniDataSection section = IniDataSection.createByXML(sectionNodes.item(i), conf.factory);
					conf.getSections().put(section.getSectionName(), section);
				}
			}
			return conf;
		}

		public String getFilename() {
			return filename;
		}

		public Map<String, IniDataSection> getSections() {
			return sections;
		}
		
		public boolean hasSection(String sectionName) {
			return sections.containsKey(sectionName);
		}
		
		public String[] getSectionNames() {
			return sections.keySet().toArray(new String[sections.size()]);
		}

		public IEntryFactory getFactory() {
			return factory;
		}
		
	}
	
	public static class IniDataSection {
		private String sectionName;
		private Map<String, IniDataEntry> entries = new HashMap<String, IniDataEntry>();		
		
		protected IniDataSection() {
		}
		
		public static IniDataSection createByXML(Node sectionNode, IEntryFactory factory) throws InvalidIniConfigurationException {
			IniDataSection section = new IniDataSection();
			if (sectionNode.getAttributes() == null || 
					sectionNode.getAttributes().getLength() == 0 || 
					sectionNode.getAttributes().getNamedItem("name") == null) {
				throw new InvalidIniConfigurationException("A <section> tag must have a name=\"\" attribute");
			}
			section.sectionName = sectionNode.getAttributes().getNamedItem("name").getNodeValue();
			// TODO implement 'optional' <section> attribute
			NodeList entryNodes = sectionNode.getChildNodes();
			for(int i = 0; i < entryNodes.getLength();i++) {
				if (entryNodes.item(i).getNodeName() == "entry") {
					IniDataEntry entry = IniDataEntry.createByXML(entryNodes.item(i), factory);
					section.getEntries().put(entry.getEntryName(), entry);
				}
			}
			return section;
		}
		
		public String getSectionName() {
			return sectionName;
		}

		public Map<String, IniDataEntry> getEntries() {
			return entries;
		}
		
		public boolean hasEntry(String entryName) {
			return entries.containsKey(entryName);
		}
		
		public String[] getEntryNames() {
			return entries.keySet().toArray(new String[entries.size()]);
		}
		
	}
	
	public static class IniDataEntry {
		protected String entryName;
		protected Class<?> entryClass;
		protected String entryDescription;
		
		protected IniDataEntry() {
		}
		
		
		public static IniDataEntry createByXML(Node entryNode, IEntryFactory factory) throws InvalidIniConfigurationException {
			IniDataEntry entry = new IniDataEntry();
			if (entryNode.getAttributes() == null || 
					entryNode.getAttributes().getLength() < 2 || 
					entryNode.getAttributes().getNamedItem("name") == null ||
					entryNode.getAttributes().getNamedItem("class") == null) {
				throw new InvalidIniConfigurationException("An <entry> tag must have a 'name=\"\"' and a 'class=\"\"' attribute");
			}
			entry.entryName = entryNode.getAttributes().getNamedItem("name").getNodeValue();
			try {
				Class<?> configClass = Class.forName(entryNode.getAttributes().getNamedItem("class").getNodeValue());
					entry.entryClass = configClass;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			if (entryNode.getAttributes().getNamedItem("description") != null) {
				entry.entryDescription = entryNode.getAttributes().getNamedItem("description").getNodeValue();
			}
			return entry;
		}
		
		public String getEntryName() {
			return entryName;
		}

		public Class<?> getEntryClass() {
			return entryClass;
		}
		public String getDescription() {
			return entryDescription;
		}
		
		public void setEntryClass(Class<?> cls) {
			entryClass = cls;
		}
		public void setEntryName(String name) {
			entryName = name;
		}
		public void setDescription(String desc) {
			entryDescription = desc;
		}
		
	}
	
	private InputStream xmlFile;
	private Map<String, IniConfiguration> configurations = new HashMap<String, IniConfiguration>(4);
	
	public IniData(InputStream stream) {
		xmlFile = stream;
	}
	
	/**
	 * Returns the configuration that is declared for files with name of <tt>filename</tt>.
	 * @param filename including extension
	 * @return the configuration or <tt>null</tt>
	 */
	public IniConfiguration getConfigurationFor(String filename) {
		return configurations.get(filename);
	}
	
	public void parse() {
		try {			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xmlFile);
			doc.getDocumentElement().normalize();
			if (!doc.getDocumentElement().getNodeName().equals("clonkiniconfig")) {
				throw new ParserConfigurationException("Invalid xml document. Wrong root node '" + doc.getDocumentElement().getNodeName() + "'.");
			}
			NodeList nodeList = doc.getElementsByTagName("file");
			for (int i = 0; i < nodeList.getLength();i++) {
				try {
					IniConfiguration conf = IniConfiguration.createByXML(nodeList.item(i));
					configurations.put(conf.getFilename(), conf);
				}
				catch (InvalidIniConfigurationException e) {
					e.printStackTrace();
				}
			}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	public static Class<?> entryClassFromDocumentation(String docType) {
//		if (docType.equals("Integer"))
//			return SignedInteger.class;
//		if (docType.endsWith("Integer"))
//			return IntegerArray.class;
//		if (docType.startsWith("Zeichenfolge"))
//			return String.class;
//		if (docType.equalsIgnoreCase("id"))
//			return C4ID.class;
//		return null;
//	}
//	
//	public static void xmlFromText(InputStream input, PrintStream output) throws IOException {
//		BufferedScanner scanner = new BufferedScanner(input);
//		for (String name = scanner.readWord(); name != null && name.length() > 0; name = scanner.readWord()) {
//			scanner.eatWhitespace();
//			String type = scanner.readWord();
//			scanner.eatWhitespace();
//			if (Character.isDigit(type.charAt(0))) {
//				type = type + " " + scanner.readWord();
//				scanner.eatWhitespace();
//			}
//			String desc = scanner.readStringUntil(BufferedScanner.NEWLINE_DELIMITERS);
//			scanner.eatWhitespace();
//			Class<?> entryClass = entryClassFromDocumentation(type);
//			output.println("<entry name=\""+name+"\" class=\""+entryClass.getName()+"\" description=\""+desc+"\" />");
//		}
//	}
//	
//	static {
//		try {
//			xmlFromText(new FileInputStream("/Users/madeen/Desktop/material.txt"), System.out);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
}

