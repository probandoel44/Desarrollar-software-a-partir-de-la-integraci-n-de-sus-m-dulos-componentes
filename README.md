# MiParqueo

Aplicación web para administrar un parqueadero: cuentas de clientes, vehículos, espacios, reservas, movimientos, pagos y fidelización.

## Requisitos

- Java 17
- MySQL 8, por ejemplo el incluido con XAMPP
- phpMyAdmin para administrar visualmente la base de datos (opcional)

## Base de datos

La configuración local está en `src/main/resources/application.properties`:

- Base de datos: `miparqueo_db`
- Servidor: `localhost:3306`
- Usuario MySQL: `root`
- Contraseña MySQL: vacía

Antes de iniciar la aplicación, crea el esquema en phpMyAdmin. Abre la pestaña **SQL** y ejecuta el siguiente script en una base vacía. No lo ejecutes sobre una base que ya contenga estas tablas o datos. Hibernate está configurado con `ddl-auto=validate`: valida el esquema existente y no crea ni modifica tablas automáticamente.

```sql
CREATE DATABASE IF NOT EXISTS miparqueo_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE miparqueo_db;

CREATE TABLE usuarios (
  id_usuario INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombres VARCHAR(80) NOT NULL,
  apellidos VARCHAR(80) NOT NULL,
  correo VARCHAR(120) NOT NULL,
  telefono VARCHAR(15) NOT NULL,
  contrasena_hash VARCHAR(255) NOT NULL,
  fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  estado VARCHAR(20) NOT NULL DEFAULT 'activo',
  PRIMARY KEY (id_usuario),
  UNIQUE KEY uq_usuarios_correo (correo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE parqueaderos (
  id_parqueadero INT UNSIGNED NOT NULL AUTO_INCREMENT,
  nombre VARCHAR(100) NOT NULL,
  direccion VARCHAR(150) NOT NULL,
  ciudad VARCHAR(60) NOT NULL,
  latitud DECIMAL(10,7) NOT NULL,
  longitud DECIMAL(10,7) NOT NULL,
  total_espacios INT UNSIGNED NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'activo',
  PRIMARY KEY (id_parqueadero)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE espacios (
  id_espacio INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_parqueadero INT UNSIGNED NOT NULL,
  codigo_espacio VARCHAR(20) NOT NULL,
  tipo_espacio VARCHAR(30) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'disponible',
  nivel VARCHAR(20) DEFAULT NULL,
  zona VARCHAR(30) DEFAULT NULL,
  PRIMARY KEY (id_espacio),
  UNIQUE KEY uq_espacios_codigo (id_parqueadero, codigo_espacio),
  KEY idx_espacios_parqueadero (id_parqueadero),
  CONSTRAINT fk_espacios_parqueaderos FOREIGN KEY (id_parqueadero)
    REFERENCES parqueaderos (id_parqueadero) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE vehiculos (
  id_vehiculo INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario INT UNSIGNED NOT NULL,
  placa VARCHAR(10) NOT NULL,
  tipo_vehiculo VARCHAR(20) NOT NULL,
  marca VARCHAR(50) NOT NULL,
  modelo VARCHAR(50) NOT NULL,
  color VARCHAR(30) NOT NULL,
  codigo_qr VARCHAR(100) DEFAULT NULL,
  activo TINYINT(1) NOT NULL DEFAULT 1,
  PRIMARY KEY (id_vehiculo),
  UNIQUE KEY uq_vehiculos_placa (placa),
  UNIQUE KEY uq_vehiculos_codigo_qr (codigo_qr),
  KEY idx_vehiculos_usuario (id_usuario),
  CONSTRAINT fk_vehiculos_usuarios FOREIGN KEY (id_usuario)
    REFERENCES usuarios (id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE reservas (
  id_reserva INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario INT UNSIGNED NOT NULL,
  id_vehiculo INT UNSIGNED NOT NULL,
  id_espacio INT UNSIGNED NOT NULL,
  codigo_reserva VARCHAR(40) NOT NULL,
  fecha_reserva TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  hora_inicio DATETIME NOT NULL,
  hora_fin_estimada DATETIME NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'activa',
  PRIMARY KEY (id_reserva),
  UNIQUE KEY uq_reservas_codigo (codigo_reserva),
  KEY idx_reservas_usuario (id_usuario),
  KEY idx_reservas_vehiculo (id_vehiculo),
  KEY idx_reservas_espacio (id_espacio),
  CONSTRAINT fk_reservas_espacios FOREIGN KEY (id_espacio)
    REFERENCES espacios (id_espacio) ON UPDATE CASCADE,
  CONSTRAINT fk_reservas_usuarios FOREIGN KEY (id_usuario)
    REFERENCES usuarios (id_usuario) ON UPDATE CASCADE,
  CONSTRAINT fk_reservas_vehiculos FOREIGN KEY (id_vehiculo)
    REFERENCES vehiculos (id_vehiculo) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE ingresos_salidas (
  id_movimiento INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_reserva INT UNSIGNED DEFAULT NULL,
  id_vehiculo INT UNSIGNED NOT NULL,
  hora_ingreso DATETIME NOT NULL,
  hora_salida DATETIME DEFAULT NULL,
  metodo_acceso VARCHAR(20) NOT NULL,
  observaciones VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (id_movimiento),
  KEY idx_ingresos_reserva (id_reserva),
  KEY idx_ingresos_vehiculo (id_vehiculo),
  CONSTRAINT fk_ingresos_reservas FOREIGN KEY (id_reserva)
    REFERENCES reservas (id_reserva) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT fk_ingresos_vehiculos FOREIGN KEY (id_vehiculo)
    REFERENCES vehiculos (id_vehiculo) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE pagos (
  id_pago INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_reserva INT UNSIGNED NOT NULL,
  monto DECIMAL(10,2) NOT NULL,
  metodo_pago VARCHAR(30) NOT NULL,
  estado_pago VARCHAR(20) NOT NULL DEFAULT 'pendiente',
  fecha_pago DATETIME DEFAULT NULL,
  referencia_transaccion VARCHAR(80) DEFAULT NULL,
  PRIMARY KEY (id_pago),
  UNIQUE KEY uq_pagos_referencia (referencia_transaccion),
  KEY idx_pagos_reserva (id_reserva),
  CONSTRAINT fk_pagos_reservas FOREIGN KEY (id_reserva)
    REFERENCES reservas (id_reserva) ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE ubicaciones_vehiculo (
  id_ubicacion INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_movimiento INT UNSIGNED NOT NULL,
  latitud DECIMAL(10,7) NOT NULL,
  longitud DECIMAL(10,7) NOT NULL,
  nivel VARCHAR(20) DEFAULT NULL,
  zona VARCHAR(30) DEFAULT NULL,
  fecha_registro TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_ubicacion),
  KEY idx_ubicaciones_movimiento (id_movimiento),
  CONSTRAINT fk_ubicaciones_movimientos FOREIGN KEY (id_movimiento)
    REFERENCES ingresos_salidas (id_movimiento) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE fidelizacion (
  id_fidelizacion INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario INT UNSIGNED NOT NULL,
  puntos_acumulados INT UNSIGNED NOT NULL DEFAULT 0,
  nivel VARCHAR(30) NOT NULL DEFAULT 'basico',
  ultima_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id_fidelizacion),
  UNIQUE KEY uq_fidelizacion_usuario (id_usuario),
  KEY idx_fidelizacion_usuario (id_usuario),
  CONSTRAINT fk_fidelizacion_usuarios FOREIGN KEY (id_usuario)
    REFERENCES usuarios (id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

CREATE TABLE notificaciones (
  id_notificacion INT UNSIGNED NOT NULL AUTO_INCREMENT,
  id_usuario INT UNSIGNED NOT NULL,
  titulo VARCHAR(100) NOT NULL,
  mensaje VARCHAR(255) NOT NULL,
  tipo VARCHAR(20) NOT NULL,
  fecha_envio TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  leida TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id_notificacion),
  KEY idx_notificaciones_usuario (id_usuario),
  CONSTRAINT fk_notificaciones_usuarios FOREIGN KEY (id_usuario)
    REFERENCES usuarios (id_usuario) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

Al iniciar la aplicación, se crean las cuentas de demostración, el parqueadero y 24 espacios si aún no existen.

No uses las credenciales locales de MySQL en producción. Configura usuario y contraseña mediante variables de entorno o un archivo de configuración excluido del repositorio.

## Ejecutar

Desde la carpeta del proyecto, en PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

La aplicación queda disponible en:

- Inicio de sesión: http://localhost:8081/login.html
- Panel de cliente: http://localhost:8081/view/cliente
- Panel de administración: http://localhost:8081/dashboard
- Administración de usuarios: http://localhost:8081/view/usuarios/list

Para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

## Cuentas de demostración

Al iniciar, la aplicación crea estas cuentas si sus correos todavía no existen:

| Perfil | Usuario | Contraseña inicial |
| --- | --- | --- |
| Administrador | `admin@miparqueo.local` | `admin` |
| Cliente | `cliente@miparqueo.local` | `cliente` |

Las contraseñas se guardan como hashes BCrypt. El inicializador no reemplaza la contraseña de una cuenta existente; si ya había una cuenta con ese correo, su contraseña puede ser distinta. Cambia las claves de demostración antes de usar la aplicación fuera del entorno local.

El administrador se identifica por el correo configurado en `app.admin.email`. Las demás cuentas se tratan como clientes.

## Servicio de autenticación

La API recibe y responde JSON:

- `POST /api/auth/register`: registra una cuenta con `usuario` (correo) y `contrasena`.
- `POST /api/auth/login`: autentica y crea una sesión web.

Ejemplo de solicitud:

```json
{
  "usuario": "cliente@ejemplo.com",
  "contrasena": "mi-clave"
}
```

El registro también acepta opcionalmente `nombres`, `apellidos` y `telefono`.

## Paneles

- El cliente consulta sus vehículos, espacios disponibles, reservas, estancias, pagos y puntos.
- El administrador gestiona ocupación, ingresos y salidas, alertas, pagos, vehículos, fidelización y usuarios.
- La eliminación definitiva de una cuenta elimina sus datos asociados y requiere confirmación.

## Evidencia de requerimientos

Los estados indican implementación en código, no ejecución verificada. La lectura QR y la barrera son simulaciones; no hay hardware ni protocolo conectado.

| Requerimiento | Módulo y código | Pruebas | Estado |
| --- | --- | --- | --- |
| R01. Consultar disponibilidad por período y reservar un puesto. | `controller/ClienteController.java`, `service/ParqueaderoService.java`, `repository/ReservaRepository.java`, `static/js/cliente.js` | `ReservaRulesTest`, `AppmiparqueoApplicationTests` (consulta y concurrencia) | Implementado en código; pruebas pendientes de ejecución. |
| R02. Extender/cancelar y actualizar disponibilidad conservando historial. | `ClienteController.java`, `ParqueaderoService.java`, `cliente.html`, `cliente.js` | `AppmiparqueoApplicationTests` (extensión, conflicto, cancelación y vencimiento) | Implementado en código; pruebas pendientes de ejecución. |
| R03. Confirmar creación, extensión y cancelación dentro de la aplicación. | `ParqueaderoService.java`, `repository/AlertaRepository.java`, `cliente.html` | `AppmiparqueoApplicationTests` (avisos) | Implementado como notificación interna persistida; no se envía correo ni SMS. |
| R04. Reconocer placa o simular lectura del código de un vehículo. | `service/SimuladorAccesoService.java`, `controller/AccesoSimuladoController.java`, `movimientos.html`, `model/Vehiculo.java` | `SimuladorAccesoServiceTest` | Simulado con entrada de texto; no hay OCR, cámara ni lector QR físico. |
| R05. Accionar una barrera después de una lectura válida. | `service/BarreraSimulada.java`, `SimuladorAccesoService.java` | `BarreraSimuladaTest`, `SimuladorAccesoServiceTest` | Ciclo de apertura/cierre simulado; integración física pendiente. |
| R06. Guardar entradas y salidas con fecha y hora. | `service/ParqueaderoService.java`, `model/Movimiento.java`, `repository/MovimientoRepository.java` | `AppmiparqueoApplicationTests` (ciclo de estacionamiento) | Implementado para operaciones manuales y simuladas; pruebas pendientes de ejecución. |
| R07. Sincronizar lectura, operación y persistencia. | `ParqueaderoService.java`, `repository/PuestoRepository.java`, `repository/ReservaRepository.java` | `AppmiparqueoApplicationTests` (concurrencia) | Transacciones y bloqueo de puesto implementados en una instancia; sincronización con dispositivo físico pendiente. |
| R08. Informar errores y registrar incidentes de acceso/operación. | `controller/AccesoSimuladoController.java`, `controller/MovimientosController.java`, `ParqueaderoService.java`, `AlertasController.java` | `SimuladorAccesoServiceTest`; falta prueba de integración para persistencia del incidente | Parcial: se registran errores de movimiento y lectura simulada como alertas administrativas. |
| R09. Consultar historial propio de movimientos y reservas. | `ClienteController.java`, `MovimientoRepository.java`, `ReservaRepository.java`, `cliente.html` | `AppmiparqueoApplicationTests` (panel e historial de reserva) | Implementado en código; pruebas pendientes de ejecución. |
| R10. Mantener actualizada la disponibilidad de la interfaz. | `DisponibilidadController.java`, `DisponibilidadEventPublisher.java`, `ClienteController.java`, `cliente.js` | Falta prueba de integración de SSE y actualización visual | Implementado mediante eventos SSE dentro de la instancia; sin SLA acordado y sin propagación entre varias instancias. |

## Plataforma y capas

- Sistema operativo de desarrollo observado: Windows; su versión no está especificada. El proyecto requiere Java 17 y Maven Wrapper (`mvnw.cmd`).
- Backend: Spring Boot 2.5.14, Spring MVC y Bean Validation. El starter web utiliza normalmente Tomcat embebido; no se configura un servidor externo.
- Reglas: servicios Spring (`service`). Persistencia: Spring Data JPA y MySQL Connector/J 8.0.33 (`repository` y `model`).
- Interfaz: Thymeleaf, HTML, CSS y JavaScript sin framework frontend adicional (`templates` y `static`).
- Seguridad existente: BCrypt y sesión web con interceptor de acceso por rol. La aplicación no incorpora `spring-boot-starter-security`; no se ha documentado una política formal de CSRF, privacidad ni conservación de datos.
- Pruebas: JUnit 5, Mockito y Spring Test, gestionados por Spring Boot Test. No se encontró una base en memoria ni configuración de perfil de pruebas.

La arquitectura actual es una aplicación MVC monolítica con MySQL. Un contenedor o máquina virtual no es requisito para ejecutarla; podría servir para aislar versiones y reproducir un ambiente de integración, pero añadirlos requeriría decidir y mantener esa infraestructura. No se agregaron por asumir que fueran obligatorios.

## Ambientes

### Desarrollo

1. Instala Java 17 y MySQL 8. En Windows, XAMPP puede proporcionar MySQL; phpMyAdmin es opcional.
2. Crea `miparqueo_db` y ejecuta el esquema SQL de la sección Base de datos. No hay migraciones automáticas; `ddl-auto=validate` requiere que el esquema exista.
3. La configuración actual está en `src/main/resources/application.properties`. Se puede sobreescribir con variables de entorno de PowerShell; reemplaza los marcadores por valores locales y no los guardes en el repositorio:

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/miparqueo_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME = "<usuario-local>"
$env:SPRING_DATASOURCE_PASSWORD = "<clave-local>"
```

4. Desde la carpeta raíz, inicia con `.\mvnw.cmd spring-boot:run`. La aplicación usa el puerto `8081`. El inicializador crea datos de demostración si faltan.
5. Inicia sesión en `http://localhost:8081/login.html`. El cliente puede registrar vehículos, consultar horarios, reservar, extender/cancelar y ver confirmaciones. En administración, `Movimientos` permite operaciones manuales y un simulador de placa/código.

### Pruebas

Usa una base separada, por ejemplo `miparqueo_test_db`, y carga en ella el mismo esquema antes de probar. Para ello, crea la base y cambia las instrucciones `CREATE DATABASE` y `USE` del script SQL a `miparqueo_test_db`; no ejecutes el script de desarrollo sin cambiar su destino. No apuntes las pruebas de integración a datos que quieras conservar: una prueba concurrente crea y elimina sus registros, y las demás usan transacciones de prueba. No se configuró todavía un perfil Maven/Spring independiente para pruebas.

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:mysql://localhost:3306/miparqueo_test_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_USERNAME = "<usuario-pruebas>"
$env:SPRING_DATASOURCE_PASSWORD = "<clave-pruebas>"
.\mvnw.cmd test
```

Para ejecutar solo las pruebas unitarias nuevas:

```powershell
.\mvnw.cmd "-Dtest=ReservaRulesTest,SimuladorAccesoServiceTest,BarreraSimuladaTest" test
```

Para probar la integración de reservas y el ciclo de estacionamiento:

```powershell
.\mvnw.cmd "-Dtest=AppmiparqueoApplicationTests" test
```

Resultado esperado cuando Java 17, MySQL y el esquema de pruebas estén disponibles: Maven termina con `BUILD SUCCESS` y Surefire informa cero fallos. Las pruebas no se ejecutaron al preparar esta actualización porque el entorno de trabajo no tiene `java` en `PATH` ni `JAVA_HOME` configurado.

## Criterios no funcionales pendientes

Los siguientes puntos no tienen umbrales aprobados en los documentos disponibles; no se asignan cifras arbitrarias.

| Categoría | Criterio propuesto | Verificación o decisión pendiente |
| --- | --- | --- |
| Usabilidad | Mostrar disponibilidad, confirmación y errores en el flujo de reserva. | Recorrido de cliente y prueba de mensajes; definir con el instructor el plazo máximo que significa “tiempo real”. |
| Rendimiento | Medir consultas y reservas bajo concurrencia representativa. | Acordar carga y límite de latencia antes de una prueba de carga. |
| Fiabilidad | Rechazar exactamente una de dos reservas concurrentes para períodos cruzados; permitir períodos contiguos. | Prueba de integración con MySQL y revisión de estado final. |
| Seguridad | Hash BCrypt, acceso autenticado y permisos por rol; evitar exponer hashes. | Ampliar pruebas de autorización y definir política CSRF antes de producción. |
| Privacidad | Un cliente solo consulta su historial y se define conservación de datos. | Pruebas de acceso cruzado; falta una política de retención aprobada. |
| Interoperabilidad | Definir contrato para lector y barrera y probarlo contra simulador y equipo real. | No hay protocolo ni hardware; la prueba física permanece pendiente. |

El vencimiento se procesa automáticamente cuando se consulta disponibilidad, se reserva o se abre un panel operativo; no hay tarea periódica de fondo. Los eventos SSE actualizan las sesiones conectadas a esta instancia, pero no coordinan varias instancias. Los horarios se manejan como `LocalDateTime`; la política de zona horaria entre navegador, servidor y base de datos aún debe acordarse. Esas limitaciones deben revisarse si el despliegue cambia.

## Lista de entrega SENA

Este apartado contrasta los elementos solicitados para la evidencia con los archivos y repositorios disponibles. Los estados describen evidencia localizada; no equivalen a aprobación del instructor ni a pruebas ejecutadas.

### Repositorios de probandoel44

La lista se obtuvo del endpoint público de repositorios de GitHub el 2026-11-22. Los nombres con `-main` o `-master` son repositorios separados; no se consideran ramas del candidato integrado.

| Repositorio | Uso/evidencia visible | Rama y commit revisados | Estado para la entrega MiParqueo |
| --- | --- | --- | --- |
| [Desarrollar-software-a-partir-de-la-integraci-n-de-sus-m-dulos-componentes](https://github.com/probandoel44/Desarrollar-software-a-partir-de-la-integraci-n-de-sus-m-dulos-componentes) | Aplicación integrada Spring Boot MVC con capas de controladores, servicios, repositorios, modelos y vistas. | `main`, `e39245a0f227d1e0d29e35745a4b7df938b1044e` | Candidato canónico recomendado; falta aprobación oficial y pruebas asociadas a esta versión. |
| [appMiparqueo](https://github.com/probandoel44/appMiparqueo) | Otra copia del proyecto Java MiParqueo; README contiene instalación y esquema local. | `main`, `f321e5db81f6803fa94736f9baea4f073e0dd98f` | Snapshot separado; comparar con el candidato antes de tratarlo como fuente vigente. |
| [appmiparqueo-master](https://github.com/probandoel44/appmiparqueo-master) | Copia importada bajo una carpeta `appmiparqueo-master`; GitHub reporta Java. | `main`, `0aeb7c49bf393f94427cc24fcef82151facf9a65` | Snapshot anterior/alternativo; no se verificó como dependencia del candidato. |
| [app-mi-parqueo-vista-main](https://github.com/probandoel44/app-mi-parqueo-vista-main) | Copia de vistas y recursos web bajo una carpeta `app-mi-parqueo-vista-main`. | `main`, `c5ac91970a32cf13238ec11b788e97b6aecc91dc` | Snapshot de interfaz; no hay evidencia de un backend o despliegue independiente. |
| [app-mi-parqueo-javaScript-y-css-master](https://github.com/probandoel44/app-mi-parqueo-javaScript-y-css-master) | Copia del proyecto de vistas/JavaScript/CSS con código Java anterior, importada bajo carpeta homónima. | `main`, `0f3666c057af7e7c78a07613d17238f37dc93230` | Snapshot anterior; no se debe sincronizar a ciegas con la aplicación candidata. |
| [Desarrollo-de-servicios-web-main](https://github.com/probandoel44/Desarrollo-de-servicios-web-main) | Copia importada del proyecto integrado anterior, bajo carpeta homónima. | `main`, `3c1e47f7bff27e0b726f75c23dd4abfbe141bb1c` | Antecesor/snapshot; no contiene necesariamente los cambios del candidato. |
| [Mi-parqueo-JDBC-servlet-main](https://github.com/probandoel44/Mi-parqueo-JDBC-servlet-main) | Prototipo Java Servlet/JDBC, con `src/miparqueo` y biblioteca `lib`. | `main`, `28d5d7d8f144b0a4f2c602b1638103e572128a07` | Prototipo distinto; no se encontró integración con la aplicación Spring/JPA. |
| [M-dulos-integrados.](https://github.com/probandoel44/M-dulos-integrados.) | GitHub muestra repositorio vacío, sin árbol de código. | Sin commit de contenido | Sin módulo verificable; pendiente de definir propósito. |
| [Calculadora.java-master](https://github.com/probandoel44/Calculadora.java-master) | Proyecto Java de calculadora, importado bajo carpeta homónima. | `main`, `ebbc0b7d8e907e847c117f1aea3e80ec7a24fd7a` | No pertenece a MiParqueo; excluido de la evidencia funcional. |

La URL al código fuente no es una URL de ejecución. Solo el primer repositorio es el candidato integrado recomendado; sus capas son partes de la misma aplicación y no requieren una URL por controlador. Las copias no se actualizan automáticamente y no están demostradas como servicios independientes. No se propagan cambios funcionales a snapshots, al prototipo JDBC, al repositorio vacío ni a la calculadora porque hacerlo sin comparar y definir el rol de cada repositorio podría crear versiones divergentes o atribuir requisitos a un proyecto ajeno.

### Requisitos de la evidencia

| Elemento solicitado | Evidencia actual | Estado y acción pendiente |
| --- | --- | --- |
| Estudiar el componente formativo. | No se localizó el PDF SENA `requerimientos.pdf` en el workspace. | Pendiente: adjuntar el PDF o un enlace accesible y registrar los conceptos aplicables. |
| Conocer los requerimientos del sistema. | Este README contiene la matriz R01–R10 basada en el resumen funcional compartido y el código revisado. | Parcial: falta contrastarla con el informe oficial de evaluación; el resumen no sustituye ese informe. |
| Documentos o actas de aprobación. | No se encontró un acta firmada ni evidencia equivalente. | Pendiente: anexar la aprobación auténtica. No se infieren firmas, fechas ni responsables. |
| Repositorio de control de versiones para entregar código. | El candidato está en GitHub, rama `main`, commit `e39245a0f227d1e0d29e35745a4b7df938b1044e`. | Existe control de versiones; la etiqueta/release oficial para entrega está pendiente. |
| Archivos ejecutables. | `pom.xml` define una aplicación Java/Spring Boot. No se encontró un JAR generado atribuible a este commit. | Pendiente: generar y verificar el JAR. Un `.exe` no corresponde a esta aplicación web Java; el JAR es el artefacto esperado si la compilación lo genera. |
| URLs desplegadas. | El README contiene direcciones locales `http://localhost:8081/...`; no se encontró URL pública de despliegue. | Pendiente: aportar y verificar ambiente/URL desplegada. `localhost` no es una URL accesible para el evaluador. |
| Documentación por módulo/componente, entradas y salidas. | La estructura de capas y la matriz R01–R10 están descritas aquí; los formularios y controladores muestran parámetros de reserva y movimientos. | Parcial: completar fichas con tipos, validaciones, respuestas, errores y efectos persistidos por flujo. |
| Pruebas y resultados por módulo. | Hay fuentes de prueba unitarias y de integración. `target/surefire-reports` contiene reportes históricos de 9 pruebas de integración y 3 de autenticación, pero su ruta corresponde a otra instalación y no identifica el commit candidato. | No verificado para esta versión. Ejecutar contra la base de pruebas y conservar el reporte, incluyendo pruebas bloqueadas o fallidas. |
| Configuración de servidores y bases de datos. | `application.properties` define MySQL local, `ddl-auto=validate` y puerto `8081`; el README incluye el esquema SQL. | Parcial: separar y validar configuraciones de servidor/base por ambiente; no usar configuración local como producción. No hay migraciones automatizadas identificadas. |
| Ambientes de desarrollo y pruebas. | README da pasos locales con Java 17, Maven Wrapper y MySQL 8. | Parcial: no hay perfil de pruebas separado ni datasource de prueba configurado en el repositorio. Definir una base aislada y registrar su preparación. |
| Manual técnico. | Este README explica instalación local, base de datos, inicio, autenticación, requerimientos y pruebas previstas. | Parcial: faltan manuales completos por flujo, solución de problemas basada en ejecuciones reales y resultados verificados para el commit candidato. |

### Límites de verificación

- El código de reconocimiento de placa/QR y la barrera es un simulador de entrada; no hay cámara, lector ni dispositivo físico conectado. Las pruebas del simulador no verifican hardware real.
- Las confirmaciones de reserva son internas a la aplicación. No se documenta ni se configura un canal externo de correo o SMS.
- No se ha establecido un umbral de latencia para “tiempo real”, una carga objetivo ni una URL de producción.
- Los reportes guardados en `target` son artefactos preexistentes y no deben presentarse como resultado de la versión `e39245a`.
- El repositorio vacío `M-dulos-integrados.` no se considera dependencia de ejecución ni módulo entregado hasta que se confirme su propósito.

### Acciones para cerrar la entrega

1. Adjuntar el informe oficial de evaluación y `requerimientos.pdf`.
2. Obtener o adjuntar el acta real de aprobación de requerimientos.
3. Confirmar si el repositorio vacío debe alojar algún contenido o si se mantiene fuera de la entrega.
4. Definir el commit/tag oficial, compilar el JAR y registrar su nombre y ubicación reales.
5. Ejecutar pruebas unitarias, de integración y un flujo integrado en un ambiente aislado; guardar comandos y resultados asociados al commit oficial.
6. Aportar la URL real de despliegue o declarar formalmente que solo se entrega ejecución local.
7. Completar las fichas de entradas/salidas, configuraciones de ambientes y manual técnico con evidencia verificable.
