package com.appmiparqueo.appmiparqueo.repository;

import com.appmiparqueo.appmiparqueo.model.Reserva;
import com.appmiparqueo.appmiparqueo.model.Usuarios;
import com.appmiparqueo.appmiparqueo.model.Vehiculo;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservaRepository extends JpaRepository<Reserva, Integer> {
    List<Reserva> findByUsuarioOrderByHoraInicioDesc(Usuarios usuario);
    List<Reserva> findByUsuarioAndEstadoOrderByHoraInicioDesc(Usuarios usuario, String estado);
    Optional<Reserva> findFirstByVehiculoAndEstadoOrderByHoraInicioDesc(Vehiculo vehiculo, String estado);
    List<Reserva> findByEstadoAndHoraFinEstimadaLessThanEqual(String estado, LocalDateTime hora);
    List<Reserva> findByEspacio_IdAndEstadoInAndHoraFinEstimadaAfter(
        Integer espacioId, Collection<String> estados, LocalDateTime hora);

    @Query("select r from Reserva r where r.espacio.id = :espacioId and r.estado in :estados "
        + "and r.horaInicio < :horaFin and r.horaFinEstimada > :horaInicio")
    List<Reserva> findOverlappingReservations(@Param("espacioId") Integer espacioId,
        @Param("estados") Collection<String> estados,
        @Param("horaInicio") LocalDateTime horaInicio,
        @Param("horaFin") LocalDateTime horaFin);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Reserva r where r.id = :id")
    Optional<Reserva> findLockedById(@Param("id") Integer id);
}