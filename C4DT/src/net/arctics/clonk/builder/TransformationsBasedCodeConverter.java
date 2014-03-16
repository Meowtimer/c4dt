package net.arctics.clonk.builder;

import static net.arctics.clonk.util.Utilities.as;

import java.util.Map;

import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.Declaration;
import net.arctics.clonk.ast.ITransformer;
import net.arctics.clonk.c4script.ast.AccessVar;
import net.arctics.clonk.c4script.ast.IDLiteral;
import net.arctics.clonk.c4script.ast.Tidy;
import net.arctics.clonk.index.ID;
import net.arctics.clonk.index.ProjectConversionConfiguration;
import net.arctics.clonk.index.ProjectConversionConfiguration.CodeTransformation;

public final class TransformationsBasedCodeConverter extends CodeConverter {
	private final ProjectConversionConfiguration configuration;
	public TransformationsBasedCodeConverter(ProjectConversionConfiguration configuration) {
		this.configuration = configuration;
	}
	@Override
	public ASTNode performConversion(final ASTNode expression, final Declaration declaration, final ICodeConverterContext context) {
		if (configuration == null)
			return expression;
		final ITransformer transformer = new ITransformer() {
			@Override
			public Object transform(final ASTNode prev, final Object prevT, ASTNode expression) {
				if (expression == null)
					return null;
				if (expression instanceof IDLiteral) {
					final IDLiteral lit = (IDLiteral) expression;
					final ID mapped = configuration.idMap().get(lit.literal());
					if (mapped != null)
						return new IDLiteral(mapped);
				}
				else if (expression instanceof AccessVar && (((AccessVar)expression).proxiedDefinition()) != null) {
					final ID mapped = configuration.idMap().get(ID.get(expression.toString()));
					if (mapped != null)
						return new AccessVar(mapped.stringValue());
				}
				for (final ProjectConversionConfiguration.CodeTransformation ct : configuration.transformations()) {
					boolean success = false;
					for (CodeTransformation c = ct; c != null; c = c.chain()) {
						final Map<String, Object> matched = c.template().match(expression);
						if (matched != null) {
							expression = c.transformation().transform(matched, context);
							success = true;
						}
					}
					if (success)
						return expression;
				}
				return expression.transformSubElements(this);
			}
		};
		ASTNode node = as(transformer.transform(null, null, expression), ASTNode.class);
		if (node != null)
			try {
				node = new Tidy(declaration.topLevelStructure(), 2).tidyExhaustive(node);
			} catch (final CloneNotSupportedException e) {}
		return node;
	}
}