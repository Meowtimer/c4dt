package net.arctics.clonk.mapcreator;

import static net.arctics.clonk.mapcreator.GlobalFunctions.BoundBy;
import static net.arctics.clonk.mapcreator.GlobalFunctions.Random;
import net.arctics.clonk.ini.CategoriesValue;

public class ScenarioValue {
	public int Std, Rnd, Min, Max;
	public int Evaluate() {
		return BoundBy(Std+Random(2*Rnd+1)-Rnd,Min,Max);
	}
	public ScenarioValue(final int std, final int rnd) {
		this(std, rnd, 0, 100);
	}
	public ScenarioValue(final int std) {
		this(std, 0);
	}
	public ScenarioValue(final int std, final int rnd, final int min, final int max) {
		super();
		Std = std;
		Rnd = rnd;
		Min = min;
		Max = max;
	}
	public ScenarioValue() {
		this(0);
	}
	public ScenarioValue(final CategoriesValue[] array) {
		Std = val(array, 0, 0);
		Rnd = val(array, 1, 0);
		Min = val(array, 2, 0);
		Max = val(array, 3, 100);
	}
	private static int val(final CategoriesValue[] array, final int ndx, final int defaultV) {
		return ndx < array.length ? array[ndx].summedValue() : defaultV;
	}
}
