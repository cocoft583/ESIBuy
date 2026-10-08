package es.uclm.esibuy.backend.usuarios.infrastructure.adapter.out.persistence;

import es.uclm.esibuy.backend.usuarios.application.port.out.UsuarioRepositoryPort;
import es.uclm.esibuy.backend.usuarios.application.port.in.ListarUsuariosQuery;
import es.uclm.esibuy.backend.usuarios.domain.model.Usuario;
import es.uclm.esibuy.backend.usuarios.domain.model.UsuarioPage;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Adaptador de salida que guarda usuarios en MongoDB (patrón Repository).
 *
 * <p>Implementa el puerto que necesita el núcleo ({@link UsuarioRepositoryPort}) usando el
 * repositorio de Spring Data. Así, dominio y aplicación no dependen de MongoDB.
 */
public class UsuarioMongoAdapter implements UsuarioRepositoryPort {

    private final UsuarioMongoRepository repository;
    private final UsuarioDocumentMapper mapper;
    private final MongoTemplate mongoTemplate;

    public UsuarioMongoAdapter(UsuarioMongoRepository repository, UsuarioDocumentMapper mapper) {
        // Se conserva este constructor para las operaciones de registro y los tests
        // existentes; el listado requiere además el MongoTemplate de la aplicación.
        this(repository, mapper, null);
    }

    public UsuarioMongoAdapter(UsuarioMongoRepository repository, UsuarioDocumentMapper mapper,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mapper = mapper;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void guardar(Usuario usuario) {
        repository.save(mapper.aDocumento(usuario));
    }

    @Override
    public boolean existePorCorreo(String correo) {
        return repository.existsByEmail(correo);
    }

    @Override
    public UsuarioPage listar(ListarUsuariosQuery query) {
        if (mongoTemplate == null) {
            throw new IllegalStateException("MongoTemplate es obligatorio para listar usuarios");
        }

        Query mongoQuery = construirConsulta(query);
        // El total debe calcularse antes de limitar la consulta a una página.
        long total = mongoTemplate.count(mongoQuery, UsuarioDocument.class);
        Pageable pageable = PageRequest.of(query.pagina(), query.tamano());
        mongoQuery.with(pageable);

        // El adaptador convierte los documentos a dominio antes de devolverlos
        // para que el resto de la aplicación no dependa de MongoDB.
        List<Usuario> usuarios = mongoTemplate.find(mongoQuery, UsuarioDocument.class)
                .stream()
                .map(mapper::aDominio)
                .toList();
        return new UsuarioPage(usuarios, query.pagina(), query.tamano(), total);
    }

    private Query construirConsulta(ListarUsuariosQuery query) {
        List<Criteria> criterios = new ArrayList<>();
        if (query.busqueda() != null && !query.busqueda().isBlank()) {
            // La búsqueda textual se aplica a cualquiera de los tres campos
            // visibles del listado y se escapa para no interpretar regex del usuario.
            String textoEscapado = Pattern.quote(query.busqueda().trim());
            String patron = ".*" + textoEscapado + ".*";
            criterios.add(new Criteria().orOperator(
                    Criteria.where("email").regex(patron, "i"),
                    Criteria.where("nombre").regex(patron, "i"),
                    Criteria.where("apellidos").regex(patron, "i")
            ));
        }
        if (query.rol() != null) {
            criterios.add(Criteria.where("rol").is(query.rol().name()));
        }
        if (query.bloqueado() != null) {
            criterios.add(Criteria.where("bloqueado").is(query.bloqueado()));
        }
        Query mongoQuery = new Query();
        if (!criterios.isEmpty()) {
            // Cada filtro opcional se combina con AND; los campos de búsqueda
            // se agrupan previamente con OR dentro de su propio criterio.
            mongoQuery.addCriteria(new Criteria().andOperator(criterios));
        }
        return mongoQuery;
    }
}
