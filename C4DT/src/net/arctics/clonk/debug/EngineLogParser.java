package net.arctics.clonk.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.arctics.clonk.util.Sink;

public class EngineLogParser {
	
	private static final Pattern ENGINE_ERROR_START_PATTERN = Pattern.compile("\\[.*?\\]\\w*ERROR\\:(.*)");
	private static final Pattern STACK_TRACE_ENTRY_PATTERN = Pattern.compile("by\\:\\w*(.*)?\\((.*)?\\)\\w*\\((.*)?\\)\\w*\\((.*)?\\:(.*)?\\)");
	
	public class EngineErrorFileLocation {
		private String file;
		private int line;
		public EngineErrorFileLocation(String file, int line) {
			super();
			this.file = file;
			this.line = line;
		}
		public String getFile() {
			return file;
		}
		public int getLine() {
			return line;
		}
	}
	
	public class EngineError {
		private List<EngineErrorFileLocation> stackTrace;
		private String message;
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
	
	private BufferedReader logReader;
	
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
						String fnName = stackTraceEntryMatcher.group(1);
						String objInfo = stackTraceEntryMatcher.group(2);
						String objInfo2 = stackTraceEntryMatcher.group(3);
						String fileLocation = stackTraceEntryMatcher.group(4);
						int lineNumber = Integer.valueOf(stackTraceEntryMatcher.group(5));
						stackTrace.add(new EngineErrorFileLocation(fileLocation, lineNumber));
						
					} else {
						readLine = readLine2;
						break;
					}
				}
				sink.receivedObject(new EngineError(stackTrace, msg));
			}
			if (readLine == null)
				readLine = logReader.readLine();
		} while (mainLoopPending);
	}
}
