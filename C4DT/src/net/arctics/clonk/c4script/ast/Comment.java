package net.arctics.clonk.c4script.ast;

import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.Core;
import net.arctics.clonk.ProblemException;
import net.arctics.clonk.ast.ASTNode;
import net.arctics.clonk.ast.ASTNodePrinter;
import net.arctics.clonk.ast.EntityRegion;
import net.arctics.clonk.ast.ExpressionLocator;
import net.arctics.clonk.ast.IEntityLocator;
import net.arctics.clonk.ast.IPlaceholderPatternMatchTarget;
import net.arctics.clonk.c4script.ScriptParser;
import net.arctics.clonk.c4script.Conf;
import net.arctics.clonk.c4script.Function;
import net.arctics.clonk.c4script.Script;
import net.arctics.clonk.c4script.Variable;
import net.arctics.clonk.parser.BufferedScanner;
import net.arctics.clonk.util.StringUtil;

/**
 * A comment in the code. Instances of this class can be used as regular {@link ASTNode} objects or
 * be attached to {@link Statement}s as {@link Attachment}.
 * @author madeen
 *
 */
public class Comment extends Statement implements Statement.Attachment, IPlaceholderPatternMatchTarget {

	private static final long serialVersionUID = Core.SERIAL_VERSION_UID;
	private String comment;
	private boolean multiLine;
	private boolean prependix;
	private final boolean javaDoc;
	private int absoluteOffset;

	/**
	 * Used for linked list of comments interpreted by parser as one documentation comment
	 */
	public transient Comment previousComment;

	/**
	 * Create new {@link Comment}, specifying content and whether it's a multi-line comment.
	 * @param comment Content
	 * @param multiLine Whether multiline
	 * @param javadoc The comment is a javadoc-comment
	 */
	public Comment(String comment, boolean multiLine, boolean javadoc) {
		super();
		this.comment = comment;
		this.multiLine = multiLine;
		this.javaDoc = javadoc;
	}

	/**
	 * Return the text of the comment or comment chain.
	 * @return Return the comment string. If {@link #previousComment} is set, the returned string will be the text of all comments in the linked list concatenated.
	 */
	public String text() {
		if (previousComment == null)
			return comment;
		else {
			int cap = 0;
			final String lineBreak = "<br/>";
			for (Comment c = this; c != null; c = c.previousComment)
				cap += c.comment.length() + (c != this ? lineBreak.length() : 0);
			final StringBuilder builder = new StringBuilder(cap);
			for (Comment c = this; c != null; c = c.previousComment) {
				if (c != this)
					builder.insert(0, lineBreak);
				builder.insert(0, c.comment);
			}
			return builder.toString();
		}
	}

	/**
	 * Set the comment string.
	 * @param comment The comment string
	 */
	public void setComment(String comment) {
		this.comment = comment;
	}

	@Override
	public void doPrint(ASTNodePrinter builder, int depth) {
		final String c = comment;
		if (multiLine = multiLine || c.contains("\n")) {
			builder.append("/*"); //$NON-NLS-1$
			if (javaDoc)
				builder.append('*');
			builder.append(c);
			builder.append("*/"); //$NON-NLS-1$
		}
		else {
			builder.append("//"); //$NON-NLS-1$
			if (javaDoc)
				builder.append('/');
			builder.append(c);
		}
	}

	/**
	 * Return whether this comment is multiline.
	 * @return True if the source code representation of this node is \/* ... *\/
	 */
	public boolean isMultiLine() { return multiLine; }

	/**
	 * Set multiline-ness of this {@link Comment}.
	 * @param multiLine Whether multiline
	 */
	public void setMultiLine(boolean multiLine) { this.multiLine = multiLine; }

	/**
	 * Whether comment is a javadoc comment
	 * @return Yes
	 */
	public boolean isJavaDoc() {
		return javaDoc;
	}

	/**
	 * Return whether this {@link Comment} is the last text element preceding the specified offset.
	 * This is true only if at most one line-break is between the {@link Comment} text and the offset.
	 * @param offset The offset
	 * @param script Script text queried for content to determine the return value.
	 * @return Whether this comment precedes the offset as described.
	 */
	public boolean precedesOffset(int offset, char[] script) {
		int count = 0;
		if (offset > absoluteOffset+getLength()) {
			for (int i = absoluteOffset+getLength(); i < offset; i++) {
				if (!BufferedScanner.isLineDelimiterChar(script[i]))
					return false;
				if (script[i] == '\n' && ++count > 1)
					return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public EntityRegion entityAt(int offset, IEntityLocator l) {
		// parse comment as expression and see what goes
		try {
			final Script script = parentOfType(Script.class);
			if (script == null)
				return null;
			final ScriptParser commentParser = new ScriptParser(comment, script, script.scriptFile()) {
				@Override
				protected void initialize() {
					super.initialize();
					markers().enabled(false);
				}
				@Override
				public int sectionOffset() { return 0; }
			};
			final ExpressionLocator<Comment> locator = new ExpressionLocator<Comment>(offset-2-this.sectionOffset()); // make up for '//' or /*'
			commentParser.parseStandaloneStatement(comment, parentOfType(Function.class)).traverse(locator, this);
			if (locator.expressionAtRegion() != null) {
				final EntityRegion reg = locator.expressionAtRegion().entityAt(offset, locator);
				if (reg != null)
					return reg.incrementRegionBy(start()+2);
				else
					return null;
			}
		} catch (final ProblemException e) {}
		return super.entityAt(offset, l);
	}

	@Override
	public void applyAttachment(Position position, ASTNodePrinter builder, int depth) {
		switch (position) {
		case Pre:
			if (prependix) {
				this.print(builder, depth);
				builder.append("\n");
				Conf.printIndent(builder, depth);
			}
			break;
		case Post:
			if (!prependix) {
				builder.append(" ");
				this.print(builder, depth);
			}
			break;
		}
	}

	/**
	 * Set the absolute offset of this comment. This value is only is used in {@link #precedesOffset(int, CharSequence)}
	 * @param offset The offset to set as absolute one.
	 */
	public void setAbsoluteOffset(int offset) {
		absoluteOffset = offset;
	}

	/**
	 * Return whether the comment - as an {@link Attachment} to a {@link Statement} - is to be printed before or after the statement
	 * @return What he said
	 */
	public boolean isPrependix() {
		return prependix;
	}

	/**
	 * Sets whether the comment - as an {@link Attachment} to a {@link Statement} - is to be printed before the {@link Statement} or following it
	 * @param Whether to or not
	 */
	public void setPrependix(boolean prependix) {
		this.prependix = prependix;
	}

	private static final Pattern PARAMDESCPATTERN = Pattern.compile("\\s*\\*?\\s*@param\\s*([^\\s:]*):? (.*)$");
	private static final Pattern RETURNDESCPATTERN = Pattern.compile("\\s*?\\*?\\s*@return\\s*:? (.*)$");
	private static final Pattern TAGPATTERN = Pattern.compile("\\\\([cb]) ([^\\s]*)");
	private static final Pattern JAVADOCLINESTART = Pattern.compile("\\s*\\*");

	/**
	 * Apply documentation in this comment to the given {@link Function}.
	 * \@param tags in javadoc comments are taken into account and applied to individual parameters of the function.
	 * @param function The function to apply the documentation to
	 */
	public void applyDocumentation(Function function) {
		if (isJavaDoc()) {
			final String text = this.text().trim();
			final StringReader reader = new StringReader(text);
			final Matcher parmDescMatcher = PARAMDESCPATTERN.matcher("");
			final Matcher returnDescMatcher = RETURNDESCPATTERN.matcher("");
			final Matcher lineStartMatcher = JAVADOCLINESTART.matcher("");
			final StringBuilder builder = new StringBuilder(text.length());
			for (String line : StringUtil.lines(reader)) {
				line = processTags(line, lineStartMatcher);
				if (parmDescMatcher.reset(line).matches()) {
					final String parmName = parmDescMatcher.group(1);
					final String parmDesc = parmDescMatcher.group(2);
					final Variable parm = function.findParameter(parmName);
					if (parm != null)
						parm.setUserDescription(parmDesc);
				} else if (returnDescMatcher.reset(line).matches())
					function.setReturnDescription(returnDescMatcher.group(1));
				else {
					builder.append(line);
					builder.append("\n");
				}
			}
			function.setUserDescription(builder.toString());
		} else
			function.setUserDescription(this.text().trim());
	}

	private static String processTags(String line, Matcher lineStartMatcher) {
		final Matcher matcher = TAGPATTERN.matcher(line);
		final StringBuilder builder = new StringBuilder(line);
		int shift = 0;
		if (lineStartMatcher.reset(line).lookingAt()) {
			builder.delete(0, lineStartMatcher.end());
			shift -= lineStartMatcher.end();
		}
		while (matcher.find()) {
			final String replacement = "<b>"+matcher.group(2)+"</b>";
			builder.replace(matcher.start()+shift, matcher.end()+shift, replacement);
			shift += replacement.length()-matcher.end()+matcher.start();
		}
		return builder.toString();
	}

	@Override
	public String patternMatchingText() { return text(); }

}