package net.arctics.clonk.c4script.typing.dabble;

import static net.arctics.clonk.util.StreamUtil.ofType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Assert;
import org.junit.Test;
import org.paukov.combinatorics.Factory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import net.arctics.clonk.DefinitionInfo;
import net.arctics.clonk.Problem;
import net.arctics.clonk.TestBase;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.ScriptParserTest;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.ArrayType;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.Maybe;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.c4script.typing.TypeChoice;
import net.arctics.clonk.index.Definition;
import net.arctics.clonk.index.MetaDefinition;
import net.arctics.clonk.parser.Markers;
import net.arctics.clonk.util.StringUtil;

public class DabbleInferenceTest extends TestBase {

	public static class Setup extends ScriptParserTest.Setup {
		public final DabbleInference inference = new DabbleInference();
		public final Markers inferenceMarkers = new Markers();
		public Setup(final DefinitionInfo... scripts) {
			this((Object[])scripts);
		}
		public Setup(final Object... scripts) {
			super(scripts);
			inference.configure(index, "");
			parsers.forEach(Runnable::run);
			index.refresh();
			this.scripts.forEach(Script::deriveInformation);
			inference.initialize(inferenceMarkers, new NullProgressMonitor(), this.scripts.toArray(new Script[this.scripts.size()]));
		}
		public void performInference() {
			inference.steer(() -> {
				inference.run();
				inference.apply();
				inference.run2();
			});
		}
	}

	@Test
	public void testAcceptThisCallToUnknownFunction() {
		final Setup setup = new Setup(StringUtil.join("\n",
			"func Test() {",
			"	var obj = CreateObject(GetID());",
			"	Log(obj->Unknown());",
			"}"
		));
		setup.performInference();
		Assert.assertTrue(
			StreamSupport.stream(setup.inferenceMarkers.spliterator(), true)
				.anyMatch(m -> m.code == Problem.TypingJudgment)
		);
	}

	@Test
	public void testTypeChoiceIfElse() {
		final Setup setup = new Setup(StringUtil.join("\n",
			"func IfElse()",
			"{",
			"	var x = 123;",
			"	if (Random(2))",
			"		x = \"ugh\";",
			"	else if (Random(3))",
			"		x = true;",
			"}"
		));
		setup.performInference();
		final IType ty = setup.script.findLocalFunction("IfElse", false).locals()[0].type();
		Assert.assertTrue(ty instanceof TypeChoice);
		final List<IType> types = ((TypeChoice)ty).flatten();
		Assert.assertEquals(2, types.size());
		Assert.assertTrue(types.contains(PrimitiveType.INT));
		Assert.assertTrue(types.contains(PrimitiveType.STRING));
	}

	/*
	@Test
	public void testCallTypesChained() {
		def baseSource =
			
			StringUtil.join("\n",

				func Func3(s)
			{
				Log(s);
			}

			func Func1()
			{
				CreateObject(Derived)->Func2(123);
			}
			

			)
		def derivedSource =
			
			StringUtil.join("\n",

				#include Base

			func Func2(x)
			{
				CreateObject(Base)->Func3(x);
			}
			

			)
		def order = { baseThenDerived ->
			Setup setup
			Definition base, derived
			if (baseThenDerived) {
				setup = new Setup(
					new DefinitionInfo(source:baseSource, name:"Base"),
					new DefinitionInfo(source:derivedSource, name:"Derived")
				)
				(base, derived) = setup.scripts
			} else {
				setup = new Setup(
					new DefinitionInfo(source:derivedSource, name:"Derived"),
					new DefinitionInfo(source:baseSource, name:"Base"),
				)
				(derived, base) = setup.scripts
			}
			setup.inference.perform()
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
	}*/

	@Test
	public void testReturnTypeOfInheritedFunctionRevisited() {
		final DefinitionInfo[] definitions = new DefinitionInfo[] {
			new DefinitionInfo(StringUtil.join("\n",
				"// Base",
				"func Type() { return Clonk; }",
				"func MakeObject() {",
				"  return CreateObject(Type());",
				"}"
			), "Base"),
			new DefinitionInfo(StringUtil.join("\n",
				"// Derived",
				"#include Base",
				"",
				"func Type() { return Wipf; }",
				"",
				"func Usage() {",
				"  // MakeObject() typed",
				"  //as instance of type Wipf",
				"  MakeObject()->WipfMethod();",
				"}"
			), "Derived"),
			new DefinitionInfo("", "Clonk"),
			new DefinitionInfo("", "Wipf")
		};

		final Setup setup = new Setup(definitions);
		final Script base = setup.scripts.get(0);
		final Script derived = setup.scripts.get(1);
		final Script clonk = setup.scripts.get(2);
		final Script wipf = setup.scripts.get(3);
		setup.performInference();

		Assert.assertEquals(clonk, base.typings().getFunctionTyping("MakeObject").returnType);
		Assert.assertEquals(new MetaDefinition((Definition) clonk), base.typings().getFunctionTyping("Type").returnType);
		Assert.assertEquals(wipf, derived.typings().getFunctionTyping("MakeObject").returnType);
		Assert.assertEquals(new MetaDefinition((Definition) wipf), derived.typings().getFunctionTyping("Type").returnType);
	}

	@Test
	public void testCreateObjectResult() {
		final Setup setup = new Setup(StringUtil.join("\n",
			"func Test()",
			"{",
			"	final var t = Clonk;",
			"	final var a = CreateObject(Clonk);",
			"	final var b = CreateObject(t);",
			"}"),
			new DefinitionInfo("", "Clonk")
		);
		setup.performInference();

		final Script scr = setup.scripts.get(0);
		final Script clonk = setup.scripts.get(1);
		final IType at = scr.findLocalFunction("Test", false).findLocalDeclaration("a", Variable.class).type();
		Assert.assertEquals(clonk, at);
		Assert.assertEquals(clonk, ((Variable)scr.findLocalFunction("Test", false).findDeclaration("b")).type());
	}

	@Test
	public void testCallTypeConsensus() {

		final DefinitionInfo[] infos = new DefinitionInfo[] {
			new DefinitionInfo(
				StringUtil.join("\n",
					"public func Action()",
					"{",
					"	final var test = CreateObject(B);",
					"	test->CallSomeMethod(CreateObject(Clonk));",
					"	test->CallSomeMethod(123);",
					"	test->CallSomeMethod(321);",
					"    test->CallSomeMethod(CreateObject(Clonk));",
					"}"
				),
				"A"
			),
			new DefinitionInfo("", "Clonk"),
			new DefinitionInfo(
				StringUtil.join("\n",
					"public func CallSomeMethod(obj)",
					"{",
					"	obj->RequireThisMethod();",
					"}"
				),
				"B"
			)
		};

		final ICombinatoricsVector<DefinitionInfo> vector = Factory.createVector(infos);
		final Generator<DefinitionInfo> gen = Factory.createPermutationGenerator(vector);

		for (final ICombinatoricsVector<DefinitionInfo> perm : gen) {
			final DefinitionInfo[] permArray = perm.getVector().toArray(new DefinitionInfo[0]);
			System.out.println(String.format("Testing permutation %s", Arrays.stream(permArray).map(it -> it.name).collect(Collectors.joining(", "))));
			final Setup setup = new Setup(permArray);

			setup.performInference();

			final Definition b = ofType(setup.scripts.stream(), Definition.class).filter(it -> it.id().stringValue().equals("B")).findFirst().orElse(null);
			final Definition clonk = ofType(setup.scripts.stream(), Definition.class).filter(it -> it.id().stringValue().equals("Clonk")).findFirst().orElse(null);
			final IType expectedParmType = TypeChoice.make(clonk, PrimitiveType.INT);
			Assert.assertEquals(expectedParmType, b.findLocalFunction("CallSomeMethod", false).parameter(0).type());
		}
	}

	@Test
	public void testRevisitIncluded() {
		final String baseSource = StringUtil.join("\n",
			"local count;",
			"public func SetStackCount(int amount)",
			"{",
			"	count = amount;",
			"}",
			"public func TakeObject()",
			"{",
			"	if (count == 1)",
			"	{",
			"		Exit();",
			"		return this;",
			"	}",
			"	else if (count > 1)",
			"	{",
			"		final var take = CreateObject(GetID(), 0, 0, GetOwner());",
			"		take->SetStackCount(1);",
			"		return take;",
			"	}",
			"}"
		);
		final String derivedSource = StringUtil.join("\n",
			"#include Base",
			"func Test()",
			"{",
			"	return TakeObject();",
			"}"
		);
		final Setup setup = new Setup(
			new DefinitionInfo(baseSource, "Base"),
			new DefinitionInfo(derivedSource, "Derived")
		);

		setup.performInference();

		final Script derived = setup.scripts.get(1);
		final IType t = derived.typings().getFunctionTyping("TakeObject").returnType;
		Assert.assertEquals(derived, t);
	}

	@Test
	public void testReturnTypeOfInheritedFunctionRevisitedIndirect() {
		final DefinitionInfo[] definitions = new DefinitionInfo[] {
			new DefinitionInfo(
				StringUtil.join("\n",
					"// Base",
					"func Type() { return Clonk; }",
					"func MakeObject() {",
					"	return CreateObject(Type());",
					"}"
				),
				"Base"
			),
			new DefinitionInfo(
				StringUtil.join("\n",
					"#include Base",
					"",
					"func Type() { return Wipf; }",
					"",
					"func Usage() {",
					"	// MakeObject() typed",
					"	//as instance of type Wipf",
					"	MakeObject()->WipfMethod();",
					"}"
				),
				"Derived"
			),
			new DefinitionInfo("", "Clonk"),
			new DefinitionInfo("", "Wipf"),
			new DefinitionInfo(
				StringUtil.join("\n",
					"func Usage() {",
					"	return CreateObject(Derived)->MakeObject();",
					"}"
				),
				"User"
			)
		};

		final Setup setup = new Setup(definitions);
		final Script base = setup.scripts.get(0);
		final Script derived = setup.scripts.get(1);
		final Script clonk = setup.scripts.get(2);
		final Script wipf = setup.scripts.get(3);
		final Script user = setup.scripts.get(4);
		setup.performInference();

		Assert.assertEquals(clonk, base.typings().getFunctionTyping("MakeObject").returnType);
		Assert.assertEquals(new MetaDefinition((Definition) clonk), base.typings().getFunctionTyping("Type").returnType);
		Assert.assertEquals(wipf, derived.typings().getFunctionTyping("MakeObject").returnType);
		Assert.assertEquals(new MetaDefinition((Definition) wipf), derived.typings().getFunctionTyping("Type").returnType);
		Assert.assertEquals(wipf, user.typings().getFunctionTyping("Usage").returnType);
	}

	@Test
	public void testAbysseses() {
		System.out.println("Abyssesses! ---");
		final String[] funcs = new String[] {
			
			StringUtil.join("\n",
				"func MakeAbyssMarkers()",
				"{",
				"	RemoveAll(Find_ID(Abyss));",
				"	final var waypoints = FindObjects(Find_ID(Waypoint), Sort_Distance(0, 0));",
				"	WaypointsCheck(waypoints);",
				"	final var abyssObjects = CreateArray(GetLength(waypoints)/2);",
				"	for (var i = 0; i < GetLength(waypoints); i += 2)",
				"	{",
				"		final var num = i / 2;",
				"		final var first = waypoints[i];",
				"		final var second = waypoints[i + 1];",
				"		final var abyss = CreateObject(Abyss);",
				"		abyssObjects[num] = PrepareAbyss(first, second, abyss, num);",
				"	}",
				"	return abyssObjects;",
				"}"
			),
			
			StringUtil.join("\n",
				"func PrepareAbyss(first, second, abyss, num)",
				"{",
				"	abyss->SetLeft(first);",
				"	abyss->SetRight(second);",
				"	if (num == 0)",
				"		return PrepareAbyssOne(abyss);",
				"	else if (num == 1)",
				"		return PrepareAbyssTwo(abyss);",
				"}"
			),
			
			StringUtil.join("\n",
				"func PrepareAbyssOne(abyss)",
				"{",
				"	var tactic = CreateObject(RopebridgeRescue);",
				"	var objectCreator = CreateObject(ConfigurableObjectCreator);",
				"	objectCreator->SetType(Ropebridge_Post);",
				"	tactic->SetObjectCreator(objectCreator);",
				"	abyss->SetStrategy(tactic);",
				"	return abyss;",
				"}"
			),
			
			StringUtil.join("\n",
				"func PrepareAbyssTwo(abyss)",
				"{",
				"	var tactic = CreateObject(JumpRescue);",
				"	abyss->SetStrategy(tactic);",
				"	return abyss;",
				"}"
			)
		};

		final ICombinatoricsVector<String> vector = Factory.createVector(funcs);
		final Generator<String> gen = Factory.createPermutationGenerator(vector);
		final ArrayList<String> failedAssertions = new ArrayList<String>();

		int counter = 0;
		for (final ICombinatoricsVector<String> fns : gen) {
			final String rescuesBuilderSource = StringUtil.blockString("", "", "\n", fns);
			final DefinitionInfo[] definitions = new DefinitionInfo[] {
				new DefinitionInfo("", "Waypoint"),
				new DefinitionInfo("", "Abyss"),
				new DefinitionInfo(rescuesBuilderSource, "RescuesBuilder")
			};
			final Setup setup = new Setup(definitions);
			final Script abyss = setup.scripts.get(1);
			final Script rescuesBuilder = setup.scripts.get(2);

			final List<String> functionNames = rescuesBuilder.functions().stream().map(it -> it.name()).collect(Collectors.toList());
			final int makeAbyssMarkersIndex = functionNames.indexOf("MakeAbyssMarkers");
			final int prepareAbyssIndex = functionNames.indexOf("PrepareAbyss");
			final int prepareAbyssOneIndex = functionNames.indexOf("PrepareAbyssOne");
			final int prepareAbyssTwoIndex = functionNames.indexOf("PrepareAbyssTwo");
			final String fnOrderStr = StringUtil.blockString("", "", ", ", functionNames);

			System.out.println();
			System.out.println(fnOrderStr);
			setup.performInference();

			try {
				Assert.assertEquals(Maybe.make(abyss), rescuesBuilder.findFunction("PrepareAbyssOne").parameter(0).type());
				Assert.assertEquals(Maybe.make(abyss), rescuesBuilder.findFunction("PrepareAbyssTwo").parameter(0).type());
				Assert.assertEquals(new ArrayType(abyss), rescuesBuilder.findFunction("MakeAbyssMarkers").returnType());
			} catch (final AssertionError e) {
				if (prepareAbyssIndex > makeAbyssMarkersIndex) {
					System.out.println("--- PrepareAbyss after MakeAbyssMarkers");
				}
				if (prepareAbyssOneIndex < prepareAbyssIndex || prepareAbyssTwoIndex < prepareAbyssIndex) {
					System.out.println("--- PrepareAbyssOne or PrepareAbyssTwo before PrepareAbyss");
				}
				final String msg = String.format("%s: %s (%d)",
					fnOrderStr,
					e.getMessage(),
					counter
				);
				System.out.println(msg);
				failedAssertions.add(msg);
			}
			counter++;
		}

		Assert.assertEquals(StringUtil.blockString("", "", "\n", failedAssertions), 0, failedAssertions.size());
	}

	@Test
	public void testCollectionInheritance() {
		final DefinitionInfo[] definitions = new DefinitionInfo[] {
			new DefinitionInfo(
				StringUtil.join("\n",
					"// Base",
					"local items;",
					"func Type() { return nil; }",
					"func AddItem(item) { items[GetLength(items)] = item; }",
					"func AddNewItem() { AddItem(CreateObject(Type())); }",
					"func ToArray() { return items; }"
				),
				"Base"
			),
			new DefinitionInfo(
				StringUtil.join("\n",
					"#include Base",
					"func Type() { return Clonk; }"
				),
				"Derived"
			),
			new DefinitionInfo("", "Clonk"),
			new DefinitionInfo("", "Wipf")
		};

		final Setup setup = new Setup(definitions);
		setup.performInference();

		Assert.assertEquals(
			new ArrayType(setup.scripts.get(2)),
			setup.scripts.get(0).typings().getVariableType("items")
		);
		Assert.assertEquals(new ArrayType(setup.scripts.get(2)), setup.scripts.get(1).typings().getFunctionTyping("ToArray").returnType);
	}

	@Test
	public void testDabbleInferencePlan() {
		final DefinitionInfo[] definitions = new DefinitionInfo[] {
			new DefinitionInfo(
				StringUtil.join("\n",
					"func A(x) { B(123); }",
					"func B(x) { return A(321); }"
				),
				"Tangled"
			)
		};
		final Setup setup = new Setup(definitions);
		setup.performInference();
	}
}
