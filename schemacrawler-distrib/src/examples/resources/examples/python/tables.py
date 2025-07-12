# Copyright (c) Sualeh Fatehi
# SPDX-License-Identifier: EPL-2.0

print(catalog.crawlInfo)

for table in catalog.tables:
  print('')
  print(table.fullName)
  for childTable in table.referencingTables:
    print("  [child] " + childTable.fullName)
