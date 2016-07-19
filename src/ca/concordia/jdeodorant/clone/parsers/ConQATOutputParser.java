package ca.concordia.jdeodorant.clone.parsers;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ca.concordia.jdeodorant.clone.parsers.ResourceInfo.ICompilationUnitNotFoundException;

public class ConQATOutputParser extends CloneDetectorOutputParser{
	
	private Document document;

	public ConQATOutputParser(IJavaProject iJavaProject, String cloneOutputFilePath) throws InvalidInputFileException {
		super(iJavaProject, cloneOutputFilePath);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setIgnoringElementContentWhitespace(true);
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			File file = new File(this.getToolOutputFilePath());
			this.document = builder.parse(file);
			NodeList cloneClassesNodeList = document.getElementsByTagName("cloneClass");
			if (cloneClassesNodeList.getLength() != 0) {
				this.setCloneGroupCount(cloneClassesNodeList.getLength());
			} else {			
				this.document = null;
				throw new InvalidInputFileException();
			}
		} catch (IOException ioex) {
			ioex.printStackTrace();
		} catch (SAXException saxe) {
			saxe.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	@Override
	public CloneGroupList readInputFile() throws InvalidInputFileException {

		CloneGroupList cloneGroups = new CloneGroupList(getIJavaProject());
		
		if (this.document == null)
			throw new InvalidInputFileException();
		
		Map<Integer, String> filesMap = getFilesIdToPathMap();
		
		NodeList cloneClassesNodeList = this.document.getElementsByTagName("cloneClass");
		for (int i = 0; i < cloneClassesNodeList.getLength(); i++) {
			
			if (this.isOperationCanceled())
				break;
			
			try {
				int cloneGroupIndex = i + 1;
				Node cloneClassNode = cloneClassesNodeList.item(i);
				int cloneGroupID = Integer.parseInt(cloneClassNode.getAttributes().getNamedItem("id").getNodeValue());
				CloneGroup cloneGroup = new CloneGroup(cloneGroupID);
				NodeList cloneClassNodeChilds = ((Element)cloneClassNode).getElementsByTagName("clone");
				for (int j = 0; j < cloneClassNodeChilds.getLength(); j++) {
					int cloneInstanceID = j + 1;
					Node cloneNode = cloneClassNodeChilds.item(j);
					int cloneFileID = Integer.parseInt(cloneNode.getAttributes().getNamedItem("sourceFileId").getNodeValue());
					String filePath = filesMap.get(cloneFileID);
					int startLine = Integer.parseInt(cloneNode.getAttributes().getNamedItem("startLine").getNodeValue()); 
					int endLine = Integer.parseInt(cloneNode.getAttributes().getNamedItem("endLine").getNodeValue()); ; 
					CloneInstance cloneInstance = getCloneInstance(filePath, cloneInstanceID, true, startLine, 0, endLine, 0);
					cloneGroup.addClone(cloneInstance);
				}
				
				if (cloneGroup.getCloneGroupSize() > 1)
					cloneGroups.add(cloneGroup);
				
				progress(cloneGroupIndex);
			} catch (NullPointerException npex) {
				addExceptionHappenedDuringParsing(npex);
			} catch (StringIndexOutOfBoundsException siobex) {
				addExceptionHappenedDuringParsing(siobex);
			} catch (NumberFormatException nfex) {
				addExceptionHappenedDuringParsing(nfex);
			} catch (JavaModelException jme) {
				addExceptionHappenedDuringParsing(jme);
			} catch (ICompilationUnitNotFoundException infe) {
				addExceptionHappenedDuringParsing(infe);
			}
		}
		
		if (cloneGroups.getCloneGroupsCount() == 0)
			throw new InvalidInputFileException();
		
		return cloneGroups;
	}
	
	private Map<Integer, String> getFilesIdToPathMap() {
		Map<Integer, String> resultingMap = new HashMap<Integer, String>();
		NodeList sourceFiles = this.document.getElementsByTagName("sourceFile");
		for (int i = 0; i < sourceFiles.getLength(); i++) {
			Node fileNode = sourceFiles.item(i);
			int id = Integer.parseInt(fileNode.getAttributes().getNamedItem("id").getNodeValue()); 
			String path = fileNode.getAttributes().getNamedItem("location").getNodeValue().replace("\\", "/");
			resultingMap.put(id, path);
		}
		return resultingMap;
	}

}
