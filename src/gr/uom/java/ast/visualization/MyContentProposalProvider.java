package gr.uom.java.ast.visualization;

import java.util.ArrayList;

import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;

public class MyContentProposalProvider implements IContentProposalProvider {

	private String[] proposals;
	
	public MyContentProposalProvider(String[] classNames){
		super();
		this.proposals= classNames;
	}

	public IContentProposal[] getProposals(String contents, int position) {

		ArrayList list = new ArrayList();
		for (int i = 0; i < proposals.length; i++) {
			//if (proposals[i].length() >= contents.length()	&& proposals[i].substring(0, contents.length()).equalsIgnoreCase(contents) ) {
			if(proposals[i].toLowerCase().contains(contents.toLowerCase())){
				list.add(new ContentProposal(proposals[i]));
			}
		}
		return (IContentProposal[]) list.toArray(new IContentProposal[list.size()]);

	}

}
