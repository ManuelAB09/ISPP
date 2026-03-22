package es.us.meerkat.backend.service;

import es.us.meerkat.backend.entity.MensajeComunidad;
import es.us.meerkat.backend.entity.MensajeComunidadLeido;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeComunidadLeidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MensajeComunidadLeidoService {
    @Autowired
    private MensajeComunidadLeidoRepository mensajeComunidadLeidoRepository;

    public void marcarComoLeido(MensajeComunidad mensaje, Usuario usuario) {
        if (!mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(mensaje, usuario).isPresent()) {
            MensajeComunidadLeido ml = MensajeComunidadLeido.builder()
                    .mensajeComunidad(mensaje)
                    .usuario(usuario)
                    .leidoAt(LocalDateTime.now())
                    .build();
            mensajeComunidadLeidoRepository.save(ml);
        }
    }

    public boolean estaLeido(MensajeComunidad mensaje, Usuario usuario) {
        return mensajeComunidadLeidoRepository.findByMensajeComunidadAndUsuario(mensaje, usuario).isPresent();
    }

    public List<Long> obtenerIdsMensajesLeidos(Long usuarioId, List<Long> mensajeIds) {
        return mensajeComunidadLeidoRepository.findMensajeIdsLeidosByUsuario(usuarioId, mensajeIds);
    }
}
