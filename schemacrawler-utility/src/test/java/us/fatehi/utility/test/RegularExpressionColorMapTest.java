/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2023, Sualeh Fatehi <sualeh@hotmail.com>.
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

import static com.github.npathai.hamcrestopt.OptionalMatchers.isEmpty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import us.fatehi.utility.Color;
import us.fatehi.utility.RegularExpressionColorMap;

public class RegularExpressionColorMapTest {

  private static final Color test_color = Color.fromRGB(26, 59, 92);

  @Test
  public void badColors() {
    final RegularExpressionColorMap colorMap = new RegularExpressionColorMap();

    colorMap.put("SC.*", "1A3B5");
    MatcherAssert.assertThat(colorMap.match("SCH"), isEmpty());

    colorMap.put("SC.*", test_color.toString().substring(1) + "A");
    MatcherAssert.assertThat(colorMap.match("SCH"), isEmpty());

    colorMap.put("SC.*", test_color.toString().substring(1));
    MatcherAssert.assertThat(colorMap.match("SCH"), isEmpty());
  }

  @Test
  public void badPatterns() {
    final RegularExpressionColorMap colorMap = new RegularExpressionColorMap();

    colorMap.put("SC(H", test_color.toString());
    MatcherAssert.assertThat(colorMap.match("SCH"), isEmpty());
  }

  @Test
  public void fromProperties() {
    final Map<String, String> properties = new HashMap<>();
    properties.put(test_color.toString().substring(1), "SC.*");
    properties.put(test_color.toString().substring(1) + "A", "SC.*");
    properties.put("000000", "QW.*");
    final RegularExpressionColorMap colorMap = new RegularExpressionColorMap(properties);

    assertThat(colorMap.size(), is(2));
    MatcherAssert.assertThat(colorMap.match("SCH"), is(not(isEmpty())));
    assertThat(colorMap.match("SCH").get().equals(test_color), is(true));
    MatcherAssert.assertThat(colorMap.match("SHC"), isEmpty());
    MatcherAssert.assertThat(colorMap.match("QW"), is(not(isEmpty())));
  }

  @Test
  public void happyPath() {
    final RegularExpressionColorMap colorMap = new RegularExpressionColorMap();

    colorMap.put("SC.*", test_color.toString());
    MatcherAssert.assertThat(colorMap.match("SCH"), is(not(isEmpty())));
    assertThat(colorMap.match("SCH").get().equals(test_color), is(true));
    MatcherAssert.assertThat(colorMap.match("SC.*"), is(not(isEmpty())));
    MatcherAssert.assertThat(colorMap.match("SHC"), isEmpty());
  }

  @Test
  public void literals() {
    final RegularExpressionColorMap colorMap = new RegularExpressionColorMap();

    colorMap.putLiteral("SC.*", test_color);
    MatcherAssert.assertThat(colorMap.match("SCH"), isEmpty());
    MatcherAssert.assertThat(colorMap.match("SC.*"), is(not(isEmpty())));
  }
}
