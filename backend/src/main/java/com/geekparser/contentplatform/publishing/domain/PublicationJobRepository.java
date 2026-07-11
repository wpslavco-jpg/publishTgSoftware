package com.geekparser.contentplatform.publishing.domain;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PublicationJobRepository extends JpaRepository<PublicationJob, Long> {

    List<PublicationJob> findAllByOrderByScheduledAtDesc();

    @Query("""
            select job from PublicationJob job
            join fetch job.preparedArticle
            where job.status = :status
              and job.scheduledAt < :scheduledAt
            order by job.scheduledAt asc
            """)
    List<PublicationJob> findDueJobs(@Param("status") PublicationStatus status,
                                     @Param("scheduledAt") OffsetDateTime scheduledAt);
}
