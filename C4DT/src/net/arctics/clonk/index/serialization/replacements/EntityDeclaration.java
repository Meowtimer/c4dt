package net.arctics.clonk.index.serialization.replacements;

import static net.arctics.clonk.util.ArrayUtil.iterable;

import java.io.Serializable;
import java.util.Iterator;

import net.arctics.clonk.Core;
import net.arctics.clonk.index.IDeserializationResolvable;
import net.arctics.clonk.index.Index;
import net.arctics.clonk.index.IndexEntity;
import net.arctics.clonk.parser.Declaration;
import net.arctics.clonk.parser.c4script.Function;
import net.arctics.clonk.parser.c4script.IType;
import net.arctics.clonk.parser.c4script.PrimitiveType;
import net.arctics.clonk.parser.c4script.Variable;

public class EntityDeclaration implements Serializable, IDeserializationResolvable {
	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private final IndexEntity containingEntity;
	private final String declarationPath;
	private final Class<? extends Declaration> declarationClass;
	private transient IndexEntity deserializee;
	public EntityDeclaration(Declaration declaration, IndexEntity containingEntity) {
		this.containingEntity = containingEntity;
		this.declarationPath = declaration.pathRelativeToIndexEntity();
		this.declarationClass = declaration.getClass();
	}
	@Override
	public Declaration resolve(Index index, IndexEntity deserializee) {
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
			else
				System.out.println(String.format("Giving up on resolving '%s::%s'",
					containingEntity != null ? containingEntity.qualifiedName() : "<null>",
					declarationPath
				));
		return result;
	}
	
	Declaration makeDeferred() {
		if (IType.class.isAssignableFrom(declarationClass))
			return new DeferredType();
		else if (Variable.class.isAssignableFrom(declarationClass))
			return new DeferredVariable();
		else if (Function.class.isAssignableFrom(declarationClass))
			return new DeferredFunction();
		else
			return null;
	}
	
	Declaration resolveDeferred() {
		final Declaration d = containingEntity.findDeclarationByPath(declarationPath, declarationClass);
		if (d == null)
			System.out.println(String.format("%s: Failed to resolve %s::%s",
				deserializee != null ? deserializee.qualifiedName() : "<null>",
				containingEntity.qualifiedName(),
				declarationPath
			));
		return d;
	}
	
	@SuppressWarnings("serial")
	class DeferredVariable extends Variable implements IDeferredDeclaration {
		@Override
		public Declaration resolve() { return resolveDeferred(); }
	}
	
	@SuppressWarnings("serial")
	class DeferredFunction extends Function implements IDeferredDeclaration {
		@Override
		public Declaration resolve() { return resolveDeferred(); }
	}

	@SuppressWarnings("serial")
	class DeferredType extends Declaration implements IType, IDeferredDeclaration {
		@Override
		public Declaration resolve() { return resolveDeferred(); }
		@Override
		public Iterator<IType> iterator() { return iterable((IType)PrimitiveType.ANY).iterator(); }
		@Override
		public String typeName(boolean special) { return PrimitiveType.ANY.typeName(special); }
		@Override
		public IType simpleType() { return PrimitiveType.ANY; }
	}
}