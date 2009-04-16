package net.arctics.clonk.parser;

import java.util.ArrayList;
import java.util.List;

import net.arctics.clonk.resource.ExternalLib;

public class ExternIndex extends ClonkIndex {
	
	private static final long serialVersionUID = 1L;

	private List<ExternalLib> libs;
	
	public ExternIndex() {
		libs = new ArrayList<ExternalLib>();
	}
	
	public List<ExternalLib> getLibs() {
		return libs;
	}
	
	@Override
	public void clear() {
		super.clear();
		libs.clear();
	}

}
