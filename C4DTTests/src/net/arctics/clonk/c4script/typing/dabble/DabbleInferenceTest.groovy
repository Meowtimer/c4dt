package net.arctics.clonk.c4script.typing.dabble

import net.arctics.clonk.DefinitionInfo
import net.arctics.clonk.Problem;
import net.arctics.clonk.TestBase
import net.arctics.clonk.c4script.typing.ArrayType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeChoice;
import net.arctics.clonk.c4script.typing.dabble.DabbleInference;
import net.arctics.clonk.index.Definition
import net.arctics.clonk.index.MetaDefinition
import net.arctics.clonk.parser.Markers
import net.arctics.clonk.c4script.ScriptParserTest
import net.arctics.clonk.c4script.ast.ThisType;
import net.arctics.clonk.util.StringUtil;
import net.arctics.clonk.util.TaskExecution;

import org.eclipse.core.runtime.NullProgressMonitor
import org.junit.Assert
import org.junit.Test

import org.paukov.combinatorics.Factory
import org.paukov.combinatorics.ICombinatoricsVector;

public class DabbleInferenceTest extends TestBase {

	static class Setup extends ScriptParserTest.Setup {
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
			setup.scripts.each { it.deriveInformation() }
			setup.inference.run()
			Assert.assertEquals(PrimitiveType.STRING.unified(), base.findLocalFunction("Func3", false).parameters()[0].type())
			Assert.assertEquals(PrimitiveType.STRING.unified(), derived.findLocalFunction("Func2", false).parameters()[0].type())
			Assert.assertTrue(setup.inferenceMarkers.size() >= 1)
			setup.inferenceMarkers.each { it -> Assert.assertTrue(
				it.code == Problem.IncompatibleTypes ||
				it.code == Problem.ConcreteArgumentMismatch
			)}
		}
		int successful = 0
		int runs = 0
		for (int cores = 1; cores <= Runtime.getRuntime().availableProcessors(); cores++) {
			TaskExecution.threadPoolSize = cores
			System.out.println("Using $cores cores")
			for (int i = 1; i <= 100; i++) {
				System.out.println("PASS $i")
				try {
					runs++
					order(true)
					order(false)
					successful++
				} catch (AssertionError ae) {
					System.out.println("(cores=$cores, pass=$i, ${ae.getMessage()})")
				}
			}
		}
		if (successful < runs)
			Assert.fail("$successful of $runs passes successful")
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
		setup.scripts.each { it.deriveInformation() }
		setup.inference.run()

		Assert.assertEquals(clonk, base.typings().functionReturnTypes['MakeObject'])
		Assert.assertEquals(new MetaDefinition(clonk), base.typings().functionReturnTypes['Type'])
		Assert.assertEquals(wipf, derived.typings().functionReturnTypes['MakeObject'])
		Assert.assertEquals(new MetaDefinition(wipf), derived.typings().functionReturnTypes['Type'])
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
		setup.scripts.each { it.deriveInformation() }
		setup.inference.run()

		def (scr, clonk) = setup.scripts
		Assert.assertEquals(clonk, scr.findLocalFunction("Test", false).findDeclaration("a").type())
		Assert.assertEquals(clonk, scr.findLocalFunction("Test", false).findDeclaration("b").type())
	}

	@Test
	public void testCallTypeConsensus() {

		def infos = [
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
		]

		def vector = Factory.createVector(infos)
		def gen = Factory.createPermutationGenerator(vector)

		for (ICombinatoricsVector<DefinitionInfo> perm : gen) {
			def permArray = perm.vector.toArray()
			System.out.println(String.format('Testing permutation %s', permArray.collect({it -> it.name}).join(', ')))
			def setup = new Setup(permArray)

			setup.parsers.each { it.run() }
			setup.index.refresh()
			setup.scripts.each { it.deriveInformation() }
			setup.inference.run()

			Definition a = setup.scripts.find { it -> (it as Definition).id().stringValue().equals('A') }
			Definition b = setup.scripts.find { it -> (it as Definition).id().stringValue().equals('B') }
			Definition clonk = setup.scripts.find { it -> (it as Definition).id().stringValue().equals('Clonk') }
			Assert.assertEquals(clonk, b.findLocalFunction('CallSomeMethod', false).parameter(0).type())
		}
	}

	@Test
	public void testRevisitIncluded() {
		def baseSource =
		"""
		local count;
		public func SetStackCount(int amount)
		{
			count = amount;
		}
		public func TakeObject()
		{
			if (count == 1)
			{
				Exit();
				return this;
			}
			else if (count > 1)
			{
				var take = CreateObject(GetID(), 0, 0, GetOwner());
				take->SetStackCount(1);
				return take;
			}
		}
		"""
		def derivedSource =
		"""
		#include Base
		func Test()
		{
			return TakeObject();
		}
		"""
		def setup = new Setup(
			new DefinitionInfo(name: "Base", source: baseSource),
			new DefinitionInfo(name: "Derived", source: derivedSource)
		)

		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.deriveInformation() }
		setup.inference.run()

		def (base, derived) = setup.scripts

		def t = derived.typings().functionReturnTypes['TakeObject']
		Assert.assertEquals(derived, t)
	}

	@Test
	public void testReturnTypeOfInheritedFunctionRevisitedIndirect() {
		def definitions = [
			new DefinitionInfo(name:'Base', source:
				"""// Base
				func Type() { return Clonk; }
				func MakeObject() {
					return CreateObject(Type());
				}"""
			),
			new DefinitionInfo(name:'Derived', source:
				"""
				#include Base

				func Type() { return Wipf; }

				func Usage() {
					// MakeObject() typed
					//as instance of type Wipf
					MakeObject()->WipfMethod();
				}"""
			),
			new DefinitionInfo(name:'Clonk', source:''),
			new DefinitionInfo(name: 'Wipf', source:''),
			new DefinitionInfo(name: 'User', source:
				"""
				func Usage() {
					return CreateObject(Derived)->MakeObject();
				}
				"""
			)
		] as DefinitionInfo[]

		def setup = new Setup(definitions)
		def (base, derived, clonk, wipf, user) = setup.scripts
		setup.parsers.each { it.run() }
		setup.index.refresh()
		setup.scripts.each { it.deriveInformation() }
		setup.inference.run()

		Assert.assertEquals(clonk, base.typings().functionReturnTypes['MakeObject'])
		Assert.assertEquals(new MetaDefinition(clonk), base.typings().functionReturnTypes['Type'])
		Assert.assertEquals(wipf, derived.typings().functionReturnTypes['MakeObject'])
		Assert.assertEquals(new MetaDefinition(wipf), derived.typings().functionReturnTypes['Type'])
		Assert.assertEquals(wipf, user.typings().functionReturnTypes['Usage'])
	}

	@Test
	public void testAbysseses() {
		System.out.println("Abyssesses! ---")
		def funcs = [
			'''
			func MakeAbyssMarkers()
			{
				RemoveAll(Find_ID(Abyss));
				var waypoints = FindObjects(Find_ID(Waypoint), Sort_Distance(0, 0));
				WaypointsCheck(waypoints);
				var abyssObjects = CreateArray(GetLength(waypoints)/2);
				for (var i = 0; i < GetLength(waypoints); i += 2)
				{
					var num = i / 2;
					var first = waypoints[i];
					var second = waypoints[i + 1];
					var abyss = CreateObject(Abyss);
					abyssObjects[num] = PrepareAbyss(first, second, abyss, num);
				}
				return abyssObjects;
			}
			''',

			'''
			func PrepareAbyss(first, second, abyss, num)
			{
				abyss->SetLeft(first);
				abyss->SetRight(second);
				if (num == 0)
					return PrepareAbyssOne(abyss);
				else if (num == 1)
					return PrepareAbyssTwo(abyss);
			}
			''',

			'''
			func PrepareAbyssOne(abyss)
			{
				var tactic = CreateObject(RopebridgeRescue);
				var objectCreator = CreateObject(ConfigurableObjectCreator);
				objectCreator->SetType(Ropebridge_Post);
				tactic->SetObjectCreator(objectCreator);
				abyss->SetStrategy(tactic);
				return abyss;
			}
			''',

			'''
			func PrepareAbyssTwo(abyss)
			{
				var tactic = CreateObject(JumpRescue);
				abyss->SetStrategy(tactic);
				return abyss;
			}
			'''
		]

		def vector = Factory.createVector(funcs)
		def gen = Factory.createPermutationGenerator(vector)
		def failedAssertions = new ArrayList<String>()

		int counter = 0
		for (ICombinatoricsVector<String> fns : gen) {
			def rescuesBuilderSource = StringUtil.blockString("", "", "\n", fns)
			def definitions = [
				new DefinitionInfo(name:'Waypoint', source:''),
				new DefinitionInfo(name:'Abyss', source:''),
				new DefinitionInfo(name:'RescuesBuilder', source:rescuesBuilderSource)
			] as DefinitionInfo[]
			def setup = new Setup(definitions)
			def (waypoint, abyss, rescuesBuilder) = setup.scripts
			setup.parsers.each { it.run() }
			setup.index.refresh()
			setup.scripts.each { it.deriveInformation() }

			def functionNames = rescuesBuilder.functions().collect { it.name() }.toList()
			def makeAbyssMarkersIndex = functionNames.indexOf("MakeAbyssMarkers")
			def prepareAbyssIndex = functionNames.indexOf("PrepareAbyss")
			def prepareAbyssOneIndex = functionNames.indexOf("PrepareAbyssOne")
			def prepareAbyssTwoIndex = functionNames.indexOf("PrepareAbyssTwo")
			def fnOrderStr = StringUtil.blockString("", "", ", ", rescuesBuilder.functions().collect { it.name() })

			System.out.println()
			System.out.println(fnOrderStr)
			setup.inference.run()

			try {
				Assert.assertEquals(abyss, rescuesBuilder.findFunction("PrepareAbyssOne").parameter(0).type())
				Assert.assertEquals(abyss, rescuesBuilder.findFunction("PrepareAbyssTwo").parameter(0).type())
				Assert.assertEquals(new ArrayType(abyss), rescuesBuilder.findFunction("MakeAbyssMarkers").returnType())
			} catch (AssertionError e) {
				if (prepareAbyssIndex > makeAbyssMarkersIndex)
					System.out.println("--- PrepareAbyss after MakeAbyssMarkers")
				if (prepareAbyssOneIndex < prepareAbyssIndex || prepareAbyssTwoIndex < prepareAbyssIndex)
					System.out.println("--- PrepareAbyssOne or PrepareAbyssTwo before PrepareAbyss")
				def msg = String.format("%s: %s (%d)",
					fnOrderStr,
					e.getMessage(),
					counter
				)
				System.out.println(msg)
				failedAssertions.add(msg);
			}
			counter++
		}

		Assert.assertEquals(StringUtil.blockString("", "", "\n", failedAssertions), 0, failedAssertions.size())
	}

}
