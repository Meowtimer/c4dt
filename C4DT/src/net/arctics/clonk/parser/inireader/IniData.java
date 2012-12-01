package net.arctics.clonk.parser.inireader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.arctics.clonk.Core;
import net.arctics.clonk.parser.ID;
import net.arctics.clonk.util.ArrayUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class IniData {
	
	public static class IniConfiguration {
		private String filename;
		protected Map<String, IniSectionDefinition> sections = new HashMap<String, IniSectionDefinition>();
		protected IEntryFactory factory = null;
		
		protected IniConfiguration() {}
		
		public static IniConfiguration fromXML(Node fileNode) throws InvalidIniConfigurationException {
			IniConfiguration conf = new IniConfiguration();
			if (fileNode.getAttributes() == null ||  fileNode.getAttributes().getNamedItem("name") == null) //$NON-NLS-1$
				throw new InvalidIniConfigurationException("A <file> tag must have a name"); //$NON-NLS-1$
			conf.filename = fileNode.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
			conf.factory = EntryFactory.INSTANCE;

			NodeList sectionNodes = fileNode.getChildNodes();
			for(int i = 0; i < sectionNodes.getLength(); i++)
				if (sectionNodes.item(i).getNodeName() == "section") { //$NON-NLS-1$
					IniSectionDefinition section = IniSectionDefinition.fromXML(sectionNodes.item(i), conf.factory);
					conf.sections().put(section.sectionName(), section);
				}
			return conf;
		}
		
		public static IniConfiguration createFromClass(Class<?> clazz) {
			IniConfiguration result = new IniConfiguration();
			for (Field f : clazz.getFields()) {
				IniField annotation;
				if ((annotation = f.getAnnotation(IniField.class)) != null) {
					IniSectionDefinition section = result.sections().get(IniUnitParser.category(annotation, clazz));
					if (section == null) {
						section = new IniSectionDefinition();
						section.sectionName = annotation.category();
						if (section.sectionName.equals(""))
							section.sectionName = IniUnitParser.defaultSection(clazz);
						result.sections.put(section.sectionName, section);
					}
					section.entries.put(f.getName(), new IniEntryDefinition(f.getName(), f.getType()));
				}
			}
			result.factory = EntryFactory.INSTANCE;
			return result;
		}

		public String fileName() {
			return filename;
		}

		public Map<String, IniSectionDefinition> sections() {
			return sections;
		}
		
		public boolean hasSection(String sectionName) {
			return sections.containsKey(sectionName);
		}
		
		public String[] sectionNames() {
			return sections.keySet().toArray(new String[sections.size()]);
		}

		public IEntryFactory factory() {
			return factory;
		}
		
	}

	public static class IniDataBase {}

	public static class IniSectionDefinition extends IniDataBase {
		private String sectionName;
		private final Map<String, IniDataBase> entries = new HashMap<String, IniDataBase>();
		
		protected IniSectionDefinition() {}
		
		public static IniSectionDefinition fromXML(Node sectionNode, IEntryFactory factory) throws InvalidIniConfigurationException {
			IniSectionDefinition section = new IniSectionDefinition();
			if (sectionNode.getAttributes() == null || 
					sectionNode.getAttributes().getLength() == 0 || 
					sectionNode.getAttributes().getNamedItem("name") == null)
				throw new InvalidIniConfigurationException("A <section> tag must have a name=\"\" attribute"); //$NON-NLS-1$
			section.sectionName = sectionNode.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
			NodeList entryNodes = sectionNode.getChildNodes();
			for(int i = 0; i < entryNodes.getLength();i++) {
				Node node = entryNodes.item(i);
				// there was a '==' comparison all the time :D - did work by chance or what?
				if (node.getNodeName().equals("entry")) { //$NON-NLS-1$
					IniEntryDefinition entry = IniEntryDefinition.fromXML(node, factory);
					section.entries().put(entry.name(), entry);
				}
				else if (node.getNodeName().equals("section")) {
					IniSectionDefinition sec = IniSectionDefinition.fromXML(node, factory);
					section.entries().put(sec.sectionName(), sec);
				}
			}
			return section;
		}
		
		public String sectionName() {
			return sectionName;
		}

		public Map<String, IniDataBase> entries() {
			return entries;
		}
		
		public boolean hasEntry(String entryName) {
			return entries.containsKey(entryName);
		}
		
		public boolean hasSection(String section) {
			IniDataBase item = entryForKey(section);
			return item instanceof IniSectionDefinition;
		}

		public IniDataBase entryForKey(String key) {
			return entries().get(key);
		}
		
	}
	
	public static final class IniEntryDefinition extends IniDataBase {
		protected String name;
		protected Class<?> entryClass;
		protected String description;
		protected String categoryFilter;
		protected String constantsPrefix;
		protected Map<String, Integer> enumValues;
		
		protected IniEntryDefinition() {}
		
		public IniEntryDefinition(String name, Class<?> valueType) {
			this.name = name;
			if (valueType == String.class)
				entryClass = valueType;
			else if (valueType == Integer.TYPE || valueType == Long.TYPE)
				entryClass = SignedInteger.class;
			else if (valueType == java.lang.Boolean.TYPE)
				entryClass = Boolean.class;
			else
				entryClass = valueType;
		}
		
		private static Class<?> cls(String name) {
			if (name.equals("C4ID")) //$NON-NLS-1$
				return ID.class;
			if (!name.contains("."))
				name = Core.id("parser.inireader."+name); //$NON-NLS-1$
			try {
				return Class.forName(name);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}
		
		public static IniEntryDefinition fromXML(Node entryNode, IEntryFactory factory) throws InvalidIniConfigurationException {
			Node n;
			IniEntryDefinition entry = new IniEntryDefinition();
			if (entryNode.getAttributes() == null || 
					entryNode.getAttributes().getLength() < 2 || 
					entryNode.getAttributes().getNamedItem("name") == null || //$NON-NLS-1$
					entryNode.getAttributes().getNamedItem("class") == null)
				throw new InvalidIniConfigurationException("An <entry> tag must have a 'name=\"\"' and a 'class=\"\"' attribute"); //$NON-NLS-1$
			entry.name = entryNode.getAttributes().getNamedItem("name").getNodeValue(); //$NON-NLS-1$
			String className = entryNode.getAttributes().getNamedItem("class").getNodeValue(); //$NON-NLS-1$
			Class<?> configClass = cls(className);
			if (configClass == null)
				throw new InvalidIniConfigurationException("Bad class " + entryNode.getAttributes().getNamedItem("class").getNodeValue()); //$NON-NLS-1$ //$NON-NLS-2$
			entry.entryClass = configClass;
			if ((n = entryNode.getAttributes().getNamedItem("description")) != null)
				entry.description = n.getNodeValue();
			if ((n = entryNode.getAttributes().getNamedItem("enumValues")) != null) //$NON-NLS-1$
				entry.enumValues = ArrayUtil.mapValueToIndex(n.getNodeValue().split(",")); //$NON-NLS-1$
			if ((n = entryNode.getAttributes().getNamedItem("constantsPrefix")) != null)
				entry.constantsPrefix = n.getNodeValue();
			if ((n = entryNode.getAttributes().getNamedItem("categoryFilter")) != null)
				entry.categoryFilter = n.getNodeValue();
			return entry;
		}
		
		public String name() { return name; }
		public Class<?> entryClass() { return entryClass; }
		public String description() { return description; }
		public String categoryFilter() { return categoryFilter; }
		public String constantsPrefix() { return constantsPrefix; }
		public Map<String, Integer> enumValues() { return enumValues; }
		
		@Override
		public String toString() {
			return String.format("%s: %s", name, entryClass != null ? entryClass.getSimpleName() : "?");
		}
		
	}
	
	private final InputStream xmlFile;
	private final Map<String, IniConfiguration> configurations = new HashMap<String, IniConfiguration>(4);
	
	public IniData(InputStream stream) {
		xmlFile = stream;
	}
	
	/**
	 * Returns the configuration that is declared for files with name of <tt>filename</tt>.
	 * @param filename including extension
	 * @return the configuration or <tt>null</tt>
	 */
	public IniConfiguration configurationFor(String filename) {
		return configurations.get(filename);
	}
	
	public void parse() {
		try {			
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(xmlFile);
			doc.getDocumentElement().normalize();
			if (!doc.getDocumentElement().getNodeName().equals("clonkiniconfig"))
				throw new ParserConfigurationException("Invalid xml document. Wrong root node '" + doc.getDocumentElement().getNodeName() + "'."); //$NON-NLS-1$ //$NON-NLS-2$
			NodeList nodeList = doc.getElementsByTagName("file"); //$NON-NLS-1$
			for (int i = 0; i < nodeList.getLength();i++)
				try {
					IniConfiguration conf = IniConfiguration.fromXML(nodeList.item(i));
					configurations.put(conf.fileName(), conf);
				}
				catch (InvalidIniConfigurationException e) {
					e.printStackTrace();
				}
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

