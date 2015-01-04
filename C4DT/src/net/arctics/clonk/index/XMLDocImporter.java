package net.arctics.clonk.index;

import static net.arctics.clonk.util.Utilities.attempt;
import static net.arctics.clonk.util.Utilities.attemptWithResource;
import static net.arctics.clonk.util.Utilities.block;
import static net.arctics.clonk.util.Utilities.defaulting;
import static net.arctics.clonk.util.Utilities.getOrAdd;
import static net.arctics.clonk.util.Utilities.voidResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.ITypeable;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.util.IStorageLocation;
import net.arctics.clonk.util.StreamUtil;
import net.arctics.clonk.util.StringUtil;

import org.eclipse.core.runtime.Path;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class XMLDocImporter {

	private static final String DOCS = "docs";
	private static XPath xPath = XPathFactory.newInstance().newXPath();
	private static XPathExpression xp(final String expr) {
		try {
			return xPath.compile(expr);
		} catch (final XPathExpressionException e) {
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

	@SuppressWarnings("unchecked")
	private static <T> T evaluatePathExpression(XPathExpression expr, Object item, QName returnType) {
		return attempt(() -> (T)expr.evaluate(item, returnType), XPathExpressionException.class, Exception::printStackTrace);
	}

	private static Pattern fileLocationPattern = Pattern.compile("#: (.*?)\\:([0-9]+)\\((.*?)\\)");
	private static Pattern msgIdPattern = Pattern.compile("msgid \\\"(.*?)\"$");
	private static Pattern msgStrPattern = Pattern.compile("msgstr \\\"(.*?)\"$");

	private static class PoTranslationFragment {
		final int line;
		String english;
		String localized;
		public PoTranslationFragment(final int line) {
			super();
			this.line = line;
		}
		@Override
		public String toString() {
			return String.format("Line %d: %s = %s", line, english, localized);
		}
	}

	private Map<String, Map<String, List<PoTranslationFragment>>> translationFragments = new HashMap<String, Map<String, List<PoTranslationFragment>>>();

	private IStorageLocation storageLocation;
	private static Pattern TITLE_PATTERN = Pattern.compile("\\<title\\>(.*)\\<\\/title\\>"); //$NON-NLS-1$
	private boolean initialized = false;

	public synchronized void discardInitialization() {
		translationFragments.clear();
		initialized = false;
	}

	public synchronized XMLDocImporter initialize() {
		if (!initialized) {
			initialized = true;
			readTranslationFragmentsFromPoFiles();
		}
		return this;
	}

	public XMLDocImporter setStorageLocation(IStorageLocation location) {
		storageLocation = location;
		return this;
	}

	protected void readTranslationFragmentsFromPoFiles() {
		final IStorageLocation loc = storageLocation;
		if (loc == null)
			return;
		final Map<String, Map<String, List<PoTranslationFragment>>> frags =
			new HashMap<String, Map<String, List<PoTranslationFragment>>>();
		loc
			.locatorsOfContainer(DOCS, true).stream()
			.filter(url -> url.getPath().endsWith(".po"))
			.forEach(poFile -> {
				final String langId = StringUtil.rawFileName(poFile.getPath()).toUpperCase();
				final InputStreamReader reader = attempt(
					() -> new InputStreamReader(poFile.openStream(), "UTF8"),
					IOException.class, Exception::printStackTrace
				);
				if (reader == null)
					return;
				try {
					final Matcher fileLocationMatcher = fileLocationPattern.matcher("");
					final Matcher msgIdMatcher = msgIdPattern.matcher("");
					final Matcher msgStrMatcher = msgStrPattern.matcher("");
					final List<PoTranslationFragment> l = new LinkedList<PoTranslationFragment>();
					String english = null;
					for (final String line : StringUtil.lines(reader))
						if (fileLocationMatcher.reset(line).matches()) {
							final String file = fileLocationMatcher.group(1);
							final int fileLine = Integer.valueOf(fileLocationMatcher.group(2));
							final PoTranslationFragment fragment = new PoTranslationFragment(fileLine);
							l.add(fragment);
							getOrAdd(getOrAdd(frags, langId, () -> new HashMap<>()), file, () -> new LinkedList<>())
								.add(fragment);
						} else if (msgIdMatcher.reset(line).matches())
							english = msgIdMatcher.group(1).replaceAll("\\\\\\\"", "\"");
						else if (msgStrMatcher.reset(line).matches()) {
							final String localized = msgStrMatcher.group(1).replaceAll("\\\\\\\"", "\"");
							for (final PoTranslationFragment f : l) {
								f.english = english;
								f.localized = localized;
							}
							l.clear();
							english = null;
						}
				} finally {
					attempt(
						voidResult(reader::close),
						IOException.class, Exception::printStackTrace
					);
				}
			});
		translationFragments = frags;
	}

	public class ExtractedDeclarationDocumentation {
		public String name;
		public List<Variable> parameters = new LinkedList<Variable>();
		public String description;
		public IType returnType;
		public boolean isVariable;
	}

	public static final int DOCUMENTATION = 1;
	public static final int SIGNATURE = 2;

	public ExtractedDeclarationDocumentation extractDeclarationInformationFromFunctionXml(final String functionName, final String langId, final int flags) {
		if (!initialized || storageLocation == null)
			return null;
		final Path docsRelativePath = new Path("sdk/script/fn/"+functionName+".xml");
		final URL url = storageLocation.locatorForEntry(new Path(DOCS).append(docsRelativePath).toString(), false);
		if (url == null)
			return null;
		return attemptWithResource(url::openStream, stream -> {
			final DocumentBuilder builder = makeDocumentBuilder();
			if (builder == null)
				return null;
			final String source = StreamUtil.stringFromInputStream(stream);
			final boolean importDocumentation = (flags & DOCUMENTATION) != 0;
			final String translated = importDocumentation ? translate(langId, docsRelativePath, source) : source;
			// get rid of pesky meta information
			final String text_ = translated.replaceAll("\\<\\?.*\\?\\>", "").replaceAll("\\<\\!.*\\>", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			final Document doc = parseDocument(builder, text_);
			final Node titleNode = (Node) evaluatePathExpression(titleExpr, doc, XPathConstants.NODE);
			final Node rTypeNode = (Node) evaluatePathExpression(rtypeExpr, doc, XPathConstants.NODE);
			final NodeList parmNodes = (NodeList) evaluatePathExpression(parmsExpr, doc, XPathConstants.NODESET);
			final Node descNode = importDocumentation ? (Node) evaluatePathExpression(descExpr, doc, XPathConstants.NODE) : null;

			return (titleNode != null && rTypeNode != null) ? block(() -> {
				final ExtractedDeclarationDocumentation result = new ExtractedDeclarationDocumentation();
				result.name = getTextIncludingTags(titleNode);
				if (parmNodes != null && (parmNodes.getLength() > 0 || !Declaration.looksLikeConstName(result.name)))
					IntStream.range(0, parmNodes.getLength()).mapToObj(parmNodes::item).map(n -> {
						final Node nameNode  = (Node) evaluatePathExpression(parmNameExpr, n, XPathConstants.NODE);
						final Node typeNode  = (Node) evaluatePathExpression(parmTypeExpr, n, XPathConstants.NODE);
						final Node descNode_ = importDocumentation ? (Node) evaluatePathExpression(parmDescExpr, n, XPathConstants.NODE) : null;
						final String typeStr = typeNode != null ? getTextIncludingTags(typeNode) : PrimitiveType.ANY.toString();
						return nameNode != null ? block(() -> {
							final Variable parm = new Variable(getTextIncludingTags(nameNode), PrimitiveType.fromString(typeStr));
							if (descNode_ != null)
								parm.setUserDescription(getTextIncludingTags(descNode_));
							return parm;
						}) : null;
					}).filter(x -> x != null).forEach(result.parameters::add);
				else
					result.returnType = PrimitiveType.INT;
				result.returnType = PrimitiveType.fromString(getTextIncludingTags(rTypeNode));
				if (descNode != null)
					result.description = getTextIncludingTags(descNode);
				return result;
			}) : null;
		}, IOException.class, Exception::printStackTrace);
	}

	private Document parseDocument(final DocumentBuilder builder, final String text_) {
		return attempt(
			() -> builder.parse(new ByteArrayInputStream(text_.getBytes("UTF8"))),
			Exception.class,
			e -> {
				final Matcher m = TITLE_PATTERN.matcher(text_);
				if (m.find())
					System.out.println(m.group(1));
				e.printStackTrace();
			}
		);
	}

	private String translate(final String langId, final Path docsRelativePath, final String source) {
		return defaulting(block(() -> {
			final List<PoTranslationFragment> translationFragments = this.translationFragments.get(langId).get(docsRelativePath.toString());
			if (translationFragments == null)
				return null;
			int lineNo = 1;
			final StringBuilder builder = new StringBuilder(source.length());
			for (String textLine : StringUtil.lines(new StringReader(source))) {
				for (final PoTranslationFragment f : translationFragments)
					if (f.line == lineNo) {
						final String englishWithPlaceholdersReplacedWithTagCaptureGroups = Pattern.quote(f.english).replaceAll("<placeholder\\-([0-9]+)/>", "\\\\E(<.*?>.*?</.*?>)\\\\Q");
						//System.out.println(englishWithPlaceholdersReplacedWithTagCaptureGroups);
						final Matcher englishMatcher = Pattern.compile(englishWithPlaceholdersReplacedWithTagCaptureGroups).matcher(textLine);
						final Matcher placeHolderInLocalizedMatcher = Pattern.compile("<placeholder\\-([0-9]+)/>").matcher(f.localized);
						final StringBuilder localizedWithTagsPutIn = new StringBuilder(f.localized);
						int builderOffsetCausedByReplacing = 0;
						if (englishMatcher.find()) {
							for (int g = 1; g <= englishMatcher.groupCount(); g++)
								if (placeHolderInLocalizedMatcher.find()) {
									final String actualTag = englishMatcher.group(g);
									placeHolderInLocalizedMatcher.start();
									localizedWithTagsPutIn.replace(placeHolderInLocalizedMatcher.start()+builderOffsetCausedByReplacing, placeHolderInLocalizedMatcher.end()+builderOffsetCausedByReplacing, actualTag);
									builderOffsetCausedByReplacing += actualTag.length() - placeHolderInLocalizedMatcher.group().length();
								}
							final StringBuilder lineBuilder = new StringBuilder(textLine);
							lineBuilder.replace(englishMatcher.start(), englishMatcher.end(), localizedWithTagsPutIn.toString());
							textLine = lineBuilder.toString();
						}
					}
				builder.append(textLine);
				builder.append("\n");
				lineNo++;
			}
			return builder.toString();
		}), source);
	}

	private DocumentBuilder makeDocumentBuilder() {
		return attempt(() -> {
			final DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			final InputSource dtdInputSource = attempt(
				() -> new InputSource(
					storageLocation.locatorForEntry("docs/clonk.dtd", false).openStream()
				),
				Exception.class,
				Exception::printStackTrace
			);
			b.setEntityResolver((publicId, systemId) ->
				systemId.endsWith("clonk.dtd") //$NON-NLS-1$
					? dtdInputSource
					: null
			);
			return b;
		}, Exception.class, Exception::printStackTrace);
	}

	private static void appendContentsOfNode(final Node n, final StringBuilder builder) {
		for (int i = 0; i < n.getChildNodes().getLength(); i++) {
			final Node c = n.getChildNodes().item(i);
			if (c.getNodeValue() != null)
				builder.append(c.getNodeValue());
			else {
				builder.append("<"+c.getNodeName()+">");
				appendContentsOfNode(c, builder);
				builder.append("<"+c.getNodeName()+"/>");
			}
		}
	}

	private static String getTextIncludingTags(final Node n) {
		final StringBuilder b = new StringBuilder();
		appendContentsOfNode(n, b);
		return b.toString();
	}

	public boolean initialized() {
		return initialized;
	}

	public <T extends ITypeable> boolean fleshOutPlaceholder(final T placeholder, final boolean placeholdersFleshedOutFlag) {
		if (!placeholdersFleshedOutFlag) {
			final ExtractedDeclarationDocumentation d = extractDeclarationInformationFromFunctionXml(
				placeholder.name(), ClonkPreferences.languagePref(), XMLDocImporter.SIGNATURE
			);
			if (d != null) {
				if (placeholder instanceof Function) {
					final Function f = (Function)placeholder;
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
