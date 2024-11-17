/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2025, Sualeh Fatehi <sualeh@hotmail.com>.
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

package schemacrawler.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static schemacrawler.test.utility.DatabaseTestUtility.schemaRetrievalOptionsDefault;
import static schemacrawler.test.utility.ExecutableTestUtility.executableExecution;
import static schemacrawler.test.utility.ExecutableTestUtility.hasSameContentAndTypeAs;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.outputOf;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import schemacrawler.schemacrawler.InfoLevel;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.test.utility.AssertNoSystemErrOutput;
import schemacrawler.test.utility.AssertNoSystemOutOutput;
import schemacrawler.test.utility.TestUtility;
import schemacrawler.test.utility.WithTestDatabase;
import schemacrawler.tools.command.text.operation.options.OperationType;
import schemacrawler.tools.command.text.schema.options.SchemaTextOptionsBuilder;
import schemacrawler.tools.command.text.schema.options.TextOutputFormat;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;
import schemacrawler.tools.options.OutputFormat;
import us.fatehi.utility.datasource.DatabaseConnectionSource;

@AssertNoSystemErrOutput
@AssertNoSystemOutOutput
@WithTestDatabase
public class SpinThroughOperationsExecutableTest {

  private static final String SPIN_THROUGH_OPERATIONS_OUTPUT = "spin_through_operations_output/";

  @BeforeAll
  public static void clean() throws Exception {
    TestUtility.clean(SPIN_THROUGH_OPERATIONS_OUTPUT);
  }

  protected static Stream<Arguments> spinThroughArguments() {
    return Arrays.stream(OperationType.values())
        .flatMap(
            operation ->
                Arrays.stream(InfoLevel.values())
                    .filter(infoLevel -> infoLevel != InfoLevel.unknown)
                    .flatMap(
                        infoLevel ->
                            Arrays.stream(
                                    new TextOutputFormat[] {
                                      TextOutputFormat.text, TextOutputFormat.html
                                    })
                                .map(
                                    outputFormat ->
                                        Arguments.of(operation, infoLevel, outputFormat))));
  }

  private static String referenceFile(
      final OperationType operation, final InfoLevel infoLevel, final OutputFormat outputFormat) {
    final String referenceFile =
        String.format(
            "%d%d.%s_%s.%s",
            operation.ordinal(),
            infoLevel.ordinal(),
            operation,
            infoLevel,
            outputFormat.getFormat());
    return referenceFile;
  }

  @DisplayName("Spin through operations for output")
  @ParameterizedTest()
  @MethodSource("spinThroughArguments")
  public void spinThroughOperationsExecutable(
      final OperationType operation,
      final InfoLevel infoLevel,
      final TextOutputFormat outputFormat,
      final DatabaseConnectionSource dataSource)
      throws Exception {

    // Special case where no output is generated
    if (infoLevel == InfoLevel.minimum && operation == OperationType.dump) {
      return;
    }

    final LimitOptionsBuilder limitOptionsBuilder =
        LimitOptionsBuilder.builder()
            .includeAllSequences()
            .includeAllSynonyms()
            .includeAllRoutines();
    final LoadOptionsBuilder loadOptionsBuilder =
        LoadOptionsBuilder.builder().withSchemaInfoLevel(infoLevel.toSchemaInfoLevel());
    final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions()
            .withLimitOptions(limitOptionsBuilder.toOptions())
            .withLoadOptions(loadOptionsBuilder.toOptions());

    final SchemaTextOptionsBuilder schemaTextOptionsBuilder = SchemaTextOptionsBuilder.builder();
    schemaTextOptionsBuilder.noInfo(false);

    final SchemaCrawlerExecutable executable = new SchemaCrawlerExecutable(operation.name());
    executable.setSchemaCrawlerOptions(schemaCrawlerOptions);
    executable.setAdditionalConfiguration(schemaTextOptionsBuilder.toConfig());
    executable.setSchemaRetrievalOptions(schemaRetrievalOptionsDefault);

    final String referenceFile =
        SPIN_THROUGH_OPERATIONS_OUTPUT + referenceFile(operation, infoLevel, outputFormat);
    assertThat(
        outputOf(executableExecution(dataSource, executable, outputFormat)),
        hasSameContentAndTypeAs(classpathResource(referenceFile), outputFormat));
  }
}
