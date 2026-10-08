package es.uclm.esibuy.backend.usuarios.infrastructure.adapter.out.persistence;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Forma en que se guarda un usuario en MongoDB (colección {@code usuarios}).
 *
 * <p>Es distinta de {@code Usuario} del dominio, que no conoce MongoDB. Los nombres de los
 * campos siguen el «Planteamiento inicial BBDD». El rol se guarda como texto para que el
 * documento no dependa del enum del dominio.
 *
 */
@Document(collection = "usuarios")
public class UsuarioDocument {

    @Id
    private String id;
    private final String email;
    private final String passwordHash;
    private final String rol;
    private final String nombre;
    private final String apellidos;
    private final String tipoCliente;
    private final boolean bloqueado;

    public UsuarioDocument(String email, String passwordHash, String rol) {
        this(null, email, passwordHash, rol, null, null, null, false);
    }

    @PersistenceCreator //Se añadio para indicar a Spring Data qué constructor debe utilizar para leer los documentos.
    public UsuarioDocument(String id, String email, String passwordHash, String rol,
            String nombre, String apellidos, String tipoCliente, boolean bloqueado) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = rol;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.tipoCliente = tipoCliente;
        this.bloqueado = bloqueado;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRol() {
        return rol;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellidos() {
        return apellidos;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public boolean isBloqueado() {
        return bloqueado;
    }
}
