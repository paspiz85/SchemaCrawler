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

package schemacrawler.test.utility;

import static java.nio.file.Files.newBufferedWriter;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static schemacrawler.test.utility.TestUtility.copyResourceToTempFile;
import static schemacrawler.test.utility.TestUtility.flattenCommandlineArgs;
import static schemacrawler.tools.commandline.utility.CommandLineUtility.newCommandLine;
import static schemacrawler.utility.SchemaCrawlerUtility.getCatalog;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import picocli.CommandLine;
import schemacrawler.Main;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;
import schemacrawler.tools.commandline.state.SchemaCrawlerShellState;
import schemacrawler.tools.options.OutputFormat;
import us.fatehi.utility.IOUtility;

public final class CommandlineTestUtility {

  public static Path commandlineExecution(
      final DatabaseConnectionInfo connectionInfo,
      final String command,
      final Map<String, String> argsMap,
      final Map<String, String> config,
      final OutputFormat outputFormat)
      throws Exception {
    return commandlineExecution(
        connectionInfo, command, argsMap, writeConfigToTempFile(config), outputFormat.getFormat());
  }

  public static Path commandlineExecution(
      final DatabaseConnectionInfo connectionInfo,
      final String command,
      final Map<String, String> argsMap,
      final OutputFormat outputFormat)
      throws Exception {
    return commandlineExecution(
        connectionInfo, command, argsMap, (Path) null, outputFormat.getFormat());
  }

  public static Path commandlineExecution(
      final DatabaseConnectionInfo connectionInfo,
      final String command,
      final Map<String, String> argsMap,
      final Path propertiesFile,
      final String outputFormatValue,
      final Path out)
      throws Exception {
    final Map<String, String> commandlineArgsMap = new HashMap<>();
    commandlineArgsMap.put("-url", connectionInfo.getConnectionUrl());
    commandlineArgsMap.put("-user", "sa");
    commandlineArgsMap.put("-password", "");
    if (propertiesFile != null) {
      commandlineArgsMap.put("g", propertiesFile.toString());
    }
    commandlineArgsMap.put("c", command);
    commandlineArgsMap.put("-output-format", outputFormatValue);
    commandlineArgsMap.put("-output-file", out.toString());

    // Override and add to command-line arguments
    if (argsMap != null) {
      commandlineArgsMap.putAll(argsMap);
    }

    Main.main(flattenCommandlineArgs(commandlineArgsMap));

    return out;
  }

  public static Path commandlineExecution(
      final DatabaseConnectionInfo connectionInfo,
      final String command,
      final Map<String, String> argsMap,
      final String outputFormatValue)
      throws Exception {
    return commandlineExecution(connectionInfo, command, argsMap, (Path) null, outputFormatValue);
  }

  public static Path commandlineExecution(
      final DatabaseConnectionInfo connectionInfo,
      final String command,
      final Map<String, String> argsMap,
      final String propertiesFileResource,
      final OutputFormat outputFormat)
      throws Exception {
    final Path propertiesFile = copyResourceToTempFile(propertiesFileResource);
    return commandlineExecution(
        connectionInfo, command, argsMap, propertiesFile, outputFormat.getFormat());
  }

  public static SchemaCrawlerShellState createLoadedSchemaCrawlerShellState(
      final Connection connection) throws SchemaCrawlerException {
    final SchemaCrawlerOptions schemaCrawlerOptions =
        DatabaseTestUtility.schemaCrawlerOptionsWithMaximumSchemaInfoLevel;
    final Catalog catalog = getCatalog(connection, schemaCrawlerOptions);

    final SchemaCrawlerShellState state = new SchemaCrawlerShellState();
    state.setSchemaCrawlerOptionsBuilder(
        SchemaCrawlerOptionsBuilder.builder().fromOptions(schemaCrawlerOptions));
    state.setSchemaRetrievalOptionsBuilder(SchemaRetrievalOptionsBuilder.builder());
    state.setDataSource(() -> connection); // is-connected
    state.setCatalog(catalog); // is-loaded
    return state;
  }

  public static void runCommandInTest(final Object object, final String[] args) {

    class SaveExceptionHandler
        implements CommandLine.IParameterExceptionHandler, CommandLine.IExecutionExceptionHandler {
      private Exception lastException;

      public RuntimeException getLastException() {
        if (lastException == null) {
          return new NullPointerException();
        } else if (lastException instanceof RuntimeException) {
          return (RuntimeException) lastException;
        } else {
          return new RuntimeException(lastException);
        }
      }

      @Override
      public int handleExecutionException(
          final Exception ex,
          final CommandLine commandLine,
          final CommandLine.ParseResult parseResult)
          throws Exception {
        lastException = ex;
        return 0;
      }

      @Override
      public int handleParseException(final CommandLine.ParameterException ex, final String[] args)
          throws Exception {
        lastException = ex;
        return 0;
      }

      public boolean hasException() {
        return lastException != null;
      }
    }

    final SaveExceptionHandler saveExceptionHandler = new SaveExceptionHandler();
    final CommandLine commandLine = newCommandLine(object, null, true);
    commandLine.setParameterExceptionHandler(saveExceptionHandler);
    commandLine.setExecutionExceptionHandler(saveExceptionHandler);
    commandLine.execute(args);
    if (saveExceptionHandler.hasException()) {
      throw saveExceptionHandler.getLastException();
    }
  }

  private static Path commandlineExecution(
      final DatabaseConnectionInfo connectionInfo,
      final String command,
      final Map<String, String> argsMap,
      final Path propertiesFile,
      final String outputFormatValue)
      throws Exception {
    final TestWriter testout = new TestWriter();
    try (final TestWriter out = testout) {
      commandlineExecution(
          connectionInfo, command, argsMap, propertiesFile, outputFormatValue, out.getFilePath());
    }
    return testout.getFilePath();
  }

  private static Path writeConfigToTempFile(final Map<String, String> config) throws IOException {
    if (config == null) {
      return null;
    }

    final Path tempFile =
        IOUtility.createTempFilePath("test", ".properties").normalize().toAbsolutePath();

    final Properties properties = new Properties();
    properties.putAll(config);

    try (final Writer tempFileWriter =
        newBufferedWriter(tempFile, WRITE, TRUNCATE_EXISTING, CREATE); ) {
      properties.store(tempFileWriter, "Store config to temporary file for testing");
    }

    return tempFile;
  }

  private CommandlineTestUtility() {
    // Prevent instantiation
  }
}
