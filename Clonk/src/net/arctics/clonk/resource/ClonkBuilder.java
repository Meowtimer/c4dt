package net.arctics.clonk.resource;

import java.util.Map;

import net.arctics.clonk.Utilities;
import net.arctics.clonk.parser.C4DefCoreWrapper;
import net.arctics.clonk.parser.C4Object;
import net.arctics.clonk.parser.C4ObjectParser;
import net.arctics.clonk.parser.C4ScriptParser;
import net.arctics.clonk.parser.ClonkIndex;
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
 */
public class ClonkBuilder extends IncrementalProjectBuilder implements IResourceDeltaVisitor, IResourceVisitor {
	
	private int buildPhase;
	
	public ClonkBuilder() {
		super();
	}

	@SuppressWarnings("unchecked")
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor)
			throws CoreException {
		try {
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
					for (buildPhase = 0; buildPhase < 2; buildPhase++)
						proj.accept(this);
				}
				proj.touch(monitor);
			}
			Utilities.getProject(proj).getIndexedData().refreshCache();
			monitor.done();
			return new IProject[] { proj };
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public boolean visit(IResourceDelta delta) throws CoreException {
		if (delta == null) 
			return false;

		if (delta.getResource() instanceof IFile)
			if (delta.getKind() != IResourceDelta.REMOVED) {
				C4Object container = C4Object.objectCorrespondingTo(delta.getResource().getParent());
				if (container != null) {
					try {
						if (delta.getResource().getName().endsWith(".c")) {
							// remove and add object to refresh globalFunctions/staticVariables
							ClonkIndex index = Utilities.getProject(delta.getResource()).getIndexedData();
							index.removeObject(container);
							new C4ScriptParser((IFile) delta.getResource(), container).parse();
							index.addObject(container);
						}
						else if (delta.getResource().getName().equals("DefCore.txt")) {
							new C4DefCoreWrapper((IFile) delta.getResource()).parse();
						}
					} catch (CompilerException e) {
						// TODO display CompilerException messages
						e.printStackTrace();
					}
				} else {
					/// ???
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
//		if (resource instanceof IFile) {
//			C4Object container = Utilities.getProject(resource).getIndexedData()
//				.getObject(C4ID.getID(resource.getParent().getPersistentProperty(ClonkCore.FOLDER_C4ID_PROPERTY_ID)));
//			try {
//				if (resource.getName().endsWith(".c")) {
//					new C4ScriptParser((IFile) resource, container).parse();
//				}
//				else if (resource.getName().equals("DefCore.txt")) {
//					new C4DefCoreParser((IFile) resource).parse();
//				}
//			} catch (CompilerException e) {
//				e.printStackTrace();
//			}
//			return false;
//		}
		if (resource instanceof IContainer) {
			switch (buildPhase) {
			case 0:
				// first phase: just gather declarations
				C4ObjectParser parser = C4ObjectParser.create((IContainer) resource);
				if (parser != null) { // is complete c4d (with DefCore.txt Script.c and Graphics)
					try {
						parser.parse();
					} catch (CompilerException e) {
						// TODO display CompilerException messages
						e.printStackTrace();
					}
				}
				return true;
			case 1:
				// check correctness of function code (or compile into bytecodes ;D)
				ClonkIndex index = Utilities.getProject(resource).getIndexedData();
				C4Object obj = index.getObject((IContainer)resource);
				if (obj != null && obj.getScript() != null) {
					try {
						new C4ScriptParser((IFile)obj.getScript(), obj).parseFunctionCode();
					} catch (CompilerException e) {
						e.printStackTrace();
					} 
				}
				return true;
			default:
				return false; // :C ?
			}
		}
		else return false;
	}

}
