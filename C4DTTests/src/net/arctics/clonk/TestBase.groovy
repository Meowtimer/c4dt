package net.arctics.clonk;

import org.eclipse.jface.text.IDocument;
import org.junit.Before;

public abstract class TestBase {
	public static final String ENGINE = "OpenClonk";
	@Before
	public void headlessSetup() {
		Core.headlessInitialize(System.getenv("HOME")+"/Projects/Clonk/C4DT/C4DT/res/engines", ENGINE);
		Flags.DEBUG = false;
	}
	protected IDocument documentMock(String source) {
		[
			get: { offset, len ->
				(offset != null && len != null) ? source.substring(offset, offset+len) : source },
			getChar: { ndx -> source[ndx] as char },
			getLength: { source.length() }
		] as IDocument
	}
}
