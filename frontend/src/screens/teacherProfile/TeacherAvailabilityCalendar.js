import { useEffect, useMemo, useState } from "react";
import {
  getDisponibilidadTutorFecha,
  getHorariosOcupadosContratacion,
} from "../../api/solicitudContratacion";
import { getDisponibilidades } from "../../api/disponibilidad";
import "./TeacherAvailabilityCalendar.css";

const WEEK_DAYS = ["Lun", "Mar", "Mie", "Jue", "Vie", "Sab", "Dom"];

const formatDateKey = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

const timeToMinutes = (time) => {
  if (!time) return 0;
  const [h, m] = String(time).slice(0, 5).split(":").map(Number);
  return h * 60 + m;
};

const minutesToTime = (minutes) => {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
};

const normalizeRange = (item) => ({
  start: String(item?.start ?? item?.horaInicio ?? "").slice(0, 5),
  end: String(item?.end ?? item?.horaFin ?? "").slice(0, 5),
});

const normalizeDateKey = (value) => {
  if (!value) return "";
  return String(value).slice(0, 10);
};

const mergeUniqueRanges = (rangesA, rangesB) => {
  const seen = new Set();
  const merged = [...rangesA, ...rangesB].filter((slot) => {
    const key = `${slot.start}-${slot.end}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });

  return merged.sort((a, b) => timeToMinutes(a.start) - timeToMinutes(b.start));
};

const buildPunctualByDate = (allDisponibilidades = []) => {
  return allDisponibilidades.reduce((acc, item) => {
    if (item?.esRecurrente !== false) return acc;
    const dayKey = normalizeDateKey(item.fechaPuntual);
    if (!dayKey) return acc;

    const range = normalizeRange(item);
    if (!range.start || !range.end) return acc;

    if (!acc[dayKey]) acc[dayKey] = [];
    acc[dayKey].push(range);
    return acc;
  }, {});
};

const subtractIntervals = (base, blockers) => {
  const baseStart = timeToMinutes(base.start);
  const baseEnd = timeToMinutes(base.end);

  if (baseEnd <= baseStart) return [];

  const sorted = [...blockers]
    .map((b) => ({ start: timeToMinutes(b.start), end: timeToMinutes(b.end) }))
    .filter((b) => b.end > b.start)
    .sort((a, b) => a.start - b.start);

  const result = [];
  let cursor = baseStart;

  sorted.forEach((block) => {
    const overlapStart = Math.max(cursor, block.start);
    const overlapEnd = Math.min(baseEnd, block.end);

    if (overlapStart > cursor) {
      result.push({ start: minutesToTime(cursor), end: minutesToTime(overlapStart) });
    }

    if (overlapEnd > cursor) {
      cursor = overlapEnd;
    }
  });

  if (cursor < baseEnd) {
    result.push({ start: minutesToTime(cursor), end: minutesToTime(baseEnd) });
  }

  return result;
};

const getFreeSlots = (availability, occupied) => {
  if (!availability.length) return [];

  const occupiedRanges = occupied.map(normalizeRange);

  return availability
    .map(normalizeRange)
    .flatMap((slot) => subtractIntervals(slot, occupiedRanges))
    .filter((slot) => timeToMinutes(slot.end) > timeToMinutes(slot.start));
};

const getMonthDays = (monthDate) => {
  const year = monthDate.getFullYear();
  const month = monthDate.getMonth();

  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);

  // Convertir domingo (0) a 6 para comenzar en lunes.
  const leading = (firstDay.getDay() + 6) % 7;
  const totalDays = lastDay.getDate();

  const cells = [];
  for (let i = 0; i < leading; i += 1) {
    cells.push(null);
  }

  for (let day = 1; day <= totalDays; day += 1) {
    cells.push(new Date(year, month, day));
  }

  while (cells.length % 7 !== 0) {
    cells.push(null);
  }

  return cells;
};

const monthLabel = (date) =>
  date.toLocaleDateString("es-ES", { month: "long", year: "numeric" });

const isFutureDate = (dateKey) => {
  const selected = new Date(`${dateKey}T00:00:00`);
  const tomorrow = new Date();
  tomorrow.setHours(0, 0, 0, 0);
  tomorrow.setDate(tomorrow.getDate() + 1);
  return selected >= tomorrow;
};

const TeacherAvailabilityCalendar = ({ tutorId, canHire, onPickSlot }) => {
  const [currentMonth, setCurrentMonth] = useState(() => {
    const now = new Date();
    return new Date(now.getFullYear(), now.getMonth(), 1);
  });
  const [selectedDate, setSelectedDate] = useState(formatDateKey(new Date()));
  const [loadingMonth, setLoadingMonth] = useState(false);
  const [monthData, setMonthData] = useState({});

  const monthDays = useMemo(() => getMonthDays(currentMonth), [currentMonth]);

  useEffect(() => {
    let cancelled = false;

    const loadMonth = async () => {
      setLoadingMonth(true);

      const year = currentMonth.getFullYear();
      const month = currentMonth.getMonth();
      const lastDay = new Date(year, month + 1, 0).getDate();

      const dates = Array.from({ length: lastDay }, (_, idx) => {
        const d = new Date(year, month, idx + 1);
        return formatDateKey(d);
      });

      try {
        let puntualByDate = {};
        try {
          const allDisponibilidadesRes = await getDisponibilidades(tutorId);
          const allDisponibilidades = Array.isArray(allDisponibilidadesRes?.data)
            ? allDisponibilidadesRes.data
            : Array.isArray(allDisponibilidadesRes)
            ? allDisponibilidadesRes
            : [];
          puntualByDate = buildPunctualByDate(allDisponibilidades);
        } catch {
          puntualByDate = {};
        }

        const responses = await Promise.all(
          dates.map(async (dateKey) => {
            try {
              const [dispRes, occRes] = await Promise.all([
                getDisponibilidadTutorFecha(tutorId, dateKey),
                getHorariosOcupadosContratacion(tutorId, dateKey),
              ]);

              const availability = Array.isArray(dispRes?.data) ? dispRes.data : [];
              const occupied = Array.isArray(occRes?.data) ? occRes.data : [];

              return {
                dateKey,
                data: {
                  availability: availability.map(normalizeRange),
                  occupied: occupied.map(normalizeRange),
                  free: getFreeSlots(availability, occupied),
                },
              };
            } catch {
              return {
                dateKey,
                data: {
                  availability: [],
                  occupied: [],
                  free: [],
                },
              };
            }
          })
        );

        if (!cancelled) {
          const next = {};
          responses.forEach(({ dateKey, data }) => {
            const puntualRanges = puntualByDate[dateKey] || [];
            const mergedAvailability = mergeUniqueRanges(data.availability, puntualRanges);
            next[dateKey] = {
              ...data,
              availability: mergedAvailability,
              free: getFreeSlots(mergedAvailability, data.occupied),
            };
          });
          setMonthData(next);

          if (!next[selectedDate]) {
            const firstWithData = Object.keys(next).find(
              (k) => next[k].availability.length > 0 || next[k].occupied.length > 0
            );
            if (firstWithData) setSelectedDate(firstWithData);
          }
        }
      } finally {
        if (!cancelled) setLoadingMonth(false);
      }
    };

    if (tutorId) loadMonth();

    return () => {
      cancelled = true;
    };
  }, [currentMonth, tutorId, selectedDate]);

  const selectedData = monthData[selectedDate] || { availability: [], occupied: [], free: [] };

  const handlePrevMonth = () => {
    setCurrentMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() - 1, 1));
  };

  const handleNextMonth = () => {
    setCurrentMonth((prev) => new Date(prev.getFullYear(), prev.getMonth() + 1, 1));
  };

  const handleDayClick = (date) => {
    if (!date) return;
    const dayKey = formatDateKey(date);
    setSelectedDate(dayKey);

    const day = monthData[dayKey];
    if (canHire && day?.free?.length > 0 && isFutureDate(dayKey)) {
      const firstFree = day.free[0];
      onPickSlot?.({
        dia: dayKey,
        horaInicio: firstFree.start,
        horaFin: firstFree.end,
      });
    }
  };

  return (
    <section className="tp-calendar">
      <div className="tp-calendar__head">
        <h2 className="tp-section-title">Calendario de tutorias</h2>
        <div className="tp-calendar__month-nav">
          <button className="tp-calendar__nav-btn" onClick={handlePrevMonth} aria-label="Mes anterior">
            ‹
          </button>
          <span className="tp-calendar__month-label">{monthLabel(currentMonth)}</span>
          <button className="tp-calendar__nav-btn" onClick={handleNextMonth} aria-label="Mes siguiente">
            ›
          </button>
        </div>
      </div>

      <div className="tp-calendar__legend">
        <span className="tp-calendar__legend-item tp-calendar__legend-item--booked">Tutorias ocupadas</span>
        <span className="tp-calendar__legend-item tp-calendar__legend-item--free">Huecos libres</span>
        {canHire && <span className="tp-calendar__legend-help">Haz clic en un hueco libre para contratar</span>}
      </div>

      <div className="tp-calendar__grid">
        {WEEK_DAYS.map((day) => (
          <div key={day} className="tp-calendar__weekday">
            {day}
          </div>
        ))}

        {monthDays.map((date, idx) => {
          if (!date) {
            return <div key={`empty-${idx}`} className="tp-calendar__cell tp-calendar__cell--empty" />;
          }

          const dayKey = formatDateKey(date);
          const dayData = monthData[dayKey] || { occupied: [], free: [] };
          const isSelected = dayKey === selectedDate;

          return (
            <button
              key={dayKey}
              className={`tp-calendar__cell ${isSelected ? "tp-calendar__cell--selected" : ""}`}
              onClick={() => handleDayClick(date)}
            >
              <span className="tp-calendar__day-number">{date.getDate()}</span>
              <div className="tp-calendar__markers">
                {dayData.occupied.slice(0, 1).map((slot, i) => (
                  <span key={`o-${i}`} className="tp-calendar__chip tp-calendar__chip--booked">
                    {slot.start}-{slot.end}
                  </span>
                ))}
                {dayData.free.slice(0, 1).map((slot, i) => (
                  <span key={`f-${i}`} className="tp-calendar__chip tp-calendar__chip--free">
                    {slot.start}-{slot.end}
                  </span>
                ))}
                {dayData.occupied.length + dayData.free.length > 2 && (
                  <span className="tp-calendar__more">
                    +{dayData.occupied.length + dayData.free.length - 2}
                  </span>
                )}
              </div>
            </button>
          );
        })}
      </div>

      <div className="tp-calendar__details">
        <h3 className="tp-calendar__details-title">Detalle de {selectedDate}</h3>

        {loadingMonth ? <p className="tp-calendar__loading">Cargando calendario...</p> : null}

        {!loadingMonth && selectedData.occupied.length === 0 && selectedData.free.length === 0 ? (
          <p className="tp-calendar__empty">Sin tutorias ni disponibilidad registrada para este dia.</p>
        ) : null}

        {selectedData.occupied.length > 0 && (
          <div className="tp-calendar__detail-group">
            <h4>Tutorias ocupadas</h4>
            <div className="tp-calendar__slot-list">
              {selectedData.occupied.map((slot, index) => (
                <span key={`occ-${index}`} className="tp-calendar__slot tp-calendar__slot--booked">
                  {slot.start} - {slot.end}
                </span>
              ))}
            </div>
          </div>
        )}

        {selectedData.free.length > 0 && (
          <div className="tp-calendar__detail-group">
            <h4>Huecos libres</h4>
            <div className="tp-calendar__slot-list">
              {selectedData.free.map((slot, index) => (
                <button
                  key={`free-${index}`}
                  type="button"
                  className={`tp-calendar__slot tp-calendar__slot--free ${canHire ? "tp-calendar__slot--clickable" : ""}`}
                  onClick={() =>
                    canHire &&
                    isFutureDate(selectedDate) &&
                    onPickSlot?.({
                      dia: selectedDate,
                      horaInicio: slot.start,
                      horaFin: slot.end,
                    })
                  }
                >
                  {slot.start} - {slot.end}
                </button>
              ))}
            </div>
          </div>
        )}
      </div>
    </section>
  );
};

export default TeacherAvailabilityCalendar;
