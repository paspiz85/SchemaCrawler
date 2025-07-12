/*
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2025, Sualeh Fatehi <sualeh@hotmail.com>.
 * All rights reserved.
 * SPDX-License-Identifier: EPL-2.0
 */


package schemacrawler.test;

import org.junit.jupiter.api.Test;
import nl.jqno.equalsverifier.EqualsVerifier;
import schemacrawler.tools.lint.Lint;
import schemacrawler.tools.lint.config.LinterConfig;

public class EqualsTest {

  @Test
  public void lint() {
    EqualsVerifier.forClass(Lint.class)
        .withIgnoredFields("lintId", "linterInstanceId", "objectName")
        .verify();
  }

  @Test
  public void linterConfig() {
    EqualsVerifier.forClass(LinterConfig.class)
        .withIgnoredFields(
            "config",
            "runLinter",
            "threshold",
            "tableInclusionPattern",
            "tableExclusionPattern",
            "columnInclusionPattern",
            "columnExclusionPattern")
        .verify();
  }
}
