package ru.taximaxim.codekeeper.apgdiff.model.exporter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.loader.ProjectLoader;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgPrivilege;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgStatementWithSearchPath;
import cz.startnet.utils.pgdiff.schema.StatementOverride;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement;

public class PgOverridesModelExporter extends AbstractModelExporter {

    public PgOverridesModelExporter(Path outDir, PgDatabase newDb, PgDatabase oldDb,
            Collection<TreeElement> changedObjects, String sqlEncoding) {
        super(outDir, newDb, oldDb, changedObjects, sqlEncoding);
    }

    @Override
    public void exportFull() throws IOException {
        // no impl
    }

    @Override
    protected void deleteObject(TreeElement el) {
        // no impl
    }

    @Override
    protected void editObject(TreeElement el) throws IOException {
        PgStatement st = el.getPgStatement(newDb);
        switch (st.getStatementType()) {
        case CONSTRAINT:
        case INDEX:
        case TRIGGER:
        case RULE:
        case EXTENSION:
        case COLUMN:
            break;
        case FUNCTION:
        case PROCEDURE:
        case OPERATOR:
            dumpFuncOverrides(el, st);
            break;
        default:
            deleteStatementIfExists(st);
            dumpOverrides(st);
        }
    }

    @Override
    protected void createObject(TreeElement el) {
        // no impl
    }

    private void dumpFuncOverrides(TreeElement el, PgStatement st) throws IOException {
        PgDiffArguments args = new PgDiffArguments();
        Path path = outDir.resolve(getRelativeFilePath(st, true));
        StringBuilder sb = new StringBuilder();

        Map<PgStatement, StatementOverride> privs;
        try {
            privs = new ProjectLoader(outDir.toString(), args)
                    .getOverridesFromPath(path, oldDb);
        } catch (InterruptedException e) {
            // unreachable
            throw new IllegalStateException(e);
        }
        boolean isFound = false;
        for (Entry<PgStatement, StatementOverride> entry : privs.entrySet()) {
            PgStatement oldSt = entry.getKey();
            if (st.getName().equals(oldSt.getName())) {
                isFound = true;
                // new state
                PgStatement.appendOwnerSQL(st, st.getOwner(), sb);
                st.appendPrivileges(sb);
            } else {
                // we can't change oldSt
                StatementOverride override = entry.getValue();
                PgStatement.appendOwnerSQL(oldSt, override.getOwner(), sb);

                if (!override.getPrivileges().isEmpty()) {
                    sb.append("\n\n-- ")
                    .append(oldSt.getStatementType())
                    .append(' ');
                    sb.append(((PgStatementWithSearchPath)oldSt).getContainingSchema().getName())
                    .append('.');
                    sb.append(oldSt.getName())
                    .append(' ')
                    .append("GRANT\n");

                    // old state
                    for (PgPrivilege priv : override.getPrivileges()) {
                        sb.append('\n').append(priv.getCreationSQL()).append(';');
                    }
                }
            }
            sb.append(GROUP_DELIMITER);
        }

        if (!isFound) {
            // add new privileges to end if not found
            PgStatement.appendOwnerSQL(st, st.getOwner(), sb);
            st.appendPrivileges(sb);
        } else if (sb.length() > 0) {
            // remove trailing delimiter from loop above
            sb.setLength(sb.length() - GROUP_DELIMITER.length());
        }

        deleteStatementIfExists(st);

        if (sb.length() > 0) {
            dumpSQL(sb.toString(), path);
        }
    }

    @Override
    protected Path getRelativeFilePath(PgStatement st, boolean addExtension) {
        return ModelExporter.getRelativeFilePath(
                st, Paths.get(ApgdiffConsts.OVERRIDES_DIR), addExtension);
    }
}
