package net.arctics.clonk.ini;

import static net.arctics.clonk.util.Utilities.defaulting;
import net.arctics.clonk.Problem;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.IASTPositionProvider;
import net.arctics.clonk.ast.SourceLocation;
import net.arctics.clonk.c4group.C4GroupItem;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.parser.CStyleScanner;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.StreamUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

public class IniUnitParser extends CStyleScanner implements IASTPositionProvider {

	private final IniUnit unit;
	private final IFile file;
	private final Markers markers = new Markers();

	public IniUnitParser(IniUnit unit) {
		super(unit.input);
		figureOutIndentation();
		this.unit = unit;
		file = unit.file();
	}

	@Override
	public void reset() {
		super.reset();
		if (file != null)
			this.reset(StreamUtil.stringFromFile(file));
	}

	protected IniSection parseSection(boolean modifyMarkers, IniSection parentSection) throws ProblemException {
		final int targetIndentation = parentSection != null ? parentSection.indentation()+1 : 0;
		final int rollback = tell();
		while (skipComment());
		// parse head
		final int indentation = currentIndentation();
		final int start = tell();
		if (read() == '[' && indentation == targetIndentation) {
			final String name = readStringUntil(']', '\n', '\r');
			int end;
			if (read() != ']') {
				if (modifyMarkers)
					markers.marker(this, Problem.TokenExpected, unit, tell()-1, tell(), Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, IMarker.SEVERITY_ERROR, (Object)"]"); //$NON-NLS-1$
				return null;
			} else {
				if (!unit.isSectionNameValid(name, parentSection))
					if (modifyMarkers)
						markers.marker(this, Problem.UnknownSection, unit, start+1, tell()-1, Markers.ABSOLUTE_MARKER_LOCATION|Markers.NO_THROW, IMarker.SEVERITY_WARNING, name);
				end = tell();
				eat(BufferedScanner.NEWLINE_CHARS); // ignore rest of section line
			}
			final int psoff = parentSection != null ? parentSection.absoluteOffset() : 0;
			final IniSection section = new IniSection(new SourceLocation(start-psoff, end-psoff), name);
			section.setParent(parentSection != null ? parentSection : unit);
			section.setIndentation(indentation);
			// parse entries
			IniItem item = null;
			section.setDefinition(unit.sectionDataFor(section, parentSection));
			while ((item = parseSectionOrEntry(section, modifyMarkers, section)) != null)
				section.addDeclaration((Declaration)item);
			// include single line ending following the entries so that editor region collapse management works properly
			skipSingleLineEnding();
			section.setEnd(tell()-psoff);
			return section;
		}
		else {
			seek(rollback);
			return null;
		}
	}

	protected boolean skipComment() {
		eatWhitespace();
		int _r;
		for (_r = read(); _r == 0; _r = read());
		final char r = (char) _r;
		if (r == ';' || r == '#') {
			readStringUntil('\n');
			eatWhitespace();
			return true;
		}
		else if (r == '/')
			switch (read()) {
			case '/':
				eatUntil(BufferedScanner.NEWLINE_CHARS);
				eatWhitespace();
				return true;
			case '*':
				for (;!reachedEOF();)
					if (read() == '/')
						break;
					else
						unread();
				eatWhitespace();
				return true;
			default:
				unread(); unread();
				return false;
			}
		else {
			unread();
			return false;
		}
	}

	public final synchronized void parse(boolean modifyMarkers) throws ProblemException {
		reset();
		parseBuffer(modifyMarkers);
	}

	public synchronized void parseBuffer(boolean modifyMarkers) throws ProblemException {
		unit.startParsing();
		try {
			if (modifyMarkers && unit.file() != null)
				try {
					unit.file().deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ONE);
					// deactivate creating markers if it's contained in a linked group
					modifyMarkers = C4GroupItem.groupItemBackingResource(unit.file()) == null;
				} catch (final CoreException e) {
					e.printStackTrace();
				}
			unit.clear();
			seek(0);
			IniSection section;
			while ((section = parseSection(modifyMarkers, null)) != null) {
				unit.sectionsMap.put(section.name(), section);
				unit.sectionsList.add(section);
			}
		} finally {
			unit.endParsing();
		}
	}

	protected IniEntry parseEntry(IniSection section, boolean modifyMarkers, IniSection parentSection) throws ProblemException {
		final int targetIndentation = parentSection != null ? parentSection.indentation() : 0;
		final int rollback = tell();
		while (skipComment());
		eatWhitespace();
		final int indentation = currentIndentation();
		if (indentation != targetIndentation || read() == '[') {
			seek(rollback);
			return null;
		}
		unread();
		if (reachedEOF()) {
			seek(rollback);
			return null;
		}
		final int keyStart = tell();
		final String key = readIdent();
		eatWhitespace();
		if (read() != '=')
			if (modifyMarkers)
				markers.marker(this, Problem.TokenExpected, unit, keyStart+key.length(), tell(), Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION, IMarker.SEVERITY_ERROR, (Object)"="); //$NON-NLS-1$
		eat(BufferedScanner.WHITESPACE_WITHOUT_NEWLINE_CHARS);
		final int valStart = tell();
		String value = readStringUntil(BufferedScanner.NEWLINE_CHARS);
		int valEnd = tell();
		final int commentStart = value != null ? value.indexOf('#') : -1;
		if (commentStart != -1) {
			valEnd -= value.length()-commentStart;
			value = value.substring(0, commentStart);
		}
		final IniEntry entry = new IniEntry(keyStart, valEnd, key, defaulting(value, ""));
		entry.setParent(section);
		try {
			unit.validateEntry(entry, section, modifyMarkers);
			if (entry.value() instanceof ASTNode)
				((ASTNode)entry.value()).setLocation(valStart-keyStart, valEnd-keyStart);
			return entry;
		} catch (final IniParserException e) {
			if (modifyMarkers)
				markers.marker(this, Problem.GenericError, unit, e.offset(), e.endOffset(),
					Markers.NO_THROW|Markers.ABSOLUTE_MARKER_LOCATION,
					e.severity(), (Object)e.getMessage());
			return entry;
		}
	}

	protected IniItem parseSectionOrEntry(IniSection section, boolean modifyMarkers, IniSection parentSection) throws ProblemException {
		final IniEntry entry = parseEntry(section, modifyMarkers, parentSection);
		if (entry != null)
			return entry;
		final IniSection sec = parseSection(modifyMarkers, parentSection);
		if (sec != null)
			return sec;

		return null;
	}

	@Override
	public IFile file() { return file; }
	@Override
	public Declaration container() { return unit; }
	@Override
	public int fragmentOffset() { return 0; }

}