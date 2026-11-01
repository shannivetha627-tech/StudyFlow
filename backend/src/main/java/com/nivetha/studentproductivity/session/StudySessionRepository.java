package com.nivetha.studentproductivity.session;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nivetha.studentproductivity.user.User;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {

	List<StudySession> findByUserOrderBySessionDateAscStartTimeAsc(User user);

	List<StudySession> findByUserAndSessionDateOrderByStartTimeAsc(User user, LocalDate date);

	List<StudySession> findByUserAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
			User user, LocalDate startDate, LocalDate endDate
	);

	Optional<StudySession> findByIdAndUser(Long id, User user);

	@Query("""
		SELECT s FROM StudySession s
		WHERE s.user = :user
		  AND s.sessionDate = :date
		  AND (:excludeId IS NULL OR s.id <> :excludeId)
		  AND ((s.startTime < :endTime AND s.endTime > :startTime))
	""")
	List<StudySession> findOverlappingSessions(
			@Param("user") User user,
			@Param("date") LocalDate date,
			@Param("startTime") java.time.LocalTime startTime,
			@Param("endTime") java.time.LocalTime endTime,
			@Param("excludeId") Long excludeId
	);
}

