---
layout: page
title: Flowman Lifecycles
permalink: /lifecycle.html
---
# Lifecycles and Phases

Flowman sees data as artifacts with a common lifecycle. The full lifecycle consists out of specific phases, as follows:

1. **CREATE**.
This will create all relations (tables and directories) specified as targets. The tables will not contain any data,
they only provide an empty hull.

2. **MIGRATE**.
The second step will migrate an existing relation (table or directory) to a new schema, if required. Note that this
is not supported by all target types, and even if a target supports migration in general, it may not be possible due
to unmigratable changes.

3. **BUILD**.
The *build* phase will actually create all records and fill them into the specified relations.

4. **VERIFY**.
The *verify* phase will perform simple checks (for example if a specific Hive partition exists), or may also include
some specific user defined tests that compare data. If verification fails, the build process stops.

5. **TRUNCATE**.
*Truncate* is the first of two phases responsible for cleanup. *Truncate* will only remove individual partitions from
tables (i.e. it will delete data), but it will keep tables alive.

6. **DESTROY**.
The final phase *destroy* is used to phyiscally remove relations including their data. This will also remove table
definitions, views and directories. It performs the opposite operation than the *create* phase.


## Built In Lifecycles:

Some of the execution phases can be performed in a meaningful way one after the other. Such a sequence of phases is
called *lifecycle*. Flowman has the following lifecycles built in:

### Build

The first lifecycle contains the three phases *CREATE*, *MIGRATE* and *BUILD*.

### Truncate

The second lifecycle contains only the single phase *TRUNCATE*

### Destroy

The last lifecycle contains only the single phase *DESTROY*