package org.de.unistuttgart.quadrama.db.orm.beans;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class DBRelation {
	@DatabaseField(generatedId = true)
	int id;

	@DatabaseField
	String name;
}