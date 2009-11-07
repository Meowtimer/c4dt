package net.arctics.clonk.debug;

import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

public class ClonkDebugSourceLookupDirector extends AbstractSourceLookupDirector {

	public ClonkDebugSourceLookupDirector() {
		System.out.println("Created Source Locator");
	}

	@Override
	public void initializeParticipants() {
		this.addParticipants(new ISourceLookupParticipant[] {new ClonkDebugSourceLookupParticipant()});
	}

}
