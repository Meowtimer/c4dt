package net.arctics.clonk;

import java.io.File;

/**
 * VSCode language server wrapped by some node module
 * @author madeen
 */
public class LanguageServer {

	final String extensionFolder;

	public LanguageServer(String extensionFolder) {
		this.extensionFolder = extensionFolder;
		Core.headlessInitialize(new File(extensionFolder, "C4DT/res/engines").getAbsolutePath(), "OpenClonk");
	}

	public String helloWorld() {
		return "Indeed";
	}

}
