func Main() {
	Log("Setting up workspace");
	setupWorkspace("/Users/madeen/Projects/Clonk/openclonk.git", "/Applications/Games/Clonk");
	//linkFolderAsProject("/Users/madeen/Desktop/HorrorWorkspace", "Horror", "ClonkRage");
	//linkFolderAsProject("/Users/madeen/Desktop/HorrorWorkspace.oc", "Horror.oc", "OpenClonk");
	linkFolderAsProject("/Users/madeen/Desktop/ClonkRage.oc", "ClonkRage.oc", "OpenClonk");
	convertProject("ClonkRage", "ClonkRage.oc", {
		idMap: mapIDToName("ClonkRage", {
			CLNK: "RageClonk"
		}
	});
}