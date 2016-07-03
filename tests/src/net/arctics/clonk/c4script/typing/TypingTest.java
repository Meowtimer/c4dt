package net.arctics.clonk.c4script.typing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import net.arctics.clonk.TestBase;

import org.junit.Test;

public class TypingTest extends TestBase {

	@Test
	public void testCompatible() {
		final Typing t = Typing.INFERRED;
		final IType ty = new TypeChoice(
			new ArrayType(new ArrayType(new TypeChoice(PrimitiveType.INT, PrimitiveType.STRING))),
			PrimitiveType.STRING
		);
		assertEquals("string | array[array[string | int]]", ty.typeName(true));
		assertTrue(t.compatible(
			ty,
			new TypeChoice(PrimitiveType.ARRAY, PrimitiveType.STRING)
		));
	}

}
