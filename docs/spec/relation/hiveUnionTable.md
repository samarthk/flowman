# Hive Union Table

The `hiveUnionTable` is a compound target for storing data in Hive that also provides extended schema migration
capabilities. In addition to schema changes which are supported out of the box via Hive, this target also supports
more changes like dropping columns, changing data types. This is accomplished by creating a UNION view on top of
possibly multiple Hive tables (each of them having a different incompatible schema).

## Example

```yaml
relations:
  some_table:
    kind: hiveUnionTable
    viewDatabase: "crm"
    view: "my_table"
    tableDatabase: "crm"
    tablePrefix: "zz_my_table"
    locationPrefix: "/hive/crm/zz_my_table"
    external: true
    format: parquet
    partitions:
    - name: landing_date
      type: string
      description: "The date on which the contract event was generated"
    schema:
      kind: mapping
      mapping: some_mapping
```

## Fields

## Fields
* `kind` **(mandatory)** *(string)*: `hiveUnionTable`

* `viewDatabase` **(optional)** *(string)* *(default: empty)*: 

* `view` **(mandatory)** *(string)*: 
Name of the view to be created and managed by Flowman.

* `viewDatabase` **(optional)** *(string)* *(default: empty)*: 
Name of the Hive database where the view should be created in

* `tablePrefix` **(mandatory)** *(string)*: 
Prefix of all tables which will be created and managed by Flowman. A version number will be appended to the prefix
to form the full table name.

* `tableDatabase` **(optional)** *(string)* *(default: empty)*: 
Name of the Hive database where the tables should be created in

 * `locationPrefix` **(optional)** *(string)* *(default: empty)*:
 Specifies the location prefix of the files stored in this Hive table. This setting is only used
 when Flowman is used to create the Hive table and is ignored otherwise. This corresponds
 to the `LOCATION` in a `CREATE TABLE` statement.
 
 * `format` **(optional)** *(string)* *(default: empty)*:
 Specifies the format of the files stored in this Hive table. This setting is only used
 when Flowman is used to create the Hive table and is ignored otherwise. This corresponds
 to the `FORMAT` in a `CREATE TABLE` statement.

 * `rowFormat` **(optional)** *(string)* *(default: empty)*:
 Specifies the row format of the files stored in this Hive table. This setting is only used
 when Flowman is used to create the Hive table and is ignored otherwise. This corresponds
 to the `ROW FORMAT` in a `CREATE TABLE` statement.

 * `inputFormat` **(optional)** *(string)* *(default: empty)*:
 Specifies the input format of the files stored in this Hive table. This setting is only used
 when Flowman is used to create the Hive table and is ignored otherwise. This corresponds
 to the `INPUT FORMAT` in a `CREATE TABLE` statement.

 * `outputFormat` **(optional)** *(string)* *(default: empty)*:
 Specifies the input format of the files stored in this Hive table. This setting is only used
 when Flowman is used to create the Hive table and is ignored otherwise. This corresponds
 to the `OUTPUT FORMAT` in a `CREATE TABLE` statement.

 * `partitions` **(optional)** *(list:partition)* *(default: empty)*:
 Specifies all partition columns. This is used both for creating Hive tables, but also for
 writing and reading to and from them. Therefore if you are working with partitioned Hive
 tables **you have to specify partition columns, even if Flowman is not used for creating
 the table**.

 * `properties` **(optional)** *(map:string)* *(default: empty)*:
 Specifies additional properties of the Hive table. This setting is only used
 when Flowman is used to create the Hive table and is ignored otherwise. This corresponds
 to the `TBLPROPERTIES` in a `CREATE TABLE` statement.
