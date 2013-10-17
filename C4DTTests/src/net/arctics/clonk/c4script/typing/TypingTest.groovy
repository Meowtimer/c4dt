package net.arctics.clonk.c4script.typing;

import static org.junit.Assert.*

import org.junit.Test

class TypingTest {

	@Test
	public void testCompatible() {
		Typing t = Typing.INFERRED;
		IType ty = new TypeChoice(
			new ArrayType(new ArrayType(new TypeChoice(PrimitiveType.INT, PrimitiveType.STRING))),
			PrimitiveType.STRING
		);
		assertEquals("array[array[int | string]] | string", ty.typeName(true));
		assertTrue(t.compatible(
			ty,
			new TypeChoice(PrimitiveType.ARRAY, PrimitiveType.STRING)
		));
	}

}
