package ca.concordia.jdeodorant.clone.parsers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;

public class CCFinderOutputParser extends CloneDetectorOutputParser {

	private static class Token {
		
		private final int startOffset;
		private final int endOffst;
		
		public Token(int startOffset, int endOffset) {
			this.startOffset = startOffset;
			endOffst = endOffset;
		}

		public int getStartOffset() {
			return startOffset;
		}

		public int getEndOffst() {
			return endOffst;
		}
		
		@Override
		public String toString() {
			return String.format("<%s, %s>", getStartOffset(), getEndOffst());
		}
	}
	
	private static class CloneFragment {
		
		private final long cloneGroupID;
		private final String path;
		private final int start;
		private final int end;
		
		public CloneFragment(long cloneGroupID, String path, int start, int end) {
			this.cloneGroupID = cloneGroupID;
			this.path = path;
			this.start = start;
			this.end = end;
		}
		
		public long getCloneGroupID() {
			return cloneGroupID;
		}
		
		public String getPath() {
			return path;
		}
		
		public int getStart() {
			return start;
		}
		
		public int getEnd() {
			return end;
		}
		
		@Override
		public String toString() {
			return String.format("Start token: %s, End token: %s, File: %s, Clone Group ID: % s", 
					getStart(),
					getEnd(),
					getPath(),
					getCloneGroupID());
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (int) (cloneGroupID ^ (cloneGroupID >>> 32));
			result = prime * result + end;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
			result = prime * result + start;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			CloneFragment other = (CloneFragment) obj;
			if (cloneGroupID != other.cloneGroupID)
				return false;
			if (end != other.end)
				return false;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			if (start != other.start)
				return false;
			return true;
		}
		
	}

	private final String pathToCcfxprepdir; 
	private final RandomAccessFile raFile;
	private String analyzedPathPrefix;
	private String preprocessedFilePostfix;
	private static final byte LINE_FEED_BYTE = (byte) 0xa/* \n */;
	private final Map<String, List<Token>> preprocessedFilesTokens = new HashMap<String, List<Token>>();
	private Map<Long, Set<CloneFragment>> cloneFragments = new HashMap<Long, Set<CloneFragment>>();
	private Set<Long> cloneSetIDs = new TreeSet<Long>(new Comparator<Long>() {
		public int compare(Long o1, Long o2) {
			return Long.compare(o1, o2);
		}
	});
	
	public CCFinderOutputParser(IJavaProject iJavaProject, String cloneOutputFilePath, String pathToCcfxprepdir) throws InvalidInputFileException {
		super(iJavaProject, formatPath(cloneOutputFilePath));
		this.pathToCcfxprepdir = formatPath(pathToCcfxprepdir);
		
		try {
			raFile = new RandomAccessFile(cloneOutputFilePath, "r");
			FileChannel channel = raFile.getChannel();

			ByteBuffer buffer = ByteBuffer.allocate(8);
			channel.read(buffer);
			byte[] ary = buffer.array();
			String magicString = new String(ary);
			if (!magicString.equals("ccfxraw0")) {
				throw new InvalidInputFileException();
			}
			
			int v1 = readInt(channel);
			int v2 = readInt(channel);
			readInt(channel);
			
			buffer = ByteBuffer.allocate(4);
			channel.read(buffer);
			ary = buffer.array();
			String b = new String(ary);
			if (b.equals("pa:d")) {
				if (!(v1 == 167772160 && v2 == 33554432)) {
					throw new InvalidInputFileException("Version mismatch");
				}
			} else {
				throw new InvalidInputFileException("Invalid format");
			}
			
		} catch (FileNotFoundException e) {
			throw new InvalidInputFileException(e);
		} catch (IOException e) {
			throw new InvalidInputFileException(e);
		}
		
		readCloneSetData();
		
		this.setCloneGroupCount(cloneSetIDs.size());
	}

	@Override
	public CloneGroupList readInputFile() throws InvalidInputFileException {

		CloneGroupList cloneGroups = new CloneGroupList(getIJavaProject());
		int cloneSetIndex = 0;
		for (long cloneSetID : cloneSetIDs) {
			
			CloneGroup cloneGroup = new CloneGroup((int)cloneSetID);
			
			Set<CloneFragment> cloneFragments = this.cloneFragments.get(cloneSetID);
			int cloneInstanceIndex = 0;
			for (CloneFragment cloneFragment : cloneFragments) {
				try {
					String path = cloneFragment.getPath().replace(this.analyzedPathPrefix, "");
					String preprocessedFilePath = 
							this.pathToCcfxprepdir + path.replace(this.analyzedPathPrefix, "") + this.preprocessedFilePostfix;
					List<Token> tokens = getPreproprossedFile(preprocessedFilePath);
					int startOffst = tokens.get(cloneFragment.getStart()).getStartOffset();
					int endOffset = tokens.get(cloneFragment.getEnd() - 1).getEndOffst() - 1;
					CloneInstance cloneInstance = getCloneInstance(path, cloneInstanceIndex, false, startOffst, endOffset);
					cloneGroup.addClone(cloneInstance);
					cloneInstanceIndex++;
				} catch (JavaModelException jme) {
					addExceptionHappenedDuringParsing(jme);
				} catch (ICompilationUnitNotFoundException iunf) {
					addExceptionHappenedDuringParsing(iunf);
				}
			}

			if (cloneGroup.getCloneGroupSize() > 1)
				cloneGroups.add(cloneGroup);
			
			progress(cloneSetIndex++);
		}
		
		if (cloneGroups.getCloneGroupsCount() == 0)
			throw new InvalidInputFileException();
		
		return cloneGroups;

	}
	
	private void readCloneSetData() throws InvalidInputFileException {
		
		FileChannel channel = raFile.getChannel();

		try {
			// Skip options, get n and preprocessed_file_postfix only
			while (true) {
				String line = readUtf8StringUntil(channel, LINE_FEED_BYTE);
				if (line.startsWith("n")) {
					this.analyzedPathPrefix = formatPath(line.split("\t")[1]);
				} else if (line.startsWith("preprocessed_file_postfix")) {
					this.preprocessedFilePostfix = line.split("\t")[1];
				}
				if (line.length() == 0) {
					line = readUtf8StringUntil(channel, LINE_FEED_BYTE); 
					if (!"java".equals(line))
						throw new InvalidInputFileException();
					break;
				}
			}

			List<String> sourceFilesList = new ArrayList<String>();

			// Read file info
			while (true) {
				String filePath = readUtf8StringUntil(channel, LINE_FEED_BYTE);
				if (filePath.length() == 0) {
					int fileId = readInt(channel);
					int length = readInt(channel);
					if (!(fileId == 0 && length == 0)) {
						throw new InvalidInputFileException("Invalid file terminator");
					}
					break;
				}
				sourceFilesList.add(filePath.replace("\\", "/"));
				/*int fileId = */readInt(channel);
				/*int length = */readInt(channel);
			}

			// read source file remarks
			Map<Integer, List<String>> sourceFileRemarks = new HashMap<Integer, List<String>>();
			while (true) {
				String remarkText = readUtf8StringUntil(channel, LINE_FEED_BYTE);
				if (remarkText.length() == 0) {
					int fileId = readInt(channel);
					if (fileId != 0) {
						throw new InvalidInputFileException("Invalid file remark terminator");
					}
					break; // while true
				}
				int fileId = readInt(channel);
				List<String> remarks = null;
				if (!sourceFileRemarks.containsKey(fileId)) {
					remarks = new ArrayList<String>();
					sourceFileRemarks.put(fileId, remarks);
				} else {
					remarks = sourceFileRemarks.get(fileId);
				}
				remarks.add(remarkText);
			}
			
			// read clone data
			ByteBuffer bbuffer32 = ByteBuffer.allocate(32);
			bbuffer32.order(ByteOrder.LITTLE_ENDIAN);
			long position = channel.position();
			while (position < channel.size()) {
				bbuffer32.clear();
				channel.read(bbuffer32);
				int leftFileIndex = bbuffer32.getInt(0);
				if (leftFileIndex == 0)
					break;
				int leftFileBegin = bbuffer32.getInt(4);
				int leftFileEnd = bbuffer32.getInt(8);
				int rightFileIndex = bbuffer32.getInt(12);
				int rightFileBegin = bbuffer32.getInt(16);
				int rightFileEnd = bbuffer32.getInt(20);
				long cloneGroupID = bbuffer32.getLong(24);
				cloneSetIDs.add(cloneGroupID);
				CloneFragment cloneFragmentLeft = new CloneFragment(cloneGroupID, sourceFilesList.get(leftFileIndex - 1), leftFileBegin, leftFileEnd);
				CloneFragment cloneFragmentRight = new CloneFragment(cloneGroupID, sourceFilesList.get(rightFileIndex - 1), rightFileBegin, rightFileEnd);
				Set<CloneFragment> cloneFragmentsForThisCloneSet;
				if (!cloneFragments.containsKey(cloneGroupID)) {
					cloneFragmentsForThisCloneSet = new HashSet<CloneFragment>(); 
					cloneFragments.put(cloneGroupID, cloneFragmentsForThisCloneSet);
				} else {
					cloneFragmentsForThisCloneSet = cloneFragments.get(cloneGroupID);
				}
				cloneFragmentsForThisCloneSet.add(cloneFragmentLeft);
				cloneFragmentsForThisCloneSet.add(cloneFragmentRight);
				position = channel.position();
			}

		} catch (IOException ioex) {
			ioex.printStackTrace();
		} finally {
			try {
				channel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private String readUtf8StringUntil(FileChannel c, byte terminatingByte) throws IOException {
		List<Byte> bytes = new ArrayList<Byte>();
		while (true) {
			ByteBuffer buffer = ByteBuffer.allocate(1);
			if (c.read(buffer) == -1) {
				throw new IOException();
			}
			byte[] array = buffer.array();
			if (array[0] == terminatingByte) {
				byte[] toReturnBytes = new byte[bytes.size()];
				for (int i = 0; i < bytes.size(); i++)
					toReturnBytes[i] = bytes.get(i);
				try {	
					return new String(toReturnBytes, "UTF-8");
				} catch (UnsupportedEncodingException e) {
					return "";
				}
			}
			bytes.add(array[0]);
		}
		
	}
	
	private List<Token> getPreproprossedFile(String filePath) {
		
		if (this.preprocessedFilesTokens.containsKey(filePath))
			return this.preprocessedFilesTokens.get(filePath);
		
		List<Token> toReturn = new ArrayList<Token>();
		
		String fileContents = readFileContents(filePath);
		
		String linePattern = "([a-f0-9]+)\\.([a-f0-9]+)\\.([a-f0-9]+)\\t(([\\+a-f0-9]+)|([a-f0-9]+)\\.([a-f0-9]+)\\.([a-f0-9]+))\\t(.+)";
		Pattern pattern = Pattern.compile(linePattern);
		Matcher matcher = pattern.matcher(fileContents);
		
		while (matcher.find()) {
			int beginOffset = Integer.valueOf(matcher.group(3), 16);
			String lengthString = matcher.group(4);
			int length = 0;
			if (lengthString.startsWith("+")) {
				length = Integer.valueOf(matcher.group(5).substring(1), 16);
			} else {
				length = Integer.valueOf(matcher.group(8), 16);
			}
			Token token = new Token(beginOffset, beginOffset + length);
			toReturn.add(token);
		}
		
		this.preprocessedFilesTokens.put(filePath, toReturn);
		
		return toReturn;
		
	}
	
	public int readInt(FileChannel channel) throws IOException {
		ByteBuffer buffer = ByteBuffer.allocate(4);
		channel.read(buffer);
		buffer.rewind();
		return buffer.getInt();
	}

}
