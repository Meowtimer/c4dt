package net.arctics.clonk.debug;

import net.arctics.clonk.parser.c4script.C4Function;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.IPersistableSourceLocator;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

public class ClonkDebugSourceLocator implements ISourceLocator, IPersistableSourceLocator {

	public ClonkDebugSourceLocator() {
		System.out.println("Created Source Locator");
	}
	
	@Override
	public Object getSourceElement(IStackFrame stackFrame) {
		C4Function f = ((ClonkDebugStackFrame)stackFrame).getFunction();
		if (f != null) {
			return f.getScript().getEditorInput();
		}
		else
			return null;
	}

	@Override
	public String getMemento() throws CoreException {
		return "yep";
	}

	@Override
	public void initializeDefaults(ILaunchConfiguration configuration)
			throws CoreException {
		
	}

	@Override
	public void initializeFromMemento(String memento) throws CoreException {
		// done!
	}

}
