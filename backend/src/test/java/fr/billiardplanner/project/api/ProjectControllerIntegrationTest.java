package fr.billiardplanner.project.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.billiardplanner.project.domain.Project;
import fr.billiardplanner.project.domain.ProjectRepository;
import fr.billiardplanner.project.domain.ProjectStatus;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ProjectControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(
                    DockerImageName
                            .parse("postgis/postgis:17-3.7")
                            .asCompatibleSubstituteFor("postgres")
            )
                    .withDatabaseName("billiard_api_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProjectRepository projectRepository;

    private Project createProject(String name) {
        Project project = Project.create(
                name,
                "Integration client",
                "Lyon, France",
                "Controller integration test",
                Instant.now()
        );

        return projectRepository.save(project);
    }

    @Test
    void shouldCreateProject() throws Exception {
        String request = """
                {
                    "name": "API Created Project",
                    "clientName": "Test Client",
                    "siteAddress": "Lyon",
                    "description": "Created through REST API"
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("API Created Project"))
                .andExpect(jsonPath("$.clientName").value("Test Client"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldRejectProjectWithBlankName() throws Exception {
        String request = """
                {
                    "name": "   ",
                    "clientName": "Test Client"
                }
                """;

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void shouldRetrieveProject() throws Exception {
        Project project = createProject("API Retrieve Project");

        mockMvc.perform(get("/api/v1/projects/{id}", project.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().toString()))
                .andExpect(jsonPath("$.name").value("API Retrieve Project"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldReturn404WhenProjectDoesNotExist() throws Exception {
        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/projects/{id}", unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").exists());
    }

    @Test
    void shouldUpdateProject() throws Exception {
        Project project = createProject("Before Update");

        String request = """
                {
                    "name": "After Update",
                    "clientName": "Updated Client",
                    "siteAddress": "Paris",
                    "description": "Updated through API"
                }
                """;

        mockMvc.perform(put("/api/v1/projects/{id}", project.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(project.getId().toString()))
                .andExpect(jsonPath("$.name").value("After Update"))
                .andExpect(jsonPath("$.clientName").value("Updated Client"))
                .andExpect(jsonPath("$.siteAddress").value("Paris"));
    }

    @Test
    void shouldArchiveProject() throws Exception {
        Project project = createProject("Project To Archive");

        mockMvc.perform(delete("/api/v1/projects/{id}", project.getId()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        Project archived = projectRepository.findById(project.getId())
                .orElseThrow();

        org.junit.jupiter.api.Assertions.assertEquals(
                ProjectStatus.ARCHIVED,
                archived.getStatus()
        );

        org.junit.jupiter.api.Assertions.assertNotNull(
                archived.getArchivedAt()
        );
    }

    @Test
    void shouldReturn404WhenArchivingUnknownProject() throws Exception {
        mockMvc.perform(delete(
                        "/api/v1/projects/{id}",
                        UUID.randomUUID()
                ))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldListProjectsWithPagination() throws Exception {
        createProject("API Pagination Alpha");
        createProject("API Pagination Bravo");
        createProject("API Pagination Charlie");

        mockMvc.perform(get("/api/v1/projects")
                        .param("search", "API Pagination")
                        .param("page", "0")
                        .param("size", "2")
                        .param("sortBy", "NAME")
                        .param("direction", "ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projects", hasSize(2)))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2));
    }

    @Test
    void shouldRejectInvalidPaginationParameters() throws Exception {
        mockMvc.perform(get("/api/v1/projects")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ invalid json }"))
                .andExpect(status().isBadRequest());
    }
}