package net.arctics.clonk.parser.c4script;

import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IStorage;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.SimpleScriptStorage;

public class C4StandaloneScript extends C4ScriptBase {

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;
	private static final ClonkIndex STANDALONE_INDEX = new ClonkIndex();
	
	private String scriptText;
	
	public C4StandaloneScript() {
		super();
	}
	
	public String getScriptText() {
		return scriptText;
	}

	public void setScriptText(String script) {
		this.scriptText = script;
	}

	@Override
	public ClonkIndex getIndex() {
		return STANDALONE_INDEX;
	}

	@Override
	public IStorage getScriptStorage() {
		try {
			return new SimpleScriptStorage(getName(), scriptText);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

}
