package com.nivetha.studentproductivity.session;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nivetha.studentproductivity.task.TaskAccessDeniedException;
import com.nivetha.studentproductivity.task.TaskNotFoundException;
import com.nivetha.studentproductivity.user.User;
import com.nivetha.studentproductivity.user.UserService;

@Service
public class StudySessionService {

	private final StudySessionRepository studySessionRepository;
	private final UserService userService;

	public StudySessionService(StudySessionRepository studySessionRepository, UserService userService) {
		this.studySessionRepository = studySessionRepository;
		this.userService = userService;
	}

	@Transactional(readOnly = true)
	public List<StudySession> getAllSessions() {
		User currentUser = userService.getAuthenticatedUser();
		return studySessionRepository.findByUserOrderBySessionDateAscStartTimeAsc(currentUser);
	}

	@Transactional(readOnly = true)
	public List<StudySession> getSessionsForDate(LocalDate date) {
		User currentUser = userService.getAuthenticatedUser();
		return studySessionRepository.findByUserAndSessionDateOrderByStartTimeAsc(currentUser, date);
	}

	@Transactional(readOnly = true)
	public List<StudySession> getSessionsForRange(LocalDate start, LocalDate end) {
		if (start.isAfter(end)) {
			throw new IllegalArgumentException("Start date must be on or before end date");
		}
		User currentUser = userService.getAuthenticatedUser();
		return studySessionRepository.findByUserAndSessionDateBetweenOrderBySessionDateAscStartTimeAsc(
				currentUser, start, end
		);
	}

	@Transactional(readOnly = true)
	public StudySession getSession(Long id) {
		User currentUser = userService.getAuthenticatedUser();
		StudySession session = findSession(id);
		verifyOwnership(session, currentUser);
		return session;
	}

	@Transactional
	public StudySession createSession(StudySessionRequest request) {
		User currentUser = userService.getAuthenticatedUser();
		validateTimes(request);

		List<StudySession> overlaps = studySessionRepository.findOverlappingSessions(
				currentUser, request.sessionDate(), request.startTime(), request.endTime(), null
		);
		if (!overlaps.isEmpty()) {
			throw new IllegalArgumentException("Session overlaps with existing session: " + overlaps.get(0).getTopic());
		}

		StudySession session = new StudySession();
		session.setUser(currentUser);
		updateFields(session, request);
		return studySessionRepository.save(session);
	}

	@Transactional
	public StudySession updateSession(Long id, StudySessionRequest request) {
		User currentUser = userService.getAuthenticatedUser();
		StudySession session = findSession(id);
		verifyOwnership(session, currentUser);
		validateTimes(request);

		List<StudySession> overlaps = studySessionRepository.findOverlappingSessions(
				currentUser, request.sessionDate(), request.startTime(), request.endTime(), id
		);
		if (!overlaps.isEmpty()) {
			throw new IllegalArgumentException("Session overlaps with existing session: " + overlaps.get(0).getTopic());
		}

		updateFields(session, request);
		return studySessionRepository.save(session);
	}

	@Transactional
	public StudySession toggleCompleted(Long id) {
		User currentUser = userService.getAuthenticatedUser();
		StudySession session = findSession(id);
		verifyOwnership(session, currentUser);
		session.setCompleted(!session.isCompleted());
		return studySessionRepository.save(session);
	}

	@Transactional
	public void deleteSession(Long id) {
		User currentUser = userService.getAuthenticatedUser();
		StudySession session = findSession(id);
		verifyOwnership(session, currentUser);
		studySessionRepository.delete(session);
	}

	private StudySession findSession(Long id) {
		return studySessionRepository.findById(id)
				.orElseThrow(() -> new TaskNotFoundException(id));
	}

	private void verifyOwnership(StudySession session, User currentUser) {
		if (session.getUser() == null || !session.getUser().getId().equals(currentUser.getId())) {
			throw new TaskAccessDeniedException("You do not have permission to access this session");
		}
	}

	private void validateTimes(StudySessionRequest request) {
		if (request.startTime().isAfter(request.endTime()) || request.startTime().equals(request.endTime())) {
			throw new IllegalArgumentException("Start time must be before end time");
		}
	}

	private void updateFields(StudySession session, StudySessionRequest request) {
		session.setSubject(request.subject().trim());
		session.setTopic(request.topic().trim());
		session.setDescription(request.description() == null ? null : request.description().trim());
		session.setSessionDate(request.sessionDate());
		session.setStartTime(request.startTime());
		session.setEndTime(request.endTime());
		session.setReminderEnabled(request.reminderEnabled() == null || request.reminderEnabled());
		session.setReminderMinutes(request.reminderMinutes() == null ? 10 : request.reminderMinutes());
		if (request.completed() != null) {
			session.setCompleted(request.completed());
		}
	}
}
