package net.arctics.clonk.parser.c4script;

import java.io.InputStream;

import org.eclipse.core.resources.IStorage;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.index.C4GroupEntryStorage;
import net.arctics.clonk.index.ClonkIndex;
import net.arctics.clonk.index.IContainedInExternalLib;
import net.arctics.clonk.index.IExternalScript;
import net.arctics.clonk.preferences.ClonkPreferences;
import net.arctics.clonk.resource.ExternalLib;
import net.arctics.clonk.resource.c4group.C4GroupEntry;
import net.arctics.clonk.util.ITreeNode;
import net.arctics.clonk.util.Utilities;

public class C4ScriptExtern extends C4ScriptBase implements IExternalScript {
	
	private static final long serialVersionUID = 1L;
	
	private IStorage scriptStorage;
	private ITreeNode parentNode;
	private transient ExternalLib externalLib;

	public C4ScriptExtern(C4GroupEntry script, ITreeNode parentNode) {
		this.parentNode = parentNode;
		if (parentNode != null)
			parentNode.addChild(this);
		setName(script.getName());
		scriptStorage = new C4GroupEntryStorage((IContainedInExternalLib)this.getParentNode(), script);
		//scriptStorage = new SimpleScriptStorage((C4GroupEntry) script);
	}
	
	@Override
	public ClonkIndex getIndex() {
		ExternalLib lib = getExternalLib();
		return lib != null ? getExternalLib().getIndex() : null;
	}

	@Override
	public String getScriptText() {
		try {
			InputStream contents = scriptStorage.getContents();
			try {
				return Utilities.stringFromInputStream(scriptStorage.getContents(), ClonkPreferences.getPreferenceOrDefault(ClonkPreferences.EXTERNAL_INDEX_ENCODING));
			} finally {
				contents.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Override
	public Object getScriptFile() {
		return scriptStorage;
	}
	
	@Override
	public ITreeNode getParentNode() {
		return parentNode;
	}
	
	public ExternalLib getExternalLib() {
		// copypasta'd
		if (externalLib == null)
			for (ITreeNode node = this; node != null; node = node.getParentNode())
				if (node instanceof ExternalLib) {
					externalLib = (ExternalLib) node;
					break;
				}
		return externalLib;
	}

}
