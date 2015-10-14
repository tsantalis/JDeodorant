package ca.concordia.jdeodorant.clone.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class CloneInstanceLocationInfo {
	
	private static final String LINE_FEED = "\n";
	private final String filePath;
	private int startOffset;
	private int length;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	
	private CloneInstanceLocationInfo(String filePath) {
		this.filePath = filePath;
	}
	
	public CloneInstanceLocationInfo(String filePath, int startOffset, int endOffset) {
		this(filePath);
		this.startOffset = startOffset;
		this.length = endOffset - startOffset + 1;
		String containingFileContents = readFileContents(filePath);
		if (!"".equals(containingFileContents)) {
			if (this.length < containingFileContents.length()) {
				
				String[] linesBeforeAndIncludingOffset = getLinesBeforeAndIncludingOffset(containingFileContents, startOffset);
				this.startLine = linesBeforeAndIncludingOffset.length;
				this.startColumn = this.startOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, this.startLine - 1);
				
				linesBeforeAndIncludingOffset = getLinesBeforeAndIncludingOffset(containingFileContents, endOffset);
				this.endLine = linesBeforeAndIncludingOffset.length;
				this.endColumn = endOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, this.endLine - 1);
			}
		}
	}
	
	public CloneInstanceLocationInfo(String filePath, int startLine, int startColumn, int endLine, int endColumn) {
		this(filePath);
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
		String fileContents = readFileContents(filePath);
		String[] lines = fileContents.split(LINE_FEED);
		int numberOfCharsForLines = getNumberOfCharsForLines(lines, startLine - 1); // The first offset of the start line

		this.startOffset = numberOfCharsForLines + this.startColumn;
		while (isWhiteSpaceCharacter(fileContents.charAt(startOffset))) {
			this.startOffset++;
		}
		numberOfCharsForLines = getNumberOfCharsForLines(lines, endLine) - 1; // The last offset of the end line
		int endOffset = numberOfCharsForLines + this.endColumn;
		while (isWhiteSpaceCharacter(fileContents.charAt(endOffset))) {
			endOffset--;
		}
		this.length = endOffset - this.startOffset + 1;
	}
	
	private boolean isWhiteSpaceCharacter(char character) {
		return character == ' ' || character == '\t' || character == '\n' || character == '\r';
	}

	private String readFileContents(String filePath) {
		try {
			InputStream in = new FileInputStream(new File(filePath));
			InputStreamReader isr = new InputStreamReader(in);
			StringWriter sw = new StringWriter();
			int DEFAULT_BUFFER_SIZE = 1024 * 4;
			char[] buffer = new char[DEFAULT_BUFFER_SIZE];
			int n = 0;
			while (-1 != (n = isr.read(buffer))) {
				sw.write(buffer, 0, n);
			}
			isr.close();
			return sw.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private int getNumberOfCharsForLines(String[] lines, int line) {
		int charsBeforeLine = 0;
		for (int i = 0; i < line && i < lines.length; i++) {
			charsBeforeLine += lines[i].length() + LINE_FEED.length();
		}
		return charsBeforeLine;
	}

	private String[] getLinesBeforeAndIncludingOffset(String fileContents, int offset) {
		return fileContents.substring(0, offset - 1).split(LINE_FEED);
	}
	
	public String getContainingFilePath() {
		try {
			return (new File(filePath)).getCanonicalPath();
		} catch (IOException e) {
			return this.filePath;
		}
	}

	public int getStartOffset() {
		return startOffset;
	}

	public int getLength() {
		return length;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getStartColumn() {
		return startColumn;
	}

	public int getEndLine() {
		return endLine;
	}

	public int getEndColumn() {
		return endColumn;
	}
	
	public String getContainingFileContents() {
		return readFileContents(filePath);
	}
	
	@Override
	public boolean equals(Object other) {
		
		if (other == null)
			return false;
		
		if (other.getClass() != CloneInstanceLocationInfo.class)
			return false;
		
		CloneInstanceLocationInfo otherLocationInfo = (CloneInstanceLocationInfo) other;
		
		return this.filePath.equals(otherLocationInfo.filePath) &&
				this.startOffset == otherLocationInfo.startOffset &&
				this.length == otherLocationInfo.length;
		
	}
	
	@Override
	public int hashCode() {
		int result = 17;
		result = 31 * result + this.filePath.hashCode();
		result = 31 * result + this.startOffset;
		result = 31 * result + this.length;
		return result;
	}
	
	@Override
	public String toString() {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(filePath).append(System.lineSeparator());
		stringBuilder.append(String.format("<Offset: %s, Length: %s>)", this.startOffset, this.length));
		return stringBuilder.toString();
	}

}
