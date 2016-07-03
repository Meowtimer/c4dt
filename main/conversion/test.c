func Main() {
	setupWorkspace(
		"/Users/madeen/Projects/Clonk/openclonk.git",
		"/Applications/Games/Clonk"
	);
	linkFolderAsProject("TestCR", "TestCR", "ClonkRage");
	linkFolderAsProject("TestOC", "TestOC", "OpenClonk");
	convertProject("TestCR", "TestOC", nil);
}