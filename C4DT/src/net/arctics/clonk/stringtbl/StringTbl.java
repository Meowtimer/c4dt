package net.arctics.clonk.stringtbl;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.NameValueAssignment;
import net.arctics.clonk.ast.Structure;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.Region;

public class StringTbl extends Structure implements ITreeNode {

	public static final Pattern PATTERN = Pattern.compile("StringTbl(..)\\.txt", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	private final Map<String, NameValueAssignment> map = new HashMap<String, NameValueAssignment>();
	private transient IFile file;


	@Override
	public void setFile(IFile file) {
		this.file = file;
		setName(file != null ? file.getName() : null);
	}

	@Override
	public IFile file() { return file; }
	@Override
	public IResource resource() { return file; }
	public Map<String, NameValueAssignment> map() { return map; }
	@Override
	public void addChild(ITreeNode node) {}
	@Override
	public Collection<? extends ITreeNode> childCollection() { return map.values(); }
	@Override
	public String nodeName() { return "StringTbl"; } //$NON-NLS-1$
	@Override
	public ITreeNode parentNode() { return null; }
	@Override
	public IPath path() { return ITreeNode.Default.path(this); }
	@Override
	public boolean subNodeOf(ITreeNode node) { return ITreeNode.Default.subNodeOf(this, node); }
	@Override
	public boolean requiresScriptReparse() { return true; }

	public void addTblEntry(String key, String value, int start, int end) {
		final NameValueAssignment nv = new NameValueAssignment(start, end, key, value);
		nv.setParent(this);
		map.put(key, nv);
	}

	@Override
	public Declaration findLocalDeclaration(String declarationName, Class<? extends Declaration> declarationClass) {
		if (declarationClass == NameValueAssignment.class)
			return map.get(declarationName);
		return null;
	}

	@Override
	public Declaration findDeclaration(String declarationName) {
		return map.get(declarationName);
	}

	public static void readStringTbl(Reader reader, StringTbl tbl) {
		BufferedScanner scanner;
		scanner = new BufferedScanner(reader);
		while (!scanner.reachedEOF()) {
			scanner.eatWhitespace();
			if (scanner.read() == '#')
				scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			else {
				scanner.unread();
				final int start = scanner.tell();
				final String key = scanner.readStringUntil('=');
				if (scanner.read() == '=') {
					String value = scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
					if (value == null)
						value = "";
					tbl.addTblEntry(key, value, start, scanner.tell());
				}
				else
					scanner.unread();
			}
		}
	}

	public void read(Reader reader) {
		readStringTbl(reader, this);
	}

	@Override
	public Object[] subDeclarationsForOutline() {
		return map.values().toArray(new Object[map.values().size()]);
	}

	public Iterator<NameValueAssignment> iterator() {
		return new ReadOnlyIterator<NameValueAssignment>(map.values().iterator());
	}

	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			private final Matcher stringTblFileMatcher = PATTERN.matcher(""); //$NON-NLS-1$
			@Override
			public Structure create(IResource resource, boolean duringBuild) {
				if (resource instanceof IFile && stringTblFileMatcher.reset(resource.getName()).matches()) {
					final IFile file = (IFile) resource;
					final StringTbl tbl = new StringTbl();
					tbl.setFile(file);
					String fileContents;
					try {
						fileContents = StreamUtil.stringFromFileDocument(file);
					} catch (final Exception e) {
						e.printStackTrace();
						return null;
					}
					final StringReader reader = new StringReader(fileContents);
					tbl.read(reader);
					return tbl;
				}
				return null;
			}
		});
	}

	public static EntityRegion entryRegionInString(String stringValue, int exprStart, int offset) {
		final int firstDollar = stringValue.lastIndexOf('$', offset-1);
		final int secondDollar = stringValue.indexOf('$', offset);
		if (firstDollar != -1 && secondDollar != -1) {
			final String entry = stringValue.substring(firstDollar+1, secondDollar);
			return new EntityRegion(null, new Region(exprStart+1+firstDollar, secondDollar-firstDollar+1), entry);
		}
		return null;
	}

	public static EntityRegion entryForLanguagePref(String stringValue, int exprStart, int offset, Declaration container, boolean returnNullIfNotFound) {
		EntityRegion result = entryRegionInString(stringValue, exprStart, offset);
		if (result != null) {
			final StringTbl stringTbl = container.localStringTblMatchingLanguagePref();
			final Declaration e = stringTbl != null ? stringTbl.map().get(result.text()) : null;
			if (e == null && returnNullIfNotFound)
				result = null;
			else
				result.setEntity(e);
		}
		return result;
	}

	public static class EvaluationResult {
		public String evaluated;
		public EntityRegion singleDeclarationRegionUsed;
		public boolean anySubstitutionsApplied;
	}

	/**
	 * Evaluate a string containing $...$ placeholders by fetching the actual strings from respective StringTbl**.txt files.
	 * @param context The declaration/script to be used as a hint on where to look for StringTbl files
	 * @param value The value to be evaluated
	 * @return The evaluated string
	 */
	public static EvaluationResult evaluateEntries(Declaration context, String value, boolean evaluateEscapes) {
		final int valueLen = value.length();
		final StringBuilder builder = new StringBuilder(valueLen*2);
		// insert stringtbl entries
		EntityRegion reg = null;
		boolean moreThanOneSubstitution = false;
		boolean substitutionsApplied = false;
		Outer: for (int i = 0; i < valueLen;) {
			if (i+1 < valueLen)
				switch (value.charAt(i)) {
				case '$':
					moreThanOneSubstitution = reg != null;
					final EntityRegion region = entryForLanguagePref(value, 0, i+1, context, true);
					if (region != null) {
						substitutionsApplied = true;
						builder.append(((NameValueAssignment)region.entityAs(Declaration.class)).stringValue());
						i += region.region().getLength();
						reg = region;
						continue Outer;
					}
					break;
				case '\\':
					if (evaluateEscapes) switch (value.charAt(++i)) {
					case '"': case '\\':
						builder.append(value.charAt(i++));
						continue Outer;
					}
					break;
				}
			builder.append(value.charAt(i++));
		}
		final EvaluationResult r = new EvaluationResult();
		r.evaluated = builder.toString();
		r.singleDeclarationRegionUsed = moreThanOneSubstitution ? null : reg;
		r.anySubstitutionsApplied = substitutionsApplied;
		return r;
	}

}