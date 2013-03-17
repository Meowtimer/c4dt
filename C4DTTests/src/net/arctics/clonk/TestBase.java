package net.arctics.clonk;

import org.junit.Before;

public abstract class TestBase {
	public static final String ENGINE = "OpenClonk";
	@Before
	public void headlessSetup() {
		Core.headlessInitialize(System.getenv("HOME")+"/Projects/Clonk/C4DT/C4DT/res/engines", ENGINE);
	}
}
