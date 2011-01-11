package net.arctics.clonk.parser.c4script.ast;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import net.arctics.clonk.ClonkCore;
import net.arctics.clonk.parser.ParserErrorCode;
import net.arctics.clonk.parser.ParsingException;
import net.arctics.clonk.parser.c4script.C4ScriptParser;
import net.arctics.clonk.parser.c4script.C4Type;
import net.arctics.clonk.parser.c4script.IType;

/**
 * Baseclass for statements.
 *
 */
public class Statement extends ExprElm implements Cloneable {
	

	private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

	public interface Attachment extends Serializable {
		public enum Position {
			Pre,
			Post
		}
		void applyAttachment(Attachment.Position position, ExprWriter builder, int depth);
	}
	
	public static class EmptyLinesAttachment implements Attachment {

		private static final long serialVersionUID = 1L;

		private int num;
		public int getNum() {
			return num;
		}
		public EmptyLinesAttachment(int num) {
			super();
			this.num = num;
		}
		@Override
		public void applyAttachment(Attachment.Position position, ExprWriter builder, int depth) {
			switch (position) {
			case Pre:
				for (int i = 0; i < num; i++) {
					//printIndent(builder, depth);
					builder.append("\n");
				}
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
	
	@SuppressWarnings("unchecked")
	public <T extends Attachment> T getAttachment(Class<T> cls) {
		if (attachments != null) {
			for (Attachment a : attachments) {
				if (cls.isAssignableFrom(a.getClass())) {
					return (T) a;
				}
			}
		}
		return null;
	}

	public Comment getInlineComment() {
		return getAttachment(Comment.class);
	}

	public void setInlineComment(Comment inlineComment) {
		Comment old = getInlineComment();
		if (old != null) {
			attachments.remove(old);
		}
		addAttachment(inlineComment);
	}

	@Override
	protected IType obtainType(C4ScriptParser context) {
		return C4Type.UNKNOWN;
	}

	@Override
	public boolean hasSideEffects() {
		return true;
	}
	
	@Override
	public void reportErrors(C4ScriptParser parser) throws ParsingException {
		super.reportErrors(parser);
		warnIfNoSideEffects(parser);
		if (!flagsEnabled(STATEMENT_REACHED))
			parser.warningWithCode(ParserErrorCode.NeverReached, this);
		//			for (ExprElm elm : getSubElements())
		//				if (elm != null)
		//					elm.reportErrors(parser);
	}

	public void printPrependix(ExprWriter builder, int depth) {
		if (attachments != null) {
			for (Attachment a : attachments) {
				a.applyAttachment(Attachment.Position.Pre, builder, depth);
			}
		}	
	}
	
	public void printAppendix(ExprWriter builder, int depth) {
		if (attachments != null) {
			for (Attachment a : attachments) {
				a.applyAttachment(Attachment.Position.Post, builder, depth);
			}
		}
	}
	
	public static final Statement NULL_STATEMENT = new Statement() {
		private static final long serialVersionUID = ClonkCore.SERIAL_VERSION_UID;

		@Override
		public void doPrint(ExprWriter output, int depth) {
			// blub
		};
	};

}