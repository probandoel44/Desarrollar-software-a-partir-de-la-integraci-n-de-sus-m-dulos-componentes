package com.appmiparqueo.appmiparqueo.repository;

import com.appmiparqueo.appmiparqueo.model.Alerta;
import com.appmiparqueo.appmiparqueo.model.Usuarios;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertaRepository extends JpaRepository<Alerta, Integer> {
    List<Alerta> findByResueltaAndNivelInOrderByFechaDesc(boolean resuelta, List<String> niveles);
    List<Alerta> findByUsuario(Usuarios usuario);
    List<Alerta> findByUsuarioAndNivelStartingWithOrderByFechaDesc(Usuarios usuario, String prefijo);
    long countByResueltaFalseAndNivelIn(List<String> niveles);
}