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
package richtercloud.reflection.form.builder.jpa.sequence;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.FieldRetriever;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorageConf;

/**
 *
 * @author richter
 */
public class PostgresqlSequenceManagerIT {
    private final static Logger LOGGER = LoggerFactory.getLogger(PostgresqlSequenceManagerIT.class);

    /**
     * Test of createSequence method, of class PostgresqlSequenceManager.
     */
    @Test
    public void testCreateSequence() throws Exception {
        String sequenceName = "with-minus";
        Set<Class<?>> entityClasses = new HashSet<Class<?>>(Arrays.asList(EntityA.class));
        File databaseDir = File.createTempFile(PostgresqlSequenceManagerIT.class.getSimpleName(), "database");
        LOGGER.debug(String.format("using '%s' as database directory", databaseDir));
        FileUtils.forceDelete(databaseDir);
        File schemeChecksumFile = File.createTempFile(PostgresqlSequenceManagerIT.class.getSimpleName(), "checksum");
        String username = "reflection-form-builder";
        String password = username;
        String databaseName = "reflection-form-builder";
        PostgresqlAutoPersistenceStorageConf storageConf = new PostgresqlAutoPersistenceStorageConf(entityClasses,
                username,
                schemeChecksumFile,
                databaseDir.getAbsolutePath() //databaseDir
        );
        storageConf.setDatabaseName(databaseName);
        storageConf.setPassword(password);
        String persistenceUnitName = "reflection-form-builder-it";
        FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
        PersistenceStorage<Long> storage = new PostgresqlAutoPersistenceStorage(storageConf,
                persistenceUnitName,
                10, //parallelQueryCount
                fieldRetriever);
        storage.start();
        PostgresqlSequenceManager instance = new PostgresqlSequenceManager(storage);
        try {
            instance.createSequence(sequenceName);
            long nextSequenceValue = instance.getNextSequenceValue(sequenceName);
            assertEquals(1L, nextSequenceValue);
        }finally {
            storage.shutdown();
        }
    }
}
