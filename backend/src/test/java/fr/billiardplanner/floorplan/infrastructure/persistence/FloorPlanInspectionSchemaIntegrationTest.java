package fr.billiardplanner.floorplan.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class FloorPlanInspectionSchemaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName.parse("postgis/postgis:17-3.7")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("billiard_inspection_schema_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add(
                "billiard-planner.storage.floor-plans-directory",
                () -> "target/test-floor-plans"
        );
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldCreatePdfPagesTableWithExpectedColumns() {
        assertThat(tableExists("floor_plan_pages")).isTrue();

        assertThat(columnsOf("floor_plan_pages"))
                .contains(
                        "id",
                        "floor_plan_id",
                        "page_number",
                        "width_points",
                        "height_points",
                        "rotation_degrees"
                );
    }

    @Test
    void shouldCreateImageMetadataTableWithExpectedColumns() {
        assertThat(tableExists("floor_plan_image_metadata")).isTrue();

        assertThat(columnsOf("floor_plan_image_metadata"))
                .contains(
                        "floor_plan_id",
                        "format",
                        "width_pixels",
                        "height_pixels",
                        "color_model",
                        "dpi_x",
                        "dpi_y",
                        "physical_width_inches",
                        "physical_height_inches"
                );
    }

    private boolean tableExists(String tableName) {
        String result = jdbcTemplate.queryForObject(
                "SELECT to_regclass(?)",
                String.class,
                "planner." + tableName
        );

        return result != null;
    }

    private Set<String> columnsOf(String tableName) {
        return new HashSet<>(
                jdbcTemplate.queryForList(
                        """
                        SELECT column_name
                        FROM information_schema.columns
                        WHERE table_schema = 'planner'
                          AND table_name = ?
                        """,
                        String.class,
                        tableName
                )
        );
    }
}