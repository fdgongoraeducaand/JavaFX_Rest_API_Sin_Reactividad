package com.example.javafx_sinreactividad.modelos;

import com.example.javafx_sinreactividad.modelos.Usuario;
import com.example.javafx_sinreactividad.modelos.ApiService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UsuarioRepository {

    // Almacén en memoria: java.util.List estándar (sin ObservableList)
    private final List<Usuario> baseDatosMemoria = new ArrayList<>();
    private final ApiService apiService = new ApiService();

    /**
     * Carga o reinicia los datos consumiendo la API REST externa.
     */
    public void cargarDesdeApi() throws IOException, InterruptedException {
        List<Usuario> remotos = apiService.obtenerUsuarios();
        baseDatosMemoria.clear();
        baseDatosMemoria.addAll(remotos);
    }

    /**
     * READ: Devuelve una copia inmutable de la lista actual en memoria.
     */
    public List<Usuario> listarTodos() {
        return Collections.unmodifiableList(new ArrayList<>(baseDatosMemoria));
    }

    /**
     * CREATE: Agrega un nuevo usuario calculando un identificador incremental.
     */
    public void crear(Usuario nuevo) {
        int proximoId = baseDatosMemoria.stream()
                .mapToInt(Usuario::getId)
                .max()
                .orElse(0) + 1;
        nuevo.setId(proximoId);
        baseDatosMemoria.add(nuevo);
    }

    /**
     * UPDATE: Modifica los atributos del usuario existente en memoria según su ID.
     */
    public boolean actualizar(Usuario usuarioEditado) {
        for (int i = 0; i < baseDatosMemoria.size(); i++) {
            if (baseDatosMemoria.get(i).getId() == usuarioEditado.getId()) {
                Usuario actual = baseDatosMemoria.get(i);
                actual.setName(usuarioEditado.getName());
                actual.setUsername(usuarioEditado.getUsername());
                actual.setEmail(usuarioEditado.getEmail());
                actual.setPhone(usuarioEditado.getPhone());
                return true;
            }
        }
        return false;
    }

    /**
     * DELETE: Elimina un usuario por su identificador único.
     */
    public boolean eliminar(int id) {
        return baseDatosMemoria.removeIf(u -> u.getId() == id);
    }
}