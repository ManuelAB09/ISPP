package es.us.meerkat.backend.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import es.us.meerkat.backend.dto.CreateFeedbackRequest;
import es.us.meerkat.backend.dto.FeedbackResponse;
import es.us.meerkat.backend.entity.Feedback;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/feedbacks")
@Tag(name = "Feedback", description = "Feedback profesor->alumno dentro de una comunidad")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @Operation(summary = "Dar feedback a un alumno (solo profesor/admin)")
    @ApiResponse(responseCode = "201", description = "Feedback creado")
    public ResponseEntity<FeedbackResponse> createFeedback(
            @PathVariable Long communityId,
            @Valid @RequestBody CreateFeedbackRequest request,
            @AuthenticationPrincipal Usuario usuario) {

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Feedback fb =
                    feedbackService.createFeedback(
                            usuario.getId(),
                            request.alumnoId(),
                            communityId,
                            request.contenido(),
                            request.calificacion());
            return ResponseEntity.status(HttpStatus.CREATED).body(FeedbackResponse.fromEntity(fb));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping
    @Operation(summary = "Listar feedbacks de una comunidad")
    public ResponseEntity<?> listFeedbacks(
            @PathVariable Long communityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<Feedback> feedbacks = feedbackService.listFeedbacksByCommunity(communityId, pageable);
        Page<FeedbackResponse> resp = feedbacks.map(FeedbackResponse::fromEntity);
        return ResponseEntity.ok(resp);
    }
}
