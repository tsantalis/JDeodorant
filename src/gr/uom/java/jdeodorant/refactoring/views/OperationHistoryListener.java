package gr.uom.java.jdeodorant.refactoring.views;

import gr.uom.java.ast.CompilationUnitCache;

import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.OperationHistoryEvent;

public class OperationHistoryListener implements IOperationHistoryListener {

	public void historyNotification(OperationHistoryEvent event) {
		if(event.getEventType() == OperationHistoryEvent.UNDONE ||
				event.getEventType() == OperationHistoryEvent.REDONE ||
				event.getEventType() == OperationHistoryEvent.DONE) {
			CompilationUnitCache.getInstance().clearCache();
		}
	}

}
