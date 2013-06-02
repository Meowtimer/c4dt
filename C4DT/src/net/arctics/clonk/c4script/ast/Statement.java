package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.ArrayUtil.filter;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.Core;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.c4script.Conf;

/**
 * Baseclass for statements.
 *
 */
public class Statement extends ASTNode implements Cloneable {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

	public interface Attachment extends Serializable {
		public enum Position {
			Pre,
			Post
		}
		void applyAttachment(Attachment.Position position, ASTNodePrinter builder, int depth);
	}

	public static class EmptyLinesAttachment implements Attachment {

		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

		private final int num;
		public int num() {
			return num;
		}
		public EmptyLinesAttachment(int num) {
			super();
			this.num = num;
		}
		@Override
		public void applyAttachment(Attachment.Position position, ASTNodePrinter builder, int depth) {
			switch (position) {
			case Pre:
				for (int i = 0; i < num; i++)
					//printIndent(builder, depth);
					builder.append("\n");
				Conf.printIndent(builder, depth);
				break;
			default:
				break;
			}
		}
	}

	private List<Attachment> attachments;

	public void addAttachment(Attachment attachment) {
		if (attachments == null)
			attachments = new LinkedList<Attachment>();
		attachments.add(attachment);
	}

	public void addAttachments(Collection<? extends Attachment> attachmentsToAdd) {
		if (attachments == null)
			attachments = new LinkedList<Attachment>();
		attachments.addAll(attachmentsToAdd);
	}

	public List<Attachment> attachments() { return attachments; }

	@SuppressWarnings("unchecked")
	public <T extends Attachment> T attachmentOfType(Class<T> cls) {
		if (attachments != null)
			for (final Attachment a : attachments)
				if (cls.isAssignableFrom(a.getClass()))
					return (T) a;
		return null;
	}

	public Comment inlineComment() {
		return attachmentOfType(Comment.class);
	}

	public void setInlineComment(Comment inlineComment) {
		final Comment old = inlineComment();
		if (old != null)
			attachments.remove(old);
		addAttachment(inlineComment);
	}

	@Override
	public boolean hasSideEffects() {
		return true;
	}

	@Override
	public void printPrefix(ASTNodePrinter builder, int depth) {
		if (attachments != null)
			for (final Attachment a : attachments)
				a.applyAttachment(Attachment.Position.Pre, builder, depth);
	}

	@Override
	public void printSuffix(ASTNodePrinter builder, int depth) {
		if (attachments != null)
			for (final Attachment a : attachments)
				a.applyAttachment(Attachment.Position.Post, builder, depth);
	}

	public static final Statement NULL_STATEMENT = new Statement() {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		@Override
		public void doPrint(ASTNodePrinter output, int depth) {
			// blub
		}
	};

	@Override
	protected ASTNode[] traversalSubElements() {
		if (attachments != null)
			return concat(super.traversalSubElements(), filter(attachments.toArray(), ASTNode.class));
		else
			return super.traversalSubElements();
	}

}