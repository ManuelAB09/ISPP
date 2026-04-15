package es.us.meerkat.backend.dto.tutors;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlot {
    private LocalDateTime start;
    private LocalDateTime end;
    private boolean available;
}
