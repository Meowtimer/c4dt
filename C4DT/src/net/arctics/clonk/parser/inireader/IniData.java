package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
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
		
		public static IniConfiguration createFromXML(Node fileNode) throws InvalidIniConfigurationException {
			IniConfiguration conf = new IniConfiguration();
			if (fileNode.getAttributes() == null || 
					fileNode.getAttributes().getLength() < 2 || 
					fileNode.getAttributes().getNamedItem("name") == null || //$NON-NLS-1$
					fileNode.getAttributes().getNamedItem("factoryclass") == null) { //$NON-NLS-1$
				throw new InvalidIniConfigurationException("A <file> tag must have a name=\"\" and a factoryclass=\"\" attribute"); //$NON-NLS-1$
			}
			conf.filename = fileNode.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
			try {
				Class<?> configClass = Class.forName(fileNode.getAttributes().getNamedItem("factoryclass").getNodeValue()); //$NON-NLS-1$
				if (!cachedFactories.containsKey(configClass)) {
					if (IEntryFactory.class.isAssignableFrom(configClass))
						cachedFactories.put(configClass, (IEntryFactory) configClass.newInstance());
					else
						throw new InvalidIniConfigurationException("Value of 'factorymethod' in file declaration '" + conf.filename + "' is not a subtype of IEntryFactory");; //$NON-NLS-1$ //$NON-NLS-2$
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
				if (sectionNodes.item(i).getNodeName() == "section") { //$NON-NLS-1$
					IniDataSection section = IniDataSection.createFromXML(sectionNodes.item(i), conf.factory);
					conf.getSections().put(section.getSectionName(), section);
				}
			}
			return conf;
		}
		
		public static IniConfiguration createFromClass(Class<?> clazz) {
			IniConfiguration result = new IniConfiguration();
			for (Field f : clazz.getFields()) {
				IniField annotation;
				if ((annotation = f.getAnnotation(IniField.class)) != null) {
					IniDataSection section = result.getSections().get(annotation.category());
					if (section == null) {
						section = new IniDataSection();
						section.sectionName = annotation.category();
						result.sections.put(annotation.category(), section);
					}
					section.entries.put(f.getName(), new IniDataEntry(f.getName(), f.getType()));
				}
			}
			result.factory = new GenericEntryFactory();
			return result;
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
	
	public static final class IniDataSection {
		private String sectionName;
		private Map<String, IniDataEntry> entries = new HashMap<String, IniDataEntry>();		
		
		protected IniDataSection() {
		}
		
		public static IniDataSection createFromXML(Node sectionNode, IEntryFactory factory) throws InvalidIniConfigurationException {
			IniDataSection section = new IniDataSection();
			if (sectionNode.getAttributes() == null || 
					sectionNode.getAttributes().getLength() == 0 || 
					sectionNode.getAttributes().getNamedItem("name") == null) { //$NON-NLS-1$
				throw new InvalidIniConfigurationException("A <section> tag must have a name=\"\" attribute"); //$NON-NLS-1$
			}
			section.sectionName = sectionNode.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
			// TODO implement 'optional' <section> attribute
			NodeList entryNodes = sectionNode.getChildNodes();
			for(int i = 0; i < entryNodes.getLength();i++) {
				if (entryNodes.item(i).getNodeName() == "entry") { //$NON-NLS-1$
					IniDataEntry entry = IniDataEntry.createFromXML(entryNodes.item(i), factory);
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

		public IniDataEntry getEntry(String key) {
			return getEntries().get(key);
		}
		
	}
	
	public static final class IniDataEntry {
		protected String entryName;
		protected Class<?> entryClass;
		protected String entryDescription;
		protected Object extraData;
		
		protected IniDataEntry() {
		}
		
		protected IniDataEntry(String name, Class<?> valueType) {
			entryName = name;
			if (valueType == String.class)
				entryClass = valueType;
			else if (valueType == Integer.TYPE)
				entryClass = SignedInteger.class;
			else if (valueType == java.lang.Boolean.TYPE)
				entryClass = Boolean.class;
			else
				entryClass = valueType;
		}
		
		public static IniDataEntry createFromXML(Node entryNode, IEntryFactory factory) throws InvalidIniConfigurationException {
			Node n;
			IniDataEntry entry = new IniDataEntry();
			if (entryNode.getAttributes() == null || 
					entryNode.getAttributes().getLength() < 2 || 
					entryNode.getAttributes().getNamedItem("name") == null || //$NON-NLS-1$
					entryNode.getAttributes().getNamedItem("class") == null) { //$NON-NLS-1$
				throw new InvalidIniConfigurationException("An <entry> tag must have a 'name=\"\"' and a 'class=\"\"' attribute"); //$NON-NLS-1$
			}
			entry.entryName = entryNode.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
			try {
				Class<?> configClass = Class.forName(entryNode.getAttributes().getNamedItem("class").getNodeValue()); //$NON-NLS-1$
					entry.entryClass = configClass;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			if ((n = entryNode.getAttributes().getNamedItem("description")) != null) { //$NON-NLS-1$
				entry.entryDescription = n.getNodeValue();
			}
			if (entry.entryClass == CategoriesArray.class) {
				if ((n = entryNode.getAttributes().getNamedItem("flags")) != null) { //$NON-NLS-1$
					entry.extraData = n.getNodeValue().split(","); //$NON-NLS-1$
				}
				else if ((n = entryNode.getAttributes().getNamedItem("constantsPrefix")) != null) {
					entry.extraData = n.getNodeValue();
				}
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
		
		public String getConstantsPrefix() {
			try {
				return (String) extraData;
			} catch (ClassCastException e) {
				return null;
			}
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
			if (!doc.getDocumentElement().getNodeName().equals("clonkiniconfig")) { //$NON-NLS-1$
				throw new ParserConfigurationException("Invalid xml document. Wrong root node '" + doc.getDocumentElement().getNodeName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
			}
			NodeList nodeList = doc.getElementsByTagName("file"); //$NON-NLS-1$
			for (int i = 0; i < nodeList.getLength();i++) {
				try {
					IniConfiguration conf = IniConfiguration.createFromXML(nodeList.item(i));
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
//		if (docType.equals("IDListe"))
//			return IDArray.class;
//		if (docType.equals("DWORD"))
//			return SignedInteger.class;
//		if (docType.equals("Materialliste"))
//			return MaterialArray.class;
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
//			xmlFromText(new FileInputStream("/Users/madeen/Desktop/scenario.txt"), System.out);
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	
}

