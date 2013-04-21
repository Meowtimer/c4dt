package net.arctics.clonk.parser.c4script;

import static org.junit.Assert.*;

import net.arctics.clonk.c4script.ArrayType;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.PrimitiveType;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.parser.c4script.Function.ParameterStringOption;

import org.junit.Assert;
import org.junit.Test;

public class FunctionTest {
	@Test
	public void testParameterPrinting() {
		Function f = new Function("Test", PrimitiveType.INT,
			new Variable("a", PrimitiveType.STRING),
			new Variable("b", PrimitiveType.INT),
			new Variable("c", PrimitiveType.ANY),
			new Variable("d", new ArrayType(PrimitiveType.INT)),
		);
		def engineString = f.parameterString(EnumSet.of(ParameterStringOption.EngineCompatible))
		Assert.assertEquals("string a, int b, c, array d", engineString)
		def ideString = f.parameterString(EnumSet.noneOf(ParameterStringOption.class))
		Assert.assertEquals("string a, int b, any c, array[int] d", ideString)
	}
}
