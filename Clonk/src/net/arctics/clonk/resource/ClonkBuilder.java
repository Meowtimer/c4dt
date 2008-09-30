package net.arctics.clonk.resource;

import java.util.Map;

import net.arctics.clonk.parser.C4DefCoreParser;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.CompilerException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An incremental builder for all project data.
 * This builder launches the parser that indexes all c4objects and highlights syntax errors.
 * @author ZokRadonh
 *
 */
public class ClonkBuilder extends IncrementalProjectBuilder implements IResourceDeltaVisitor, IResourceVisitor {

	public ClonkBuilder() {
		super();
		// intentionally left blank
	}

	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		
		IProject proj = getProject();
		monitor.beginTask("Build project " + proj.getName(), 1);
		switch(kind) {
		case AUTO_BUILD:
		case INCREMENTAL_BUILD:
			IResourceDelta delta = getDelta(proj);
			if (delta != null)
				delta.accept(this);
			delta.getResource().touch(monitor);
			break;
		case FULL_BUILD:
		case CLEAN_BUILD:
			if (proj != null) {
				proj.accept(this);
			}
			proj.touch(monitor);
		}
		monitor.done();
		return null;
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;
		
		if (delta.getResource() instanceof IFile)
			if (delta.getKind() != IResourceDelta.REMOVED) {
				if (delta.getResource().getName().endsWith(".c")) {
					try {
						C4ScriptParser parser = new C4ScriptParser((IFile) delta.getResource());
						parser.parse();
					} catch (CompilerException e) {
						e.printStackTrace();
					}
				}
				else if (delta.getResource().getName().equals("DefCore.txt")) {
					C4DefCoreParser.getInstance().update(delta.getResource());
				}
			}
		if (delta.getResource() instanceof IContainer)
			return true;
		else
			return false;
	}

	public boolean visit(IResource resource) throws CoreException {
		if (resource == null)
			return false;
		if (resource instanceof IFile) {
			if (resource.getName().endsWith(".c")) {
				try {
					C4ScriptParser parser = new C4ScriptParser((IFile) resource);
					parser.parse();
				} catch (CompilerException e) {
					e.printStackTrace();
				}
			}
			else if (resource.getName().equals("DefCore.txt")) {
				C4DefCoreParser.getInstance().update(resource);
			}
			return false;
		}
		if (resource instanceof IContainer) return true;
		else return false;
	}

}
