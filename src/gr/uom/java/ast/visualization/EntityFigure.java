package gr.uom.java.ast.visualization;



import java.util.ArrayList;
import java.util.List;
import org.eclipse.draw2d.Label;
import org.eclipse.draw2d.MarginBorder;
import org.eclipse.swt.graphics.Image;



public class EntityFigure extends Label{

	private String name;
	private List<JConnection> outgoingConnections = new ArrayList<JConnection>();
	private LeftAnchor leftAnchor= null;
	private RightAnchor rightAnchor= null;
	
	public EntityFigure(String name){
		super(name);
		this.name= name;

		//new EntityFigureListener(this);


		setLabelAlignment(LEFT);

		setOpaque(true);
		setBorder(new MarginBorder(3,3,3,3));

		setSize(200,200);
		setToolTip(new Label(name));

	}

	public EntityFigure(String name, Image image, boolean highlight){
		super(name, image);
		
		if (highlight)
		new EntityFigureListener(this);
		
		this.name= name;

		setLabelAlignment(LEFT);

		setOpaque(true);
		setBorder(new MarginBorder(3,3,3,3));

		setSize(200,200);
		setToolTip(new Label(name));


	}
	
	

	public LeftAnchor getLeftAnchor() {
		return leftAnchor;
	}

	public void setLeftAnchor(LeftAnchor leftAnchor) {
		this.leftAnchor = leftAnchor;
	}

	public RightAnchor getRightAnchor() {
		return rightAnchor;
	}

	public void setRightAnchor(RightAnchor rightAnchor) {
		this.rightAnchor = rightAnchor;
	}

	public String getName() {
		return name;
	}

	public List<JConnection> getOutgoingConnections() {
		return outgoingConnections;
	}



	public JConnection addLeftLeftConnection(ConnectionType type, EntityFigure label, Integer occurences, int bendHeight){

		JConnection connection = new JConnection(type);

		connection.setLeftLeftAnchors(this, label);

		//connection.setSourceBendRouter(bendHeight, classWidth);
		connection.setFullBendRouter(bendHeight);
		connection.setLabel(occurences);
		outgoingConnections.add(connection);


		return connection;
	}

	public JConnection addRightRightConnection(ConnectionType type, EntityFigure label, Integer occurences, int bendHeight){

		JConnection connection = new JConnection(type);

		connection.setRightRightAnchors(this, label);

		//connection.setSourceBendRouter(-bendHeight, -classWidth);
		connection.setFullBendRouter(-bendHeight);
		connection.setLabel(occurences);
		outgoingConnections.add(connection);


		return connection;
	}

	public JConnection addRightLeftConnection(ConnectionType type, EntityFigure label, Integer occurences){

		JConnection connection = new JConnection(type);
		connection.setRightLeftAnchors(this, label);
		connection.setLabel(occurences);

		outgoingConnections.add(connection);
		return connection;
	}

	public JConnection addLeftRightConnection(ConnectionType type,EntityFigure label, Integer occurences){

		JConnection connection = new JConnection(type);

		connection.setLeftRightAnchors(this, label);

		connection.setLabel(occurences);
		outgoingConnections.add(connection);
		return connection;
	}

	public JConnection addToSourceMethodConnection(ConnectionType type, EntityFigure label, Integer occurences){
		JConnection connection = addLeftRightConnection(type, label, occurences);
		connection.setMethodToMethodStyle();
		connection.setDottedLine();
		return connection;
	}

	public JConnection addToTargetMethodConnection(ConnectionType type, EntityFigure label, Integer occurences){
		JConnection connection = addRightLeftConnection(type, label, occurences);
		connection.setMethodToMethodStyle();

		return connection;
	}

	public JConnection addToSameClassWriteConnectionRR(ConnectionType type, EntityFigure label, Integer occurences, int bendHeight){
		JConnection connection = addRightRightConnection(type, label, occurences, bendHeight);

		connection.setWriteStyle();

		return connection;
	}

	public JConnection addToSameClassWriteConnectionLL(ConnectionType type,EntityFigure label, Integer occurences, int bendHeight){
		JConnection connection = addLeftLeftConnection(type, label, occurences, bendHeight);

		connection.setWriteStyle();

		return connection;
	}

	/*public JConnection addMethodToMethodConnection(ConnectionType type,EntityFigure label, Integer occurences, int classWidth, int bendHeight){
		JConnection connection = addRightRightConnection(type,label, occurences, classWidth, bendHeight);

		connection.setReadStyle();
		connection.setForegroundColor(ColorConstants.cyan);

		return connection;
	}*/

	public JConnection addToSameClassReadConnectionRR(ConnectionType type,EntityFigure label, Integer occurences, int bendHeight){
		JConnection connection = addRightRightConnection(type, label, occurences, bendHeight);

		connection.setReadStyle();

		return connection;
	}

	public JConnection addToSameClassReadConnectionLL(ConnectionType type,EntityFigure label, Integer occurences, int bendHeight){
		JConnection connection = addLeftLeftConnection(type, label, occurences, bendHeight);

		connection.setReadStyle();

		return connection;
	}

	public JConnection addToSameClassMethodConnectionRR(ConnectionType type, EntityFigure label, Integer occurences,int bendHeight){
		JConnection connection = addRightRightConnection(type,label, occurences, bendHeight);

		connection.setMethodToMethodStyle();

		return connection;
	}

	public JConnection addToSameClassMethodConnectionLL(ConnectionType type, EntityFigure label, Integer occurences,  int bendHeight){
		JConnection connection = addLeftLeftConnection(type, label, occurences,  bendHeight);

		connection.setMethodToMethodStyle();

		return connection;
	}

	public JConnection addLeftRightMethodConnection(ConnectionType type,EntityFigure name, Integer occurences){
		JConnection connection = addLeftRightConnection(type,name, occurences);

		connection.setMethodToMethodStyle();

		return connection;
	}

	public JConnection addRightLeftMethodConnection(ConnectionType type,EntityFigure name, Integer occurences){
		JConnection connection = addRightLeftConnection(type, name, occurences);
		connection.setMethodToMethodStyle();

		return connection;
	}

	public JConnection addToSourceReadConnection(ConnectionType type, EntityFigure name, Integer occurences){
		JConnection connection = addLeftRightConnection(type, name, occurences);
		

		connection.setReadStyle();

		return connection;
	}

	public JConnection addToSourceWriteConnection(ConnectionType type, EntityFigure name, Integer occurences){

		JConnection connection = addLeftRightConnection(type, name, occurences);

		connection.setWriteStyle();

		return connection;
	}

	public JConnection addToSourceWeakReadConnection(ConnectionType type, EntityFigure name, Integer occurences){
		JConnection connection = addToSourceReadConnection(type, name, occurences);
		connection.setDottedLine();
		
		return connection;
	}

	public JConnection addToSourceWeakWriteConnection(ConnectionType type,EntityFigure name, Integer occurences){
		JConnection connection = addToSourceWriteConnection(type, name, occurences);
		connection.setDottedLine();

		return connection;
	}

	public JConnection addToTargetBendConnection(ConnectionType type, EntityFigure name, Integer occurences){
		JConnection connection = addRightLeftConnection(type, name, occurences);
		connection.setSlightBendRouter();
		connection.setWriteStyle();

		return connection;
	}
	
	public JConnection addToSourceBendConnection(ConnectionType type, EntityFigure name, Integer occurences){
		JConnection connection = addLeftRightConnection(type, name, occurences);
		connection.setSlightBendRouter();
		connection.setDottedLine();
		connection.setWriteStyle();

		return connection;
	}

	public JConnection addToTargetReadConnection(ConnectionType type, EntityFigure name, Integer occurences){
		JConnection connection = addRightLeftConnection(type, name, occurences);
		connection.setReadStyle();

		return connection;
	}


	public JConnection addToTargetWriteConnection(ConnectionType type,EntityFigure name, Integer occurences){
		JConnection connection = addRightLeftConnection(type, name, occurences);
		connection.setWriteStyle();

		return connection;
	}


}
