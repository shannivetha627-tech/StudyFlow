package com.nivetha.studentproductivity.session;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/study-sessions")
public class StudySessionController {

	private final StudySessionService studySessionService;

	public StudySessionController(StudySessionService studySessionService) {
		this.studySessionService = studySessionService;
	}

	@GetMapping
	public List<StudySession> getSessions(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
	) {
		if (date != null) {
			return studySessionService.getSessionsForDate(date);
		}
		if (start != null && end != null) {
			return studySessionService.getSessionsForRange(start, end);
		}
		return studySessionService.getAllSessions();
	}

	@GetMapping("/{id}")
	public StudySession getSession(@PathVariable Long id) {
		return studySessionService.getSession(id);
	}

	@PostMapping
	public ResponseEntity<StudySession> createSession(@Valid @RequestBody StudySessionRequest request) {
		StudySession session = studySessionService.createSession(request);
		URI location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(session.getId())
				.toUri();
		return ResponseEntity.created(location).body(session);
	}

	@PutMapping("/{id}")
	public StudySession updateSession(@PathVariable Long id, @Valid @RequestBody StudySessionRequest request) {
		return studySessionService.updateSession(id, request);
	}

	@PatchMapping("/{id}/complete")
	public StudySession toggleCompleted(@PathVariable Long id) {
		return studySessionService.toggleCompleted(id);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
		studySessionService.deleteSession(id);
		return ResponseEntity.noContent().build();
	}
}

