package es.uclm.esibuy.backend.usuarios.application.port.out;

import es.uclm.esibuy.backend.usuarios.application.port.in.ListarUsuariosQuery;
import es.uclm.esibuy.backend.usuarios.domain.model.Usuario;
import es.uclm.esibuy.backend.usuarios.domain.model.UsuarioPage;

/**
 * Puerto de persistencia que necesita el módulo de usuarios.
 *
 * <p>La aplicación depende de este contrato, no de Spring Data ni de MongoDB.
 */
public interface UsuarioRepositoryPort {

    void guardar(Usuario usuario);

    boolean existePorCorreo(String correo);

    /**
     * Consulta usuarios aplicando los criterios recibidos.
     */
    UsuarioPage listar(ListarUsuariosQuery query);

}
