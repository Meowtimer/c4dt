package net.arctics.clonk.parser.stringtbl;

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.Region;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.Structure;
import net.arctics.clonk.parser.DeclarationRegion;
import net.arctics.clonk.parser.NameValueAssignment;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.ReadOnlyIterator;
import net.arctics.clonk.util.StreamUtil;

public class StringTbl extends Structure implements ITreeNode, ITableEntryInformationSink {
	
	public static final Pattern PATTERN = Pattern.compile("StringTbl(..)\\.txt", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	
	private Map<String, NameValueAssignment> map = new HashMap<String, NameValueAssignment>();
	private transient IFile file;

	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
		setName(file != null ? file.getName() : null);
	}
	
	@Override
	public IResource getResource() {
		return file;
	}

	public Map<String, NameValueAssignment> getMap() {
		return map;
	}
	
	public void addTblEntry(String key, String value, int start, int end) {
		NameValueAssignment nv = new NameValueAssignment(start, end, key, value);
		nv.setParentDeclaration(this);
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
	
	public static void readStringTbl(Reader reader, ITableEntryInformationSink sink) {
		BufferedScanner scanner;
		scanner = new BufferedScanner(reader);
		while (!scanner.reachedEOF()) {
			scanner.eatWhitespace();
			if (scanner.read() == '#')
				scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
			else {
				scanner.unread();
				int start = scanner.getPosition();
				String key = scanner.readStringUntil('=');
				if (scanner.read() == '=') {
					String value = scanner.readStringUntil(BufferedScanner.NEWLINE_CHARS);
					sink.addTblEntry(key, value, start, scanner.getPosition());
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
	public Object[] getSubDeclarationsForOutline() {
		return map.values().toArray(new Object[map.values().size()]);
	}
	
	public Iterator<NameValueAssignment> iterator() {
		return new ReadOnlyIterator<NameValueAssignment>(map.values().iterator());
	}
	
	public static void register() {
		registerStructureFactory(new IStructureFactory() {
			private final Matcher stringTblFileMatcher = PATTERN.matcher(""); //$NON-NLS-1$ //$NON-NLS-2$
			public Structure create(IResource resource, boolean duringBuild) {
				if (resource instanceof IFile && stringTblFileMatcher.reset(resource.getName()).matches()) {
					IFile file = (IFile) resource;
					StringTbl tbl = new StringTbl();
					tbl.setFile(file);
					String fileContents;
					try {
						fileContents = StreamUtil.stringFromFileDocument(file);
					} catch (Exception e) {
						e.printStackTrace();
						return null;
					}
					StringReader reader = new StringReader(fileContents);
					tbl.read(reader);
					return tbl;
				}
				return null;
			}
		});
	}

	public void addChild(ITreeNode node) {
	}

	public Collection<? extends ITreeNode> getChildCollection() {
		return map.values();
	}

	public String getNodeName() {
		return "StringTbl";  //$NON-NLS-1$
	}

	public ITreeNode getParentNode() {
		return null;
	}

	public IPath getPath() {
		return ITreeNode.Default.getPath(this);
	}

	public boolean subNodeOf(ITreeNode node) {
		return ITreeNode.Default.subNodeOf(this, node);
	}
	
	@Override
	public boolean requiresScriptReparse() {
		return true;
	}
	
	public static DeclarationRegion getEntryRegion(String stringValue, int exprStart, int offset) {
		int firstDollar = stringValue.lastIndexOf('$', offset-1);
		int secondDollar = stringValue.indexOf('$', offset);
		if (firstDollar != -1 && secondDollar != -1) {
			String entry = stringValue.substring(firstDollar+1, secondDollar);
			return new DeclarationRegion(null, new Region(exprStart+1+firstDollar, secondDollar-firstDollar+1), entry);
		}
		return null;
	}
	
	public static DeclarationRegion getEntryForLanguagePref(String stringValue, int exprStart, int offset, Declaration container, boolean returnNullIfNotFound) {
		DeclarationRegion result = getEntryRegion(stringValue, exprStart, offset);
		if (result != null) {
			StringTbl stringTbl = container.getStringTblForLanguagePref();
			Declaration e = stringTbl != null ? stringTbl.getMap().get(result.getText()) : null;
			if (e == null && returnNullIfNotFound) {
				result = null;
			} else {
				result.setDeclaration(e);
			}
		}
		return result;
	}
	
	public static String evaluateEntries(Declaration context, String value, int exprStart) {
		int valueLen = value.length();
		StringBuilder builder = new StringBuilder(valueLen*2);
		// insert stringtbl entries
		Outer: for (int i = 0; i < valueLen;) {
			if (i+1 < valueLen) {
				switch (value.charAt(i)) {
				case '$':
					DeclarationRegion region = getEntryForLanguagePref(value, exprStart, i+1, context, true);
					if (region != null) {
						builder.append(((NameValueAssignment)region.getDeclaration()).getValue());
						i += region.getRegion().getLength();
						continue Outer;
					}
					break;
				case '\\':
					switch (value.charAt(++i)) {
					case '"': case '\\':
						builder.append(value.charAt(i++));
						continue Outer;
					}
					break;
				}
			}
			builder.append(value.charAt(i++));
		}
		return builder.toString();
	}

}