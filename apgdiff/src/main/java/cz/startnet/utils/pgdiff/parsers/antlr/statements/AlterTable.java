package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.util.Arrays;
import java.util.List;

import cz.startnet.utils.pgdiff.parsers.antlr.QNameParser;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Alter_table_statementContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Foreign_optionContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.IdentifierContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Identity_bodyContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Sequence_bodyContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Storage_parameter_optionContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Table_actionContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Table_column_definitionContext;
import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.VexContext;
import cz.startnet.utils.pgdiff.schema.AbstractConstraint;
import cz.startnet.utils.pgdiff.schema.AbstractIndex;
import cz.startnet.utils.pgdiff.schema.AbstractPgTable;
import cz.startnet.utils.pgdiff.schema.AbstractRegularTable;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.AbstractTable;
import cz.startnet.utils.pgdiff.schema.IRelation;
import cz.startnet.utils.pgdiff.schema.PgColumn;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgObjLocation;
import cz.startnet.utils.pgdiff.schema.PgRule;
import cz.startnet.utils.pgdiff.schema.PgSequence;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.StatementActions;
import ru.taximaxim.codekeeper.apgdiff.model.difftree.DbObjType;

public class AlterTable extends TableAbstract {

    private final Alter_table_statementContext ctx;
    public AlterTable(Alter_table_statementContext ctx, PgDatabase db) {
        super(db);
        this.ctx = ctx;
    }

    @Override
    public void parseObject() {
        List<IdentifierContext> ids = ctx.name.identifier();
        AbstractSchema schema = getSchemaSafe(ids);
        IdentifierContext nameCtx = QNameParser.getFirstNameCtx(ids);
        AbstractPgTable tabl = null;

        PgObjLocation loc = addObjReference(ids, DbObjType.TABLE, StatementActions.ALTER);

        for (Table_actionContext tablAction : ctx.table_action()) {
            if (tablAction.column != null && tablAction.DROP() != null) {
                loc.setWarningText(PgObjLocation.DROP_COLUMN);
            } else if (tablAction.datatype != null) {
                loc.setWarningText(PgObjLocation.ALTER_COLUMN_TYPE);
            }

            if (tablAction.owner_to() != null) {
                IRelation r = getSafe(AbstractSchema::getRelation, schema, nameCtx);
                if (r instanceof PgStatement) {
                    fillOwnerTo(tablAction.owner_to(), (PgStatement) r);
                }
                continue;
            }

            // everything else requires a real table, so fail immediately
            tabl = (AbstractPgTable) getSafe(AbstractSchema::getTable, schema, nameCtx);

            if (tablAction.tabl_constraint != null) {
                IdentifierContext conNameCtx = tablAction.tabl_constraint.constraint_name;
                AbstractConstraint con = parseAlterTableConstraint(tablAction,
                        createTableConstraintBlank(tablAction.tabl_constraint), db,
                        getSchemaNameSafe(ids), nameCtx.getText());

                if (!con.getName().isEmpty()) {
                    addSafe(AbstractPgTable::addConstraint, tabl, con, Arrays.asList(
                            QNameParser.getSchemaNameCtx(ids), nameCtx, conNameCtx));
                } else {
                    doSafe(AbstractPgTable::addConstraint, tabl, con);
                }
            }


            if (tablAction.drop_constraint() != null) {
                addObjReference(Arrays.asList(QNameParser.getSchemaNameCtx(ids), nameCtx,
                        tablAction.drop_constraint().constraint_name),
                        DbObjType.CONSTRAINT, StatementActions.DROP);
            }

            if (isRefMode()) {
                continue;
            }

            if (tablAction.table_column_definition() != null) {
                Table_column_definitionContext column = tablAction.table_column_definition();
                addColumn(column.column_name.getText(), column.datatype,
                        column.collate_name, column.colmn_constraint, tabl);
            }

            if (tablAction.column != null) {
                PgColumn col;
                if (tabl.getInherits().isEmpty()) {
                    col = (PgColumn) getSafe(AbstractTable::getColumn, tabl,
                            QNameParser.getFirstNameCtx(tablAction.column.identifier()));
                } else {
                    String colName = QNameParser.getFirstName(tablAction.column.identifier());
                    col = (PgColumn) tabl.getColumn(colName);
                    if (col == null) {
                        col = new PgColumn(colName);
                        col.setInherit(true);
                        tabl.addColumn(col);
                    }
                }

                // column statistics
                if (tablAction.STATISTICS() != null) {
                    col.setStatistics(Integer.valueOf(tablAction.integer.getText()));
                }

                // column not null constraint
                if (tablAction.set != null) {
                    col.setNullValue(false);
                }

                // column default
                if (tablAction.set_def_column() != null) {
                    VexContext exp = tablAction.set_def_column().expression;
                    col.setDefaultValue(getFullCtxText(exp));
                    db.addContextForAnalyze(col, exp);
                }

                // column options
                if (tablAction.set_attribute_option() != null){
                    for (Storage_parameter_optionContext option :
                        tablAction.set_attribute_option().storage_parameter().storage_parameter_option()){
                        String value = option.value == null ? "" : option.value.getText();
                        fillOptionParams(value, option.storage_param.getText(), false, col::addOption);
                    }
                }

                // foreign options
                if (tablAction.define_foreign_options() != null) {
                    for (Foreign_optionContext option : tablAction.define_foreign_options().foreign_option()) {
                        String value = option.value == null ? "" : option.value.getText();
                        fillOptionParams(value, option.name.getText(), false, col::addForeignOption);
                    }
                }

                // column storage
                if (tablAction.set_storage() != null){
                    col.setStorage(tablAction.set_storage().storage_option().getText());
                }

                // since 10 PostgreSQL
                Identity_bodyContext identity = tablAction.identity_body();
                if (identity != null) {
                    String name = null;
                    for (Sequence_bodyContext body : identity.sequence_body()) {
                        if (body.NAME() != null) {
                            name = body.name.getText();
                        }
                    }
                    PgSequence sequence = new PgSequence(name);
                    CreateSequence.fillSequence(sequence, identity.sequence_body());

                    col.setSequence(sequence);
                    sequence.setParent(schema);
                    col.setIdentityType(identity.ALWAYS() != null ? "ALWAYS" : "BY DEFAULT");
                }
            }

            if (tablAction.index_name != null) {
                IdentifierContext indexName = QNameParser.getFirstNameCtx(tablAction.index_name.identifier());
                AbstractIndex index = getSafe(AbstractTable::getIndex, tabl, indexName);
                index.setClusterIndex(true);
            }

            if (tablAction.WITHOUT() != null && tablAction.OIDS() != null) {
                tabl.setHasOids(false);
            } else if (tablAction.WITH() != null && tablAction.OIDS() != null) {
                tabl.setHasOids(true);
            }

            if (tablAction.RULE() != null) {
                createRule(tabl, tablAction);
            }

            if (tablAction.SECURITY() != null && tabl instanceof AbstractRegularTable) {
                AbstractRegularTable regTable = (AbstractRegularTable) tabl;
                // since 9.5 PostgreSQL
                if (tablAction.FORCE() != null) {
                    regTable.setForceSecurity(tablAction.NO() == null);
                } else {
                    regTable.setRowSecurity(tablAction.ENABLE() != null);
                }
            }
        }
    }

    private void createRule(AbstractPgTable tabl, Table_actionContext tablAction) {
        PgRule rule = getSafe(AbstractTable::getRule, tabl, tablAction.rewrite_rule_name.identifier(0));
        if (rule != null) {
            if (tablAction.DISABLE() != null) {
                rule.setEnabledState("DISABLE");
            } else if (tablAction.ENABLE() != null) {
                if (tablAction.REPLICA() != null) {
                    rule.setEnabledState("ENABLE REPLICA");
                } else if (tablAction.ALWAYS() != null) {
                    rule.setEnabledState("ENABLE ALWAYS");
                }
            }
        }
    }

    public static AbstractConstraint parseAlterTableConstraint(Table_actionContext tableAction,
            AbstractConstraint constrBlank, PgDatabase db, String schemaName, String tableName) {
        constrBlank.setNotValid(tableAction.not_valid != null);
        processTableConstraintBlank(tableAction.tabl_constraint, constrBlank, db,
                schemaName, tableName);
        return constrBlank;
    }
}
