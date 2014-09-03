package net.arctics.clonk.c4script;

import static java.util.Arrays.stream;
import net.arctics.clonk.c4script.Function.PrintParametersOptions;
import net.arctics.clonk.c4script.typing.ArrayType;
import net.arctics.clonk.c4script.typing.PrimitiveType;

import org.junit.Assert;
import org.junit.Test;

public class FunctionTest {
	@Test
	public void testParameterPrinting() {
		final Variable[] pars = new Variable[] {
			new Variable("a", PrimitiveType.STRING),
			new Variable("b", PrimitiveType.INT),
			new Variable("c", PrimitiveType.ANY),
			new Variable("d", new ArrayType(PrimitiveType.INT))
		};
		stream(pars).forEach(it -> it.forceType(it.type(), true));
		final Function f = new Function("Test", PrimitiveType.INT, pars);
		final String engineString = f.parameterString(new PrintParametersOptions(null, false, true, false));
		Assert.assertEquals("string a, int b, c, array d", engineString);
		final String ideString = f.parameterString(new PrintParametersOptions(null, false, false, false));
		Assert.assertEquals("string a, int b, any c, array[int] d", ideString);
	}
}
