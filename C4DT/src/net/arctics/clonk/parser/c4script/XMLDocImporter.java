package net.arctics.clonk.parser.c4script;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import net.arctics.clonk.parser.c4script.Variable.Scope;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.Utilities;

import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLDocImporter {

	private static XPath xPath = XPathFactory.newInstance().newXPath();
	private static XPathExpression xp(String expr) {
		try {
			return xPath.compile(expr);
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			return null;
		}
	}
	private static final XPathExpression parmNameExpr = xp("./name"); //$NON-NLS-1$
	private static final XPathExpression parmTypeExpr = xp("./type"); //$NON-NLS-1$
	private static final XPathExpression parmDescExpr = xp("./desc"); //$NON-NLS-1$
	private static final XPathExpression titleExpr = xp("./funcs/*[self::func or self::const]/title[1]"); //$NON-NLS-1$
	private static final XPathExpression rtypeExpr = xp("./funcs/*[self::func or self::const]/syntax/rtype[1]"); //$NON-NLS-1$
	private static final XPathExpression parmsExpr = xp("./funcs/*[self::func or self::const]/syntax/params/param"); //$NON-NLS-1$
	private static final XPathExpression descExpr = xp("./funcs/*[self::func or self::const]/desc[1]"); //$NON-NLS-1$
	
	private static Pattern fileLocationPattern = Pattern.compile("#: (.*?)\\:([0-9]+)\\((.*?)\\)");
	private static Pattern msgIdPattern = Pattern.compile("msgid \\\"(.*?)\"$");
	private static Pattern msgStrPattern = Pattern.compile("msgstr \\\"(.*?)\"$");
	
	private static class PoTranslationFragment {
		int line;
		String english;
		String localized;
		public PoTranslationFragment(int line) {
			super();
			this.line = line;
		}
		@Override
		public String toString() {
			return String.format("Line %d: %s = %s", line, english, localized);
		}
	}
	
	private final Map<String, Map<String, List<PoTranslationFragment>>> translationFragments = new HashMap<String, Map<String, List<PoTranslationFragment>>>(); 
	
	private String repositoryPath;
	private static Pattern TITLE_PATTERN = Pattern.compile("\\<title\\>(.*)\\<\\/title\\>"); //$NON-NLS-1$
	private boolean initialized = false;

	public synchronized String repositoryPath() {
		return repositoryPath;
	}
	
	public synchronized void discardInitialization() {
		translationFragments.clear();
		initialized = false;
	}
	
	public synchronized XMLDocImporter initialize() {
		if (!initialized) {
			readTranslationFragmentsFromPoFiles();
			initialized = true;
		}
		return this;
	}

	public synchronized void setRepositoryPath(String repositoryPath) {
		if (Utilities.objectsEqual(repositoryPath, this.repositoryPath))
			return;
		this.repositoryPath = repositoryPath;
		discardInitialization();
	}

	protected void readTranslationFragmentsFromPoFiles() {
		translationFragments.clear();
		if (repositoryPath == null)
			return;
		File docsFolder = new File(repositoryPath + "/docs");
		if (!docsFolder.isDirectory())
			return;
		for (File poFile : docsFolder.listFiles(StreamUtil.patternFilter(".*\\.po"))) {
			String langId = StringUtil.rawFileName(poFile.getName()).toUpperCase();
			InputStreamReader reader;
			try {
				reader = new InputStreamReader(new FileInputStream(poFile), "UTF8");
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
				continue;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				continue;
			}
			try {
				Matcher fileLocationMatcher = fileLocationPattern.matcher("");
				Matcher msgIdMatcher = msgIdPattern.matcher("");
				Matcher msgStrMatcher = msgStrPattern.matcher("");
				List<PoTranslationFragment> l = new LinkedList<PoTranslationFragment>();
				String english = null;
				for (String line : StringUtil.lines(reader)) {
					if (fileLocationMatcher.reset(line).matches()) {
						String file = fileLocationMatcher.group(1);
						int fileLine = Integer.valueOf(fileLocationMatcher.group(2));
						PoTranslationFragment fragment = new PoTranslationFragment(fileLine);
						l.add(fragment);
						Map<String, List<PoTranslationFragment>> fileToFragments = translationFragments.get(langId);
						if (fileToFragments == null) {
							fileToFragments = new HashMap<String, List<PoTranslationFragment>>();
							translationFragments.put(langId, fileToFragments);
						}
						List<PoTranslationFragment> list = fileToFragments.get(file);
						if (list == null) {
							list = new LinkedList<PoTranslationFragment>();
							fileToFragments.put(file, list);
						}
						list.add(fragment);
					} else if (msgIdMatcher.reset(line).matches()) {
						english = msgIdMatcher.group(1).replaceAll("\\\\\\\"", "\"");
					} else if (msgStrMatcher.reset(line).matches()) {
						String localized = msgStrMatcher.group(1).replaceAll("\\\\\\\"", "\"");
						for (PoTranslationFragment f : l) {
							f.english = english;
							f.localized = localized;
						}
						l.clear();
						english = null;
					}
				}
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public class ExtractedDeclarationDocumentation {
		public String name;
		public List<Variable> parameters = new LinkedList<Variable>();
		public String description;
		public IType returnType;
		public boolean isVariable;
		public Declaration toDeclaration() {
			if (isVariable) {
				return new Variable(name, returnType, description, Scope.CONST);
			} else {
				Function f = new Function(name, returnType, parameters.toArray(new Variable[parameters.size()]));
				f.setUserDescription(description);
				return f;
			}
		}
	}
	
	public static final int DOCUMENTATION = 1;
	public static final int SIGNATURE = 2;
	
	public ExtractedDeclarationDocumentation extractDeclarationInformationFromFunctionXml(String functionName, String langId, int flags) {
		if (!initialized || repositoryPath == null)
			return null;
		Path docsRelativePath = new Path("sdk/script/fn/"+functionName+".xml");
		File functionXmlFile = new Path(repositoryPath).append("docs").append(docsRelativePath).toFile();
		if (!functionXmlFile.exists())
			return null;
		try {
			FileInputStream stream = new FileInputStream(functionXmlFile);
			try {
				DocumentBuilder builder;
				try {
					builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
					builder.setEntityResolver(new EntityResolver() {	
						@Override
						public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
							if (systemId.endsWith("clonk.dtd")) //$NON-NLS-1$
								return new InputSource(new FileReader(repositoryPath + "/docs/clonk.dtd"));
							return null;
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
				String text = StreamUtil.stringFromInputStream(stream); 
				boolean importDocumentation = (flags & DOCUMENTATION) != 0;
				if (importDocumentation) {
					try {
						List<PoTranslationFragment> translationFragments = this.translationFragments.get(langId).get(docsRelativePath.toString());
						int lineNo = 1;
						StringBuilder translatedRebuild = new StringBuilder(text.length());
						for (String textLine : StringUtil.lines(new StringReader(text))) {
							for (PoTranslationFragment f : translationFragments) {
								if (f.line == lineNo) {
									String englishWithPlaceholdersReplacedWithTagCaptureGroups = Pattern.quote(f.english).replaceAll("<placeholder\\-([0-9]+)/>", "\\\\E(<.*?>.*?</.*?>)\\\\Q");
									System.out.println(englishWithPlaceholdersReplacedWithTagCaptureGroups);
									Matcher englishMatcher = Pattern.compile(englishWithPlaceholdersReplacedWithTagCaptureGroups).matcher(textLine);
									Matcher placeHolderInLocalizedMatcher = Pattern.compile("<placeholder\\-([0-9]+)/>").matcher(f.localized);
									StringBuilder localizedWithTagsPutIn = new StringBuilder(f.localized);
									int builderOffsetCausedByReplacing = 0;
									if (englishMatcher.find()) {
										for (int g = 1; g <= englishMatcher.groupCount(); g++) {
											if (placeHolderInLocalizedMatcher.find()) {
												String actualTag = englishMatcher.group(g);
												placeHolderInLocalizedMatcher.start();
												localizedWithTagsPutIn.replace(placeHolderInLocalizedMatcher.start()+builderOffsetCausedByReplacing, placeHolderInLocalizedMatcher.end()+builderOffsetCausedByReplacing, actualTag);
												builderOffsetCausedByReplacing += actualTag.length() - placeHolderInLocalizedMatcher.group().length();
											}
										}
										StringBuilder lineBuilder = new StringBuilder(textLine);
										lineBuilder.replace(englishMatcher.start(), englishMatcher.end(), localizedWithTagsPutIn.toString());
										textLine = lineBuilder.toString();
									}
								}
							}
							translatedRebuild.append(textLine);
							translatedRebuild.append("\n");
							lineNo++;
						}
						text = translatedRebuild.toString();
					} catch (NullPointerException e) {
						// ignore
					}
				}
				// get rid of pesky meta information
				text = text.replaceAll("\\<\\?.*\\?\\>", "").replaceAll("\\<\\!.*\\>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				Document doc;
				try {
					doc = builder.parse(new ByteArrayInputStream(text.getBytes("UTF8"))); //$NON-NLS-1$
				} catch (Exception e) {
					Matcher m = TITLE_PATTERN.matcher(text);
					if (m.find()) {
						System.out.println(m.group(1));
					}
					e.printStackTrace();
					return null;
				}
				Node titleNode = (Node) titleExpr.evaluate(doc, XPathConstants.NODE);
				Node rTypeNode = (Node) rtypeExpr.evaluate(doc, XPathConstants.NODE);
				NodeList parmNodes = (NodeList) parmsExpr.evaluate(doc, XPathConstants.NODESET);
				Node descNode = importDocumentation ? (Node) descExpr.evaluate(doc, XPathConstants.NODE) : null;
				
				if (titleNode != null && rTypeNode != null) {
					ExtractedDeclarationDocumentation result = new ExtractedDeclarationDocumentation();
					result.name = getTextIncludingTags(titleNode);
					if (parmNodes != null && (parmNodes.getLength() > 0 || !Declaration.looksLikeConstName(result.name))) {
						for (int i = 0; i < parmNodes.getLength(); i++) {
							Node n = parmNodes.item(i);
							Node nameNode  = (Node) parmNameExpr.evaluate(n, XPathConstants.NODE);
							Node typeNode  = (Node) parmTypeExpr.evaluate(n, XPathConstants.NODE);
							Node descNode_ = (Node) parmDescExpr.evaluate(n, XPathConstants.NODE);
							String typeStr = typeNode != null ? getTextIncludingTags(typeNode) : PrimitiveType.ANY.toString();
							if (nameNode != null) {
								Variable parm = new Variable(getTextIncludingTags(nameNode), PrimitiveType.makeType(typeStr));
								if (descNode_ != null)
									parm.setUserDescription(getTextIncludingTags(descNode_));
								result.parameters.add(parm);
							}
						}
					} else {
						result.returnType = PrimitiveType.INT;
					}
					result.returnType = PrimitiveType.makeType(getTextIncludingTags(rTypeNode));
					if (descNode != null)
						result.description = getTextIncludingTags(descNode);
					return result;
				}
				return null;
			} finally {
				stream.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static void appendContentsOfNode(Node n, StringBuilder builder) {
		for (int i = 0; i < n.getChildNodes().getLength(); i++) {
			Node c = n.getChildNodes().item(i);
			if (c.getNodeValue() != null)
				builder.append(c.getNodeValue());
			else {
				builder.append("<"+c.getNodeName()+">");
				appendContentsOfNode(c, builder);
				builder.append("<"+c.getNodeName()+"/>");
			}
		}
	}
	
	private static String getTextIncludingTags(Node n) {
		StringBuilder b = new StringBuilder();
		appendContentsOfNode(n, b);
		return b.toString();
	}

	public boolean initialized() {
		return initialized;
	}
	
	public <T extends ITypeable & IHasSubDeclarations> boolean fleshOutPlaceholder(T placeholder, boolean placeholdersFleshedOutFlag) {
		if (!placeholdersFleshedOutFlag) {
			ExtractedDeclarationDocumentation d = extractDeclarationInformationFromFunctionXml(placeholder.name(), ClonkPreferences.getLanguagePref(), XMLDocImporter.SIGNATURE);
			if (d != null) {
				if (placeholder instanceof Function) {
					Function f = (Function)placeholder;
					if (d.parameters != null)
						f.setParameters(d.parameters);
				}
				if (d.returnType != null)
					placeholder.forceType(d.returnType);
			}
		}
		return true;
	}

}
