package net.arctics.clonk.c4script.ast;

import static net.arctics.clonk.util.ArrayUtil.concat;
import static net.arctics.clonk.util.ArrayUtil.filter;

import java.io.Serializable;
import java.util.ArrayList;
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

	public interface Attachment extends Serializable, Cloneable {
		public enum Position {
			Pre,
			Post
		}
		void applyAttachment(Attachment.Position position, ASTNodePrinter builder, int depth);
		Attachment clone() throws CloneNotSupportedException;
	}

	public static class EmptyLinesAttachment implements Attachment {

		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;

		private final int num;
		public int num() {
			return num;
		}
		public EmptyLinesAttachment(final int num) {
			super();
			this.num = num;
		}
		@Override
		public void applyAttachment(final Attachment.Position position, final ASTNodePrinter builder, final int depth) {
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
		@Override
		public EmptyLinesAttachment clone() throws CloneNotSupportedException { return (EmptyLinesAttachment) super.clone(); }
	}

	private List<Attachment> attachments;

	public void addAttachment(final Attachment attachment) {
		if (attachments == null)
			attachments = new LinkedList<Attachment>();
		attachments.add(attachment);
		if (attachment instanceof ASTNode)
			((ASTNode) attachment).setParent(this);
	}

	public void addAttachments(final Collection<? extends Attachment> attachmentsToAdd) {
		for (final Attachment a : attachmentsToAdd)
			addAttachment(a);
	}

	public List<Attachment> attachments() { return attachments; }

	@SuppressWarnings("unchecked")
	public <T extends Attachment> T attachmentOfType(final Class<T> cls) {
		if (attachments != null)
			for (final Attachment a : attachments)
				if (cls.isAssignableFrom(a.getClass()))
					return (T) a;
		return null;
	}

	public Comment inlineComment() {
		return attachmentOfType(Comment.class);
	}

	public void setInlineComment(final Comment inlineComment) {
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
	public void printPrefix(final ASTNodePrinter builder, final int depth) {
		if (attachments != null)
			for (final Attachment a : attachments)
				a.applyAttachment(Attachment.Position.Pre, builder, depth);
	}

	@Override
	public void printSuffix(final ASTNodePrinter builder, final int depth) {
		if (attachments != null)
			for (final Attachment a : attachments)
				a.applyAttachment(Attachment.Position.Post, builder, depth);
	}

	public static final Statement NULL_STATEMENT = new Statement() {
		private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
		@Override
		public void doPrint(final ASTNodePrinter output, final int depth) {
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
	
	@Override
	public ASTNode clone() {
		final Statement clone = (Statement)super.clone();
		if (this.attachments != null) {
			clone.attachments = new ArrayList<Attachment>(this.attachments);
			for (int i = 0; i < clone.attachments.size(); i++)
				try {
					final Attachment a = clone.attachments.get(i).clone();
					if (a instanceof ASTNode)
						((ASTNode)a).setParent(this);
					clone.attachments.set(i, a);
				} catch (final CloneNotSupportedException e) {
					clone.attachments.remove(i);
					i--;
				}
		}
		return clone;
	}

}