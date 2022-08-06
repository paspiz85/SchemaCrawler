/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2022, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/
package us.fatehi.utility.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

import us.fatehi.utility.datasource.DatabaseConnectionSource;
import us.fatehi.utility.datasource.PooledConnectionUtility;

@TestInstance(Lifecycle.PER_CLASS)
public class PooledConnectionTest {

  private Connection connection;
  private Map<String, Method> methodsMap;
  private final DatabaseConnectionSource databaseConnectionSource =
      mock(DatabaseConnectionSource.class);

  @BeforeEach
  public void createDatabase() throws Exception {

    final EmbeddedDatabase db =
        new EmbeddedDatabaseBuilder()
            .generateUniqueName(true)
            .setScriptEncoding("UTF-8")
            .ignoreFailedDrops(true)
            .addScript("testdb.sql")
            .build();

    connection = db.getConnection();
  }

  @BeforeAll
  public void methodsMap() throws Exception {
    methodsMap = new HashMap<>();
    for (final Method method : Connection.class.getMethods()) {
      if (method.getParameterCount() > 0) {
        continue;
      }
      methodsMap.put(method.getName(), method);
    }
  }

  @Test
  public void setSavepoint() throws SQLException {
    final Connection pooledConnection =
        PooledConnectionUtility.newPooledConnection(connection, databaseConnectionSource);

    final Method method = methodsMap.get("setSavepoint");
    assertThrows(
        InvocationTargetException.class,
        () -> method.invoke(pooledConnection),
        method.toGenericString());

    assertThrows(SQLException.class, () -> pooledConnection.setSavepoint());
  }

  @Test
  public void testClosedPooledConnection() throws SQLException {
    final Connection pooledConnection =
        PooledConnectionUtility.newPooledConnection(connection, databaseConnectionSource);
    pooledConnection.close();

    for (final Method method : methodsMap.values()) {
      if (method.getParameterCount() == 0) {
        if (Arrays.asList("close", "setSavepoint", "isClosed").contains(method.getName())) {
          continue;
        }
        assertThrows(
            InvocationTargetException.class,
            () -> method.invoke(pooledConnection),
            method.toGenericString());
      }
    }
  }

  @Test
  public void testPooledConnection() throws Exception {
    final Connection pooledConnection =
        PooledConnectionUtility.newPooledConnection(connection, databaseConnectionSource);

    for (final Method method : methodsMap.values()) {
      if (method.getParameterCount() == 0) {
        if (Arrays.asList("close", "setSavepoint").contains(method.getName())) {
          continue;
        }
        // Assert nothing is thrown from method call
        method.invoke(pooledConnection);
      }
    }
  }

  @Test
  public void toStringTest() throws Exception {
    final Connection pooledConnection =
        PooledConnectionUtility.newPooledConnection(connection, databaseConnectionSource);

    final Method method = Object.class.getMethod("toString");
    final String returnValue = (String) method.invoke(pooledConnection);
    assertThat(returnValue, startsWith("Pooled connection"));
  }

  @Test
  public void wrapper() throws Exception, InvocationTargetException {
    final Connection pooledConnection =
        PooledConnectionUtility.newPooledConnection(connection, databaseConnectionSource);

    boolean testedIsWrapperFor = false;
    boolean testedUnrwap = false;
    for (final Method method : Connection.class.getMethods()) {
      if (method.getName().equals("isWrapperFor")) {
        testedIsWrapperFor = true;
        boolean returnValue;

        returnValue = (Boolean) method.invoke(pooledConnection, Connection.class);
        assertThat(returnValue, is(true));

        returnValue = (Boolean) method.invoke(pooledConnection, Boolean.class);
        assertThat(returnValue, is(false));
      }

      if (method.getName().equals("unwrap")) {
        testedUnrwap = true;
        Connection returnValue;

        returnValue = (Connection) method.invoke(pooledConnection, Connection.class);
        assertThat(returnValue, is(not(nullValue())));
        assertThat(returnValue == pooledConnection, is(false));
      }
    }
    assertThat(testedIsWrapperFor, is(true));
    assertThat(testedUnrwap, is(true));
  }
}
