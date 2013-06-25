package net.arctics.clonk.c4script

import static org.junit.Assert.*

import net.arctics.clonk.c4script.Function
import net.arctics.clonk.c4script.Function.PrintParametersOptions
import net.arctics.clonk.c4script.Variable
import net.arctics.clonk.c4script.typing.ArrayType
import net.arctics.clonk.c4script.typing.PrimitiveType

import org.eclipse.core.resources.IStorage
import org.junit.Assert
import org.junit.Test

public class FunctionTest {
	@Test
	public void testParameterPrinting() {
		Variable[] pars = [
			new Variable("a", PrimitiveType.STRING),
			new Variable("b", PrimitiveType.INT),
			new Variable("c", PrimitiveType.ANY),
			new Variable("d", new ArrayType(PrimitiveType.INT))
		]
		pars.each { it -> it.forceType(it.type(), true) }
		Function f = new Function("Test", PrimitiveType.INT, pars)
		def engineString = f.parameterString(new PrintParametersOptions(null, false, true, false))
		Assert.assertEquals("string a, int b, c, array d", engineString)
		def ideString = f.parameterString(new PrintParametersOptions(f.script(), false, false, false))
		Assert.assertEquals("string a, int b, any c, array[int] d", ideString)
	}
}
