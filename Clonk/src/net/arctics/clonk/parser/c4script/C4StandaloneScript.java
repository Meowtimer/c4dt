package net.arctics.clonk.parser.c4script;

import java.io.UnsupportedEncodingException;

import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.parser.SimpleScriptStorage;

public class C4StandaloneScript extends C4ScriptBase {

	private static final long serialVersionUID = 1L;
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
	public Object getScriptFile() {
		try {
			return new SimpleScriptStorage(getName(), scriptText);
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

}
