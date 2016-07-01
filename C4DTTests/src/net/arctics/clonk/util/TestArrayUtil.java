package net.arctics.clonk.util;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static org.junit.Assert.assertArrayEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class TestArrayUtil {

	@Test
	public void testConcat() {
		final List<Integer[]> things = new ArrayList<>();
		things.add(new Integer[] { 1, 2, 3 });
		things.add(new Integer[] { 4, 5 });
		things.add(new Integer[] { 6 });
		final Integer[] result = concat(Integer.class, things);
		assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5, 6 }, result);
	}

}
