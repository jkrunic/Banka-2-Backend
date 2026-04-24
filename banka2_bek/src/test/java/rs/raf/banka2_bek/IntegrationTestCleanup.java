package rs.raf.banka2_bek;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class IntegrationTestCleanup {

    private IntegrationTestCleanup() {}

    public static void truncateAllTables(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SET REFERENTIAL_INTEGRITY FALSE");

            // H2 MODE=PostgreSQL sa DATABASE_TO_LOWER prijavljuje schema 'public';
            // MODE=MySQL prijavljuje 'PUBLIC'. Case-insensitive poredjenje.
            ResultSet rs = stmt.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE UPPER(TABLE_SCHEMA)='PUBLIC'");
            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
            for (String table : tables) {
                stmt.execute("TRUNCATE TABLE " + table);
            }

            stmt.execute("SET REFERENTIAL_INTEGRITY TRUE");
        } catch (Exception e) {
            throw new RuntimeException("Failed to truncate tables", e);
        }
    }
}
