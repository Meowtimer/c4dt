package net.arctics.clonk.resource;

import java.util.Map;

import net.arctics.clonk.parser.C4ScriptParser;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An incremental builder for all project data.
 * This builder launches the parser that indexes all c4objects and highlights syntax errors.
 * @author ZokRadonh
 *
 */
public class ClonkBuilder extends IncrementalProjectBuilder {

	public ClonkBuilder() {
		// TODO Auto-generated constructor stub
	}

	@Override
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		switch(kind) {
		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
		case FULL_BUILD:
		case CLEAN_BUILD:
			C4ScriptParser parser = new C4ScriptParser();
			IProject proj = getProject();
			IResourceDelta delta = getDelta(proj);
			if (delta != null)
				delta.accept(parser);
		}
		return null;
	}

}
