package cz.startnet.utils.pgdiff.parsers.antlr.statements;

import java.nio.file.Path;

import cz.startnet.utils.pgdiff.parsers.antlr.SQLParser.Alter_schema_statementContext;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgStatement;

public class AlterSchema extends ParserAbstract {
    private Alter_schema_statementContext ctx;
    public AlterSchema(Alter_schema_statementContext ctx, PgDatabase db, Path filePath) {
        super(db, filePath);
        this.ctx = ctx;
    }

    @Override
    public PgStatement getObject() {
        String name = getName(ctx.schema_with_name().name);
        PgSchema schema = new PgSchema(name, getFullCtxText(ctx.getParent()));
        if (ctx.owner_to() != null) {
            schema.setOwner(removeQuotes(ctx.owner_to().name));
            PgSchema sch = db.getSchema(name);
            if (sch != null) {
                sch.setOwner(removeQuotes(ctx.owner_to().name));
                return null;
            }
            return schema;
        }
        return null;
    }

}
