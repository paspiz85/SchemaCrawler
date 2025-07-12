/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2025, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */


package schemacrawler.test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static schemacrawler.test.utility.DatabaseTestUtility.schemaRetrievalOptionsDefault;
import static schemacrawler.tools.lint.config.LinterConfigUtility.readLinterConfigs;
import static schemacrawler.tools.utility.SchemaCrawlerUtility.getCatalog;
import java.sql.Connection;
import org.junit.jupiter.api.Test;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Schema;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.test.utility.WithTestDatabase;
import schemacrawler.tools.command.lint.options.LintOptions;
import schemacrawler.tools.command.lint.options.LintOptionsBuilder;
import schemacrawler.tools.lint.Lint;
import schemacrawler.tools.lint.LinterRegistry;
import schemacrawler.tools.lint.Linters;
import schemacrawler.tools.lint.Lints;
import schemacrawler.tools.lint.config.LinterConfigs;
import schemacrawler.tools.options.Config;
import us.fatehi.utility.datasource.DatabaseConnectionSource;

@WithTestDatabase
public class Issue419LintTest {

  @Test
  public void issue419(final DatabaseConnectionSource dataSource) throws Exception {

    final SchemaCrawlerOptions schemaCrawlerOptions =
        SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions();

    final Catalog catalog =
        getCatalog(dataSource, schemaRetrievalOptionsDefault, schemaCrawlerOptions, new Config());
    assertThat(catalog, notNullValue());
    assertThat(catalog.getSchemas().size(), is(6));
    final Schema schema = catalog.lookupSchema("PUBLIC.FOR_LINT").orElse(null);
    assertThat("FOR_LINT schema not found", schema, notNullValue());
    assertThat("FOR_LINT tables not found", catalog.getTables(schema), hasSize(6));

    final LintOptions lintOptions =
        LintOptionsBuilder.builder().withLinterConfigs("/issue419-linter-configs.yaml").toOptions();

    final LinterConfigs linterConfigs = readLinterConfigs(lintOptions);

    final Linters linters = new Linters(linterConfigs, false);
    final LinterRegistry linterRegistry = LinterRegistry.getLinterRegistry();
    linters.initialize(linterRegistry);

    try (final Connection connection = dataSource.get(); ) {
      linters.lint(catalog, connection);
      final Lints lintReport = linters.getLints();

      assertThat(lintReport.size(), is(1));
      assertThat(
          lintReport.stream().map(Lint::toString).collect(toList()),
          containsInAnyOrder("[PUBLIC.\"PUBLISHER SALES\".REGIONS] primary key not first" /*,
            "[PUBLIC.FOR_LINT.EXTRA_PK] primary key not first" */));
    }
  }
}
