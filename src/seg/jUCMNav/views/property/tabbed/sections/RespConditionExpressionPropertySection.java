package seg.jUCMNav.views.property.tabbed.sections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

public class RespConditionExpressionPropertySection extends
		ConditionExpressionPropertySection {

	protected Text createText(Composite parent) {
		Text result = getWidgetFactory().createText(parent, "", SWT.MULTI | SWT.WRAP);
		
		result.setEnabled(false);

		GridData gridData = new GridData();
		gridData.heightHint = 75;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.verticalAlignment = GridData.CENTER;
		
		result.setLayoutData(gridData);
		
		return result;
	}
	
}
