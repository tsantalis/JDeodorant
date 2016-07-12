package ca.concordia.jdeodorant.clone.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

public class CloneInstanceLocationInfo {
	
	private final String filePath;
	private int startOffset;
	private int endOffset; 
	private int length;
	private int startLine;
	private int startColumn;
	private int endLine;
	private int endColumn;
	private int updatedStartOffset;
	private int updatedEndOffset;
	
	public CloneInstanceLocationInfo(String filePath, int startOffset, int endOffset) {
		this.filePath = filePath;
		this.startOffset = startOffset;
		this.endOffset = endOffset;
		this.updatedStartOffset = startOffset;
		this.updatedEndOffset = endOffset;
		this.length = endOffset - startOffset + 1;
		String containingFileContents = readFileContents(filePath);
		if (!"".equals(containingFileContents)) {
			if (this.length < containingFileContents.length()) {			
				String linesBeforeAndIncludingOffset = containingFileContents.substring(0, startOffset - 1);
				this.startLine = getLines(linesBeforeAndIncludingOffset).length;
				this.startColumn = this.startOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, this.startLine - 1);
				
				linesBeforeAndIncludingOffset = containingFileContents.substring(0, endOffset - 1);
				this.endLine = getLines(linesBeforeAndIncludingOffset).length;
				this.endColumn = endOffset - getNumberOfCharsForLines(linesBeforeAndIncludingOffset, this.endLine - 1);
			}
		}
	}

	public CloneInstanceLocationInfo(String filePath, int startLine, int startColumn, int endLine, int endColumn) {
		this.filePath = filePath;
		this.startLine = startLine;
		this.startColumn = startColumn;
		this.endLine = endLine;
		this.endColumn = endColumn;
		String fileContents = readFileContents(filePath);
		int numberOfCharsForLines = getNumberOfCharsForLines(fileContents, startLine - 1); // The first offset of the start line

		this.startOffset = numberOfCharsForLines + this.startColumn;
		while (this.startOffset < fileContents.length() && isWhiteSpaceCharacter(fileContents.charAt(startOffset))) {
			this.startOffset++;
		}
		numberOfCharsForLines = getNumberOfCharsForLines(fileContents, endLine) - 1; // The last offset of the end line
		this.endOffset = numberOfCharsForLines + this.endColumn;

		while (this.endOffset >= 0 && isWhiteSpaceCharacter(fileContents.charAt(this.endOffset))) {
			this.endOffset--;
		}
		this.length = this.endOffset - this.startOffset + 1;
		this.updatedStartOffset = startOffset;
		this.updatedEndOffset = endOffset;
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

	private String[] getLines(String string) {
		if (string.indexOf("\n") >= 0) {
			return string.split("\n");
		} else if (string.indexOf("\r") >= 0) {
			return string.split("\r");
		}
		return new String[] { string };
	}
	
	private int getNumberOfCharsForLines(String fileContents, int line) {
		int charsBeforeLine = 0;
		String[] lines = getLines(fileContents);
		for (int i = 0; i < line && i < lines.length; i++) {
			charsBeforeLine += lines[i].length() + 1; // 1 for Line Feed character
		}
		// Happens when the last char of the document is not a line feed character
		if (charsBeforeLine > fileContents.length() - 1) {
			charsBeforeLine = fileContents.length() - 1;
		}
		return charsBeforeLine;
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
	
	public int getEndOffset() {
		return endOffset;
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
	
	public boolean updateOffsets(int startOffset, int endOffset) {
		if (this.updatedStartOffset != startOffset || this.updatedEndOffset != endOffset) {
			this.updatedStartOffset = startOffset;
			this.updatedEndOffset = endOffset;
			return true;
		}
		return false;
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

	public CloneInstanceStatus getCloneLocationStatus() {
		if (updatedStartOffset == -1)
			return CloneInstanceStatus.TAMPERED;
		else if (this.updatedStartOffset == this.startOffset && this.updatedEndOffset == this.endOffset)
			return CloneInstanceStatus.ORIGINAL_LOCATION;
		else
			return CloneInstanceStatus.OFFSETS_SHIFTED;
	}

	public int getUpdatedStartOffset() {
		return this.updatedStartOffset;
	}

	public int getUpdatedEndOffset() {
		return this.updatedEndOffset;
	}

}
