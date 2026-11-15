
package fr.billiardplanner.project.infrastructure.persistence;

import fr.billiardplanner.project.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProjectJpaRepository
        extends JpaRepository<ProjectEntity, UUID> {

    @Query("""
            SELECT p
            FROM ProjectEntity p
            WHERE (
                :includeArchived = true
                OR p.archivedAt IS NULL
            )
            AND (
                :searchTerm = ''
                OR LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                OR LOWER(COALESCE(p.clientName, ''))
                        LIKE LOWER(CONCAT('%', :searchTerm, '%'))
                )
            AND (
                :status IS NULL
                OR p.status = :status
            )
            """)
    Page<ProjectEntity> search(
            @Param("searchTerm") String searchTerm,
            @Param("status") ProjectStatus status,
            @Param("includeArchived") boolean includeArchived,
            Pageable pageable
    );
}
