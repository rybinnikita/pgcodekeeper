/**
 * Copyright 2006 StartNet s.r.o.
 *
 * Distributed under MIT license
 */
package cz.startnet.utils.pgdiff.loader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import cz.startnet.utils.pgdiff.PgDiffArguments;
import cz.startnet.utils.pgdiff.schema.AbstractColumn;
import cz.startnet.utils.pgdiff.schema.AbstractConstraint;
import cz.startnet.utils.pgdiff.schema.AbstractIndex;
import cz.startnet.utils.pgdiff.schema.AbstractSchema;
import cz.startnet.utils.pgdiff.schema.AbstractSequence;
import cz.startnet.utils.pgdiff.schema.AbstractTrigger.TgTypes;
import cz.startnet.utils.pgdiff.schema.Argument;
import cz.startnet.utils.pgdiff.schema.MsColumn;
import cz.startnet.utils.pgdiff.schema.MsConstraint;
import cz.startnet.utils.pgdiff.schema.MsFunction;
import cz.startnet.utils.pgdiff.schema.MsIndex;
import cz.startnet.utils.pgdiff.schema.MsSchema;
import cz.startnet.utils.pgdiff.schema.MsSequence;
import cz.startnet.utils.pgdiff.schema.MsTrigger;
import cz.startnet.utils.pgdiff.schema.MsView;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgPrivilege;
import cz.startnet.utils.pgdiff.schema.SimpleMsTable;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffConsts;
import ru.taximaxim.codekeeper.apgdiff.ApgdiffTestUtils;
import ru.taximaxim.codekeeper.apgdiff.model.exporter.ModelExporter;

/**
 * An abstract 'factory' that creates 'artificial'
 * PgDatabase MSSQL-objects for specific test-cases.
 *
 * @author Alexander Levsha
 */
abstract class MsDatabaseObjectCreator {

    /**
     * The method makes up a PgDatabase object specific to the test needs.
     */
    public abstract PgDatabase getDatabase();
}

/**
 * Tests for PgDiffLoader class.
 *
 * @author fordfrog
 */
@RunWith(value = Parameterized.class)
public class MsAntlrLoaderTest {

    private final String encoding = ApgdiffConsts.UTF_8;
    /**
     * Provides parameters for running the tests.
     *
     * @return parameters for the tests
     */
    @Parameters
    public static Collection<?> parameters() {
        return Arrays.asList(
                new Object[][]{
                    // SONAR-OFF
                    {0},
                    {1},
                    {2},
                    {3},
                    {4},
                    {5},
                    {6},
                    {7},
                    {8}
                    // SONAR-ON
                });
    }
    /**
     * Index of the file that should be tested.
     */
    private final int fileIndex;

    /**
     * Array of implementations of {@link MsDatabaseObjectCreator}
     * each returning a specific {@link PgDatabase} for a test-case.
     */
    private static final MsDatabaseObjectCreator[] DB_OBJS = {
            new MsDB0(),
            new MsDB1(),
            new MsDB2(),
            new MsDB3(),
            new MsDB4(),
            new MsDB5(),
            new MsDB6(),
            new MsDB7(),
            new MsDB8()
    };

    /**
     * Creates a new instance of PgDumpLoaderTest.
     *
     * @param fileIndex {@link #fileIndex}
     */
    public MsAntlrLoaderTest(final int fileIndex) {
        this.fileIndex = fileIndex;
    }

    @Test
    public void loadSchema() throws InterruptedException, IOException {

        // first test the dump loader itself
        String filename = "ms_schema_" + fileIndex + ".sql";
        PgDiffArguments args = new PgDiffArguments();
        args.setInCharsetName(encoding);
        args.setKeepNewlines(true);
        args.setMsSql(true);
        PgDatabase d = ApgdiffTestUtils.loadTestDump(
                filename, MsAntlrLoaderTest.class, args);

        // then check result's validity against handmade DB object
        if(fileIndex > DB_OBJS.length) {
            Assert.fail("No predefined object for file: " + filename);
        }

        PgDatabase dbPredefined = DB_OBJS[fileIndex].getDatabase();

        Assert.assertEquals("PgDumpLoader: predefined object is not equal to file "
                + filename, dbPredefined, d);

        // test deepCopy mechanism
        Assert.assertEquals("PgStatement deep copy altered", d, d.deepCopy());
        Assert.assertEquals("PgStatement deep copy altered original", dbPredefined, d);
    }

    /**
     * Tests ModelExporter exportFull() method
     * @throws InterruptedException
     */
    @Test
    public void exportFullDb() throws IOException, InterruptedException {
        // prepare db object from sql file
        String filename = "ms_schema_" + fileIndex + ".sql";
        PgDiffArguments args = new PgDiffArguments();
        args.setInCharsetName(encoding);
        args.setKeepNewlines(true);
        args.setMsSql(true);
        PgDatabase dbFromFile = ApgdiffTestUtils.loadTestDump(
                filename, MsAntlrLoaderTest.class, args);

        PgDatabase dbPredefined = DB_OBJS[fileIndex].getDatabase();
        Path exportDir = null;
        try{
            exportDir = Files.createTempDirectory("pgCodekeeper-test-files");
            new ModelExporter(exportDir.toFile(), dbPredefined, encoding).exportFull();

            args = new PgDiffArguments();
            args.setInCharsetName(encoding);
            args.setKeepNewlines(true);
            args.setMsSql(true);
            PgDatabase dbAfterExport = PgDumpLoader.loadDatabaseSchemaFromDirTree(
                    exportDir.toString(), args, null, null);

            // check the same db similarity before and after export
            Assert.assertEquals("ModelExporter: predefined object PgDB" + fileIndex +
                    " is not equal to exported'n'loaded.", dbPredefined, dbAfterExport);

            Assert.assertEquals("ModelExporter: exported predefined object is not "
                    + "equal to file " + filename, dbAfterExport, dbFromFile);
        }finally{
            if (exportDir != null){
                deleteRecursive(exportDir.toFile());
            }
        }
    }

    /**
     * Deletes folder and its contents recursively. FOLLOWS SYMLINKS!
     */
    private static void deleteRecursive(File f) throws IOException {
        if (f.isDirectory()) {
            for (File sub : f.listFiles()) {
                deleteRecursive(sub);
            }
        }
        Files.delete(f.toPath());
    }
}

// SONAR-OFF

class MsDB0 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        SimpleMsTable table = new SimpleMsTable("fax_boxes", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        AbstractColumn col = new MsColumn("fax_box_id");
        col.setType("[int]");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("name");
        col.setType("[text]");
        table.addColumn(col);

        AbstractConstraint constraint = new MsConstraint("PK_fax_boxes", "");
        table.addConstraint(constraint);
        constraint.setDefinition("PRIMARY KEY CLUSTERED  ([fax_box_id]) ON [PRIMARY]");

        table.setOwner("ms_user");

        table = new SimpleMsTable("faxes", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        col = new MsColumn("fax_id");
        col.setType("[int]");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("fax_box_id");
        col.setType("[int]");
        table.addColumn(col);

        col = new MsColumn("from_name");
        col.setType("[text]");
        table.addColumn(col);

        col = new MsColumn("from_number");
        col.setType("[text]");
        table.addColumn(col);

        col = new MsColumn("status");
        col.setType("[int]");
        table.addColumn(col);

        col = new MsColumn("pages");
        col.setType("[int]");
        table.addColumn(col);

        col = new MsColumn("time_received");
        col.setType("[datetime]");
        // TODO replace constraint 'constraint = new MsConstraint("DF_faxes_time_received", "")' by this,
        // when default value setting will be fixed
        // TODO fix default value setting; at this moment trying to set default value gives us:
        //    ALTER TABLE [dbo].[faxes] ALTER COLUMN [time_received] DROP CONSTRAINT null
        //    ALTER TABLE [dbo].[faxes] ADD CONSTRAINT [DF_faxes_time_received] DEFAULT (getdate()) FOR time_received
        // col.setDefaultValue("getdate()");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("getdate()")' when it will be fixed
        constraint = new MsConstraint("DF_faxes_time_received", "");
        constraint.setDefinition("DEFAULT (getdate()) FOR time_received");
        table.addConstraint(constraint);

        col = new MsColumn("time_finished_received");
        col.setType("[datetime]");
        table.addColumn(col);

        col = new MsColumn("read");
        col.setType("[int]");
        // TODO replace constraint 'constraint = new MsConstraint("DF_faxes_read", "")' by this,
        // when default value setting will be fixed
        // TODO fix default value setting; at this moment trying to set default value gives us:
        //    ALTER TABLE [dbo].[faxes] ALTER COLUMN [read] DROP CONSTRAINT null
        //    ALTER TABLE [dbo].[faxes] ADD CONSTRAINT [DF_faxes_read] DEFAULT 0 FOR [read]
        // col.setDefaultValue("0");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("0")' when it will be fixed
        constraint = new MsConstraint("DF_faxes_read", "");
        constraint.setDefinition("DEFAULT 0 FOR [read]");
        table.addConstraint(constraint);

        col = new MsColumn("station_id");
        col.setType("[text]");
        table.addColumn(col);

        constraint = new MsConstraint("PK_faxes", "");
        constraint.setDefinition("PRIMARY KEY CLUSTERED  ([fax_id]) ON [PRIMARY]");
        table.addConstraint(constraint);

        constraint = new MsConstraint("FK_faxes_fax_box_id", "");
        constraint.setDefinition("FOREIGN KEY (fax_box_id) \n" +
                "    REFERENCES [dbo].[fax_boxes](fax_box_id) ON DELETE SET NULL ON UPDATE CASCADE");
        table.addConstraint(constraint);

        table = new SimpleMsTable("extensions", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        col = new MsColumn("id");
        col.setType("[int]");
        col.setNullValue(false);
        table.addColumn(col);

        constraint = new MsConstraint("FK_extensions_fax_box_id", "");
        constraint.setDefinition("FOREIGN KEY (fax_box_id) \n" +
                "    REFERENCES [dbo].[fax_boxes](fax_box_id) ON DELETE SET NULL ON UPDATE CASCADE");
        table.addConstraint(constraint);

        return d;
    }
}

class MsDB1 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();

        AbstractSchema schema = new MsSchema("msschema", "");
        d.addSchema(schema);

        schema = d.getSchema(ApgdiffConsts.DBO);

        SimpleMsTable table = new SimpleMsTable("contacts", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        AbstractColumn col = new MsColumn("id");
        col.setType("[int]");
        table.addColumn(col);

        col = new MsColumn("number_pool_id");
        col.setType("[int]");
        table.addColumn(col);

        col = new MsColumn("name");
        col.setType("[varchar](50)");
        table.addColumn(col);

        AbstractIndex idx = new MsIndex("IX_number_pool_id", "");
        table.addIndex(idx);
        idx.setTableName("contacts");
        idx.setDefinition("([number_pool_id])");

        return d;
    }
}

class MsDB2 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractSequence seq = new MsSequence("admins_aid_seq", "");
        seq.setStartWith("1");
        seq.setMinMaxInc(1L, 1000000000L, null, null);
        seq.setCached(true);
        seq.setCache("1");
        schema.addSequence(seq);

        SimpleMsTable table = new SimpleMsTable("admins", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        AbstractColumn col = new MsColumn("aid");
        col.setType("[int]");
        col.setNullValue(false);
        // TODO replace constraint 'constraint = new MsConstraint("DF_admins_aid", "")' by this,
        // when default value setting will be fixed
        // TODO fix default value setting; at this moment trying to set default value gives us:
        //    ALTER TABLE [dbo].[admins] ALTER COLUMN [aid] DROP CONSTRAINT null
        //    ALTER TABLE [dbo].[admins] ADD CONSTRAINT [DF_admins_aid] DEFAULT (NEXT VALUE FOR [dbo].[admins_aid_seq]) FOR aid
        // col.setDefaultValue("(NEXT VALUE FOR [dbo].[admins_aid_seq])");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("(NEXT VALUE FOR [dbo].[admins_aid_seq])")' when it will be fixed
        AbstractConstraint constraint = new MsConstraint("DF_admins_aid", "");
        constraint.setDefinition("DEFAULT (NEXT VALUE FOR [dbo].[admins_aid_seq]) FOR aid");
        table.addConstraint(constraint);

        constraint = new MsConstraint("PK_admins", "");
        constraint.setDefinition("PRIMARY KEY CLUSTERED  ([aid]) ON [PRIMARY]");
        table.addConstraint(constraint);

        col = new MsColumn("companyid");
        col.setType("[int]");
        // col.setDefaultValue("0");
        col.setNullValue(false);
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("0")' when it will be fixed
        constraint = new MsConstraint("DF_admins_companyid", "");
        constraint.setDefinition("DEFAULT 0 FOR companyid");
        table.addConstraint(constraint);

        col = new MsColumn("groupid");
        col.setType("[int]");
        // col.setDefaultValue("0");
        col.setNullValue(false);
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("0")' when it will be fixed
        constraint = new MsConstraint("DF_admins_groupid", "");
        constraint.setDefinition("DEFAULT 0 FOR groupid");
        table.addConstraint(constraint);

        col = new MsColumn("username");
        col.setType("[nvarchar](max)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("password");
        col.setType("[nvarchar](40)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("superuser");
        col.setType("[bit]");
        // col.setDefaultValue("0");
        col.setNullValue(false);
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("0")' when it will be fixed
        constraint = new MsConstraint("DF_admins_superuser", "");
        constraint.setDefinition("DEFAULT 0 FOR superuser");
        table.addConstraint(constraint);

        col = new MsColumn("name");
        col.setType("[nvarchar](40)");
        table.addColumn(col);

        col = new MsColumn("surname");
        col.setType("[nvarchar](40)");
        table.addColumn(col);

        col = new MsColumn("email");
        col.setType("[nvarchar](100)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("tel");
        col.setType("[nvarchar](40)");
        table.addColumn(col);

        col = new MsColumn("mobile");
        col.setType("[nvarchar](40)");
        table.addColumn(col);

        col = new MsColumn("enabled");
        col.setType("[bit]");
        // col.setDefaultValue("1");
        col.setNullValue(false);
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("1")' when it will be fixed
        constraint = new MsConstraint("DF_admins_enabled", "");
        constraint.setDefinition("DEFAULT 1 FOR enabled");
        table.addConstraint(constraint);

        col = new MsColumn("lastlogints");
        col.setType("[datetimeoffset]");
        // col.setDefaultValue("getdate()");
        col.setNullValue(false);
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("getdate()")' when it will be fixed
        constraint = new MsConstraint("DF_admins_lastlogints", "");
        constraint.setDefinition("DEFAULT (getdate()) FOR lastlogints");
        table.addConstraint(constraint);

        col = new MsColumn("expirienced");
        col.setType("[bit]");
        // col.setDefaultValue("0");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("0")' when it will be fixed
        constraint = new MsConstraint("DF_admins_expirienced", "");
        constraint.setDefinition("DEFAULT 0 FOR expirienced");
        table.addConstraint(constraint);

        return d;
    }

}

class MsDB3 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        AbstractSequence seq = new MsSequence("call_logs_id_seq", "");
        seq.setStartWith("1");
        seq.setMinMaxInc(1L, 1000000000L, null, null);
        seq.setCached(true);
        seq.setCache("1");
        schema.addSequence(seq);

        SimpleMsTable table = new SimpleMsTable("call_logs", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        AbstractColumn col = new MsColumn("id");
        col.setType("[bigint]");
        col.setNullValue(false);
        // TODO replace constraint 'constraint = new MsConstraint("DF_admins_aid", "")' by this,
        // when default value setting will be fixed
        // TODO fix default value setting; at this moment trying to set default value gives us:
        //    ALTER TABLE [dbo].[call_logs] ALTER COLUMN [id] DROP CONSTRAINT null
        //    ALTER TABLE [dbo].[call_logs] ADD CONSTRAINT [DF_call_logs_id] DEFAULT (NEXT VALUE FOR [dbo].[call_logs_id_seq]) FOR id
        // col.setDefaultValue("(NEXT VALUE FOR [dbo].[call_logs_id_seq])");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("(NEXT VALUE FOR [dbo].[call_logs_id_seq])")' when it will be fixed
        AbstractConstraint constraint = new MsConstraint("DF_call_logs_id", "");
        constraint.setDefinition("DEFAULT (NEXT VALUE FOR [dbo].[call_logs_id_seq]) FOR id");
        table.addConstraint(constraint);

        return d;
    }
}

class MsDB4 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        SimpleMsTable table = new SimpleMsTable("table1", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        AbstractColumn col = new MsColumn("id");
        col.setType("[int]");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("entityId");
        col.setType("[int]");
        col.setNullValue(false);
        table.addColumn(col);

        MsFunction func = new MsFunction("gtsq_in", "");
        func.setAnsiNulls(true);
        func.setQuotedIdentified(true);
        func.setBody("WITH RETURNS NULL ON NULL INPUT\nAS\nBEGIN\n"
                + "    Declare @logid varchar(50);\n"
                + "    SELECT @logid = tbl1.id from [dbo].[table1] AS tbl1\n"
                + "    WHERE tbl1.entityId = @eid\n    RETURN  @logid\nEND");
        func.setReturns("varchar(100)");
        Argument arg = new Argument("@eid", "int");
        func.addArgument(arg);
        schema.addFunction(func);

        func = new MsFunction("multiply_numbers", "");
        func.setAnsiNulls(true);
        func.setQuotedIdentified(true);
        func.setBody("AS\nBEGIN\n    DECLARE @Res integer = 0\n\n"
                + "    SET @Res = @First * @Second\n\n"
                + "    IF @Res < 0\n"
                + "        SET @Res = 0\n\n"
                + "    RETURN @Res\nEND");
        func.setReturns("integer");
        arg = new Argument("@First", "int");
        func.addArgument(arg);
        arg = new Argument("@Second", "int");
        func.addArgument(arg);
        schema.addFunction(func);

        func = new MsFunction("select_something", "");
        func.setAnsiNulls(true);
        func.setQuotedIdentified(true);
        func.setBody("AS\nBEGIN\n    DECLARE @Res integer = 0\n"
                + "    SELECT  @Res = COUNT(*) FROM [dbo].[table1];\n"
                + "    RETURN @Res + @First * @Second\nEND");
        func.setReturns("integer");
        arg = new Argument("@First", "int");
        func.addArgument(arg);
        arg = new Argument("@Second", "int");
        func.addArgument(arg);
        schema.addFunction(func);

        return d;
    }
}

class MsDB5 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        schema.addPrivilege(new PgPrivilege("REVOKE", "SELECT", "SCHEMA::[dbo]", "[ms_user]", false));
        schema.addPrivilege(new PgPrivilege("REVOKE", "UPDATE", "SCHEMA::[dbo]", "[ms_user]", false));
        schema.addPrivilege(new PgPrivilege("REVOKE", "DELETE", "SCHEMA::[dbo]", "[ms_user]", false));
        schema.addPrivilege(new PgPrivilege("REVOKE", "INSERT", "SCHEMA::[dbo]", "[ms_user]", false));

        schema.addPrivilege(new PgPrivilege("GRANT", "SELECT", "SCHEMA::[dbo]", "[ms_user]", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "UPDATE", "SCHEMA::[dbo]", "[ms_user]", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "DELETE", "SCHEMA::[dbo]", "[ms_user]", false));
        schema.addPrivilege(new PgPrivilege("GRANT", "INSERT", "SCHEMA::[dbo]", "[ms_user]", false));

        SimpleMsTable table = new SimpleMsTable("test_table", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        AbstractColumn col = new MsColumn("id");
        col.setType("[bigint]");
        table.addColumn(col);

        col = new MsColumn("date_deleted");
        col.setType("[datetime]");
        table.addColumn(col);

        table.setOwner("ms_user");

        AbstractIndex idx = new MsIndex("IX_date_deleted", "");
        idx.setTableName("test_table");
        idx.setDefinition("([date_deleted])");
        idx.setWhere("(date_deleted IS NULL)");
        table.addIndex(idx);

        return d;
    }
}

class MsDB6 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();

        AbstractSchema schema = new MsSchema("common", "");
        d.addSchema(schema);
        d.setDefaultSchema("common");

        MsFunction func = new MsFunction("t_common_casttotext", "");
        func.setAnsiNulls(true);
        func.setQuotedIdentified(true);
        func.setBody("AS\nBEGIN\n"
                + "    DECLARE @Res varchar(100) = ''\n"
                + "    SELECT  @Res = DATENAME(dw, '09/23/2013')\n"
                + "    RETURN  @Res\nEND");
        func.setReturns("varchar(100)");

        schema.addFunction(func);

        return d;
    }
}

class MsDB7 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        // TODO uncomment and remade this for MsType when it will be supported
        // MsType type = new MsType("testtt", MsTypeForm.COMPOSITE, "");
        // AbstractColumn col = new MsColumn("a");
        // col.setType("[int]");
        // type.addAttr(col);
        // col = new MsColumn("b");
        // col.setType("[text]");
        // type.addAttr(col);
        // type.setOwner("ms_user");
        // schema.addType(type);

        schema = new MsSchema("``54'253-=9!@#$%^&*()__<>?:\"\"{};',./", "");
        d.addSchema(schema);

        MsFunction func = new MsFunction(".x\"\".\"\"\"\".", "");
        func.setAnsiNulls(true);
        func.setQuotedIdentified(true);
        func.setBody("AS\nBEGIN\n" +
                "    DECLARE @Res bit = 0\n\n" +
                "    IF @arg1 > 1\n" +
                "        SET @Res = 1\n\n" +
                "    RETURN @Res\nEND");
        func.setReturns("bit");

        Argument arg = new Argument("@arg1", "int");
        func.addArgument(arg);

        func.setOwner("ms_user");

        schema.addFunction(func);

        return d;
    }
}

class MsDB8 extends MsDatabaseObjectCreator {
    @Override
    public PgDatabase getDatabase() {
        PgDatabase d = ApgdiffTestUtils.createDumpMsDB();
        AbstractSchema schema = d.getDefaultSchema();

        MsSequence seq = new MsSequence("user_id_seq", "");
        seq.setMinMaxInc(1L, null, null, null);
        seq.setCached(true);
        seq.setCache("1");
        seq.setOwner("ms_user");
        schema.addSequence(seq);

        SimpleMsTable table = new SimpleMsTable("user_data", "");
        table.setAnsiNulls(true);
        table.setOwner("ms_user");
        schema.addTable(table);

        AbstractColumn col = new MsColumn("id");
        col.setType("[bigint]");
        col.setNullValue(false);
        // col.setDefaultValue("(NEXT VALUE FOR [dbo].[user_id_seq])");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("(NEXT VALUE FOR [dbo].[user_id_seq])")' when it will be fixed
        AbstractConstraint constraint = new MsConstraint("DF_user_data_id", "");
        constraint.setDefinition("DEFAULT (NEXT VALUE FOR [dbo].[user_id_seq]) FOR id");
        table.addConstraint(constraint);

        col = new MsColumn("email");
        col.setType("[nvarchar](128)");
        col.setNullValue(false);
        table.addColumn(col);

        col = new MsColumn("created");
        col.setType("[datetimeoffset]");
        // col.setDefaultValue("getdate()");
        table.addColumn(col);

        // TODO replace constraint by 'col.setDefaultValue("getdate()")' when it will be fixed
        constraint = new MsConstraint("DF_user_data_created", "");
        constraint.setDefinition("DEFAULT (getdate()) FOR created");
        table.addConstraint(constraint);

        table = new SimpleMsTable("t1", "");
        table.setAnsiNulls(true);
        schema.addTable(table);

        col = new MsColumn("c1");
        col.setType("[int]");
        table.addColumn(col);

        MsView view = new MsView("\"user\"", "");
        view.setAnsiNulls(true);
        view.setQuotedIdentified(true);
        view.setQuery("SELECT [user_data].[id], [user_data].[email], [user_data].[created] FROM [dbo].[user_data]");
        view.setOwner("ms_user");
        schema.addView(view);

        MsTrigger trigger = new MsTrigger("instead_of_delete", "");
        trigger.setQuotedIdentified(true);
        trigger.setAnsiNulls(true);
        trigger.setType(TgTypes.INSTEAD_OF);
        trigger.setOnDelete(true);
        trigger.setTableName("\"user\"");
        trigger.setQuery("BEGIN\n"
                + "        DELETE FROM [dbo].[user_data]\n"
                + "        WHERE id = 10  \n"
                + "    END");
        view.addTrigger(trigger);

        trigger = new MsTrigger("instead_of_insert", "");
        trigger.setQuotedIdentified(true);
        trigger.setAnsiNulls(true);
        trigger.setType(TgTypes.INSTEAD_OF);
        trigger.setOnInsert(true);
        trigger.setTableName("\"user\"");
        trigger.setQuery("BEGIN\n"
                + "        INSERT INTO [dbo].[user_data] (id, email, created)\n"
                + "        VALUES(1, 'test@supermail.loc', getdate())\n"
                + "    END");
        view.addTrigger(trigger);

        trigger = new MsTrigger("instead_of_update", "");
        trigger.setQuotedIdentified(true);
        trigger.setAnsiNulls(true);
        trigger.setType(TgTypes.INSTEAD_OF);
        trigger.setOnUpdate(true);
        trigger.setTableName("\"user\"");
        trigger.setQuery("BEGIN\n"
                + "        UPDATE [dbo].[user_data] \n"
                + "        SET id = 55, email = 'super@supermail.loc'\n"
                + "        WHERE id = 4\n"
                + "    END");
        view.addTrigger(trigger);

        view = new MsView("ws_test", "");
        view.setAnsiNulls(true);
        view.setQuotedIdentified(true);
        view.setQuery("SELECT ud.[id] AS \"   i   d   \" FROM [dbo].[user_data] ud");
        schema.addView(view);

        return d;
    }
}

// SONAR-ON
