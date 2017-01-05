/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package richtercloud.reflection.form.builder.jpa;

import richtercloud.reflection.form.builder.jpa.entities.EntityFMappedByInverse;
import richtercloud.reflection.form.builder.jpa.entities.EntityDMappedBy;
import richtercloud.reflection.form.builder.jpa.entities.EntityF;
import richtercloud.reflection.form.builder.jpa.entities.EntityEMappedBy;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.entities.EntityAMappedBy;
import richtercloud.reflection.form.builder.jpa.entities.EntityB;
import richtercloud.reflection.form.builder.jpa.entities.EntityBMappedBy;
import richtercloud.reflection.form.builder.jpa.entities.EntityEMappedByInverse;
import richtercloud.reflection.form.builder.jpa.entities.EntityBMappedByInverse;
import richtercloud.reflection.form.builder.jpa.entities.EntityCMappedBy;
import richtercloud.reflection.form.builder.jpa.entities.EntityC;
import richtercloud.reflection.form.builder.jpa.entities.EntityE;
import richtercloud.reflection.form.builder.jpa.entities.EntityFMappedBy;
import richtercloud.reflection.form.builder.jpa.entities.EntityAMappedByInverse;
import richtercloud.reflection.form.builder.jpa.entities.EntityD;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.JOptionPane;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.message.handler.ConfirmMessageHandler;
import richtercloud.message.handler.LoggerMessageHandler;
import richtercloud.message.handler.Message;
import richtercloud.message.handler.MessageHandler;
import richtercloud.reflection.form.builder.fieldhandler.FieldUpdateEvent;
import richtercloud.reflection.form.builder.fieldhandler.MappedFieldUpdateEvent;
import richtercloud.reflection.form.builder.jpa.idapplier.GeneratedValueIdApplier;
import richtercloud.reflection.form.builder.jpa.idapplier.IdApplier;
import richtercloud.reflection.form.builder.jpa.storage.DerbyEmbeddedPersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.DerbyEmbeddedPersistenceStorageConf;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.storage.StorageConfValidationException;
import richtercloud.reflection.form.builder.storage.StorageCreationException;
import richtercloud.reflection.form.builder.storage.StorageException;

/**
 * Shows that {@link JPAReflectionFormBuilder} handles setting of mapped fields
 * when specified in {@code reflection-form-builder} (by using
 * {@link MappedFieldUpdateEvent}) regardless of whether the
 * JPA-{@code mappedBy} attribute is used on relationship definitions.
 *
 * This feature allows users to request bidirectional storage regardless of the
 * configured database scheme.
 *
 * @author richter
 */
public class JPAReflectionFormBuilderIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(JPAReflectionFormBuilderIT.class);

    @Test
    public void testOnFieldUpdate() throws IOException, StorageCreationException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException, StorageConfValidationException, StorageException, SQLException {
        Set<Class<?>> entityClasses = new HashSet<Class<?>>(Arrays.asList(EntityA.class,
                EntityB.class,
                EntityC.class,
                EntityD.class,
                EntityE.class,
                EntityF.class));
        File databaseDir = File.createTempFile(JPAReflectionFormBuilderIT.class.getSimpleName(), null);
        FileUtils.forceDelete(databaseDir);
        //databaseDir mustn't exist for Apache Derby
        String databaseName = databaseDir.getAbsolutePath();
        LOGGER.debug(String.format("database directory: %s", databaseName));
        Connection connection = DriverManager.getConnection(String.format("jdbc:derby:%s;create=true", databaseDir.getAbsolutePath()));
        connection.close();
        File schemeChecksumFile = File.createTempFile(JPAReflectionFormBuilderIT.class.getSimpleName(), null);
        DerbyEmbeddedPersistenceStorageConf storageConf = new DerbyEmbeddedPersistenceStorageConf(entityClasses, databaseName, schemeChecksumFile);
        String persistenceUnitName = "reflection-form-builder-it";
        JPAFieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        PersistenceStorage storage = new DerbyEmbeddedPersistenceStorage(storageConf,
                persistenceUnitName,
                1, //parallelQueryCount
                fieldRetriever);
        storage.start();
        MessageHandler messageHandler = new LoggerMessageHandler(LOGGER);
        ConfirmMessageHandler confirmMessageHandler = new ConfirmMessageHandler() {
            @Override
            public int confirm(Message message) {
                return JOptionPane.YES_OPTION;//confirm everything
            }

            @Override
            public String confirm(Message message, String... options) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        };
        IdApplier idApplier = new GeneratedValueIdApplier();
        JPAReflectionFormBuilder instance = new JPAReflectionFormBuilder(storage,
                "dialog title",
                messageHandler,
                confirmMessageHandler,
                fieldRetriever,
                idApplier,
                new HashMap<Class<?>, WarningHandler<?>>() //warningHandlers
        );

        //general test scenario:
        //1. persist entities which are set on relationship properties (set or
        //added to collection)
        //2. run instance.onFieldUpdate which should set the relationship values
        //on the test entity
        //3. persist the test entity
        //without mappedBy
        //test many-to-many
        EntityA entityA1 = new EntityA(1L);
        EntityB entityB1 = new EntityB(2L);
        EntityB entityB2 = new EntityB(3L);
        storage.store(entityB1);
        storage.store(entityB2);
        FieldUpdateEvent event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityB1, entityB2)),
                EntityB.class.getDeclaredField("as") //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        Field field = EntityA.class.getDeclaredField("bs");
        instance.onFieldUpdate(event, field, entityA1);
        storage.store(entityA1); //store all entities
        EntityA entityA1Stored = storage.retrieve(entityA1.getId(),
                EntityA.class);
        EntityB entityB1Stored = storage.retrieve(entityB1.getId(),
                EntityB.class);
        EntityB entityB2Stored = storage.retrieve(entityB2.getId(),
                EntityB.class);
        assertTrue(entityA1Stored.getBs().contains(entityB1));
        assertTrue(entityA1Stored.getBs().contains(entityB2));
        assertTrue(entityB1Stored.getAs().contains(entityA1));
        assertTrue(entityB2Stored.getAs().contains(entityA1));
        //test one-to-many
        EntityC entityC1 = new EntityC(10L);
        EntityD entityD1 = new EntityD(11L);
        EntityD entityD2 = new EntityD(12L);
        storage.store(entityD1);
        storage.store(entityD2);
        field = EntityC.class.getDeclaredField("ds");
        event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityD1, entityD2)),
                EntityD.class.getDeclaredField("c") //mappedField
        );
        instance.onFieldUpdate(event, field, entityC1);
        storage.store(entityC1); //only store entityC1
        EntityC entityC1Stored = storage.retrieve(entityC1.getId(),
                EntityC.class);
        EntityD entityD1Stored = storage.retrieve(entityD1.getId(),
                EntityD.class);
        assertTrue(entityC1Stored.getDs().contains(entityD1));
        assertTrue(entityC1Stored.getDs().contains(entityD2));
        assertTrue(entityD1Stored.getC().equals(entityC1));
        //test one-to-one
        EntityE entityE1 = new EntityE(20L);
        EntityF entityF1 = new EntityF(21L);
        storage.store(entityF1);
        field = EntityE.class.getDeclaredField("f");
        event = new MappedFieldUpdateEvent(entityF1,
                EntityF.class.getDeclaredField("e") //mappedField
        );
        instance.onFieldUpdate(event, field, entityE1);
        storage.store(entityE1); //only store entityE1
        EntityE entityE1Stored = storage.retrieve(entityE1.getId(),
                EntityE.class);
        EntityF entityF1Stored = storage.retrieve(entityF1.getId(),
                EntityF.class);
        assertTrue(entityE1Stored.getF().equals(entityF1));
        assertTrue(entityF1Stored.getE().equals(entityE1));
        //test many-to-one (reuse EntityC and EntityD, but inverse)
        EntityD entityD10 = new EntityD(30L);
        EntityC entityC10 = new EntityC(31L);
        storage.store(entityC10);
        field = EntityD.class.getDeclaredField("c");
        event = new MappedFieldUpdateEvent(entityC10,
                EntityC.class.getDeclaredField("ds"));
        instance.onFieldUpdate(event, field, entityD10);
        storage.store(entityD10); //only store entityD10
        EntityC entityC10Stored = storage.retrieve(entityC10.getId(),
                EntityC.class);
        EntityD entityD10Stored = storage.retrieve(entityD10.getId(),
                EntityD.class);
        assertTrue(entityD10Stored.getC().equals(entityC10));
        assertTrue(entityC10Stored.getDs().contains(entityD10));

        //with mappedBy (one way)
        //test many-to-many
        EntityAMappedBy entityA1MappedBy = new EntityAMappedBy(1L);
        EntityBMappedBy entityB1MappedBy = new EntityBMappedBy(2L);
        EntityBMappedBy entityB2MappedBy = new EntityBMappedBy(3L);
        storage.store(entityB1MappedBy);
        storage.store(entityB2MappedBy);
        event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityB1MappedBy, entityB2MappedBy)),
                EntityBMappedBy.class.getDeclaredField("as") //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        field = EntityAMappedBy.class.getDeclaredField("bs");
        instance.onFieldUpdate(event, field, entityA1MappedBy);
        storage.store(entityA1MappedBy); //only store entityA1
        EntityAMappedBy entityA1MappedByStored = storage.retrieve(entityA1MappedBy.getId(),
                EntityAMappedBy.class);
        EntityBMappedBy entityB1MappedByStored = storage.retrieve(entityB1MappedBy.getId(),
                EntityBMappedBy.class);
        EntityBMappedBy entityB2MappedByStored = storage.retrieve(entityB2MappedBy.getId(),
                EntityBMappedBy.class);
        assertTrue(entityA1MappedByStored.getBs().contains(entityB1MappedBy));
        assertTrue(entityA1MappedByStored.getBs().contains(entityB2MappedBy));
        assertTrue(entityB1MappedByStored.getAs().contains(entityA1MappedBy));
        assertTrue(entityB2MappedByStored.getAs().contains(entityA1MappedBy));
        //test one-to-many
        EntityCMappedBy entityC1MappedBy = new EntityCMappedBy(10L);
        EntityDMappedBy entityD1MappedBy = new EntityDMappedBy(11L);
        EntityDMappedBy entityD2MappedBy = new EntityDMappedBy(12L);
        storage.store(entityD1MappedBy);
        storage.store(entityD2MappedBy);
        field = EntityCMappedBy.class.getDeclaredField("ds");
        event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityD1MappedBy, entityD2MappedBy)),
                EntityDMappedBy.class.getDeclaredField("c") //mappedField
        );
        instance.onFieldUpdate(event, field, entityC1MappedBy);
        storage.store(entityC1MappedBy); //only store entityC1
        EntityCMappedBy entityC1MappedByStored = storage.retrieve(entityC1MappedBy.getId(),
                EntityCMappedBy.class);
        EntityDMappedBy entityD1MappedByStored = storage.retrieve(entityD1MappedBy.getId(),
                EntityDMappedBy.class);
        assertTrue(entityC1MappedByStored.getDs().contains(entityD1MappedBy));
        assertTrue(entityC1MappedByStored.getDs().contains(entityD2MappedBy));
        assertTrue(entityD1MappedByStored.getC().equals(entityC1MappedBy));
        //test one-to-one
        EntityEMappedBy entityE1MappedBy = new EntityEMappedBy(20L);
        EntityFMappedBy entityF1MappedBy = new EntityFMappedBy(21L);
        storage.store(entityF1MappedBy);
        field = EntityEMappedBy.class.getDeclaredField("f");
        event = new MappedFieldUpdateEvent(entityF1MappedBy,
                EntityFMappedBy.class.getDeclaredField("e") //mappedField
        );
        instance.onFieldUpdate(event, field, entityE1MappedBy);
        storage.store(entityE1MappedBy); //only store entityE1
        EntityEMappedBy entityE1MappedByStored = storage.retrieve(entityE1MappedBy.getId(),
                EntityEMappedBy.class);
        EntityFMappedBy entityF1MappedByStored = storage.retrieve(entityF1MappedBy.getId(),
                EntityFMappedBy.class);
        assertTrue(entityE1MappedByStored.getF().equals(entityF1MappedBy));
        assertTrue(entityF1MappedByStored.getE().equals(entityE1MappedBy));
        //test many-to-one (reuse EntityC and EntityD, but inverse)
        EntityDMappedBy entityD10MappedBy = new EntityDMappedBy(30L);
        EntityCMappedBy entityC10MappedBy = new EntityCMappedBy(31L);
        storage.store(entityC10MappedBy);
        field = EntityDMappedBy.class.getDeclaredField("c");
        event = new MappedFieldUpdateEvent(entityC10MappedBy,
                EntityCMappedBy.class.getDeclaredField("ds"));
        instance.onFieldUpdate(event, field, entityD10MappedBy);
        storage.store(entityD10MappedBy); //only store entityD10
        EntityCMappedBy entityC10MappedByStored = storage.retrieve(entityC10MappedBy.getId(),
                EntityCMappedBy.class);
        EntityDMappedBy entityD10MappedByStored = storage.retrieve(entityD10MappedBy.getId(),
                EntityDMappedBy.class);
        assertTrue(entityD10MappedByStored.getC().equals(entityC10MappedBy));
        assertTrue(entityC10MappedByStored.getDs().contains(entityD10MappedBy));

        //with mappedBy on the side which is not stored (@ManyToOne doesn't have
        //a mappedBy attribute)
        //test many-to-many
        EntityAMappedByInverse entityA1MappedByInverse = new EntityAMappedByInverse(1L);
        EntityBMappedByInverse entityB1MappedByInverse = new EntityBMappedByInverse(2L);
        EntityBMappedByInverse entityB2MappedByInverse = new EntityBMappedByInverse(3L);
        storage.store(entityB1MappedByInverse);
        storage.store(entityB2MappedByInverse);
        event = new MappedFieldUpdateEvent(new LinkedList(Arrays.asList(entityB1MappedByInverse, entityB2MappedByInverse)),
                EntityBMappedByInverse.class.getDeclaredField("as") //mappedField
        );
            //only MappedFieldUpdateEvents are interesting
        field = EntityAMappedByInverse.class.getDeclaredField("bs");
        instance.onFieldUpdate(event, field, entityA1MappedByInverse);
        storage.store(entityA1MappedByInverse); //only store entityA1
        EntityAMappedByInverse entityA1MappedByInverseStored = storage.retrieve(entityA1MappedByInverse.getId(),
                EntityAMappedByInverse.class);
        EntityBMappedByInverse entityB1MappedByInverseStored = storage.retrieve(entityB1MappedByInverse.getId(),
                EntityBMappedByInverse.class);
        EntityBMappedByInverse entityB2MappedByInverseStored = storage.retrieve(entityB2MappedByInverse.getId(),
                EntityBMappedByInverse.class);
        assertTrue(entityA1MappedByInverseStored.getBs().contains(entityB1MappedByInverse));
        assertTrue(entityA1MappedByInverseStored.getBs().contains(entityB2MappedByInverse));
        assertTrue(entityB1MappedByInverseStored.getAs().contains(entityA1MappedByInverse));
        assertTrue(entityB2MappedByInverseStored.getAs().contains(entityA1MappedByInverse));
        //testing one-to-many and many-to-one not necessary because they don't
        //allow to switch the mappedBy attribute because only @OneToMany has it
        //test one-to-one
        EntityEMappedByInverse entityE1MappedByInverse = new EntityEMappedByInverse(20L);
        EntityFMappedByInverse entityF1MappedByInverse = new EntityFMappedByInverse(21L);
        storage.store(entityF1MappedByInverse);
        field = EntityEMappedByInverse.class.getDeclaredField("f");
        event = new MappedFieldUpdateEvent(entityF1MappedByInverse,
                EntityFMappedByInverse.class.getDeclaredField("e") //mappedField
        );
        instance.onFieldUpdate(event, field, entityE1MappedByInverse);
        storage.store(entityE1MappedByInverse); //only store entityE1
        EntityEMappedByInverse entityE1MappedByInverseStored = storage.retrieve(entityE1MappedByInverse.getId(),
                EntityEMappedByInverse.class);
        EntityFMappedByInverse entityF1MappedByInverseStored = storage.retrieve(entityF1MappedByInverse.getId(),
                EntityFMappedByInverse.class);
        assertTrue(entityE1MappedByInverseStored.getF().equals(entityF1MappedByInverse));
        assertTrue(entityF1MappedByInverseStored.getE().equals(entityE1MappedByInverse));
    }
}
