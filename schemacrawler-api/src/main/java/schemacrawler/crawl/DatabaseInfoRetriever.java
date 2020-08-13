/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2020, Sualeh Fatehi <sualeh@hotmail.com>.
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

package schemacrawler.crawl;


import static schemacrawler.schemacrawler.InformationSchemaKey.DATABASE_USERS;
import static schemacrawler.schemacrawler.InformationSchemaKey.SERVER_INFORMATION;
import static us.fatehi.utility.Utility.isBlank;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import schemacrawler.inclusionrule.IncludeAll;
import schemacrawler.schema.Property;
import schemacrawler.schemacrawler.InformationSchemaViews;
import schemacrawler.schemacrawler.Query;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import us.fatehi.utility.DatabaseUtility;
import schemacrawler.SchemaCrawlerLogger;
import us.fatehi.utility.string.StringFormat;

final class DatabaseInfoRetriever
  extends AbstractRetriever
{

  private static final SchemaCrawlerLogger LOGGER =
    SchemaCrawlerLogger.getLogger(DatabaseInfoRetriever.class.getName());

  private static final List<String> ignoreMethods = Arrays.asList(
    "getDatabaseProductName",
    "getDatabaseProductVersion",
    "getURL",
    "getUserName",
    "getDriverName",
    "getDriverVersion");

  /**
   * Checks if a method is a result set method.
   *
   * @param method
   *   Method
   * @return Whether a method is a result set method
   */
  private static boolean isDatabasePropertiesResultSetMethod(final Method method)
  {
    final Class<?> returnType = method.getReturnType();
    final boolean isPropertiesResultSetMethod =
      returnType.equals(ResultSet.class)
      && method.getParameterTypes().length == 0;
    return isPropertiesResultSetMethod;
  }

  /**
   * Checks if a method is a database property.
   *
   * @param method
   *   Method
   * @return Whether method is a database property
   */
  private static boolean isDatabasePropertyListMethod(final Method method)
  {
    final Class<?> returnType = method.getReturnType();
    final boolean isDatabasePropertyListMethod =
      returnType.equals(String.class) && method
        .getName()
        .endsWith("s") && method.getParameterTypes().length == 0;
    return isDatabasePropertyListMethod;
  }

  /**
   * Checks if a method is a database property.
   *
   * @param method
   *   Method
   * @return Whether method is a database property
   */
  private static boolean isDatabasePropertyMethod(final Method method)
  {
    final Class<?> returnType = method.getReturnType();
    final boolean notPropertyMethod = returnType.equals(ResultSet.class)
                                      || returnType.equals(Connection.class)
                                      || method.getParameterTypes().length > 0;
    return !notPropertyMethod;
  }

  private static ImmutableDatabaseProperty retrieveResultSetTypeProperty(final DatabaseMetaData dbMetaData,
                                                                         final Method method,
                                                                         final int resultSetType,
                                                                         final String resultSetTypeName)
    throws IllegalAccessException, InvocationTargetException
  {
    final String name =
      method.getName() + "For" + resultSetTypeName + "ResultSets";
    final Boolean propertyValue =
      (Boolean) method.invoke(dbMetaData, Integer.valueOf(resultSetType));
    return new ImmutableDatabaseProperty(name, propertyValue);
  }

  DatabaseInfoRetriever(final RetrieverConnection retrieverConnection,
                        final MutableCatalog catalog,
                        final SchemaCrawlerOptions options)
    throws SQLException
  {
    super(retrieverConnection, catalog, options);
  }

  /**
   * Provides additional information on the database.
   *
   * @throws SQLException
   *   On a SQL exception
   */
  void retrieveAdditionalDatabaseInfo()
  {
    final DatabaseMetaData dbMetaData = getMetaData();
    final MutableDatabaseInfo dbInfo = catalog.getDatabaseInfo();

    final Collection<ImmutableDatabaseProperty> dbProperties =
      new ArrayList<>();

    final Method[] methods = DatabaseMetaData.class.getMethods();
    for (final Method method : methods)
    {
      try
      {
        if (method.getParameterTypes().length > 0 || ignoreMethods.contains(
          method.getName()))
        {
          continue;
        }

        LOGGER.log(Level.FINER,
                   new StringFormat(
                     "Retrieving database property using method <%s>",
                     method));

        final Object methodReturnValue = method.invoke(dbMetaData);
        if (isDatabasePropertyListMethod(method))
        {
          final String value = (String) methodReturnValue;
          final String[] list = value == null? new String[0]: value.split(",");
          dbProperties.add(new ImmutableDatabaseProperty(method.getName(),
                                                         list));
        }
        else if (isDatabasePropertyMethod(method))
        {
          dbProperties.add(new ImmutableDatabaseProperty(method.getName(),
                                                         methodReturnValue));
        }
        else if (isDatabasePropertiesResultSetMethod(method))
        {
          final ResultSet results = (ResultSet) methodReturnValue;
          final List<String> resultsList =
            DatabaseUtility.readResultsVector(results);
          Collections.sort(resultsList);
          dbProperties.add(new ImmutableDatabaseProperty(method.getName(),
                                                         resultsList.toArray(new String[resultsList.size()])));
        }

      }
      catch (final IllegalAccessException | InvocationTargetException e)
      {
        LOGGER.log(Level.FINE,
                   new StringFormat("Could not execute method <%s>", method),
                   e.getCause());
      }
      catch (final AbstractMethodError | SQLFeatureNotSupportedException e)
      {
        logSQLFeatureNotSupported(new StringFormat(
          "Database metadata method <%s> not supported",
          method), e);
      }
      catch (final SQLException e)
      {
        logPossiblyUnsupportedSQLFeature(new StringFormat(
          "SQL exception invoking method <%s>",
          method), e);
      }

    }

    final Collection<ImmutableDatabaseProperty> resultSetTypesProperties =
      retrieveResultSetTypesProperties(dbMetaData);
    dbProperties.addAll(resultSetTypesProperties);

    dbInfo.addAll(dbProperties);

  }

  /**
   * Provides information on the JDBC driver.
   *
   * @throws SQLException
   *   On a SQL exception
   */
  void retrieveAdditionalJdbcDriverInfo()
  {
    final MutableJdbcDriverInfo driverInfo = catalog.getJdbcDriverInfo();
    if (driverInfo == null)
    {
      return;
    }

    try
    {
      final DatabaseMetaData dbMetaData = getMetaData();
      final String url = dbMetaData.getURL();

      final Driver jdbcDriver = getRetrieverConnection().getDriver();
      final DriverPropertyInfo[] propertyInfo =
        jdbcDriver.getPropertyInfo(url, new Properties());
      for (final DriverPropertyInfo driverPropertyInfo : propertyInfo)
      {
        driverInfo.addJdbcDriverProperty(new ImmutableJdbcDriverProperty(
          driverPropertyInfo));
      }
    }
    catch (final SQLException e)
    {
      LOGGER.log(Level.WARNING, "Could not obtain JDBC driver information", e);
    }

  }

  void retrieveCrawlInfo()
  {
    catalog.setCrawlInfo();
  }

  /**
   * Provides information on the database.
   *
   * @throws SQLException
   *   On a SQL exception
   */
  void retrieveDatabaseInfo()
  {

    final MutableDatabaseInfo dbInfo = catalog.getDatabaseInfo();
    if (dbInfo == null)
    {
      return;
    }

    final DatabaseMetaData dbMetaData;
    try
    {
      dbMetaData = getMetaData();

      dbInfo.setUserName(dbMetaData.getUserName());
      dbInfo.setProductName(dbMetaData.getDatabaseProductName());
      dbInfo.setProductVersion(dbMetaData.getDatabaseProductVersion());
    }
    catch (final SQLException e)
    {
      LOGGER.log(Level.WARNING, "Could not obtain database information", e);
    }

  }

  /**
   * Provides information on the JDBC driver.
   *
   * @throws SQLException
   *   On a SQL exception
   */
  void retrieveJdbcDriverInfo()
  {
    final MutableJdbcDriverInfo driverInfo = catalog.getJdbcDriverInfo();
    if (driverInfo == null)
    {
      return;
    }

    try
    {
      final DatabaseMetaData dbMetaData = getMetaData();
      final String url = dbMetaData.getURL();

      driverInfo.setDriverName(dbMetaData.getDriverName());
      driverInfo.setDriverVersion(dbMetaData.getDriverVersion());
      driverInfo.setConnectionUrl(url);
      final Driver jdbcDriver = getRetrieverConnection().getDriver();
      driverInfo.setJdbcDriverClassName(jdbcDriver
                                          .getClass()
                                          .getName());
      driverInfo.setJdbcCompliant(jdbcDriver.jdbcCompliant());
    }
    catch (final SQLException e)
    {
      LOGGER.log(Level.WARNING, "Could not obtain JDBC driver information", e);
    }

  }

  void retrieveServerInfo()
  {
    final MutableDatabaseInfo dbInfo = catalog.getDatabaseInfo();
    if (dbInfo == null)
    {
      return;
    }

    final InformationSchemaViews informationSchemaViews =
      getRetrieverConnection().getInformationSchemaViews();
    if (!informationSchemaViews.hasQuery(SERVER_INFORMATION))
    {
      LOGGER.log(Level.INFO,
                 "Not retrieving server information, since this was not requested");
      LOGGER.log(Level.FINE,
                 "Server information SQL statement was not provided");
      return;
    }
    final Query serverInfoSql =
      informationSchemaViews.getQuery(SERVER_INFORMATION);

    final Connection connection = getDatabaseConnection();
    try (
      final Statement statement = connection.createStatement();
      final MetadataResultSet results = new MetadataResultSet(serverInfoSql,
                                                              statement,
                                                              new IncludeAll())
    )
    {
      results.setDescription("retrieveServerInfo");
      while (results.next())
      {
        final String propertyName = results.getString("NAME");
        if (isBlank(propertyName))
        {
          continue;
        }

        final String propertyValue = results.getString("VALUE");
        final String propertyDescription = results.getString("DESCRIPTION");

        LOGGER.log(Level.FINER,
                   new StringFormat(
                     "Retrieving server information property: %s=%s",
                     propertyName,
                     propertyValue));

        final Property serverInfoProperty = new ImmutableServerInfoProperty(
          propertyName,
          propertyValue,
          propertyDescription);
        dbInfo.addServerInfo(serverInfoProperty);
      }
    }
    catch (final Exception e)
    {
      LOGGER.log(Level.WARNING, "Could not retrieve server information", e);
    }

  }

  void retrieveDatabaseUsers()
  {

    final InformationSchemaViews informationSchemaViews =
      getRetrieverConnection().getInformationSchemaViews();
    if (!informationSchemaViews.hasQuery(DATABASE_USERS))
    {
      LOGGER.log(Level.INFO,
                 "Not retrieving database users information, since this was not requested");
      LOGGER.log(Level.FINE, "Database users SQL statement was not provided");
      return;
    }
    final Query databaseUsersSql = informationSchemaViews.getQuery(
      DATABASE_USERS);

    final Connection connection = getDatabaseConnection();
    try (
      final Statement statement = connection.createStatement();
      final MetadataResultSet results = new MetadataResultSet(databaseUsersSql,
                                                              statement,
                                                              new IncludeAll())
    )
    {
      results.setDescription("retrieveDatabaseUsers");
      while (results.next())
      {
        final String username = results.getString("USERNAME");
        if (isBlank(username))
        {
          continue;
        }
        LOGGER.log(Level.FINER,
                   new StringFormat("Retrieving database user: %s", username));

        final ImmutableDatabaseUser databaseUser =
          new ImmutableDatabaseUser(username);
        databaseUser.addAttributes(results.getAttributes());
        catalog.addDatabaseUser(databaseUser);
      }
    }
    catch (final Exception e)
    {
      LOGGER.log(Level.WARNING, "Could not retrieve database users", e);
    }

  }


  private Collection<ImmutableDatabaseProperty> retrieveResultSetTypesProperties(
    final DatabaseMetaData dbMetaData)
  {
    final Collection<ImmutableDatabaseProperty> dbProperties =
      new ArrayList<>();
    final String[] resultSetTypesMethods = new String[] {
      "deletesAreDetected",
      "insertsAreDetected",
      "updatesAreDetected",
      "othersInsertsAreVisible",
      "othersDeletesAreVisible",
      "othersUpdatesAreVisible",
      "ownDeletesAreVisible",
      "ownInsertsAreVisible",
      "ownUpdatesAreVisible",
      "supportsResultSetType"
    };
    for (final String methodName : resultSetTypesMethods)
    {
      try
      {
        final Method method =
          DatabaseMetaData.class.getMethod(methodName, int.class);
        LOGGER.log(Level.FINER,
                   new StringFormat(
                     "Retrieving database property using method <%s>",
                     method));

        dbProperties.add(retrieveResultSetTypeProperty(dbMetaData,
                                                       method,
                                                       ResultSet.TYPE_FORWARD_ONLY,
                                                       "TYPE_FORWARD_ONLY"));
        dbProperties.add(retrieveResultSetTypeProperty(dbMetaData,
                                                       method,
                                                       ResultSet.TYPE_SCROLL_INSENSITIVE,
                                                       "TYPE_SCROLL_INSENSITIVE"));
        dbProperties.add(retrieveResultSetTypeProperty(dbMetaData,
                                                       method,
                                                       ResultSet.TYPE_SCROLL_SENSITIVE,
                                                       "TYPE_SCROLL_SENSITIVE"));
      }
      catch (final Exception e)
      {
        LOGGER.log(Level.FINE,
                   new StringFormat("Could not execute method <%s>",
                                    methodName),
                   e.getCause());
        continue;
      }
    }

    return dbProperties;
  }

}
