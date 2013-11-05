package net.arctics.clonk.c4script;

import static java.lang.String.format;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.Index;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

public class LocalizedScript extends SystemScript {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	public static final Pattern FILENAME_PATTERN = Pattern.compile("Script(..)\\.c", Pattern.CASE_INSENSITIVE);
	public LocalizedScript(Index index, IFile scriptFile) throws CoreException {
		super(index, scriptFile);
		final Matcher m = FILENAME_PATTERN.matcher(scriptFile.getName());
		if (!m.matches())
			throw new IllegalArgumentException(format("%s needs a file whose name matches '%s'", getClass().getSimpleName(), FILENAME_PATTERN));
		if (definition() == null)
			throw new IllegalArgumentException(format("%s needs a file contained in a definition folder", getClass().getSimpleName()));
		language = m.group(1);
	}
	@Override
	public boolean gatherIncludes(Index contextIndex, Script origin, Collection<Script> set, int options) {
		if (!super.gatherIncludes(contextIndex, origin, set, options))
			return false;
		set.add(definition());
		return true;
	}
	private final String language;
	public String language() { return language; }
	public Definition definition() { return Definition.at(file().getParent()); }
}
