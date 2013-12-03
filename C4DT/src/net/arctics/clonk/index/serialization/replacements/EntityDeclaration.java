package net.arctics.clonk.index.serialization.replacements;

import static java.lang.String.format;
import static net.arctics.clonk.Flags.DEBUG;
import static net.arctics.clonk.util.ArrayUtil.iterable;
import static net.arctics.clonk.util.Utilities.defaulting;

import java.io.Serializable;
import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.c4script.typing.IType;
import net.arctics.clonk.c4script.typing.PrimitiveType;
import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;

/**
 * Placeholder for declaration in another entity used for serialization.
 * @author madeen
 *
 */
public class EntityDeclaration implements Serializable, IDeserializationResolvable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final IndexEntity containingEntity;
	private final String declarationPath;
	private final Class<? extends Declaration> declarationClass;
	private transient IndexEntity deserializee;

	/**
	 * Create placeholder
	 * @param declaration Declaration to create the placeholder for
	 * @param containingEntity Entity containing the declaration
	 */
	public EntityDeclaration(final Declaration declaration, final IndexEntity containingEntity) {
		this.containingEntity = containingEntity;
		this.declarationPath = declaration.pathRelativeToIndexEntity();
		this.declarationClass = declaration.getClass();
	}

	@Override
	public String toString() {
		return format("%s::%s: %s",
			containingEntity != null ? containingEntity.toString() : "<Unknown>",
			declarationPath,
			declarationClass != null ? declarationClass.getSimpleName() : "<Unknown>"
		);
	}

	/**
	 * Attempt to resolve the original declaration the placeholder was created for.
	 * If that does not work right away create a {@link IDeferredDeclaration} compatible to
	 * the class of the original declaration which later needs to be truly resolved by
	 * calling {@link IDeferredDeclaration#resolve()}
	 */
	@Override
	public Declaration resolve(final Index index, final IndexEntity deserializee) {
		Declaration result;
		this.deserializee = deserializee;
		if (containingEntity != null) {
			containingEntity.requireLoaded();
			result = containingEntity.findDeclarationByPath(declarationPath, declarationClass);
		} else
			result = null;
		if (result == null)
			if (containingEntity != null)
				return makeDeferred();
			else if (DEBUG)
				System.out.println(format("Giving up on resolving '%s::%s'",
					containingEntity != null ? containingEntity.qualifiedName() : "<null>",
					declarationPath
				));
		return result;
	}

	private Declaration makeDeferred() {
		if (IType.class.isAssignableFrom(declarationClass))
			return new DeferredType();
		else if (Variable.class.isAssignableFrom(declarationClass))
			return new DeferredVariable();
		else if (Function.class.isAssignableFrom(declarationClass))
			return new DeferredFunction();
		else
			return null;
	}

	private Object resolveDeferred() {
		final Declaration d = containingEntity.findDeclarationByPath(declarationPath, declarationClass);
		if (d == null && DEBUG)
			System.out.println(format("%s: Failed to resolve %s::%s (%s)",
				deserializee != null ? deserializee.qualifiedName() : "<null>",
				containingEntity.qualifiedName(),
				declarationPath,
				declarationClass.getSimpleName()
			));
		return d;
	}

	private String deferredDescription() { return format("Deferred: %s", toString()); }

	@SuppressWarnings("serial")
	private class DeferredVariable extends Variable implements IDeferredDeclaration {
		@Override
		public Object resolve() { return resolveDeferred(); }
		@Override
		public String toString() { return deferredDescription(); }
		@Override
		public Object saveReplacement(final Index context) { return EntityDeclaration.this; }
	}

	@SuppressWarnings("serial")
	private class DeferredFunction extends Function implements IDeferredDeclaration {
		@Override
		public Object resolve() { return resolveDeferred(); }
		@Override
		public String toString() { return deferredDescription(); }
		@Override
		public Object saveReplacement(final Index context) { return EntityDeclaration.this; }
	}

	@SuppressWarnings("serial")
	private class DeferredType extends Declaration implements IType, IDeferredDeclaration {
		@Override
		public Object resolve() { return defaulting(resolveDeferred(), PrimitiveType.ANY); }
		@Override
		public Iterator<IType> iterator() { return iterable((IType)PrimitiveType.ANY).iterator(); }
		@Override
		public String typeName(final boolean special) { return PrimitiveType.ANY.typeName(special); }
		@Override
		public IType simpleType() { return PrimitiveType.ANY; }
		@Override
		public String toString() { return deferredDescription(); }
		@Override
		public Object saveReplacement(final Index context) { return EntityDeclaration.this; }
	}
}