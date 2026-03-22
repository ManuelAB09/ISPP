package es.us.meerkat.backend.service;

import es.us.meerkat.backend.entity.Mensaje;
import es.us.meerkat.backend.entity.MensajeLeido;
import es.us.meerkat.backend.entity.Usuario;
import es.us.meerkat.backend.repository.MensajeLeidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MensajeLeidoService {
    @Autowired
    private MensajeLeidoRepository mensajeLeidoRepository;

    public void marcarComoLeido(Mensaje mensaje, Usuario usuario) {
        if (!mensajeLeidoRepository.findByMensajeAndUsuario(mensaje, usuario).isPresent()) {
            MensajeLeido ml = MensajeLeido.builder()
                    .mensaje(mensaje)
                    .usuario(usuario)
                    .leidoAt(LocalDateTime.now())
                    .build();
            mensajeLeidoRepository.save(ml);
        }
    }

    public boolean estaLeido(Mensaje mensaje, Usuario usuario) {
        return mensajeLeidoRepository.findByMensajeAndUsuario(mensaje, usuario).isPresent();
    }

    public List<Long> obtenerIdsMensajesLeidos(Long usuarioId, List<Long> mensajeIds) {
        return mensajeLeidoRepository.findMensajeIdsLeidosByUsuario(usuarioId, mensajeIds);
    }
}
