package app.rekord.application.persistence.migration;

/** The baseline creates nothing, so its schema is the empty one. */
class V1MigrationIT extends MigrationSchemaCheck {

    @Override
    protected String version() {
        return "1";
    }

    @Override
    protected SchemaSnapshot expected() {
        return SchemaSnapshot.empty();
    }
}
