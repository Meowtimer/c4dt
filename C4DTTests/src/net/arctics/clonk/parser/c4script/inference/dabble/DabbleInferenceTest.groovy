package net.arctics.clonk.parser.c4script.inference.dabble

import net.arctics.clonk.TestBase
import net.arctics.clonk.parser.Markers
import net.arctics.clonk.parser.ParsingException
import net.arctics.clonk.parser.c4script.C4ScriptParserTest
import net.arctics.clonk.parser.c4script.Script
import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.Test
import org.junit.Assert

public class DabbleInferenceTest extends TestBase {

	static class Setup extends C4ScriptParserTest.Setup {
		final DabbleInference inference = new DabbleInference();
		final Markers inferenceMarkers = new Markers();
		Setup(final String script) {
			super(script)
			inference.initialize(inferenceMarkers, new NullProgressMonitor(), [this.script] as Script[])
		}
	}
	
	@Test
	public void testAcceptThisCallToUnknownFunction() throws UnsupportedEncodingException, ParsingException {
		def setup = new Setup(
			"""func Test() {
	var obj = CreateObject(GetID());
	Log(obj->Unknown());
}""")
		setup.parser.parse()
		setup.inference.run()
		Assert.assertTrue(setup.inferenceMarkers.size() == 0)
	}

}
