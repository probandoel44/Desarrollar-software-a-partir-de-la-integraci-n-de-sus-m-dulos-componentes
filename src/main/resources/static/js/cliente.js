(() => {
    const section = document.getElementById("reservas");
    if (!section) return;

    const localDateTime = (date) => {
        const pad = (value) => String(value).padStart(2, "0");
        return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
    };

    const makeField = (labelText, control) => {
        const label = document.createElement("label");
        label.append(labelText, control);
        return label;
    };

    let form = section.querySelector(".reservation-form");
    let spaceSelect;
    let vehicleSelect;
    let startInput;
    let endInput;
    const notice = document.createElement("p");
    notice.className = "notice";
    notice.setAttribute("aria-live", "polite");

    if (!form) {
        form = document.createElement("form");
        form.action = "/view/cliente/reservas";
        form.method = "post";
        form.className = "inline-form reservation-form";
        spaceSelect = document.createElement("select");
        spaceSelect.name = "puestoId";
        spaceSelect.required = true;
        vehicleSelect = document.createElement("select");
        vehicleSelect.name = "vehiculoId";
        vehicleSelect.required = true;
        startInput = document.createElement("input");
        startInput.type = "datetime-local";
        startInput.name = "horaInicio";
        startInput.required = true;
        endInput = document.createElement("input");
        endInput.type = "datetime-local";
        endInput.name = "horaFin";
        endInput.required = true;
        form.append(
            makeField("Espacio", spaceSelect),
            makeField("Vehículo", vehicleSelect),
            makeField("Inicio", startInput),
            makeField("Fin estimado", endInput)
        );
        const submit = document.createElement("button");
        submit.className = "primary-button";
        submit.type = "submit";
        submit.textContent = "Reservar";
        form.append(submit);
        section.insertBefore(notice, section.querySelector(".table-wrap"));
        section.insertBefore(form, section.querySelector(".table-wrap"));
        fetch("/view/cliente/vehiculos/opciones")
            .then((response) => response.ok ? response.json() : [])
            .then((vehicles) => {
                for (const vehicle of vehicles) {
                    const option = document.createElement("option");
                    option.value = vehicle.id;
                    option.textContent = `${vehicle.placa} · ${vehicle.tipo}`;
                    vehicleSelect.append(option);
                }
                if (vehicles.length === 0) {
                    notice.textContent = "Registra un vehículo antes de reservar.";
                    form.hidden = true;
                }
            });
    } else {
        spaceSelect = form.elements.namedItem("puestoId");
        vehicleSelect = form.elements.namedItem("vehiculoId");
        startInput = form.elements.namedItem("horaInicio");
        endInput = form.elements.namedItem("horaFin");
        section.insertBefore(notice, form);
    }

    const now = new Date();
    now.setSeconds(0, 0);
    if (!startInput.value) startInput.value = localDateTime(now);
    if (!endInput.value) {
        const end = new Date(now.getTime() + 60 * 60 * 1000);
        endInput.value = localDateTime(end);
    }

    const refreshAvailability = async () => {
        if (!startInput.value || !endInput.value || endInput.value <= startInput.value) {
            notice.textContent = "Selecciona un período válido para consultar espacios.";
            return;
        }
        const query = new URLSearchParams({ horaInicio: startInput.value, horaFin: endInput.value });
        const response = await fetch(`/view/cliente/disponibilidad?${query}`);
        if (!response.ok) {
            notice.textContent = "No fue posible consultar la disponibilidad.";
            return;
        }
        const spaces = await response.json();
        const selected = spaceSelect.value;
        spaceSelect.replaceChildren(new Option("Selecciona un espacio", ""));
        for (const space of spaces) {
            const option = new Option(`${space.codigo} · ${space.zona || "Zona sin asignar"}`, space.id);
            spaceSelect.add(option);
        }
        if (spaces.some((space) => String(space.id) === selected)) spaceSelect.value = selected;
        notice.textContent = `${spaces.length} espacios disponibles para el período seleccionado.`;
        const count = document.querySelector(".stat-grid .stat-card:nth-child(3) strong");
        if (count) count.textContent = String(spaces.length);
        const grid = document.querySelector("#espacios .parking-grid");
        if (grid) {
            grid.replaceChildren();
            for (const space of spaces) {
                const spot = document.createElement("span");
                spot.className = "spot";
                spot.textContent = space.codigo;
                grid.append(spot);
            }
            if (spaces.length === 0) {
                const empty = document.createElement("p");
                empty.className = "empty-state";
                empty.textContent = "No hay espacios disponibles para este período.";
                grid.append(empty);
            }
        }
    };

    startInput.addEventListener("change", refreshAvailability);
    endInput.addEventListener("change", refreshAvailability);
    refreshAvailability();

    const reservationsTable = section.querySelector(".table-wrap table");
    if (reservationsTable) {
        for (const row of reservationsTable.tBodies[0].rows) {
            const cancelForm = row.querySelector('form[action*="/cancelar"]');
            if (!cancelForm) continue;
            const reservationId = cancelForm.action.match(/reservas\/(\d+)\/cancelar/);
            if (!reservationId) continue;
            const extendForm = document.createElement("form");
            extendForm.action = `/view/cliente/reservas/${reservationId[1]}/extender`;
            extendForm.method = "post";
            extendForm.className = "inline-form";
            const newEnd = document.createElement("input");
            newEnd.type = "datetime-local";
            newEnd.name = "horaFin";
            newEnd.required = true;
            newEnd.setAttribute("aria-label", "Nueva hora fin");
            newEnd.value = row.cells[4].textContent.trim().slice(0, 16);
            const extendButton = document.createElement("button");
            extendButton.type = "submit";
            extendButton.className = "text-button";
            extendButton.textContent = "Extender";
            extendForm.append(newEnd, extendButton);
            cancelForm.parentElement.append(extendForm);
        }
    }

    const events = new EventSource("/view/cliente/disponibilidad/eventos");
    events.addEventListener("availability", refreshAvailability);
})();