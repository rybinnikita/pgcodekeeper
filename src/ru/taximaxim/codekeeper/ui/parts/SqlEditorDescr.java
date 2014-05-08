package ru.taximaxim.codekeeper.ui.parts;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.di.extensions.EventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts;
import ru.taximaxim.codekeeper.ui.pgdbproject.PgDbProject;

public class SqlEditorDescr {
    
    @Inject
    private MPart part;
    
    @Inject
    private EPartService partService;
    
    @PostConstruct
    private void postConstruct(Composite parent, PgDbProject proj)
            throws UnsupportedEncodingException, IOException {
        parent.setLayout(new FillLayout());
        
        Text txt = new Text(parent, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.READ_ONLY | SWT.MULTI);
        
        txt.setFont(JFaceResources.getFont(JFaceResources.TEXT_FONT));
        txt.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        
        File fileText = new File(part.getPersistedState().get(UIConsts.PART_SQL_EDITOR_FILENAME));
        txt.setText(new String(Files.readAllBytes(fileText.toPath()),
                proj.getString(UIConsts.PROJ_PREF_ENCODING)));
    }
    
    /**
     * On PgDbProject change and reinjection close editors that do not belong to new project.
     * 
     * @param proj
     */
    @Inject
    private void projectChanged(
            PgDbProject proj,
            @Optional
            @EventTopic(UIConsts.EVENT_REOPEN_PROJECT)
            PgDbProject proj2) {
        File myFile = new File(
                part.getPersistedState().get(UIConsts.PART_SQL_EDITOR_FILENAME));
        if(proj == null
                || !myFile.toPath().startsWith(proj.getProjectWorkingDir().toPath()) || !myFile.exists()) {
            partService.hidePart(part);
        }
    }
    
    public static void openNew(File file, EModelService model, EPartService partService,
            MApplication app, String nameToDisplay) {
        for(MPart existingPart : model.findElements(
                app, UIConsts.PART_SQL_EDITOR, MPart.class, null)) {
            if(file.getAbsolutePath().equals(
                    existingPart.getPersistedState().get(UIConsts.PART_SQL_EDITOR_FILENAME))) {
                partService.activate(existingPart);
                return;
            }
        }
        
        Log.log(Log.LOG_DEBUG, "About to show editor: " + file.getAbsolutePath());
        
        MPart newEditor = partService.createPart(UIConsts.PART_SQL_EDITOR);
        newEditor.setLabel(nameToDisplay);
        newEditor.setTooltip(file.getAbsolutePath());
        newEditor.getPersistedState().put(
                UIConsts.PART_SQL_EDITOR_FILENAME, file.getAbsolutePath());
        ((MPartStack) model.find(UIConsts.PART_STACK_EDITORS, app)).getChildren().add(newEditor);
        partService.activate(newEditor, false);
    }
}
