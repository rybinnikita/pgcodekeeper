package ru.taximaxim.codekeeper.ui.pgdbproject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;

import ru.taximaxim.codekeeper.ui.Log;
import ru.taximaxim.codekeeper.ui.UIConsts.DBSources;
import ru.taximaxim.codekeeper.ui.UIConsts.PREF;
import ru.taximaxim.codekeeper.ui.UIConsts.PROJ_PREF;
import ru.taximaxim.codekeeper.ui.differ.DbSource;
import ru.taximaxim.codekeeper.ui.fileutils.ProjectUpdater;
import ru.taximaxim.codekeeper.ui.localizations.Messages;
import cz.startnet.utils.pgdiff.loader.ParserClass;
import cz.startnet.utils.pgdiff.schema.PgDatabase;

public class InitProjectFromSource implements IRunnableWithProgress {

    private final String exePgdump;
    private final String pgdumpCustom;
    private final String password;
    
    private final IPreferenceStore mainPrefs;
    private final PgDbProject props;

    private final String dumpPath;

    public InitProjectFromSource(final IPreferenceStore mainPrefs,
            final PgDbProject props, final String dumpPath, String password) {
        this.mainPrefs = mainPrefs;
        this.exePgdump = mainPrefs.getString(PREF.PGDUMP_EXE_PATH);
        this.pgdumpCustom = mainPrefs.getString(PREF.PGDUMP_CUSTOM_PARAMS);
        this.props = props;
        this.dumpPath = dumpPath;
        this.password = password;
    }

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
            Log.log(Log.LOG_INFO, "Init project at " + props.getPathToProject()); //$NON-NLS-1$
            
            SubMonitor pm = SubMonitor.convert(monitor, 
                    Messages.initProjectFromSource_initializing_project, 75);

            initRepoFromSource(pm);
            
            pm.done();
        } catch (IOException | CoreException ex) {
            throw new InvocationTargetException(ex, MessageFormat.format(
                            Messages.initProjectFromSource_ioexception_while_creating_project,
                                    ex.getLocalizedMessage()));
        }
    }

    /**
     * clean repository, generate new file structure, preserve and fix repo
     * metadata, repo rm/add, commit new revision
     * @throws CoreException 
     */
    private void initRepoFromSource(SubMonitor pm)
            throws IOException, InvocationTargetException, CoreException {
        SubMonitor taskpm = pm.newChild(25); // 50

        PgDatabase db;
        switch (DBSources.getEnum(props.getPrefs().get(PROJ_PREF.SOURCE, ""))) { //$NON-NLS-1$
        case SOURCE_TYPE_DB:
            db = DbSource.fromDb(mainPrefs.getBoolean(PREF.USE_ANTLR) ? 
                    ParserClass.getAntlr(taskpm, 1) : ParserClass.getLegacy(null, 1),
                    exePgdump, pgdumpCustom, props, password).get(taskpm);
            break;

        case SOURCE_TYPE_DUMP:
            db = DbSource.fromFile(mainPrefs.getBoolean(PREF.USE_ANTLR) ? 
                    ParserClass.getAntlr(taskpm, 1) : ParserClass.getLegacy(null, 1), dumpPath,
                    props.getProjectCharset()).get(taskpm);
            break;

        case SOURCE_TYPE_JDBC:
            db = DbSource.fromJdbc(props, password, mainPrefs.getBoolean(PREF.USE_ANTLR))
                    .get(taskpm);
            break;
            
        default:
            throw new InvocationTargetException(new IllegalStateException(
                    Messages.initProjectFromSource_init_request_but_no_schema_source));
        }
        if (taskpm.isCanceled()) {
            Log.log(Log.LOG_WARNING, "Task was cancelled");
        }
        pm.newChild(25).subTask(Messages.initProjectFromSource_exporting_db_model); // 75
        new ProjectUpdater(db, null, null, props).updateFull();
    }
}
