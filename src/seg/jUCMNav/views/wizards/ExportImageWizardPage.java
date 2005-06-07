package seg.jUCMNav.views.wizards;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;

import ucm.map.Map;

/**
 * Created on 3-Jun-2005
 * 
 * Contains controls to set the export directory, export file type and selected maps.
 * 
 * @author jkealey
 *  
 */
public class ExportImageWizardPage extends WizardPage {

    private Combo cboImageType;

    // widget list
    private List lstMaps;
    private HashMap mapsToEditor;
    private Vector mapsToExport;
    private Text txtExportPath;

    /**
     * @param pageName
     */
    protected ExportImageWizardPage(String pageName, Vector mapsToExport, HashMap mapsToEditor) {
        super(pageName);
        setDescription("Please select the image export format and choose an export directory. Existing files will be overwritten.");
        setTitle("Export Image Wizard");

        this.mapsToEditor = mapsToEditor;
        this.mapsToExport = mapsToExport;
        
        

        if (mapsToExport.size() == 0) {
            setErrorMessage("No maps have been selected for export. Either you did not select a .jucm file or the .jucm file you selected does not contain any Maps.");
            setPageComplete(false);
        }

    }

    /**
     * Contains controls to set the export directory, export file type and selected maps.
     */
    public void createControl(Composite parent) {
        // create the composite to hold the widgets
        Composite composite = new Composite(parent, SWT.NONE);

        // create the desired layout for this wizard page
        GridLayout gl = new GridLayout(4, false);
        composite.setLayout(gl);
        GridData data;

        Label lblPath = new Label(composite, SWT.NONE);
        lblPath.setText("Directory to export images to : ");
        data = new GridData();
        data.horizontalSpan = 4;
        lblPath.setLayoutData(data);

        txtExportPath = new Text(composite, SWT.BORDER | SWT.SINGLE | SWT.LEFT);
        txtExportPath.setText(getPath());

        data = new GridData();
        data.horizontalAlignment = GridData.FILL;
        data.grabExcessHorizontalSpace = true;
        data.horizontalSpan = 3;
        //        data.grabExcessVerticalSpace = true;
        txtExportPath.setLayoutData(data);
        txtExportPath.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                File dir = new File(txtExportPath.getText());
                if (!(dir.exists() && dir.isDirectory())) {
                    setErrorMessage("Invalid path specified");
                    setPageComplete(false);
                } else {
                    setErrorMessage(null);
                    setPageComplete(true);
                }
            }
        });

        Button b = new Button(getShell(), SWT.PUSH);
        b.setParent(composite);
        b.setText("...");
        b.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(getShell(), SWT.OPEN);
                dialog.setFilterPath(txtExportPath.getText());

                dialog.setText("Please select the export directory");
                String path = dialog.open();

                if (path != null) {
                    txtExportPath.setText(path);
                }

            }
        });

        Label lblOrientation = new Label(composite, SWT.NONE);
        lblOrientation.setText("File type : ");
        data = new GridData();
        data.horizontalSpan = 1;
        data.horizontalAlignment = GridData.FILL;
        lblOrientation.setLayoutData(data);

        cboImageType = new Combo(composite, SWT.READ_ONLY);

        //cboImageType.setItems(new String[] { "BMP", "GIF", "JPEG", "PNG", "TIFF" });

        // others not yet implemented.
        // 
        // Example :
        // https://bugs.eclipse.org/bugs/show_bug.cgi?id=24697
        //

        cboImageType.setItems(new String[] { "BMP", "JPEG" });
        cboImageType.select(getImageType());

        data = new GridData();
        data.horizontalSpan = 3;
        data.horizontalAlignment = GridData.FILL;
        cboImageType.setLayoutData(data);

        Label lblMaps = new Label(composite, SWT.NONE);
        lblMaps.setText("Maps to be exported: ");
        data = new GridData();
        data.horizontalSpan = 4;
        data.horizontalAlignment = GridData.FILL;
        lblMaps.setLayoutData(data);

        lstMaps = new List(composite, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        for (Iterator iter = mapsToExport.iterator(); iter.hasNext();) {
            Map map = (Map) iter.next();
            lstMaps.add(((ExportImageWizard) getWizard()).getMapName(map));
        }
        lstMaps.selectAll();
        lstMaps.add("kjhlkj");
        lstMaps.addSelectionListener(new SelectionListener() {
            public void widgetDefaultSelected(SelectionEvent e) {
                setPageComplete(lstMaps.getSelectionCount() > 0);
            }

            public void widgetSelected(SelectionEvent e) {
                setPageComplete(lstMaps.getSelectionCount() > 0);
            }
        });

        data = new GridData();
        data.heightHint = 200;
        data.horizontalSpan = 4;
        data.horizontalAlignment = GridData.FILL;
        lstMaps.setLayoutData(data);

        setControl(composite);
    }

    /**
     * Updates passed Vector and preference store with the selection properties
     * 
     * @return
     */
    public boolean finish() {
        File dir = new File(txtExportPath.getText());
        if (!(dir.exists() && dir.isDirectory())) {
            setErrorMessage("Invalid path specified");
            return false;
        }

        setPath(txtExportPath.getText());
        setImageType(cboImageType.getSelectionIndex());
        updateMapsToExport();

        return true;
    }

    public int getImageType() {
        return ExportImageWizard.getPreferenceStore().getInt(ExportImageWizard.PREF_IMAGETYPE);
    }

    public String getPath() {
        return ExportImageWizard.getPreferenceStore().getString(ExportImageWizard.PREF_PATH);
    }

    public void setImageType(int type) {
        ExportImageWizard.getPreferenceStore().setValue(ExportImageWizard.PREF_IMAGETYPE, type);
    }

    public void setPath(String path) {
        ExportImageWizard.getPreferenceStore().setValue(ExportImageWizard.PREF_PATH, path);
    }

    /**
     * Rebuilds mapToExport according to the current selection.
     *  
     */
    private void updateMapsToExport() {
        Vector toKeep = new Vector();
        for (int i = 0; i < lstMaps.getSelectionIndices().length; i++) {
            int index = lstMaps.getSelectionIndices()[i];
            toKeep.add(mapsToExport.get(i));
        }

        mapsToExport.removeAllElements();
        mapsToExport.addAll(toKeep);

    }

}