func Main() {
	Log("Setting up workspace");
	setupWorkspace("/Users/madeen/Projects/Clonk/openclonk.git", "/Applications/Games/Clonk");
	linkFolderAsProject("/Users/madeen/Desktop/ClonkRage.oc/openclonk.app/Contents/Resources", "ClonkRage.oc", "OpenClonk");
	convertProject("ClonkRage", "ClonkRage.oc", {
		idMap: mapIDToName("ClonkRage", {})
	});
}