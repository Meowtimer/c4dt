package net.arctics.clonk.parser.c4script;

import java.util.HashMap;
import java.util.Map;

import net.arctics.clonk.util.Pair;
import net.arctics.clonk.util.Utilities;

public class TypeRelationshipAssessor {
	private static Map<Pair<Class<? extends IType>, Class<? extends IType>>, TypeRelationshipAssessor> registry = new HashMap<Pair<Class<? extends IType>, Class<? extends IType>>, TypeRelationshipAssessor>();
	private static TypeRelationshipAssessor defaultAssessor = new TypeRelationshipAssessor();
	public static void register(TypeRelationshipAssessor assessor, Class<? extends IType> a, Class<? extends IType> b, boolean dual) {
		registry.put(new Pair<Class<? extends IType>, Class<? extends IType>>(a, b), assessor);
		if (dual)
			registry.put(new Pair<Class<? extends IType>, Class<? extends IType>>(b, a), assessor);
	}
	@SuppressWarnings("unchecked")
	public static TypeRelationshipAssessor get(IType a, IType b) {
		TypeRelationshipAssessor assessor;
		Pair<Class<? extends IType>, Class<? extends IType>> key = new Pair<Class<? extends IType>, Class<? extends IType>>(a.getClass(), b.getClass());
		for (Class<?> x = a.getClass(); x != null && IType.class.isAssignableFrom(x); x = x.getSuperclass()) {
			key.setFirst((Class<? extends IType>) x);
			assessor = registry.get(key);
			if (assessor != null)
				return assessor;
		}
		key.setFirst(a.getClass());
		for (Class<?> x = b.getClass(); x != null && IType.class.isAssignableFrom(x); x = x.getSuperclass()) {
			key.setSecond((Class<? extends IType>) x);
			assessor = registry.get(key);
			if (assessor != null)
				return assessor;
		}
		return defaultAssessor;
	}
	public boolean typesAreEqual(IType a, IType b) {
		return Utilities.objectsEqual(a, b);
	}
	public static boolean typesEqual(IType a, IType b) {
		return get(a, b).typesAreEqual(a, b);
	}
}
