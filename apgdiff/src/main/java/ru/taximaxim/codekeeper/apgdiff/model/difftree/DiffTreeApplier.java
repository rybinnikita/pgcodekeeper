package ru.taximaxim.codekeeper.apgdiff.model.difftree;

import java.util.ArrayList;
import java.util.List;

import ru.taximaxim.codekeeper.apgdiff.model.difftree.TreeElement.DiffSide;
import cz.startnet.utils.pgdiff.schema.PgConstraint;
import cz.startnet.utils.pgdiff.schema.PgDatabase;
import cz.startnet.utils.pgdiff.schema.PgDomain;
import cz.startnet.utils.pgdiff.schema.PgExtension;
import cz.startnet.utils.pgdiff.schema.PgFunction;
import cz.startnet.utils.pgdiff.schema.PgIndex;
import cz.startnet.utils.pgdiff.schema.PgSchema;
import cz.startnet.utils.pgdiff.schema.PgSequence;
import cz.startnet.utils.pgdiff.schema.PgStatement;
import cz.startnet.utils.pgdiff.schema.PgTable;
import cz.startnet.utils.pgdiff.schema.PgTrigger;
import cz.startnet.utils.pgdiff.schema.PgType;
import cz.startnet.utils.pgdiff.schema.PgView;

/**
 * Copies source DB into a new one updating, adding (from target DB) and
 * removing entries as per diff tree.
 * 
 * LEFT elements are not copied (removed), RIGHT & BOTH elements are copied from
 * target DB (updated/added).
 * 
 * сливает 2 БД в новую на основании дерева диффа (и селекшенов в нем): дропает
 * LEFT элементы, копирует новое состояние для RIGHT и BOTH
 */
public class DiffTreeApplier {

    private final PgDatabase dbSource, dbTarget;
    /**
     * Tree representation of user selected changes
     */
    private final TreeElement root;

    private List<PgStatement> lstNoCopy = new ArrayList<>();
    
    public DiffTreeApplier(PgDatabase dbSource, PgDatabase dbTarget, TreeElement root) {
        this.dbSource = dbSource;
        this.dbTarget = dbTarget;
        this.root = root;
    }
    
    public PgDatabase apply() {
        // fill the no copy with all selected LEFT statements 
        noCopyLeft(root);
        
        // first get modified & added elements by filtering target DB with our selection
        PgDatabase dbNew = new PgDbFilter2(dbTarget, root, DiffSide.RIGHT).apply();
        
        // now copy everything from source that isn't already in the new DB
        // and check if statement is in no-copy list (selected for removal)
        
        for(PgExtension ext : dbSource.getExtensions()) {
            if(dbNew.getExtension(ext.getName()) == null && !isNoCopy(ext)) {
                dbNew.addExtension(ext.shallowCopy());
            }
        }
        
        for(PgSchema schema : dbSource.getSchemas()) {
            if(isNoCopy(schema)) {
                continue;
            }
            
            PgSchema dstSchema = dbNew.getSchema(schema.getName());
            if(dstSchema == null) {
                dstSchema = schema.shallowCopy();
                dbNew.addSchema(dstSchema);
            } else {
                dbNew.tryReplacePublicDef(schema);
            }
            
            for(PgFunction func : schema.getFunctions()) {
                if(!dstSchema.containsFunction(func.getSignature()) && !isNoCopy(func)) {
                    dstSchema.addFunction(func.shallowCopy());
                }
            }
            for(PgSequence seq : schema.getSequences()) {
                if(!dstSchema.containsSequence(seq.getName()) && !isNoCopy(seq)) {
                    dstSchema.addSequence(seq.shallowCopy());
                }
            }
            for (PgType type : schema.getTypes()) {
                if(!dstSchema.containsType(type.getName()) && !isNoCopy(type)) {
                    dstSchema.addType(type.shallowCopy());
                }
            }
            for (PgDomain domain : schema.getDomains()) {
                if(!dstSchema.containsDomain(domain.getName()) && !isNoCopy(domain)) {
                    dstSchema.addDomain(domain.shallowCopy());
                }
            }
            for(PgView view : schema.getViews()) {
                if(!dstSchema.containsView(view.getName()) && !isNoCopy(view)) {
                    dstSchema.addView(view.shallowCopy());
                }
            }
            for(PgTable table : schema.getTables()) {
                if(isNoCopy(table)) {
                    continue;
                }
                
                PgTable dstTable = dstSchema.getTable(table.getName());
                if(dstTable == null) {
                    dstTable = table.shallowCopy();
                    dstSchema.addTable(dstTable);
                }
                
                for(PgIndex idx : table.getIndexes()) {
                    if(!dstTable.containsIndex(idx.getName()) && !isNoCopy(idx)) {
                        dstTable.addIndex(idx.shallowCopy());
                    }
                }
                for(PgTrigger trigger : table.getTriggers()) {
                    if(!dstTable.containsTrigger(trigger.getName()) && !isNoCopy(trigger)) {
                        dstTable.addTrigger(trigger.shallowCopy());
                    }
                }
                for(PgConstraint constr : table.getConstraints()) {
                    if(!dstTable.containsConstraint(constr.getName()) && !isNoCopy(constr)) {
                        dstTable.addConstraint(constr.shallowCopy());
                    }
                }
            }
        }
        
        return dbNew;
    }
    
    private boolean isNoCopy(PgStatement statement) {
        for(PgStatement e : lstNoCopy) {
            if(e == statement) {
                return true;
            }
        }
        
        return false;
    }
    
    private void noCopyLeft(TreeElement el) {
        // skip RIGHTs
        if(el.getSide() == DiffSide.RIGHT) {
            return;
        }
        
        // add LEFT statements to no-copy list
        if(el.getSide() == DiffSide.LEFT
                && el.isSelected()) {
            lstNoCopy.add(el.getPgStatement(dbSource));
            return;
        }
        
        // recurse through BOTH statements and LEFT containers
        for(TreeElement sub : el.getChildren()) {
            noCopyLeft(sub);
        }
    }
}