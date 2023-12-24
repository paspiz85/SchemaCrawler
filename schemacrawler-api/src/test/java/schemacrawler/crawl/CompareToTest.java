/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2024, Sualeh Fatehi <sualeh@hotmail.com>.
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.comparesEqualTo;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

import org.junit.jupiter.api.Test;

import schemacrawler.schema.NamedObject;
import schemacrawler.schema.NamedObjectKey;
import schemacrawler.schema.Schema;
import schemacrawler.schema.TableType;
import schemacrawler.schemacrawler.SchemaReference;

public class CompareToTest {

  /**
   * See: <a href= "https://github.com/schemacrawler/SchemaCrawler/issues/228">Inconsistent table
   * comparison</a>
   */
  @Test
  public void compareTables() {
    final MutableTable tbl =
        new MutableTable(new SchemaReference(null, "public"), "booking_detail");
    tbl.setTableType(new TableType("table"));

    final MutableView view =
        new MutableView(new SchemaReference(null, "public"), "blog_monthly_stat_fa");
    view.setTableType(new TableType("materialized view"));

    assertThat(view, lessThan(null));
    assertThat(tbl, lessThan(null));

    assertThat(tbl, comparesEqualTo(tbl));
    assertThat(view, comparesEqualTo(view));

    assertThat(tbl, lessThan(view));
    assertThat(view, greaterThan(tbl));
  }

  @Test
  public void databaseObject() {
    class TestDatabaseObject extends AbstractDatabaseObject {

      /** */
      private static final long serialVersionUID = -7594540047157616727L;

      TestDatabaseObject(final Schema schema, final String name) {
        super(schema, name);
      }
    }

    final SchemaReference schema = new SchemaReference("catalog", "schema");
    final SchemaReference schema1 = new SchemaReference("catalog", "schema1");
    final TestDatabaseObject tstDbObj1 = new TestDatabaseObject(schema, "tstDbObj1");
    final TestDatabaseObject tstDbObj2 = new TestDatabaseObject(schema, "tstDbObj2");
    final TestDatabaseObject tstDbObj3 = new TestDatabaseObject(schema1, "tstDbObj1");

    assertThat(tstDbObj1, lessThan(null));
    assertThat(tstDbObj2, lessThan(null));
    assertThat(tstDbObj3, lessThan(null));

    assertThat(tstDbObj1, lessThan(tstDbObj2));
    assertThat(tstDbObj2, greaterThan(tstDbObj1));

    assertThat(tstDbObj1, lessThan(tstDbObj3));
    assertThat(tstDbObj2, lessThan(tstDbObj3));
    assertThat(tstDbObj3, greaterThan(tstDbObj1));
    assertThat(tstDbObj3, greaterThan(tstDbObj2));

    assertThat(
        tstDbObj1,
        greaterThan(
            new NamedObject() {
              /** */
              private static final long serialVersionUID = -1308483158535248447L;

              @Override
              public int compareTo(final NamedObject o) {
                return 0;
              }

              @Override
              public String getFullName() {
                return "";
              }

              @Override
              public String getName() {
                return "";
              }

              @Override
              public NamedObjectKey key() {
                return null;
              }
            }));
  }
}
