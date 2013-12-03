package net.arctics.clonk.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.util.Sink;

import org.eclipse.ui.console.IPatternMatchListener;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public class EngineLogParser implements IPatternMatchListener {

	private static final Pattern ENGINE_ERROR_START_PATTERN = Pattern.compile("\\[.*?\\]\\w*ERROR\\:(.*)");
	private static final Pattern STACK_TRACE_ENTRY_PATTERN = Pattern.compile("by\\:\\w*(.*)?\\((.*)?\\)\\w*\\((.*)?\\)\\w*\\((.*)?\\:(.*)?\\)");

	public class EngineErrorFileLocation {
		private final String file;
		private final int line;
		public EngineErrorFileLocation(final String file, final int line) {
			super();
			this.file = file;
			this.line = line;
		}
		public String file() {
			return file;
		}
		public int line() {
			return line;
		}
	}

	public class EngineError {
		private final List<EngineErrorFileLocation> stackTrace;
		private final String message;
		public EngineError(final List<EngineErrorFileLocation> stackTrace,
				final String message) {
			super();
			this.stackTrace = stackTrace;
			this.message = message;
		}
		public List<EngineErrorFileLocation> getStackTrace() {
			return stackTrace;
		}
		public String getMessage() {
			return message;
		}
	}

	private final BufferedReader logReader;
	public EngineLogParser(final Reader reader) { logReader = new BufferedReader(reader); }

	public void parse(final Sink<EngineError> sink) throws IOException {
		final Matcher errorStartMatcher = ENGINE_ERROR_START_PATTERN.matcher("");
		final Matcher stackTraceEntryMatcher = STACK_TRACE_ENTRY_PATTERN.matcher("");
		final boolean mainLoopPending = true;
		String readLine = logReader.readLine();
		while (readLine != null) {
			errorStartMatcher.reset(readLine);
			readLine = null;
			if (errorStartMatcher.matches()) {
				final String msg = errorStartMatcher.group(1);
				String readLine2;
				final List<EngineErrorFileLocation> stackTrace = new LinkedList<EngineErrorFileLocation>();
				while ((readLine2 = logReader.readLine()) != null) {
					stackTraceEntryMatcher.reset(readLine2);
					if (stackTraceEntryMatcher.matches()) {
						/*String fnName = stackTraceEntryMatcher.group(1);
						String objInfo = stackTraceEntryMatcher.group(2);
						String objInfo2 = stackTraceEntryMatcher.group(3);*/
						final String fileLocation = stackTraceEntryMatcher.group(4);
						final int lineNumber = Integer.valueOf(stackTraceEntryMatcher.group(5));
						stackTrace.add(new EngineErrorFileLocation(fileLocation, lineNumber));
					} else {
						readLine = readLine2;
						break;
					}
				}
				sink.elutriate(new EngineError(stackTrace, msg));
			}
			if (readLine == null)
				readLine = logReader.readLine();
		} while (mainLoopPending);
	}

	@Override
	public void connect(final TextConsole console) {}
	@Override
	public void disconnect() {}
	@Override
	public void matchFound(final PatternMatchEvent event) {}
	@Override
	public String getPattern() { return null; }
	@Override
	public int getCompilerFlags() { return 0; }
	@Override
	public String getLineQualifier() { return null;}
}
