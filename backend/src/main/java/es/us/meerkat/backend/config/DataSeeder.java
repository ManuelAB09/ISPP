package es.us.meerkat.backend.config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import es.us.meerkat.backend.entity.AsistenciaEvento;
import es.us.meerkat.backend.entity.Categoria;
import es.us.meerkat.backend.entity.Comunidad;
import es.us.meerkat.backend.entity.EstadoAsistencia;
import es.us.meerkat.backend.entity.EstadoComunidad;
import es.us.meerkat.backend.entity.Evento;
import es.us.meerkat.backend.entity.Institution;
import es.us.meerkat.backend.entity.MiembroComunidad;
import es.us.meerkat.backend.entity.RolComunidad;
import es.us.meerkat.backend.entity.TipoGrupo;
import es.us.meerkat.backend.entity.TipoPlan;
import es.us.meerkat.backend.entity.TipoPlanComunidad;
import es.us.meerkat.backend.entity.TipoPlanCorporativo;
import es.us.meerkat.backend.entity.Tutor;
import es.us.meerkat.backend.entity.Ubicacion;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.AsistenciaEventoRepository;
import es.us.meerkat.backend.repository.CategoriaRepository;
import es.us.meerkat.backend.repository.ComunidadRepository;
import es.us.meerkat.backend.repository.EventoRepository;
import es.us.meerkat.backend.repository.InstitutionRepository;
import es.us.meerkat.backend.repository.MiembroComunidadRepository;
import es.us.meerkat.backend.repository.TutorRepository;
import es.us.meerkat.backend.repository.UbicacionRepository;
import es.us.meerkat.backend.repository.UsuarioRepository;

/**
 * Seeder de datos iniciales para desarrollo.
 *
 * <p>Se ejecuta únicamente con el perfil por defecto (H2 en memoria). No se activa en los perfiles
 * "staging" ni "production".
 */
@Configuration
public class DataSeeder {

    @Bean
    @Profile("!staging & !production")
    CommandLineRunner seedDatabase(
            final UsuarioRepository usuarioRepo,
            final TutorRepository tutorRepo,
            final UbicacionRepository ubicacionRepo,
            final ComunidadRepository comunidadRepo,
            final MiembroComunidadRepository miembroRepo,
            final CategoriaRepository categoriaRepo,
            final EventoRepository eventoRepo,
            final AsistenciaEventoRepository asistenciaRepo,
            final InstitutionRepository institutionRepo,
            final BCryptPasswordEncoder passwordEncoder) {

        return args -> {
            // Evitar duplicados si ya existen datos
            if (usuarioRepo.count() > 0) {
                return;
            }

            // ============================
            // 1. USUARIOS
            // ============================
            Usuario u1 = new Usuario();
            u1.setEmail("admin@meerkat.es");
            u1.setPassword(passwordEncoder.encode("Admin1234!"));
            u1.setNombre("Admin MeerKat");
            u1.setFoto("https://i.pravatar.cc/150?u=admin");
            u1.setBio("Administrador de la plataforma MeerKat.");
            u1.setIntereses(List.of("Gestión", "Tecnología", "Educación"));
            u1.setVisibleEnListados(true);
            u1.setEsTutor(false);
            u1.setPlan(TipoPlan.FREE);

            Usuario u2 = new Usuario();
            u2.setEmail("maria.garcia@alum.us.es");
            u2.setPassword(passwordEncoder.encode("Maria1234!"));
            u2.setNombre("María García López");
            u2.setFoto("https://i.pravatar.cc/150?u=maria");
            u2.setBio("Estudiante de Ingeniería del Software, apasionada por el desarrollo web.");
            u2.setIntereses(List.of("Programación", "Diseño Web", "Bases de Datos"));
            u2.setVisibleEnListados(true);
            u2.setEsTutor(false);
            u2.setPlan(TipoPlan.FREE);

            Usuario u3 = new Usuario();
            u3.setEmail("carlos.ruiz@alum.us.es");
            u3.setPassword(passwordEncoder.encode("Carlos1234!"));
            u3.setNombre("Carlos Ruiz Martín");
            u3.setFoto("https://i.pravatar.cc/150?u=carlos");
            u3.setBio("Estudiante de 3º de ISPP. Me encanta Java y Spring Boot.");
            u3.setIntereses(List.of("Java", "Spring Boot", "Microservicios"));
            u3.setVisibleEnListados(true);
            u3.setEsTutor(true);
            u3.setPlan(TipoPlan.FREE);

            Usuario u4 = new Usuario();
            u4.setEmail("laura.fernandez@alum.us.es");
            u4.setPassword(passwordEncoder.encode("Laura1234!"));
            u4.setNombre("Laura Fernández Pérez");
            u4.setFoto("https://i.pravatar.cc/150?u=laura");
            u4.setBio("Apasionada de la inteligencia artificial y el aprendizaje automático.");
            u4.setIntereses(List.of("IA", "Machine Learning", "Python"));
            u4.setVisibleEnListados(true);
            u4.setEsTutor(true);
            u4.setPlan(TipoPlan.FREE);

            Usuario u5 = new Usuario();
            u5.setEmail("pedro.sanchez@alum.us.es");
            u5.setPassword(passwordEncoder.encode("Pedro1234!"));
            u5.setNombre("Pedro Sánchez Díaz");
            u5.setFoto("https://i.pravatar.cc/150?u=pedro");
            u5.setBio("Estudiante de último año, interesado en ciberseguridad.");
            u5.setIntereses(List.of("Ciberseguridad", "Redes", "Linux"));
            u5.setVisibleEnListados(true);
            u5.setEsTutor(true);
            u5.setPlan(TipoPlan.FREE);

            Usuario u6 = new Usuario();
            u6.setEmail("ana.lopez@alum.us.es");
            u6.setPassword(passwordEncoder.encode("Ana12345!"));
            u6.setNombre("Ana López Romero");
            u6.setFoto("https://i.pravatar.cc/150?u=ana");
            u6.setBio("Estudiante de Matemáticas y Ciencia de Datos.");
            u6.setIntereses(List.of("Estadística", "R", "Visualización de Datos"));
            u6.setVisibleEnListados(true);
            u6.setEsTutor(true);
            u6.setPlan(TipoPlan.FREE);

            Usuario u7 = new Usuario();
            u7.setEmail("jorge.moreno@alum.us.es");
            u7.setPassword(passwordEncoder.encode("Jorge1234!"));
            u7.setNombre("Jorge Moreno Castro");
            u7.setFoto("https://i.pravatar.cc/150?u=jorge");
            u7.setBio("Desarrollador frontend con pasión por React y TypeScript.");
            u7.setIntereses(List.of("React", "TypeScript", "UX/UI"));
            u7.setVisibleEnListados(true);
            u7.setEsTutor(false);
            u7.setPlan(TipoPlan.FREE);

            Usuario u8 = new Usuario();
            u8.setEmail("sara.jimenez@alum.us.es");
            u8.setPassword(passwordEncoder.encode("Sara12345!"));
            u8.setNombre("Sara Jiménez Torres");
            u8.setFoto("https://i.pravatar.cc/150?u=sara");
            u8.setBio("Amante de las bases de datos y la arquitectura de software.");
            u8.setIntereses(List.of("SQL", "NoSQL", "Arquitectura"));
            u8.setVisibleEnListados(false);
            u8.setEsTutor(false);
            u8.setPlan(TipoPlan.FREE);

            List<Usuario> usuarios = usuarioRepo.saveAll(List.of(u1, u2, u3, u4, u5, u6, u7, u8));
            usuarioRepo.flush();

            // ============================
            // 2. UBICACIONES
            // ============================
            Ubicacion ub1 =
                    Ubicacion.builder()
                            .nombre("Biblioteca ETSII")
                            .direccion("Av. Reina Mercedes s/n, Sevilla")
                            .latitud(37.3588)
                            .longitud(-5.9868)
                            .tipo("biblioteca")
                            .coste("gratis")
                            .build();

            Ubicacion ub2 =
                    Ubicacion.builder()
                            .nombre("Aula A0.10 - ETSII")
                            .direccion("Av. Reina Mercedes s/n, Sevilla")
                            .latitud(37.3582)
                            .longitud(-5.9872)
                            .tipo("aula")
                            .coste("gratis")
                            .build();

            Ubicacion ub3 =
                    Ubicacion.builder()
                            .nombre("Salón de Actos - ETSII")
                            .direccion("Av. Reina Mercedes s/n, Sevilla")
                            .latitud(37.3585)
                            .longitud(-5.9865)
                            .tipo("auditorio")
                            .coste("gratis")
                            .build();

            Ubicacion ub4 =
                    Ubicacion.builder()
                            .nombre("Cafetería ETSII")
                            .direccion("Av. Reina Mercedes s/n, Sevilla")
                            .latitud(37.3580)
                            .longitud(-5.9875)
                            .tipo("cafeteria")
                            .coste("de pago")
                            .build();

            Ubicacion ub5 =
                    Ubicacion.builder()
                            .nombre("Biblioteca General US")
                            .direccion("C/ San Fernando 4, Sevilla")
                            .latitud(37.3770)
                            .longitud(-5.9870)
                            .tipo("biblioteca")
                            .coste("gratis")
                            .build();

            List<Ubicacion> ubicaciones = ubicacionRepo.saveAll(List.of(ub1, ub2, ub3, ub4, ub5));

            // ============================
            // 3. COMUNIDADES
            // ============================
            Comunidad c1 =
                    Comunidad.builder()
                            .nombre("ISPP - Grupo MeerKat")
                            .descripcion(
                                    "Comunidad oficial del grupo MeerKat para la asignatura "
                                            + "Ingeniería del Software y Práctica Profesional.")
                            .tipoGrupo(TipoGrupo.GRUPO_PRIVADO)
                            .estado(EstadoComunidad.ACTIVA)
                            .tipoPlan(TipoPlanComunidad.PREMIUM)
                            .maxMiembros(20)
                            .creador(u2)
                            .build();

            Comunidad c2 =
                    Comunidad.builder()
                            .nombre("Desarrollo Web Full Stack")
                            .descripcion(
                                    "Grupo de estudio de tecnologías web: HTML, CSS, "
                                            + "JavaScript, React, Node.js y Spring Boot.")
                            .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                            .estado(EstadoComunidad.ACTIVA)
                            .tipoPlan(TipoPlanComunidad.FREE)
                            .maxMiembros(50)
                            .creador(u3)
                            .build();

            Comunidad c3 =
                    Comunidad.builder()
                            .nombre("Inteligencia Artificial US")
                            .descripcion(
                                    "Comunidad para estudiantes interesados en IA, "
                                            + "Machine Learning y Deep Learning.")
                            .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                            .estado(EstadoComunidad.ACTIVA)
                            .tipoPlan(TipoPlanComunidad.FREE)
                            .maxMiembros(100)
                            .creador(u4)
                            .build();

            Comunidad c4 =
                    Comunidad.builder()
                            .nombre("Ciberseguridad & Hacking Ético")
                            .descripcion(
                                    "Grupo privado para practicar CTFs y "
                                            + "compartir recursos de ciberseguridad.")
                            .tipoGrupo(TipoGrupo.GRUPO_PRIVADO)
                            .estado(EstadoComunidad.ACTIVA)
                            .tipoPlan(TipoPlanComunidad.PREMIUM)
                            .maxMiembros(30)
                            .creador(u5)
                            .build();

            Comunidad c5 =
                    Comunidad.builder()
                            .nombre("Matemáticas y Estadística")
                            .descripcion(
                                    "Comunidad para resolver dudas y preparar exámenes "
                                            + "de asignaturas de matemáticas.")
                            .tipoGrupo(TipoGrupo.COMUNIDAD_PUBLICA)
                            .estado(EstadoComunidad.ACTIVA)
                            .tipoPlan(TipoPlanComunidad.FREE)
                            .maxMiembros(80)
                            .creador(u6)
                            .build();

            List<Comunidad> comunidades = comunidadRepo.saveAll(List.of(c1, c2, c3, c4, c5));

            // ============================
            // 4. CATEGORÍAS DE COMUNIDADES
            // ============================
            Categoria cat1 =
                    Categoria.builder()
                            .nombre("Sprint 1")
                            .descripcion("Tareas del Sprint 1")
                            .orden(1)
                            .comunidad(c1)
                            .build();
            Categoria cat2 =
                    Categoria.builder()
                            .nombre("Sprint 2")
                            .descripcion("Tareas del Sprint 2")
                            .orden(2)
                            .comunidad(c1)
                            .build();
            Categoria cat3 =
                    Categoria.builder()
                            .nombre("Frontend")
                            .descripcion("Recursos de frontend")
                            .orden(1)
                            .comunidad(c2)
                            .build();
            Categoria cat4 =
                    Categoria.builder()
                            .nombre("Backend")
                            .descripcion("Recursos de backend")
                            .orden(2)
                            .comunidad(c2)
                            .build();
            Categoria cat5 =
                    Categoria.builder()
                            .nombre("Proyectos ML")
                            .descripcion("Proyectos de Machine Learning")
                            .orden(1)
                            .comunidad(c3)
                            .build();
            Categoria cat6 =
                    Categoria.builder()
                            .nombre("CTFs")
                            .descripcion("Retos Capture The Flag")
                            .orden(1)
                            .comunidad(c4)
                            .build();
            Categoria cat7 =
                    Categoria.builder()
                            .nombre("Álgebra")
                            .descripcion("Álgebra lineal y abstracta")
                            .orden(1)
                            .comunidad(c5)
                            .build();
            Categoria cat8 =
                    Categoria.builder()
                            .nombre("Cálculo")
                            .descripcion("Cálculo diferencial e integral")
                            .orden(2)
                            .comunidad(c5)
                            .build();

            categoriaRepo.saveAll(List.of(cat1, cat2, cat3, cat4, cat5, cat6, cat7, cat8));

            // ============================
            // 5. MIEMBROS DE COMUNIDADES
            // ============================
            // c1 - ISPP (creador: u2=María)
            MiembroComunidad m1 =
                    MiembroComunidad.builder()
                            .usuario(u2)
                            .comunidad(c1)
                            .rol(RolComunidad.ADMIN)
                            .build();
            MiembroComunidad m2 =
                    MiembroComunidad.builder()
                            .usuario(u3)
                            .comunidad(c1)
                            .rol(RolComunidad.MIEMBRO)
                            .build();
            MiembroComunidad m3 =
                    MiembroComunidad.builder()
                            .usuario(u7)
                            .comunidad(c1)
                            .rol(RolComunidad.MIEMBRO)
                            .build();
            MiembroComunidad m4 =
                    MiembroComunidad.builder()
                            .usuario(u8)
                            .comunidad(c1)
                            .rol(RolComunidad.MIEMBRO)
                            .build();

            // c2 - Full Stack (creador: u3=Carlos)
            MiembroComunidad m5 =
                    MiembroComunidad.builder()
                            .usuario(u3)
                            .comunidad(c2)
                            .rol(RolComunidad.ADMIN)
                            .build();
            MiembroComunidad m6 =
                    MiembroComunidad.builder()
                            .usuario(u2)
                            .comunidad(c2)
                            .rol(RolComunidad.MIEMBRO)
                            .build();
            MiembroComunidad m7 =
                    MiembroComunidad.builder()
                            .usuario(u7)
                            .comunidad(c2)
                            .rol(RolComunidad.MIEMBRO)
                            .build();

            // c3 - IA (creador: u4=Laura)
            MiembroComunidad m8 =
                    MiembroComunidad.builder()
                            .usuario(u4)
                            .comunidad(c3)
                            .rol(RolComunidad.ADMIN)
                            .build();
            MiembroComunidad m9 =
                    MiembroComunidad.builder()
                            .usuario(u6)
                            .comunidad(c3)
                            .rol(RolComunidad.MIEMBRO)
                            .build();
            MiembroComunidad m10 =
                    MiembroComunidad.builder()
                            .usuario(u3)
                            .comunidad(c3)
                            .rol(RolComunidad.MIEMBRO)
                            .build();

            // c4 - Ciberseguridad (creador: u5=Pedro)
            MiembroComunidad m11 =
                    MiembroComunidad.builder()
                            .usuario(u5)
                            .comunidad(c4)
                            .rol(RolComunidad.ADMIN)
                            .build();
            MiembroComunidad m12 =
                    MiembroComunidad.builder()
                            .usuario(u3)
                            .comunidad(c4)
                            .rol(RolComunidad.MIEMBRO)
                            .build();

            // c5 - Matemáticas (creador: u6=Ana)
            MiembroComunidad m13 =
                    MiembroComunidad.builder()
                            .usuario(u6)
                            .comunidad(c5)
                            .rol(RolComunidad.ADMIN)
                            .build();
            MiembroComunidad m14 =
                    MiembroComunidad.builder()
                            .usuario(u4)
                            .comunidad(c5)
                            .rol(RolComunidad.MIEMBRO)
                            .build();
            MiembroComunidad m15 =
                    MiembroComunidad.builder()
                            .usuario(u8)
                            .comunidad(c5)
                            .rol(RolComunidad.MIEMBRO)
                            .build();

            miembroRepo.saveAll(
                    List.of(m1, m2, m3, m4, m5, m6, m7, m8, m9, m10, m11, m12, m13, m14, m15));

            // ============================
            // 6. EVENTOS
            // ============================
            LocalDateTime now = LocalDateTime.now();

            // Evento 1 - Presencial público
            Evento e1 = new Evento();
            e1.setTitulo("Sesión de estudio ISPP - Sprint Review");
            e1.setDescripcion(
                    "Revisaremos las tareas completadas del Sprint 1 "
                            + "y planificaremos el Sprint 2.");
            e1.setFechaHora(now.plusDays(2).withHour(10).withMinute(0));
            e1.setFechaFin(now.plusDays(2).withHour(13).withMinute(0));
            e1.setAforo(20);
            e1.setAsistentesConfirmados(3);
            e1.setQueLlevar("Portátil y apuntes del Sprint 1");
            e1.setEsVirtual(false);
            e1.setVisibleMapa(true);
            e1.setCancelado(false);
            e1.setPrivado(false);
            e1.setCreador(u2);
            e1.setUbicacion(ub1);
            e1.setComunidad(c1);

            // Evento 2 - Virtual público
            Evento e2 = new Evento();
            e2.setTitulo("Workshop: React + TypeScript");
            e2.setDescripcion(
                    "Taller práctico de introducción a React con TypeScript. "
                            + "Crearemos una app desde cero.");
            e2.setFechaHora(now.plusDays(5).withHour(16).withMinute(0));
            e2.setFechaFin(now.plusDays(5).withHour(19).withMinute(0));
            e2.setAforo(50);
            e2.setAsistentesConfirmados(2);
            e2.setQueLlevar("Node.js instalado, editor de código");
            e2.setEsVirtual(true);
            e2.setEnlaceVirtual("https://meet.google.com/abc-defg-hij");
            e2.setVisibleMapa(true);
            e2.setCancelado(false);
            e2.setPrivado(false);
            e2.setCreador(u3);
            e2.setUbicacion(ub2);
            e2.setComunidad(c2);

            // Evento 3 - Presencial privado
            Evento e3 = new Evento();
            e3.setTitulo("CTF Night - Hack The Box");
            e3.setDescripcion("Noche de retos CTF en grupo. " + "Nivel intermedio-avanzado.");
            e3.setFechaHora(now.plusDays(7).withHour(20).withMinute(0));
            e3.setFechaFin(now.plusDays(8).withHour(2).withMinute(0));
            e3.setAforo(15);
            e3.setAsistentesConfirmados(1);
            e3.setQueLlevar("Portátil con Kali Linux o Parrot OS");
            e3.setEsVirtual(false);
            e3.setVisibleMapa(false);
            e3.setCancelado(false);
            e3.setPrivado(true);
            e3.setCreador(u5);
            e3.setUbicacion(ub3);
            e3.setComunidad(c3);

            // Evento 4 - Presencial público
            Evento e4 = new Evento();
            e4.setTitulo("Repaso de Álgebra Lineal");
            e4.setDescripcion(
                    "Sesión de repaso de temas 1 al 4 de Álgebra. "
                            + "Resolveremos ejercicios de exámenes anteriores.");
            e4.setFechaHora(now.plusDays(3).withHour(9).withMinute(30));
            e4.setFechaFin(now.plusDays(3).withHour(12).withMinute(0));
            e4.setAforo(40);
            e4.setAsistentesConfirmados(2);
            e4.setQueLlevar("Calculadora científica y apuntes");
            e4.setEsVirtual(false);
            e4.setVisibleMapa(true);
            e4.setCancelado(false);
            e4.setPrivado(false);
            e4.setCreador(u6);
            e4.setUbicacion(ub5);
            e4.setComunidad(c5);

            // Evento 5 - Virtual público
            Evento e5 = new Evento();
            e5.setTitulo("Intro a Machine Learning con Python");
            e5.setDescripcion(
                    "Charla introductoria sobre ML usando scikit-learn y pandas. "
                            + "No se requiere experiencia previa.");
            e5.setFechaHora(now.plusDays(10).withHour(17).withMinute(0));
            e5.setFechaFin(now.plusDays(10).withHour(19).withMinute(30));
            e5.setAforo(100);
            e5.setAsistentesConfirmados(0);
            e5.setQueLlevar("Python 3.10+ instalado");
            e5.setEsVirtual(true);
            e5.setEnlaceVirtual("https://meet.google.com/xyz-uvwx-rst");
            e5.setVisibleMapa(false);
            e5.setCancelado(false);
            e5.setPrivado(false);
            e5.setCreador(u4);
            e5.setUbicacion(ub2);
            e5.setComunidad(c4);

            // Evento 6 - Presencial público (próximo)
            Evento e6 = new Evento();
            e6.setTitulo("Coding Dojo: Spring Boot & JPA");
            e6.setDescripcion(
                    "Sesión práctica de desarrollo con Spring Boot. "
                            + "Construiremos una API REST completa.");
            e6.setFechaHora(now.plusDays(1).withHour(15).withMinute(0));
            e6.setFechaFin(now.plusDays(1).withHour(18).withMinute(0));
            e6.setAforo(25);
            e6.setAsistentesConfirmados(4);
            e6.setQueLlevar("Portátil con Java 21 y Maven");
            e6.setEsVirtual(false);
            e6.setVisibleMapa(true);
            e6.setCancelado(false);
            e6.setPrivado(false);
            e6.setCreador(u3);
            e6.setUbicacion(ub2);
            e6.setComunidad(c2);

            // Evento 7 - Cancelado (para probar estados)
            Evento e7 = new Evento();
            e7.setTitulo("Tutoría de Bases de Datos");
            e7.setDescripcion("Sesión de tutoría sobre normalización y SQL avanzado.");
            e7.setFechaHora(now.plusDays(4).withHour(11).withMinute(0));
            e7.setFechaFin(now.plusDays(4).withHour(13).withMinute(0));
            e7.setAforo(15);
            e7.setAsistentesConfirmados(0);
            e7.setQueLlevar("Apuntes de BBDD");
            e7.setEsVirtual(false);
            e7.setVisibleMapa(false);
            e7.setCancelado(true);
            e7.setMotivoCancelacion("El tutor no puede asistir por motivos personales.");
            e7.setPrivado(false);
            e7.setCreador(u8);
            e7.setUbicacion(ub4);
            e7.setComunidad(c1);

            // Evento 8 - Pasado (ya ocurrió)
            Evento e8 = new Evento();
            e8.setTitulo("Hackathon MeerKat - Edición Primavera");
            e8.setDescripcion("Hackathon de 12 horas para desarrollar prototipos innovadores.");
            e8.setFechaHora(now.minusDays(7).withHour(9).withMinute(0));
            e8.setFechaFin(now.minusDays(7).withHour(21).withMinute(0));
            e8.setAforo(60);
            e8.setAsistentesConfirmados(45);
            e8.setQueLlevar("Portátil, cargador, y muchas ganas");
            e8.setEsVirtual(false);
            e8.setVisibleMapa(false);
            e8.setCancelado(false);
            e8.setPrivado(false);
            e8.setCreador(u1);
            e8.setUbicacion(ub3);
            e8.setComunidad(c1);

            List<Evento> eventos = eventoRepo.saveAll(List.of(e1, e2, e3, e4, e5, e6, e7, e8));

            // ============================
            // 7. ASISTENCIAS A EVENTOS
            // ============================
            AsistenciaEvento a1 = new AsistenciaEvento();
            a1.setEvento(e1);
            a1.setUsuario(u3);
            a1.setEstado(EstadoAsistencia.CONFIRMADA);
            a1.setCreatedAt(now.minusHours(2));

            AsistenciaEvento a2 = new AsistenciaEvento();
            a2.setEvento(e1);
            a2.setUsuario(u7);
            a2.setEstado(EstadoAsistencia.CONFIRMADA);
            a2.setCreatedAt(now.minusHours(1));

            AsistenciaEvento a3 = new AsistenciaEvento();
            a3.setEvento(e1);
            a3.setUsuario(u8);
            a3.setEstado(EstadoAsistencia.CONFIRMADA);
            a3.setCreatedAt(now);

            AsistenciaEvento a4 = new AsistenciaEvento();
            a4.setEvento(e2);
            a4.setUsuario(u2);
            a4.setEstado(EstadoAsistencia.CONFIRMADA);
            a4.setCreatedAt(now);

            AsistenciaEvento a5 = new AsistenciaEvento();
            a5.setEvento(e2);
            a5.setUsuario(u7);
            a5.setEstado(EstadoAsistencia.CONFIRMADA);
            a5.setCreatedAt(now);

            AsistenciaEvento a6 = new AsistenciaEvento();
            a6.setEvento(e4);
            a6.setUsuario(u4);
            a6.setEstado(EstadoAsistencia.CONFIRMADA);
            a6.setCreatedAt(now);

            AsistenciaEvento a7 = new AsistenciaEvento();
            a7.setEvento(e4);
            a7.setUsuario(u8);
            a7.setEstado(EstadoAsistencia.CONFIRMADA);
            a7.setCreatedAt(now);

            AsistenciaEvento a8 = new AsistenciaEvento();
            a8.setEvento(e6);
            a8.setUsuario(u2);
            a8.setEstado(EstadoAsistencia.CONFIRMADA);
            a8.setCreatedAt(now);

            AsistenciaEvento a9 = new AsistenciaEvento();
            a9.setEvento(e6);
            a9.setUsuario(u4);
            a9.setEstado(EstadoAsistencia.CONFIRMADA);
            a9.setCreatedAt(now);

            AsistenciaEvento a10 = new AsistenciaEvento();
            a10.setEvento(e6);
            a10.setUsuario(u5);
            a10.setEstado(EstadoAsistencia.CONFIRMADA);
            a10.setCreatedAt(now);

            AsistenciaEvento a11 = new AsistenciaEvento();
            a11.setEvento(e6);
            a11.setUsuario(u7);
            a11.setEstado(EstadoAsistencia.CONFIRMADA);
            a11.setCreatedAt(now);

            AsistenciaEvento a12 = new AsistenciaEvento();
            a12.setEvento(e3);
            a12.setUsuario(u3);
            a12.setEstado(EstadoAsistencia.CONFIRMADA);
            a12.setCreatedAt(now);

            AsistenciaEvento a13 = new AsistenciaEvento();
            a13.setEvento(e8);
            a13.setUsuario(u2);
            a13.setEstado(EstadoAsistencia.CONFIRMADA);
            a13.setCreatedAt(now.minusDays(8));

            AsistenciaEvento a14 = new AsistenciaEvento();
            a14.setEvento(e8);
            a14.setUsuario(u5);
            a14.setEstado(EstadoAsistencia.CANCELADA);
            a14.setCreatedAt(now.minusDays(8));

            asistenciaRepo.saveAll(
                    List.of(a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14));

            // ============================
            // 8. PERFILES DE TUTOR
            // ============================
            Tutor t1 = new Tutor();
            t1.setUsuario(u3);
            t1.setEspecialidades(List.of("Java", "Spring Boot", "Microservicios"));
            t1.setTarifaHora(new java.math.BigDecimal("20.00"));
            t1.setDisponibilidad("Tardes de lunes a viernes");
            t1.setBio(
                    "Estudiante de 3º de ISPP con experiencia en desarrollo backend con Java y"
                            + " Spring Boot.");
            t1.setVerificado(true);
            t1.setClassroomConectado(false);

            Tutor t2 = new Tutor();
            t2.setUsuario(u6);
            t2.setEspecialidades(List.of("Estadística", "R", "Python", "Machine Learning"));
            t2.setTarifaHora(new java.math.BigDecimal("18.50"));
            t2.setDisponibilidad("Fines de semana y lunes por la tarde");
            t2.setBio(
                    "Estudiante de Matemáticas y Ciencia de Datos. Especializada en estadística"
                            + " aplicada y visualización.");
            t2.setVerificado(true);
            t2.setClassroomConectado(false);

            Tutor t3 = new Tutor();
            t3.setUsuario(u4);
            t3.setEspecialidades(
                    List.of(
                            "Inteligencia Artificial",
                            "Machine Learning",
                            "Deep Learning",
                            "Python"));
            t3.setTarifaHora(new java.math.BigDecimal("22.00"));
            t3.setDisponibilidad("Mañanas de martes y jueves, tardes de viernes");
            t3.setBio(
                    "Apasionada de la IA y el aprendizaje automático. Experiencia con TensorFlow,"
                            + " PyTorch y scikit-learn.");
            t3.setVerificado(true);
            t3.setClassroomConectado(false);

            Tutor t4 = new Tutor();
            t4.setUsuario(u5);
            t4.setEspecialidades(List.of("Ciberseguridad", "Hacking Ético", "Redes", "Linux"));
            t4.setTarifaHora(new java.math.BigDecimal("25.00"));
            t4.setDisponibilidad("Noches de entre semana y fines de semana");
            t4.setBio(
                    "Estudiante de último año especializado en ciberseguridad ofensiva y defensiva."
                            + " CTF player activo.");
            t4.setVerificado(true);
            t4.setClassroomConectado(false);

            tutorRepo.saveAll(List.of(t1, t2, t3, t4));

            // ============================
            // INSTITUCIONES
            // ============================
            Institution i1 = new Institution();
            i1.setNombre("Universidad de Sevilla");
            i1.setDescripcion(
                    "Principal universidad pública de Andalucía, referente en ciencia e"
                            + " ingeniería.");
            i1.setEmailContacto("contacto@us.es");
            i1.setTelefonoContacto("+34 954 551 000");
            i1.setDominioEmail("us.es");
            i1.setUbicacion("Sevilla, España");
            i1.setSitioweb("https://www.us.es");
            i1.setLogoUrl(
                    "https://upload.wikimedia.org/wikipedia/commons/e/e1/Universidad_de_Sevilla_Logo.svg");
            i1.setVerificada(true);
            i1.setPlanCorporativo(TipoPlanCorporativo.ESTANDAR);
            i1.setPlanActivo(true);
            i1.setFechaInicioPlan(LocalDateTime.now().minusMonths(2));
            i1.setFechaFinPlan(LocalDateTime.now().plusMonths(10));
            i1.setNumUsuariosPermitidos(500);
            i1.setUsuarioAdmin(u1);

            institutionRepo.save(i1);

            System.out.println("========================================");
            System.out.println("  SEEDER: Datos de prueba cargados");
            System.out.println("  - " + usuarios.size() + " usuarios");
            System.out.println("  - " + ubicaciones.size() + " ubicaciones");
            System.out.println("  - " + comunidades.size() + " comunidades");
            System.out.println("  - 15 miembros de comunidad");
            System.out.println("  - " + eventos.size() + " eventos");
            System.out.println("  - 14 asistencias a eventos");
            System.out.println("  - 4 perfiles de tutor verificados");
            System.out.println("  - 1 institución (Universidad de Sevilla)");
            System.out.println("========================================");
        };
    }
}
