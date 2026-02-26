package es.us.meerkat.backend.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "solicitudes_comunidad")
public class SolicitudComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne private Usuario solicitante;

    @ManyToOne private Comunidad comunidad;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EstadoSolicitud estado = EstadoSolicitud.PENDIENTE;

    @Size(max = 500, message = "El mensaje no puede exceder 500 caracteres")
    private String mensaje;

    @ManyToOne private Usuario respondidaPor;

    @Builder.Default private LocalDateTime fechaSolicitud = LocalDateTime.now();

    private LocalDateTime fechaRespuesta;
}
