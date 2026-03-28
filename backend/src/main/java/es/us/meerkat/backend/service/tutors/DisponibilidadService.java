package es.us.meerkat.backend.service.tutors;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.us.meerkat.backend.dto.tutors.CreateDisponibilidadRequest;
import es.us.meerkat.backend.dto.tutors.DisponibilidadTutorResponse;
import es.us.meerkat.backend.entity.DisponibilidadTutor;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.repository.DisponibilidadTutorRepository;
import es.us.meerkat.backend.repository.TutorRepository;

/**
 * Servicio para gestionar disponibilidades horarias de tutores.
 *
 * <p>Permite a los tutores definir sus horarios disponibles para reservas de clases.
 */
@Service
public class DisponibilidadService {

    @Autowired private DisponibilidadTutorRepository disponibilidadRepository;

    @Autowired private TutorRepository tutorRepository;

    /** Crea una nueva disponibilidad horaria para un tutor. */
    @Transactional
    public DisponibilidadTutorResponse crearDisponibilidad(
            Long tutorId, CreateDisponibilidadRequest request, Long usuarioId) {

        Tutor tutor =
                tutorRepository
                        .findById(tutorId)
                        .orElseThrow(() -> new IllegalArgumentException("Tutor no encontrado"));

        // Verificar que el usuario sea el dueño del perfil de tutor
        if (!tutor.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para modificar disponibilidades de otro tutor");
        }

        // Validaciones
        if (request.getEsRecurrente()) {
            if (request.getDiaSemana() == null) {
                throw new IllegalArgumentException(
                        "Día de semana requerido para disponibilidad recurrente");
            }
        } else {
            if (request.getFechaPuntual() == null) {
                throw new IllegalArgumentException(
                        "Fecha específica requerida para disponibilidad puntual");
            }
        }

        if (!request.getHoraInicio().isBefore(request.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin");
        }

        validarNoSolapamiento(tutorId, request, null);

        DisponibilidadTutor disponibilidad =
                DisponibilidadTutor.builder()
                        .tutor(tutor)
                        .esRecurrente(request.getEsRecurrente())
                        .diaSemana(request.getDiaSemana())
                        .fechaPuntual(request.getFechaPuntual())
                        .horaInicio(request.getHoraInicio())
                        .horaFin(request.getHoraFin())
                        .modalidad(request.getModalidad())
                        .ubicacionPresencial(request.getUbicacionPresencial())
                        .activa(true)
                        .build();

        disponibilidad = disponibilidadRepository.save(disponibilidad);
        return mapToResponse(disponibilidad);
    }

    /** Obtiene todas las disponibilidades activas de un tutor. */
    @Transactional(readOnly = true)
    public List<DisponibilidadTutorResponse> getDisponibilidades(Long tutorId) {
        List<DisponibilidadTutor> disponibilidades =
                disponibilidadRepository.findByTutorIdAndActivaTrue(tutorId);
        return disponibilidades.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /** Obtiene disponibilidades para una fecha específica. */
    @Transactional(readOnly = true)
    public List<DisponibilidadTutorResponse> getDisponibilidadesPorFecha(
            Long tutorId, LocalDate fecha) {
        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        List<DisponibilidadTutor> disponibilidades =
                disponibilidadRepository.findDisponibilidadesPara(tutorId, dayOfWeek, fecha);
        return disponibilidades.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    /** Desactiva una disponibilidad. */
    @Transactional
    public void desactivarDisponibilidad(Long disponibilidadId, Long usuarioId) {
        DisponibilidadTutor disponibilidad =
                disponibilidadRepository
                        .findById(disponibilidadId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Disponibilidad no encontrada"));

        if (!disponibilidad.getTutor().getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para modificar esta disponibilidad");
        }

        disponibilidad.setActiva(false);
        disponibilidadRepository.save(disponibilidad);
    }

    /** Actualiza una disponibilidad existente. */
    @Transactional
    public DisponibilidadTutorResponse actualizarDisponibilidad(
            Long disponibilidadId, CreateDisponibilidadRequest request, Long usuarioId) {

        DisponibilidadTutor disponibilidad =
                disponibilidadRepository
                        .findById(disponibilidadId)
                        .orElseThrow(
                                () -> new IllegalArgumentException("Disponibilidad no encontrada"));

        if (!disponibilidad.getTutor().getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException(
                    "No tienes permisos para modificar esta disponibilidad");
        }

        // Validaciones (igual que en crear)
        if (request.getEsRecurrente()) {
            if (request.getDiaSemana() == null) {
                throw new IllegalArgumentException(
                        "Día de semana requerido para disponibilidad recurrente");
            }
        } else {
            if (request.getFechaPuntual() == null) {
                throw new IllegalArgumentException(
                        "Fecha específica requerida para disponibilidad puntual");
            }
        }

        if (!request.getHoraInicio().isBefore(request.getHoraFin())) {
            throw new IllegalArgumentException("La hora de inicio debe ser anterior a la de fin");
        }

        validarNoSolapamiento(disponibilidad.getTutor().getId(), request, disponibilidadId);

        disponibilidad.setEsRecurrente(request.getEsRecurrente());
        disponibilidad.setDiaSemana(request.getDiaSemana());
        disponibilidad.setFechaPuntual(request.getFechaPuntual());
        disponibilidad.setHoraInicio(request.getHoraInicio());
        disponibilidad.setHoraFin(request.getHoraFin());
        disponibilidad.setModalidad(request.getModalidad());
        disponibilidad.setUbicacionPresencial(request.getUbicacionPresencial());

        disponibilidad = disponibilidadRepository.save(disponibilidad);
        return mapToResponse(disponibilidad);
    }

    /** Elimina todas las disponibilidades de un tutor (al desactivar el perfil, por ej). */
    @Transactional
    public void eliminarTodasDisponibilidades(Long tutorId) {
        disponibilidadRepository.deleteAllByTutorId(tutorId);
    }

    // ==================== Métodos Privados ====================

    private void validarNoSolapamiento(
            Long tutorId, CreateDisponibilidadRequest request, Long disponibilidadIdExcluir) {
        final List<DisponibilidadTutor> existentes =
                disponibilidadRepository.findByTutorIdAndActivaTrue(tutorId);
        final List<DisponibilidadTutor> seguras =
                existentes == null ? Collections.emptyList() : existentes;

        for (DisponibilidadTutor existente : seguras) {
            if (disponibilidadIdExcluir != null
                    && disponibilidadIdExcluir.equals(existente.getId())) {
                continue;
            }

            if (seSolapan(existente, request)) {
                throw new IllegalArgumentException(
                        "Esta disponibilidad se solapa con otra existente en el mismo día");
            }
        }
    }

    private boolean seSolapan(DisponibilidadTutor existente, CreateDisponibilidadRequest request) {
        if (!coincidenEnMismoDia(existente, request)) {
            return false;
        }

        return request.getHoraInicio().isBefore(existente.getHoraFin())
                && request.getHoraFin().isAfter(existente.getHoraInicio());
    }

    private boolean coincidenEnMismoDia(
            DisponibilidadTutor existente, CreateDisponibilidadRequest request) {
        final Boolean requestEsRecurrente = request.getEsRecurrente();
        final Boolean existenteEsRecurrente = existente.getEsRecurrente();

        if (Boolean.TRUE.equals(requestEsRecurrente)
                && Boolean.TRUE.equals(existenteEsRecurrente)) {
            return request.getDiaSemana() != null
                    && existente.getDiaSemana() != null
                    && request.getDiaSemana().equals(existente.getDiaSemana());
        }

        if (Boolean.FALSE.equals(requestEsRecurrente)
                && Boolean.FALSE.equals(existenteEsRecurrente)) {
            return request.getFechaPuntual() != null
                    && existente.getFechaPuntual() != null
                    && request.getFechaPuntual()
                            .toLocalDate()
                            .equals(existente.getFechaPuntual().toLocalDate());
        }

        if (Boolean.TRUE.equals(requestEsRecurrente)
                && Boolean.FALSE.equals(existenteEsRecurrente)) {
            return request.getDiaSemana() != null
                    && existente.getFechaPuntual() != null
                    && request.getDiaSemana().equals(existente.getFechaPuntual().getDayOfWeek());
        }

        if (Boolean.FALSE.equals(requestEsRecurrente)
                && Boolean.TRUE.equals(existenteEsRecurrente)) {
            return request.getFechaPuntual() != null
                    && existente.getDiaSemana() != null
                    && request.getFechaPuntual().getDayOfWeek().equals(existente.getDiaSemana());
        }

        return false;
    }

    private DisponibilidadTutorResponse mapToResponse(DisponibilidadTutor disponibilidad) {
        return DisponibilidadTutorResponse.builder()
                .id(disponibilidad.getId())
                .esRecurrente(disponibilidad.getEsRecurrente())
                .diaSemana(disponibilidad.getDiaSemana())
                .fechaPuntual(disponibilidad.getFechaPuntual())
                .horaInicio(disponibilidad.getHoraInicio())
                .horaFin(disponibilidad.getHoraFin())
                .modalidad(disponibilidad.getModalidad())
                .ubicacionPresencial(disponibilidad.getUbicacionPresencial())
                .activa(disponibilidad.getActiva())
                .createdAt(disponibilidad.getCreatedAt())
                .build();
    }
}
