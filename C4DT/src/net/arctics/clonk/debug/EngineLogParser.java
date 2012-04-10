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
		public EngineErrorFileLocation(String file, int line) {
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
		public EngineError(List<EngineErrorFileLocation> stackTrace,
				String message) {
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
	
	public EngineLogParser(Reader reader) {
		logReader = new BufferedReader(reader);
	}
	
	public void parse(Sink<EngineError> sink) throws IOException {
		Matcher errorStartMatcher = ENGINE_ERROR_START_PATTERN.matcher("");
		Matcher stackTraceEntryMatcher = STACK_TRACE_ENTRY_PATTERN.matcher("");
		boolean mainLoopPending = true;
		String readLine = logReader.readLine();
		while (readLine != null) {
			errorStartMatcher.reset(readLine);
			readLine = null;
			if (errorStartMatcher.matches()) {
				String msg = errorStartMatcher.group(1);
				String readLine2;
				List<EngineErrorFileLocation> stackTrace = new LinkedList<EngineErrorFileLocation>();
				while ((readLine2 = logReader.readLine()) != null) {
					stackTraceEntryMatcher.reset(readLine2);
					if (stackTraceEntryMatcher.matches()) {
						/*String fnName = stackTraceEntryMatcher.group(1);
						String objInfo = stackTraceEntryMatcher.group(2);
						String objInfo2 = stackTraceEntryMatcher.group(3);*/
						String fileLocation = stackTraceEntryMatcher.group(4);
						int lineNumber = Integer.valueOf(stackTraceEntryMatcher.group(5));
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
	public void connect(TextConsole console) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disconnect() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void matchFound(PatternMatchEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getPattern() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCompilerFlags() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getLineQualifier() {
		// TODO Auto-generated method stub
		return null;
	}
}
