package net.arctics.clonk.parser.c4script.inference.dabble

import net.arctics.clonk.DefinitionInfo
import net.arctics.clonk.TestBase
import net.arctics.clonk.index.Definition
import net.arctics.clonk.index.MetaDefinition
import net.arctics.clonk.parser.Markers
import net.arctics.clonk.parser.Problem;
import net.arctics.clonk.parser.c4script.C4ScriptParserTest
import net.arctics.clonk.parser.c4script.PrimitiveType
import net.arctics.clonk.parser.c4script.ast.TypeChoice

import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.Assert
import org.junit.Test

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
	public void testTypeChoiceIfElse() {
		def setup = new Setup(
			"""
			func IfElse()
			{
				var x = 123;
				if (Random(2))
					x = "ugh";
				else if (Random(3))
					x = true;
			}
			"""
		)
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
	public void testCallTypesChained() {
		def baseSource =
			"""
			func Func3(s)
			{
				Log(s);
			}
			
			func Func1()
			{
				CreateObject(Derived)->Func2(123);
			}
			"""
		def derivedSource =
			"""
			#include Base
			
			func Func2(x)
			{
				CreateObject(Base)->Func3(x);
			}
			"""
		def order = { baseThenDerived ->
			Setup setup
			Definition base, derived
			if (baseThenDerived) {
				setup = new Setup(
					new DefinitionInfo(source:baseSource, name:'Base'),
					new DefinitionInfo(source:derivedSource, name:'Derived')
				)
				(base, derived) = setup.scripts
			} else {
				setup = new Setup(
					new DefinitionInfo(source:derivedSource, name:'Derived'),
					new DefinitionInfo(source:baseSource, name:'Base'),
				)
				(derived, base) = setup.scripts
			}
			setup.parsers.each { it.run() }
			setup.index.refresh()
			setup.scripts.each { it.generateCaches() }
			setup.inference.run()
			Assert.assertEquals(PrimitiveType.STRING, base.findLocalFunction("Func3", false).parameters()[0].type())
			Assert.assertEquals(PrimitiveType.STRING, derived.findLocalFunction("Func2", false).parameters()[0].type)
			Assert.assertTrue(setup.inferenceMarkers.size() >= 1)
			setup.inferenceMarkers.each { it -> Assert.assertTrue(
				it.code == Problem.IncompatibleTypes ||
				it.code == Problem.ConcreteArgumentMismatch
			)}
		}
		order(true)
		order(false)
	}

	@Test
	public void testReturnTypeOfInheritedFunctionRevisited() {
		def definitions = [
			new DefinitionInfo(source:"""// Base
func Type() { return Clonk; }
func MakeObject() {
  return CreateObject(Type());
}""", name:'Base'),
			new DefinitionInfo(source:"""// Derived
#include Base

func Type() { return Wipf; }

func Usage() {
  // MakeObject() typed
  //as instance of type Wipf
  MakeObject()->WipfMethod();
}""", name:'Derived'),
			new DefinitionInfo(source:'', name:'Clonk'),
			new DefinitionInfo(source:'', name:'Wipf')
		] as DefinitionInfo[]

		def setup = new Setup(definitions)
		def (base, derived, clonk, wipf) = setup.scripts
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.generateCaches() }
		setup.inference.run()

		Assert.assertEquals(clonk, base.functionReturnTypes()['MakeObject'])
		Assert.assertEquals(new MetaDefinition(clonk), base.functionReturnTypes()['Type'])
		Assert.assertEquals(wipf, derived.functionReturnTypes()['MakeObject'])
		Assert.assertEquals(new MetaDefinition(wipf), derived.functionReturnTypes()['Type'])
	}
	
	@Test
	public void testCreateObjectResult() {
		def setup = new Setup(
"""
func Test()
{
	var t = Clonk;
	var a = CreateObject(Clonk);
	var b = CreateObject(t);
}
""", new DefinitionInfo(source:'', name:'Clonk'))
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.generateCaches() }
		setup.inference.run()
		
		def (scr, clonk) = setup.scripts
		Assert.assertEquals(clonk, scr.findLocalFunction("Test", false).findDeclaration("a").type())
		Assert.assertEquals(clonk, scr.findLocalFunction("Test", false).findDeclaration("b").type())
	}
	
	@Test
	public void testCallTypeConsensus() {
		def setup = new Setup(
			new DefinitionInfo(source:
				"""
				public func Action()
				{
					var test = CreateObject(B);
					test->CallSomeMethod(CreateObject(Clonk));
					test->CallSomeMethod(123);
					test->CallSomeMethod(321);
				    test->CallSomeMethod(CreateObject(Clonk));
				}
				""", name: 'A'
			),
			new DefinitionInfo(source:'', name:'Clonk'),
			new DefinitionInfo(source:
				"""
				public func CallSomeMethod(obj)
				{
					obj->RequireThisMethod();
				}
				""", name: 'B'
			)
		)
		
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.generateCaches() }
		setup.inference.run()
		
		def (a, clonk, b) = setup.scripts
		Assert.assertEquals(clonk, b.findLocalFunction('CallSomeMethod', false).parameter(0).type())
	}

}
