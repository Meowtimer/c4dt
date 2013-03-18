package net.arctics.clonk.parser.c4script.inference.dabble

import net.arctics.clonk.DefinitionInfo
import net.arctics.clonk.TestBase
import net.arctics.clonk.index.Definition
import net.arctics.clonk.parser.Markers
import net.arctics.clonk.parser.ParsingException
import net.arctics.clonk.parser.c4script.C4ScriptParserTest
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Script
import net.arctics.clonk.parser.c4script.ast.TypeChoice
import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.Test
import org.junit.Assert

public class DabbleInferenceTest extends TestBase {

	static class Setup extends C4ScriptParserTest.Setup {
		final DabbleInference inference = new DabbleInference();
		final Markers inferenceMarkers = new Markers();
		Setup(final... scripts) {
			super(scripts)
			inference.initialize(inferenceMarkers, new NullProgressMonitor(), this.scripts)
		}
	}
	
	@Test
	public void testAcceptThisCallToUnknownFunction() {
		def setup = new Setup(
			"""func Test() {
	var obj = CreateObject(GetID());
	Log(obj->Unknown());
}""")
		setup.parser.parse()
		setup.inference.run()
		Assert.assertTrue(setup.inferenceMarkers.size() == 0)
	}
	
	@Test
	public void testTypeChoiceForIf() {
		def setup = new Setup(
			"""func IfElse() 
		{
			var x = 123;
			if (Random(2))
				x = "ugh";
			else if (Random(3))
				x = true;
		}""")
		setup.parser.run()
		setup.inference.run()
		def ty = setup.script.findLocalFunction("IfElse", false).locals()[0].type()
		Assert.assertTrue(ty instanceof TypeChoice)
		def types = (ty as TypeChoice).flatten()
		Assert.assertEquals(2, types.size())
		Assert.assertTrue(types.contains(PrimitiveType.INT))
		Assert.assertTrue(types.contains(PrimitiveType.STRING))
	}
	
	@Test
	public void testConcreteParameterTypesChained() {
		def baseSource =
		"""
func Func3(s)
{
	Log(s);
}

func Func1()
{
	CreateObject(Derived)->Func2(123);
}"""
		def derivedSource =
		"""#include Base

func Func2(x)
{
	CreateObject(Base)->Func3(x);	
}"""
		def setup = new Setup(
			new DefinitionInfo(source:derivedSource, name:'Derived'),
			new DefinitionInfo(source:baseSource, name:'Base'),
		)
		def base = setup.scripts[1] as Definition
		def derived = setup.scripts[0] as Definition
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.generateCaches() }
		setup.inference.run()
		Assert.assertEquals(PrimitiveType.STRING, base.findLocalFunction("Func3", false).parameters()[0].type())
		Assert.assertEquals(PrimitiveType.STRING, derived.findLocalFunction("Func2", false).parameters()[0].type)
		//Assert.assertEquals(1, setup.inferenceMarkers.size())
	}

}
