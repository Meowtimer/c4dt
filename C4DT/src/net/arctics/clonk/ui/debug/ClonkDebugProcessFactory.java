package net.arctics.clonk.ui.debug;

import java.util.Map;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.IProcessFactory;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.RuntimeProcess;

public class ClonkDebugProcessFactory implements IProcessFactory {

	@Override
	public IProcess newProcess(ILaunch launch, Process process, String label, @SuppressWarnings("rawtypes") Map attributes) {
		return new RuntimeProcess(launch, process, label, attributes) {
			
		};
	}

}
