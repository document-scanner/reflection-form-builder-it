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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import richtercloud.reflection.form.builder.jpa.JPACachedFieldRetriever;
import richtercloud.reflection.form.builder.jpa.entities.EntityA;
import richtercloud.reflection.form.builder.jpa.storage.PersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorage;
import richtercloud.reflection.form.builder.jpa.storage.PostgresqlAutoPersistenceStorageConf;
import richtercloud.validation.tools.FieldRetriever;

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
        PersistenceStorage<Long> storage = null;
        try {
            String sequenceName = "with-minus";
            Set<Class<?>> entityClasses = new HashSet<Class<?>>(Arrays.asList(EntityA.class));
            File databaseDir = Files.createTempDirectory(PostgresqlSequenceManagerIT.class.getSimpleName()).toFile();
            LOGGER.debug(String.format("using '%s' as database directory", databaseDir));
            FileUtils.forceDelete(databaseDir);
            File schemeChecksumFile = File.createTempFile(PostgresqlSequenceManagerIT.class.getSimpleName(), "checksum");
            String username = "reflection-form-builder";
            String password = username;
            String databaseName = "reflection-form-builder";
            Pair<String, String> bestPostgresqlBaseDir = PostgresqlAutoPersistenceStorageConf.findBestInitialPostgresqlBasePath();
                //@TODO: add discovery for other OS and allow specification as system property
            if(bestPostgresqlBaseDir == null) {
                throw new IllegalArgumentException("no PostgreSQL initdb binary could be found (currently only Debian-based systems with PostgreSQL binaries in /usr/lib/postgresql/[version] are supported.");
            }
            PostgresqlAutoPersistenceStorageConf storageConf = new PostgresqlAutoPersistenceStorageConf(entityClasses,
                    "localhost",
                    username,
                    password,
                    databaseName,
                    schemeChecksumFile,
                    databaseDir.getAbsolutePath(), //databaseDir
                    bestPostgresqlBaseDir.getKey(),
                    bestPostgresqlBaseDir.getValue(),
                    "createdb" //createdbBinaryPath
            );
            String persistenceUnitName = "reflection-form-builder-it";
            FieldRetriever fieldRetriever = new JPACachedFieldRetriever();
            storage = new PostgresqlAutoPersistenceStorage(storageConf,
                    persistenceUnitName,
                    10, //parallelQueryCount
                    fieldRetriever);
            storage.start();
            PostgresqlSequenceManager instance = new PostgresqlSequenceManager(storage);
            instance.createSequence(sequenceName);
            long nextSequenceValue = instance.getNextSequenceValue(sequenceName);
            assertEquals(1L, nextSequenceValue);
        }finally {
            if(storage != null) {
                storage.shutdown();
            }
        }
    }
}
