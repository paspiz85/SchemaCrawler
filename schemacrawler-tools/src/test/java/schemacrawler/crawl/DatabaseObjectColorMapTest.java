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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import schemacrawler.schemacrawler.SchemaReference;
import schemacrawler.tools.text.utility.DatabaseObjectColorMap;
import us.fatehi.utility.Color;

public class DatabaseObjectColorMapTest {

  @Test
  public void generateColors() {
    final DatabaseObjectColorMap colorMap = DatabaseObjectColorMap.initialize(false);

    Color color;

    assertThrows(NullPointerException.class, () -> colorMap.getColor(null));

    color = colorMap.getColor(new MutableTable(new SchemaReference(null, null), "table"));
    assertThat(color, is(Color.fromRGB(0xF2, 0xE9, 0xCE)));

    color = colorMap.getColor(new MutableTable(new SchemaReference(null, "schema"), "table"));
    assertThat(color, is(Color.fromRGB(0xF2, 0xE2, 0xCE)));

    color = colorMap.getColor(new MutableTable(new SchemaReference("catalog", "schema"), "table"));
    assertThat(color, is(Color.fromRGB(0xF2, 0xDF, 0xCE)));

    color = colorMap.getColor(new MutableTable(new SchemaReference("catalog", null), "table"));
    assertThat(color, is(Color.fromRGB(0xCE, 0xF2, 0xED)));
  }
}
